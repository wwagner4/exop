package exop.ielems

import exop.Font
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.Namespace
import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter
import java.io.Writer

object ISvgRenderer {

    private val svgNamespace = Namespace.getNamespace("http://www.w3.org/2000/svg")
    private val fontFamily = Font.Family.league

    fun writeSvg(writer: Writer, image: IImage) {

        fun z(element: Element): Int {
            if (element.name == "text") return 10
            return 0
        }

        fun addStyle(root: Element) {
            if (fontFamily.def.import != null) {
                val style = svgElem("style")
                style.text = fFontFamilyStyle(image.textStyle.fontFamily)
                val defs = svgElem("defs")
                defs.addContent(style)
                root.addContent(defs)
            }
        }

        val root = svgElem("svg")
        // root.setAttribute("viewBox", "0 0 ${pageSize.widthMm} ${pageSize.heightMm}")
        root.setAttribute("width", fDoubleMm(image.page.width))
        root.setAttribute("height", fDoubleMm(image.page.height))
        addStyle(root)
        val elems = createDom(image.page.canvas, image.page, emptyList(), image.textStyle)
        elems.sortedBy { z(it) }.forEach { root.addContent(it) }

        val document = Document()
        document.setContent(root)
        val outputter = XMLOutputter()
        outputter.format = Format.getPrettyFormat()
        outputter.output(document, writer)
    }

    private fun createDom(
        element: IElement,
        page: IPage,
        collections: List<ICollection>,
        textStyle: ITextStyle
    ): List<Element> {

        return when (element) {
            is IText -> {
                val elem = svgElem("text")
                elem.setAttribute("x", fDoubleMm(Util.xAbs(element.origin.x, page, collections)))
                elem.setAttribute("y", fDoubleMm(Util.yAbs(element.origin.y, page, collections)))
                elem.setAttribute("fill", fColor(element.color))
                elem.setAttribute("opacity", fOpacity(element.opacity))
                elem.setAttribute("font-family", fFontFamily(textStyle.fontFamily))
                elem.setAttribute("font-size", fFontSize(element.size, page.height, textStyle))
                if (element.textAnchor == ITextAnchor.END) elem.setAttribute("text-anchor", "end")
                elem.text = element.text
                listOf(elem)
            }
            is ICircle -> {
                val xAbs = Util.xAbs(element.origin.x, page, collections)
                val yAbs = Util.yAbs(element.origin.y, page, collections)
                val elem = svgElem("circle")
                elem.setAttribute("cx", fDoubleMm(xAbs))
                elem.setAttribute("cy", fDoubleMm(yAbs))
                elem.setAttribute("r", fDoubleMm(element.radius * page.height))
                elem.setAttributeNullable("opacity", fOpacity(element.opacity))
                elem.setAttributeNullable("fill", fColor(element.color))
                listOf(elem)
            }
            is IRectangle -> {
                val c = fColor(element.color)
                val elem = svgElem("rect")
                elem.setAttribute("x", fDoubleMm(Util.xAbs(element.origin.x, page, collections)))
                elem.setAttribute("y", fDoubleMm(Util.yAbs(element.origin.y, page, collections)))
                elem.setAttribute("width", fDoubleMm(Util.wAbs(element.width, page, collections)))
                elem.setAttribute("height", fDoubleMm(Util.hAbs(element.height, page, collections)))
                elem.setAttributeNullable("opacity", fOpacity(element.opacity))
                elem.setAttribute("style", "fill:$c;")
                listOf(elem)
            }
            is ICanvas -> {
                element.elements.flatMap {
                    createDom(it, page, collections + listOf(element), textStyle)
                }
            }
            is ICollection -> {
                element.elements.flatMap {
                    createDom(it, page, collections + listOf(element), textStyle)
                }
            }
            is ILineHorizontal -> {
                val elem = svgElem("line")
                elem.setAttribute("x1", fDoubleMm(Util.xAbs(element.origin.x, page, collections)))
                elem.setAttribute("y1", fDoubleMm(Util.yAbs(element.origin.y, page, collections)))
                elem.setAttribute(
                    "x2",
                    fDoubleMm(Util.xAbs(element.origin.x + element.length, page, collections))
                )
                elem.setAttribute("y2", fDoubleMm(Util.yAbs(element.origin.y, page, collections)))
                elem.setAttributeNullable("opacity", fOpacity(element.opacity))
                val sw = fDoubleMm(element.strokeWidth)
                elem.setAttribute("style", "stroke:${element.color};stroke-width:$sw")
                listOf(elem)
            }
            else -> {
                throw NotImplementedError(
                    "No implementation for ${element.javaClass.toString()}"
                )
            }
        }

    }

    private fun Element.setAttributeNullable(name: String, value: String?) {
        if (value != null) {
            this.setAttribute(name, value)
        }
    }

    private fun svgElem(name: String): Element {
        val elem = Element(name)
        elem.namespace = svgNamespace
        return elem
    }

    private fun fDoubleMm(value: Double): String {
        return "%.3fmm".format(value)
    }

    private fun fDouble(value: Double): String {
        return "%.3f".format(value)
    }

    private fun fFontSize(size: ITextSize, topHeight: Double, textStyle: ITextStyle): String {
        return when (size) {
            ITextSize.L -> fDoubleMm(topHeight * textStyle.fontScale(size))
            ITextSize.M -> fDoubleMm(topHeight * textStyle.fontScale(size))
            ITextSize.S -> fDoubleMm(topHeight * textStyle.fontScale(size))
        }
    }

    private fun fFontFamily(font: IFont): String {
        return when (font) {
            IFont.TURRET -> Font.Family.turretRoad.def.fontName
        }
    }

    private fun fFontFamilyStyle(font: IFont): String {
        return when (font) {
            IFont.TURRET -> Font.Family.turretRoad.def.import!!
        }
    }

    private fun fColor(color: IColor?): String? {
        return when (color) {
            IColor.BLUE -> "blue"
            IColor.RED -> "red"
            IColor.GREEN -> "green"
            IColor.ORANGE -> "orange"
            IColor.YELLOW -> "yellow"
            IColor.BLACK -> "black"
            IColor.WHITE -> "white"
            null -> null
        }
    }

    private fun fOpacity(opacity: IOpacity?): String? {
        return when (opacity) {
            IOpacity.LOW -> fDouble(0.4)
            IOpacity.XLOW -> fDouble(0.1)
            IOpacity.MEDIUM -> fDouble(0.7)
            IOpacity.FULL -> fDouble(1.0)
            null -> null
        }
    }

    object Util {

        private fun sum(v: IndexedValue<List<Double>>): Double {
            return v.value.slice(0..v.index).reduce { acc, i -> acc * i }
        }

        /**
         * Calculates the distance from the zero point of a page in a hierarchy of relative canvases
         */
        fun absDistance(base: Double, canvasWith: List<Double>, canvasOffset: List<Double>, relOffset: Double): Double {
            return if (canvasWith.isEmpty()) relOffset
            else zippedProduct(base, canvasWith, canvasOffset, relOffset).sum()
        }

        /**
         * Calculates the absolute width inside a hierarchy of relative canvases
         */
        fun absWidth(base: Double, canvasWith: List<Double>, canvasOffset: List<Double>, relWidth: Double): Double {
            return if (canvasWith.isEmpty()) relWidth
            else zippedProduct(base, canvasWith, canvasOffset, relWidth).last()
        }

        private fun zippedProduct(
            base: Double,
            canvasWith: List<Double>,
            canvasOffset: List<Double>,
            relOffset: Double
        ): List<Double> {
            val widthList = listOf(base) + canvasWith
            val offsetLists = canvasOffset + listOf(relOffset)
            val indexed = List(widthList.size) { widthList }.withIndex()
            val indexedSum = indexed.map(::sum)
            val zipped = indexedSum.zip(offsetLists)
            return zipped.map { it.first * it.second }
        }

        private fun width(coll: ICollection): Double {
            return when (coll) {
                is ICanvas -> coll.width
                else -> 1.0
            }
        }

        private fun height(coll: ICollection): Double {
            return when (coll) {
                is ICanvas -> coll.height
                else -> 1.0
            }
        }

        fun xAbs(xRel: Double, page: IPage, collections: List<ICollection>): Double {
            val widthList = collections.map { width(it) }
            val offsetList = collections.map { it.origin.x }
            return absDistance(page.width, widthList, offsetList, xRel)
        }

        fun wAbs(wRel: Double, page: IPage, collections: List<ICollection>): Double {
            val widthList = collections.map { width(it) }
            val offsetList = collections.map { it.origin.x }
            return absWidth(page.width, widthList, offsetList, wRel)
        }

        fun yAbs(yRel: Double, page: IPage, collections: List<ICollection>): Double {
            val heightList = collections.map { height(it) }
            val offsetList = collections.map { it.origin.y }
            return absDistance(page.height, heightList, offsetList, yRel)
        }

        fun hAbs(hRel: Double, page: IPage, collections: List<ICollection>): Double {
            val heightList = collections.map { height(it) }
            val offsetList = collections.map { it.origin.x }
            return absWidth(page.height, heightList, offsetList, hRel)
        }
    }
}