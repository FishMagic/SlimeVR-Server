package me.ftmc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fazecast.jSerialComm.SerialPort
import kotlinx.coroutines.delay
import java.io.IOException
import java.io.OutputStreamWriter
import java.util.*

@Composable
fun WiFiPage() {
  Column(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
    Row(
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth()
    ) {
      Text(text = "WiFi", style = MaterialTheme.typography.h5)
    }
    Spacer(Modifier.height(4.dp))
    Row(modifier = Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
      WiFiContent()
    }
  }
}

@Composable
private fun WiFiContent() {
  Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally){
    var trackerPort: SerialPort? = remember { null }
    var portUsing by remember { mutableStateOf(false) }
    LaunchedEffect(true) {
      val ports = SerialPort.getCommPorts()
      while (true){
        if (!portUsing){
          for (port in ports) {
            if (
              port.descriptivePortName.lowercase(Locale.getDefault()).contains("ch340") ||
              port.descriptivePortName.lowercase(Locale.getDefault()).contains("cp21") ||
              port.descriptivePortName.lowercase(Locale.getDefault()).contains("ch910")
            ) {
              trackerPort = port
              break
            }
            trackerPort = null
          }
        }
        delay(1000L)
      }
    }
    if (trackerPort == null) {
      Text(text = "No trackers connected, connect tracker to USB")
    } else {
      var networkName by remember { mutableStateOf("") }
      var networkPassword by remember { mutableStateOf("") }
      var sendButtonEnable by remember { mutableStateOf(true) }
      var statusText by remember { mutableStateOf("") }
      var progressIndicatorData by remember { mutableStateOf(0f) }
      Text(text = "Tracker connected to " + trackerPort!!.getSystemPortName() + " (" + trackerPort!!.getDescriptivePortName() + ")")
      Row {
        Text(text = "Network Name:")
        OutlinedTextField(value = networkName, onValueChange = {networkName = it})
      }
      Row {
        Text(text = "Network Password:")
        OutlinedTextField(value = networkPassword, onValueChange = {networkPassword = it})
      }
      Button(onClick = {
        portUsing = true
        sendButtonEnable = false
        if (trackerPort!!.openPort()) {
          trackerPort!!.baudRate = 115200
          statusText = "Port Opened"
          progressIndicatorData = .33f
          val outputStream = trackerPort!!.outputStream
          val writer = OutputStreamWriter(outputStream)
          try{
            writer.append("SET WIFI \"$networkName\" \"$networkPassword\"\n")
            writer.flush()
            statusText = "WiFi Written"
            progressIndicatorData = .66f
          } catch (_: IOException) {
            statusText = "Write WiFi Error!"
          }
          trackerPort!!.closePort()
          statusText = "Done!"
          progressIndicatorData = 1f
          portUsing = false
        } else {
          statusText = "Port Open Error"
        }
      },
        enabled = sendButtonEnable) {
        Text(text = "Send")
      }
      Text(text = statusText)
      LinearProgressIndicator(progressIndicatorData)
    }
  }
}
