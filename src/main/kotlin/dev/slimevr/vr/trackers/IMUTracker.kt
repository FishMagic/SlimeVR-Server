package dev.slimevr.vr.trackers

import com.jme3.math.FastMath
import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import dev.slimevr.vr.trackers.udp.TrackersUDPServer
import io.eiren.util.BufferedTimer

open class IMUTracker(
	override val trackerId: Int,
	override val name: String,
	override val descriptiveName: String,
	protected val server: TrackersUDPServer
) :
	Tracker, TrackerWithTPS, TrackerWithBattery {
	//public final Vector3f gyroVector = new Vector3f();
	//public final Vector3f accelVector = new Vector3f();
	val magVector = Vector3f()
	val rotQuaternion = Quaternion()
	val rotMagQuaternion = Quaternion()
	val rotAdjust = Quaternion()
	protected val correction = Quaternion()
	protected var mounting: TrackerMountingRotation? = null
	override var status = TrackerStatus.OK
	override var confidenceLevel = 0f
	override var batteryVoltage = 0f
	override var batteryLevel = 0f
	override val tps: Float
		get() = timer.averageFPS
	var calibrationStatus = 0
	var magCalibrationStatus = 0
	var magnetometerAccuracy = 0f
	protected var magnetometerCalibrated = false
	var hasNewCorrectionData = false
	protected var timer = BufferedTimer(1f)
	var ping = -1
	var signalStrength = -1
	var temperature = 0f
	override var bodyPosition: TrackerPosition? = null

	override fun saveConfig(config: TrackerConfig) {
		config.setDesignation(if (bodyPosition == null) null else bodyPosition!!.designation)
		config.mountingRotation = if (mounting != null) mounting!!.name else null
	}

	override fun loadConfig(config: TrackerConfig) {
		// Loading a config is an act of user editing, therefore it shouldn't not be allowed if editing is not allowed
		if (userEditable()) {
			if (config.mountingRotation != null) {
				try {
					mounting = TrackerMountingRotation.valueOf(config.mountingRotation)
				} catch (e: Exception) { // FORWARD was renamed to FRONT
					mounting = TrackerMountingRotation.FRONT
					config.mountingRotation = "FRONT"
				}
				if (mounting != null) {
					rotAdjust.set(mounting!!.quaternion)
				} else {
					rotAdjust.loadIdentity()
				}
			} else {
				rotAdjust.loadIdentity()
			}
			bodyPosition = TrackerPosition.getByDesignation(config.designation)
		}
	}

	var mountingRotation: TrackerMountingRotation?
		get() = mounting
		set(mr) {
			mounting = mr
			if (mounting != null) {
				rotAdjust.set(mounting!!.quaternion)
			} else {
				rotAdjust.loadIdentity()
			}
		}

	override fun tick() {
		if (magnetometerCalibrated && hasNewCorrectionData) {
			hasNewCorrectionData = false
			if (magnetometerAccuracy <= MAX_MAG_CORRECTION_ACCURACY) {
				// Adjust gyro rotation to match magnetometer rotation only if magnetometer
				// accuracy is within the parameters
				calculateLiveMagnetometerCorrection()
			}
		}
	}

	override fun getPosition(store: Vector3f): Boolean {
		store[0f, 0f] = 0f
		return false
	}

	override fun getRotation(store: Quaternion): Boolean {
		store.set(rotQuaternion)
		//correction.mult(store, store); // Correction is not used now to prevent accidental errors while debugging other things
		store.multLocal(rotAdjust)
		return true
	}

	fun getCorrection(store: Quaternion) {
		store.set(correction)
	}

	override fun dataTick() {
		timer.update()
	}

	fun setConfidence(newConf: Float) {
		confidenceLevel = newConf
	}

	override fun resetFull(reference: Quaternion) {
		resetYaw(reference)
	}

	/**
	 * Does not perform actual gyro reset to reference, that's the task of
	 * reference adjusted tracker. Only aligns gyro with magnetometer if
	 * it's reliable
	 */
	override fun resetYaw(reference: Quaternion) {
		if (magCalibrationStatus >= CalibrationAccuracy.HIGH.status) {
			magnetometerCalibrated = true
			// During calibration set correction to match magnetometer readings exactly
			// TODO : Correct only yaw
			correction.set(rotQuaternion).inverseLocal().multLocal(rotMagQuaternion)
		}
	}

	/**
	 * Calculate correction between normal and magnetometer
	 * readings up to accuracy threshold
	 */
	protected fun calculateLiveMagnetometerCorrection() {
		// TODO Magic, correct only yaw
		// TODO Print "jump" length when correcting if it's more than 1 degree
	}

	override fun userEditable(): Boolean {
		return true
	}

	override fun hasRotation(): Boolean {
		return true
	}

	override fun hasPosition(): Boolean {
		return false
	}

	override val isComputed: Boolean
		get() = false

	enum class CalibrationAccuracy(val status: Int) {
		UNRELIABLE(0), LOW(1), MEDIUM(2), HIGH(3);

		companion object {
			private val byStatus = arrayOfNulls<CalibrationAccuracy>(4)
			fun getByStatus(status: Int): CalibrationAccuracy? {
				return if (status < 0 || status > 3) null else byStatus[status]
			}

			init {
				for (ca in values()) byStatus[ca.status] = ca
			}
		}
	}

	companion object {
		const val MAX_MAG_CORRECTION_ACCURACY = 5 * FastMath.RAD_TO_DEG
	}
}
