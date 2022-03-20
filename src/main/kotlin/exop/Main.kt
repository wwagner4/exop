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
    try {
        parser.parse(args)
        when (action) {
            Action.i01 -> ExopImages.i01(action.name, action.description)
            Action.svgt -> ExopImages.createTest()
            Action.tryout -> Tryout.tryout()
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

