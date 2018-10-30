package org.sea9.android.secret

import org.junit.Test

import org.junit.Assert.*
import org.sea9.android.secret.compat.SmartConverter
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
	fun testConverter() {
		val tags: List<String> = arrayListOf("AAA", "BBB", "CCC")
		val converter: SmartConverter = SmartConverter.getInstance(tags)
		val content =
				"AAA\n" +
				"How are you\n" +
				"I'm fine thanks\n\n" +
				"BBB\n" +
				"Yo Bro!!!"
		val result = converter.convert("CAT0", "TTL1", content)
		assertNull(result)
	}
}
