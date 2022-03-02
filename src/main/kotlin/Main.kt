import org.jdom2.Element
import org.jdom2.input.SAXBuilder
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.toList
import java.nio.file.Path.of as pathOf

data class Planet(val name: String, val radius: Double?, val period: Double?)
data class Star(val name: String, val radius: Double?, val planets: List<Planet>)
data class SolarSystem(val name: String, val star: Star)


fun main() {
    val files = catFiles()
    //val files = testFiles()
    val solSysts = files.take(10000000).mapNotNull { readSystem(it) }
    printAllObjects(solSysts)
}

private fun toDouble(elem: Element, name: String): Double? {
    val radiusList = elem.children
        .filter { it.name == name }
        .map {
            when {
                it.text.isEmpty() -> null
                else -> it.text.toDouble()
            }
        }
    return when {
        radiusList.isEmpty() -> null
        else -> radiusList[0]
    }
}

private fun toStar(starElem: Element): Star {
    return Star(
        name = starElem.children.filter { it.name == "name" }.map { it.text }.first(),
        radius = toDouble(starElem, "radius"),
        planets = starElem.children.filter { it.name == "planet" }.map { toPlanet(it) }
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

