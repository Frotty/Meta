package de.fatox.meta.desktop; /**
 * ZivLempel.java
 * <p>
 * Created by Gus Silva and Anil Jethani
 * <p>
 * The code for the compress and decompress functions
 * was written entirely by Gus Silva and Anil Jethani.
 * The algorithm implemented by the code is from the
 * Wikipedia documentation on Ziv-Lempel.
 */

import com.badlogic.gdx.utils.*;

import java.io.IOException;
import java.util.Arrays;


public class ZivLempel {
    static int maxDictSize = 4000;

    public static void main(String args[]) throws IOException {
        byte[] input = "test".getBytes();//Files.readAllBytes(new File(ZivLempel.class.getClassLoader().getResource("war3map.j").getFile()).toPath());
        IntArray positions = compress(input);
        Encoder encoder = new Encoder();
        for (int i : positions.toArray()) {
            encoder.encode(i, dictSize);
        }
        encoder.encode(dictSize, maxDictSize);
        System.out.println("dictsize " + dictSize);
        byte[] save = encoder.save();
        System.out.println("compressed " + Arrays.toString(save));

        encoder.load(save);
        int decodedDictSize = encoder.decode(maxDictSize);
        System.out.println("decoded dictsize " + decodedDictSize);
        IntArray rpositions = new IntArray();
        while (!encoder.isEmpty()) {
            rpositions.add(encoder.decode(decodedDictSize));
        }

        rpositions.reverse();

        System.out.println("input:\t " + input.length);
        System.out.println("output:\t " + save.length);
        System.out.println("Deflate: " + CompressionUtils.compress(input).length);
        byte[] decompress = decompress(rpositions);
        System.out.println(new String(decompress));
    }

    static int dictSize = 0;
    static ObjectIntMap<String> dictionary = new ObjectIntMap<>();

    /**
     * @return boolean - True if compression is successful,
     * false otherwise.
     */
    public static IntArray compress(byte[] data) {
        IntArray intArray = new IntArray();
        dictSize = 0;
        dictionary.clear();
        IntMap<Integer> dictionary = new IntMap<>();
        for (int i = 0; i < 128; i++) {
            dictionary.put(MurmurHash.hash32(new byte[]{(byte) i}, 1), dictSize);
            dictSize++;
        }

        ByteArray A = new ByteArray(), C = new ByteArray();
        byte B = 0;
        int inc = 0;
        while (inc < data.length - 1) {
            A.clear();
            A.add(data[inc]);
            if (dictionary.get(MurmurHash.hash32(A.toArray(), A.size))==null) {
                System.out.println("impo: " + data[inc]);
            }
            inc++;//X is current character
            B = data[inc];        //Z is next character
            C.clear();
            C.addAll(A);
            C.add(B);
            while (dictionary.containsKey(MurmurHash.hash32(C.toArray(), C.size)))     //Loop until the sequence of
            {                                   //characters is not in dictionary
                A.add(B);
                inc++;
                if (inc < data.length) {
                    B = data[inc];
                    C.clear();
                    C.addAll(A);
                    C.add(B);
                } else {
                    break;
                }

            }
            if (A==null) {
                System.out.println("Aaaa");
            }
            if (A.toArray()==null) {
                System.out.println("bbbb");
            }
            if (dictionary.get(MurmurHash.hash32(A.toArray(), A.size))==null) {
                System.out.println("cccc");
            }
            intArray.add(dictionary.get(MurmurHash.hash32(A.toArray(), A.size)));
            dictionary.put(MurmurHash.hash32(C.toArray(), C.size), dictSize);
            dictSize++;
            //Write index to output
        }
        if (C.size > A.size)
            intArray.add(dictionary.get(MurmurHash.hash32(new byte[]{B}, 1)));

        System.out.println(Arrays.toString(intArray.toArray()));

        return intArray;
    }

    /**
     * @return boolean - true if successful, false otherwise.
     */
    public static byte[] decompress(IntArray data) {
        ByteArray output = new ByteArray();

        Array<byte[]> dict = new Array<>();
        for (int i = 0; i < 128; i++) {
            dict.add(new byte[]{(byte) i});
        }

        ByteArray A = new ByteArray();
        ByteArray C = new ByteArray();
        byte B;
        int[] ints = data.toArray();
        for (int i = 0; i < ints.length; i++) {
            A.clear();
            A.addAll(dict.get(ints[i]));
            C.clear();
            C.addAll(A);
            if ((i + 1) < ints.length && ints[i + 1] < dict.size) {
                B = dict.get(ints[i + 1])[0];
                C.clear();
                C.addAll(A);
                C.add(B);
                dict.add(C.toArray());//must be first letter of current String
            } else if ((i) >= ints.length) { //Reached end of input
                B = 0;
            } else {                                //Dictionary does not have next index
                B = A.get(0);
                C.clear();
                C.addAll(A);
                C.add(B);
                dict.add(C.toArray());//must be first letter of current String
            }
            output.addAll(A);                    //Write to output
        }
        System.out.println(output);  //Print dictionary
        return output.toArray();
    }
}