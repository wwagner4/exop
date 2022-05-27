package exop.ielems

import exop.Util

object IUtil {

    fun <T> equallyDistributedElements(objects: List<T>, mapFunction: (T) -> List<IElement>): List<IElement> {
        fun element(objectsIndexed: IndexedValue<T>, systemCount: Int): IElement {
            val diff = 1.0 / (systemCount - 1)
            val y = objectsIndexed.index * diff
            return object : ICollection {
                override val elements: List<IElement>
                    get() = mapFunction(objectsIndexed.value)
                override val origin: IPoint
                    get() = point(0, y)
            }
        }

        return objects.withIndex().map { element(it, objects.size) }
    }

    fun page(canvas: ICanvas, pageSize: Util.PageSize): IPage = object : IPage {
        override val width: Double
            get() = pageSize.width
        override val height: Double
            get() = pageSize.height
        override val canvas: ICanvas
            get() = canvas
    }

    fun baseText(
        origin: IPoint, text: String, size: ITextSize, textAnchor: ITextAnchor = ITextAnchor.START
    ): IText = object : IText {
        override val text: String
            get() = text
        override val size: ITextSize
            get() = size
        override val textAnchor: ITextAnchor
            get() = textAnchor
        override val color: IColor
            get() = IColor.BLUE
        override val opacity: IOpacity
            get() = IOpacity.FULL
        override val origin: IPoint
            get() = origin
    }

    fun multilineText(
        origin: IPoint, lines: List<String>, lineSpacing: Double
    ): ICollection {
        fun txtElem(index: Int, txt: String): IElement {
            return baseText(point(0, index * lineSpacing), txt, ITextSize.M, ITextAnchor.END)
        }

        return object : ICollection {
            override val elements: List<IElement>
                get() = lines.withIndex().map { txtElem(it.index, it.value) }
            override val origin: IPoint
                get() = origin
        }
    }

    fun borderCanvas(
        elements: List<IElement>, top: Double, right: Double, bottom: Double, left: Double
    ): ICanvas = object : ICanvas {
        override val width: Double
            get() = 1.0 - left - right
        override val height: Double
            get() = 1.0 - top - bottom
        override val elements: List<IElement>
            get() = elements
        override val origin: IPoint
            get() = point(left, top)
    }

    fun fillRect(color: IColor, opacity: IOpacity): IElement {
        return object : IRectangle {
            override val width: Double
                get() = 1.0
            override val height: Double
                get() = 1.0
            override val color: IColor
                get() = color
            override val opacity: IOpacity
                get() = opacity
            override val origin: IPoint
                get() = point(0, 0)
        }
    }

    fun point(x: Number, y: Number): IPoint = object : IPoint {
        override val x: Double
            get() = x.toDouble()
        override val y: Double
            get() = y.toDouble()
    }
}