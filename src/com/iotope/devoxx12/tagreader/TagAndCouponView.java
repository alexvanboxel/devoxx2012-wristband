package com.iotope.devoxx12.tagreader;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.View;

import com.iotope.devoxx12.tagreader.TagAndCoupon.Coupon;
import com.iotope.devoxx12.tagreader.TagAndCoupon.State;

public class TagAndCouponView extends View {

	private Paint colorText = new Paint();
	private Bitmap coupon;
	private Bitmap couponR;
	private TagAndCoupon model = TagAndCoupon.i();
	private int couponW;
	private int couponH;

	public TagAndCouponView(Context context, AttributeSet attrs) {
		super(context, attrs);
		loadResources(context);
	}

	public TagAndCouponView(Context context) {
		super(context);
		loadResources(context);
	}

	public TagAndCouponView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		loadResources(context);
	}

	private void loadResources(Context context) {
		Resources res = getContext().getResources();
		coupon = ((BitmapDrawable) res.getDrawable(R.drawable.bmp_coupon)).getBitmap();
		couponR = ((BitmapDrawable) res.getDrawable(R.drawable.bmp_strip)).getBitmap();
		colorText.setColor(Color.BLACK);
		colorText.setTextAlign(Align.LEFT);
		colorText.setTextSize(30);

		couponW = coupon.getWidth() + couponR.getWidth();
		couponH = coupon.getHeight();
	}

	private class CouponPos {
		public CouponPos(int border, int x, int y, float rot) {
			couponMatrix = new Matrix();
			couponMatrix.setRotate(rot, couponW / 2, couponH / 2);
			couponMatrix.preTranslate(border + x, border + y + 0);

			couponStripMatrix = new Matrix();
			couponStripMatrix.setRotate(rot, couponW / 2, couponH / 2);
			couponStripMatrix.preTranslate(border + x + coupon.getWidth() - 1, border + y + 0);
		}

		Matrix couponMatrix, couponStripMatrix;
	}

	private static final Coupon[] couponDrawOrder = new Coupon[] { Coupon.BAG, Coupon.NOXX, Coupon.EAT1, Coupon.EAT2, Coupon.EAT3, Coupon.EAT4 };
	private Map<Coupon, CouponPos> couponPosition = new HashMap<Coupon, CouponPos>();

	private void calcCouponPositions() {
		int border = couponH / 2;
		int canvasW = getWidth() - border * 2;
		int canvasH = getHeight() - border * 2;

		Random random = new Random(System.currentTimeMillis());

		if (canvasH > canvasW) {
			int distrY = canvasH / couponDrawOrder.length;
			int randRangeX = (int) (canvasW - couponW + (canvasW - couponW) * 0.10);
			int randRangeY = (int) (couponH * 0.40);

			int y = 0;
			for (Coupon coupon : couponDrawOrder) {
				couponPosition.put(coupon, new CouponPos(border, (random.nextInt(randRangeX)), y + (random.nextInt(randRangeY)), 10 - random.nextFloat() * 20));
				y += distrY;
			}
		} else {
			int distrY = canvasH / (couponDrawOrder.length / 2);
			int randRangeX = (int) (canvasW - couponW * 2 + (canvasW - couponW) * 0.10) / 2;
			int randRangeY = (int) (couponH * 0.40);

			int y = 0;
			for (int i = 0; i < 3; i++) {
				Coupon left = couponDrawOrder[i];
				couponPosition.put(left, new CouponPos(border, (random.nextInt(randRangeX)), y + (random.nextInt(randRangeY)), 10 - random.nextFloat() * 20));
				Coupon right = couponDrawOrder[i + 3];
				couponPosition.put(right, new CouponPos(border, canvasW / 2 + (random.nextInt(randRangeX)), y + (random.nextInt(randRangeY)), 10 - random.nextFloat() * 20));
				y += distrY;
			}
		}

	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		calcCouponPositions();
		drawCoupons(canvas);
		drawWristband(canvas);
		drawName(canvas);
	}

	private void drawCoupons(Canvas canvas) {
		for (Coupon coupon : couponDrawOrder) {
			drawCoupon(canvas, coupon);
		}
	}

	private void drawWristband(Canvas canvas) {
		String ticketType = "Unknown";
		switch (model.getTicketType()) {
		case COMBI:
			ticketType = "Combi";
			break;
		case UNIVERSITY:
			ticketType = "University";
			break;
		case CONFERENCE:
			ticketType = "Conference";
			break;
		case UNKNOWN:
			ticketType = "Unknown";
			break;
		case ERROR:
			ticketType = "Error";
			break;
		default:
		}
		canvas.drawText(ticketType, 0, 30, colorText);
	}

	private void drawName(Canvas canvas) {
		canvas.drawText(model.getName(), 0, 60, colorText);
	}

	private void drawCoupon(Canvas canvas, Coupon couponType) {
		CouponPos pos = couponPosition.get(couponType);
		switch (model.getCoupon(couponType)) {
		case VERIFIED:
			canvas.drawBitmap(coupon, pos.couponMatrix, null);
			canvas.drawBitmap(couponR, pos.couponStripMatrix, null);
			break;
		default:
			canvas.drawBitmap(coupon, pos.couponMatrix, null);
		}
	}

	public void onModelChange() {
	}
}
