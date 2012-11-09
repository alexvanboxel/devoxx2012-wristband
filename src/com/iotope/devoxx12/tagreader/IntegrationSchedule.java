package com.iotope.devoxx12.tagreader;

import java.util.List;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;

public class IntegrationSchedule {
	private Activity activity;

	private String actionName = "com.devoxx.android.SCHEDULE";
	private final String TAG = "IntegrationSchedule";
	private final String preferedApp = "net.peterkuterna.android.apps.devoxxsched";
	private final String applicationType = "Devoxx Scheduling Application";

	public IntegrationSchedule(Activity activity) {
		this.activity = activity;
	}

	public void launchActivity() {
		Intent intent = new Intent();
		intent.setAction(actionName);
		if(isActivityAvailable(intent)) {
			try {
				activity.startActivity(intent);
			}
			catch(ActivityNotFoundException e) {
				Log.e(TAG,e.getMessage());
			}
		}
		else {
			downloadPreferedApp();
		}
	}

	private void downloadPreferedApp() {
		Uri uri = Uri.parse("market://details?id=" + preferedApp);
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		try {
			activity.startActivity(intent);
		} catch (ActivityNotFoundException anfe) {
			Log.w(TAG, "Android Market is not installed; cannot install " + applicationType);
		}
	}

	private boolean isActivityAvailable(Intent intentSchedule) {
		PackageManager pm = activity.getPackageManager();
		List<ResolveInfo> availableApps = pm.queryIntentActivities(intentSchedule, PackageManager.GET_ACTIVITIES);
		return (availableApps.size() > 0);
	}
}
