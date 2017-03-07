package de.fatox.meta.desktop;

/**
 * Created by Frotty on 06.03.2017.
 */
public class old {
//    package de.fatox.meta.desktop; /**
//     * ZivLempel.java
//     * <p>
//     * Created by Gus Silva and Anil Jethani
//     * <p>
//     * The code for the compress and decompress functions
//     * was written entirely by Gus Silva and Anil Jethani.
//     * The algorithm implemented by the code is from the
//     * Wikipedia documentation on Ziv-Lempel.
//     */
//
//import com.badlogic.gdx.utils.IntArray;
//import com.badlogic.gdx.utils.ObjectIntMap;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Scanner;
//
//
//    public class ZivLempel {
//        static String charset1 = "_0123456789abcdefghijklmnopqrstuvwxyz-[]";
//        static int maxDictSize = 4000;
//
//        public static void main(String args[]) {
//            String input = "abcabcabcabcabcabc";
//            IntArray positions = compress(input, charset1);
//            Encoder encoder = new Encoder();
//            for(int i : positions.toArray()) {
//                encoder.encode(i, dictSize);
//            }
//            encoder.encode(dictSize, maxDictSize);
//            System.out.println("bn " + encoder.bignum.dump());
//            String save = encoder.save();
//            System.out.println("compressed " + save);
//
//            System.out.println("input:\t " + input.length());
//            System.out.println("output:\t " + outp);
//        }
//
//        static int dictSize = 0;
//        static ObjectIntMap<String>  dictionary = new ObjectIntMap<>();
//        /**
//         * @return boolean - True if compression is successful,
//         * false otherwise.
//         */
//        public static IntArray compress(String data, String charset) {
//            IntArray intArray = new IntArray();
//            dictSize = 0;
//            dictionary.clear();
//            for (int i = 0; i < charset.length(); i++) {
//                dictionary.put(charset.substring(i, i + 1), i);
//                dictSize++;
//            }
//
//            String X = "", Z = "";
//            int inc = 0;
//            while (inc < data.length() - 1) {
//                X = data.charAt(inc) + "";
//                inc++;//X is current character
//                Z = data.charAt(inc) + "";        //Z is next character
//                while (dictionary.containsKey(X + Z))     //Loop until the sequence of
//                {                                   //characters is not in dictionary
//                    X = X + Z;
//                    inc++;
//                    if (inc < data.length()) {
//                        Z = data.charAt(inc) + "";
//                    }
//                }
//                intArray.add(dictionary.get(X, -1));
//                dictionary.put(X + Z, dictSize);
//                dictSize++;
//                //Write index to output
//                System.out.println("X: " + X + " Z: " + Z);
//            }
//            if (Z != null && !(X.charAt(X.length()-1) + "").equalsIgnoreCase(Z))
//                intArray.add(dictionary.get(Z, -1));
//
//
//            System.out.println(Arrays.toString(intArray.toArray())); //Print Dictionary
//            return intArray;
//        }
//
//        /**
//         * @return boolean - true if successful, false otherwise.
//         */
//        public static String decompress(String data, String charset) {
//            Scanner in;
//            String output = "";
//            //Prepare Input File
//            /**
//             * Anil and I have chosen to initialize the dictionary with only letters A-Z
//             * for simplicity. However, we acknowledge that industrial strength
//             * code would initialize dictionary with entire ASCII table.
//             * */
//            ArrayList<String> dictionary = new ArrayList<>(255);
//            for (int i = 0; i < charset.length(); i++) {
//                dictionary.add(charset.substring(i, i + 1));
//            }
//            String curr, X, Z;
//            String[] line;
//            Integer[] ints;
//
//            line = data.split(" ");                 // Convert input into an array
//            ints = new Integer[line.length];
//            for (int i = 0; i < ints.length; i++)   // Parse Strings in array into Ints
//                ints[i] = Integer.parseInt(line[i]);// and transfer to Integer array
//            System.out.println(Arrays.toString(ints));
//            for (int i = 0; i < ints.length-1; i++) {
//                X = dictionary.get(ints[i]);
//                if ((i + 1) < ints.length && ints[i + 1] < dictionary.size()) {
//                    Z = dictionary.get(ints[i + 1]);
//                    Z = Z.charAt(0) + "";
//                } else if ((i) >= ints.length)       //Reached end of input
//                    Z = null;
//                else                                //Dictionary does not have next index
//                    Z = X.charAt(0) + "";           //must be first letter of current String
//                if (Z != null) {
//                    dictionary.add(X + Z);
//                }
//                output += (X);                    //Write to output
//            }
//            output += dictionary.get(ints[ints.length-1]);
//            System.out.println(dictionary.toString());  //Print dictionary
//            System.out.println(output);  //Print dictionary
//            return output;
//        }
//    }
}
