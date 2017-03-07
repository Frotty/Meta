package de.fatox.meta.desktop;

/**
 * Created by Frotty on 12.02.2017.
 */
public class BigNum {
    public static float log(float py, float pbase) {
        float y = py;
        float base = pbase;
        float factor = 1.0f;
        float logy = 0.0f;
        float sign = 1.0f;
        if (y < 0.)
            return 0.0f;

        if (y < 1.) {
            y = 1.0f / y;
            sign = -1.0f;
        }

        //Chop out powers of the base
        while (y >= 1.0001) {
            if (y > base) {
                y = y / base;
                logy = logy + factor;
            } else {
                base = (float) Math.sqrt(base);     //If you use just one base a lot, precompute its squareroots
                factor = factor / 2.0f;
            }
        }
        return sign * logy;
    }

    static class BigNum_l {
        int leaf = 0;
        BigNum_l next = null;
        static int count = 0;

        public BigNum_l() {
            count++;
        }

        void destroy() {
            count--;
        }

        //true:  want destroy
        boolean clean() {
            if (next == null && leaf == 0) {
                return true;
            } else if (next != null && next.clean()) {
                next.destroy();
                next = null;
                return leaf == 0;
            }
            return false;
        }

        int divSmall(int base, int denom) {
            int remainder = 0;
            if (next != null) {
                remainder = next.divSmall(base, denom);
            }

            int num = leaf + remainder * base;
            int quotient = num / denom;
            remainder = num - quotient * denom;
            leaf = quotient;
            return remainder;
        }

    }

    BigNum_l list = null;
    int base;

    public BigNum(int base) {
        this.base = base;
    }

    void destroy() {
        BigNum_l cur = list;
        BigNum_l next;
        while (cur != null) {
            next = cur.next;
            cur.destroy();
            cur = next;
        }
    }

    boolean isZero() {
        BigNum_l cur = list;
        while (cur != null) {
            if (cur.leaf != 0) {
                return false;
            }
            cur = cur.next;
        }
        return true;
    }

    void clean() {
        BigNum_l cur = list;
        cur.clean();
    }


        //fails if bignum is null
        //BASE() + carry must be less than MAXINT()

    void addSmall(int pcarry) {
        BigNum_l cur = list;
        int sum;
        int carry = pcarry;

        if (cur == null) {
            cur = new BigNum_l();
            list = cur;
        }

        while (carry != 0) {
            sum = cur.leaf + carry;
            carry = sum / base;
            sum = sum - carry * base;
            cur.leaf = sum;

            if (cur.next == null) {
                cur.next = new BigNum_l();
            }
            cur = cur.next;
        }
    }


    //x*BASE() must be less than MAXINT()
    void mulSmall(int x) {
        BigNum_l cur = list;
        int product;
        int remainder;
        int carry = 0;

        while (cur != null || carry != 0) {
            product = x * cur.leaf + carry;
            carry = product / base;
            remainder = product - carry * base;
            cur.leaf = remainder;
            if (cur.next == null && carry != 0) {
                cur.next = new BigNum_l();
            }
            cur = cur.next;
        }
    }

    //Returns remainder
    int divSmall(int denom) {
//		print("in divSmall : base=" + base.toString() + " denom=" + denom.toString())
        return list.divSmall(base, denom);
    }

    int lastDigit() {
        BigNum_l cur = list;
        BigNum_l next = cur.next;
        while (next != null) {
            cur = next;
            next = cur.next;
        }
        return cur.leaf;
    }

    String dump() {
        BigNum_l cur = this.list;
        String s = "";
        while (cur != null) {
            s = cur.leaf + " " + s;
            cur = cur.next;
        }
        return s;
    }
}
