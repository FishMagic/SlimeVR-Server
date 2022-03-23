package dev.slimevr.vr.trackers

import com.jme3.math.Quaternion
import com.jme3.math.Vector3f

class ReferenceAdjustedTracker<E : Tracker?>(val tracker: E) : Tracker {
	val yawFix = Quaternion()
	val gyroFix = Quaternion()
	val attachmentFix = Quaternion()
	protected var confidenceMultiplier = 1.0f

	override fun userEditable(): Boolean {
		return tracker!!.userEditable()
	}

	override fun loadConfig(config: TrackerConfig) {
		tracker!!.loadConfig(config)
	}

	override fun saveConfig(config: TrackerConfig) {
		tracker!!.saveConfig(config)
	}

	/**
	 * Reset the tracker so that it's current rotation
	 * is counted as (0, <HMD Yaw>, 0). This allows tracker
	 * to be strapped to body at any pitch and roll.
	 *
	 * Performs [.resetYaw] for yaw
	 * drift correction.
	</HMD> */
	override fun resetFull(reference: Quaternion) {
		tracker!!.resetFull(reference)
		fixGyroscope()
		val sensorRotation = Quaternion()
		tracker.getRotation(sensorRotation)
		gyroFix.mult(sensorRotation, sensorRotation)
		attachmentFix.set(sensorRotation).inverseLocal()
		fixYaw(reference)
	}

	/**
	 * Reset the tracker so that it's current yaw rotation
	 * is counted as <HMD Yaw>. This allows the tracker
	 * to have yaw independent of the HMD. Tracker should
	 * still report yaw as if it was mounted facing HMD,
	 * mounting position should be corrected in the source.
	</HMD> */
	override fun resetYaw(reference: Quaternion) {
		tracker!!.resetYaw(reference)
		fixYaw(reference)
	}

	private fun fixYaw(reference: Quaternion) {
		// Use only yaw HMD rotation
		val targetTrackerRotation = Quaternion(reference)
		val angles = FloatArray(3)
		targetTrackerRotation.toAngles(angles)
		targetTrackerRotation.fromAngles(0f, angles[1], 0f)
		val sensorRotation = Quaternion()
		tracker!!.getRotation(sensorRotation)
		gyroFix.mult(sensorRotation, sensorRotation)
		sensorRotation.multLocal(attachmentFix)
		sensorRotation.toAngles(angles)
		sensorRotation.fromAngles(0f, angles[1], 0f)
		yawFix.set(sensorRotation).inverseLocal().multLocal(targetTrackerRotation)
	}

	private fun fixGyroscope() {
		val angles = FloatArray(3)
		val sensorRotation = Quaternion()
		tracker!!.getRotation(sensorRotation)
		sensorRotation.toAngles(angles)
		sensorRotation.fromAngles(0f, angles[1], 0f)
		gyroFix.set(sensorRotation).inverseLocal()
	}

	protected fun adjustInternal(store: Quaternion) {
		gyroFix.mult(store, store)
		store.multLocal(attachmentFix)
		yawFix.mult(store, store)
	}

	override fun getRotation(store: Quaternion): Boolean {
		tracker!!.getRotation(store)
		adjustInternal(store)
		return true
	}

	override fun getPosition(store: Vector3f): Boolean {
		return tracker!!.getPosition(store)
	}

	override val name: String
		get() = tracker!!.name + "/adj"
	override val status: TrackerStatus?
		get() = tracker!!.status
	override val confidenceLevel: Float
		get() = tracker!!.confidenceLevel * confidenceMultiplier
	override var bodyPosition: TrackerPosition?
		get() = tracker!!.bodyPosition
		set(position) {
			tracker!!.bodyPosition = position
		}

	override fun tick() {
		tracker!!.tick()
	}

	override fun hasRotation(): Boolean {
		return tracker!!.hasRotation()
	}

	override fun hasPosition(): Boolean {
		return tracker!!.hasPosition()
	}

	override val isComputed: Boolean
		get() = tracker!!.isComputed
	override val trackerId: Int
		get() = tracker!!.trackerId
	override val descriptiveName: String?
		get() = tracker!!.descriptiveName
}
