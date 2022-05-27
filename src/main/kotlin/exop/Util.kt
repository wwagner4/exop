package exop

import org.jdom2.Element
import org.jdom2.input.SAXBuilder
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.io.path.absolute
import kotlin.io.path.name
import kotlin.math.PI
import kotlin.math.pow

object Util {

    const val massSun: Double = 1.989e30 // kg
    const val secondsInDay = 24.0 * 60 * 60
    const val au = 149597870e3 // m
    const val earthDist = 1.0 // au

    data class Planet(
        val names: List<String>,
        val systName: String,
        val dist: Double?, // in au (astronomic units)
        val radius: Double?, // in radius jupiter. radius jupiter = 71492km
        val period: Double?  // in days
    )

    data class Star(
        val names: List<String>,
        val radius: Double?, // in radius sun. radius sun = 696342km
        val mass: Double?, // in solar masses
        val planets: List<Planet>,
        val binaryStarSeparation: Double? = null, // in au (astronomical units)
    )

    data class SolarSystem(
        val name: String,
        val star: Star,
    )

    interface PageSize {
        val width: Double
        val height: Double
        val name: String
    }

    @Suppress("unused")
    enum class PageSizeIso(override val width: Double, override val height: Double) : PageSize {
        A0(841.0, 1189.0),
        A1(594.0, 841.0),
        A2(420.0, 594.0),
        A3(297.0, 420.0),
        A4(210.0, 297.0),
        A5(148.0, 210.0);
    }

    fun outDir(default: String?): Path {
        if (default == null) {
            val out = Path.of("/out")
            if (Files.exists((out))) return out
            val target = Path.of("target", "images")
            if (Files.notExists(target)) Files.createDirectories(target)
            return target
        } else {
            val path = Path.of(default)
            if (Files.notExists(path)) throw IllegalStateException("${path.absolute()} does not exist")
            if (!Files.isDirectory(path)) throw IllegalStateException("${path.absolute()} is not a directory")
            return path
        }
    }

    private fun readSystem(file: Path): List<SolarSystem> {
        val systName = file.fileName.toString().substringBefore(".")
        val db = SAXBuilder()
        val doc = db.build(file.toFile())
        val solSys = doc.rootElement
        return parseElement(solSys, systName)
    }


    private fun parseElement(
        element: Element,
        systemName: String,
        binaryStarSeparation: Double? = null
    ): List<SolarSystem> {
        return when (element.name) {
            "star" -> listOf(
                SolarSystem(
                    systemName,
                    toStar(element, systemName, binaryStarSeparation = binaryStarSeparation)
                )
            )
            "system" -> element.children.flatMap { parseElement(it, systemName) }
            "binary" -> element.children.flatMap {
                parseElement(
                    it,
                    systemName,
                    binaryStarSeparation = separation(element)
                )
            }
            else -> emptyList()
        }
    }


    fun separation(binarySystem: Element): Double? {

        fun separationAu(el: Element): Double? {
            if (el.name == "separation") {
                if (el.attributes.filter { it.name == "unit" && it.value == "AU" }.isNotEmpty()) {
                    if (el.value.isNotEmpty()) return el.value.toDouble()
                }
            }
            return null
        }

        val separations = binarySystem.getChildren("separation").mapNotNull { separationAu(it) }
        return if (separations.isEmpty()) null
        else if (separations.size == 1) separations[0]
        else throw IllegalStateException("Binary system with more than one separation of unit AU")
    }


    fun loadSolarSystem(catalogue: String?): SolarSystem {
        val path = catFiles(catalogue).first { it.name == "Sun.xml" }
        return readSystem(path)[0]
    }

    fun loadCatalog(catalogueDir: String?, maxNumber: Int = Int.MAX_VALUE): List<SolarSystem> {
        val files = catFiles(catalogueDir).filter { it.fileName.toString() != "Sun.xml" }
        return files.take(maxNumber).flatMap { readSystem(it) }
    }

    fun catFiles(catalogueDir: String?): List<Path> {

        fun catFiles(baseDir: Path, catName: String): List<Path> {
            val sysDir = baseDir.resolve(catName)
            return Files.list(sysDir).toList().filter { it.fileName.toString().endsWith("xml") }
        }

        val cataloguePath = catDir(catalogueDir)
        val catNames = listOf("systems", "systems_kepler")
        return catNames.flatMap { catFiles(cataloguePath, it) }
    }

    fun catDir(cliOutDir: String?): Path {

        fun default(): String {
            val dir = Path.of("..", "open_exoplanet_catalogue")
            if (Files.notExists(dir)) throw IllegalStateException("${dir.absolute()} must exist or environment variable CATALOGUE must be defined")
            return dir.absolute().toString()
        }

        return if (cliOutDir.isNullOrBlank()) {
            val catPath = System.getenv("CATALOGUE") ?: default()
            Path.of(catPath)
        } else {
            Path.of(cliOutDir)
        }
    }

    private fun toStar(starElem: Element, systName: String, binaryStarSeparation: Double?): Star {
        val starMass = toDouble(starElem, "mass")
        val starNames = starElem.children.filter { it.name == "name" }.map { it.text }
        return Star(
            names = starNames,
            radius = toDouble(starElem, "radius"),
            planets = starElem.children.filter { it.name == "planet" }
                .map { toPlanet(it, systName = systName, starMass = starMass) },
            mass = starMass,
            binaryStarSeparation = binaryStarSeparation,
        )
    }

    private fun toPlanet(
        elem: Element, systName: String, starMass: Double?
    ): Planet {
        val names = elem.children.filter { it.name == "name" }.map { it.text }
        val planetPeriod = toDouble(elem, "period")

        fun dist(): Double? {
            if (planetPeriod != null && starMass != null) return largeSemiAxis(
                planetPeriod * secondsInDay, 0.0, starMass * massSun
            )
            return null
        }
        return Planet(
            names = names,
            radius = toDouble(elem, "radius"),
            period = planetPeriod,
            dist = dist(),
            systName = systName
        )
    }

    private fun toDouble(elem: Element, name: String): Double? {
        return elem.children.filter { it.name == name }.map {
            when {
                it.text.isEmpty() -> null
                else -> it.text.toDouble()
            }
        }.firstOrNull()
    }

    fun String.runCommand(workingDir: Path): String? {
        try {
            val parts = this.split("\\s".toRegex())
            val proc = ProcessBuilder(*parts.toTypedArray())
                .directory(workingDir.toFile())
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

            proc.waitFor(5, TimeUnit.SECONDS)
            return proc.inputStream.bufferedReader().readText()
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    data class LogOutput(
        val date: String,
        val text: String,
    )

    fun parseGitLogOutput(output: String): List<LogOutput> {
        fun groups(commit: String): List<String>? {
            val clean = commit.replace("\n", "<a>")
            val regex = Regex("<a>Date:(.*)<a><a>(.*)<a><a>?")
            val match = regex.find(clean)
            val r1 = match?.groups?.toList()?.map { it?.value ?: "" }
            if (r1 != null) return r1
            val regex1 = Regex("<a>Date:(.*)<a><a>(.*)<a>?")
            val match1 = regex1.find(clean)
            val r2 = match1?.groups?.toList()?.map { it?.value ?: "" }
            if (r2 != null) return r2
            println("could not match '$clean'")
            return null
        }

        val commits = output.split("commit").filter { it.length > 0 }
        val lines = commits.map {
            val grps = groups(it)
            if (grps != null) {
                val date = grps[1].trim().replace("-", "&#8209;")
                val text = splitLongWords(grps[2].trim())
                LogOutput(date, text)
            } else null
        }.filterNotNull()
        return lines
    }

    fun splitLongWords(text: String): String {

        fun splitLong(word: String): String {
            return word.chunked(30).joinToString("<br/>")
        }
        return try {
            println("splitting '$text'")
            val splitted = text.split(" ").map { splitLong(it) }.joinToString(" ")
            println("splitted $text into $splitted")
            splitted
        } catch (e: Exception) {
            println("Error splitting '$text'")
            text
        }
    }

    fun nowMonthFormatted(): String {
        val dtf = DateTimeFormatter.ofPattern("MMM yyyy")
        val now = LocalDateTime.now()
        return dtf.format(now)
    }

    fun doubleFormatted(v: Double?, size: Int): String {
        if (v == null) {
            val f = "%${size}s"
            return f.format("-")
        }
        val f = "%${size}.2f"
        return f.format(v)
    }

    fun maxDistancePlanet(star: Star): Double? {
        return star.planets.mapNotNull { it.dist }.maxOrNull()
    }

    /**
     * Calculates the large semi axis of a planet
     *
     * @param period in seconds
     * @param mass1 in kg
     * @param mass2 in kg
     * @return large semi axis (distance) in au (astronomic units)
     */
    fun largeSemiAxis(period: Double, mass1: Double, mass2: Double): Double {
        val g = 6.667408e-11
        val m = mass1 + mass2
        val a = period * period * g * m / (PI * PI * 4.0)
        return a.pow(1.0 / 3) / au
    }


}