package exop

import exop.Util.runCommand
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import java.io.StringWriter
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.absolute

object Server {

    fun start(output: String?, catalogue: String?) {
        println("Starting a web-server. catalogue:$catalogue output:$output")

        val catDir = Util.catDir(catalogue)
        println("Catalogue dir:'${catDir.absolute()}'")

        val envName = "EXOP_REACT_DIR"
        val envVal = System.getenv(envName)
        val reactDir =
            if (envVal == null) Path("src", "exop-react", "build")
            else Path(envVal)
        println("reactDir: $reactDir")

        embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
            install(CORS) {
                method(HttpMethod.Options)
                method(HttpMethod.Get)
                method(HttpMethod.Post)
                method(HttpMethod.Put)
                method(HttpMethod.Delete)
                method(HttpMethod.Patch)
                header(HttpHeaders.AccessControlAllowHeaders)
                header(HttpHeaders.ContentType)
                header(HttpHeaders.AccessControlAllowOrigin)
                allowCredentials = true
                anyHost()
            }
            routing {
                get("/update") {
                    val htmlText = try {
                        "git pull".runCommand(catDir)
                        val stdout = "git --no-pager log -n8 --date=short".runCommand(catDir)!!
                        val rows = Util.parseGitLogOutput(stdout).map { "<tr><td>${it.date}</td><td>${it.text}</td>" }
                            .joinToString("")
                        val table = "<table><tbody>$rows</tbody></table>"
                        "update was successful. if you create now a poster you will use the latest known exoplanet data.<br><br>$table"
                    } catch (e: Exception) {
                        "Error on catalogue update ${e.message}"
                    }
                    call.respondText(htmlText, contentType = ContentType.Text.Html)
                }
                get("/image") {
                    val size: String = call.request.queryParameters["size"] ?: "A4"
                    val ps = Util.PageSize.valueOf(size)
                    println("get $size image size: $ps")
                    val sw = StringWriter()
                    ExopImages.i01(sw, ps, null)
                    val content = sw.buffer.toString()
                    call.respondText(content, contentType = ContentType.Image.SVG)
                }
                get("/react") {
                    log.info("reactDir:${reactDir.absolute()}")
                    val file = reactDir.resolve("index.html")
                    log.info("/react file:${file.absolute()}")
                    if (Files.notExists(file)) throw IllegalStateException("/react Could not fine path: ${file.absolute()}")
                    call.respondFile(file.toFile())
                }
                get("/{path0}") {
                    val path = call.parameters["path0"]
                    val file = reactDir.resolve(path!!)
                    log.info("file: ${file.absolute()}")
                    call.respondFile(file.toFile())
                }
                get("/{path1}/{path0}") {
                    val path0 = call.parameters["path0"]
                    val path1 = call.parameters["path1"]
                    val rel = Path(path1!!, path0!!)
                    val file = reactDir.resolve(rel)
                    log.info("file: ${file.absolute()}")
                    call.respondFile(file.toFile())
                }
                get("/{path2}/{path1}/{path0}") {
                    val path0 = call.parameters["path0"]
                    val path1 = call.parameters["path1"]
                    val path2 = call.parameters["path2"]
                    val rel = Path(path2!!, path1!!, path0!!)
                    val file = reactDir.resolve(rel)
                    log.info("file: ${file.absolute()}")
                    call.respondFile(file.toFile())
                }
            }
        }.start(wait = true)

    }

}