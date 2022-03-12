package exop

object StarName {

    private val catalogPrefixes =
        listOf(
            "1SWASP",
            "2M",
            "BD",
            "CD",
            "CED",
            "CPD",
            "CoRoT",
            "DMPP",
            "EPIC",
            "EWS",
            "GJ",
            "GPM",
            "GSC",
            "Gaia",
            "Gl",
            "HAT",
            "HD",
            "HIP",
            "HR",
            "K2",
            "KELT",
            "KIC",
            "KMT",
            "KOI",
            "Kepler",
            "LHS",
            "LTT",
            "MASCARA",
            "MOA",
            "NGC",
            "NGTS",
            "OGLE",
            "PPM",
            "PSR",
            "SAO",
            "TIC",
            "TOI",
            "TYC",
            "UCAC",
            "UGA",
            "USNO",
            "USco",
            "WASP",
            "WD",
            "WISE",
        )

    private fun noCatalog(name: String): Boolean {
        return catalogPrefixes.none { name.startsWith(it) }
    }


    fun starName(names: List<String>): String {
        if (names.size == 1) return names[0]
        val noCatNames = names.filter { noCatalog(it) }
        return if (noCatNames.isEmpty()) names[0]
        else noCatNames[0]
    }

}