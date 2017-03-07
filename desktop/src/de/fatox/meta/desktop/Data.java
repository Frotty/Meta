package de.fatox.meta.desktop;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Frotty on 17.02.2017.
 */
public class Data {

    private static final char[] ALLOWED_CHARS = "abcdefghijklmnopqrstuvwxyz".toCharArray();

    private static final AtomicInteger uniqueIdCounter = new AtomicInteger();

    public static String getToken() {
        int id = uniqueIdCounter.getAndIncrement();
        return toBijectiveNumber(id);
    }

    public static String toBijectiveNumber(int id) {
        assert id >= 0;

        StringBuilder sb = new StringBuilder(8);

        int divisor = 1;
        int length = 1;
        while (id >= divisor * ALLOWED_CHARS.length) {
            divisor *= ALLOWED_CHARS.length;
            length++;

            id -= divisor;
        }

        for (int i = 0; i < length; i++) {
            sb.append(ALLOWED_CHARS[(id / divisor) % ALLOWED_CHARS.length]);
            divisor /= ALLOWED_CHARS.length;
        }

        return sb.toString();
    }
}
