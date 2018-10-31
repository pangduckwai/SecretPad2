package org.sea9.android.secret

import org.junit.Test

import org.junit.Assert.*
import org.sea9.android.secret.compat.SmartConverter

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
		val tags = arrayListOf("AaA", "BBb", "cCC")
		val converter = SmartConverter.getInstance(tags)
		val content =
				"AAA\n" +
				"How are you\n" +
				"I'm fine thanks\n\n" +
				"CCC\n" +
				"Yo Bro!!!\n" +
				"  \n" +
				"DDD\n" +
				"XXXX:YYYY\n\n" +
				"AAABBB\n" +
				"Walawalawal Yoyoyo\n" +
				"Cool man!\n"
		val result = converter.convert("CAT0", "TTL1", content)
		// TEMP >>>>>>>>>>>>>>>
		for (entry in result.entries) {
			System.out.println("[KEY] ${entry.key}")
			if (entry.value.size > 0) {
				System.out.println("[Content]")
				System.out.println(entry.value[0])
				System.out.print("[TAG] ")
				for (i in 1 until entry.value.size) {
					if (i > 1) System.out.print(" / ")
					System.out.print(entry.value[i])
				}
				System.out.println()
			}
			System.out.println()
		}
		// TEMP <<<<<<<<<<<<<<<
		assertTrue((result.size == 4) &&
				(result["TTL1"]?.size == 2) &&
				(result["TTL1 AAA"]?.size == 3) &&
				(result["TTL1 CCC"]?.size == 3) &&
				(result["TTL1 AAABBB"]?.size == 4)
		)
	}
}
