package exop

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolute

object Util {

    @Suppress("unused")
    enum class PageSize(private val width: Double, private val height: Double) {
        A0(841.0, 1189.0),
        A1(594.0, 841.0),
        A2(420.0, 594.0),
        A3(297.0, 420.0),
        A4(210.0, 297.0),
        A5(148.0, 210.0);

        val widthMm: Double get() = width
        val heightMm: Double get() = height
    }

    fun outDir(default: String?): Path {
        if (default == null) {
            val out = Path.of("/out")
            if (Files.exists((out))) return out
            val target = Path.of("target", "images")
            if (Files.notExists(target)) Files.createDirectories(target)
            return target
        }
        else {
            val path = Path.of(default)
            if (Files.notExists(path)) throw IllegalStateException("${path.absolute()} does not exist")
            if (!Files.isDirectory(path)) throw IllegalStateException("${path.absolute()} is not a directory")
            return path
        }
    }


}