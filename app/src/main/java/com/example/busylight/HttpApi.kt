package com.example.busylight

import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

/** Minimal dependency-free HTTP server. GET /status /red /green /auto */
class HttpApi(private val port: Int) {
    @Volatile private var server: ServerSocket? = null

    fun start() {
        thread(name = "busylight-http", isDaemon = true) {
            try {
                val s = ServerSocket(port).also { server = it }
                while (!s.isClosed) {
                    val client = s.accept()
                    handle(client)
                }
            } catch (_: Exception) { }
        }
    }

    fun stop() { try { server?.close() } catch (_: Exception) { } }

    private fun handle(c: Socket) {
        try {
            c.soTimeout = 3000
            val line = c.getInputStream().bufferedReader().readLine() ?: return
            val path = line.split(" ").getOrNull(1)?.trim('/')?.substringBefore('?') ?: ""
            when (path) {
                "red", "green", "auto" -> BusyState.mode = path
            }
            val body = BusyState.json().toByteArray()
            val out = c.getOutputStream()
            out.write(
                ("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\n" +
                 "Content-Length: ${body.size}\r\nConnection: close\r\n\r\n").toByteArray()
            )
            out.write(body); out.flush()
        } catch (_: Exception) {
        } finally { try { c.close() } catch (_: Exception) { } }
    }
}
