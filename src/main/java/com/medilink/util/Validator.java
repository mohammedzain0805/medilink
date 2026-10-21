package com.medilink.util;

import java.util.regex.Pattern;

public final class Validator {
    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    private Validator() {}

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL.matcher(email).matches();
    }
}