package dev.slimevr.posestreamer

import dev.slimevr.vr.processor.skeleton.HumanSkeleton
import java.io.IOException

open class TickPoseStreamer(skeleton: HumanSkeleton?) : PoseStreamer(skeleton) {
	protected var nextFrameTimeMs = -1L
	fun doTick() {
		val poseFileStream: PoseDataStream = this.poseFileStream ?: return
		val skeleton = skeleton ?: return
		val curTime = System.currentTimeMillis()
		if (curTime < nextFrameTimeMs) {
			return
		}
		nextFrameTimeMs += frameRecordingInterval

		// To prevent duplicate frames, make sure the frame time is always in the future
		if (nextFrameTimeMs <= curTime) {
			nextFrameTimeMs = curTime + frameRecordingInterval
		}
		captureFrame()
	}

	@Synchronized
	@Throws(IOException::class)
	override fun setOutput(poseDataStream: PoseDataStream) {
		super.setOutput(poseDataStream)
		nextFrameTimeMs = -1L	// Reset the frame timing
	}

}
