/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package javax.jmdns.util.test;

import java.io.IOException;

import static org.junit.Assert.*;

import javax.jmdns.impl.util.ByteWrangler;

import org.junit.Test;

import static util.StringUtil.*;

public class ByteWranglerTest {

    @Test
    public void testEmptyString() throws IOException {
        final String emtpyString = "";

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

    @Test
    public void testTooLongString() throws IOException {

        final String randomString = randomAsciiString(256);

        final byte[] text = ByteWrangler.encodeText(randomString);

        assertEquals("Byte array should be empty because its too long", ByteWrangler.EMPTY_TXT, text);
    }


    @Test
    public void testTooLongNonAsciiString() throws IOException {
        final String randomString = maxSizeRandomString(256);

        final byte[] text = ByteWrangler.encodeText(randomString);

        assertEquals("Byte array should be empty because its too long", ByteWrangler.EMPTY_TXT, text);
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
    public void testReadingUTF() {
        final int length = 255;

        final String str = maxSizeRandomString(length);
        byte[] bytes = str.getBytes(UTF_8);

        // no read the String back using the ByteWrangler method
        final String readStr = ByteWrangler.readUTF(bytes, 0, bytes.length);

        assertEquals("Resulting String length", str.length(), readStr.length());
        assertEquals("Resulting String", str, readStr);
    }

}