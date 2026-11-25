package utils;

import java.util.regex.Pattern;

public final class AuthValidation {
    private static final Pattern PHONE = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern EMAIL = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern USERNAME = Pattern.compile("^[a-zA-Z0-9_\\u4e00-\\u9fa5]{2,20}$");

    private AuthValidation() {
    }

    public static boolean isPhone(String phone) {
        return hasText(phone) && PHONE.matcher(phone.trim()).matches();
    }

    public static boolean isEmail(String email) {
        return hasText(email) && EMAIL.matcher(email.trim()).matches();
    }

    public static boolean isUsername(String username) {
        return hasText(username) && USERNAME.matcher(username.trim()).matches();
    }

    public static String trimToNull(String value) {
        if (value == null) return null;
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
