package org.sea9.android.secret.compat;

import org.sea9.android.secret.crypto.CryptoUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

/**
 * Only used in importing old data files.
 */
public class CompatCryptoUtils {
	private static final int DEFAULT_ITERATION = 2048;
	private static final int DEFAULT_ITERATION_OLD = 255;

	/**
	 * @param msg message to be decrypted
	 * @param passwd password the encryption key is derived from.
	 * @param salt salt used in the encryption
	 * @return the decrypted message.
	 */
	public static char[] decrypt(char[] msg, char[] passwd, byte[] salt) {
		try {
			return _decrypt(msg, passwd, salt, DEFAULT_ITERATION);
		} catch (Exception e0) {
			try {
				return _decrypt(msg, passwd, salt, DEFAULT_ITERATION_OLD);
			} catch (Exception e1) {
				throw new RuntimeException(e1);
			}
		}
	}

	private static char[] _decrypt(char[] msg, char[] passwd, byte[] salt, int iteration) throws BadPaddingException {
		return CryptoUtils.convert(
				CryptoUtils.doCipher(
						CryptoUtils.decode(CryptoUtils.convert(msg))
						, new PBEKeySpec(passwd)
						, new PBEParameterSpec(salt, iteration)
						, false));
	}
}
