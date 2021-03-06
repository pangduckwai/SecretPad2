package org.sea9.android.secret

import org.junit.Test

import org.junit.Assert.*
import org.sea9.android.secret.compat.SmartConverter
import org.sea9.android.secret.data.NoteRecord
import java.util.regex.Pattern

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

	@Test
	fun testSequence() {
		val notes = listOf(
				NoteRecord(3, "Key3", "Hello 3", null, "xxx yyy", 0),
				NoteRecord(1, "Key1", "Hello 1", null, "xxx yyy", 0),
				NoteRecord(4, "Key4", "Hello 4", null, "xxx yyy", 0),
				NoteRecord(7, "Key7", "Hello 7", null, "xxx yyy", 0),
				NoteRecord(2, "Key2", "Hello 2", null, "xxx yyy", 0),
				NoteRecord(6, "Key6", "Hello 6", null, "xxx yyy", 0),
				NoteRecord(5, "Key5", "Hello 5", null, "xxx yyy", 0))
		val idx = notes.asSequence()
				.indexOfFirst {
					it.pid == 2L
				}
		System.out.println("Index found is $idx")
		assertTrue(idx == 4)
	}

	@Test
	fun testListEqual() {
		val l1 = listOf(3L, 1L, 4L)
		val l2 = listOf(3L, 1L, 4L)
		val l3 = listOf(4L, 1L, 3L)
		assertTrue(l1.equals(l2))
		assertTrue(!l1.equals(l3))
	}

	@Test
	fun testRegex() {
		val pattern = Pattern.compile("(?=.{8,})(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*_=,.></?~-]).*")
		assertTrue(pattern.matcher("Abcd12#4").matches())
		assertTrue(pattern.matcher("ABcD!2#$").matches())
		assertTrue(pattern.matcher("Abcd12#45678").matches())
		assertFalse(pattern.matcher("ABCD12#4").matches())
		assertFalse(pattern.matcher("abcd12#4").matches())
		assertFalse(pattern.matcher("AbcdEf#h").matches())
		assertFalse(pattern.matcher("abcdefgh").matches())
		assertFalse(pattern.matcher("12345678").matches())
		assertFalse(pattern.matcher("ABCDEFGH").matches())
		assertFalse(pattern.matcher("Abcd12#").matches())
	}
}
