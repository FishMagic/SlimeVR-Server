package dev.slimevr.vr.processor

import dev.slimevr.vr.trackers.ComputedTracker
import dev.slimevr.vr.trackers.ShareableTracker
import dev.slimevr.vr.trackers.TrackerRole
import dev.slimevr.vr.trackers.TrackerWithTPS
import io.eiren.util.BufferedTimer

class ComputedHumanPoseTracker(
	trackerId: Int,
	skeletonPosition: ComputedHumanPoseTrackerPosition,
	override val trackerRole: TrackerRole
) :
	ComputedTracker(trackerId, "human://" + skeletonPosition.name, true, true), TrackerWithTPS, ShareableTracker {
	private var timer = BufferedTimer(1f)
//	override fun getTPS(): Float {
//		return timer.averageFPS
//	}
	override val tps: Float
		get() = timer.averageFPS

	override fun dataTick() {
		timer.update()
	}

}
