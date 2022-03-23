package dev.slimevr.vr.processor.skeleton

import dev.slimevr.util.ann.VRServerThread
import dev.slimevr.vr.processor.TransformNode
import io.eiren.util.ann.ThreadSafe

abstract class HumanSkeleton {
	@VRServerThread
	abstract fun updatePose()

	@get:ThreadSafe
	abstract val rootNode: TransformNode?

	@get:ThreadSafe
	abstract val allNodes: Array<TransformNode>

	@get:ThreadSafe
	abstract val skeletonConfig: SkeletonConfig?

	@ThreadSafe
	abstract fun resetSkeletonConfig(config: SkeletonConfigValue?)
	@ThreadSafe
	fun resetAllSkeletonConfigs() {
		for (config in SkeletonConfigValue.values) {
			resetSkeletonConfig(config)
		}
	}

	@VRServerThread
	abstract fun resetTrackersFull()
	@VRServerThread
	abstract fun resetTrackersYaw()
}
