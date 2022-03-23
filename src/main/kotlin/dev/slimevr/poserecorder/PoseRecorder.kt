package dev.slimevr.poserecorder

import dev.slimevr.VRServer
import dev.slimevr.util.ann.VRServerThread
import dev.slimevr.vr.trackers.Tracker
import io.eiren.util.collections.FastList
import io.eiren.util.logging.LogManager
import org.apache.commons.lang3.tuple.Pair
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future

class PoseRecorder(protected val server: VRServer) {
	protected var poseFrame: PoseFrames? = null
	protected var numFrames = -1
	protected var frameCursor = 0
	protected var frameRecordingInterval = 60L
	protected var nextFrameTimeMs = -1L
	protected var currentRecording: CompletableFuture<PoseFrames?>? = null
	var trackers = FastList<Pair<Tracker, PoseFrameTracker>>()

	init {
		server.addOnTick { onTick() }
	}

	@VRServerThread
	fun onTick() {
		if (numFrames <= 0) {
			return
		}
		val poseFrame = poseFrame
		val trackers: List<Pair<Tracker, PoseFrameTracker>> =
			trackers
		if (poseFrame == null || trackers == null) {
			return
		}
		if (frameCursor >= numFrames) {
			// If done and hasn't yet, send finished recording
			stopFrameRecording()
			return
		}
		val curTime = System.currentTimeMillis()
		if (curTime < nextFrameTimeMs) {
			return
		}
		nextFrameTimeMs += frameRecordingInterval

		// To prevent duplicate frames, make sure the frame time is always in the future
		if (nextFrameTimeMs <= curTime) {
			nextFrameTimeMs = curTime + frameRecordingInterval
		}

		// Make sure it's synchronized since this is the server thread interacting with
		// an unknown outside thread controlling this class
		synchronized(this) {

			// A stopped recording will be accounted for by an empty "trackers" list
			val cursor = frameCursor++
			for (tracker in trackers) {
				// Add a frame for each tracker
				tracker.right.addFrame(cursor, tracker.left)
			}

			// If done, send finished recording
			if (frameCursor >= numFrames) {
				stopFrameRecording()
			}
		}
	}

	@Synchronized
	fun startFrameRecording(numFrames: Int, intervalMs: Long): Future<PoseFrames?> {
		return startFrameRecording(numFrames, intervalMs, server.getAllTrackers())
	}

	@Synchronized
	fun startFrameRecording(numFrames: Int, intervalMs: Long, trackers: List<Tracker?>?): Future<PoseFrames?> {
		require(numFrames >= 1) { "numFrames must at least have a value of 1" }
		require(intervalMs >= 1) { "intervalMs must at least have a value of 1" }
		requireNotNull(trackers) { "trackers must not be null" }
		require(!trackers.isEmpty()) { "trackers must have at least one entry" }
		check(isReadyToRecord) { "PoseRecorder isn't ready to record!" }
		cancelFrameRecording()
		poseFrame = PoseFrames(trackers.size)

		// Update tracker list
		this.trackers.ensureCapacity(trackers.size)
		for (tracker in trackers) {
			// Ignore null and computed trackers
			if (tracker == null || tracker.isComputed) {
				continue
			}

			// Pair tracker with recording
			this.trackers.add(Pair.of(tracker, poseFrame!!.addTracker(tracker, numFrames)))
		}
		frameCursor = 0
		this.numFrames = numFrames
		frameRecordingInterval = intervalMs
		nextFrameTimeMs = -1L
		LogManager.log.info("[PoseRecorder] Recording $numFrames samples at a $intervalMs ms frame interval")
		currentRecording = CompletableFuture()
		return currentRecording!!
	}

	@Synchronized
	fun stopFrameRecording() {
		val currentRecording = currentRecording
		if (currentRecording != null && !currentRecording.isDone) {
			// Stop the recording, returning the frames recorded
			currentRecording.complete(poseFrame)
		}
		numFrames = -1
		frameCursor = 0
		trackers.clear()
		poseFrame = null
	}

	@Synchronized
	fun cancelFrameRecording() {
		val currentRecording = currentRecording
		if (currentRecording != null && !currentRecording.isDone) {
			// Cancel the current recording and return nothing
			currentRecording.cancel(true)
		}
		numFrames = -1
		frameCursor = 0
		trackers.clear()
		poseFrame = null
	}

	@get:Synchronized
	val isReadyToRecord: Boolean
		get() = server.trackersCount > 0

	@get:Synchronized
	val isRecording: Boolean
		get() = numFrames > frameCursor

	@Synchronized
	fun hasRecording(): Boolean {
		return currentRecording != null
	}

	@get:Synchronized
	val framesAsync: Future<PoseFrames?>?
		get() = currentRecording

	@get:Throws(ExecutionException::class, InterruptedException::class)
	@get:Synchronized
	val frames: PoseFrames?
		get() {
			val currentRecording = currentRecording
			return currentRecording?.get()
		}
}
