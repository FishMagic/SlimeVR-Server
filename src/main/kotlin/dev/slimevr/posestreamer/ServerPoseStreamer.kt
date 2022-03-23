package dev.slimevr.posestreamer

import dev.slimevr.VRServer
import dev.slimevr.util.ann.VRServerThread
import dev.slimevr.vr.processor.skeleton.HumanSkeleton

class ServerPoseStreamer(protected val server: VRServer) : TickPoseStreamer(null) {
	init {

		// Register callbacks/events
		server.addSkeletonUpdatedCallback { skeleton: HumanSkeleton? ->
			onSkeletonUpdated(
				skeleton
			)
		}
		server.addOnTick { onTick() }
	}

	@VRServerThread
	fun onSkeletonUpdated(skeleton: HumanSkeleton?) {
		this.skeleton = skeleton!!
	}

	@VRServerThread
	fun onTick() {
		super.doTick()
	}
}
