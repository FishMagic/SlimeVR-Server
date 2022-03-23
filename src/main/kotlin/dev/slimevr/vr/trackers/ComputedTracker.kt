package dev.slimevr.vr.trackers

import com.jme3.math.Quaternion
import com.jme3.math.Vector3f

open class ComputedTracker(
	override val trackerId: Int,
	protected val serial: String,
	override val name: String,
	protected val hasRotation: Boolean,
	protected val hasPosition: Boolean
) :
	Tracker, TrackerWithTPS {
	val position = Vector3f()
	val rotation = Quaternion()
	override val confidenceLevel: Float
		get() = 1.0f
	override val isComputed: Boolean
		get() = true
	override val tps: Float
		get() = (-1).toFloat()
	override var status = TrackerStatus.DISCONNECTED
	override var bodyPosition: TrackerPosition? = null

	constructor(trackerId: Int, name: String, hasRotation: Boolean, hasPosition: Boolean) : this(
		trackerId,
		name,
		name,
		hasRotation,
		hasPosition
	)

	override fun saveConfig(config: TrackerConfig) {
		config.setDesignation(if (bodyPosition == null) null else bodyPosition!!.designation)
	}

	override fun loadConfig(config: TrackerConfig) {
		// Loading a config is an act of user editing, therefore it shouldn't not be allowed if editing is not allowed
		if (userEditable()) {
			bodyPosition = TrackerPosition.getByDesignation(config.designation)
		}
	}

	override fun getPosition(store: Vector3f): Boolean {
		store.set(position)
		return true
	}

	override fun getRotation(store: Quaternion): Boolean {
		store.set(rotation)
		return true
	}

	override fun resetFull(reference: Quaternion) {}
	override fun resetYaw(reference: Quaternion) {}


	override fun userEditable(): Boolean {
		return false
	}

	override fun dataTick() {}
	override fun tick() {}
	override fun hasRotation(): Boolean {
		return hasRotation
	}

	override fun hasPosition(): Boolean {
		return hasPosition
	}
}
