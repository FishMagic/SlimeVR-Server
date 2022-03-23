package dev.slimevr.poserecorder

import dev.slimevr.vr.trackers.Tracker
import io.eiren.util.collections.FastList

class PoseFrames : Iterable<Array<TrackerFrame?>?> {
	private val trackers: FastList<PoseFrameTracker>

	constructor(trackers: FastList<PoseFrameTracker>) {
		this.trackers = trackers
	}

	@JvmOverloads
	constructor(initialCapacity: Int = 5) {
		trackers = FastList(initialCapacity)
	}

	fun addTracker(tracker: PoseFrameTracker): PoseFrameTracker {
		trackers.add(tracker)
		return tracker
	}

	@JvmOverloads
	fun addTracker(tracker: Tracker, initialCapacity: Int = 5): PoseFrameTracker {
		return addTracker(PoseFrameTracker(tracker.name, initialCapacity))
	}

	fun removeTracker(index: Int): PoseFrameTracker {
		return trackers.removeAt(index)
	}

	fun removeTracker(tracker: PoseFrameTracker): PoseFrameTracker {
		trackers.remove(tracker)
		return tracker
	}

	fun clearTrackers() {
		trackers.clear()
	}

	fun fakeClearTrackers() {
		trackers.fakeClear()
	}

	val trackerCount: Int
		get() = trackers.size

	fun getTrackers(): List<PoseFrameTracker> {
		return trackers
	}

	val maxFrameCount: Int
		get() {
			var maxFrames = 0
			for (i in trackers.indices) {
				val tracker = trackers[i]
				if (tracker != null && tracker.frameCount > maxFrames) {
					maxFrames = tracker.frameCount
				}
			}
			return maxFrames
		}

	fun getFrames(frameIndex: Int, buffer: Array<TrackerFrame?>): Int {
		for (i in trackers.indices) {
			val tracker = trackers[i]
			buffer[i] = tracker?.safeGetFrame(frameIndex)
		}
		return trackers.size
	}

	fun getFrames(frameIndex: Int, buffer: MutableList<TrackerFrame?>): Int {
		for (i in trackers.indices) {
			val tracker = trackers[i]
			buffer.add(i, tracker?.safeGetFrame(frameIndex))
		}
		return trackers.size
	}

	fun getFrames(frameIndex: Int): Array<TrackerFrame?> {
		val trackerFrames = arrayOfNulls<TrackerFrame>(trackers.size)
		getFrames(frameIndex, trackerFrames)
		return trackerFrames
	}

	override fun iterator(): Iterator<Array<TrackerFrame?>?> {
		return PoseFrameIterator(this)
	}

	inner class PoseFrameIterator(private val poseFrame: PoseFrames) :
		MutableIterator<Array<TrackerFrame?>?> {
		private val trackerFrameBuffer: Array<TrackerFrame?>
		private var cursor = 0

		init {
			trackerFrameBuffer = arrayOfNulls(poseFrame.trackerCount)
		}

		override fun hasNext(): Boolean {
			if (trackers.isEmpty()) {
				return false
			}
			for (i in trackers.indices) {
				val tracker = trackers[i]
				if (tracker != null && cursor < tracker.frameCount) {
					return true
				}
			}
			return false
		}

		override fun next(): Array<TrackerFrame?> {
			if (!hasNext()) {
				throw NoSuchElementException()
			}
			poseFrame.getFrames(cursor++, trackerFrameBuffer)
			return trackerFrameBuffer
		}

		override fun remove() {
		}
	}
}
