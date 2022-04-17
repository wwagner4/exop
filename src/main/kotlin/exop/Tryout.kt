package exop

import exop.Util.runCommand

object Tryout {
    fun tryout(output: String?, catalogue: String?) {
        println("tryout $output $catalogue")
        val catDir = Util.catDir(catalogue)
        val text = "git --no-pager log -n20 --date=short".runCommand(catDir) ?: "???"
        Util.parseGitLogOutput(text).forEach {
            println(it)
        }
    }


}