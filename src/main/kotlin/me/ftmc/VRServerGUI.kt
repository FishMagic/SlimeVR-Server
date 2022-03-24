package me.ftmc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.NavigationRail
import androidx.compose.material.NavigationRailItem
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.EmojiPeople
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import dev.slimevr.VRServer
import dev.slimevr.platform.windows.WindowsNamedPipeBridge
import dev.slimevr.posestreamer.ServerPoseStreamer
import dev.slimevr.vr.processor.TransformNode
import dev.slimevr.vr.trackers.Tracker

@Composable
fun VRServerGUI(vrServer: VRServer) {
  val trackerList = remember { mutableListOf<Tracker>() }
  var nodeList by remember { mutableStateOf(arrayOf<TransformNode>()) }
  val poseStreamer = ServerPoseStreamer(vrServer)
  var loading by remember { mutableStateOf(false) }
  LaunchedEffect(true) {
    vrServer.addNewTrackerConsumer {
      loading = true
      trackerList.add(it)
      loading = false
    }
    vrServer.addSkeletonUpdatedCallback {
      nodeList = it.allNodes
    }
  }
  Row(modifier = Modifier.fillMaxSize()) {
    val pages = mutableMapOf<String, ImageVector>()
    var selected by remember { mutableStateOf("Trackers") }
    pages["Trackers"] = Icons.Filled.PieChart
    pages["Debug"] = Icons.Filled.BugReport
    pages["Body"] = Icons.Filled.EmojiPeople
    if (vrServer.hasBridge(WindowsNamedPipeBridge::class.java)) {
      pages["Steam VR"] = Icons.Filled.ViewInAr
    }
    pages["Skeleton"] = Icons.Filled.Analytics
    pages["WiFi"] = Icons.Filled.Wifi
    NavigationRail {
      for ((title, icon) in pages) {
        NavigationRailItem(icon = { Icon(imageVector = icon, contentDescription = title) },
          selected = selected == title,
          onClick = { selected = title },
          label = { Text(text = title) })
      }
    }
    if (!loading) {
      when (selected) {
        "Trackers" -> TrackerPage(vrServer, trackerList)
        "Debug" -> DebugPage(trackerList)
        "Body" -> BodyPage(vrServer)
        "Steam VR" -> SteamVRPage(vrServer)
      }
    } else {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        CircularProgressIndicator()
      }
    }
  }
}
