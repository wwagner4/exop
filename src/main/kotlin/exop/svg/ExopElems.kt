package exop.svg

import org.jdom2.Element

class ExopElems(private val svgBasic: Basic) {

    fun planet(center: Basic.SvgPoint, radius: Double): Element {
        return svgBasic.circle(center, radius, "green", 0.8)
    }

    fun planetUnknownRadius(center: Basic.SvgPoint, radius: Double): Element {
        return svgBasic.circle(center, radius, "green", 0.4)
    }

    fun solarPlanet(center: Basic.SvgPoint, radius: Double): Element {
        return svgBasic.circle(center, radius, "red", 0.9)
    }

    fun star(center: Basic.SvgPoint, radius: Double): Element {
        return svgBasic.circle(center, radius, "orange", 0.8)
    }

    fun sun(center: Basic.SvgPoint, radius: Double): Element {
        return svgBasic.circle(center, radius, "red", 0.9)
    }

    fun planetLine(from: Basic.SvgPoint, to: Basic.SvgPoint, strokeWith: Double): Element {
        return svgBasic.line(from, to, strokeWith, "green")
    }

    fun nameSystem(
        origin: Basic.SvgPoint,
        text: String?,
        size: Double,
        offset: Double,
        textStyle: Basic.TextStyle,
        anchorLeft: Boolean = true
    ): Element? {
        val xOff = if (anchorLeft) -offset else offset
        val origin1 = Basic.SvgPoint(
            origin.x.toDouble() + xOff, origin.y.toDouble() - offset
        )
        return text?.let {
            svgBasic.text(
                text, origin1, size, textStyle, anchorLeft
            )
        }
    }

    fun legendElems(
        xBase: Number,
        yBase: Number,
        imgOffsetX: Double,
        imgOffsetY: Double,
        imgSize: Double,
        textStyle: Basic.TextStyle,
        textSize: Double,
        textAnchorLeft: Boolean = true
    ): List<Element> {
        data class LegendElem(
            val text: String, val fElem: (Basic.SvgPoint, Double) -> Element
        )

        val vDist = textSize * 1.5

        fun line(elem: LegendElem, i: Int, textAnchorLeft: Boolean): List<Element> {
            val x = if (textAnchorLeft) xBase.toDouble() + imgOffsetX
            else xBase.toDouble() - imgOffsetX
            val y = yBase.toDouble() + i * vDist
            val txtOrigin = Basic.SvgPoint(xBase, y)
            val imgOrigin = Basic.SvgPoint(x, y + imgOffsetY)
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
        textStyle: Basic.TextStyle,
        textSize: Double,
        textAnchorLeft: Boolean = true
    ): List<Element> {

        val vDist = textSize * 1.6

        fun line(line: String, i: Int): List<Element> {
            val y = yBase.toDouble() + i * vDist
            val txtOrigin = Basic.SvgPoint(xBase, y)
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