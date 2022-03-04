import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import org.jdom2.Element
import org.jdom2.input.SAXBuilder
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.*
import java.nio.file.Path.of as pathOf

data class Planet(
    val name: String,
    val radius: Double?, // in radius jupiter. radius jupiter = 71492km
    val period: Double?  // in days
)

data class Star(
    val name: String,
    val radius: Double?, // in radius sun. radius sun = 696342km
    val mass: Double?, // in solar masses
    val planets: List<Planet>
)

data class SolarSystem(val name: String, val star: Star)


enum class Catalog { OEC, TEST }

fun main(args: Array<String>) {
    val parser = ArgParser("example")
    val input by parser.option(
        ArgType.String,
        shortName = "c",
        description = "catalog. OEC(open exoplanet catalog), TEST"
    ).default("OEC")

    parser.parse(args)
    val catalog = Catalog.valueOf(input)
    solarSys(catalog)
}

/**
 * Calculates the large semi axis of a planet
 *
 * @param period in seconds
 * @param mass1 in kg
 * @param mass2 in kg
 * @return large semi axis (distance) m
 */
fun largeSemiAxis(period: Double, mass1: Double, mass2: Double): Double {
    val g = 6.667408e-11
    val m = mass1 + mass2
    val a = period * period * g * m / (PI * PI * 4.0)
    return a.pow(1.0 / 3)
}

private fun solarSys(catalog: Catalog) {
    val files = when (catalog) {
        Catalog.OEC -> catFiles()
        Catalog.TEST -> testFiles()
    }
    val solSysts = files.take(10).mapNotNull { readSystem(it) }
    printAllObjects(solSysts)
}

private fun toDouble(elem: Element, name: String): Double? {
    return elem.children
        .filter { it.name == name }
        .map {
            when {
                it.text.isEmpty() -> null
                else -> it.text.toDouble()
            }
        }.firstOrNull()
}


private fun toStar(starElem: Element): Star {
    return Star(
        name = starElem.children.filter { it.name == "name" }.map { it.text }.first(),
        radius = toDouble(starElem, "radius"),
        planets = starElem.children.filter { it.name == "planet" }.map { toPlanet(it) },
        mass = toDouble(starElem, "mass"),
    )
}

private fun toPlanet(elem: Element): Planet {
    return Planet(
        name = elem.children.filter { it.name == "name" }.map { it.text }.first(),
        radius = toDouble(elem, "radius"),
        period = toDouble(elem, "period")
    )
}

private fun catFiles(): List<Path> {
    val catPath = System.getenv("CATALOGUE")
        ?: throw IllegalStateException("Environment variable CATALOGUE must be defined")
    val catDir = pathOf(catPath)
    val catNames = listOf("systems", "systems_kepler")
    return catNames.flatMap { catFiles(catDir, it) }
}

private fun testFiles(): List<Path> {
    val catDir = pathOf("src", "main", "resources")
    return catFiles(catDir, "test_catalog")
}

private fun <T> printAllObjects(obj: List<T>) {
    obj.withIndex().forEach {
        println("${it.index} - ${it.value}")
    }
}

private fun catFiles(baseDir: Path, catName: String): List<Path> {
    val sysDir = baseDir.resolve(catName)
    return Files.list(sysDir).toList().filter { it.fileName.toString().endsWith("xml") }
}

private fun readSystem(file: Path): SolarSystem? {
    val sysName = file.fileName.toString().substringBefore(".")
    val db = SAXBuilder()
    val doc = db.build(file.toFile())
    val solSys = doc.rootElement
    val stars = solSys.children.filter { it.name == "star" }.map { toStar(it) }
    val star = when {
        stars.isEmpty() -> null
        stars.size == 1 && stars[0].planets.isNotEmpty() -> stars[0]
        stars.size == 1 -> null
        else -> throw IllegalStateException("System $sysName has more than one sun")
    }
    return star?.let { SolarSystem(sysName, it) }
}


