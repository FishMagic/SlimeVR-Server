package dev.slimevr.posestreamer

import dev.slimevr.vr.processor.skeleton.HumanSkeleton
import io.eiren.util.logging.LogManager
import java.io.IOException

open class PoseStreamer(@get:Synchronized var skeleton: HumanSkeleton) {
	protected var frameRecordingInterval = 60L
	protected var poseFileStream: PoseDataStream? = null

	@Synchronized
	fun captureFrame() {
		// Make sure the stream is open before trying to write
		if (this.poseFileStream!!.isClosed) {
			return
		}
		try {
			this.poseFileStream!!.writeFrame(skeleton)
		} catch (e: Exception) {
			// Handle any exceptions without crashing the program
			LogManager.log.severe("[PoseStreamer] Exception while saving frame", e)
		}
	}

	@get:Synchronized
	@set:Synchronized
	var frameInterval: Long
		get() = frameRecordingInterval
		set(intervalMs) {
			require(intervalMs >= 1) { "intervalMs must at least have a value of 1" }
			frameRecordingInterval = intervalMs
		}

	@Synchronized
	@Throws(IOException::class)
	open fun setOutput(poseFileStream: PoseDataStream) {
		poseFileStream.writeHeader(skeleton, this)
		this.poseFileStream = poseFileStream
	}

	@Synchronized
	@Throws(IOException::class)
	fun setOutput(poseFileStream: PoseDataStream, intervalMs: Long) {
		frameInterval = intervalMs
		setOutput(poseFileStream)
	}

	@Synchronized
	fun getOutput(): PoseDataStream? {
		return this.poseFileStream
	}

	@Synchronized
	@Throws(IOException::class)
	fun closeOutput() {
		val poseFileStream = this.poseFileStream
		if (poseFileStream != null) {
			closeOutput(poseFileStream)
			this.poseFileStream = null
		}
	}

	@Synchronized
	@Throws(IOException::class)
	fun closeOutput(poseFileStream: PoseDataStream?) {
		if (poseFileStream != null) {
			poseFileStream.writeFooter(skeleton)
			poseFileStream.close()
		}
	}
}
