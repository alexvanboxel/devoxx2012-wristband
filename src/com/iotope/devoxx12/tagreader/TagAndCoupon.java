package com.iotope.devoxx12.tagreader;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.util.Log;

public class TagAndCoupon {

	private static final String TAG = "TagAndCoupon";

	public enum State {
		NOT_AUTHENTICATED, VERIFIED, NOT_VALID, UNKNOWN
	}

	public enum Type {
		ERROR, UNKNOWN, CONFERENCE, UNIVERSITY, COMBI
	}

	public enum Coupon {
		BAG(15), NOXX(14), EAT1(13), EAT2(12), EAT3(11), EAT4(10);

		private int sector;

		private Coupon(int sector) {
			this.sector = sector;
		}

		public int getSector() {
			return sector;
		}
	}

	private static final TagAndCoupon instance = new TagAndCoupon();
	
	public static TagAndCoupon i() {
		return instance;
	}
	
	private TagAndCoupon() {
		map = new HashMap<Coupon, State>();
		resetState(false);
	}

	private String tagId;

	private String publicKey = "Devoxx2012";

	private Map<Coupon, State> map;

	private Type ticketType;

	private String name;

	public void handleTagEvent(Tag tag) {
		try {
			MifareClassic mfc = MifareClassic.get(tag);
			if (mfc == null) {
				resetState(true);
				return;
			}
			mfc.connect();
			tagId = IOUtil.hexbin(tag.getId());
			readInfo(mfc);
			for (Coupon coupon : map.keySet()) {
				Log.i(TAG, "Verifying coupon " + coupon);
				try {
					map.put(coupon, verifyCoupon(mfc, coupon.getSector()));
				} catch (IOException e) {
					Log.i(TAG, "Exception for coupon " + coupon + ": " + e.getMessage());
					map.put(coupon, State.UNKNOWN);
				}
			}
		} catch (IOException e1) {
			resetState(true);
		}
	}

	private void resetState(boolean wasError) {
		if(wasError) {
			ticketType = Type.ERROR;
		}
		else {
			ticketType = Type.UNKNOWN;
		}
		map.put(Coupon.BAG, State.UNKNOWN);
		map.put(Coupon.NOXX, State.UNKNOWN);
		map.put(Coupon.EAT1, State.UNKNOWN);
		map.put(Coupon.EAT2, State.UNKNOWN);
		map.put(Coupon.EAT3, State.UNKNOWN);
		map.put(Coupon.EAT4, State.UNKNOWN);
		tagId = "";
		name = "";
	}

	public State getCoupon(Coupon coupon) {
		return map.get(coupon);
	}

	/**
	 * <p>
	 * Authenticate the sector with the default known Mifare Classic Default
	 * Key.
	 * </p>
	 * <p>
	 * In Mifare Classic two keys can be defined. We use key B as a private key,
	 * but Key A can be public. The default key is FF FF FF FF FF FF.
	 * </p>
	 */
	private boolean authenticate(MifareClassic mfc, int sector) throws IOException {
		boolean auth = mfc.authenticateSectorWithKeyA(sector, MifareClassic.KEY_DEFAULT);
		if (auth) {
			Log.i(TAG, "Authenticated with Mifare Classic Default Key :" + IOUtil.hexbin(MifareClassic.KEY_DEFAULT));
		}
		return auth;
	}

	private State verifyCoupon(MifareClassic mfc, int sector) throws IOException {
		Log.i(TAG, "Sector is " + sector);
		if (authenticate(mfc, sector)) {
			byte[] block = mfc.readBlock(sector * 4);
			Log.i(TAG, "Public block content is: " + IOUtil.hexbin(block));
			byte[] verify = getDigest(sector);
			Log.i(TAG, "We expect the follow   : " + IOUtil.hexbin(verify));
			// return true;
			if (Arrays.equals(verify, block)) {
				Log.i(TAG, "Coupon is valid");
				return State.VERIFIED;
			} else {
				Log.i(TAG, "Coupon is NOT valid");
				return State.NOT_VALID;
			}
		}
		Log.i(TAG, "Sector could not be Authenticated.");
		return State.NOT_AUTHENTICATED;
	}

	private void readInfo(MifareClassic mfc) throws IOException {
		if (authenticate(mfc, 2)) {
			byte[] block0 = mfc.readBlock(2 * 4);
			byte[] block1 = mfc.readBlock(2 * 4 + 1);
			byte[] block2 = mfc.readBlock(2 * 4 + 2);
			switch (block0[0]) {
			case 'C':
				ticketType = Type.CONFERENCE;
				break;
			case 'U':
				ticketType = Type.CONFERENCE;
				break;
			case 'X':
				ticketType = Type.CONFERENCE;
				break;
			default:
				ticketType = Type.UNKNOWN;
				name = "";
				return;
			}
			int length = block0[1];
			if (length > 0) {
				byte[] nameBuffer = new byte[length];
				for (int i = 0; i < (length > 15 ? 15 : length); i++) {
					nameBuffer[i] = block1[i];
				}
				if (length > 15) {
					for (int i = 16; i < length; i++) {
						nameBuffer[i] = block2[i - 16];
					}
				}
				name = new String(nameBuffer, "utf-8");
			} else {
				name = "";
			}
		}
		else {
			ticketType = Type.ERROR;
			name = "";
		}
	}

	private byte[] getDigest(int sector) {
		byte[] buffer = new byte[16];
		String message = publicKey + tagId + sector;
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-1");
			byte[] hash = md.digest(message.getBytes("utf-8"));
			for (int i = 0; i < buffer.length; i++) {
				buffer[i] = hash[i];
			}
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return buffer;
	}

	public String getTagId() {
		return tagId;
	}

	public Type getTicketType() {
		return ticketType;
	}

	public String getName() {
		return name;
	}
}