package javax.jmdns.impl.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;


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
     *
     * FIXME: Should this be exported as a method since it could change externally???
     */
    public final static byte[] EMPTY_TXT = new byte[] { 0 };

    /**
     * Write a UTF string with a length to a stream.
     */
    public static void writeUTF(final OutputStream out, final String str) throws IOException {
        final int len = str.length();
        for (int i = 0; i < len; i++) {
            final int c = str.charAt(i);
            if ((c >= 0x0001) && (c <= 0x007F)) {
                out.write(c);
            } else {
                if (c > 0x07FF) {
                    out.write(0xE0 | ((c >> 12) & 0x0F));
                    out.write(0x80 | ((c >> 6) & 0x3F));
                    out.write(0x80 | ((c >> 0) & 0x3F));
                } else {
                    out.write(0xC0 | ((c >> 6) & 0x1F));
                    out.write(0x80 | ((c >> 0) & 0x3F));
                }
            }
        }
    }

    /**
     * Read data bytes as UTF-8 to a String.
     */
    public static String readUTF(final byte data[]) {
        return readUTF(data, 0, data.length);
    }

    /**
     * Read data bytes as UTF-8 to a String.
     */
    public static String readUTF(final byte data[], final int off, final int len) {
        int offset = off;
        final StringBuilder sb = new StringBuilder();
        for (final int end = offset + len; offset < end;) {
            int ch = data[offset++] & 0xFF;
            switch (ch >> 4) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                    // 0xxxxxxx
                    break;
                case 12:
                case 13:
                    if (offset >= len) {
                        return null;
                    }
                    // 110x xxxx 10xx xxxx
                    ch = ((ch & 0x1F) << 6) | (data[offset++] & 0x3F);
                    break;
                case 14:
                    if (offset + 2 >= len) {
                        return null;
                    }
                    // 1110 xxxx 10xx xxxx 10xx xxxx
                    ch = ((ch & 0x0f) << 12) | ((data[offset++] & 0x3F) << 6) | (data[offset++] & 0x3F);
                    break;
                default:
                    if (offset + 1 >= len) {
                        return null;
                    }
                    // 10xx xxxx, 1111 xxxx
                    ch = ((ch & 0x3F) << 4) | (data[offset++] & 0x0f);
                    break;
            }
            sb.append((char) ch);
        }
        return sb.toString();
    }

    public static void readProperties(final Map<String, byte[]> properties, final byte[] textBytes) throws Exception {
        if (textBytes != null) {
            int off = 0;
            while (off < textBytes.length) {
                // length of the next key value pair
                final int len = textBytes[off++] & 0xFF;

                // error case
                if (
                        (len == 0) ||                       // no date
                        (off + len > textBytes.length)      // length of data would exceed array bounds
                ) {
                    properties.clear();
                    break;
                }
                // look for the '='
                int i = 0;
                for (; (i < len) && (textBytes[off + i] != '='); i++) {
                    /* Stub */
                }

                // get the property name
                final String name = readUTF(textBytes, off, i);
                if (name == null) {
                    properties.clear();
                    break;
                }
                if (i == len) {
                    properties.put(name, NO_VALUE);
                } else {
                    final byte value[] = new byte[len - ++i];
                    System.arraycopy(textBytes, off + i, value, 0, len - i);
                    properties.put(name, value);
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
                    byte data[] = out2.toByteArray();
                    if (data.length > MAX_VALUE_LENGTH) {
                        throw new IOException("Cannot have individual values larger that 255 chars. Offending value: " + key + (val != null ? "" : "=" + val));
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
        final byte data[] = out2.toByteArray();
        if (data.length > MAX_VALUE_LENGTH) {
            throw new IOException("Cannot have individual values larger that 255 chars. Offending value: " + text);
        }
        out.write((byte) data.length);
        out.write(data, 0, data.length);

        final byte[] encodedText = out.toByteArray();

        return (encodedText.length > 0 ? encodedText : EMPTY_TXT);
    }

}
