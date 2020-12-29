package de.fatox.meta.assets;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtils {
    private static final ByteBuffer buffer = ByteBuffer.allocate(1024 * 4 * 4);

    public static byte[] computeSha1(ReadableByteChannel channel) {
        try {
			buffer.rewind();
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            while (channel.read(buffer) != -1) {
                buffer.flip();
                digest.update(buffer);
                buffer.clear();
            }
            return digest.digest();
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException(e);
        }
    }

	//  http://stackoverflow.com/a/3940857/314015
	static String hex(byte[] data) {
		return String.format("%040x", new BigInteger(1, data));
	}

}

