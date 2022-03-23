package dev.slimevr.poserecorder

enum class TrackerFrameData(id: Int) {
	DESIGNATION(0), ROTATION(1), POSITION(2);

	val flag: Int

	init {
		flag = 1 shl id
	}

	fun check(dataFlags: Int): Boolean {
		return dataFlags and flag != 0
	}
}
