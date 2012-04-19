package communix;
import java.io.UnsupportedEncodingException;

import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.SecretKeySpec;

public class AES {
	private Cipher ecipher;
	private Cipher dcipher;
	
	private SecretKey key;
	private final byte[] testKeyBytes; 
	
	public static final AES instance = new AES();
	
	private AES() {
		this.testKeyBytes = new byte[32];
		for (byte i = 0; i < this.testKeyBytes.length; i++) {
			this.testKeyBytes[i] = i;
		}
		
		try {
			SecretKeySpec keySpec = new SecretKeySpec(this.testKeyBytes, "AES");
			this.key = SecretKeyFactory.getInstance("AES").generateSecret(keySpec);
			
			ecipher = Cipher.getInstance("AES");
			dcipher = Cipher.getInstance("AES");
			ecipher.init(Cipher.ENCRYPT_MODE, this.key);
			dcipher.init(Cipher.DECRYPT_MODE, this.key);
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public String encrypt(String str) {
		try {
			byte[] utf8 = str.getBytes("UTF8");
			byte[] enc = ecipher.doFinal(utf8);
			return new sun.misc.BASE64Encoder().encode(enc);
		} catch (javax.crypto.BadPaddingException e) {
		} catch (IllegalBlockSizeException e) {
		} catch (UnsupportedEncodingException e) {
		} 
		return null;
	}

	public String decrypt(String str) {
		try {
			byte[] dec = new sun.misc.BASE64Decoder().decodeBuffer(str);
			byte[] utf8 = dcipher.doFinal(dec);
			return new String(utf8, "UTF8");
		} catch (javax.crypto.BadPaddingException e) {
		} catch (IllegalBlockSizeException e) {
		} catch (UnsupportedEncodingException e) {
		} catch (java.io.IOException e) {
		}
		return null;
	}	
}
