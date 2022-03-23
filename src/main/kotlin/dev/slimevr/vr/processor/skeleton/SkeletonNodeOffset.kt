package dev.slimevr.vr.processor.skeleton

enum class SkeletonNodeOffset {
	HEAD, NECK, CHEST, CHEST_TRACKER, WAIST, HIP, HIP_TRACKER, LEFT_HIP, RIGHT_HIP, KNEE, KNEE_TRACKER, ANKLE, FOOT, FOOT_TRACKER, HAND, ELBOW, ELBOW_TRACKER;

	companion object {
		operator fun get(i: Int): SkeletonNodeOffset {
			return values[i]
		}

		val values = values()
	}


}
