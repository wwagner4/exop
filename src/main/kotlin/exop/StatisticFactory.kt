package exop

object StatisticFactory {

    fun overview(catalogueDir: String?) {
        println("## Statistics ${Util.nowMonthFormatted()}")

        println("### Summaries")
        val files = Util.catFiles(catalogueDir)
        println("* ${files.size} data files")

        val systems = Util.loadCatalog(catalogueDir)
        println("* ${systems.size} systems with one star")

        val systemWithPlanets = systems.filter { it.star.planets.isNotEmpty() }
        println("* ${systemWithPlanets.size} systems with at least one planet")

        val systemInBinary = systemWithPlanets.filter { it.star.binaryStarSeparation != null }
        println("* ${systemInBinary.size} systems with a binary partner and at least one planet")

        val planets = systems.flatMap { it.star.planets }
        println("* ${planets.size} planets")

        println("### Systems with a binary partner and at least one planet")
        println("Separation of binaries < 100 AU")
        println()
        println("|Number|Name|Separation[AU]|Planet count|System size[AU]|")
        println("|----|----|----|----|----|")
        systemInBinary.filter { it.star.binaryStarSeparation!! < 100.0 }.sortedBy { it.star.binaryStarSeparation }
            .withIndex()
            .forEach {
                val dist = Util.doubleFormatted(Util.maxDistancePlanet(it.value.star), 8)
                val s = "|%5d | %25s | %9.1f | %4d | %s|".format(
                    it.index,
                    it.value.name,
                    it.value.star.binaryStarSeparation,
                    it.value.star.planets.size,
                    dist
                )
                println(s)
            }
    }
}