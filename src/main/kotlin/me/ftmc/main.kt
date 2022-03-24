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
import org.apache.commons.lang3.JavaVersion
import org.apache.commons.lang3.SystemUtils
import java.io.File
import java.io.IOException
import java.net.ServerSocket

fun main() = application {
  Window(
    onCloseRequest = ::exitApplication,
    title = "SlimeVR: Server",
    icon = painterResource("icon256.png"),
    state = WindowState(size = DpSize(width = 950.dp, height = 850.dp)),
    resizable = false
  ) {
    init()

    val vrServer = VRServer()
    vrServer.start()
    MaterialTheme {
      Surface {
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
      title = "SlimeVR: Java Runtime Mismatch",
      state = DialogState(size = DpSize(450.dp, 150.dp)),
      resizable = false
    ) {
      Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Text(text = "SlimeVR start-up error!\nA minimum of Java 11 is required.")
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
          Button(onClick = { exitApplication() }) {
            Text(text = "OK")
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
    Dialog (
      onCloseRequest = ::exitApplication,
      title = "SlimeVR: Ports are busy",
      state = DialogState(size = DpSize(450.dp, 150.dp)),
      resizable = false
    ) {
      Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Text(text = "SlimeVR start-up error!\nRequired ports are busy.\nMake sure there is no other instance of SlimeVR Server running.")
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
          Button(onClick = { exitApplication() }) {
            Text(text = "OK")
          }
        }
      }
    }
  }
}

