package exop.ielems

enum class IColor {
    ORANGE, GREEN, BLUE, YELLOW, BLACK, RED, WHITE
}

enum class IOpacity {
    FULL, MEDIUM, LOW, XLOW
}

enum class ITextSize {
    S, M, L
}

enum class IFont {
    TURRET
}

enum class ITextAnchor {
    START, END
}

interface IImage {
    val page: IPage
    val textStyle: ITextStyle
}


typealias IFontScale = (ITextSize) -> Double

interface ITextStyle {
    val fontFamily: IFont
    val fontScale: IFontScale
}

interface IPage {
    val width: Double // in mm
    val height: Double // in mm
    val canvas: ICanvas
}

interface ICollection : IElement {
    val elements: List<IElement>
}

interface ICanvas : ICollection {
    val width: Double // [0, 1] relative to enclosing canvas
    val height: Double // [0, 1] relative to enclosing canvas
}

interface IPoint {
    val x: Double
    val y: Double
}

interface IElement {
    val origin: IPoint  // x, and y [0, 1] relative to enclosing canvas
}

interface IDrawableElement : IElement {
    val color: IColor?
    val opacity: IOpacity?
}

interface IText : IDrawableElement {
    val text: String
    val size: ITextSize
    val textAnchor: ITextAnchor
}

interface ILineHorizontal : IDrawableElement {
    val length: Double
    val strokeWidth: Double
}

interface ICircle : IDrawableElement {
    val radius: Double
}

interface IRectangle : IDrawableElement {
    val width: Double
    val height: Double
}

