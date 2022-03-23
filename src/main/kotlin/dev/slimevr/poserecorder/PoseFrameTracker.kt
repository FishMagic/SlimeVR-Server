package dev.slimevr.poserecorder

import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import dev.slimevr.vr.trackers.Tracker
import dev.slimevr.vr.trackers.TrackerConfig
import dev.slimevr.vr.trackers.TrackerPosition
import dev.slimevr.vr.trackers.TrackerStatus
import io.eiren.util.collections.FastList

class PoseFrameTracker(name: String?, frames: FastList<TrackerFrame>?) : Tracker,
	Iterable<TrackerFrame?> {
	override val name: String
	private val frames: FastList<TrackerFrame>
	var cursor = 0
		private set
	override val trackerId = Tracker.getNextLocalTrackerId()

	init {
		if (frames == null) {
			throw NullPointerException("frames must not be null")
		}
		this.name = name ?: ""
		this.frames = frames
	}

	@JvmOverloads
	constructor(name: String?, initialCapacity: Int = 5) : this(name, FastList<TrackerFrame>(initialCapacity)) {
	}

	private fun limitCursor(): Int {
		if (cursor < 0 || frames.isEmpty()) {
			cursor = 0
		} else if (cursor >= frames.size) {
			cursor = frames.size - 1
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

	val frameCount: Int
		get() = frames.size

	fun addFrame(index: Int, trackerFrame: TrackerFrame): TrackerFrame {
		frames.add(index, trackerFrame)
		return trackerFrame
	}

	fun addFrame(index: Int, tracker: Tracker?): TrackerFrame {
		return addFrame(index, TrackerFrame.fromTracker(tracker))
	}

	fun addFrame(trackerFrame: TrackerFrame): TrackerFrame {
		frames.add(trackerFrame)
		return trackerFrame
	}

	fun addFrame(tracker: Tracker?): TrackerFrame {
		return addFrame(TrackerFrame.fromTracker(tracker))
	}

	fun removeFrame(index: Int): TrackerFrame {
		val trackerFrame = frames.removeAt(index)
		limitCursor()
		return trackerFrame
	}

	fun removeFrame(trackerFrame: TrackerFrame): TrackerFrame {
		frames.remove(trackerFrame)
		limitCursor()
		return trackerFrame
	}

	fun clearFrames() {
		frames.clear()
		limitCursor()
	}

	fun fakeClearFrames() {
		frames.fakeClear()
		limitCursor()
	}

	fun getFrame(index: Int): TrackerFrame {
		return frames[index]
	}

	val frame: TrackerFrame
		get() = getFrame(cursor)

	@JvmOverloads
	fun safeGetFrame(index: Int = cursor): TrackerFrame? {
		return try {
			getFrame(index)
		} catch (e: Exception) {
			null
		}
	}

	//#region Tracker Interface Implementation
	override fun getRotation(store: Quaternion): Boolean {
		val frame = safeGetFrame()
		if (frame != null && frame.hasData(TrackerFrameData.ROTATION)) {
			store.set(frame.rotation)
			return true
		}
		store.set(Quaternion.IDENTITY)
		return false
	}

	override fun getPosition(store: Vector3f): Boolean {
		val frame = safeGetFrame()
		if (frame != null && frame.hasData(TrackerFrameData.POSITION)) {
			store.set(frame.position)
			return true
		}
		store.set(Vector3f.ZERO)
		return false
	}

	override val status: TrackerStatus
		get() = TrackerStatus.OK

	override fun loadConfig(config: TrackerConfig) {
		throw UnsupportedOperationException("PoseFrameTracker does not implement configuration")
	}

	override fun saveConfig(config: TrackerConfig) {
		throw UnsupportedOperationException("PoseFrameTracker does not implement configuration")
	}

	override val confidenceLevel: Float
		get() = 1f

	override fun resetFull(reference: Quaternion) {
		throw UnsupportedOperationException("PoseFrameTracker does not implement calibration")
	}

	override fun resetYaw(reference: Quaternion) {
		throw UnsupportedOperationException("PoseFrameTracker does not implement calibration")
	}

	override fun tick() {
		throw UnsupportedOperationException("PoseFrameTracker does not implement this method")
	}

	override var bodyPosition: TrackerPosition?
		get() {
			val frame = safeGetFrame()
			return frame?.designation
		}
		set(position) {
			throw UnsupportedOperationException("PoseFrameTracker does not allow setting the body position")
		}

	override fun userEditable(): Boolean {
		return false
	}

	override fun hasRotation(): Boolean {
		val frame = safeGetFrame()
		return frame != null && frame.hasData(TrackerFrameData.ROTATION)
	}

	override fun hasPosition(): Boolean {
		val frame = safeGetFrame()
		return frame != null && frame.hasData(TrackerFrameData.POSITION)
	}

	override val isComputed: Boolean
		get() = true

	//#endregion
	override fun iterator(): Iterator<TrackerFrame> {
		return frames.iterator()
	}
}
