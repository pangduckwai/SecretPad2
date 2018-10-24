package org.sea9.android.secret

import org.junit.Test

import org.junit.Assert.*
import org.sea9.android.secret.crypto.CryptoUtils

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class SecretPad2UnitTest {
	@Test
	fun addition_isCorrect() {
		assertEquals(4, 2 + 2)
	}

	@Test
	fun testHash() {
		val hash = CryptoUtils.convert(CryptoUtils.encode(CryptoUtils.hash(CryptoUtils.convert("abcd1234".toCharArray()))))
		System.out.println("'$hash'")
		assertTrue(hash.isNotEmpty())
	}
}
