package exop

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import java.io.FileWriter
import java.io.Writer

@Suppress("EnumEntryName")
enum class Action(val description: String) {
    i01("Earthlike Distance"),
    server("Start a web-server"),
    svgt("Test svg creation"),
    tryout("Helpful during development"),
}

fun main(args: Array<String>) {
    val parser = ArgParser("exop")
    val action by parser.argument(
        ArgType.Choice<Action>(), description = argDescription()
    )
    val catalogue by parser.option(
        ArgType.String,
        shortName = "c",
        description = "Path containing the 'open exoplanet catalogue' in the local file system"
    )
    val output by parser.option(
        ArgType.String, shortName = "o", description = "Output directory"
    )
    try {
        parser.parse(args)
        when (action) {
            Action.i01 -> writeToFiles(action.name, output, catalogue, ExopImages::i01)
            Action.svgt -> writeToFiles(action.name, output, catalogue, ExopImages::createTest)
            Action.tryout -> Tryout.tryout(output, catalogue)
            Action.server -> Server.start(output, catalogue)
        }
    } catch (e: IllegalStateException) {
        println("ERROR: ${e.message}")
    }
}

fun writeToFiles(
    id: String,
    output: String?,
    catalogue: String?,
    f: (writer: Writer, pageSize: Util.PageSize, catalogue: String?) -> Unit) {

    fun writeToFile(pageSize: Util.PageSize) {
        val filename = "exop_image_${id}_${pageSize.name}.svg"
        val outDir = Util.outDir(output)
        val path = outDir.resolve(filename)
        val fileWriter = FileWriter(path.toFile())
        f(fileWriter, pageSize, catalogue)
        println("Wrote exop image to $path")
    }

    Util.PageSize.values().forEach { writeToFile(it) }
}

private fun argDescription(): String {
    val table: String = Action.values().joinToString("\n") {
        "              %7s : %s}".format(
            it.name, it.description
        )
    }
    return "Descriptions: \n$table"
}

