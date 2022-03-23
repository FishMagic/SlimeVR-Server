package dev.slimevr.vr.trackers

object TrackerUtils {
	fun findTrackerForBodyPosition(allTrackers: Array<Tracker?>, position: TrackerPosition?): Tracker? {
		if (position == null) return null
		for (i in allTrackers.indices) {
			val t: Tracker? = allTrackers[i]
			if (t != null && t.bodyPosition === position) return t
		}
		return null
	}

	fun findTrackerForBodyPosition(allTrackers: List<Tracker?>, position: TrackerPosition?): Tracker? {
		if (position == null) return null
		for (i in allTrackers.indices) {
			val t: Tracker? = allTrackers[i]
			if (t != null && t.bodyPosition === position) return t
		}
		return null
	}

	fun findTrackerForBodyPosition(
		allTrackers: List<Tracker?>,
		position: TrackerPosition?,
		altPosition: TrackerPosition?
	): Tracker? {
		val t = findTrackerForBodyPosition(allTrackers, position)
		return t ?: findTrackerForBodyPosition(allTrackers, altPosition)
	}

	fun findTrackerForBodyPosition(
		allTrackers: Array<Tracker?>,
		position: TrackerPosition?,
		altPosition: TrackerPosition?,
		secondAltPosition: TrackerPosition?
	): Tracker? {
		var t = findTrackerForBodyPosition(allTrackers, position)
		if (t != null) return t
		t = findTrackerForBodyPosition(allTrackers, altPosition)
		return t ?: findTrackerForBodyPosition(allTrackers, secondAltPosition)
	}

	fun findTrackerForBodyPositionOrEmpty(
		allTrackers: List<Tracker?>,
		position: TrackerPosition?,
		altPosition: TrackerPosition?,
		secondAltPosition: TrackerPosition?
	): Tracker? {
		var t = findTrackerForBodyPosition(allTrackers, position)
		if (t != null) return t
		t = findTrackerForBodyPosition(allTrackers, altPosition)
		if (t != null) return t
		t = findTrackerForBodyPosition(allTrackers, secondAltPosition)
		return t ?: ComputedTracker(Tracker.getNextLocalTrackerId(), "Empty tracker", false, false)
	}

	fun findTrackerForBodyPositionOrEmpty(
		allTrackers: Array<Tracker?>,
		position: TrackerPosition?,
		altPosition: TrackerPosition?
	): Tracker {
		var t = findTrackerForBodyPosition(allTrackers, position)
		if (t != null) return t
		t = findTrackerForBodyPosition(allTrackers, altPosition)
		return t ?: ComputedTracker(Tracker.getNextLocalTrackerId(), "Empty tracker", false, false)
	}
}
