package dev.slimevr.posestreamer

import dev.slimevr.poserecorder.PoseFrameIO
import dev.slimevr.poserecorder.PoseFrameSkeleton
import dev.slimevr.poserecorder.PoseFrames
import java.io.File

class PoseFrameStreamer(val frames: PoseFrames) :
	PoseStreamer(PoseFrameSkeleton(frames.trackers, null)) {

	constructor(path: String?) : this(File(path)) {}
	constructor(file: File?) : this(PoseFrameIO.readFromFile(file)) {}

	@Synchronized
	fun streamAllFrames() {
		val skeleton = skeleton as PoseFrameSkeleton
		for (i in 0 until frames.maxFrameCount) {
			skeleton.cursor = i
			skeleton.updatePose()
			captureFrame()
		}
	}
}
