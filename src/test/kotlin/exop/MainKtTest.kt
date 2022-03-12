package exop

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class MainKtTest {

    private val massSun = 1.989e30 // kg
    private val secondsInDay = 24.0 * 60 * 60
    private val sol = loadSolarSystem()

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
        val planet = sol.star.planets.first { it.name == name }
        val dist = largeSemiAxis(planet.period!! * secondsInDay, 0.0, massSun)
        assertEquals(expectedDist, dist, 0.1)
    }
}