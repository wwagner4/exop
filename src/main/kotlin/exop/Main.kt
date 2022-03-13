package exop

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
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
import kotlin.io.path.name
import kotlin.math.*
import java.nio.file.Path.of as pathOf

data class Planet(
    val name: String,
    val dist: Double?, // in au (astronomic units)
    val radius: Double?, // in radius jupiter. radius jupiter = 71492km
    val period: Double?  // in days
)

data class Star(
    val names: List<String>,
    val radius: Double?, // in radius sun. radius sun = 696342km
    val mass: Double?, // in solar masses
    val planets: List<Planet>
) {
    val name: String
        get() {
            return StarName.starName(names)
        }

}

data class SolarSystem(val name: String, val star: Star)

@Suppress("EnumEntryName")
enum class Action(val description: String) {
    i01("Image. Comparison to the inner solar system"),
    i02("Image. Earthlike planets"),
    all("Image. All images"),
    svgt("Test svg creation"),
    tryout("Helpful during development"),
}

private const val au = 149597870e3 // m
const val massSun: Double = 1.989e30 // kg
const val secondsInDay = 24.0 * 60 * 60


fun main(args: Array<String>) {
    val parser = ArgParser("exop")
    val action by parser.argument(
        ArgType.Choice<Action>(),
        description = argDescription()
    )
    try {
        parser.parse(args)
        when (action) {
            Action.i01 -> SVG.i01(action.name, action.description)
            Action.i02 -> SVG.i02(action.name, action.description)
            Action.all -> {
                SVG.i01(Action.i01.name, Action.i01.description); SVG.i02(Action.i02.name, Action.i02.description)
            }
            Action.svgt -> SVG.createTest()
            Action.tryout -> tryout()
        }
    } catch (e: IllegalStateException) {
        println("ERROR: ${e.message}")
    }
}

private fun argDescription(): String {
    val table: String = Action.values()
        .joinToString("\n") { "              %7s : %s}".format(it.name, it.description) }
    return "Descriptions: \n$table"
}

object SVG {

    data class Canvas(val width: Int, val height: Int)

    data class Point(val x: Number, val y: Number)

    private data class SystemParameters(
        val maxSystemDist: Double,
        val maxStarRadius: Double,
        val maxPlanetRadius: Double,
    )

    private val svgNamespace = Namespace.getNamespace("http://www.w3.org/2000/svg")

    fun i01(name: String, description: String) {
        println("creating image $name (${description})")

        val sol = loadSolarSystemInner()
        val distMars = maxPlanetDist(sol) ?: throw IllegalStateException("Could not calculate distance of Mars")

        fun isValidSys(syst: SolarSystem): Boolean {
            val dist = maxPlanetDist(syst)
            return dist != null && dist < 1.2 * distMars && dist > 0.9 * distMars && syst.star.radius != null
        }

        val catFiltered = loadCatalog().filter { isValidSys(it) }
        val solarSystems = (catFiltered + listOf(loadSolarSystemInner())).sortedBy { maxPlanetDist(it) }
        val allSystemParameters = allSystemParameters(solarSystems)

        val canvas = Canvas(1000, 600)
        val borderX = 40.0
        val borderY = 30.0
        val planetSizeFactor = 1.7
        val starSizeFactor = 1.7

        val outDir = getCreateOutDir()
        val outFile = outDir.resolve("${name}.svg")

        val paintVertDist = (canvas.height - 2 * borderY) / (solarSystems.size - 1)

        fun solSysElems(solarSystem: SolarSystem, index: Int): List<Element> {
            val isSol = solarSystem.name == "Sun"
            val systemSize = maxPlanetDist(solarSystem)
                ?: throw IllegalStateException("solar system with no planet ${solarSystem.name}")

            val paintSystemY = index * paintVertDist + borderY

            val paintSystemDist = (canvas.width - 2 * borderX) * systemSize / allSystemParameters.maxSystemDist
            val lineElem = ExopElems.planetLine(Point(borderX, paintSystemY), Point(borderX + paintSystemDist, paintSystemY))

            val paintRadiusStar =
                starSizeFactor * paintVertDist * (solarSystem.star.radius
                    ?: sol.star.radius!!) / allSystemParameters.maxStarRadius
            val starElem =
                if (isSol) ExopElems.sun(Point(borderX, paintSystemY), paintRadiusStar)
                else ExopElems.star(Point(borderX, paintSystemY), paintRadiusStar)

            val starTxtElem = ExopElems.nameText(
                Point(borderX, paintSystemY - paintVertDist * 0.15),
                solarSystem.star.name,
                paintVertDist,
                textAnchorLeft = true
            )

            val planetElems = solarSystem.star.planets.flatMap {
                if (it.dist == null) listOf()
                else {
                    val paintDistPlanet = (canvas.width - 2 * borderX) * it.dist / allSystemParameters.maxSystemDist
                    val px = borderX + paintDistPlanet
                    if (isSol) {
                        val elemPlanet = ExopElems.solarPlanet(
                            Point(px, paintSystemY),
                            planetSizeFactor * paintVertDist * it.radius!! / allSystemParameters.maxPlanetRadius
                        )
                        listOf(
                            elemPlanet,
                            ExopElems.nameText(Point(px, paintSystemY), it.name, paintVertDist)
                        )
                    } else {
                        val elemPlanet =
                            if (it.radius != null) ExopElems.planet(
                                Point(px, paintSystemY),
                                planetSizeFactor * paintVertDist * it.radius / allSystemParameters.maxPlanetRadius
                            )
                            else ExopElems.planetUnknownRadius(Point(px, paintSystemY), paintVertDist)
                        listOf(
                            elemPlanet,
                            ExopElems.nameText(Point(px, paintSystemY), it.name, paintVertDist)
                        )
                    }
                }
            }

            return listOf(lineElem, starElem) + planetElems + starTxtElem
        }

        val bgElem = Basic.rect(Point(0, 0), canvas.width.toDouble(), canvas.height.toDouble(), color = "white")
        val imgElems = solarSystems.withIndex().flatMap { (i, sys) -> solSysElems(sys, i) }
        Basic.writeSvg(outFile, canvas) { listOf(bgElem) + imgElems }
    }

    fun i02(name: String, description: String) {
        val numberOfSystems = 50

        data class Syst(
            val minEarthDist: Double,
            val solarSystem: SolarSystem,
        )

        val sol = loadSolarSystemInner()

        val earth = sol.star.planets.first { it.name == "Earth" }
        val earthDist = earth.dist!!

        fun minEarthDist(solarSystem: SolarSystem): Syst? {
            data class Dist(
                val dist: Double,
                val distAbs: Double,
            )

            val earthDists = solarSystem.star.planets.mapNotNull { it.dist }.map { it - earthDist }
            if (earthDists.isEmpty()) return null
            val minDist: Dist? = earthDists.map { Dist(it, abs(it)) }.minByOrNull { it.distAbs }
            return Syst(minDist!!.dist, solarSystem)
        }

        println("creating image '${description}'")

        val solarSystemsDists: Map<Boolean, List<Syst>> =
            loadCatalog().mapNotNull { minEarthDist(it) }.groupBy { it.minEarthDist < 0 }
        val smaller = solarSystemsDists[true]!!.sortedBy { -it.minEarthDist }.map { it.solarSystem }
        val greater = solarSystemsDists[false]!!.sortedBy { it.minEarthDist }.map { it.solarSystem }

        val n = (numberOfSystems / 2.0).toInt()
        val solarSystems = smaller.take(n).reversed() + listOf(sol) + greater.take(n)
        val canvas = Canvas(1000, 600)
        val borderX = 40.0
        val borderY = 20.0
        val planetSizeFactor = 1.7
        val starSizeFactor = 1.7

        val fixedSystemDist = 1.7
        val allParams = allSystemParameters(solarSystems)

        val vertDist = (canvas.height - 2 * borderY) / (solarSystems.size - 1)
        val outDir = getCreateOutDir()
        val outFile = outDir.resolve("${name}.svg")

        val bgElem = Basic.rect(Point(0, 0), canvas.width.toDouble(), canvas.height.toDouble(), color = "white")


        fun solSysElems(solarSystem: SolarSystem, index: Int): List<Element> {
            val isSol = solarSystem.name == "Sun"
            val systemDist = maxPlanetDist(solarSystem)
                ?: throw IllegalStateException("solar system with no planet ${solarSystem.name}")

            val paintY = index * vertDist + borderY

            val paintSystemDist = (canvas.width - 2 * borderX) * systemDist / fixedSystemDist
            val paintMaxSystemDist = (canvas.width - borderX)
            val lineElem =
                ExopElems.planetLine(Point(borderX, paintY), Point(min(borderX + paintSystemDist, paintMaxSystemDist), paintY))

            val paintRadiusStar = starSizeFactor * vertDist * (solarSystem.star.radius ?: 1.0) / allParams.maxStarRadius
            val starElem =
                if (isSol) ExopElems.sun(Point(borderX, paintY), paintRadiusStar)
                else ExopElems.star(Point(borderX, paintY), paintRadiusStar)

            val systemTxtElem = ExopElems.nameText(
                Point(borderX, paintY), solarSystem.name,
                vertDist,
                textAnchorLeft = true
            )
            val starTxtElem =
                if (solarSystem.name == solarSystem.star.name) null
                else ExopElems.nameText(Point(borderX, paintY), solarSystem.star.name, vertDist)

            val planetElems = solarSystem.star.planets.flatMap {
                if (it.dist == null) listOf()
                else {
                    val paintDistPlanet = (canvas.width - 2 * borderX) * it.dist / fixedSystemDist
                    val paintPlanetX = borderX + paintDistPlanet
                    if (paintPlanetX > paintMaxSystemDist) listOf()
                    else if (isSol) {
                        val radius = planetSizeFactor * vertDist * it.radius!! / allParams.maxPlanetRadius
                        val elemPlanet = ExopElems.solarPlanet(Point(paintPlanetX, paintY), radius)
                        listOf(
                            elemPlanet,
                            ExopElems.nameText(Point(paintPlanetX, paintY), it.name, vertDist)
                        )
                    } else {
                        val elemPlanet =
                            if (it.radius != null) {
                                val radius = planetSizeFactor * vertDist * it.radius / allParams.maxPlanetRadius
                                ExopElems.planet(Point(paintPlanetX, paintY), radius)
                            } else {
                                ExopElems.planetUnknownRadius(Point(paintPlanetX, paintY), vertDist)
                            }
                        listOf(
                            elemPlanet,
                            ExopElems.nameText(Point(paintPlanetX, paintY), it.name, vertDist)
                        )
                    }
                }
            }
            return (listOf(lineElem, starElem) + planetElems + listOf(systemTxtElem, starTxtElem)).filterNotNull()
        }

        val imgElems = solarSystems.withIndex().flatMap { (i, sys) -> solSysElems(sys, i) }
        Basic.writeSvg(outFile, canvas) { listOf(bgElem) + imgElems }
    }

    fun createTest() {

        fun testElems(): List<Element> = listOf(
            ExopElems.planet(Point(40.0, 50.0), 20.0),
            ExopElems.star(Point(46.0, 55.0), 30.0),
            ExopElems.star(Point(45.0, 56.55), 130.0),
            ExopElems.planetUnknownRadius(Point(55.0, 44.0), 22.9),
            Basic.line(Point(10.0, 10.0), Point(200.0, 500.0), 0.2, "green"),
            Basic.line(Point(10.0, 10.0), Point(200.0, 510.0), 0.1, "blue"),
            Basic.line(Point(10.0, 10.0), Point(200.0, 520.0), 0.5, "orange"),
            ExopElems.nameText(Point(10.0, 200.0), "hallo wolfi", 20.0),
            ExopElems.nameText(Point(11.0, 400.0), "I like DJ", 20.0),
        )

        println("create test svg")
        val outDir = Path.of("target", "svg")
        val outFile = outDir.resolve("t2.svg")

        if (Files.notExists(outDir)) Files.createDirectories(outDir)

        Basic.writeSvg(outFile, Canvas(600, 600)) { testElems() }
    }

    private fun getCreateOutDir(): Path {
        val outDir = Path.of("target", "images")
        if (Files.notExists(outDir)) Files.createDirectories(outDir)
        return outDir
    }

    private fun allSystemParameters(solarSystems: List<SolarSystem>): SystemParameters {
        val maxSystemDist = solarSystems.flatMap { it.star.planets }.mapNotNull { it.dist }.maxOrNull()
            ?: throw IllegalStateException("Could not calculate max system distance")
        val maxStarRadius = solarSystems.mapNotNull { it.star.radius }.maxOrNull()
            ?: throw IllegalStateException("Could not calculate maxStarRadius")
        val maxPlanetRadius = solarSystems.flatMap { it.star.planets }.mapNotNull { it.radius }.maxOrNull()
            ?: throw IllegalStateException("Could not calculate maximum planet radius")
        return SystemParameters(maxSystemDist, maxStarRadius, maxPlanetRadius)
    }

    private fun maxPlanetDist(solarSystem: SolarSystem): Double? {
        return solarSystem.star.planets.mapNotNull { it.dist }.maxOrNull()
    }


    object ExopElems {
        fun planet(center: Point, radius: Double): Element {
            return Basic.circle(center, radius, "green", 0.8)
        }

        fun planetUnknownRadius(center: Point, systemVertHeight: Double): Element {
            return Basic.circle(center, systemVertHeight * 0.1, "green", 0.4)
        }

        fun solarPlanet(center: Point, radius: Double): Element {
            return Basic.circle(center, radius, "red", 0.9)
        }

        fun star(center: Point, radius: Double): Element {
            return Basic.circle(center, radius, "orange", 0.8)
        }

        fun sun(center: Point, radius: Double): Element {
            return Basic.circle(center, radius, "red", 0.9)
        }

        fun planetLine(from: Point, to: Point): Element {
            return Basic.line(from, to, 0.1, "green")
        }

        fun nameText(
            origin: Point,
            text: String,
            systemVertHeight: Double,
            textAnchorLeft: Boolean = false
        ): Element {
            val offset = systemVertHeight * 0.1
            val origin1 =
                if (textAnchorLeft) Point(origin.x.toDouble() - offset, origin.y.toDouble() - offset)
                else Point(origin.x.toDouble() + offset, origin.y.toDouble() - offset)
            return Basic.text(text, origin1, "blue", 0.8, 0.2, textAnchorLeft = textAnchorLeft)
        }
    }

    object Basic {

        fun writeSvg(outFile: Path, canvas: Canvas, createElems: () -> List<Element>) {
            fun z(element: Element): Int {
                if (element.name == "text") return 10
                return 0
            }

            val root = svgElem("svg")
            root.setAttribute("viewBox", "0 0 ${canvas.width} ${canvas.height}")

            createElems().sortedBy { z(it) }.forEach { root.addContent(it) }

            val document = Document()
            document.setContent(root)
            val writer = FileWriter(outFile.toFile())
            val outputter = XMLOutputter()
            outputter.format = Format.getPrettyFormat()
            outputter.output(document, writer)
            println("Wrote file to ${outFile.absolute()}")
        }

        fun circle(center: Point, radius: Double, color: String, opacity: Double): Element {
            val elem = svgElem("circle")
            elem.setAttribute("cx", center.x.f())
            elem.setAttribute("cy", center.y.f())
            elem.setAttribute("r", radius.f())
            elem.setAttribute("opacity", opacity.f())
            elem.setAttribute("fill", color)
            return elem
        }

        fun line(
            from: Point,
            to: Point,
            strokeWidth: Double,
            color: String,
            opacity: Double = 0.8
        ): Element {
            val elem = svgElem("line")
            elem.setAttribute("x1", from.x.f())
            elem.setAttribute("y1", from.y.f())
            elem.setAttribute("x2", to.x.f())
            elem.setAttribute("y2", to.y.f())
            elem.setAttribute("opacity", opacity.f())
            elem.setAttribute("style", "stroke:$color;stroke-width:${strokeWidth.f()}")
            return elem
        }

        fun rect(
            origin: Point,
            width: Double,
            height: Double,
            color: String,
            opacity: Double = 1.0,
        ): Element {
            val elem = svgElem("rect")
            elem.setAttribute("x", origin.x.f())
            elem.setAttribute("y", origin.y.f())
            elem.setAttribute("width", width.f())
            elem.setAttribute("height", height.f())
            elem.setAttribute("opacity", opacity.f())
            elem.setAttribute("style", "fill:$color;")
            return elem
        }

        fun text(
            text: String,
            origin: Point,
            color: String,
            opacity: Double,
            size: Double,
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

        private fun svgElem(name: String): Element {
            val elem = Element(name)
            elem.namespace = svgNamespace
            return elem
        }

        private fun Number.f(): String {
            return "%.3f".format(this.toDouble())
        }
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
    val path = catFiles().first { it.name == "Sun.xml" }
    return readSystem(path) ?: throw IllegalStateException("found no data at $path")
}

fun loadSolarSystemInner(): SolarSystem {
    val solSyst = loadSolarSystem()
    val innerNames = listOf("Mercury", "Venus", "Earth", "Mars")
    val planetsInner = solSyst.star.planets.filter { innerNames.contains(it.name) }
    val starInner = solSyst.star.copy(planets = planetsInner)
    return solSyst.copy(star = starInner)
}

private fun tryout() {
    println("Tryout")
}

private fun loadCatalog(maxNumber: Int = Int.MAX_VALUE): List<SolarSystem> {
    val files = catFiles().filter { it.fileName.toString() != "Sun.xml" }
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
    val starNames = starElem.children.filter { it.name == "name" }.map { it.text }
    return Star(
        names = starNames,
        radius = toDouble(starElem, "radius"),
        planets = starElem.children.filter { it.name == "planet" }.map { toPlanet(it, starNames[0], starMass) },
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


