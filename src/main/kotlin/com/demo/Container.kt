package com.demo

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.testcontainers.containers.BrowserWebDriverContainer
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.VncRecordingContainer
import java.io.InputStream

class CustomVnc(targetContainer: GenericContainer<*>) : VncRecordingContainer(targetContainer) {

    override fun streamRecording(): InputStream? {
        //copy raw vnc recording
        execInContainer("cp", "screen.flv", "screen_copy.flv")

        //compress it into mp4
        val newFileOutput = "/newScreen.mp4"
        execInContainer("ffmpeg", "-i", "screen_copy.flv", "-vcodec", "libx264", "-movflags", "faststart", "-pix_fmt", "yuv420p", newFileOutput);

        val archiveInputStream = TarArchiveInputStream(
            dockerClient.copyArchiveFromContainerCmd(containerId, newFileOutput).exec()
        )
        archiveInputStream.nextEntry
        return archiveInputStream
    }
}

class Container : BrowserWebDriverContainer<Nothing>() {
    lateinit var vnc: CustomVnc

    override fun configure() {
        super.configure()

        vnc = CustomVnc(this)
        val f = BrowserWebDriverContainer::class.java.declaredFields.find {
            it.name == "vncRecordingContainer"
        }!!
        f.isAccessible = true

        copy(vnc, f.get(this))
        f.set(this, vnc)
    }
}

fun copy(dist: Any, origin: Any) {
    val cls = dist.javaClass
    cls.declaredFields.forEach { f ->
        f.isAccessible = true
        try {
            f.set(dist, f.get(origin))
        } catch (e: Exception) {
            println("Cant set $f")
        }
    }
}