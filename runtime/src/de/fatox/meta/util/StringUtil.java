package de.fatox.meta.util;

import java.util.regex.Pattern;

public class StringUtil {
    public static final String SPACE = " ";
    public static final String EMPTY = "";
    public static final String LF = "\n";
    public static final String CR = "\r";
    private static final Pattern VALID_FOLDER_REGEX = Pattern.compile("[-_.A-Za-z0-9, ]+");

    public static boolean isValidFolderName(final CharSequence cs) {
        return VALID_FOLDER_REGEX.matcher(cs).matches();
    }

    public static boolean isEmpty(final CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    public static boolean isBlank(final CharSequence cs) {
        int strLen;
        if (cs == null || (strLen = cs.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (Character.isWhitespace(cs.charAt(i)) == false) {
                return false;
            }
        }
        return true;
    }

    public static boolean equalsIgnoreCase(final CharSequence str1, final CharSequence str2) {
        if (str1 == null || str2 == null) {
            return str1 == str2;
        } else if (str1 == str2) {
            return true;
        } else if (str1.length() != str2.length()) {
            return false;
        } else {
            return regionMatches(str1, true, 0, str2, 0, str1.length());
        }
    }

    static boolean regionMatches(final CharSequence cs, final boolean ignoreCase, final int thisStart,
                                 final CharSequence substring, final int start, final int length) {
        if (cs instanceof String && substring instanceof String) {
            return ((String) cs).regionMatches(ignoreCase, thisStart, (String) substring, start, length);
        }
        int index1 = thisStart;
        int index2 = start;
        int tmpLen = length;

        while (tmpLen-- > 0) {
            char c1 = cs.charAt(index1++);
            char c2 = substring.charAt(index2++);

            if (c1 == c2) {
                continue;
            }

            if (!ignoreCase) {
                return false;
            }

            // The same check as in String.regionMatches():
            if (Character.toUpperCase(c1) != Character.toUpperCase(c2) &&
                    Character.toLowerCase(c1) != Character.toLowerCase(c2)) {
                return false;
            }
        }

        return true;
    }

}
