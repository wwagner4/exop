package exop

import org.jdom2.Element
import org.jdom2.input.SAXBuilder
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.absolute
import kotlin.io.path.name
import kotlin.math.floor

object Util {

    private const val massSun: Double = 1.989e30 // kg
    private const val secondsInDay = 24.0 * 60 * 60

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
        val planets: List<Planet>
    )

    data class SolarSystem(
        val name: String,
        val star: Star,
    )


    @Suppress("unused")
    enum class PageSize(private val width: Double, private val height: Double) {
        A0(841.0, 1189.0),
        A1(594.0, 841.0),
        A2(420.0, 594.0),
        A3(297.0, 420.0),
        A4(210.0, 297.0),
        A5(148.0, 210.0);

        val widthMm: Double get() = floor(width * 0.999)
        val heightMm: Double get() = floor(height * 0.999)
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

    private fun readSystem(file: Path): SolarSystem? {
        val systName = file.fileName.toString().substringBefore(".")
        val db = SAXBuilder()
        val doc = db.build(file.toFile())
        val solSys = doc.rootElement
        val stars = solSys.children.filter { it.name == "star" }.map { toStar(it, systName) }
        val star = when {
            stars.isEmpty() -> null
            stars.size == 1 && stars[0].planets.isNotEmpty() -> stars[0]
            stars.size == 1 -> null
            else -> throw IllegalStateException("System $systName has more than one sun")
        }
        return star?.let { SolarSystem(systName, it) }
    }

    fun loadSolarSystem(output: String?): SolarSystem {
        val path = catFiles(output).first { it.name == "Sun.xml" }
        return readSystem(path) ?: throw IllegalStateException("found no data at $path")
    }

    fun loadCatalog(output: String?, maxNumber: Int = Int.MAX_VALUE): List<SolarSystem> {
        val files = catFiles(output).filter { it.fileName.toString() != "Sun.xml" }
        return files.take(maxNumber).mapNotNull { readSystem(it) }
    }

    private fun catFiles(cliOutDir: String?): List<Path> {

        fun catFiles(baseDir: Path, catName: String): List<Path> {
            val sysDir = baseDir.resolve(catName)
            return Files.list(sysDir).toList().filter { it.fileName.toString().endsWith("xml") }
        }

        val catDir = catDir(cliOutDir)
        val catNames = listOf("systems", "systems_kepler")
        return catNames.flatMap { catFiles(catDir, it) }
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

    private fun toStar(starElem: Element, systName: String): Star {
        val starMass = toDouble(starElem, "mass")
        val starNames = starElem.children.filter { it.name == "name" }.map { it.text }
        return Star(
            names = starNames,
            radius = toDouble(starElem, "radius"),
            planets = starElem.children.filter { it.name == "planet" }
                .map { toPlanet(it, systName = systName, starMass = starMass) },
            mass = starMass,
        )
    }

    private fun toPlanet(
        elem: Element, systName: String, starMass: Double?
    ): Planet {
        val names = elem.children.filter { it.name == "name" }.map { it.text }
        val planetPeriod = toDouble(elem, "period")

        fun dist(): Double? {
            if (planetPeriod != null && starMass != null) return ExopImages.largeSemiAxis(
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


}