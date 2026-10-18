package com.quran.labs.androidquran.ui.modern

/**
 * Metadata representation of a Quran Juz (Para) for filtering, revision batching,
 * and Hifz mastery tracking (Easy Quran Memorizer Pro parity).
 */
data class JuzData(
  val number: Int,
  val nameArabic: String,
  val nameEnglish: String,
  val surahs: List<Int>,
  val description: String = ""
)

object JuzConstants {
  val ALL_JUZ: List<JuzData> = listOf(
    JuzData(1, "الم", "Alif Lam Meem", listOf(1, 2), "Al-Fatihah 1 - Al-Baqarah 141"),
    JuzData(2, "سيقول", "Sayaqool", listOf(2), "Al-Baqarah 142 - Al-Baqarah 252"),
    JuzData(3, "تلك الرسل", "Tilka ar-Rusul", listOf(2, 3), "Al-Baqarah 253 - Ali 'Imran 92"),
    JuzData(4, "لن تنالوا", "Lan Tanaalu", listOf(3, 4), "Ali 'Imran 93 - An-Nisa 23"),
    JuzData(5, "والمحصنات", "Wal-Muhsanat", listOf(4), "An-Nisa 24 - An-Nisa 147"),
    JuzData(6, "لا يحب الله", "La Yuhibbullah", listOf(4, 5), "An-Nisa 148 - Al-Ma'idah 81"),
    JuzData(7, "وإذا سمعوا", "Wa Iza Sami'oo", listOf(5, 6), "Al-Ma'idah 82 - Al-An'am 110"),
    JuzData(8, "ولو أننا", "Wa Law Annana", listOf(6, 7), "Al-An'am 111 - Al-A'raf 87"),
    JuzData(9, "قال الملأ", "Qalal-Mala'o", listOf(7, 8), "Al-A'raf 88 - Al-Anfal 40"),
    JuzData(10, "واعلموا", "Wa'lamoo", listOf(8, 9), "Al-Anfal 41 - At-Tawbah 92"),
    JuzData(11, "يعتذرون", "Ya'taziroon", listOf(9, 10, 11), "At-Tawbah 93 - Hud 5"),
    JuzData(12, "وما من دابة", "Wa Ma Min Daabbah", listOf(11, 12), "Hud 6 - Yusuf 52"),
    JuzData(13, "وما أبرئ", "Wa Ma Obarri'u", listOf(12, 13, 14), "Yusuf 53 - Ibrahim 52"),
    JuzData(14, "ربما", "Rubama", listOf(15, 16), "Al-Hijr 1 - An-Nahl 128"),
    JuzData(15, "سبحان الذي", "Subhana Allazi", listOf(17, 18), "Al-Isra 1 - Al-Kahf 74"),
    JuzData(16, "قال ألم", "Qala Alam", listOf(18, 19, 20), "Al-Kahf 75 - Ta-Ha 135"),
    JuzData(17, "اقترب", "Iqtaraba", listOf(21, 22), "Al-Anbiya 1 - Al-Hajj 78"),
    JuzData(18, "قد أفلح", "Qad Aflaha", listOf(23, 24, 25), "Al-Mu'minun 1 - Al-Furqan 20"),
    JuzData(19, "وقال الذين", "Wa Qalallazina", listOf(25, 26, 27), "Al-Furqan 21 - An-Naml 55"),
    JuzData(20, "أمن خلق", "Amman Khalaqa", listOf(27, 28, 29), "An-Naml 56 - Al-'Ankabut 45"),
    JuzData(21, "اتل ما أوحي", "Utlu Ma Oohiya", listOf(29, 30, 31, 32, 33), "Al-'Ankabut 46 - Al-Ahzab 30"),
    JuzData(22, "ومن يقنت", "Wa Man Yaqnut", listOf(33, 34, 35, 36), "Al-Ahzab 31 - Ya-Sin 27"),
    JuzData(23, "وما أنزلنا", "Wa Maliya", listOf(36, 37, 38, 39), "Ya-Sin 28 - Az-Zumar 31"),
    JuzData(24, "فمن أظلم", "Faman Azlamu", listOf(39, 40, 41), "Az-Zumar 32 - Fussilat 46"),
    JuzData(25, "إليه يرد", "Ilayhi Yuraddu", listOf(41, 42, 43, 44, 45), "Fussilat 47 - Al-Jathiyah 37"),
    JuzData(26, "حم", "Ha' Meem", listOf(46, 47, 48, 49, 50, 51), "Al-Ahqaf 1 - Az-Zariyat 30"),
    JuzData(27, "قال فما خطبكم", "Qala Fama Khatbukum", listOf(51, 52, 53, 54, 55, 56, 57), "Az-Zariyat 31 - Al-Hadid 29"),
    JuzData(28, "قد سمع الله", "Qad Sami' Allah", (58..66).toList(), "Al-Mujadila 1 - At-Tahrim 12"),
    JuzData(29, "تبارك الذي", "Tabārak Allazi", (67..77).toList(), "Al-Mulk 1 - Al-Mursalat 50"),
    JuzData(30, "عمّ", "Juz 'Amma", (78..114).toList(), "An-Naba' 1 - An-Nas 6")
  )

  fun getJuz(number: Int): JuzData? = ALL_JUZ.find { it.number == number }
}
