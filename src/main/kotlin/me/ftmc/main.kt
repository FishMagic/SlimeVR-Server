package me.ftmc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogState
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import dev.slimevr.VRServer
import io.eiren.util.logging.LogManager
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import me.ftmc.i18n.Main
import me.ftmc.i18n.i18nData
import org.apache.commons.lang3.JavaVersion
import org.apache.commons.lang3.SystemUtils
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.ServerSocket

lateinit var globalI18nObject: i18nData
private lateinit var localI18nObject: Main

@OptIn(ExperimentalSerializationApi::class)
fun main() = application {
  val vrServer = remember { VRServer() }
  val language = vrServer.config.getString("i18n", "zh-CN")
  globalI18nObject = Json.decodeFromStream(FileInputStream("i18n/${language}.json"))
  localI18nObject = globalI18nObject.main
  Window(
    onCloseRequest = ::exitApplication,
    title = localI18nObject.title,
    icon = painterResource("icon256.png"),
    state = WindowState(size = DpSize(width = 950.dp, height = 850.dp)),
    resizable = false
  ) {
    MaterialTheme {
      Surface {
        LaunchedEffect(true) {
          vrServer.start()
        }
        init()
        VRServerGUI(vrServer)
      }
    }
  }
}

@Composable
private fun ApplicationScope.init() {
  val dir = File("").absoluteFile
  try {
    LogManager.initialize(File(dir, "logs/"), dir)
  } catch (e1: Exception) {
    e1.printStackTrace()
  }

  var versionWarning by remember { mutableStateOf(false) }
  if (!SystemUtils.isJavaVersionAtLeast(JavaVersion.JAVA_11)) {
    LogManager.log.severe("SlimeVR start-up error! A minimum of Java 11 is required.")
    versionWarning = true
  }
  if (versionWarning) {
    Dialog(
      onCloseRequest = ::exitApplication,
      title = localI18nObject.version.title,
      state = DialogState(size = DpSize(450.dp, 150.dp)),
      resizable = false
    ) {
      Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Text(text = localI18nObject.version.content)
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
          Button(onClick = { exitApplication() }) {
            Text(text = localI18nObject.ok)
          }
        }
      }
    }
  }

  var busyWarning by remember { mutableStateOf(false) }
  try {
    ServerSocket(6969).close()
    ServerSocket(35903).close()
    ServerSocket(21110).close()
  } catch (e: IOException) {
    LogManager.log.severe("SlimeVR start-up error! Required ports are busy. Make sure there is no other instance of SlimeVR Server running.")
    busyWarning = true
  }
  if (busyWarning) {
    Dialog(
      onCloseRequest = ::exitApplication,
      title = localI18nObject.busy.title,
      state = DialogState(size = DpSize(450.dp, 150.dp)),
      resizable = false
    ) {
      Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Text(text = localI18nObject.busy.content)
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
          Button(onClick = { exitApplication() }) {
            Text(text = localI18nObject.ok)
          }
        }
      }
    }
  }
}

