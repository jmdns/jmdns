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
package javax.jmdns.impl.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class contains all the byte shifting 
 * 
 * @author Victor Toni
 *
 */
public class ByteWrangler {
    /**
     * Maximum number of bytes a value can consist of.
     */
    public static final int MAX_VALUE_LENGTH = 255;

    /**
     * Maximum number of bytes record data can consist of.
     * It is {@link #MAX_VALUE_LENGTH} + 1 because the first byte contains the number of the following bytes.
     */
    public static final int MAX_DATA_LENGTH = MAX_VALUE_LENGTH + 1;

    /**
     * Representation of no value. A zero length array of bytes.
     */
    public static final byte[] NO_VALUE = new byte[0];

    /**
     * Representation of empty text.
     * The first byte denotes the length of the following character bytes (in this case zero.)
     * <p>
     * FIXME: Should this be exported as a method since it could change externally???
     */
    public final static byte[] EMPTY_TXT = new byte[] { 0 };
    

    /**
     * Charset used to convert Strings to/from wire bytes: UTF-8
     */
    private final static Charset CHARSET_UTF_8 = StandardCharsets.UTF_8;

    /**
     * Write a String as UTF-8 encoded bytes to a stream.
     */
    public static void writeUTF(final OutputStream out, final String str) throws IOException {
        final byte[] utf8Bytes = str.getBytes(CHARSET_UTF_8);
        out.write(utf8Bytes);
    }

    /**
     * Read data bytes as UTF-8 to String.
     */
    public static String readUTF(final byte[] data) {
        return readUTF(data, 0, data.length);
    }

    /**
     * Read data bytes as UTF-8 to String.
     */
    public static String readUTF(final byte[] data, final int off, final int len) {
        return new String(data, off, len, CHARSET_UTF_8);
    }

    public static void readProperties(final Map<String, byte[]> properties, final byte[] textBytes) {
        if (textBytes != null) {
            int off = 0;
            while (off < textBytes.length) {
                // length of the next key value pair
                final int len = textBytes[off++] & 0xFF;

                // error case
                if (off + len > textBytes.length) { // length of data would exceed array bounds
                    properties.clear();
                    break;
                }

                if (len == 0) { // skip zero length parts
                    continue;
                }

                String utf = readUTF(textBytes, off, len);
                if (utf == null || utf.isEmpty()) {
                    continue;
                }

                // look for the '=' and get the property name
                String[] parts = utf.split("=", 2);
                if (parts.length < 2 || parts[1].isEmpty()) {
                    properties.put(parts[0], NO_VALUE);
                } else {
                    parts[1] = parts[1].replaceFirst("\\u0000$", ""); // strip zero endings
                    properties.put(parts[0], parts[1].getBytes(CHARSET_UTF_8));
                }

                off += len;
            }
        }
    }

    public static byte[] textFromProperties(final Map<String, ?> props) {
        byte[] text = null;
        if (props != null) {
            try {
                final ByteArrayOutputStream out = new ByteArrayOutputStream(MAX_DATA_LENGTH);
                for (final Map.Entry<String, ?> entry: props.entrySet()) {
                    final String key = entry.getKey();
                    Object val = entry.getValue();
                    final ByteArrayOutputStream out2 = new ByteArrayOutputStream(100);
                    writeUTF(out2, key);
                    if (val == null) {
                        // Skip
                    } else if (val instanceof String) {
                        out2.write('=');
                        writeUTF(out2, (String) val);
                    } else if (val instanceof byte[]) {
                        byte[] bval = (byte[]) val;
                        if (bval.length > 0) {
                            out2.write('=');
                            out2.write(bval, 0, bval.length);
                        } else {
                            val = null;
                        }
                    } else {
                        throw new IllegalArgumentException("Invalid property value: " + val);
                    }
                    byte[] data = out2.toByteArray();
                    if (isValueTooLarge(data, key + (val == null ? "" : "=" + val))) {
                        return EMPTY_TXT;
                    }
                    out.write((byte) data.length);
                    out.write(data, 0, data.length);
                }
                text = out.toByteArray();
            } catch (final IOException e) {
                throw new RuntimeException("unexpected exception: " + e);
            }
        }

        return (text != null && text.length > 0 ? text : EMPTY_TXT);
    }

    public static byte[] encodeText(final String text) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream(MAX_DATA_LENGTH);
        final ByteArrayOutputStream out2 = new ByteArrayOutputStream(100);
        writeUTF(out2, text);
        final byte[] data = out2.toByteArray();
        if (isValueTooLarge(data, text)) {
            return EMPTY_TXT;
        }
        out.write((byte) data.length);
        out.write(data, 0, data.length);

        final byte[] encodedText = out.toByteArray();

        return (encodedText.length > 0 ? encodedText : EMPTY_TXT);
    }

    private static boolean isValueTooLarge(final byte[] data, final String text) {
        if (data.length > MAX_VALUE_LENGTH) {
            final Logger logger = LoggerFactory.getLogger(ByteWrangler.class);
            logger.warn("Cannot have individual values larger that 255 chars. Offending value: {}", text);

            return true;
        }
        return false;
    }

}
