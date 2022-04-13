package com.demo

import io.github.bonigarcia.wdm.WebDriverManager
import io.github.bonigarcia.wdm.config.DriverManagerType
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.DesiredCapabilities
import org.testcontainers.containers.BrowserWebDriverContainer
import org.testcontainers.containers.RecordingFileFactory
import org.testcontainers.containers.VncRecordingContainer
import org.testcontainers.lifecycle.TestDescription
import java.io.File
import java.nio.file.Paths
import java.util.*

lateinit var docker: BrowserWebDriverContainer<Nothing>

fun main() {
    //1. Create webdriver and start Docker
    val webDriver = startDocker()

    //2. Open some page
    webDriver.get("https://www.github.com")

    //3. Stop the webdriver
    webDriver.quit()

    //4. Notify docker to save video
    docker.afterTest(TestDi(), Optional.of(Throwable("")))

    //5. Stop docker container
    docker.stop()
}

fun create(): WebDriver {
    WebDriverManager.getInstance(DriverManagerType.CHROME).driverVersion("latest").setup()
    return ChromeDriver(getCapabilities())
}

private class RecordingFileFactoryImpl: RecordingFileFactory {
    override fun recordingFileForTest(vncRecordingDirectory: File?, prefix: String?, succeeded: Boolean) =
        Paths.get("video.mp4").toFile()
}

private class TestDi: TestDescription {
    override fun getTestId() = ""
    override fun getFilesystemFriendlyName() = "/build/"
}

private fun startDocker() : WebDriver {
    docker = BrowserWebDriverContainer<Nothing>()
        .withCapabilities(getCapabilities())
    docker.withRecordingMode(
        BrowserWebDriverContainer.VncRecordingMode.RECORD_ALL,
        File("build"),
        VncRecordingContainer.VncRecordingFormat.MP4
    )
    docker.withRecordingFileFactory(
        RecordingFileFactoryImpl()
    )
    docker.start()

    return docker.webDriver
}

private fun getCapabilities(): DesiredCapabilities {
    val caps = DesiredCapabilities.chrome()

    val options = ChromeOptions()
    options.addArguments(
        "--allow-insecure-localhost",
        "--safebrowsing-disable-extension-blacklist",
        "--safebrowsing-disable-download-protection",
    )

    caps.setCapability(ChromeOptions.CAPABILITY, options)
    caps.setCapability("acceptInsecureCerts", true)

    val chromePrefs = HashMap<String, Any>()
    chromePrefs["profile.default_content_settings.popups"] = 0
    chromePrefs["download.default_directory"] = "build"
    chromePrefs["safebrowsing.enabled"] = "true"

    options.setExperimentalOption("prefs", chromePrefs)

    return caps
}