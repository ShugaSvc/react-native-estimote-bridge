package com.medium.reactnative.estimote;

import android.content.Context;
import android.content.SharedPreferences;

import static android.content.Context.MODE_PRIVATE;

public class PreferenceHelper {
    private SharedPreferences pref;
    private Context context;
    private final static String PREF_NAME = "com.shuga.shugaapp";
    private final static String LOG_PREFIX = "beaconlog-";

    public PreferenceHelper(Context context) {
        this.context = context;
        this.pref = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE);
    }

    public String getValue(String key, String defaultValue) {
        return this.pref.getString(key, defaultValue);
    }

    public void setBeaconData(String beaconCode, String eventType) {
        Long eventTime = System.currentTimeMillis();
        BeaconData data = new BeaconData();
        data.beaconCode = beaconCode;
        data.platform = "android";
        data.eventType = eventType;
        data.eventTime = eventTime.toString();
        this.saveToPreference(data);
    }

    private void saveToPreference(BeaconData data) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit();
        String key = LOG_PREFIX + "-" + data.eventTime + "-" + data.beaconCode + "-" + data.eventType;
        String value = data.platform;
        editor.putString(key, value);
        editor.commit();
    }
}


