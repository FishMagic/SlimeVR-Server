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
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.slimevr.VRServer
import dev.slimevr.platform.windows.WindowsNamedPipeBridge
import dev.slimevr.vr.trackers.TrackerRole

@Composable
fun SteamVRPage(vrServer: VRServer) {
  Column(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
    Row(
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Steam VR", style = MaterialTheme.typography.h5)
        Spacer(Modifier.height(4.dp))
        Text(text = "Changes may require restart of SteamVR", style = MaterialTheme.typography.subtitle1)
      }
    }
    Spacer(Modifier.height(4.dp))
    Row(modifier = Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
      SteamVRList(vrServer)
    }
  }
}

@Composable
private fun SteamVRList(vrServer: VRServer) {
  val vrBridge = vrServer.getVRBridge(WindowsNamedPipeBridge::class.java)
  Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
    TrackerRole.values.forEachIndexed { index, role ->
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
      ) {
        if (index % 2 == 0) {
          Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(400.dp)) {
            var selected by remember { mutableStateOf(vrBridge.getShareSetting(role)) }
            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
              Column(modifier = Modifier.weight(.5f), horizontalAlignment = Alignment.End) {
                Text(text = role.name)
              }
              Column(modifier = Modifier.weight(.5f)) {
                Switch(checked = selected, onCheckedChange = {
                  vrBridge.changeShareSettings(role, it)
                  selected = it
                })
              }
            }
          }
          Spacer(Modifier.width(4.dp))
          Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(400.dp)) {
            var selected by remember { mutableStateOf(vrBridge.getShareSetting(role)) }
            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
              Column(modifier = Modifier.weight(.6f), horizontalAlignment = Alignment.End) {
                Text(text = TrackerRole.values[index + 1].name)
              }
              Column(modifier = Modifier.weight(.4f)) {
                Switch(checked = selected, onCheckedChange = {
                  vrBridge.changeShareSettings(TrackerRole.values[index + 1], it)
                  selected = it
                })
              }
            }
          }
        }
      }
      if (index % 2 == 0) {
        Divider(modifier = Modifier.fillMaxWidth())
      }
    }
  }
}