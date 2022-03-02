import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.toList

fun main() {
    val catPath = System.getenv("CATALOGUE")
    println("catpath = $catPath")

    val catDir = Path.of(catPath)
    val sysFiles = catFiles(catDir, "systems")
    val syskFiles = catFiles(catDir, "systems_kepler")
    val allFiles = sysFiles + syskFiles
    println("Catalogs sys:${sysFiles.size} kepler:${syskFiles.size}  all:${allFiles.size} ")

}

private fun catFiles(baseDir: Path, catName: String): List<Path> {
    val sysDir = baseDir.resolve(catName)
    return Files.list(sysDir).toList().filter { it.fileName.toString().endsWith("xml") }
}