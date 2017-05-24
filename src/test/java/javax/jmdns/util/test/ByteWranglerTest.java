package javax.jmdns.util.test;

import java.io.IOException;

import static org.junit.Assert.*;

import javax.jmdns.impl.util.ByteWrangler;

import org.junit.Test;

import static util.StringUtil.*;

public class ByteWranglerTest {

    @Test
    public void testEmptyString() throws IOException {
        final String emtpyString = new String();

        // contains only one byte
        // - byte[0] contains the length => zero
        // - otherwise no data
        final byte[] text = ByteWrangler.encodeText(emtpyString);

        assertEquals("Resulting byte array length", 1, text.length);
        assertEquals("Resulting byte array", 0, text[0]);

        assertArrayEquals("Resulting byte array", ByteWrangler.EMPTY_TXT, text);
    }

    @Test
    public void testJustLongString() throws IOException {
        final int length = 255;

        final String randomString = randomAsciiString(length);

        // contains one more byte
        // - byte[0] contains the length
        // - byte[1-length] contains data
        final byte[] text = ByteWrangler.encodeText(randomString);

        assertEquals("Resulting byte array length", length+1, text.length);
    }

    @Test(expected=IOException.class)
    public void testTooLongString() throws IOException {

        final String randomString = randomAsciiString(256);

        final byte[] text = ByteWrangler.encodeText(randomString);
    }


    @Test(expected=IOException.class)
    public void testTooLongNonAsciiString() throws IOException {
        final String randomString = maxSizeRandomString(256);

        final byte[] text = ByteWrangler.encodeText(randomString);
    }

    @Test
    public void testJustLongNonAsciiString() throws IOException {
        final int length = 255;

        final String str = maxSizeRandomString(length);
        byte[] bytes = str.getBytes(UTF_8);

        // contains one more byte
        // - byte[0] contains the length
        // - byte[1-length] contains data
        final byte[] text = ByteWrangler.encodeText(str);

        assertEquals("Resulting byte array length", bytes.length+1, text.length);

        for (int i = 0; i < bytes.length; i++ ) {
            assertEquals("Resulting byte array", bytes[i], text[i+1]);
        }
    }

    @Test
    public void testReadingUTF() throws IOException {
        final int length = 255;

        final String str = maxSizeRandomString(length);
        byte[] bytes = str.getBytes(UTF_8);

        // no read the String back using the ByteWrangler method
        final String readStr = ByteWrangler.readUTF(bytes, 0, bytes.length);

        assertEquals("Resulting String length", str.length(), readStr.length());
        assertEquals("Resulting String", str, readStr);
    }

}
