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
    val names: List<String>, val systName: String, val dist: Double?, // in au (astronomic units)
    val radius: Double?, // in radius jupiter. radius jupiter = 71492km
    val period: Double?  // in days
) {
    val name: String
        get() {
            val nam = names.first()
            if (nam.startsWith(systName)) return nam.substring(systName.length).trim()
            return nam
        }
}

data class Star(
    val names: List<String>, val radius: Double?, // in radius sun. radius sun = 696342km
    val mass: Double?, // in solar masses
    val planets: List<Planet>
) {
    val name: String
        get() {
            return StarName.starName(names)
        }

}

data class SolarSystem(
    val name: String,
    val star: Star,
)

@Suppress("EnumEntryName")
enum class Action(val description: String) {
    i01("Earthlike Distance"), svgt("Test svg creation"), tryout(
        "Helpful during development"
    ),
}

private const val au = 149597870e3 // m
const val massSun: Double = 1.989e30 // kg
const val secondsInDay = 24.0 * 60 * 60


fun main(args: Array<String>) {
    val parser = ArgParser("exop")
    val action by parser.argument(
        ArgType.Choice<Action>(), description = argDescription()
    )
    try {
        parser.parse(args)
        when (action) {
            Action.i01 -> SVG.i01(action.name, action.description)
            Action.svgt -> SVG.createTest()
            Action.tryout -> tryout()
        }
    } catch (e: IllegalStateException) {
        println("ERROR: ${e.message}")
    }
}

private fun argDescription(): String {
    val table: String = Action.values().joinToString("\n") {
        "              %7s : %s}".format(
            it.name, it.description
        )
    }
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

    data class TextStyle(
        val color: String,
        val opacity: Double,
        val fontFamily: Font.Family,
    )


    fun i01(id: String, title: String) {
        val numberOfSystems = 60

        val width = 1000
        val canvas = Canvas(width, (width * 1.414213562).toInt())
        val borderLeft = 80.0
        val borderRight = 40.0
        val borderTop = 150.0
        val borderBottom = 50.0
        val planetSizeFactor = 1.7
        val starSizeFactor = 1.7
        val txtSize = 0.025
        val txtOffset = 0.1
        val maxSystemDist1 = 1.7

        val textStyle = TextStyle(
            color = "blue",
            opacity = 0.8,
            fontFamily = Font.Family.turretRoad
        )

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

        println("creating image '${title}'")

        val solarSystemsDists: Map<Boolean, List<Syst>> =
            loadCatalog().mapNotNull { minEarthDist(it) }.groupBy { it.minEarthDist < 0 }
        val smaller = solarSystemsDists[true]!!.sortedBy { -it.minEarthDist }.map { it.solarSystem }
        val greater = solarSystemsDists[false]!!.sortedBy { it.minEarthDist }.map { it.solarSystem }

        val n = (numberOfSystems / 2.0).toInt()
        val solarSystems = smaller.take(n).reversed() + listOf(sol) + greater.take(n)


        val allParams = allSystemParameters(solarSystems)

        val vertDist = (canvas.height - borderTop - borderBottom) / (solarSystems.size - 1)
        val outDir = getCreateOutDir()
        val outFile = outDir.resolve("${id}.svg")

        fun solSysElems(solarSystem: SolarSystem, index: Int): List<Element> {
            val isSol = solarSystem.name == "Sun"
            val systemDist = maxPlanetDist(solarSystem)
                ?: throw IllegalStateException("solar system with no planet ${solarSystem.name}")

            val paintY = index * vertDist + borderTop

            val paintSystemDist = (canvas.width - (borderLeft + borderRight)) * systemDist / maxSystemDist1
            val paintMaxSystemDist = (canvas.width - borderRight)
            val lineElem = ExopElems.planetLine(
                Point(borderLeft, paintY), Point(
                    min(borderLeft + paintSystemDist, paintMaxSystemDist), paintY
                )
            )

            val paintRadiusStar = starSizeFactor * vertDist * (solarSystem.star.radius ?: 1.0) / allParams.maxStarRadius
            val starElem = if (isSol) ExopElems.sun(
                Point(borderLeft, paintY), paintRadiusStar
            )
            else ExopElems.star(Point(borderLeft, paintY), paintRadiusStar)

            val systemTxtElem = ExopElems.nameSystem(
                Point(borderLeft, paintY), solarSystem.name,
                vertDist * txtSize,
                vertDist * txtOffset,
                textStyle,
            )
            val starTxtElem = if (solarSystem.name == solarSystem.star.name) null
            else ExopElems.nameGeneral(
                Point(borderLeft, paintY), solarSystem.star.name, vertDist * txtSize, vertDist * txtOffset, textStyle
            )

            val planetElems = solarSystem.star.planets.flatMap {
                if (it.dist == null) listOf()
                else {
                    val paintDistPlanet = (canvas.width - (borderLeft + borderRight)) * it.dist / maxSystemDist1
                    val paintPlanetX = borderLeft + paintDistPlanet
                    if (paintPlanetX > paintMaxSystemDist) listOf()
                    else if (isSol) {
                        val radius = planetSizeFactor * vertDist * it.radius!! / allParams.maxPlanetRadius
                        val elemPlanet = ExopElems.solarPlanet(
                            Point(paintPlanetX, paintY), radius
                        )
                        listOf(
                            elemPlanet, ExopElems.nameGeneral(
                                Point(paintPlanetX, paintY),
                                it.name,
                                vertDist * txtSize,
                                vertDist * txtOffset,
                                textStyle
                            )
                        )
                    } else {
                        val elemPlanet = if (it.radius != null) {
                            val radius = planetSizeFactor * vertDist * it.radius / allParams.maxPlanetRadius
                            ExopElems.planet(
                                Point(paintPlanetX, paintY), radius
                            )
                        } else {
                            ExopElems.planetUnknownRadius(
                                Point(
                                    paintPlanetX, paintY
                                ), vertDist * 0.3
                            )
                        }
                        listOf(
                            elemPlanet, ExopElems.nameGeneral(
                                Point(paintPlanetX, paintY),
                                it.name,
                                vertDist * txtSize,
                                vertDist * txtOffset,
                                textStyle
                            )
                        )
                    }
                }
            }
            return (listOf(lineElem, starElem) + planetElems + listOf(
                systemTxtElem, starTxtElem
            )).filterNotNull()
        }

        val bgElem = Basic.rect(
            Point(0, 0), canvas.width.toDouble(), canvas.height.toDouble(), color = "white"
        )

        fun titleElem(): Element {
            val x = canvas.width - borderRight
            val y = 100
            return Basic.text(
                title,
                Point(x, y),
                size = 4.0,
                textStyle = textStyle,
                textAnchorLeft = true
            )
        }


        val imgElems = solarSystems.withIndex().flatMap { (i, sys) -> solSysElems(sys, i) }
        val titleElem = titleElem()
        val explainElems = ExopElems.multilineText(
            listOf(
                "Planetary systems containing one planet that has",
                "about the same distance to its star than the earth",
            ), canvas.width - borderRight, 200, textStyle = textStyle, zoom = 0.8, textAnchorLeft = true
        )
        val legendElems = ExopElems.legendElems(canvas.width - borderRight, 400, textStyle = textStyle, zoom = 0.8)

        Basic.writeSvg(
            outFile, canvas, textStyle.fontFamily,
        ) { listOf(bgElem) + imgElems + titleElem + legendElems + explainElems }
    }

    fun createTest() {

        val textStyle = TextStyle("red", 0.5, Font.Family.monospace)

        fun testElems(): List<Element> = listOf(
            ExopElems.planet(Point(40.0, 50.0), 20.0),
            ExopElems.star(Point(46.0, 55.0), 30.0),
            ExopElems.star(Point(45.0, 56.55), 130.0),
            ExopElems.planetUnknownRadius(Point(55.0, 44.0), 22.9),
            Basic.line(Point(10.0, 10.0), Point(200.0, 500.0), 0.2, "green"),
            Basic.line(Point(10.0, 10.0), Point(200.0, 510.0), 0.1, "blue"),
            Basic.line(Point(10.0, 10.0), Point(200.0, 520.0), 0.5, "orange"),
            ExopElems.nameGeneral(Point(10.0, 200.0), "hallo wolfi", 20.0, 10.0, textStyle),
            ExopElems.nameGeneral(Point(11.0, 400.0), "I like DJ", 20.0, 20.0, textStyle),
        )

        println("create test svg")
        val outDir = Path.of("target", "svg")
        val outFile = outDir.resolve("t2.svg")

        if (Files.notExists(outDir)) Files.createDirectories(outDir)

        Basic.writeSvg(outFile, Canvas(600, 600), fontFamily = Font.Family.cursive) { testElems() }
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

        fun planetUnknownRadius(center: Point, radius: Double): Element {
            return Basic.circle(center, radius, "green", 0.4)
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

        fun nameGeneral(
            origin: Point, text: String, size: Double, offset: Double, textStyle: TextStyle
        ): Element {
            val origin1 = Point(
                origin.x.toDouble() + offset, origin.y.toDouble() - offset
            )
            return Basic.text(
                text, origin1, size, textStyle, textAnchorLeft = false
            )
        }

        fun nameSystem(
            origin: Point, text: String, size: Double, offset: Double, textStyle: TextStyle
        ): Element {
            val origin1 = Point(
                origin.x.toDouble() - offset, origin.y.toDouble() - offset
            )
            return Basic.text(
                text, origin1, size, textStyle, textAnchorLeft = true
            )
        }

        fun legendElems(
            xBase: Number, yBase: Number, textStyle: TextStyle, zoom: Double = 1.0, textAnchorLeft: Boolean = true
        ): List<Element> {
            data class LegendElem(
                val text: String, val fElem: (Point, Double) -> Element
            )

            val vDist = 30 * zoom
            val imgOffsetX = 15 * zoom
            val imgOffsetY = -5 * zoom
            val imgSize = 10.0 * zoom
            val txtSize = 1.0 * zoom

            fun line(elem: LegendElem, i: Int, textAnchorLeft: Boolean): List<Element> {
                val x = if (textAnchorLeft) xBase.toDouble() + imgOffsetX
                else xBase.toDouble() - imgOffsetX
                val y = yBase.toDouble() + i * vDist
                val txtOrigin = Point(xBase, y)
                val imgOrigin = Point(x, y + imgOffsetY)
                return listOf(
                    elem.fElem(imgOrigin, imgSize),
                    Basic.text(
                        elem.text,
                        txtOrigin,
                        size = txtSize,
                        textStyle = textStyle,
                        textAnchorLeft = textAnchorLeft
                    ),
                )
            }

            val texts = listOf(
                LegendElem("sun and planets of the solar system", ExopElems::sun),
                LegendElem("star, size relative to the sun", ExopElems::star),
                LegendElem("exoplanet, size relative to solar planets", ExopElems::planet),
                LegendElem("exoplanet, unknown size", ExopElems::planetUnknownRadius),
            )
            return texts.withIndex().flatMap { (i, t) -> line(t, i, textAnchorLeft) }
        }

        fun multilineText(
            lines: List<String>,
            xBase: Number,
            yBase: Number,
            textStyle: TextStyle,
            zoom: Double = 1.0,
            textAnchorLeft: Boolean = true
        ): List<Element> {

            val vDist = 30 * zoom
            val txtSize = 1.0 * zoom

            fun line(line: String, i: Int): List<Element> {
                val y = yBase.toDouble() + i * vDist
                val txtOrigin = Point(xBase, y)
                return listOf(
                    Basic.text(
                        line,
                        txtOrigin,
                        size = txtSize,
                        textStyle = textStyle,
                        textAnchorLeft = textAnchorLeft
                    ),
                )
            }

            return lines.withIndex().flatMap { (i, t) -> line(t, i) }
        }


    }

    object Basic {

        private val svgNamespace = Namespace.getNamespace("http://www.w3.org/2000/svg")

        fun writeSvg(
            outFile: Path, canvas: Canvas, fontFamily: Font.Family, createElems: () -> List<Element>
        ) {
            fun z(element: Element): Int {
                if (element.name == "text") return 10
                return 0
            }

            fun addStyle(root: Element) {
                if (fontFamily.def.import != null) {
                    val style = svgElem("style")
                    style.text = fontFamily.def.import
                    val defs = svgElem("defs")
                    defs.addContent(style)
                    root.addContent(defs)
                }
            }

            val root = svgElem("svg")
            root.setAttribute("viewBox", "0 0 ${canvas.width} ${canvas.height}")
            addStyle(root)
            createElems().sortedBy { z(it) }.forEach { root.addContent(it) }

            val document = Document()
            document.setContent(root)
            val writer = FileWriter(outFile.toFile())
            val outputter = XMLOutputter()
            outputter.format = Format.getPrettyFormat()
            outputter.output(document, writer)
            println("Wrote file to ${outFile.absolute()}")
        }

        fun circle(
            center: Point, radius: Double, color: String, opacity: Double
        ): Element {
            val elem = svgElem("circle")
            elem.setAttribute("cx", center.x.f())
            elem.setAttribute("cy", center.y.f())
            elem.setAttribute("r", radius.f())
            elem.setAttribute("opacity", opacity.f())
            elem.setAttribute("fill", color)
            return elem
        }

        fun line(
            from: Point, to: Point, strokeWidth: Double, color: String, opacity: Double = 0.8
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
            size: Double,
            textStyle: TextStyle,
            textAnchorLeft: Boolean = false
        ): Element {
            val elem = svgElem("text")
            elem.setAttribute("x", origin.x.f())
            elem.setAttribute("y", origin.y.f())
            elem.setAttribute("fill", textStyle.color)
            elem.setAttribute("opacity", textStyle.opacity.f())
            elem.setAttribute("font-family", textStyle.fontFamily.def.fontName)
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
    return elem.children.filter { it.name == name }.map {
        when {
            it.text.isEmpty() -> null
            else -> it.text.toDouble()
        }
    }.firstOrNull()
}


private fun toStar(starElem: Element, systName: String): Star {
    val starMass = toDouble(starElem, "mass")
    val starNames = starElem.children.filter { it.name == "name" }.map { it.text }
    return Star(
        names = starNames,
        radius = toDouble(starElem, "radius"),
        planets = starElem.children.filter { it.name == "planet" }
            .map { toPlanet(it, systName = systName, starMass = starMass) },
        mass = starMass,
    )
}

private fun toPlanet(
    elem: Element, systName: String, starMass: Double?
): Planet {
    val names = elem.children.filter { it.name == "name" }.map { it.text }
    val planetPeriod = toDouble(elem, "period")

    fun dist(): Double? {
        if (planetPeriod != null && starMass != null) return largeSemiAxis(
            planetPeriod * secondsInDay, 0.0, starMass * massSun
        )
        return null
    }
    return Planet(
        names = names, radius = toDouble(elem, "radius"), period = planetPeriod, dist = dist(), systName = systName
    )
}

private fun catFiles(): List<Path> {
    val catPath =
        System.getenv("CATALOGUE") ?: throw IllegalStateException("Environment variable CATALOGUE must be defined")
    val catDir = pathOf(catPath)
    val catNames = listOf("systems", "systems_kepler")
    return catNames.flatMap { catFiles(catDir, it) }
}

private fun catFiles(baseDir: Path, catName: String): List<Path> {
    val sysDir = baseDir.resolve(catName)
    return Files.list(sysDir).toList().filter { it.fileName.toString().endsWith("xml") }
}

fun readSystem(file: Path): SolarSystem? {
    val systName = file.fileName.toString().substringBefore(".")
    val db = SAXBuilder()
    val doc = db.build(file.toFile())
    val solSys = doc.rootElement
    val stars = solSys.children.filter { it.name == "star" }.map { toStar(it, systName) }
    val star = when {
        stars.isEmpty() -> null
        stars.size == 1 && stars[0].planets.isNotEmpty() -> stars[0]
        stars.size == 1 -> null
        else -> throw IllegalStateException("System $systName has more than one sun")
    }
    return star?.let { SolarSystem(systName, it) }
}


