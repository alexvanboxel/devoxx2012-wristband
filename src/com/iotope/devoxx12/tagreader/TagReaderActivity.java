package com.iotope.devoxx12.tagreader;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcF;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.View;

public class TagReaderActivity extends Activity {

	private static final String TAG = "TagReaderActivity";

	private NfcAdapter adapter;

	private PendingIntent mPendingIntent;
	private IntentFilter[] mFilters;
	private String[][] mTechLists;
	private IntegrationSchedule integration;

	TagAndCoupon tagAndCoupon = new TagAndCoupon();
	private TagAndCouponView tagAndCouponView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tag_reader);
		tagAndCouponView = (TagAndCouponView) findViewById(R.id.tagAndCouponView1);
		tagAndCouponView.onModelChange(tagAndCoupon);

		integration = new IntegrationSchedule(this);
		adapter = NfcAdapter.getDefaultAdapter(this);

		// Create a generic PendingIntent that will be deliver to this activity.
		// The NFC stack
		// will fill in the intent with the details of the discovered tag before
		// delivering to
		// this activity.
		mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

		IntentFilter tech = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);

		//
		IntentFilter tag = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
		// try {
		// tag.addDataType("*/*");
		// } catch (MalformedMimeTypeException e) {
		// throw new RuntimeException("fail", e);
		// }

		// Setup an intent filter for all MIME based dispatches
		IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
		try {
			ndef.addDataType("*/*");
		} catch (MalformedMimeTypeException e) {
			throw new RuntimeException("fail", e);
		}
		mFilters = new IntentFilter[] { ndef, tag, tech };

		// Setup a tech list for all NfcF tags
		mTechLists = new String[][] { new String[] { NfcF.class.getName() } };

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_tag_reader, menu);
		return true;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (adapter != null)
			adapter.enableForegroundDispatch(this, mPendingIntent, mFilters, mTechLists);
	}

	@Override
	public void onPause() {
		super.onPause();
		if (adapter != null)
			adapter.disableForegroundDispatch(this);
	}

	@Override
	public void onNewIntent(Intent intent) {
		Log.i(TAG, "Tag discovered with intent: " + intent);
		Bundle bundle = intent.getExtras();

		// If you are only interested in the ID, it's available in the bundle
		byte[] id = (byte[]) bundle.get("android.nfc.extra.ID");
		if (id != null) {
			Log.i(TAG, "NFCID is available in bundle: ID is " + IOUtil.hexbin(id));
		}

		// If your tag is NDEF formatted (like the Devoxx 12 wristband), the
		// NDEF messages are here
		Parcelable[] parcelable = (Parcelable[]) bundle.get("android.nfc.extra.NDEF_MESSAGES");
		if (parcelable != null) {
			for (Parcelable parceble : parcelable) {
				NdefMessage ndefMessage = (NdefMessage) parceble;
				for (NdefRecord record : ndefMessage.getRecords()) {
					Log.i("NDEF record payload: ", record.getPayload().toString());
				}
			}
		}

		// You can access the complete tag through the android Tag API
		final Tag tag = (Tag) bundle.get("android.nfc.extra.TAG");
		tagAndCoupon.handleTagEvent(tag);
		tagAndCouponView.onModelChange(tagAndCoupon);
		tagAndCouponView.invalidate();
	}

	public void onGotoSchedule(View view) {
		integration.launchActivity();
	}
}
