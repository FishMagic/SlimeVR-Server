package dev.slimevr.poserecorder

import dev.slimevr.VRServer
import dev.slimevr.vr.processor.ComputedHumanPoseTracker
import dev.slimevr.vr.processor.skeleton.SimpleSkeleton
import dev.slimevr.vr.processor.skeleton.SkeletonConfigValue
import dev.slimevr.vr.trackers.Tracker

class PoseFrameSkeleton : SimpleSkeleton {
	var cursor = 0
		private set

	protected constructor(computedTrackers: List<ComputedHumanPoseTracker?>?) : super(computedTrackers) {}
	constructor(server: VRServer, computedTrackers: List<ComputedHumanPoseTracker?>?) : super(
		server,
		computedTrackers
	) {
	}

	constructor(trackers: List<Tracker?>?, computedTrackers: List<ComputedHumanPoseTracker?>?) : super(
		trackers,
		computedTrackers
	) {
	}

	constructor(
		trackers: List<Tracker?>?,
		computedTrackers: List<ComputedHumanPoseTracker?>?,
		configs: Map<SkeletonConfigValue?, Float?>?,
		altConfigs: Map<SkeletonConfigValue?, Float?>?
	) : super(trackers, computedTrackers, configs, altConfigs) {
	}

	constructor(
		trackers: List<Tracker?>?,
		computedTrackers: List<ComputedHumanPoseTracker?>?,
		configs: Map<SkeletonConfigValue?, Float?>?
	) : super(trackers, computedTrackers, configs) {
	}

	private fun limitCursor(): Int {
		if (cursor < 0) {
			cursor = 0
		}
		return cursor
	}

	fun setCursor(index: Int): Int {
		cursor = index
		return limitCursor()
	}

	@JvmOverloads
	fun incrementCursor(increment: Int = 1): Int {
		cursor += increment
		return limitCursor()
	}

	// Get tracker for specific frame
	override fun trackerPreUpdate(tracker: Tracker?): Tracker? {
		if (tracker is PoseFrameTracker) {
			// Return frame if available, otherwise return the original tracker
			val frame = tracker.safeGetFrame(cursor)
			return frame ?: tracker
		}
		return tracker
	}
}
