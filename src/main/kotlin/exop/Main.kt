package exop

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType

@Suppress("EnumEntryName")
enum class Action(val description: String) {
    i01("Earthlike Distance"),
    svgt("Test svg creation"),
    tryout("Helpful during development"),
}

fun main(args: Array<String>) {
    val parser = ArgParser("exop")
    val action by parser.argument(
        ArgType.Choice<Action>(), description = argDescription()
    )
    val catalogue by parser.option(
        ArgType.String, shortName = "c", description = "Path containing the 'open exoplanet catalogue' in the local file system"
    )
    try {
        parser.parse(args)
        when (action) {
            Action.i01 -> ExopImages.i01(action.name, action.description, catalogue)
            Action.svgt -> ExopImages.createTest(catalogue)
            Action.tryout -> Tryout.tryout(catalogue)
        }
    } catch (e: IllegalStateException) {
        println("ERROR: ${e.message}")
    }
}

private fun argDescription(): String {
    val table: String = Action.values().joinToString("\n") {
        "              %7s : %s}".format(
            it.name, it.description
        )
    }
    return "Descriptions: \n$table"
}

