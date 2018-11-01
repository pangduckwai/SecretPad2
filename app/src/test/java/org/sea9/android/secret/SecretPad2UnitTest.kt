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
	private fun printResult(result: List<Array<String>>) {
		for (entry in result) {
			if (entry.isNotEmpty()) {
				System.out.println("[KEY] ${entry[0]}")
				System.out.println("[Content]")
				System.out.println(entry[1])
				System.out.print("[TAG] ")
				for (i in 2 until entry.size) {
					if (i > 2) System.out.print(" / ")
					System.out.print(entry[i])
				}
				System.out.println()
			}
			System.out.println()
		}
	}

	@Test
	fun testConvert() {
		val converter = SmartConverter.getInstance()
		val content1 =
				"Key1:\nHello there how are you\nI'm fine thank you\nvery much.\n\nKey2\nYo bro!\nGood to see ya!\n\n" +
				"Key3:\nSo what else?\nI don't know\ncant think of anything"
		val result1 = converter.convert("CAT0", "TTL1", content1)
		assertTrue(result1.size == 4)

		val content2 =
				"Key1:\nHello there how are you\nI'm fine thank you\nvery much.\n\nKey2:\nYo bro!\nGood to see ya!\n\n" +
				"Key3:\nSo what else?\nI don't know\ncant think of anything"
		val result2 = converter.convert("CAT1", "TTL2", content2)
		assertTrue(result2.size == 4)

		val content3 =
				"Key1\nHello there how are you\nI'm fine thank you\nvery much.\n\nKey2\nYo bro!\nGood to see ya!\n\n" +
				"Key3\nSo what else?\nI don't know\ncant think of anything"
		val result3 = converter.convert("CAT2", "TTL3", content3)
		assertTrue(result3.size == 1)

		val content4 =
				"Key1\nHello there how are you\nI'm fine thank you\nvery much.\n\nKey2:\nYo bro!\nGood to see ya!\n\n" +
				"Key3\nSo what else?\nI don't know\ncant think of anything"
		val result4 = converter.convert("CAT3", "TTL4", content4)
		assertTrue(result4.size == 3)

		printResult(result4)
	}
}
