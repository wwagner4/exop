package exop

import java.io.StringWriter

@Suppress("UNUSED_PARAMETER")
object Tryout {
    fun tryout(output: String?, catalogue: String?) {
        val w = StringWriter()
        Img02.create(w, Util.PageSizeIso.A5, catalogue)
        println(w.buffer.toString())
    }

}