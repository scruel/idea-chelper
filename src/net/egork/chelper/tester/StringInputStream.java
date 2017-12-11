package net.egork.chelper.tester;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class StringInputStream extends InputStream {
    private final byte[] bytes;
    private int index = 0;

    public StringInputStream(String s) {
        byte[] bytes;
        try {
            bytes = s.getBytes("UTF-8");
        } catch (UnsupportedEncodingException ignore) {
            bytes = s.getBytes();
        }
        this.bytes = bytes;
    }

    @Override
    public int read() throws IOException {
        if (index < bytes.length)
            return bytes[index++];
        return -1;
    }
}
