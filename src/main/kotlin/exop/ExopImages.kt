package exop

import exop.svg.Basic
import exop.svg.ExopElems
import org.jdom2.Element
import org.jdom2.input.SAXBuilder
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.io.path.absolute
import kotlin.io.path.name
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow

object ExopImages {

    private const val massSun: Double = 1.989e30 // kg
    private const val secondsInDay = 24.0 * 60 * 60
    private const val au = 149597870e3 // m
    private const val earthDist = 1.0 // au

    data class Planet(
        val names: List<String>,
        val systName: String,
        val dist: Double?, // in au (astronomic units)
        val radius: Double?, // in radius jupiter. radius jupiter = 71492km
        val period: Double?  // in days
    ) {
        val name: String
            get() {
                val nam = names.first()
                if (nam.startsWith(systName)) {
                    val shortNam = nam.substring(systName.length).trim()
                    if (shortNam.length <= 1) return ""
                    return shortNam
                }
                return nam
            }
    }

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

    data class SolarSystem(
        val name: String,
        val star: Star,
    )

    data class Point(val x: Number, val y: Number)

    private data class SystemStatistic(
        val size: Double, // max planet distance
        val maxStarRadius: Double,
        val maxPlanetRadius: Double,
    )

    data class TextStyle(
        val color: String,
        val opacity: Double,
        val fontFamily: Font.Family,
    )


    fun i01(id: String, title: String) {

        println("creating image '${title}'")
        val numberOfSystems = 100

        fun getFilteredSystems(): List<SolarSystem> {
            data class Syst(
                val minEarthDist: Double,
                val solarSystem: SolarSystem,
            )

            val sol = loadSolarSystem()


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

            val solarSystemsDists: Map<Boolean, List<Syst>> =
                loadCatalog().mapNotNull { minEarthDist(it) }.groupBy { it.minEarthDist < 0 }
            val smaller = solarSystemsDists[true]!!.sortedBy { -it.minEarthDist }.map { it.solarSystem }
            val greater = solarSystemsDists[false]!!.sortedBy { it.minEarthDist }.map { it.solarSystem }

            val n = (numberOfSystems / 2.0).toInt()
            return smaller.take(n).reversed() + listOf(sol) + greater.take(n)
        }

        val solarSystems = getFilteredSystems()

        fun createSvg(pageSize: Util.PageSize) {
            val unit = "mm"
            val svgBasic = Basic(unit)
            val svgExop = ExopElems(svgBasic)

            val planetSizeFactor = 1.7
            val starSizeFactor = 1.7

            val txtSize = 0.01 * pageSize.heightMm
            val maxSystemDist1 = 1.7

            val titleTxtSize = txtSize * 3.2

            val titleY = titleTxtSize * 2.5
            val subTitleY = titleY + txtSize * 2.0
            val explainY = subTitleY + txtSize * 4.0
            val legendY = explainY + 5.5 * txtSize

            val textStyle = TextStyle(
                color = "blue", opacity = 0.8, fontFamily = Font.Family.turretRoad
            )

            val borderLeft = 0.1 * pageSize.widthMm
            val borderRight = 0.07 * pageSize.widthMm
            val borderTop = 0.1 * pageSize.heightMm
            val borderBottom = 0.05 * pageSize.heightMm

            val systVertDist = (pageSize.heightMm - borderTop - borderBottom) / (solarSystems.size - 1)
            val systTxtSize = systVertDist * 0.45
            val systTxtOffset = systVertDist * 0.15

            val allParams = allSystemParameters(solarSystems)

            fun solSysElems(solarSystem: SolarSystem, index: Int): List<Element> {
                val isSol = solarSystem.name == "Sun"
                val systemDist = maxPlanetDist(solarSystem)
                    ?: throw IllegalStateException("solar system with no planet ${solarSystem.name}")

                val paintY = index * systVertDist + borderTop

                val paintSystemDist = (pageSize.widthMm - (borderLeft + borderRight)) * systemDist / maxSystemDist1
                val paintMaxSystemDist = (pageSize.widthMm - borderRight)
                val lineElem = svgExop.planetLine(
                    Point(borderLeft, paintY), Point(
                        min(borderLeft + paintSystemDist, paintMaxSystemDist), paintY
                    ),
                    systVertDist * 0.01
                )

                val paintRadiusStar =
                    starSizeFactor * systVertDist * (solarSystem.star.radius ?: 1.0) / allParams.maxStarRadius
                val starElem = if (isSol) svgExop.sun(
                    Point(borderLeft, paintY), paintRadiusStar
                )
                else svgExop.star(Point(borderLeft, paintY), paintRadiusStar)

                val systemTxtElem = svgExop.nameSystem(
                    Point(borderLeft, paintY), solarSystem.name,
                    systTxtSize,
                    systTxtOffset,
                    textStyle,
                )
                val starTxtElem = if (solarSystem.name == solarSystem.star.name) null
                else svgExop.nameSystem(
                    Point(borderLeft, paintY),
                    solarSystem.star.name,
                    systTxtSize,
                    systTxtOffset,
                    textStyle,
                    anchorLeft = false,
                )

                val planetElems = solarSystem.star.planets.flatMap {
                    if (it.dist == null) listOf()
                    else {
                        val paintDistPlanet = (pageSize.widthMm - (borderLeft + borderRight)) * it.dist / maxSystemDist1
                        val paintPlanetX = borderLeft + paintDistPlanet
                        if (paintPlanetX > paintMaxSystemDist) listOf()
                        else {
                            val elemPlanetName = svgExop.nameSystem(
                                Point(paintPlanetX, paintY),
                                it.name,
                                systTxtSize,
                                systTxtOffset,
                                textStyle,
                                anchorLeft = false,
                            )
                            if (isSol) {
                                val radius = planetSizeFactor * systVertDist * it.radius!! / allParams.maxPlanetRadius
                                val elemPlanet = svgExop.solarPlanet(
                                    Point(paintPlanetX, paintY), radius
                                )
                                listOf(
                                    elemPlanet, elemPlanetName
                                )
                            } else {
                                val elemPlanet = if (it.radius != null) {
                                    val radius = planetSizeFactor * systVertDist * it.radius / allParams.maxPlanetRadius
                                    svgExop.planet(
                                        Point(paintPlanetX, paintY), radius
                                    )
                                } else {
                                    svgExop.planetUnknownRadius(
                                        Point(
                                            paintPlanetX, paintY
                                        ), systVertDist * 0.3
                                    )
                                }
                                listOf(
                                    elemPlanet, elemPlanetName
                                )
                            }
                        }
                    }
                }
                return (listOf(lineElem, starElem) + planetElems + listOf(
                    systemTxtElem, starTxtElem
                )).filterNotNull()
            }

            val bgElem = svgBasic.rect(
                Point(0, 0), pageSize.widthMm, pageSize.heightMm, color = "white"
            )

            fun titleElem(): Element {
                val theTitle = "Known Planetary Systems"
                val origin = Point(pageSize.widthMm - borderRight, titleY)
                return svgBasic.text(
                    theTitle,
                    origin, size = titleTxtSize, textStyle = textStyle, textAnchorLeft = true
                )
            }

            fun subTitleElem(): Element {
                val dat = LocalDate.now()
                val fmt = DateTimeFormatter.ofPattern("MMM YYYY")
                val datStr = fmt.format(dat)
                val origin = Point(pageSize.widthMm - borderRight, subTitleY)
                return svgBasic.text(
                    "$title as of $datStr",
                    origin, size = txtSize, textStyle = textStyle, textAnchorLeft = true
                )
            }

            val imgElems = solarSystems.withIndex().flatMap { (i, sys) -> solSysElems(sys, i) }
            val titleElem = titleElem()
            val subTitleElem = subTitleElem()
            val explainElems = svgExop.multilineText(
                listOf(
                    "Planetary systems containing one planet that has",
                    "about the same distance to its star than the earth",
                ),
                pageSize.widthMm - borderRight,
                explainY,
                textStyle = textStyle,
                textSize = txtSize,
                textAnchorLeft = true
            )
            val legendElems = svgExop.legendElems(
                pageSize.widthMm - borderRight,
                legendY,
                imgOffsetX = 0.6 * txtSize,
                imgOffsetY = -0.3 * txtSize,
                imgSize = txtSize * 0.3,
                textStyle = textStyle,
                textSize = txtSize,
                textAnchorLeft = true,
            )

            val outDir = Util.outDir()
            val outFile = outDir.resolve("exop-${id}-${pageSize.name}.svg")
            svgBasic.writeSvg(
                outFile, pageSize, textStyle.fontFamily,
            ) { listOf(bgElem) + imgElems + titleElem + subTitleElem + legendElems + explainElems }
        }

        val sizes = listOf(
            Util.PageSize.A0,
            Util.PageSize.A1,
            Util.PageSize.A2,
            Util.PageSize.A3,
            Util.PageSize.A4,
            Util.PageSize.A5,
        )
        sizes.forEach { createSvg(it) }
    }


    fun createTest() {

        fun img(pageSize: Util.PageSize) {
            val unit = "mm"
            val svgBasic = Basic(unit)

            val fontFam = Font.Family.serif
            val textStyle = TextStyle("black", 0.9, fontFam)
            val objOpacity = 0.5

            val borderLeft = 0.1 * pageSize.widthMm
            val borderRight = 0.05 * pageSize.widthMm
            val borderTop = 0.2 * pageSize.heightMm
            val txtSizeTitle = 0.07 * pageSize.heightMm
            val txtSize = txtSizeTitle * 0.5
            val strokeWidth = pageSize.heightMm * 0.00015

            val lineHeight = txtSizeTitle * 1.1
            val horObjDist = (pageSize.widthMm - (borderLeft + borderRight)) / 10.0

            fun testElems(): List<Element> {
                return listOf(
                    svgBasic.rect(Point(0, 0), pageSize.widthMm, pageSize.heightMm, "white", 1.0),
                    svgBasic.text(
                        "Test Title",
                        Point(borderLeft, borderTop + lineHeight * 1.0),
                        txtSizeTitle,
                        textStyle
                    ),
                    svgBasic.text(
                        "Test Title right",
                        Point(pageSize.widthMm - borderRight, borderTop + lineHeight * 2.0),
                        txtSizeTitle,
                        textStyle,
                        textAnchorLeft = true
                    ),
                    svgBasic.text(
                        "This is a normal text",
                        Point(pageSize.widthMm - borderRight, borderTop + lineHeight * 3.0),
                        txtSize,
                        textStyle,
                        textAnchorLeft = true
                    ),
                    svgBasic.text(
                        "This is a normal text",
                        Point(borderLeft, borderTop + lineHeight * 4.0),
                        txtSize,
                        textStyle
                    ),
                    svgBasic.line(
                        Point(borderLeft, borderTop + lineHeight * 5.0),
                        Point(pageSize.widthMm - borderRight, borderTop + lineHeight * 5.0),
                        strokeWidth = strokeWidth,
                        color = "blue",
                        opacity = objOpacity
                    ),
                    svgBasic.line(
                        Point(borderLeft, borderTop + lineHeight * 6.0),
                        Point(pageSize.widthMm - borderRight, borderTop + lineHeight * 6.0),
                        strokeWidth = strokeWidth,
                        color = "blue",
                        opacity = objOpacity
                    ),
                    svgBasic.circle(
                        center = Point(borderLeft + horObjDist * 1.0, borderTop + lineHeight * 6.0),
                        radius = lineHeight * 1.0,
                        color = "orange",
                        opacity = objOpacity
                    ),
                    svgBasic.circle(
                        center = Point(borderLeft + horObjDist * 3.0, borderTop + lineHeight * 6.0),
                        radius = lineHeight * 1.0,
                        color = "orange",
                        opacity = objOpacity
                    ),
                    svgBasic.circle(
                        center = Point(borderLeft + horObjDist * 4.0, borderTop + lineHeight * 6.0),
                        radius = lineHeight * 0.5,
                        color = "green",
                        opacity = objOpacity
                    ),
                )
            }

            println("create test svg")
            val formatStr = "$pageSize-${fontFam.name}"
            val nam = "t2-$formatStr"
            val outDir = Util.outDir()
            val outFile = outDir.resolve("$nam.svg")

            if (Files.notExists(outDir)) Files.createDirectories(outDir)

            svgBasic.writeSvg(outFile, pageSize, fontFamily = fontFam, ::testElems)
            //Util.renderSvg("chromium", nam, outDir, ps)
        }

        val pss = listOf(
            Util.PageSize.A0,
            Util.PageSize.A3,
            Util.PageSize.A4,
            Util.PageSize.A5
        )
        pss.forEach { img(it) }
    }


    private fun allSystemParameters(solarSystems: List<SolarSystem>): SystemStatistic {
        val maxSystemDist = solarSystems.flatMap { it.star.planets }.mapNotNull { it.dist }.maxOrNull()
            ?: throw IllegalStateException("Could not calculate max system distance")
        val maxStarRadius = solarSystems.mapNotNull { it.star.radius }.maxOrNull()
            ?: throw IllegalStateException("Could not calculate maxStarRadius")
        val maxPlanetRadius = solarSystems.flatMap { it.star.planets }.mapNotNull { it.radius }.maxOrNull()
            ?: throw IllegalStateException("Could not calculate maximum planet radius")
        return SystemStatistic(maxSystemDist, maxStarRadius, maxPlanetRadius)
    }

    private fun maxPlanetDist(solarSystem: SolarSystem): Double? {
        return solarSystem.star.planets.mapNotNull { it.dist }.maxOrNull()
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

        fun default(): String {
            val dir = Path.of("..", "open_exoplanet_catalogue")
            if (Files.notExists(dir)) throw IllegalStateException("Environment variable CATALOGUE must be defined")
            return dir.absolute().toString()
        }

        val catPath = System.getenv("CATALOGUE") ?: default()
        val catDir = Path.of(catPath)
        val catNames = listOf("systems", "systems_kepler")
        return catNames.flatMap { catFiles(catDir, it) }
    }

    private fun catFiles(baseDir: Path, catName: String): List<Path> {
        val sysDir = baseDir.resolve(catName)
        return Files.list(sysDir).toList().filter { it.fileName.toString().endsWith("xml") }
    }

    private fun readSystem(file: Path): SolarSystem? {
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


}