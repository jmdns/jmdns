package javax.jmdns.util;

import java.io.IOException;

import javax.jmdns.impl.util.ByteWrangler;

import org.junit.jupiter.api.Test;

import static javax.jmdns.test.util.StringUtil.*;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ByteWranglerTest {

    @Test
    void testEmptyString() throws IOException {
        final String emtpyString = "";

        // contains only one byte
        // - byte[0] contains the length => zero
        // - otherwise no data
        final byte[] text = ByteWrangler.encodeText(emtpyString);

        assertEquals(1, text.length ,"Resulting byte array length");
        assertEquals(0, text[0], "Resulting byte array");
        assertArrayEquals(ByteWrangler.EMPTY_TXT, text, "Resulting byte array");
    }

    @Test
    void testJustLongString() throws IOException {
        final int length = 255;

        final String randomString = randomAsciiString(length);

        // contains one more byte
        // - byte[0] contains the length
        // - byte[1-length] contains data
        final byte[] text = ByteWrangler.encodeText(randomString);

        assertEquals(length + 1, text.length, "Resulting byte array length");
    }

    @Test
    void testTooLongString() throws IOException {

        final String randomString = randomAsciiString(256);

        final byte[] text = ByteWrangler.encodeText(randomString);

        assertEquals(ByteWrangler.EMPTY_TXT, text, "Byte array should be empty because its too long");
    }

    @Test
    void testTooLongNonAsciiString() throws IOException {
        final String randomString = maxSizeRandomString(256);

        final byte[] text = ByteWrangler.encodeText(randomString);

        assertEquals(ByteWrangler.EMPTY_TXT, text, "Byte array should be empty because its too long");
    }

    @Test
    void testJustLongNonAsciiString() throws IOException {
        final int length = 255;

        final String str = maxSizeRandomString(length);
        byte[] bytes = str.getBytes(UTF_8);

        // contains one more byte
        // - byte[0] contains the length
        // - byte[1-length] contains data
        final byte[] text = ByteWrangler.encodeText(str);

        assertEquals(bytes.length + 1, text.length, "Resulting byte array length");

        for (int i = 0; i < bytes.length; i++) {
            assertEquals(bytes[i], text[i + 1], "Resulting byte array");
        }
    }

    @Test
    void testReadingUTF() {
        final int length = 255;

        final String str = maxSizeRandomString(length);
        byte[] bytes = str.getBytes(UTF_8);

        // no read the String back using the ByteWrangler method
        final String readStr = ByteWrangler.readUTF(bytes, 0, bytes.length);

        assertEquals(str.length(), readStr.length(), "Resulting String length");
        assertEquals(str, readStr, "Resulting String");
    }

}