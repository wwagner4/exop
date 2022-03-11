package exop

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
    val dist: Double?, // in au (astronomic units)
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


@Suppress("EnumEntryName")
enum class Catalog { oec, test }

@Suppress("EnumEntryName")
enum class Action(val description: String) {
    i01("Image. Comparison to the inner solar system"),
    i02("Image. Earthlike planets"),
    svgt("Test svg creation"),
    tryout("Helpful during development"),
    names("Names of systems and planets")
}

private const val au = 149597870e3 // m
const val massSun: Double = 1.989e30 // kg
const val secondsInDay = 24.0 * 60 * 60


fun main(args: Array<String>) {
    val parser = ArgParser("exop")
    val catalog by parser.option(
        ArgType.Choice<Catalog>(),
        shortName = "c",
        description = "star system catalog"
    ).default(Catalog.oec)

    val action by parser.argument(
        ArgType.Choice<Action>(),
        description = argDescription()
    )

    try {
        parser.parse(args)
        when (action) {
            Action.i01 -> SVG.create(catalog)
            Action.i02 -> throw IllegalStateException("i01: Not yet implemented")
            Action.svgt -> SVG.createTest(catalog)
            Action.tryout -> tryout(catalog)
            Action.names -> printAllNames(catalog)
        }
    } catch (e: IllegalStateException) {
        println("ERROR: ${e.message}")
    }
}

private fun argDescription(): String {
    val table: String = Action.values().joinToString("\n") { "              %7s : %s}".format(it.name, it.description) }
    return "Descriptions: \n$table"
}

object SVG {

    data class Canvas(val width: Int, val height: Int)

    private val svgNamespace = Namespace.getNamespace("http://www.w3.org/2000/svg")

    private data class Point(val x: Number, val y: Number)

    private fun greatestDist(solarSystem: SolarSystem): Double? {
        return solarSystem.star.planets.mapNotNull { it.dist }.maxOrNull()
    }

    fun create(catalog: Catalog) {

        fun isValidSys(syst: SolarSystem): Boolean {
            val dist = greatestDist(syst)
            return dist != null && dist < 3.0 && dist > 0.7 && syst.star.radius != null && syst.star.radius < 5.0
        }

        println("create svg for catalog: $catalog")
        val canvas = Canvas(1000, 600)

        val solarSystems = readCatalog(catalog, 1000).filter { isValidSys(it) } + listOf(loadSolSystemInner())
        val maxSystemDist = solarSystems.flatMap { it.star.planets }.mapNotNull { it.dist }.maxOrNull()
            ?: throw IllegalStateException("Could not calculate maximum distance system")
        val maxSunRadius =
            solarSystems.mapNotNull { it.star.radius }.maxOrNull() ?: throw IllegalStateException("no sun with radius")
        val maxPlanetRadius = solarSystems.flatMap { it.star.planets }.mapNotNull { it.radius }.maxOrNull()
            ?: throw IllegalStateException("Could not calculate maximum planet radius")
        val borderX = 20.0
        val borderY = 50.0
        val vertDist = (canvas.height - 2 * borderY) / (solarSystems.size - 1)

        val outDir = Path.of("target", "svg")
        val outFile = outDir.resolve("dr1.svg")
        if (Files.notExists(outDir)) Files.createDirectories(outDir)

        fun solSysElems(solarSystem: SolarSystem, index: Int): List<Element> {
            val isSol = solarSystem.name == "SOL"
            val systemDist = greatestDist(solarSystem)
                ?: throw IllegalStateException("solar system with no planet ${solarSystem.name}")
            val paintSystemDist = (canvas.width - 2 * borderY) * systemDist / maxSystemDist
            val y = index * vertDist + borderY
            val sunRadiusPaint = vertDist * (solarSystem.star.radius ?: 1.0) / maxSunRadius

            val lineElem = line(Point(borderX, y), Point(borderX + paintSystemDist, y))
            val starElem =
                if (isSol) sun(Point(borderX, y), sunRadiusPaint)
                else star(Point(borderX, y), sunRadiusPaint)

            val starTxtElem = text(solarSystem.star.name, Point(borderX, y - vertDist * 0.15), textAnchorLeft = true)

            val planetElems = solarSystem.star.planets.flatMap {
                if (it.dist == null) listOf<Element>()
                else {
                    val paintDistPlanet = (canvas.width - 2 * borderY) * it.dist / maxSystemDist
                    val px = borderX + paintDistPlanet
                    if (isSol)
                        listOf(
                            solarPlanet(Point(px, y), 10.0 * (it.radius ?: 0.2) / maxPlanetRadius),
                            text(it.name, Point(px, y - vertDist * 0.15))
                        )
                    else {
                        val elemPlanet =
                            if (it.radius != null) planet(Point(px, y), 10.0 * it.radius / maxPlanetRadius)
                            else planetIndifferent(Point(px, y), 10.0 * 0.2 / maxPlanetRadius)
                        listOf(
                            elemPlanet,
                            text(it.name, Point(px, y - vertDist * 0.15))
                        )

                    }

                }
            }

            return listOf(lineElem, starElem) + planetElems + starTxtElem
        }

        val elems = solarSystems.sortedBy { greatestDist(it) }.withIndex().flatMap { (i, sys) -> solSysElems(sys, i) }
        writeSvg(outFile, canvas) { elems }
    }

    fun createTest(catalog: Catalog) {

        fun testElems(): List<Element> = listOf(
            planet(Point(40.0, 50.0), 20.0),
            star(Point(46.0, 55.0), 30.0),
            star(Point(45.0, 56.55), 130.0),
            planetIndifferent(Point(55.0, 44.0), 22.9),
            line(Point(10.0, 10.0), Point(200.0, 500.0)),
            line(Point(10.0, 10.0), Point(200.0, 510.0)),
            line(Point(10.0, 10.0), Point(200.0, 520.0)),
            text("hallo wolfi", Point(10.0, 200.0)),
            text("I like DJ", Point(11.0, 400.0)),
        )

        println("create test svg for catalog: $catalog")
        val outDir = Path.of("target", "svg")
        val outFile = outDir.resolve("t2.svg")

        if (Files.notExists(outDir)) Files.createDirectories(outDir)

        writeSvg(outFile, Canvas(600, 600)) { testElems() }
    }

    private fun writeSvg(outFile: Path, canvas: Canvas, createElems: () -> List<Element>) {
        val root = svgElem("svg")
        root.setAttribute("viewBox", "0 0 ${canvas.width} ${canvas.height}")

        createElems().forEach { root.addContent(it) }

        val document = Document()
        document.setContent(root)
        val writer = FileWriter(outFile.toFile())
        val outputter = XMLOutputter()
        outputter.format = Format.getPrettyFormat()
        outputter.output(document, writer)
        println("Wrote file to ${outFile.absolute()}")
    }

    private fun planet(center: Point, r: Double): Element {
        return circle(center, r, "blue", 0.6)
    }

    private fun planetIndifferent(center: Point, r: Double): Element {
        return circle(center, r, "blue", 0.45)
    }

    private fun solarPlanet(center: Point, r: Double): Element {
        return circle(center, r, "red", 0.9)
    }

    private fun star(center: Point, r: Double): Element {
        return circle(center, r, "orange", 0.6)
    }

    private fun sun(center: Point, r: Double): Element {
        return circle(center, r, "red", 0.6)
    }

    private fun svgElem(name: String): Element {
        val elem = Element(name)
        elem.namespace = svgNamespace
        return elem
    }

    private fun circle(center: Point, r: Double, color: String, opacity: Double): Element {
        val elem = svgElem("circle")
        elem.setAttribute("cx", center.x.f())
        elem.setAttribute("cy", center.y.f())
        elem.setAttribute("r", r.f())
        elem.setAttribute("opacity", opacity.f())
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
        elem.setAttribute("style", "stroke:blue;stroke-width:0.1")
        return elem
    }


    private fun text(
        text: String,
        origin: Point,
        color: String = "blue",
        opacity: Double = 0.3,
        size: Double = 0.2,
        textAnchorLeft: Boolean = false
    ): Element {
        val elem = svgElem("text")
        elem.setAttribute("x", origin.x.f())
        elem.setAttribute("y", origin.y.f())
        elem.setAttribute("fill", color)
        elem.setAttribute("opacity", opacity.f())
        elem.setAttribute("font-family", "sans-serif")
        elem.setAttribute("font-size", "${size.f()}em")
        if (textAnchorLeft) elem.setAttribute("text-anchor", "end")
        elem.text = text
        return elem
    }

    private fun Number.f(): String {
        return "%.3f".format(this)
    }
}

/**
 * Calculates the large semi axis of a planet
 *
 * @param period in seconds
 * @param mass1 in kg
 * @param mass2 in kg
 * @return large semi axis (distance) in au (astronomic units)
 */
fun largeSemiAxis(period: Double, mass1: Double, mass2: Double): Double {
    val g = 6.667408e-11
    val m = mass1 + mass2
    val a = period * period * g * m / (PI * PI * 4.0)
    return a.pow(1.0 / 3) / au
}


fun loadSolarSystem(): SolarSystem {
    val path = Path.of("src", "main", "resources", "SOL.xml")
    return readSystem(path) ?: throw IllegalStateException("found no data at $path")
}

fun loadSolSystemInner(): SolarSystem {
    val solSyst = loadSolarSystem()
    val innerNames = listOf("Mercury", "Venus", "Earth", "Mars")
    val planetsInner = solSyst.star.planets.filter { innerNames.contains(it.name) }
    val starInner = solSyst.star.copy(planets = planetsInner)
    return solSyst.copy(star = starInner)
}

private fun tryout(catalog: Catalog) {
    println("Tryout with catalog $catalog")
    loadSolarSystem().star.planets.map { it.name }.forEach { println(it) }
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
        Catalog.oec -> catFiles()
        Catalog.test -> testFiles()
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
    val starMass = toDouble(starElem, "mass")
    val starName = starElem.children.filter { it.name == "name" }.map { it.text }.first()
    return Star(
        name = starName,
        radius = toDouble(starElem, "radius"),
        planets = starElem.children.filter { it.name == "planet" }.map { toPlanet(it, starName, starMass) },
        mass = starMass,
    )
}

private fun toPlanet(elem: Element, starName: String, starMass: Double?): Planet {
    val name = elem.children.filter { it.name == "name" }.map { it.text }.first()
    val planetPeriod = toDouble(elem, "period")

    fun planetName(): String {
        if (name.startsWith(starName)) {
            return name.substring(starName.length).trim()
        }
        return name
    }

    fun dist(): Double? {
        if (planetPeriod != null && starMass != null) return largeSemiAxis(
            planetPeriod * secondsInDay,
            0.0,
            starMass * massSun
        )
        return null
    }
    return Planet(
        name = planetName(),
        radius = toDouble(elem, "radius"),
        period = planetPeriod,
        dist = dist()
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

fun readSystem(file: Path): SolarSystem? {
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


