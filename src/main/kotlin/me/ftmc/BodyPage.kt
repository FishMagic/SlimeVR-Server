package me.ftmc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import dev.slimevr.VRServer
import dev.slimevr.autobone.AutoBone
import dev.slimevr.poserecorder.PoseFrameIO
import dev.slimevr.poserecorder.PoseFrames
import dev.slimevr.poserecorder.PoseRecorder
import dev.slimevr.vr.processor.skeleton.SkeletonConfigValue
import io.eiren.util.logging.LogManager
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.commons.lang3.StringUtils
import java.io.File
import kotlin.math.sqrt

@Composable
fun BodyPage(vrServer: VRServer) {
  Column(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
    Row(
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth()
    ) {
      var reseting by remember { mutableStateOf(false) }
      var resetButtonText by remember { mutableStateOf("Reset") }
      Button(
        onClick = {
          MainScope().launch {
            reseting = true
            resetButtonText = "Resetting..."
            vrServer.humanPoseProcessor.resetAllSkeletonConfigs()
            vrServer.humanPoseProcessor.skeletonConfig.saveToConfig(vrServer.config)
            vrServer.saveConfig()
            delay(3000L)
            reseting = false
            resetButtonText = "Reset"
          }
        }, enabled = !reseting
      ) {
        Text(text = resetButtonText)
      }
      var autoBoneShow by remember { mutableStateOf(false) }
      if (autoBoneShow){ AutoBoneWindow({ autoBoneShow = false }, vrServer) }
      Text(text = "Body Proportions", style = MaterialTheme.typography.h5)
      Button(onClick = { autoBoneShow = true }) {
        Text(text = "Auto")
      }
    }
    Row(modifier = Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
      SkeletionList(vrServer)
    }
  }
}

@Composable
private fun SkeletionList(vrServer: VRServer) {
  Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
    for (config in SkeletonConfigValue.values) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(.3f), horizontalAlignment = Alignment.End) {
          Text(text = config.name)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Row(modifier = Modifier.weight(.7f).width(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
          Column(modifier = Modifier.weight(.25f), horizontalAlignment = Alignment.CenterHorizontally) {
            OutlinedButton(onClick = {
              val current = vrServer.humanPoseProcessor.getSkeletonConfig(config)
              vrServer.humanPoseProcessor.setSkeletonConfig(config, current + 0.005f)
              vrServer.humanPoseProcessor.skeletonConfig.saveToConfig(vrServer.config)
              vrServer.saveConfig()
            }) {
              Text(text = "+")
            }
          }
          Spacer(modifier = Modifier.width(4.dp))
          var boneData by remember { mutableStateOf("") }
          Column(modifier = Modifier.weight(.25f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = boneData,
              style = MaterialTheme.typography.body2
            )
          }
          Spacer(modifier = Modifier.width(4.dp))
          Column(modifier = Modifier.weight(.25f), horizontalAlignment = Alignment.CenterHorizontally) {
            OutlinedButton(onClick = {
              val current = vrServer.humanPoseProcessor.getSkeletonConfig(config)
              vrServer.humanPoseProcessor.setSkeletonConfig(config, current - 0.005f)
              vrServer.humanPoseProcessor.skeletonConfig.saveToConfig(vrServer.config)
              vrServer.saveConfig()
            }) {
              Text(text = "-")
            }
          }
          Spacer(modifier = Modifier.width(4.dp))
          Column(modifier = Modifier.weight(.25f), horizontalAlignment = Alignment.CenterHorizontally) {
            var reseting by remember { mutableStateOf(false) }
            var resetButtonText by remember { mutableStateOf("Reset") }
            when (config) {
              SkeletonConfigValue.TORSO, SkeletonConfigValue.LEGS_LENGTH -> {
                TextButton(
                  onClick = {
                    MainScope().launch {
                      reseting = true
                      resetButtonText = "Resetting..."
                      vrServer.humanPoseProcessor.resetSkeletonConfig(config)
                      vrServer.humanPoseProcessor.skeletonConfig.saveToConfig(vrServer.config)
                      vrServer.saveConfig()
                      delay(3000L)
                      reseting = false
                      resetButtonText = "Reset"
                    }
                  },
                  enabled = !reseting
                ) {
                  Text(text = resetButtonText, style = MaterialTheme.typography.body2)
                }
              }
              else -> {
                TextButton(onClick = {
                  vrServer.humanPoseProcessor.resetSkeletonConfig(config)
                  vrServer.humanPoseProcessor.skeletonConfig.saveToConfig(vrServer.config)
                  vrServer.saveConfig()
                }) {
                  Text(text = "Reset", style = MaterialTheme.typography.body2)
                }
              }
            }
          }
          MainScope().launch() {
            while (true) {
              boneData = String.format("%.1f", vrServer.humanPoseProcessor.getSkeletonConfig(config) * 100)
              delay(500L)
            }
          }
        }
      }
      Divider(modifier = Modifier.fillMaxWidth())
    }
  }
}

@Composable
private fun AutoBoneWindow(onCloseRequest: () -> Unit, vrServer: VRServer) {
  Window(
    title = "SlimeVR: Auto Bone",
    onCloseRequest = onCloseRequest,
    icon = painterResource("icon256.png"),
    resizable = false,
    state = WindowState(size = DpSize(600.dp, 150.dp))
  ) {
    var recordButtonText by remember { mutableStateOf("Start Recording") }
    var recordButtonEnable by remember { mutableStateOf(true) }
    var saveRecordButtonText by remember { mutableStateOf("Save Recording") }
    var saveRecordButtonEnable by remember { mutableStateOf(false) }
    var adjustButtonText by remember { mutableStateOf("Auto-Adjust") }
    var adjustButtonEnable by remember { mutableStateOf(false) }
    var applyButtonEnable by remember { mutableStateOf(false) }
    var processText by remember { mutableStateOf("Processing has not been started...") }
    var lengthsText by remember { mutableStateOf("") }
    val saveDir = remember { File("Recordings") }
    val loadDir = remember { File("LoadRecordings") }
    val autoBone = remember { AutoBone(vrServer) }
    fun saveRecording(frames: PoseFrames) {
      if (saveDir.isDirectory || saveDir.mkdirs()) {
        var saveRecording: File
        var recordingIndex = 1
        do {
          saveRecording = File(saveDir, "ABRecording" + recordingIndex++ + ".pfr")
        } while (saveRecording.exists())

        LogManager.log.info("[AutoBone] Exporting frames to \"" + saveRecording.path + "\"...")
        if (PoseFrameIO.writeToFile(saveRecording, frames)) {
          LogManager.log.info("[AutoBone] Done exporting! Recording can be found at \"" + saveRecording.path + "\".")
        } else {
          LogManager.log.severe("[AutoBone] Failed to export the recording to \"" + saveRecording.path + "\".")
        }
      } else {
        LogManager.log.severe("[AutoBone] Failed to create the recording directory \"" + saveDir.path + "\".")
      }
    }

    fun loadRecordings(): MutableList<Pair<String, PoseFrames>> {
      val recordings = mutableListOf<Pair<String, PoseFrames>>()
      if (loadDir.isDirectory) {
        val files = loadDir.listFiles()
        if (files != null) {
          for (file in files) {
            if (file.isFile && StringUtils.endsWithIgnoreCase(file.name, ".pfr")) {
              LogManager.log.info("[AutoBone] Detected recording at \"" + file.path + "\", loading frames...")
              val frames = PoseFrameIO.readFromFile(file)

              if (frames == null) {
                LogManager.log.severe("Reading frames from \"" + file.path + "\" failed...")
              } else {
                recordings.add(Pair(file.name, frames))
              }
            }
          }
        }
      }
      return recordings
    }

    fun getLengthsString(): String {
      val configInfo = StringBuilder()
      autoBone.configs.forEach { key, value ->
        if (configInfo.isNotEmpty()) {
          configInfo.append(',')
        }

        configInfo.append(key.stringVal + ":" + String.format("%.2f", value * 100))
      }
      return configInfo.toString()
    }

    fun processFrames(frames: PoseFrames): Float {
      autoBone.minDataDistance = vrServer.config.getInt("autobone.minimumDataDistance", autoBone.minDataDistance)
      autoBone.maxDataDistance = vrServer.config.getInt("autobone.maximumDataDistance", autoBone.maxDataDistance)

      autoBone.numEpochs = vrServer.config.getInt("autobone.epochCount", autoBone.numEpochs)

      autoBone.initialAdjustRate = vrServer.config.getFloat("autobone.adjustRate", autoBone.initialAdjustRate)
      autoBone.adjustRateDecay = vrServer.config.getFloat("autobone.adjustRateDecay", autoBone.adjustRateDecay)

      autoBone.slideErrorFactor = vrServer.config.getFloat("autobone.slideErrorFactor", autoBone.slideErrorFactor)
      autoBone.offsetSlideErrorFactor =
        vrServer.config.getFloat("autobone.offsetSlideErrorFactor", autoBone.offsetSlideErrorFactor)
      autoBone.offsetErrorFactor = vrServer.config.getFloat("autobone.offsetErrorFactor", autoBone.offsetErrorFactor)
      autoBone.proportionErrorFactor =
        vrServer.config.getFloat("autobone.proportionErrorFactor", autoBone.proportionErrorFactor)
      autoBone.heightErrorFactor = vrServer.config.getFloat("autobone.heightErrorFactor", autoBone.heightErrorFactor)
      autoBone.positionErrorFactor =
        vrServer.config.getFloat("autobone.positionErrorFactor", autoBone.positionErrorFactor)
      autoBone.positionOffsetErrorFactor =
        vrServer.config.getFloat("autobone.positionOffsetErrorFactor", autoBone.positionOffsetErrorFactor)

      val calcInitError: Boolean = vrServer.config.getBoolean("autobone.calculateInitialError", true)
      val targetHeight: Float = vrServer.config.getFloat("autobone.manualTargetHeight", -1f)
      return autoBone.processFrames(frames, calcInitError, targetHeight) { epoch: AutoBone.Epoch ->
        processText = epoch.toString()
        lengthsText = getLengthsString()
      }

    }

    val poseRecorder = remember { PoseRecorder(vrServer) }
    val recordThread = remember {
      Thread {
        try {
          if (poseRecorder.isReadyToRecord) {
            recordButtonEnable = false
            recordButtonText = "Recording..."
            val sampleCount = vrServer.config.getInt("autobone.sampleCount", 1000)
            val sampleRate = vrServer.config.getFloat("autobone.sampleRateMs", 20f).toLong()
            val framesFuture = poseRecorder.startFrameRecording(sampleCount, sampleRate)
            val frames = framesFuture.get()
            LogManager.log.info("[AutoBone] Done recording!")

            saveRecordButtonEnable = true
            adjustButtonEnable = true

            if (vrServer.config.getBoolean("autobone.saveRecordings", false)) {
              recordButtonText = "Saving..."
              saveRecording(frames)
            }
          } else {
            recordButtonText = "Not Ready"
            LogManager.log.severe("[AutoBone] Unable to record...")
            Thread.sleep(3000) // Wait for 3 seconds
            return@Thread
          }
        } catch (e: Exception) {
          recordButtonText = "Recording Failed"
          LogManager.log.severe("[AutoBone] Failed recording!", e)
          try {
            Thread.sleep(3000) // Wait for 3 seconds
          } catch (_: Exception) {
            // Ignore
          }
        } finally {
          recordButtonText = "Start Recording"
          recordButtonEnable = true
        }
      }
    }
    val recordSavingThread = remember {
      Thread {
        try {
          val framesFuture = poseRecorder.framesAsync
          if (framesFuture != null) {
            saveRecordButtonText = "Waiting for Recording..."
            val frames = framesFuture.get()

            check(frames.trackerCount > 0) { "Recording has no trackers" }

            check(frames.maxFrameCount > 0) { "Recording has no frames" }

            saveRecordButtonText = "Saving..."
            saveRecording(frames)

            saveRecordButtonText = "Recording Saved!"

            try {
              Thread.sleep(3000L)
            } catch (_: Exception) {
            }
          } else {
            saveRecordButtonText = "No Recording..."
            LogManager.log.severe("[AutoBone] Unable to save, no recording was done...")
            try {
              Thread.sleep(3000) // Wait for 3 seconds
            } catch (_: Exception) {
              // Ignore
            }
            return@Thread
          }
        } catch (e: Exception) {
          saveRecordButtonText = "Saving Failed"
          LogManager.log.severe("[AutoBone] Failed to save recording!", e)
          try {
            Thread.sleep(3000) // Wait for 3 seconds
          } catch (_: Exception) {
            // Ignore
          }
        }
      }
    }
    val autoAdjThread = remember {
      Thread {
        try {
          adjustButtonText = "Loading..."
          val frameRecordings = loadRecordings()

          if (frameRecordings.isNotEmpty()) {
            LogManager.log.info("[AutoBone] Done loading frames!")
          } else {
            val framesFuture = poseRecorder.framesAsync
            if (framesFuture != null) {
              adjustButtonText = "Waiting for Recording..."
              val frames = framesFuture.get()

              check(frames.trackerCount > 0) { "Recording has no trackers" }

              check(frames.maxFrameCount > 0) { "Recording has no frames" }

              frameRecordings.add(Pair("<Recording>", frames))
            } else {
              adjustButtonText = "No Recordings"
              LogManager.log.severe("[AutoBone] No recordings found in \"" + loadDir.path + "\" and no recording was done...")
              try {
                Thread.sleep(3000L)
              } catch (_: Exception) {
              }
            }
          }

          adjustButtonText = "Processing..."
          LogManager.log.info("[AutoBone] Processing frames...")
          val heightPercentError = mutableListOf<Float>()
          for (recording in frameRecordings) {
            LogManager.log.info("[AutoBone] Processing frames from \"" + recording.first + "\"...")
            heightPercentError.add(processFrames(recording.second))
            LogManager.log.info("[AutoBone] Done processing!")
            applyButtonEnable = true

            //#region Stats/Values
            val neckLength = autoBone.getConfig(SkeletonConfigValue.NECK)
            val chestDistance = autoBone.getConfig(SkeletonConfigValue.CHEST)
            val torsoLength = autoBone.getConfig(SkeletonConfigValue.TORSO)
            val hipWidth = autoBone.getConfig(SkeletonConfigValue.HIPS_WIDTH)
            val legsLength = autoBone.getConfig(SkeletonConfigValue.LEGS_LENGTH)
            val kneeHeight = autoBone.getConfig(SkeletonConfigValue.KNEE_HEIGHT)

            val neckTorso = if (neckLength != null && torsoLength != null) neckLength / torsoLength else 0f
            val chestTorso = if (chestDistance != null && torsoLength != null) chestDistance / torsoLength else 0f
            val torsoWaist = if (hipWidth != null && torsoLength != null) hipWidth / torsoLength else 0f
            val legTorso = if (legsLength != null && torsoLength != null) legsLength / torsoLength else 0f
            val legBody =
              if (legsLength != null && torsoLength != null && neckLength != null) legsLength / (torsoLength + neckLength) else 0f
            val kneeLeg = if (kneeHeight != null && legsLength != null) kneeHeight / legsLength else 0f

            LogManager.log.info(
              "[AutoBone] Ratios: [{Neck-Torso: " + io.eiren.util.StringUtils.prettyNumber(neckTorso) + "}, {Chest-Torso: " + io.eiren.util.StringUtils.prettyNumber(
                chestTorso
              ) + "}, {Torso-Waist: " + io.eiren.util.StringUtils.prettyNumber(torsoWaist) + "}, {Leg-Torso: " + io.eiren.util.StringUtils.prettyNumber(
                legTorso
              ) + "}, {Leg-Body: " + io.eiren.util.StringUtils.prettyNumber(legBody) + "}, {Knee-Leg: " + io.eiren.util.StringUtils.prettyNumber(
                kneeLeg
              ) + "}]"
            )

            val lengthsString = getLengthsString()
            LogManager.log.info("[AutoBone] Length values: $lengthsString")
            lengthsText = lengthsString
          }

          if (heightPercentError.isNotEmpty()) {
            var mean = 0f
            for (value in heightPercentError) {
              mean += value
            }
            mean /= heightPercentError.size

            var std = 0f
            for (value in heightPercentError) {
              val stdVal = value - mean
              std += stdVal * stdVal
            }

            std = sqrt(std / heightPercentError.size)

            LogManager.log.info(
              "[AutoBone] Average height error: " + String.format("%.6f", mean) + " (SD " + String.format(
                "%.6f",
                std
              ) + ")"
            )
          }
        } catch (e: Exception) {
          adjustButtonText = "Failed"

          LogManager.log.severe("[AutoBone] Failed adjustment!", e)
          try {
            Thread.sleep(3000) // Wait for 3 seconds
          } catch (_: Exception) {
            // Ignore
          } finally {
            adjustButtonText = "Auto-Adjust"
          }
        }
      }
    }
    Column (modifier = Modifier.padding(8.dp)) {
      Row {
        OutlinedButton(
          onClick = { recordThread.start() },
          enabled = recordButtonEnable
        ) {
          Text(text = recordButtonText)
        }
        Spacer(modifier = Modifier.width(4.dp))
        OutlinedButton(
          onClick = { recordSavingThread.start() },
          enabled = saveRecordButtonEnable
        ) {
          Text(text = saveRecordButtonText)
        }
        Spacer(modifier = Modifier.width(4.dp))
        OutlinedButton(
          onClick = { autoAdjThread.start() },
          enabled = adjustButtonEnable
        ) {
          Text(text = adjustButtonText)
        }
        Spacer(modifier = Modifier.width(4.dp))
        OutlinedButton(
          onClick = {
            applyButtonEnable = false
            autoBone.applyConfig()

          }, enabled = applyButtonEnable
        ) {
          Text(text = "Apply Values")
        }
      }
      Spacer(modifier = Modifier.height(4.dp))
      Text(text = processText)
      Spacer(modifier = Modifier.height(4.dp))
      Text(text = lengthsText)
    }
  }
}