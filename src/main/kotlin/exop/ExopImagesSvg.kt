package exop

import exop.Util.loadSolarSystem
import exop.svg.Basic
import exop.svg.ExopElems
import org.jdom2.Element
import java.io.Writer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.min


object ExopImagesSvg {

    private data class SystemStatistic(
        val size: Double, // max planet distance
        val maxStarRadius: Double,
        val maxPlanetRadius: Double,
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

                val earthDists = solarSystem.star.planets.mapNotNull { it.dist }.map { it - Util.earthDist }
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

        val txtSize = 0.008 * pageSize.height
        val maxSystemDist1 = 1.7

        val titleTxtSize = txtSize * 3.2

        val titleY = titleTxtSize * 2.0
        val subTitleY = titleY + txtSize * 2.0
        val explainY = subTitleY + txtSize * 4.0
        val legendY = explainY + 5.5 * txtSize

        val textStyle = Basic.TextStyle(
            color = "blue", opacity = 0.8, fontFamily = Font.Family.turretRoad
        )

        val borderLeft = 0.08 * pageSize.width
        val borderRight = 0.07 * pageSize.width
        val borderTop = 0.08 * pageSize.height
        val borderBottom = 0.05 * pageSize.height

        val systVertDist = (pageSize.height - borderTop - borderBottom) / (solarSystems.size - 1)
        val systTxtSize = systVertDist * 0.45
        val systTxtOffset = systVertDist * 0.15

        val allParams = allSystemParameters(solarSystems)

        fun planetPaintRadius(planet: Util.Planet) =
            planetSizeFactor * systVertDist * planet.radius!! / allParams.maxPlanetRadius

        fun solSysElems(solarSystem: Util.SolarSystem, index: Int): List<Element> {
            val isSol = solarSystem.name == "Sun"
            val systemDist = maxPlanetDist(solarSystem)
                ?: throw IllegalStateException("solar system with no planet ${solarSystem.name}")

            val paintY = index * systVertDist + borderTop

            val paintSystemDist = (pageSize.width - (borderLeft + borderRight)) * systemDist / maxSystemDist1
            val paintMaxSystemDist = (pageSize.width - borderRight)
            val lineElem = svgExop.planetLine(
                Basic.SvgPoint(borderLeft, paintY), Basic.SvgPoint(
                    min(borderLeft + paintSystemDist, paintMaxSystemDist), paintY
                ),
                systVertDist * 0.01
            )

            val paintRadiusStar =
                starSizeFactor * systVertDist * (solarSystem.star.radius ?: 1.0) / allParams.maxStarRadius
            val starElem = if (isSol) svgExop.sun(
                Basic.SvgPoint(borderLeft, paintY), paintRadiusStar
            )
            else svgExop.star(Basic.SvgPoint(borderLeft, paintY), paintRadiusStar)

            val systemTxtElem = svgExop.nameSystem(
                Basic.SvgPoint(borderLeft, paintY), solarSystem.name,
                systTxtSize,
                systTxtOffset,
                textStyle,
            )
            val starName = Names.starName(solarSystem.star, solarSystem.name)
            val starTxtElem = if (solarSystem.name == starName) null
            else svgExop.nameSystem(
                Basic.SvgPoint(borderLeft, paintY),
                starName,
                systTxtSize,
                systTxtOffset,
                textStyle,
                anchorLeft = false,
            )

            val planetElems = solarSystem.star.planets.flatMap {
                if (it.dist == null) listOf()
                else {
                    val paintDistPlanet = (pageSize.width - (borderLeft + borderRight)) * it.dist / maxSystemDist1
                    val paintPlanetX = borderLeft + paintDistPlanet
                    if (paintPlanetX > paintMaxSystemDist) listOf()
                    else {
                        val elemPlanetName = svgExop.nameSystem(
                            Basic.SvgPoint(paintPlanetX, paintY),
                            Names.planetName(it, it.systName),
                            systTxtSize,
                            systTxtOffset,
                            textStyle,
                            anchorLeft = false,
                        )
                        if (isSol) {
                            val elemPlanet =
                                svgExop.solarPlanet(Basic.SvgPoint(paintPlanetX, paintY), planetPaintRadius(it))
                            listOf(elemPlanet, elemPlanetName)
                        } else {
                            val elemPlanet = if (it.radius != null) {
                                svgExop.planet(Basic.SvgPoint(paintPlanetX, paintY), planetPaintRadius(it))
                            } else {
                                svgExop.planetUnknownRadius(
                                    Basic.SvgPoint(paintPlanetX, paintY),
                                    systVertDist * 0.3
                                )
                            }
                            listOf(elemPlanet, elemPlanetName)
                        }
                    }
                }
            }
            return (listOf(lineElem, starElem) + planetElems + listOf(
                systemTxtElem, starTxtElem
            )).filterNotNull()
        }

        val bgElem = svgBasic.rect(
            Basic.SvgPoint(0, 0), pageSize.width, pageSize.height, color = "white"
        )

        fun titleElem(): Element {
            val theTitle = "Known Planetary Systems"
            val origin = Basic.SvgPoint(pageSize.width - borderRight, titleY)
            return svgBasic.text(
                theTitle,
                origin, size = titleTxtSize, textStyle = textStyle, textAnchorLeft = true
            )
        }

        fun subTitleElem(): Element {
            val dat = LocalDate.now()
            val fmt = DateTimeFormatter.ofPattern("MMM YYYY")
            val datStr = fmt.format(dat)
            val origin = Basic.SvgPoint(pageSize.width - borderRight, subTitleY)
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
            pageSize.width - borderRight,
            explainY,
            textStyle = textStyle,
            textSize = txtSize,
            textAnchorLeft = true
        )
        val legendElems = svgExop.legendElems(
            pageSize.width - borderRight,
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

}