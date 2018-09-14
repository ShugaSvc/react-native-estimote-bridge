package com.medium.reactnative.estimote;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableMap;
import com.estimote.proximity_sdk.api.ProximityZoneContext;
import com.facebook.react.bridge.WritableNativeMap;

public class MapUtil {
    public static String[] toArray(ReadableArray readableArray) {
        String[] array = new String [readableArray.size()];
        for (int i = 0; i < readableArray.size(); i++) {
            array[i] = readableArray.getString(i);
        }
        return array;
    }

    public static WritableMap contextToMap(ProximityZoneContext context) {
        WritableMap map = new WritableNativeMap();
        map.putString("tag", context.getTag());
        map.putString("uid", context.getDeviceId());
        return map;
    }
}