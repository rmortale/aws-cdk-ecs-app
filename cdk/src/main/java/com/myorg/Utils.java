package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;

public class Utils {

    private Utils() {}

    public static void requireNonEmpty(String string, String message) {
        if (string == null || string.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    public static Environment makeEnv(String account, String region) {
        return Environment.builder()
                .account(account)
                .region(region)
                .build();
    }

    public static String getContextVar(App app, String key) {
        String value = (String) app.getNode().tryGetContext(key);
        requireNonEmpty(value, "context variable " + key + " must not be null");
        return value.toLowerCase();
    }
}
