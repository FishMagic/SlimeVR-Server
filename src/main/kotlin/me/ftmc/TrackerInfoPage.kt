package me.ftmc

import androidx.compose.foundation.VerticalScrollbar
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
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
import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import dev.slimevr.VRServer
import dev.slimevr.vr.trackers.IMUTracker
import dev.slimevr.vr.trackers.ReferenceAdjustedTracker
import dev.slimevr.vr.trackers.Tracker
import dev.slimevr.vr.trackers.TrackerMountingRotation
import dev.slimevr.vr.trackers.TrackerPosition
import dev.slimevr.vr.trackers.TrackerWithBattery
import dev.slimevr.vr.trackers.TrackerWithTPS
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.ftmc.i18n.Info
import kotlin.math.max
import kotlin.math.min

private lateinit var localI18nObject: Info

@Composable
fun TrackerPage(vrServer: VRServer, trackersList: MutableList<Tracker>) {
  localI18nObject = globalI18nObject.info
  Column(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
    Row(
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth()
    ) {
      var reseting by remember { mutableStateOf(false) }
      var resetButtonText by remember { mutableStateOf(localI18nObject.reset) }
      Button(
        onClick = {
          MainScope().launch {
            reseting = true
            resetButtonText = localI18nObject.resetting
            vrServer.resetTrackers()
            delay(3000L)
            reseting = false
            resetButtonText = localI18nObject.reset
          }
        }, enabled = !reseting
      ) {
        Text(text = resetButtonText)
      }
      Text(text = localI18nObject.title, style = MaterialTheme.typography.h5)
      Button(onClick = {
        MainScope().launch {
          vrServer.resetTrackersYaw()
        }
      }) {
        Text(text = localI18nObject.fastReset)
      }
    }
    Spacer(Modifier.height(4.dp))
    Row(modifier = Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
      TrackersList(vrServer, trackersList)
    }
  }
}

@Composable
private fun TrackersList(vrServer: VRServer, trackersList: MutableList<Tracker>) {
  val trackersMap = mutableMapOf<String, MutableList<Tracker>>()
  trackersMap.clear()
  for (tracker in trackersList) {
    val simpleName = tracker.javaClass.simpleName
    if (trackersMap[simpleName] == null) {
      trackersMap[simpleName] = mutableListOf()
    }
    trackersMap[simpleName]?.add(tracker)
  }
  val stateVertical = rememberScrollState(0)
  Column(
    modifier = Modifier.fillMaxWidth().verticalScroll(stateVertical),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    for ((simpleName, trackers) in trackersMap) {
      Text(text = localI18nObject.simpleName[simpleName], style = MaterialTheme.typography.h6)
      if (trackers.size % 2 == 0) {
        trackers.forEachIndexed { index, tracker ->
          if (index % 2 == 0) {
            Row {
              TrackerInfoCard(vrServer, tracker, 400)
              Spacer(modifier = Modifier.width(4.dp))
              TrackerInfoCard(vrServer, trackers[index + 1], 400)
            }
            Spacer(modifier = Modifier.height(4.dp))
          }
        }
      } else {
        trackers.forEachIndexed { index, tracker ->
          if (index == 0) {
            TrackerInfoCard(vrServer, tracker, 800)
            Spacer(modifier = Modifier.height(4.dp))
          } else if (index % 2 == 1) {
            Row {
              TrackerInfoCard(vrServer, tracker, 400)
              Spacer(modifier = Modifier.width(4.dp))
              TrackerInfoCard(vrServer, trackers[index + 1], 400)
            }
            Spacer(modifier = Modifier.height(4.dp))
          }
        }
      }
    }
  }
  VerticalScrollbar(
    modifier = Modifier.fillMaxHeight(),
    adapter = rememberScrollbarAdapter(stateVertical)
  )
}

@Composable
private fun TrackerInfoCard(
  vrServer: VRServer, tracker: Tracker, cardWidth: Int
) {
  val realTracker = if (tracker !is ReferenceAdjustedTracker<*>) tracker else tracker.getTracker()
  var positionString by remember { mutableStateOf("") }
  val positionStore = remember { Vector3f() }
  var rotationString by remember { mutableStateOf("") }
  val rotationStoreMiddle = remember { Quaternion() }
  val rotationStore = remember { FloatArray(3) }
  var tps by remember { mutableStateOf("") }
  var ping by remember { mutableStateOf("") }
  var signalStrength by remember { mutableStateOf("") }
  var status by remember { mutableStateOf("") }
  var battery by remember { mutableStateOf("") }
  var rawString by remember { mutableStateOf("") }
  Card(modifier = Modifier.width(cardWidth.dp)) {
    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Text(text = realTracker.name, style = MaterialTheme.typography.subtitle1)
      }
      if (tracker.userEditable()) {
        EditableTrackerInfo(vrServer, tracker)
      }
      if (realTracker is IMUTracker) {
        IMUTrackerInfo(realTracker, tracker, vrServer)
      }
      if (realTracker.hasRotation()) {
        Row {
          Text(text = localI18nObject.rotation + rotationString, style = MaterialTheme.typography.body2)
        }
      }
      if (tracker.hasPosition()) {
        Text(text = localI18nObject.position + positionString, style = MaterialTheme.typography.body2)
      }
      if (realTracker is TrackerWithTPS) {
        Text(text = localI18nObject.tps + tps, style = MaterialTheme.typography.body2)
      }
      if (realTracker is IMUTracker) {
        Text(text = localI18nObject.ping + ping, style = MaterialTheme.typography.body2)
        Text(text = localI18nObject.signal + signalStrength, style = MaterialTheme.typography.body2)
      }
      if (realTracker is TrackerWithBattery) {
        Text(text = localI18nObject.battery + battery)
      }
      Text(
        text = localI18nObject.status + status, style = MaterialTheme.typography.body2
      )
      Text(
        text = localI18nObject.raw + rawString, style = MaterialTheme.typography.body2
      )
    }
  }
  LaunchedEffect(true) {
    while (true) {
      if (tracker.hasPosition()) {
        tracker.getPosition(positionStore)
        positionString = String.format("%.2f, %.2f, %.2f", positionStore.x, positionStore.y, positionStore.z)
      }
      if (tracker.hasRotation()) {
        tracker.getRotation(rotationStoreMiddle)
        rotationStoreMiddle.toAngles(rotationStore)
        rotationString = String.format("%.2f, %.2f, %.2f", rotationStore[0], rotationStore[1], rotationStore[2])
      }
      status = localI18nObject.trackerStatus[tracker.status]
      if (realTracker is TrackerWithTPS) {
        tps = String.format("%.1f", realTracker.tps)
      }
      if (realTracker is TrackerWithBattery) {
        val level = realTracker.batteryLevel
        val voltage = realTracker.batteryVoltage
        battery = if (level == 0f) {
          String.format("%.2fV", voltage)
        } else if (voltage == 0f) {
          String.format("%d%%", level)
        } else {
          String.format("%d%% (%.2fV)", level, voltage)
        }
      }
      if (realTracker is IMUTracker) {
        ping = realTracker.ping.toString()
        val signal = realTracker.signalStrength
        if (signal == -1) {
          signalStrength = "N/A"
        } else {
          var percentage = (signal - -95) * (100 - 0) / (-40 - -95) + 0
          percentage = max(min(percentage, 100), 0)
          signalStrength = "${percentage}% ($signal) dBm"
        }
      }
      realTracker.getRotation(rotationStoreMiddle)
      rotationStoreMiddle.toAngles(rotationStore)
      rawString = String.format("%.2f, %.2f, %.2f", rotationStore[0], rotationStore[1], rotationStore[2])
      delay(500L)
    }

  }
}

@Composable
private fun IMUTrackerInfo(
  realTracker: IMUTracker, tracker: Tracker, vrServer: VRServer
) {
  val trackerMountingRotation = realTracker.mountingRotation
  var trackerMountingRotationSelectorExpand by remember { mutableStateOf(false) }
  var trackerMountingRotationSelected by remember {
    mutableStateOf(
      trackerMountingRotation.name
    )
  }
  OutlinedButton(onClick = { trackerMountingRotationSelectorExpand = true }) {
    Text(text = trackerMountingRotationSelected)
  }
  DropdownMenu(expanded = trackerMountingRotationSelectorExpand,
    onDismissRequest = { trackerMountingRotationSelectorExpand = false }) {
    TrackerMountingRotation.values.forEach { value ->
      DropdownMenuItem(onClick = {
        realTracker.mountingRotation = value
        vrServer.trackerUpdated(tracker)
        trackerMountingRotationSelected = value.name
        trackerMountingRotationSelectorExpand = false
      }) {
        Text(text = localI18nObject.imu[value])
      }
    }
  }
}

@Composable
private fun EditableTrackerInfo(vrServer: VRServer, tracker: Tracker) {
  val trackerConfig = vrServer.getTrackerConfig(tracker)
  var positionSelectorExpand by remember { mutableStateOf(false) }
  var positionSelected by remember {
    mutableStateOf(
      TrackerPosition.getByDesignation(trackerConfig.designation).name
    )
  }
  OutlinedButton(onClick = { positionSelectorExpand = true }) {
    Text(text = positionSelected)
  }
  DropdownMenu(expanded = positionSelectorExpand, onDismissRequest = { positionSelectorExpand = false }) {
    TrackerPosition.values.forEach { value ->
      DropdownMenuItem(onClick = {
        tracker.bodyPosition = value
        vrServer.trackerUpdated(tracker)
        positionSelected = value.name
        positionSelectorExpand = false
      }) {
        Text(text = localI18nObject.trackerPosition[value])
      }
    }
  }
}