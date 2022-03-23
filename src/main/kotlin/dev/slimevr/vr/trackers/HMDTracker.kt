package dev.slimevr.vr.trackers

import io.eiren.util.BufferedTimer

class HMDTracker(name: String?) : ComputedTracker(0, name!!, true, true), TrackerWithTPS {
	protected var timer = BufferedTimer(1f)

	init {
		bodyPosition = TrackerPosition.HMD
	}

	override val tps: Float
		get() = timer.averageFPS

	override fun dataTick() {
		timer.update()
	}

	override val isComputed: Boolean
		get() = false
}
