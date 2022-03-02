import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class MainKtTest {

    private val massSun = 1.989e30
    private val secondsInDay = 24.0 * 60 * 60
    private val au = 149597870e3

    @ParameterizedTest
    @CsvSource(
        "87.66, 0.39",
        "226.455, 0.72",
        "365.25, 1.0",
        "686.67, 1.52",
        "4331.865, 5.2",
        "10756.6125, 9.54",
        "30688.305, 19.19",
        "60189.5475, 30.07"
    )
    fun largeSemiAxis2(period: Double, exp: Double) {
        val a = largeSemiAxis(period * secondsInDay, 0.0, massSun)
        assertEquals(exp, a / au, 0.1)
    }



}