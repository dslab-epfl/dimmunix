package communix;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * <p>
 * This is a class designed to process AES messages sent from the LSL implementation of AES which can be found here:<br/>
 * <a href="https://wiki.secondlife.com/wiki/AES_LSL_Implementation">https://wiki .secondlife.com/wiki/AES_LSL_Implementation</a>
 * </p>
 * <p>
 * This Java class will be updated to support the same modes of operation as the LSL implementation. It currently assumes that keys and input-vectors are processed as hex-strings, and that text is received as plain-text, while ciphertext will be handled as base64 strings.
 * </p>
 * 
 * @author Haravikk
 * @date Sep 15, 2008, 4:18:48 PM
 * @version 1.0
 */
public class LSLAESCrypto {
	/** Our currently set block-cipher mode */
	protected LSLAESCryptoMode mode = LSLAESCryptoMode.CBC;
	/** Used to detect when a new {@link Cipher} Is needed. */
	protected boolean modeChanged = false;

	/** Our currently set padding mode */
	protected LSLAESCryptoPad pad = LSLAESCryptoPad.NONE;
	/** Our currently set pad-size */
	protected int padSize = 512;

	/** The currently loaded key */
	protected SecretKeySpec keySpec = null;
	/** The currently loaded input-vector */
	protected IvParameterSpec ivSpec = null;

	/** The currently active cipher */
	protected Cipher cipher = null;

	/** A random class for secure random operations. */
	protected Random random = new SecureRandom();

	/**
	 * Decrypts a base64 ciphertext into plain-text
	 * 
	 * @param base64ciphertext
	 *            the ciphertext to decrypt
	 * @return the plain-text that was originally encrypted
	 * @throws InvalidKeyException
	 *             if the currently loaded key is not valid
	 * @throws InvalidAlgorithmParameterException
	 *             if the AES algorithm is not supported by the current JVM
	 * @throws IllegalBlockSizeException
	 *             if the ciphertext is somehow unreadable (bad base64 conversion)
	 * @throws BadPaddingException
	 *             if the chosen mode of operation requires padded data
	 */
	public synchronized String decrypt(final String base64ciphertext) {
		try {
			if (this.modeChanged)
				try {
					this.createCipher();
				} catch (final Exception e) { /* Do nothing */
				}

			this.cipher.init(Cipher.DECRYPT_MODE, this.keySpec, this.ivSpec);
			return new String(this.cipher.doFinal(Base64Coder.decode(base64ciphertext)));			
		}
		catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	/**
	 * Encrypts plain-text into a base64 string
	 * 
	 * @param text
	 *            the plain-text to encrypt
	 * @return the base64 ciphertext produced
	 * @throws IllegalBlockSizeException
	 *             if the plain text is somehow invalid
	 * @throws BadPaddingException
	 *             if the chosen mode of operation requires padded data
	 * @throws InvalidKeyException
	 *             if the currently loaded key is invalid
	 * @throws InvalidAlgorithmParameterException
	 *             if the AES algorithm is not supported by the current JVM
	 */
	public synchronized String encrypt(final String text) {
		try {
			if (this.modeChanged)
				try {
					this.createCipher();
				} catch (final Exception e) { /* Do nothing */
				}

			this.cipher.init(Cipher.ENCRYPT_MODE, this.keySpec, this.ivSpec);

			byte[] data = text.getBytes();
			int bits = data.length * 8;

			/* Apply padding */
			LSLAESCryptoPad padding = this.pad;
			if (padding == LSLAESCryptoPad.NONE) {
				if (this.mode == LSLAESCryptoMode.CFB) {
					return Base64Coder.encodeString(this.cipher.doFinal(data), 0, bits);
				}
				padding = LSLAESCryptoPad.RBT;
			}

			int blockSize = this.padSize;
			if (padding == LSLAESCryptoPad.RBT)
				blockSize = 128;

			final int blocks = bits / blockSize;
			int extra = bits % blockSize;

			if (padding == LSLAESCryptoPad.RBT) {
				if (extra > 0) {
					/*
					 * This scheme takes the last encrypted block, encrypts it again, and XORs it with any leftover data, maintaining data-length. If input is less than a block in size, then the current input-vector is used.
					 */
					int bytes = extra / 8;
					if ((bytes * 8) < extra)
						++bytes;

					// Grab leftover bytes
					final byte[] t = new byte[bytes];
					if (bytes > 0)
						System.arraycopy(data, data.length - bytes, t, 0, bytes);

					// Encrypt all other data.
					byte[] lb;
					if (blocks < 1) {
						// If not enough for a block, double-encrypt IV.
						data = new byte[0];
						lb = this.cipher.doFinal(this.cipher.doFinal(this.ivSpec.getIV()));
					} else {
						// If there are blocks, then double-encrypt final one.
						data = this.cipher.doFinal(data, 0, data.length - bytes);
						lb = this.cipher.doFinal(data, data.length - 16, 16);
					}

					// XOR lb with t.
					for (int i = 0; i < t.length; ++i)
						t[i] ^= lb[i];

					lb = new byte[data.length + t.length];
					System.arraycopy(data, 0, lb, 0, data.length);
					System.arraycopy(t, 0, lb, data.length, t.length);

					return Base64Coder.encodeString(lb);
				}
				return Base64Coder.encodeString(this.cipher.doFinal(data), 0, bits);
			}

			// Padding schemes that add bytes until block-boundary is reached.
			extra = blockSize - extra;

			if (padding == LSLAESCryptoPad.NULLS_SAFE) {
				++bits;
				final int bytes = bits / 8;
				final int bit = bytes % 8;

				if (bytes < data.length)
					data[bytes] |= (1 << (8 - bit));
				else {
					final byte[] t = new byte[data.length + 1];
					System.arraycopy(data, 0, t, 0, data.length);
					t[data.length] = (byte) 0x80;
					data = t;
				}

				if ((--extra) < 0)
					extra += blockSize;
				padding = LSLAESCryptoPad.NULLS;
			}

			int bytes = extra / 8;
			if (bytes <= 0) {
				if (padding == LSLAESCryptoPad.NULLS)
					return Base64Coder.encodeString(this.cipher.doFinal(data), 0, bits);

				bytes = blockSize / 8;
				extra += blockSize;
			}

			bits += extra;
			final byte[] t = new byte[data.length + bytes];
			int i = data.length;
			System.arraycopy(data, 0, t, 0, data.length);
			data = t;

			for (; i < data.length; ++i) {
				byte b = 0;
				if ((i >= (data.length - 4)) && (padding != LSLAESCryptoPad.NULLS))
					b = (byte) bytes;
				else if (padding == LSLAESCryptoPad.RANDOM)
					b = (byte) this.random.nextInt(256);

				data[i] = b;
			}

			return Base64Coder.encodeString(this.cipher.doFinal(data), 0, bits);			
		}
		catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	/**
	 * Initialises this AES instance with a mode, pad, key, and input vector in a single operation
	 * 
	 * @param mode
	 *            the cipher-block mode of operation
	 * @param pad
	 *            the padding scheme to use
	 * @param padSize
	 *            the block-size to use when padding. Must be a non-zero, positive value that is a multiple of 128.
	 * @param hexKey
	 *            the key to use as a hexadecimal string
	 * @param hexIV
	 *            the input-vector to use as a hexadecimal string
	 * @throws NoSuchAlgorithmException
	 *             if the AES algorithm is not supported by the current JVM
	 * @throws NoSuchPaddingException
	 *             if the padding method is not supported by the current JVM
	 */
	public void init(final LSLAESCryptoMode mode, final LSLAESCryptoPad pad, final int padSize, final String hexKey, final String hexIV) throws NoSuchAlgorithmException, NoSuchPaddingException {
		if ((mode == null) || (pad == null) || (hexKey == null) || (hexIV == null))
			throw new IllegalArgumentException("No arguments may be null");

		this.setMode(mode);
		this.setPad(pad, padSize);
		this.setKey(hexKey);
		this.setInputVector(hexIV);

		this.random.nextInt();

		this.createCipher();
	}

	/**
	 * Sets the input-vector for this engine to use
	 * 
	 * @param hexIV
	 *            a hexadecimal input-vector to use
	 */
	public void setInputVector(final String hexIV) {
		if (hexIV == null)
			throw new IllegalArgumentException("Input-vector may not be null!");

		this.ivSpec = new IvParameterSpec(HexCoder.hexToBytes(hexIV));
	}

	/**
	 * Sets the key for this engine to use
	 * 
	 * @param hexKey
	 *            a hexadecimal key to use
	 */
	public void setKey(final String hexKey) {
		if (hexKey == null)
			throw new IllegalArgumentException("Key may not be null!");

		this.keySpec = new SecretKeySpec(HexCoder.hexToBytes(hexKey), "AES");
	}

	/**
	 * Sets the mode of this implementation
	 * 
	 * @param mode
	 *            the mode to set
	 */
	public void setMode(final LSLAESCryptoMode mode) {
		if (mode == null)
			throw new IllegalArgumentException("Mode may not be null!");

		this.mode = mode;
		this.modeChanged = true;
	}

	/**
	 * Sets the padding scheme of this implementation
	 * 
	 * @param pad
	 *            the padding scheme to use
	 */
	public void setPad(final LSLAESCryptoPad pad) {
		this.setPad(pad, this.padSize);
	}

	/**
	 * Sets the padding scheme of this implementation
	 * 
	 * @param pad
	 *            the padding scheme to use
	 * @param padSize
	 *            the block-size to use when padding. Must be a non-zero, positive value that is a multiple of 128.
	 */
	public void setPad(final LSLAESCryptoPad pad, final int padSize) {
		if (pad == null)
			throw new IllegalArgumentException("Pad may not be null!");
		if ((padSize <= 0) || ((padSize % 128) > 0))
			throw new IllegalArgumentException("Pad size may not be less than zero, and must be a multiple of 128");

		this.pad = pad;
		this.padSize = padSize;
	}

	/**
	 * Creates a new cipher instance for processing
	 * 
	 * @throws NoSuchPaddingException
	 *             if the padding scheme set is invalid
	 * @throws NoSuchAlgorithmException
	 *             if AES is not supported by this JVM
	 */
	protected void createCipher() throws NoSuchAlgorithmException, NoSuchPaddingException {
		this.cipher = Cipher.getInstance("AES/" + this.mode + "/NoPadding");
	}

	/** Defines modes of operation combatible with LSL */
	public enum LSLAESCryptoMode {
		/** Cipher-Block-Chaining mode */
		CBC,
		/** Cipher FeedBack mode */
		CFB;
	}

	/** Defines padding schemes compatible with LSL */
	public enum LSLAESCryptoPad {
		/** Performs no padding, will switch to RBT if mode is CBC. */
		NONE,
		/**
		 * Enables CFB mode temporarily for the final complete block, and combines with data. This preserves data-length.
		 */
		RBT,
		/**
		 * Adds null-bytes to the end of the data until it is of correct-size. This is an padding scheme (may result in loss of null-bytes from original data).
		 */
		NULLS,
		/**
		 * Same as NULLS, except that it first appends a single '1' bit to the data before padding.
		 */
		NULLS_SAFE,
		/**
		 * Appends null-bytes to the data until one word from block-size, final word is then populated with bytes describing the number of padding bytes added.
		 */
		ZEROES,
		/**
		 * Same as ZEROES, except that random-bytes are used in place of null-bytes.
		 */
		RANDOM;
	}

	private LSLAESCrypto() {
		final String myKey = "1234567890ABCDEF0123456789ABCDEF";
		final String myIV = "89ABCDEF0123456789ABCDEF01234567";

		try {
			this.init(LSLAESCryptoMode.CFB, LSLAESCryptoPad.NONE, 128, myKey, myIV);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		}
	}

	public static final LSLAESCrypto instance = new LSLAESCrypto();
}