package dev.slimevr.vr.processor

import dev.slimevr.VRServer
import dev.slimevr.util.ann.VRServerThread
import dev.slimevr.vr.processor.skeleton.HumanSkeleton
import dev.slimevr.vr.processor.skeleton.SimpleSkeleton
import dev.slimevr.vr.processor.skeleton.SkeletonConfig
import dev.slimevr.vr.processor.skeleton.SkeletonConfigValue
import dev.slimevr.vr.trackers.*
import io.eiren.util.ann.ThreadSafe
import io.eiren.util.collections.FastList
import java.util.function.Consumer

class HumanPoseProcessor(private val server: VRServer, hmd: HMDTracker?) {
	val computedTrackers: MutableList<ComputedHumanPoseTracker> = FastList()
	private val onSkeletonUpdated: MutableList<Consumer<HumanSkeleton?>> = FastList()
	var skeleton: HumanSkeleton? = null
		private set

	init {
		computedTrackers.add(
			ComputedHumanPoseTracker(
				Tracker.getNextLocalTrackerId(),
				ComputedHumanPoseTrackerPosition.WAIST,
				TrackerRole.WAIST
			)
		)
		computedTrackers.add(
			ComputedHumanPoseTracker(
				Tracker.getNextLocalTrackerId(),
				ComputedHumanPoseTrackerPosition.LEFT_FOOT,
				TrackerRole.LEFT_FOOT
			)
		)
		computedTrackers.add(
			ComputedHumanPoseTracker(
				Tracker.getNextLocalTrackerId(),
				ComputedHumanPoseTrackerPosition.RIGHT_FOOT,
				TrackerRole.RIGHT_FOOT
			)
		)
		computedTrackers.add(
			ComputedHumanPoseTracker(
				Tracker.getNextLocalTrackerId(),
				ComputedHumanPoseTrackerPosition.CHEST,
				TrackerRole.CHEST
			)
		)
		computedTrackers.add(
			ComputedHumanPoseTracker(
				Tracker.getNextLocalTrackerId(),
				ComputedHumanPoseTrackerPosition.LEFT_KNEE,
				TrackerRole.LEFT_KNEE
			)
		)
		computedTrackers.add(
			ComputedHumanPoseTracker(
				Tracker.getNextLocalTrackerId(),
				ComputedHumanPoseTrackerPosition.RIGHT_KNEE,
				TrackerRole.RIGHT_KNEE
			)
		)
		computedTrackers.add(
			ComputedHumanPoseTracker(
				Tracker.getNextLocalTrackerId(),
				ComputedHumanPoseTrackerPosition.LEFT_ELBOW,
				TrackerRole.LEFT_ELBOW
			)
		)
		computedTrackers.add(
			ComputedHumanPoseTracker(
				Tracker.getNextLocalTrackerId(),
				ComputedHumanPoseTrackerPosition.RIGHT_ELBOW,
				TrackerRole.RIGHT_ELBOW
			)
		)
	}

	@VRServerThread
	fun addSkeletonUpdatedCallback(consumer: Consumer<HumanSkeleton?>) {
		onSkeletonUpdated.add(consumer)
		if (skeleton != null) consumer.accept(skeleton)
	}

	@ThreadSafe
	fun setSkeletonConfig(key: SkeletonConfigValue?, newLength: Float) {
		if (skeleton != null) skeleton!!.skeletonConfig!!.setConfig(key!!, newLength)
	}

	@ThreadSafe
	fun resetSkeletonConfig(key: SkeletonConfigValue?) {
		if (skeleton != null) skeleton!!.resetSkeletonConfig(key)
	}

	@ThreadSafe
	fun resetAllSkeletonConfigs() {
		if (skeleton != null) skeleton!!.resetAllSkeletonConfigs()
	}

	@get:ThreadSafe
	val skeletonConfig: SkeletonConfig?
		get() = skeleton!!.skeletonConfig

	@ThreadSafe
	fun getSkeletonConfig(key: SkeletonConfigValue?): Float {
		return if (skeleton != null) {
			skeleton!!.skeletonConfig!!.getConfig(key)
		} else 0.0f
	}

	@VRServerThread
	fun trackerAdded(tracker: Tracker?) {
		updateSkeletonModel()
	}

	@VRServerThread
	fun trackerUpdated(tracker: Tracker?) {
		updateSkeletonModel()
	}

	@VRServerThread
	private fun updateSkeletonModel() {
		disconnectAllTrackers()
		skeleton = SimpleSkeleton(server, computedTrackers)
		for (i in onSkeletonUpdated.indices) onSkeletonUpdated[i].accept(skeleton)
	}

	@VRServerThread
	private fun disconnectAllTrackers() {
		for (i in computedTrackers.indices) {
			computedTrackers[i]!!.status = TrackerStatus.DISCONNECTED
		}
	}

	@VRServerThread
	fun update() {
		if (skeleton != null) skeleton!!.updatePose()
	}

	@VRServerThread
	fun resetTrackers() {
		if (skeleton != null) skeleton!!.resetTrackersFull()
	}

	@VRServerThread
	fun resetTrackersYaw() {
		if (skeleton != null) skeleton!!.resetTrackersYaw()
	}
}
