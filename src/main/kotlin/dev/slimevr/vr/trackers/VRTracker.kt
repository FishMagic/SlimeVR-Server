package dev.slimevr.vr.trackers

import io.eiren.util.BufferedTimer

class VRTracker : ComputedTracker {
	protected var timer = BufferedTimer(1f)

	constructor(id: Int, serial: String?, name: String?, hasRotation: Boolean, hasPosition: Boolean) : super(
		id,
		serial!!, name!!, hasRotation, hasPosition
	) {
	}

	constructor(id: Int, name: String?, hasRotation: Boolean, hasPosition: Boolean) : super(
		id,
		name!!, name, hasRotation, hasPosition
	) {
	}

	override val tps: Float
		get() = timer.averageFPS

	override fun dataTick() {
		timer.update()
	}

	override fun userEditable(): Boolean {
		return true
	}

	override val isComputed: Boolean
		get() = false
}
