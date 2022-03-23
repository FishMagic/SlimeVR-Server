package dev.slimevr.vr.trackers

import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import java.util.concurrent.atomic.AtomicInteger

interface Tracker {
	fun getPosition(store: Vector3f): Boolean
	fun getRotation(store: Quaternion): Boolean
	val name: String?
	val status: TrackerStatus?

	fun loadConfig(config: TrackerConfig)
	fun saveConfig(config: TrackerConfig)
	val confidenceLevel: Float

	fun resetFull(reference: Quaternion)
	fun resetYaw(reference: Quaternion)
	fun tick()
	var bodyPosition: TrackerPosition?

	fun userEditable(): Boolean
	fun hasRotation(): Boolean
	fun hasPosition(): Boolean
	val isComputed: Boolean
	val trackerId: Int
	val descriptiveName: String?
		get() = name

	companion object {
		val nextLocalTrackerId = AtomicInteger()
		fun getNextLocalTrackerId(): Int {
			return nextLocalTrackerId.incrementAndGet()
		}
	}
}
