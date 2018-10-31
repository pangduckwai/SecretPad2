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

	private fun printResult(result: Map<String, List<String>>) {
		for (entry in result.entries) {
			System.out.println("[KEY] ${entry.key}")
			if (entry.value.isNotEmpty()) {
				System.out.println("[Content]")
				System.out.println(entry.value[1])
				System.out.print("[TAG] ")
				for (i in 2 until entry.value.size) {
					if (i > 2) System.out.print(" / ")
					System.out.print(entry.value[i])
				}
				System.out.println()
			}
			System.out.println()
		}
	}

	@Test
	fun testConverterX() {
		val tags = listOf("AaA", "BBb", "cCC")
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
//		printResult(result) // TEMP
		assertTrue(result.size == 4)
		assertTrue(result["TTL1"]?.size == 3)
		assertTrue(result["TTL1 AAA"]?.size == 4)
		assertTrue(result["TTL1 CCC"]?.size == 4)
		assertTrue(result["TTL1 AAABBB"]?.size == 5)
	}

	@Test
	fun testConverterY() {
		val tags = listOf("AaA", "BBb", "cCC")
		val converter = SmartConverter.getInstance(tags)
		val content =
				"AAA\n" +
				"How are you\n" +
				"I'm fine thanks\n\n" +
				"CCC\n" +
				"Yo Bro!!!\n" +
				"  \n" +
				"AAABBB\n" +
				"Walawalawal Yoyoyo\n" +
				"Cool man!\n"
		val result = converter.convert("CAT0", "TTL1", content)
//		printResult(result) // TEMP
		assertTrue(result.size == 3)
		assertTrue(result["TTL1 AAA"]?.size == 4)
		assertTrue(result["TTL1 CCC"]?.size == 4)
		assertTrue(result["TTL1 AAABBB"]?.size == 5)
	}

	@Test
	fun testConverterZ() {
		val tags = listOf("AaA", "BBb", "cCC")
		val converter = SmartConverter.getInstance(tags)
		val content =
				"AAE\n" +
				"How are you\n" +
				"I'm fine thanks\n\n" +
				"CCC\n" +
				"Yo Bro!!!\n" +
				"  \n" +
						"BBB\n" +
						"YOYOYO\n\n"
				"AABBCC\n" +
				"Walawalawal Yoyoyo\n" +
				"Cool man!\n"
		val result = converter.convert("CAT0", "TTL1", content)
//		printResult(result) // TEMP
		assertTrue(result.size == 2)
	}

	@Test
	fun testConverter13() {
		val tags = listOf("AaA", "BBb", "cCC")
		val converter = SmartConverter.getInstance(tags)
		val content =
				"AAE\n" +
				"How are you\n" +
				"I'm fine thanks\n\n" +
				"CCC\n" +
				"Yo Bro!!!\n" +
				"  \n" +
				"BBB Hey man\n\n" +
				"AABBCC\n" +
				"Walawalawal Yoyoyo\n" +
				"Cool man!\n"
		val result = converter.convert("CAT0", "TTL1", content)
		printResult(result) // TEMP
		assertTrue(result.size == 3)
	}
}
