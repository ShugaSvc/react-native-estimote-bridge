package com.medium.reactnative.estimote;

import com.facebook.react.bridge.Arguments;
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

}