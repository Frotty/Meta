package de.fatox.meta.desktop;

import com.badlogic.gdx.utils.ByteArray;

/**
 * Created by Frotty on 12.02.2017.
 */
public class Encoder {

    int base() {
        return 255;
    }

    int hashN() {
        return 1000;
    }

    float digits = 0.0f; //logarithmic approximation
    BigNum bignum = new BigNum(base());

    void destroy() {
        bignum.destroy();
    }

    void encode(int val, int max) {
        digits = digits + BigNum.log(max + 1.0f, base() + 0.0f);
        bignum.mulSmall(max + 1);
        bignum.addSmall(val);
    }

    int decode(int max) {
        return bignum.divSmall(max + 1);
    }

    boolean isEmpty() {
        return bignum.isZero();
    }

    float length() {
        return digits;
    }

    void clean() {
        bignum.clean();
    }

    //These functions get too intimate with BigNum_l
    void pad() {
        BigNum.BigNum_l cur = bignum.list;
        BigNum.BigNum_l prev = null;
        int maxlen = ((int) (1.0 + length()));
        while (cur != null) {
            prev = cur;
            cur = cur.next;
            maxlen--;
        }
        while (maxlen > 0) {
            prev.next = new BigNum.BigNum_l();
            prev = prev.next;
            maxlen--;
        }
    }

    byte[] toByteArray() {
        ByteArray bytes = new ByteArray(Math.round(length()));
        BigNum.BigNum_l cur = bignum.list;
        while (cur != null && cur.leaf != 0) {
            bytes.add((byte) cur.leaf);
            cur = cur.next;
        }
        return bytes.toArray();
    }


    void fromByteArray(byte[] s) {
        int i = 0;
        BigNum.BigNum_l cur = new BigNum.BigNum_l();
        bignum.list = cur;
        while (true) {
            if (i > s.length-1) {
                break;
            }
            cur.leaf = s[i];
            cur.next = new BigNum.BigNum_l();
            cur = cur.next;
            i++;
        }
    }

    int hash() {
        int hash = 0;
        int x;
        BigNum.BigNum_l cur = bignum.list;
        while (cur != null) {
            x = cur.leaf;
            hash = (hash + 79 * hash / (x + 1) + 293 * x / (1 + hash - (hash / base()) * base()) + 479) % hashN();
            cur = cur.next;
        }
        return hash;
    }


    byte[] save() {
        int hash;
        clean();
        hash = hash();
        System.out.println("hash_: " + hash);
        encode(hash, hashN());
        clean();
        pad();
        return toByteArray();
    }


    boolean load(byte[] data) {
        fromByteArray(data);
        int inputhash = decode(hashN());
        clean();
        int h = hash();
        System.out.println("inputhash: " + inputhash + " h: " + h);
        return inputhash == h;
    }
}
