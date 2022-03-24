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
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
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
import com.jme3.math.FastMath
import com.jme3.math.Quaternion
import dev.slimevr.vr.trackers.IMUTracker
import dev.slimevr.vr.trackers.ReferenceAdjustedTracker
import dev.slimevr.vr.trackers.Tracker
import kotlinx.coroutines.delay

@Composable
fun DebugPage(trackersList: MutableList<Tracker>) {
  Column(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
    Row(
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth()
    ) {
      Text(text = "Trackers Debug", style = MaterialTheme.typography.h5)
    }
    Spacer(Modifier.height(4.dp))
    Row(modifier = Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
      TrackersList(trackersList)
    }
  }
}

@Composable
private fun TrackersList(trackersList: MutableList<Tracker>) {
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
      Text(text = simpleName, style = MaterialTheme.typography.h6)
      if (trackers.size % 2 == 0) {
        trackers.forEachIndexed { index, tracker ->
          if (index % 2 == 0) {
            Row {
              TrackerDebugCard(tracker, 400)
              Spacer(modifier = Modifier.width(4.dp))
              TrackerDebugCard(trackers[index + 1], 200)
            }
            Spacer(modifier = Modifier.height(4.dp))
          }
        }
      } else {
        trackers.forEachIndexed { index, tracker ->
          if (index == 0) {
            TrackerDebugCard(tracker, 800)
            Spacer(modifier = Modifier.height(4.dp))
          } else if (index % 2 == 1) {
            Row {
              TrackerDebugCard(tracker, 400)
              Spacer(modifier = Modifier.width(4.dp))
              TrackerDebugCard(trackers[index + 1], 400)
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
private fun TrackerDebugCard(tracker: Tracker, cardWidth: Int) {
  val realTracker = if (tracker !is ReferenceAdjustedTracker<*>) tracker else tracker.getTracker()
  var rotQuat by remember { mutableStateOf("") }
  var rawMag by remember { mutableStateOf("") }
  val gyroFix by remember { mutableStateOf(String.format("0x%8x", realTracker.hashCode())) }
  var calibration by remember { mutableStateOf("") }
  var magAccuracy by remember { mutableStateOf("") }
  var correction by remember { mutableStateOf("") }
  var rotAdj by remember { mutableStateOf("") }
  var adj by remember { mutableStateOf("") }
  var adjYaw by remember { mutableStateOf("") }
  var adjGyro by remember { mutableStateOf("") }
  var temperature by remember { mutableStateOf("") }
  val floatArray = remember { FloatArray(3) }
  val quaternion = remember { Quaternion() }
  Card(modifier = Modifier.width(cardWidth.dp)) {
    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Text(text = realTracker.name, style = MaterialTheme.typography.subtitle1)
      }
      if (realTracker is IMUTracker) {
        Text(text = String.format("Quat: %s", rotQuat))
        Text(text = String.format("Raw Mag: %s", rawMag))
        Text(text = String.format("Gyro Fix: %s", gyroFix))
        Text(text = String.format("Cal: %s", calibration))
        Text(text = String.format("Mag Acc: %s", magAccuracy))
        Text(text = String.format("Correction: %s", correction))
        Text(text = String.format("RotAdj: %s", rotAdj))
      }
      if (realTracker is ReferenceAdjustedTracker<*>) {
        Text(text = String.format("Att Fix: %s", adj))
        Text(text = String.format("Yaw Fix: %s", adjYaw))
        Text(text = String.format("Gyro Fix: %s", adjGyro))
        Text(text = String.format("Temp: %s", temperature))
      }
    }
  }
  LaunchedEffect(true) {
    while (true) {
      if (realTracker is IMUTracker) {
        realTracker.rotMagQuaternion.toAngles(floatArray)
        rawMag = String.format("%d, %d, %d", floatArray[0], floatArray[1], floatArray[2])
        calibration = String.format("%d / %d", realTracker.calibrationStatus, realTracker.magCalibrationStatus)
        magAccuracy = String.format("%.1f°", realTracker.magnetometerAccuracy * FastMath.RAD_TO_DEG)
        realTracker.getCorrection(quaternion)
        quaternion.toAngles(floatArray)
        correction = String.format(
          "%d, %d, %d",
          floatArray[0] * FastMath.RAD_TO_DEG,
          floatArray[1] * FastMath.RAD_TO_DEG,
          floatArray[2] * FastMath.RAD_TO_DEG
        )
        realTracker.rotQuaternion.toAngles(floatArray)
        rotQuat = String.format(
          "%d, %d, %d",
          floatArray[0] * FastMath.RAD_TO_DEG,
          floatArray[1] * FastMath.RAD_TO_DEG,
          floatArray[2] * FastMath.RAD_TO_DEG
        )
        realTracker.rotAdjust.toAngles(floatArray)
        rotAdj = String.format(
          "%d, %d, %d",
          floatArray[0] * FastMath.RAD_TO_DEG,
          floatArray[1] * FastMath.RAD_TO_DEG,
          floatArray[2] * FastMath.RAD_TO_DEG
        )
        if (realTracker.temperature == 0f) {
          temperature = "?"
        } else {
          temperature = String.format("%.1f∘C")
        }
      }
      if (realTracker is ReferenceAdjustedTracker<*>) {
        realTracker.attachmentFix.toAngles(floatArray)
        adj = String.format(
          "%d, %d, %d",
          floatArray[0] * FastMath.RAD_TO_DEG,
          floatArray[1] * FastMath.RAD_TO_DEG,
          floatArray[2] * FastMath.RAD_TO_DEG
        )
        realTracker.yawFix.toAngles(floatArray)
        adjYaw = String.format(
          "%d, %d, %d",
          floatArray[0] * FastMath.RAD_TO_DEG,
          floatArray[1] * FastMath.RAD_TO_DEG,
          floatArray[2] * FastMath.RAD_TO_DEG
        )
        realTracker.gyroFix.toAngles(floatArray)
        adjGyro = String.format(
          "%d, %d, %d",
          floatArray[0] * FastMath.RAD_TO_DEG,
          floatArray[1] * FastMath.RAD_TO_DEG,
          floatArray[2] * FastMath.RAD_TO_DEG
        )
      }
      delay(500L)
    }
  }
}