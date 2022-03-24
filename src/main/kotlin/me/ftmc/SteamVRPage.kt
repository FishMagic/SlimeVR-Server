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
import me.ftmc.i18n.SteamVr

private lateinit var localI18nObject: SteamVr

@Composable
fun SteamVRPage(vrServer: VRServer) {
  localI18nObject = globalI18nObject.steamVr
  Column(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
    Row(
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = localI18nObject.title, style = MaterialTheme.typography.h5)
        Spacer(Modifier.height(4.dp))
        Text(text = localI18nObject.notice, style = MaterialTheme.typography.subtitle1)
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
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(400.dp)) {
        var selected by remember { mutableStateOf(vrBridge.getShareSetting(TrackerRole.WAIST)) }
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
          Column(modifier = Modifier.weight(.5f), horizontalAlignment = Alignment.End) {
            Text(text = localI18nObject.waist)
          }
          Column(modifier = Modifier.weight(.5f)) {
            Switch(checked = selected, onCheckedChange = {
              vrBridge.changeShareSettings(TrackerRole.WAIST, it)
              selected = it
            })
          }
        }
      }
      Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(400.dp)) {
        var selected by remember {
          mutableStateOf(
            vrBridge.getShareSetting(TrackerRole.LEFT_FOOT) && vrBridge.getShareSetting(TrackerRole.RIGHT_FOOT)
          )
        }
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
          Column(modifier = Modifier.weight(.5f), horizontalAlignment = Alignment.End) {
            Text(text = localI18nObject.legs)
          }
          Column(modifier = Modifier.weight(.5f)) {
            Switch(checked = selected, onCheckedChange = {
              vrBridge.changeShareSettings(TrackerRole.LEFT_FOOT, it)
              vrBridge.changeShareSettings(TrackerRole.RIGHT_FOOT, it)
              selected = it
            })
          }
        }
      }
    }
    Divider(modifier = Modifier.fillMaxWidth())
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(400.dp)) {
        var selected by remember { mutableStateOf(vrBridge.getShareSetting(TrackerRole.CHEST)) }
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
          Column(modifier = Modifier.weight(.5f), horizontalAlignment = Alignment.End) {
            Text(text = localI18nObject.chest)
          }
          Column(modifier = Modifier.weight(.5f)) {
            Switch(checked = selected, onCheckedChange = {
              vrBridge.changeShareSettings(TrackerRole.CHEST, it)
              selected = it
            })
          }
        }
      }
      Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(400.dp)) {
        var selected by remember {
          mutableStateOf(
            vrBridge.getShareSetting(TrackerRole.LEFT_KNEE) && vrBridge.getShareSetting(TrackerRole.RIGHT_KNEE)
          )
        }
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
          Column(modifier = Modifier.weight(.5f), horizontalAlignment = Alignment.End) {
            Text(text = localI18nObject.knees)
          }
          Column(modifier = Modifier.weight(.5f)) {
            Switch(checked = selected, onCheckedChange = {
              vrBridge.changeShareSettings(TrackerRole.LEFT_KNEE, it)
              vrBridge.changeShareSettings(TrackerRole.RIGHT_KNEE, it)
              selected = it
            })
          }
        }
      }
    }
    Divider(modifier = Modifier.fillMaxWidth())
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(400.dp)) {
        var selected by remember {
          mutableStateOf(
            vrBridge.getShareSetting(TrackerRole.LEFT_ELBOW) && vrBridge.getShareSetting(TrackerRole.RIGHT_ELBOW)
          )
        }
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
          Column(modifier = Modifier.weight(.5f), horizontalAlignment = Alignment.End) {
            Text(text = localI18nObject.elbows)
          }
          Column(modifier = Modifier.weight(.5f)) {
            Switch(checked = selected, onCheckedChange = {
              vrBridge.changeShareSettings(TrackerRole.LEFT_ELBOW, it)
              vrBridge.changeShareSettings(TrackerRole.RIGHT_ELBOW, it)
              selected = it
            })
          }
        }
      }
      Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(400.dp)) {

      }
    }
  }
}
