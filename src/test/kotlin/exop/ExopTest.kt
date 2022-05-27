package exop

import exop.Util.loadSolarSystem
import exop.ielems.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import exop.ielems.SvgRenderer.Util as ru

internal class ExopTest {

    private val massSun = 1.989e30 // kg
    private val secondsInDay = 24.0 * 60 * 60
    private val sol = loadSolarSystem(null)

    @ParameterizedTest
    @CsvSource(
        "Mercury, 0.39",
        "Venus, 0.72",
        "Earth, 1.0",
        "Mars, 1.52",
        "Jupiter, 5.2",
        "Saturn, 9.54",
        "Uranus, 19.19",
        "Neptune, 30.07"
    )
    fun largeSemiAxis(name: String, expectedDist: Double) {
        val planet = sol.star.planets.first { Names.planetName(it, it.systName) == name }
        val dist = Util.largeSemiAxis(planet.period!! * secondsInDay, 0.0, massSun)
        assertEquals(expectedDist, dist, 0.1)
    }


    private fun splitDoubles(instr: String?): List<Double> {
        if (instr == null) return emptyList()
        return instr.split("\\s".toRegex()).map { it.toDouble() }
    }

    @ParameterizedTest
    @CsvSource(
        "100.0, 0.5 0.5, 0.1 0.1, 0.5, 27.5",
        "200.0, 0.5 0.5, 0.1 0.1, 0.5, 55.0",
        "200.0, 0.5, 0.1, 0.5, 70.0",
        "200.0, , , 23.0, 23.0",
    )
    fun testAbsDistance(
        base: Double,
        canvasWithStr: String?,
        canvasOffsetStr: String?,
        offset: Double,
        expected: Double
    ) {
        val widths = splitDoubles(canvasWithStr)
        val offsets = splitDoubles(canvasOffsetStr)
        val result = ru.absDistance(base, widths, offsets, offset)
        assertEquals(expected, result)
    }

    @ParameterizedTest
    @CsvSource(
        "200.0, 0.5, 0.1, 0.5, 50.0",
        "200.0, 0.5, 0.1, 0.4, 40.0",
        "200.0, , , 23.0, 23.0",
    )
    fun testAbsWidth(base: Double, canvasWithStr: String?, canvasOffsetStr: String?, offset: Double, expected: Double) {
        val widths = splitDoubles(canvasWithStr)
        val offsets = splitDoubles(canvasOffsetStr)
        val result = ru.absWidth(base, widths, offsets, offset)
        assertEquals(expected, result)
    }

    @Test
    fun `test x abs size with collection`() {
        val collElements = emptyList<IElement>()

        val collection = object : ICollection {
            override val elements: List<IElement>
                get() = collElements
            override val origin: IPoint
                get() = IUtil.point(0.1, 0.1)
        }

        val ps = object : Util.PageSize {
            override val width: Double
                get() = 100.0
            override val height: Double
                get() = 100.0
            override val name: String
                get() = "Testsize"
        }
        val canvas = object : ICanvas {
            override val width: Double
                get() = 0.8
            override val height: Double
                get() = 0.8
            override val elements: List<IElement>
                get() = listOf(collection)
            override val origin: IPoint
                get() = IUtil.point(0.1, 0.1)
        }
        val page = IUtil.page(canvas, ps)
        val collections = listOf(canvas, collection)

        assertEquals(34.0, ru.xAbs(0.2, page, collections))
    }

    @Test
    fun `test y abs size with collection`() {
        val collElements = emptyList<IElement>()

        val collection = object : ICollection {
            override val elements: List<IElement>
                get() = collElements
            override val origin: IPoint
                get() = IUtil.point(0.1, 0.1)
        }

        val ps = object : Util.PageSize {
            override val width: Double
                get() = 100.0
            override val height: Double
                get() = 200.0
            override val name: String
                get() = "Testsize"
        }
        val canvas = object : ICanvas {
            override val width: Double
                get() = 0.8
            override val height: Double
                get() = 0.8
            override val elements: List<IElement>
                get() = listOf(collection)
            override val origin: IPoint
                get() = IUtil.point(0.1, 0.1)
        }
        val page = IUtil.page(canvas, ps)
        val collections = listOf(canvas, collection)

        assertEquals(68.0, ru.yAbs(0.2, page, collections))
    }


}