package dev.slimevr.vr.trackers

import java.util.*

enum class TrackerPosition(val designation: String, val trackerRole: TrackerRole?) {
	NONE("", TrackerRole.NONE), HMD("HMD", TrackerRole.HMD), CHEST("body:chest", TrackerRole.CHEST), WAIST(
		"body:waist",
		TrackerRole.WAIST
	),
	HIP("body:hip", null), LEFT_LEG("body:left_leg", TrackerRole.LEFT_KNEE), RIGHT_LEG(
		"body:right_leg",
		TrackerRole.RIGHT_KNEE
	),
	LEFT_ANKLE("body:left_ankle", null), RIGHT_ANKLE("body:right_ankle", null), LEFT_FOOT(
		"body:left_foot",
		TrackerRole.LEFT_FOOT
	),
	RIGHT_FOOT("body:right_foot", TrackerRole.RIGHT_FOOT), LEFT_CONTROLLER(
		"body:left_controller",
		TrackerRole.LEFT_CONTROLLER
	),
	RIGHT_CONTROLLER("body:right_controller", TrackerRole.RIGHT_CONTROLLER), LEFT_ELBOW(
		"body:left_elbow",
		TrackerRole.LEFT_ELBOW
	),
	RIGHT_ELBOW("body:right_elbow", TrackerRole.RIGHT_ELBOW);

	companion object {
		val values = values()
		private val byDesignation: MutableMap<String, TrackerPosition> = HashMap()
		private val byRole = EnumMap<TrackerRole, TrackerPosition>(
			TrackerRole::class.java
		)

		fun getByDesignation(designation: String?): TrackerPosition? {
			return if (designation == null) null else byDesignation[designation.lowercase(Locale.getDefault())]
		}

		fun getByRole(role: TrackerRole): TrackerPosition? {
			return byRole[role]
		}

		init {
			for (tbp in values()) {
				byDesignation[tbp.designation.lowercase(Locale.getDefault())] = tbp
				if (tbp.trackerRole != null) {
					val old = byRole[tbp.trackerRole]
					if (old != null) throw AssertionError("Only one tracker position can match tracker role. " + tbp.trackerRole + " is occupied by " + old + " when adding " + tbp)
					byRole[tbp.trackerRole] = tbp
				}
			}
		}
	}
}
