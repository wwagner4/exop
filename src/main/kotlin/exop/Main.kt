package exop

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import java.io.FileWriter
import java.io.Writer

@Suppress("EnumEntryName")
enum class Action(val description: String) {
    i01("Earth-like Distance"),
    i01a("Earth-like Distance"),
    i02("Same size as the solar system"),
    it("Testimage"),
    server("Start a web-server"),
    tryout("Helpful during development"),
    stat("Create some statistics as markdown"),
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
            Action.i01 -> writeToFiles(action.name, output, catalogue, ExopImagesSvg::i01)
            Action.i01a -> writeToFiles(action.name, output, catalogue, Img01::create)
            Action.i02 -> writeToFiles(action.name, output, catalogue, Img02::create)
            Action.it -> writeToFiles(action.name, output, catalogue, TestImage::create)
            Action.tryout -> Tryout.tryout(output, catalogue)
            Action.server -> Server.start(output, catalogue)
            Action.stat -> StatisticFactory.overview(catalogue)
        }
    } catch (e: IllegalStateException) {
        println("ERROR: ${e.message}")
    }
}

fun writeToFiles(
    id: String,
    output: String?,
    catalogue: String?,
    f: (writer: Writer, pageSize: Util.PageSize, catalogue: String?) -> Unit
) {

    fun writeToFile(pageSize: Util.PageSize) {
        val filename = "exop_image_${id}_${pageSize.name}.svg"
        val outDir = Util.outDir(output)
        val path = outDir.resolve(filename)
        val fileWriter = FileWriter(path.toFile())
        f(fileWriter, pageSize, catalogue)
        println("Wrote exop image to $path")
    }

    listOf(Util.PageSizeIso.A2).forEach { writeToFile(it) }
    // Util.PageSize.values().forEach { writeToFile(it) }
    // writeToFile(Util.PageSizeIso.A2)
}

private fun argDescription(): String {
    val table: String = Action.values().joinToString("\n") {
        "              %7s : %s}".format(
            it.name, it.description
        )
    }
    return "Descriptions: \n$table"
}

