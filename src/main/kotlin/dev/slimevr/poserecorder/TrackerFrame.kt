package dev.slimevr.poserecorder

import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import dev.slimevr.vr.trackers.Tracker
import dev.slimevr.vr.trackers.TrackerConfig
import dev.slimevr.vr.trackers.TrackerPosition
import dev.slimevr.vr.trackers.TrackerStatus

class TrackerFrame(val designation: TrackerPosition?, rotation: Quaternion?, position: Vector3f?) :
	Tracker {
	var dataFlags = 0
	val rotation: Quaternion?
	val position: Vector3f?

	//#endregion
	override val trackerId = Tracker.getNextLocalTrackerId()

	init {
		if (designation != null) {
			dataFlags = dataFlags or TrackerFrameData.DESIGNATION.flag
		}
		this.rotation = rotation
		if (rotation != null) {
			dataFlags = dataFlags or TrackerFrameData.ROTATION.flag
		}
		this.position = position
		if (position != null) {
			dataFlags = dataFlags or TrackerFrameData.POSITION.flag
		}
	}

	fun hasData(flag: TrackerFrameData): Boolean {
		return flag.check(dataFlags)
	}

	//#region Tracker Interface Implementation
	override fun getRotation(store: Quaternion): Boolean {
		if (hasData(TrackerFrameData.ROTATION)) {
			store.set(rotation)
			return true
		}
		store.set(Quaternion.IDENTITY)
		return false
	}

	override fun getPosition(store: Vector3f): Boolean {
		if (hasData(TrackerFrameData.POSITION)) {
			store.set(position)
			return true
		}
		store.set(Vector3f.ZERO)
		return false
	}

	override val name: String
		get() = "TrackerFrame:/" + (designation?.designation ?: "null")
	override val status: TrackerStatus
		get() = TrackerStatus.OK

	override fun loadConfig(config: TrackerConfig) {
		throw UnsupportedOperationException("TrackerFrame does not implement configuration")
	}

	override fun saveConfig(config: TrackerConfig) {
		throw UnsupportedOperationException("TrackerFrame does not implement configuration")
	}

	override val confidenceLevel: Float
		get() = 1f

	override fun resetFull(reference: Quaternion) {
		throw UnsupportedOperationException("TrackerFrame does not implement calibration")
	}

	override fun resetYaw(reference: Quaternion) {
		throw UnsupportedOperationException("TrackerFrame does not implement calibration")
	}

	override fun tick() {
		throw UnsupportedOperationException("TrackerFrame does not implement this method")
	}

	override var bodyPosition: TrackerPosition?
		get() = designation
		set(position) {
			throw UnsupportedOperationException("TrackerFrame does not allow setting the body position")
		}

	override fun userEditable(): Boolean {
		return false
	}

	override fun hasRotation(): Boolean {
		return hasData(TrackerFrameData.ROTATION)
	}

	override fun hasPosition(): Boolean {
		return hasData(TrackerFrameData.POSITION)
	}

	override val isComputed: Boolean
		get() = true

	companion object {
		fun fromTracker(tracker: Tracker?): TrackerFrame? {
			if (tracker == null) {
				return null
			}

			// If the tracker is not ready
			if (tracker.status !== TrackerStatus.OK && tracker.status !== TrackerStatus.BUSY && tracker.status !== TrackerStatus.OCCLUDED) {
				return null
			}

			// If tracker has no data
			if (tracker.bodyPosition == null && !tracker.hasRotation() && !tracker.hasPosition()) {
				return null
			}
			var rotation: Quaternion? = null
			if (tracker.hasRotation()) {
				rotation = Quaternion()
				if (!tracker.getRotation(rotation)) {
					// If getting the rotation failed, set it back to null
					rotation = null
				}
			}
			var position: Vector3f? = null
			if (tracker.hasPosition()) {
				position = Vector3f()
				if (!tracker.getPosition(position)) {
					// If getting the position failed, set it back to null
					position = null
				}
			}
			return TrackerFrame(tracker.bodyPosition, rotation, position)
		}
	}
}
