package util;

import java.nio.charset.Charset;

import java.util.Random;
import java.util.UUID;

import static org.junit.Assert.*;

public class StringUtil {
    
    private static final String ASCII_CHARACTERS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final String NON_ASCII_CHARACTERS = "áàâäéèêíìîóòôößúùûüÁÀÂÄÉÈÊÍÌÎÓÒÔÖÚÙÛÜ";
    private static final String UNICODE_CHARACTERS;
    static {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            final char c = (char) (0x800 + i);
            sb.append(c);
        }
        for (int i = 0; i < 24; i++) {
            final char c = (char) (0x840 + i);
            sb.append(c);
        }
        for (int i = 0; i < 16; i++) {
            final char c = (char) (0x8A0 + i);
            sb.append(c);
        }
        UNICODE_CHARACTERS = sb.toString();
    }

    public static final Charset UTF_8 = Charset.forName("UTF-8");

    public static String randomAsciiString(final int length) {
        final StringBuilder sb = new StringBuilder();
        while (sb.length() < length) {
            final UUID uuid = UUID.randomUUID();
            sb.append(uuid.toString());
        }

        final String randomString = sb.substring(0, length);

        return randomString;
    }

    /**
     * Gets a random character out of the given String.
     * 
     * @param str String to get character from
     * @param random generator for random index within String
     * @return
     */
    public static char randomCharacter(final String str, final Random random) {
        // ensures that index is in bounds of the string
        final int index = random.nextInt( str.length() );

        return str.charAt(index);
    }

    /**
     * Gets a random character out of an internal set of Strings.
     * 
     * Note:
     * These Strings have only a limited set of characters but these are from various parts of the Unicode space.
     * 
     * @param random generator used for character retrieval
     * @return
     */
    public static char randomCharacter(final Random random) {
        final int i = random.nextInt(3);
        switch (i) {
            case 1:
                return randomCharacter(NON_ASCII_CHARACTERS, random);
            case 2:
                return randomCharacter(UNICODE_CHARACTERS, random);
            default:
                return randomCharacter(ASCII_CHARACTERS, random);
        }
    }

    public static String randomString(final int length) {
        final Random rnd = new Random(length*length);

        final String randomString = randomString(rnd, length);

        return randomString;
    }

    public static String randomString(final Random rnd, final int length) {
        final StringBuilder sb = new StringBuilder();
        while (sb.length() < length) {
            final char c = randomCharacter(rnd);
            sb.append(c);
        }

        final String randomString = sb.substring(0, length);

        return randomString;
    }

    /**
     * Create a String containing some Unicode characters.
     * 
     * When creating an byte array out of the String it will have a size of {@code length}. 
     * 
     * @param length maximum size of resulting byte array
     * @return
     */
    public static String maxSizeRandomString(final int length) {
        String str = randomString(length);

        byte[] bytes = str.getBytes(UTF_8);

        // as we have Unicode characters there will be more bytes than length
        while(length < bytes.length) {
            str = str.substring(0, str.length() - 1);
            bytes = str.getBytes(UTF_8);
        }

        // since we we might have removed a Unicode character 
        // we might now be some bytes of in the wrong direction
        while(bytes.length < length) {
            str = str + 'X';
            bytes = str.getBytes(UTF_8);
        }

        return str;
    }

    /**
     * Creates a byte array from a random String containing some Unicode characters.
     * 
     * The resulting byte array will have a size of {@code length} +1 since byte[0] contains the length. 
     * 
     * @param length maximum size of resulting byte array
     * @return
     */
    public static byte[] randomStringBytes(final int length) {
        final String str = maxSizeRandomString(length);

        final byte[] bytes = str.getBytes(UTF_8);
        assertEquals("Create bytes array", length, bytes.length);

        final byte[] result = new byte[bytes.length + 1];
        result[0] = (byte) (bytes.length & 0xFF);
        System.arraycopy(bytes, 0, result, 1, bytes.length);

        return result;
    }

}
