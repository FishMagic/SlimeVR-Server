package dev.slimevr.vr.trackers

class SensorTap(tapBits: Int) {
	val doubleTap: Boolean

	init {
		doubleTap = tapBits and 0x40 > 0
	}

	override fun toString(): String {
		return "Tap{" + (if (doubleTap) "double" else "") + "}"
	}

	enum class TapAxis {
		X, Y, Z
	}
}
