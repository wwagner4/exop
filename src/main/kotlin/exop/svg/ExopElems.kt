package exop.svg

import exop.ExopImages
import org.jdom2.Element

class ExopElems(private val svgBasic: Basic) {

    fun planet(center: ExopImages.Point, radius: Double): Element {
        return svgBasic.circle(center, radius, "green", 0.8)
    }

    fun planetUnknownRadius(center: ExopImages.Point, radius: Double): Element {
        return svgBasic.circle(center, radius, "green", 0.4)
    }

    fun solarPlanet(center: ExopImages.Point, radius: Double): Element {
        return svgBasic.circle(center, radius, "red", 0.9)
    }

    fun star(center: ExopImages.Point, radius: Double): Element {
        return svgBasic.circle(center, radius, "orange", 0.8)
    }

    fun sun(center: ExopImages.Point, radius: Double): Element {
        return svgBasic.circle(center, radius, "red", 0.9)
    }

    fun planetLine(from: ExopImages.Point, to: ExopImages.Point, strokeWith: Double): Element {
        return svgBasic.line(from, to, strokeWith, "green")
    }

    fun nameSystem(
        origin: ExopImages.Point,
        text: String,
        size: Double,
        offset: Double,
        textStyle: ExopImages.TextStyle,
        anchorLeft: Boolean = true
    ): Element {
        val xOff = if (anchorLeft) -offset else offset
        val origin1 = ExopImages.Point(
            origin.x.toDouble() + xOff, origin.y.toDouble() - offset
        )
        return svgBasic.text(
            text, origin1, size, textStyle, anchorLeft
        )
    }

    fun legendElems(
        xBase: Number,
        yBase: Number,
        imgOffsetX: Double,
        imgOffsetY: Double,
        imgSize: Double,
        textStyle: ExopImages.TextStyle,
        textSize: Double,
        textAnchorLeft: Boolean = true
    ): List<Element> {
        data class LegendElem(
            val text: String, val fElem: (ExopImages.Point, Double) -> Element
        )

        val vDist = textSize * 1.5

        fun line(elem: LegendElem, i: Int, textAnchorLeft: Boolean): List<Element> {
            val x = if (textAnchorLeft) xBase.toDouble() + imgOffsetX
            else xBase.toDouble() - imgOffsetX
            val y = yBase.toDouble() + i * vDist
            val txtOrigin = ExopImages.Point(xBase, y)
            val imgOrigin = ExopImages.Point(x, y + imgOffsetY)
            return listOf(
                elem.fElem(imgOrigin, imgSize),
                svgBasic.text(
                    elem.text,
                    txtOrigin,
                    size = textSize,
                    textStyle = textStyle,
                    textAnchorLeft = textAnchorLeft
                ),
            )
        }

        val texts = listOf(
            LegendElem("sun and planets of the solar system", ::sun),
            LegendElem("star, size relative to the sun", ::star),
            LegendElem("exoplanet, size relative to solar planets", ::planet),
            LegendElem("exoplanet, unknown size", ::planetUnknownRadius),
        )
        return texts.withIndex().flatMap { (i, t) -> line(t, i, textAnchorLeft) }
    }

    fun multilineText(
        lines: List<String>,
        xBase: Number,
        yBase: Number,
        textStyle: ExopImages.TextStyle,
        textSize: Double,
        textAnchorLeft: Boolean = true
    ): List<Element> {

        val vDist = textSize * 1.6

        fun line(line: String, i: Int): List<Element> {
            val y = yBase.toDouble() + i * vDist
            val txtOrigin = ExopImages.Point(xBase, y)
            return listOf(
                svgBasic.text(
                    line,
                    txtOrigin,
                    size = textSize,
                    textStyle = textStyle,
                    textAnchorLeft = textAnchorLeft
                ),
            )
        }

        return lines.withIndex().flatMap { (i, t) -> line(t, i) }
    }

}