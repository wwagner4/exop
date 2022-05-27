package exop

object Names {

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
            "GJ",
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

    fun starName(star: Util.Star, systemName: String): String? {
        fun name(): String {
            val names = star.names
            if (names.size == 1) return names[0]
            val noCatNames = names.filter { catalogPrefixes.none { it.startsWith(it) } }
            return if (noCatNames.isEmpty()) names[0]
            else noCatNames[0]
        }

        val n = name()
        return if (n == systemName) null
        else n
    }

    fun planetName(planet: Util.Planet, systemName: String): String? {
        val firstName = planet.names.first()
        if (firstName.startsWith(systemName)) {
            val shortName = firstName.substring(systemName.length).trim()
            if (shortName.length <= 1) return null
            return shortName
        }
        return firstName
    }

}