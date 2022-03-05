import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.Namespace
import org.jdom2.input.SAXBuilder
import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolute
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
enum class Action { SVG, SVG_TEST, TRYOUT, NAMES }

fun main(args: Array<String>) {
    val parser = ArgParser("example")
    val catStr by parser.option(
        ArgType.String,
        shortName = "c",
        description = "catalog. OEC(open exoplanet catalog), TEST"
    ).default("OEC")

    val actionStr by parser.option(
        ArgType.String,
        shortName = "a",
        description = "action. SVG(default), SVG_TEST, TRYOUT"
    ).default("SVG")

    parser.parse(args)
    val catalog = Catalog.valueOf(catStr)
    when (Action.valueOf(actionStr)) {
        Action.SVG -> SVG.create(catalog)
        Action.SVG_TEST -> SVG.createTest(catalog)
        Action.TRYOUT -> tryout(catalog)
        Action.NAMES -> printAllNames(catalog)
    }
}

object SVG {
    private val svgNamespace = Namespace.getNamespace("http://www.w3.org/2000/svg")

    private data class Point(val x: Double, val y: Double)

    fun create(catalog: Catalog) {
        println("create svg for catalog: $catalog")
    }

    private fun writeSvg(outFile: Path, createElems: () -> List<Element>) {
        val root = svgElem("svg")
        root.setAttribute("viewBox", "0 0 600 600")

        createElems().forEach { root.addContent(it) }

        val document = Document()
        document.setContent(root)
        val writer = FileWriter(outFile.toFile())
        val outputter = XMLOutputter()
        outputter.format = Format.getPrettyFormat()
        outputter.output(document, writer)
        println("Wrote file to ${outFile.absolute()}")
    }

    fun createTest(catalog: Catalog) {

        fun testElems(): List<Element> = listOf(
            planet(Point(40.0, 50.0), 20.0),
            sun(Point(46.0, 55.0), 30.0),
            sun(Point(45.0, 56.55), 130.0),
            planet(Point(55.0, 44.0), 10.0),
            line(Point(10.0, 10.0), Point(200.0, 500.0)),
            line(Point(10.0, 10.0), Point(200.0, 510.0)),
            line(Point(10.0, 10.0), Point(200.0, 520.0)),
            text(Point(10.0, 200.0), "hallo wolfi"),
            text(Point(11.0, 400.0), "I like DJ"),
        )

        println("create test svg for catalog: $catalog")
        val outDir = Path.of("target", "svg")
        val outFile = outDir.resolve("t2.svg")

        if (Files.notExists(outDir)) Files.createDirectories(outDir)

        writeSvg(outFile) { testElems() }
    }

    private fun planet(center: Point, r: Double): Element {
        return circle(center, r, "blue")
    }

    private fun sun(center: Point, r: Double): Element {
        return circle(center, r, "green")
    }

    private fun svgElem(name: String): Element {
        val elem = Element(name)
        elem.namespace = svgNamespace
        return elem
    }

    private fun circle(center: Point, r: Double, color: String): Element {
        val elem = svgElem("circle")
        elem.setAttribute("cx", center.x.f())
        elem.setAttribute("cy", center.y.f())
        elem.setAttribute("r", r.f())
        elem.setAttribute("opacity", "0.3")
        elem.setAttribute("fill", color)
        return elem
    }

    private fun line(from: Point, to: Point): Element {
        val elem = svgElem("line")
        elem.setAttribute("x1", from.x.f())
        elem.setAttribute("y1", from.y.f())
        elem.setAttribute("x2", to.x.f())
        elem.setAttribute("y2", to.y.f())
        elem.setAttribute("opacity", "0.3")
        elem.setAttribute("style", "stroke:blue;stroke-width:2")
        return elem
    }

    private fun text(origin: Point, text: String): Element {
        val elem = svgElem("text")
        elem.setAttribute("x", origin.x.f())
        elem.setAttribute("y", origin.y.f())
        elem.setAttribute("fill", "blue")
        elem.setAttribute("opacity", "0.3")
        elem.setAttribute("font-family", "sans-serif")
        elem.setAttribute("font-size", "6em")
        elem.text = text
        return elem
    }

    private fun Double.f(): String {
        return "%.3f".format(this)
    }
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

private fun tryout(catalog: Catalog) {
    val solSysList = readCatalog(catalog, 50)
    printAllObjects(solSysList)
}

private fun printAllNames(catalog: Catalog) {
    val solSysList = readCatalog(catalog, 51)
    val starNames = solSysList.map { it.star.name }
    val planetNames = solSysList.map { it.star.planets }.flatMap { it.map { iti -> iti.name } }
    printAllObjects(starNames)
    println("-----------------------------------------------")
    printAllObjects(planetNames)
    val all = (starNames + planetNames).joinToString(separator = " ")
    println()
    println(all)
}

private fun readCatalog(catalog: Catalog, maxNumber: Int): List<SolarSystem> {
    val files = when (catalog) {
        Catalog.OEC -> catFiles()
        Catalog.TEST -> testFiles()
    }
    return files.take(maxNumber).mapNotNull { readSystem(it) }
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


