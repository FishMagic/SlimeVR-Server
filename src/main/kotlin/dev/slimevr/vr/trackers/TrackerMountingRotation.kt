package dev.slimevr.vr.trackers

import com.jme3.math.FastMath
import com.jme3.math.Quaternion

enum class TrackerMountingRotation(val angle: Float) {
	FRONT(180f), LEFT(90f), BACK(0f), RIGHT(-90f);

	val quaternion = Quaternion()

	init {
		quaternion.fromAngles(0f, angle * FastMath.DEG_TO_RAD, 0f)
	}

	companion object {
		val values = values()
	}
}
