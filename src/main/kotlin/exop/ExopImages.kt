package exop

import exop.Util.loadSolarSystem
import exop.svg.Basic
import exop.svg.ExopElems
import org.jdom2.Element
import java.io.Writer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow

object ExopImages {

    private const val au = 149597870e3 // m
    private const val earthDist = 1.0 // au

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


    fun i01(writer: Writer, pageSize: Util.PageSize, catalogue: String?) {

        val title = "Earth-like Distance"
        println("creating image '${title}'")
        val numberOfSystems = 100

        fun getFilteredSystems(): List<Util.SolarSystem> {
            data class Syst(
                val minEarthDist: Double,
                val solarSystem: Util.SolarSystem,
            )

            val sol = loadSolarSystem(catalogue)


            fun minEarthDist(solarSystem: Util.SolarSystem): Syst? {
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
                Util.loadCatalog(catalogue).mapNotNull { minEarthDist(it) }.groupBy { it.minEarthDist < 0 }
            val smaller = solarSystemsDists[true]!!.sortedBy { -it.minEarthDist }.map { it.solarSystem }
            val greater = solarSystemsDists[false]!!.sortedBy { it.minEarthDist }.map { it.solarSystem }

            val n = (numberOfSystems / 2.0).toInt()
            return smaller.take(n).reversed() + listOf(sol) + greater.take(n)
        }

        val solarSystems = getFilteredSystems()

        val unit = "mm"
        val svgBasic = Basic(unit)
        val svgExop = ExopElems(svgBasic)

        val planetSizeFactor = 1.7
        val starSizeFactor = 1.7

        val txtSize = 0.008 * pageSize.heightMm
        val maxSystemDist1 = 1.7

        val titleTxtSize = txtSize * 3.2

        val titleY = titleTxtSize * 2.0
        val subTitleY = titleY + txtSize * 2.0
        val explainY = subTitleY + txtSize * 4.0
        val legendY = explainY + 5.5 * txtSize

        val textStyle = TextStyle(
            color = "blue", opacity = 0.8, fontFamily = Font.Family.turretRoad
        )

        val borderLeft = 0.08 * pageSize.widthMm
        val borderRight = 0.07 * pageSize.widthMm
        val borderTop = 0.08 * pageSize.heightMm
        val borderBottom = 0.05 * pageSize.heightMm

        val systVertDist = (pageSize.heightMm - borderTop - borderBottom) / (solarSystems.size - 1)
        val systTxtSize = systVertDist * 0.45
        val systTxtOffset = systVertDist * 0.15

        val allParams = allSystemParameters(solarSystems)

        fun solSysElems(solarSystem: Util.SolarSystem, index: Int): List<Element> {
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
            val starName = Names.starName(solarSystem.star.names)
            val starTxtElem = if (solarSystem.name == starName) null
            else svgExop.nameSystem(
                Point(borderLeft, paintY),
                starName,
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
                            Names.planetName(it.names, it.systName),
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
                "$title. Creation date: $datStr",
                origin, size = txtSize, textStyle = textStyle, textAnchorLeft = true
            )
        }

        val imgElems = solarSystems.withIndex().flatMap { (i, sys) -> solSysElems(sys, i) }
        val titleElem = titleElem()
        val subTitleElem = subTitleElem()
        val explainElems = svgExop.multilineText(
            listOf(
                "Planetary systems containing one planet that has about",
                "the same distance to its star as the earth to the sun",
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

        svgBasic.writeSvg(
            writer, pageSize, textStyle.fontFamily,
        ) { listOf(bgElem) + imgElems + titleElem + subTitleElem + legendElems + explainElems }
    }


    fun createTest(writer: Writer, pageSize: Util.PageSize, catalogue: String?) {

        val unit = "mm"
        val svgBasic = Basic(unit)

        val fontFam = Font.Family.serif
        val textStyle = TextStyle("black", 0.9, fontFam)
        val objOpacity = 0.5

        val borderLeft = 0.01 * pageSize.widthMm
        val borderRight = 0.01 * pageSize.widthMm
        val borderTop = 0.12 * pageSize.heightMm
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

        println("create test svg. pageSize: $pageSize catalogue: $catalogue")
        svgBasic.writeSvg(writer, pageSize, fontFamily = fontFam, ::testElems)
    }


    private fun allSystemParameters(solarSystems: List<Util.SolarSystem>): SystemStatistic {
        val maxSystemDist = solarSystems.flatMap { it.star.planets }.mapNotNull { it.dist }.maxOrNull()
            ?: throw IllegalStateException("Could not calculate max system distance")
        val maxStarRadius = solarSystems.mapNotNull { it.star.radius }.maxOrNull()
            ?: throw IllegalStateException("Could not calculate maxStarRadius")
        val maxPlanetRadius = solarSystems.flatMap { it.star.planets }.mapNotNull { it.radius }.maxOrNull()
            ?: throw IllegalStateException("Could not calculate maximum planet radius")
        return SystemStatistic(maxSystemDist, maxStarRadius, maxPlanetRadius)
    }

    private fun maxPlanetDist(solarSystem: Util.SolarSystem): Double? {
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


}