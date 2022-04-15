package exop

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


        val contentTestSvg = """
            <?xml version="1.0" encoding="UTF-8"?>
            <svg xmlns="http://www.w3.org/2000/svg" width="210.000mm" height="297.000mm">
              <rect x="0.000mm" y="0.000mm" width="210.000mm" height="297.000mm" opacity="1.000" style="fill:white;" />
              <line x1="21.000mm" y1="173.745mm" x2="199.500mm" y2="173.745mm" opacity="0.500" style="stroke:blue;stroke-width:0.045mm" />
              <line x1="21.000mm" y1="196.614mm" x2="199.500mm" y2="196.614mm" opacity="0.500" style="stroke:blue;stroke-width:0.045mm" />
              <circle cx="38.850mm" cy="196.614mm" r="22.869mm" opacity="0.500" fill="orange" />
              <circle cx="74.550mm" cy="196.614mm" r="22.869mm" opacity="0.500" fill="orange" />
              <circle cx="92.400mm" cy="196.614mm" r="11.435mm" opacity="0.500" fill="green" />
              <text x="21.000mm" y="82.269mm" fill="black" opacity="0.900" font-family="serif" font-size="20.790mm">Test Title</text>
              <text x="199.500mm" y="105.138mm" fill="black" opacity="0.900" font-family="serif" font-size="20.790mm" text-anchor="end">Test Title right</text>
              <text x="199.500mm" y="128.007mm" fill="black" opacity="0.900" font-family="serif" font-size="10.395mm" text-anchor="end">This is a normal text</text>
              <text x="21.000mm" y="150.876mm" fill="black" opacity="0.900" font-family="serif" font-size="10.395mm">This is a normal text</text>
            </svg>
        """.trimIndent()

        val contentJson = """
            {"data": "Wolfi"}
        """.trimIndent()

        val baseDir = Path("src", "exop-react", "build")

        embeddedServer(Netty, 8080) {
            install(CORS)    {
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
                get("/testsvg") {
                    call.respondText(contentTestSvg, contentType = ContentType.Image.SVG)
                }
                get("/image") {
                    val sw = StringWriter()
                    ExopImages.i01(sw, Util.PageSize.A4, null)
                    val content = sw.buffer.toString()
                    call.respondText(content, contentType = ContentType.Image.SVG)
                }
                get("/ww") {
                    call.respondText(contentJson, contentType = ContentType.Application.Json)
                }
                get("/react") {
                    val file = baseDir.resolve("index.html")
                    log.info("file:${file.absolute()}")
                    if (Files.notExists(file)) throw IllegalStateException("Could not fine path: ${file.absolute()}")
                    call.respondFile(file.toFile())
                }
                get("/{path0}") {
                    val path = call.parameters["path0"]
                    val file = baseDir.resolve(path!!)
                    log.info("file: ${file.absolute()}")
                    call.respondFile(file.toFile())
                }
                get("/{path1}/{path0}") {
                    val path0 = call.parameters["path0"]
                    val path1 = call.parameters["path1"]
                    val rel = Path(path1!!, path0!!)
                    val file = baseDir.resolve(rel)
                    log.info("file: ${file.absolute()}")
                    call.respondFile(file.toFile())
                }
                get("/{path2}/{path1}/{path0}") {
                    val path0 = call.parameters["path0"]
                    val path1 = call.parameters["path1"]
                    val path2 = call.parameters["path2"]
                    val rel = Path(path2!!, path1!!, path0!!)
                    val file = baseDir.resolve(rel)
                    log.info("file: ${file.absolute()}")
                    call.respondFile(file.toFile())
                }
            }
        }.start(wait = true)

    }

}