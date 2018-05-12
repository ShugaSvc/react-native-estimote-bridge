package com.medium.reactnative.estimote;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableMap;
import java.util.Map;

public class MapUtil {
    public static WritableMap toWritableMap(Map<String, String> payload) {
        WritableMap map = Arguments.createMap();
        for (Map.Entry<String, String> entry : payload.entrySet()) {
            map.putString(entry.getKey(), entry.getValue());
        }
        return map;
    }
    public static String[] toArray(ReadableArray readableArray) {
        String[] array = new String [readableArray.size()];
        for (int i = 0; i < readableArray.size(); i++) {
            array[i] = readableArray.getString(i);
        }
        return array;
    }
}