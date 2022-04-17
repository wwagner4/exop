package exop.svg

import exop.ExopImages
import exop.Font
import exop.Util
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.Namespace
import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter
import java.io.Writer

class Basic(private val unit: String) {

    private val svgNamespace = Namespace.getNamespace("http://www.w3.org/2000/svg")

    fun writeSvg(
        writer: Writer, pageSize: Util.PageSize, fontFamily: Font.Family, createElems: () -> List<Element>
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
        // root.setAttribute("viewBox", "0 0 ${pageSize.widthMm} ${pageSize.heightMm}")
        root.setAttribute("width", pageSize.widthMm.f(unit))
        root.setAttribute("height", pageSize.heightMm.f(unit))
        addStyle(root)
        createElems().sortedBy { z(it) }.forEach { root.addContent(it) }

        val document = Document()
        document.setContent(root)
        val outputter = XMLOutputter()
        outputter.format = Format.getPrettyFormat()
        outputter.output(document, writer)
    }

    fun circle(
        center: ExopImages.Point, radius: Double, color: String, opacity: Double
    ): Element {
        val elem = svgElem("circle")
        elem.setAttribute("cx", center.x.f(unit))
        elem.setAttribute("cy", center.y.f(unit))
        elem.setAttribute("r", radius.f(unit))
        elem.setAttribute("opacity", opacity.f())
        elem.setAttribute("fill", color)
        return elem
    }

    fun line(
        from: ExopImages.Point, to: ExopImages.Point, strokeWidth: Double, color: String, opacity: Double = 0.8
    ): Element {
        val elem = svgElem("line")
        elem.setAttribute("x1", from.x.f(unit))
        elem.setAttribute("y1", from.y.f(unit))
        elem.setAttribute("x2", to.x.f(unit))
        elem.setAttribute("y2", to.y.f(unit))
        elem.setAttribute("opacity", opacity.f())
        elem.setAttribute("style", "stroke:$color;stroke-width:${strokeWidth.f(unit)}")
        return elem
    }

    fun rect(
        origin: ExopImages.Point,
        width: Double,
        height: Double,
        color: String,
        opacity: Double = 1.0,
    ): Element {
        val elem = svgElem("rect")
        elem.setAttribute("x", origin.x.f(unit))
        elem.setAttribute("y", origin.y.f(unit))
        elem.setAttribute("width", width.f(unit))
        elem.setAttribute("height", height.f(unit))
        elem.setAttribute("opacity", opacity.f())
        elem.setAttribute("style", "fill:$color;")
        return elem
    }

    fun text(
        text: String,
        origin: ExopImages.Point,
        size: Double,
        textStyle: ExopImages.TextStyle,
        textAnchorLeft: Boolean = false
    ): Element {
        val elem = svgElem("text")
        elem.setAttribute("x", origin.x.f(unit))
        elem.setAttribute("y", origin.y.f(unit))
        elem.setAttribute("fill", textStyle.color)
        elem.setAttribute("opacity", textStyle.opacity.f())
        elem.setAttribute("font-family", textStyle.fontFamily.def.fontName)
        elem.setAttribute("font-size", size.f(unit))
        if (textAnchorLeft) elem.setAttribute("text-anchor", "end")
        elem.text = text
        return elem
    }

    private fun svgElem(name: String): Element {
        val elem = Element(name)
        elem.namespace = svgNamespace
        return elem
    }

    private fun Number.f(unit: String = ""): String {
        return "%.3f$unit".format(this.toDouble())
    }


}