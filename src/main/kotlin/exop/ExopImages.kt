package exop

import exop.Names.planetName
import exop.Names.starName
import exop.ielems.*
import java.io.Writer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.min


object Img02 {

    fun create(writer: Writer, pageSize: Util.PageSize, catalogue: String?) {

        val maxPlanetDist = 5.0
        val lineSpacing = 0.02
        val textOffsetValue = 0.001
        val starSizeFactor = 0.002
        val planetSizeFactor = 0.01
        val unknownPlanetSize = 0.25

        val textStyle = object : ITextStyle {
            override val fontFamily: IFont
                get() = IFont.TURRET
            override val fontScale: IFontScale
                get() = { size ->
                    when (size) {
                        ITextSize.L -> 0.035
                        ITextSize.M -> 0.0125
                        ITextSize.S -> 0.005
                    }
                }
        }

        fun selectSystems(): List<Util.SolarSystem> {
            data class SizeSystem(
                val size: Double, val system: Util.SolarSystem
            )

            fun numberOfPlanetsWithKnownRadius(solarSystem: Util.SolarSystem): Int {
                return solarSystem.star.planets.mapNotNull { it.radius }.size
            }

            fun sizeSystem(solarSystem: Util.SolarSystem): SizeSystem? {
                val size = solarSystem.star.planets.mapNotNull { it.dist }.sorted()
                if (size.isEmpty()) return null
                return SizeSystem(size.last(), solarSystem)
            }

            val systems = Util.loadCatalog(catalogue).asSequence().mapNotNull { sizeSystem(it) }
                .filter { it.size <= maxPlanetDist && numberOfPlanetsWithKnownRadius(it.system) >= 1 }
                .sortedBy { -it.size }.take(100).map { it.system }.toList()
            val sol = Util.loadSolarSystem(catalogue)
            return (listOf(sol) + systems).reversed()
        }

        fun createImage(solarSystems: List<Util.SolarSystem>): IImage {

            fun infoElements(): List<IElement> {

                val legendDescs = listOf(
                    ImgCommons.LegendDesc("Sun and planets of the solar system", IColor.RED, IOpacity.MEDIUM),
                    ImgCommons.LegendDesc("Star, size relative to the sun", IColor.ORANGE, IOpacity.MEDIUM),
                    ImgCommons.LegendDesc("Exoplanet, size relative to solar planets", IColor.GREEN, IOpacity.MEDIUM),
                    ImgCommons.LegendDesc("Exoplanet, unknown size", IColor.GREEN, IOpacity.LOW),
                )
                val description = listOf(
                    "Planetary systems containing at",
                    "least one planet with known size.",
                )
                val base = 0.04

                return listOf(
                    IUtil.baseText(
                        IUtil.point(1, base), "Known Planetary Systems", ITextSize.L, ITextAnchor.END
                    ),
                    IUtil.baseText(
                        IUtil.point(1, base + 0.045),
                        "Planet sizes. Creation date: ${ImgCommons.datStr()}",
                        ITextSize.M,
                        ITextAnchor.END
                    ),
                    IUtil.multilineText(IUtil.point(1, base + 0.105), description, lineSpacing),
                    ImgCommons.legend(
                        IUtil.point(1, base + 0.26),
                        legendDescs,
                        lineSpacing, textStyle,
                        -0.004
                    ),
                )
            }


            val imageElements = listOf(
                IUtil.fillRect(
                    IColor.YELLOW,
                    IOpacity.XLOW
                )
            ) + infoElements() + IUtil.equallyDistributedElements(solarSystems) {
                ImgCommons.systemElements(
                    it,
                    maxPlanetDist,
                    textOffsetValue,
                    starSizeFactor,
                    planetSizeFactor,
                    unknownPlanetSize,
                )
            }

            return object : IImage {
                override val page: IPage
                    get() {
                        return IUtil.page(
                            IUtil.borderCanvas(
                                imageElements, 0.05, 0.1, 0.05, 0.07
                            ), pageSize
                        )
                    }
                override val textStyle: ITextStyle
                    get() = textStyle
            }
        }

        val systems = selectSystems()
        val image = createImage(systems)
        SvgRenderer.writeSvg(writer, image)
    }
}

object Img01 {

    fun create(writer: Writer, pageSize: Util.PageSize, catalogue: String?) {

        val maxPlanetDist = 1.6
        val lineSpacing = 0.02
        val textOffsetValue = 0.001
        val starSizeFactor = 0.001
        val planetSizeFactor = 0.017
        val unknownPlanetSize = 0.25

        val textStyle = object : ITextStyle {
            override val fontFamily: IFont
                get() = IFont.TURRET
            override val fontScale: IFontScale
                get() = { size ->
                    when (size) {
                        ITextSize.L -> 0.035
                        ITextSize.M -> 0.01
                        ITextSize.S -> 0.005
                    }
                }
        }

        fun selectSystems(): List<Util.SolarSystem> {

            val numberOfSystems = 100

            data class System(
                val minEarthDist: Double,
                val solarSystem: Util.SolarSystem,
            )

            val sol = Util.loadSolarSystem(catalogue)


            fun minEarthDist(solarSystem: Util.SolarSystem): System? {
                data class Dist(
                    val dist: Double,
                    val distAbs: Double,
                )

                val earthDists = solarSystem.star.planets.mapNotNull { it.dist }.map { it - Util.earthDist }
                if (earthDists.isEmpty()) return null
                val minDist: Dist? = earthDists.map { Dist(it, abs(it)) }.minByOrNull { it.distAbs }
                return System(minDist!!.dist, solarSystem)
            }

            val solarSystemsDists: Map<Boolean, List<System>> =
                Util.loadCatalog(catalogue).mapNotNull { minEarthDist(it) }.groupBy { it.minEarthDist < 0 }
            val smaller = solarSystemsDists[true]!!.sortedBy { -it.minEarthDist }.map { it.solarSystem }
            val greater = solarSystemsDists[false]!!.sortedBy { it.minEarthDist }.map { it.solarSystem }

            val n = (numberOfSystems / 2.0).toInt()
            return smaller.take(n).reversed() + listOf(sol) + greater.take(n)
        }

        fun createImage(solarSystems: List<Util.SolarSystem>): IImage {

            fun infoElements(): List<IElement> {

                val legendDescs = listOf(
                    ImgCommons.LegendDesc("Sun and planets of the solar system", IColor.RED, IOpacity.MEDIUM),
                    ImgCommons.LegendDesc("Star, size relative to the sun", IColor.ORANGE, IOpacity.MEDIUM),
                    ImgCommons.LegendDesc("Exoplanet, size relative to solar planets", IColor.GREEN, IOpacity.MEDIUM),
                    ImgCommons.LegendDesc("Exoplanet, unknown size", IColor.GREEN, IOpacity.LOW),
                )
                val description = listOf(
                    "Planetary systems containing one",
                    "planet that has about the same",
                    "distance to its star as the",
                    "earth to the sun.",
                )
                val subTitle = listOf(
                    "Earth-like Distance.",
                    "Creation date: ${ImgCommons.datStr()}",
                )
                val base = -0.03

                return listOf(
                    IUtil.baseText(
                        IUtil.point(1, base), "Known Planetary Systems", ITextSize.L, ITextAnchor.END
                    ),
                    IUtil.multilineText(IUtil.point(1, base + 0.045), subTitle, lineSpacing),
                    ImgCommons.legend(IUtil.point(1, base + 0.105), legendDescs, lineSpacing, textStyle, -0.003),
                    IUtil.multilineText(IUtil.point(1, base + 0.2), description, lineSpacing),
                )
            }


            val imageElements = listOf(
                IUtil.fillRect(
                    IColor.YELLOW,
                    IOpacity.XLOW
                )
            ) + infoElements() + IUtil.equallyDistributedElements(solarSystems) {
                ImgCommons.systemElements(
                    it,
                    maxPlanetDist,
                    textOffsetValue,
                    starSizeFactor,
                    planetSizeFactor,
                    unknownPlanetSize,
                )
            }

            return object : IImage {
                override val page: IPage
                    get() {
                        return IUtil.page(
                            IUtil.borderCanvas(
                                imageElements, 0.1, 0.1, 0.05, 0.08
                            ), pageSize
                        )
                    }
                override val textStyle: ITextStyle
                    get() = textStyle
            }
        }

        val systems = selectSystems()
        val image = createImage(systems)
        SvgRenderer.writeSvg(writer, image)
    }
}

object TestImage {

    @Suppress("UNUSED_PARAMETER")
    fun create(writer: Writer, pageSize: Util.PageSize, catalogue: String?) {

        fun createImage(): IImage {
            data class TextStyle(
                override val fontFamily: IFont,
                override val fontScale: IFontScale,
            ) : ITextStyle

            data class Planet(
                override val origin: IPoint,
                override val color: IColor? = IColor.GREEN,
                override val opacity: IOpacity? = IOpacity.MEDIUM,
                override val radius: Double
            ) : ICircle

            data class Star(
                override val origin: IPoint,
                override val color: IColor? = IColor.ORANGE,
                override val opacity: IOpacity? = IOpacity.MEDIUM,
                override val radius: Double
            ) : ICircle

            data class Rectangle(
                override val origin: IPoint,
                override val width: Double,
                override val height: Double,
                override val color: IColor? = IColor.YELLOW,
                override val opacity: IOpacity? = IOpacity.MEDIUM,
            ) : IRectangle

            data class P(override val x: Double, override val y: Double) : IPoint

            class Text(
                override val size: ITextSize,
                override val origin: IPoint,
                override val text: String,
                override val textAnchor: ITextAnchor = ITextAnchor.START,
                override val color: IColor? = IColor.BLUE,
                override val opacity: IOpacity? = IOpacity.MEDIUM,
            ) : IText

            data class Image(
                override val page: IPage,
                override val textStyle: ITextStyle,
            ) : IImage

            fun square(origin: IPoint): ICollection {

                val size = 0.1

                fun corner(x: Number, y: Number) = object : ICircle {
                    override val radius: Double
                        get() = 0.02
                    override val color: IColor
                        get() = IColor.BLACK
                    override val opacity: IOpacity?
                        get() = null
                    override val origin: IPoint
                        get() = P(x.toDouble() * size, y.toDouble() * size)
                }

                return object : ICollection {
                    override val elements: List<IElement>
                        get() = listOf(
                            corner(-1, -1), corner(1, -1), corner(1, 1), corner(-1, 1)
                        )
                    override val origin: IPoint
                        get() = origin
                }
            }

            val elements = listOf(
                Rectangle(origin = P(0.0, 0.0), width = 1.0, height = 1.0, color = IColor.YELLOW),
                Text(origin = P(0.3, 0.2), size = ITextSize.L, text = "L start anchor"),
                Text(origin = P(0.3, 0.24), size = ITextSize.M, text = "A4 M start anchor"),
                Text(
                    origin = P(0.3, 0.26), size = ITextSize.S, text = "S end anchor", textAnchor = ITextAnchor.END
                ),
                Planet(origin = P(0.5, 0.5), radius = 0.03),
                Rectangle(origin = P(0.5, 0.7), width = 0.01, height = 0.295, color = IColor.GREEN),
                Rectangle(origin = P(0.52, 0.7), width = 0.01, height = 0.29, color = IColor.GREEN),
                square(P(0.5, 0.5)),
                Star(origin = P(0.5, 0.5), radius = 0.01),
            )


            val content = IUtil.borderCanvas(elements, 0.1, 0.2, 0.3, 0.4)
            val page = IUtil.page(content, Util.PageSizeIso.A4)
            val textStyle = TextStyle(fontFamily = IFont.TURRET, fontScale = { size ->
                when (size) {
                    ITextSize.L -> 0.035
                    ITextSize.M -> 0.0125
                    ITextSize.S -> 0.005
                }
            })
            return Image(page, textStyle)
        }


        val i = createImage()
        SvgRenderer.writeSvg(writer, i)
    }
}


object ImgCommons {


    data class LegendDesc(
        val text: String,
        val color: IColor,
        val opacity: IOpacity,
    )

    fun legend(
        origin: IPoint,
        legendDescs: List<LegendDesc>,
        lineSpacing: Double,
        textStyle: ITextStyle,
        circleYOffset: Double
    ): ICollection {
        val fontScale = textStyle.fontScale(ITextSize.M)
        val circleSize = fontScale * 0.5
        val circleXOffset = fontScale * 1.6

        fun circle(legendDesc: LegendDesc): IElement {
            return object : ICircle {
                override val radius: Double
                    get() = circleSize
                override val color: IColor
                    get() = legendDesc.color
                override val opacity: IOpacity
                    get() = legendDesc.opacity
                override val origin: IPoint
                    get() = IUtil.point(circleXOffset, circleYOffset)
            }
        }

        fun text(index: Int, legendDesc: LegendDesc): IElement {
            return object : ICollection {
                override val elements: List<IElement>
                    get() = listOf(
                        IUtil.baseText(IUtil.point(0, 0), legendDesc.text, ITextSize.M, ITextAnchor.END),
                        circle(legendDesc),
                    )
                override val origin: IPoint
                    get() = IUtil.point(0, index * lineSpacing)
            }
        }


        return object : ICollection {
            override val elements: List<IElement>
                get() = legendDescs.withIndex().map { text(it.index, it.value) }
            override val origin: IPoint
                get() = origin
        }
    }

    fun systemElements(
        solarSystem: Util.SolarSystem,
        maxPlanetDist: Double,
        textOffsetValue: Double,
        starSizeFactor: Double,
        planetSizeFactor: Double,
        unknownPlanetSize: Double,
    ): List<IElement> {

        val textOffset = IUtil.point(textOffsetValue, -textOffsetValue)
        val textOffsetEnd = IUtil.point(-textOffsetValue, -textOffsetValue)

        fun isSolarSystem(solarSystem: Util.SolarSystem) = solarSystem.star.names.contains("Sun")

        fun systemName(): IElement {
            return IUtil.baseText(
                text = solarSystem.name, origin = textOffsetEnd, size = ITextSize.S, textAnchor = ITextAnchor.END
            )
        }

        fun star(): IElement {
            fun name(): IElement? {
                val planetName = starName(solarSystem.star, solarSystem.name)
                return planetName?.let { IUtil.baseText(origin = textOffset, text = it, size = ITextSize.S) }
            }

            fun circle(): IElement {
                val starColor = if (isSolarSystem(solarSystem)) IColor.RED
                else IColor.ORANGE
                val (starOpacity, starRadius) = if (solarSystem.star.radius == null) Pair(IOpacity.LOW, starSizeFactor)
                else {
                    Pair(IOpacity.MEDIUM, solarSystem.star.radius * starSizeFactor)
                }
                return object : ICircle {
                    override val radius: Double
                        get() = starRadius
                    override val color: IColor
                        get() = starColor
                    override val opacity: IOpacity
                        get() = starOpacity
                    override val origin: IPoint
                        get() = IUtil.point(0, 0)

                }
            }
            return object : ICollection {
                override val elements: List<IElement>
                    get() = listOfNotNull(circle(), name())
                override val origin: IPoint
                    get() = IUtil.point(0, 0)
            }
        }

        fun planet(planet: Util.Planet, isSolarPlanet: Boolean, maxPlanetDist: Double): IElement {

            val x = (planet.dist ?: 1.0) / maxPlanetDist
            fun circle(): IElement {
                val color = if (isSolarPlanet) IColor.RED else IColor.GREEN
                val radius = (planet.radius ?: unknownPlanetSize) * planetSizeFactor
                return object : ICircle {
                    override val radius: Double
                        get() = radius
                    override val color: IColor
                        get() = color
                    override val opacity: IOpacity
                        get() = if (planet.radius != null) IOpacity.MEDIUM else IOpacity.LOW
                    override val origin: IPoint
                        get() = IUtil.point(0, 0)
                }
            }

            fun name(): IElement? {
                val planetName = planetName(planet, solarSystem.name)
                return planetName?.let { IUtil.baseText(origin = textOffset, text = planetName, size = ITextSize.S) }
            }
            return object : ICollection {
                override val elements: List<IElement>
                    get() = listOfNotNull(circle(), name())
                override val origin: IPoint
                    get() = IUtil.point(x, 0)

            }
        }

        fun line(solarSystem: Util.SolarSystem): IElement {

            val relSize = solarSystem.star.planets.mapNotNull { it.dist }.maxOf { it } / maxPlanetDist
            println("-- line maxPlanerDist: $maxPlanetDist relSize: $relSize")
            val size = min(1.0, relSize)
            return object : ILineHorizontal {
                override val length: Double
                    get() = size
                override val strokeWidth: Double
                    //                get() = 0.02
                    get() = 0.2
                override val color: IColor
                    get() = IColor.BLUE
                override val opacity: IOpacity
                    get() = IOpacity.LOW
                override val origin: IPoint
                    get() = IUtil.point(0, 0)
            }
        }

        val isSolar = isSolarSystem(solarSystem)

        return listOf(line(solarSystem), star(), systemName()) + solarSystem.star.planets.map {
            planet(
                it,
                isSolar,
                maxPlanetDist
            )
        }
    }


    fun datStr(): String {
        val dat = LocalDate.now()
        val fmt = DateTimeFormatter.ofPattern("MMM YYYY")
        return fmt.format(dat)
    }
}