package me.ftmc.i18n

import dev.slimevr.vr.processor.skeleton.SkeletonConfigValue
import dev.slimevr.vr.trackers.TrackerMountingRotation
import kotlinx.serialization.Serializable

@Serializable
data class i18nData(
  val body: Body,
  val debug: Debug,
  val gui: Gui,
  val info: Info,
  val main: Main,
  val skeleton: Skeleton,
  val steamVr: SteamVr,
  val wifi: Wifi
)

@Serializable
data class Body(
  val auto: String,
  val autoWindow: AutoWindow,
  val config: Config,
  val reset: String,
  val resetting: String,
  val title: String
)

@Serializable
data class Debug(
  val adjGyro: String,
  val attFix: String,
  val cal: String,
  val correction: String,
  val gyroFix: String,
  val magAcc: String,
  val quat: String,
  val rawMag: String,
  val rotadj: String,
  val temp: String,
  val title: String,
  val yawFix: String
)

@Serializable
data class Gui(
  val body: String,
  val debug: String,
  val skeleton: String,
  val steamVr: String,
  val trackers: String,
  val wifi: String
) {
  operator fun get(title: String): String {
    return when (title) {
      "Trackers" -> trackers
      "Body" -> body
      "Debug" -> debug
      "Steam VR" -> steamVr
      "Skeleton" -> skeleton
      "WiFi" -> wifi
      else -> title
    }
  }
}

@Serializable
data class Info(
  val battery: String,
  val fastReset: String,
  val imu: Imu,
  val ping: String,
  val position: String,
  val raw: String,
  val reset: String,
  val resetting: String,
  val rotation: String,
  val signal: String,
  val simpleName: SimpleName,
  val status: String,
  val title: String,
  val tps: String,
  val trackerPosition: TrackerPosition,
  val trackerStatus: TrackerStatus
)

@Serializable
data class Main(
  val busy: Busy,
  val ok: String,
  val title: String,
  val version: Version
)

@Serializable
data class Skeleton(
  val joint: String,
  val name: Name,
  val pitch: String,
  val roll: String,
  val title: String,
  val x: String,
  val y: String,
  val yaw: String,
  val z: String
)

@Serializable
data class SteamVr(
  val chest: String,
  val elbows: String,
  val knees: String,
  val legs: String,
  val notice: String,
  val title: String,
  val waist: String
)

@Serializable
data class Wifi(
  val connectedInfo: String,
  val disconnect: String,
  val networkName: String,
  val networkPassword: String,
  val send: String,
  val status: Status,
  val title: String
)

@Serializable
data class AutoWindow(
  val apply: String,
  val autoAdjust: AutoAdjust,
  val process: String,
  val record: Record,
  val save: Save,
  val title: String
)

@Serializable
data class Config(
  val CHEST: String,
  val CONTROLLER_DISTANCE_Y: String,
  val CONTROLLER_DISTANCE_Z: String,
  val ELBOW_DISTANCE: String,
  val FOOT_LENGTH: String,
  val FOOT_OFFSET: String,
  val HEAD: String,
  val HIPS_WIDTH: String,
  val HIP_OFFSET: String,
  val KNEE_HEIGHT: String,
  val LEGS_LENGTH: String,
  val NECK: String,
  val SKELETON_OFFSET: String,
  val TORSO: String,
  val WAIST: String
) {
  operator fun get(name: SkeletonConfigValue): String {
    return when (name) {
      SkeletonConfigValue.HEAD -> HEAD
      SkeletonConfigValue.LEGS_LENGTH -> LEGS_LENGTH
      SkeletonConfigValue.TORSO -> TORSO
      SkeletonConfigValue.CHEST -> CHEST
      SkeletonConfigValue.SKELETON_OFFSET -> SKELETON_OFFSET
      SkeletonConfigValue.CONTROLLER_DISTANCE_Y -> CONTROLLER_DISTANCE_Y
      SkeletonConfigValue.CONTROLLER_DISTANCE_Z -> CONTROLLER_DISTANCE_Z
      SkeletonConfigValue.ELBOW_DISTANCE -> ELBOW_DISTANCE
      SkeletonConfigValue.FOOT_LENGTH -> FOOT_LENGTH
      SkeletonConfigValue.FOOT_OFFSET -> FOOT_OFFSET
      SkeletonConfigValue.HIPS_WIDTH -> HIPS_WIDTH
      SkeletonConfigValue.HIP_OFFSET -> HIP_OFFSET
      SkeletonConfigValue.KNEE_HEIGHT -> KNEE_HEIGHT
      SkeletonConfigValue.NECK -> NECK
      SkeletonConfigValue.WAIST -> WAIST
      else -> name.name
    }
  }
}

@Serializable
data class AutoAdjust(
  val loading: String,
  val processing: String,
  val ready: String,
  val start: String,
  val waiting: String,
  val failed: String
)

@Serializable
data class Record(
  val failed: String,
  val ready: String,
  val recording: String,
  val saving: String,
  val start: String
)

@Serializable
data class Save(
  val failed: String,
  val ready: String,
  val saved: String,
  val saving: String,
  val start: String,
  val waiting: String
)

@Serializable
data class Imu(
  val BACK: String,
  val FRONT: String,
  val LEFT: String,
  val RIGHT: String
) {
  operator fun get(value: TrackerMountingRotation?): String {
    return when (value) {
      TrackerMountingRotation.BACK -> BACK
      TrackerMountingRotation.FRONT -> FRONT
      TrackerMountingRotation.LEFT -> LEFT
      TrackerMountingRotation.RIGHT -> RIGHT
      else -> ""
    }
  }
}

@Serializable
data class SimpleName(
  val ComputedHumanPoseTracker: String,
  val ComputedTracker: String,
  val HMDTracker: String,
  val IMUTracker: String,
  val MPUTracker: String,
  val PoseFrameTracker: String,
  val ReferenceAdjustedTracker: String,
  val ShareableTracker: String,
  val TrackerFrame: String,
  val VRTracker: String
) {
  operator fun get(simpleName: String): String {
    return when (simpleName) {
      "ComputedHumanPoseTracker" -> ComputedTracker
      "ComputedTracker" -> ComputedTracker
      "HMDTracker" -> HMDTracker
      "IMUTracker" -> IMUTracker
      "MPUTracker" -> MPUTracker
      "PoseFrameTracker" -> PoseFrameTracker
      "ReferenceAdjustedTracker" -> ReferenceAdjustedTracker
      "ShareableTracker" -> ShareableTracker
      "TrackerFrame" -> TrackerFrame
      "VRTracker" -> VRTracker
      else -> simpleName
    }
  }
}

@Serializable
data class TrackerPosition(
  val CHEST: String,
  val HIP: String,
  val HMD: String,
  val LEFT_ANKLE: String,
  val LEFT_CONTROLLER: String,
  val LEFT_ELBOW: String,
  val LEFT_FOOT: String,
  val LEFT_LEG: String,
  val NONE: String,
  val RIGHT_ANKLE: String,
  val RIGHT_CONTROLLER: String,
  val RIGHT_ELBOW: String,
  val RIGHT_FOOT: String,
  val RIGHT_LEG: String,
  val WAIST: String
) {
  operator fun get(value: dev.slimevr.vr.trackers.TrackerPosition?): String {
    return when (value) {
      dev.slimevr.vr.trackers.TrackerPosition.CHEST -> CHEST
      dev.slimevr.vr.trackers.TrackerPosition.NONE -> NONE
      dev.slimevr.vr.trackers.TrackerPosition.HIP -> HIP
      dev.slimevr.vr.trackers.TrackerPosition.HMD -> HMD
      dev.slimevr.vr.trackers.TrackerPosition.WAIST -> WAIST
      dev.slimevr.vr.trackers.TrackerPosition.LEFT_ANKLE -> LEFT_ANKLE
      dev.slimevr.vr.trackers.TrackerPosition.LEFT_CONTROLLER -> LEFT_CONTROLLER
      dev.slimevr.vr.trackers.TrackerPosition.LEFT_ELBOW -> LEFT_ELBOW
      dev.slimevr.vr.trackers.TrackerPosition.LEFT_FOOT -> LEFT_FOOT
      dev.slimevr.vr.trackers.TrackerPosition.LEFT_LEG -> LEFT_LEG
      dev.slimevr.vr.trackers.TrackerPosition.RIGHT_ANKLE -> RIGHT_ANKLE
      dev.slimevr.vr.trackers.TrackerPosition.RIGHT_CONTROLLER -> RIGHT_CONTROLLER
      dev.slimevr.vr.trackers.TrackerPosition.RIGHT_ELBOW -> RIGHT_ELBOW
      dev.slimevr.vr.trackers.TrackerPosition.RIGHT_FOOT -> RIGHT_FOOT
      dev.slimevr.vr.trackers.TrackerPosition.RIGHT_LEG -> RIGHT_LEG
      else -> NONE
    }
  }
}

@Serializable
data class TrackerStatus(
  val BUSY: String,
  val DISCONNECTED: String,
  val ERROR: String,
  val OCCLUDED: String,
  val OK: String
) {
  operator fun get(status: dev.slimevr.vr.trackers.TrackerStatus?): String {
    return when (status) {
      dev.slimevr.vr.trackers.TrackerStatus.BUSY -> BUSY
      dev.slimevr.vr.trackers.TrackerStatus.DISCONNECTED -> DISCONNECTED
      dev.slimevr.vr.trackers.TrackerStatus.ERROR -> ERROR
      dev.slimevr.vr.trackers.TrackerStatus.OCCLUDED -> OCCLUDED
      dev.slimevr.vr.trackers.TrackerStatus.OK -> OK
      else -> ERROR
    }
  }
}

@Serializable
data class Busy(
  val content: String,
  val title: String
)

@Serializable
data class Version(
  val content: String,
  val title: String
)

@Serializable
data class Name(
  val Chest: String,
  val ChestTracker: String,
  val HMD: String,
  val Head: String,
  val Hip: String,
  val LeftAnkle: String,
  val LeftFoot: String,
  val LeftFootTracker: String,
  val LeftHip: String,
  val LeftKnee: String,
  val LeftKneeTracker: String,
  val Neck: String,
  val RightAnkle: String,
  val RightFoot: String,
  val RightFootTracker: String,
  val RightHip: String,
  val RightKnee: String,
  val RightKneeTracker: String,
  val Waist: String,
  val WaistTracker: String
) {
  operator fun get(name: String?): String {
    return when (name) {
      "HMD" -> HMD
      "Head" -> Head
      "Neck" -> Neck
      "Chest" -> Chest
      "Chest-Tracker" -> ChestTracker
      "Waist" -> Waist
      "Hip" -> Hip
      "Waist-Tracker" -> WaistTracker
      "Left-Hip" -> LeftHip
      "Left-Knee" -> LeftKnee
      "Left-Knee-Tracker" -> LeftKnee
      "Left-Ankle" -> LeftAnkle
      "Left-Foot" -> LeftFoot
      "Left-Foot-Tracker" -> LeftFootTracker
      "Right-Hip" -> RightHip
      "Right-Knee" -> RightKnee
      "Right-Knee-Tracker" -> RightKneeTracker
      "Right-Ankle" -> RightAnkle
      "Right-Foot" -> RightFoot
      "Right-Foot-Tracker" -> RightFootTracker
      null -> "NULL"
      else -> name
    }
  }
}

@Serializable
data class Status(
  val done: String,
  val openError: String,
  val opened: String,
  val writeError: String,
  val written: String
)