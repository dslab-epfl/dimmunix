package communix;

public class Base64Coder {
	/** Mapping table from 6-bits to Base64 characters. */
	private static char[] BITS_TO_BASE64_CHAR =
		           { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K',
			'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
			'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k',
			'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x',
			'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+',
			'/' };
 
	/** Mapping table from Base64 characters to 6-bits. */
	private static byte[] BASE64_CHAR_TO_BITS =
		          { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
			-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
			-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1, -1, -1, 63,
			52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -1, -1, -1, -1,
			0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,
			19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1, -1, 26, 27, 28, 29,
			30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46,
			47, 48, 49, 50, 51, -1, -1, -1, -1, -1, };
 
	/**
	 * Decodes a byte array from Base64 format. No blanks or line breaks are
	 * allowed within the Base64 encoded data.
	 * 
	 * @param in
	 *            a character array containing the Base64 encoded data.
	 * @return An array containing the decoded data bytes.
	 * @throws IllegalArgumentException
	 *             if the input is not valid Base64 encoded data.
	 */
	public static byte[] decode(final char[] in)
		throws IllegalArgumentException {
		int len = in.length;
		if (len % 4 != 0)
			throw new IllegalArgumentException(
				"Length of Base64 encoded input string is not a multiple of 4.");
 
		// Ignore trailing equals
		while (len > 0 && in[len - 1] == '=')
			--len;
 
		final byte[] bytes = new byte[(len * 3) / 4];
		int o = 0;
 
		for (int i = 0; i < len;) {
			try {
				final char c0 = in[i++];
				final char c1 = in[i++];
				final char c2 = (i < len) ? in[i++] : 'A';
				final char c3 = (i < len) ? in[i++] : 'A';
 
				if (c0 > 127 || c1 > 127 || c2 > 127 || c3 > 127)
					throw new IllegalArgumentException(
						"Invalid base64 character");
 
				final byte b0 = Base64Coder.BASE64_CHAR_TO_BITS[c0];
				final byte b1 = Base64Coder.BASE64_CHAR_TO_BITS[c1];
				final byte b2 = Base64Coder.BASE64_CHAR_TO_BITS[c2];
				final byte b3 = Base64Coder.BASE64_CHAR_TO_BITS[c3];
 
				if (b0 < 0 || b1 < 0 || b2 < 0 || b3 < 0)
					throw new IllegalArgumentException(
						"Invalid base64 character");
 
				bytes[o++] = (byte) ((b0 << 2) | (b1 >>> 4));
				if (o < bytes.length) {
					bytes[o++] = (byte) (((b1 & 0xF) << 4) | (b2 >>> 2));
					if (o < bytes.length)
						bytes[o++] = (byte) (((b2 & 0x3) << 6) | b3);
				}
			} catch (final ArrayIndexOutOfBoundsException e) {
				throw new IllegalArgumentException("Invalid base64 character");
			}
		}
 
		return bytes;
	}
 
	/**
	 * Decodes a byte array from Base64 format.
	 * 
	 * @param s
	 *            a Base64 String to be decoded.
	 * @return An array containing the decoded data bytes.
	 * @throws IllegalArgumentException
	 *             if the input is not valid Base64 encoded data.
	 */
	public static byte[] decode(final String s) {
		return Base64Coder.decode(s.toCharArray());
	}
 
	/**
	 * Decodes a string from Base64 format.
	 * 
	 * @param s
	 *            a Base64 String to be decoded.
	 * @return A String containing the decoded data.
	 * @throws IllegalArgumentException
	 *             if the input is not valid Base64 encoded data.
	 */
	public static String decodeString(final String s) {
		return new String(Base64Coder.decode(s));
	}
 
	/**
	 * Encodes a byte array into Base64 format. No blanks or line breaks are
	 * inserted.
	 * 
	 * @param in
	 *            an array containing the data bytes to be encoded.
	 * @return A character array with the Base64 encoded data.
	 */
	public static char[] encode(final byte[] in) {
		return Base64Coder.encode(in, 0, in.length);
	}
 
	/**
	 * Encodes a byte array into Base64 format. No blanks or line breaks are
	 * inserted.
	 * 
	 * @param in
	 *            an array containing the data bytes to be encoded.
	 * @param offset
	 *            the offset into the array at which to begin reading.
	 * @param bits
	 *            number of <b>bits</b> to process from <code>in</code>.
	 * @return A character array with the Base64 encoded data.
	 */
	public static char[] encode(
		final byte[] in,
		final int offset,
		final int bits) {
		int length = bits / 8;
		if ((length * 8) < bits) ++length;
 
		final char[] chars = new char[((length + 2) / 3) * 4];
		final int out = ((length * 4) + 2) / 3;
 
		int mask = ~(-1 << (8 - (bits % 8))) | ~(-1 << (bits % 8));
		if (mask == 0) mask = 0xFF;
 
		int o = 0;
		final int end = length + offset;
		for (int i = offset; i < end;) {
			final int b0 = ((i + 1) == end) ? in[i++] & mask : in[i++] & 0xFF;
			final int b1 =
				(i < length) ? (((i + 1) == end) ? in[i++] & mask
					: in[i++] & 0xFF) : 0;
			final int b2 =
				(i < length) ? (((i + 1) == end) ? in[i++] & mask
					: in[i++] & 0xFF) : 0;
 
			final int i0 = (b0 >>> 2);
			final int i1 = (((b0 & 0x3) << 4) | (b1 >>> 4));
			final int i2 = (((b1 & 0xF) << 2) | (b2 >>> 6));
			final int i3 = b2 & 0x3F;
 
			chars[o++] = Base64Coder.BITS_TO_BASE64_CHAR[i0];
			chars[o++] = Base64Coder.BITS_TO_BASE64_CHAR[i1];
			chars[o] = (o < out) ? Base64Coder.BITS_TO_BASE64_CHAR[i2] : '=';
			++o;
			chars[o] = (o < out) ? Base64Coder.BITS_TO_BASE64_CHAR[i3] : '=';
			++o;
		}
 
		return chars;
	}
 
	/**
	 * Produces a base64 string from the provided byte-array.
	 * 
	 * @param bytes
	 *            the byte-array to read-from.
	 * @return the base64 encoded string produced.
	 */
	public static String encodeString(final byte[] bytes) {
		return Base64Coder.encodeString(bytes, 0, bytes.length);
	}
 
	/**
	 * Produces a base64 string from the provided byte-array slice.
	 * 
	 * @param bytes
	 *            the byte-array to read-from.
	 * @param offset
	 *            the offset into the array at which to begin reading.
	 * @param bits
	 *            number of <b>bits</b> to process from <code>bytes</code>.
	 * @return the base64 encoded string produced.
	 */
	public static String encodeString(
		final byte[] bytes,
		final int offset,
		final int bits) {
		return new String(Base64Coder.encode(bytes, offset, bits));
	}
 
	/**
	 * Encodes a string into Base64 format. No blanks or line breaks are
	 * inserted.
	 * 
	 * @param s
	 *            a String to be encoded.
	 * @return A String with the Base64 encoded data.
	 */
	public static String encodeString(final String s) {
		return new String(Base64Coder.encode(s.getBytes()));
	}
 
	/** Dummy constructor. */
	private Base64Coder() { /* Blocking constructor */}
 
}