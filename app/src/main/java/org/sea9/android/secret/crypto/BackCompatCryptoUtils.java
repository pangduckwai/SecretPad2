package org.sea9.android.secret.crypto;

import javax.crypto.BadPaddingException;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

/**
 * Only used in importing old data files.
 */
public class BackCompatCryptoUtils {
	public static final int DEFAULT_ITERATION_OLD = 255;

	/**
	 * @param msg message to be decrypted
	 * @param passwd password the encryption key is derived from.
	 * @param salt salt used in the encryption
	 * @return the decrypted message.
	 * @throws BadPaddingException if decryption failed.
	 */
	public static char[] decrypt(char[] msg, char[] passwd, byte[] salt) throws BadPaddingException {
		return CryptoUtils.convert(
				CryptoUtils.doCipher(
						CryptoUtils.decode(CryptoUtils.convert(msg))
						, new PBEKeySpec(passwd)
						, new PBEParameterSpec(salt, DEFAULT_ITERATION_OLD)
						, false));
	}
}
