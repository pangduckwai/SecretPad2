package org.sea9.android.secret.temp

import org.sea9.android.secret.DataRecord

class TempData {
	companion object {
		fun tags(): List<String> {
			return arrayListOf(
					  "0acct"
					, "1bank"
					, "2certs"
					, "3email"
					, "4eTax"
					, "5IT"
					, "6NIC"
					, "7work"
			)
		}

		fun data(): List<DataRecord> {
			return arrayListOf(
					  DataRecord("1000", "56000", arrayListOf(1, 2, 3))
					, DataRecord("1001", "56001", arrayListOf(2, 3, 4))
					, DataRecord("1002", "56002", arrayListOf(3, 4, 5))
					, DataRecord("1003", "56003", arrayListOf(4, 5, 6))
					, DataRecord("1004", "56004", arrayListOf(5, 6, 7))
					, DataRecord("1005", "56005", arrayListOf(6, 7, 0))
					, DataRecord("1006", "56006", arrayListOf(7, 0, 1))
					, DataRecord("1007", "56007", arrayListOf(0, 1, 2))
					, DataRecord("1008", "56008", arrayListOf(1, 2, 3))
					, DataRecord("1009", "56009", arrayListOf(2, 3, 4))
					, DataRecord("1010", "56010", arrayListOf(3, 4, 5))
					, DataRecord("1011", "56011", arrayListOf(4, 5, 6))
					, DataRecord("1012", "56012", arrayListOf(5, 6, 7))
					, DataRecord("1013", "56013", arrayListOf(6, 7, 0))
					, DataRecord("1014", "56014", arrayListOf(7, 0, 1))
					, DataRecord("1015", "56015", arrayListOf(0, 1, 2))
					, DataRecord("1016", "56016", arrayListOf(1, 2, 3))
					, DataRecord("1017", "56017", arrayListOf(2, 3, 4))
					, DataRecord("1018", "56018", arrayListOf(3, 4, 5))
					, DataRecord("1019", "56019", arrayListOf(4, 5, 6))
					, DataRecord("1020", "56020", arrayListOf(5, 6, 7))
					, DataRecord("1021", "56021", arrayListOf(6, 7, 0))
					, DataRecord("1022", "56022", arrayListOf(7, 0, 1))
					, DataRecord("1023", "56023", arrayListOf(0, 1, 2))
			)
		}
	}
}