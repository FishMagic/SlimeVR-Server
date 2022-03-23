package dev.slimevr.vr.processor.skeleton

import java.util.*


enum class SkeletonConfigToggle(val stringVal: String, configKey: String, defaultValue: Boolean) {

	EXTENDED_PELVIS_MODEL("Extended pelvis model", "extendedPelvis", true),
	EXTENDED_KNEE_MODEL("Extended knee model", "extendedKnee", false);

	val configKey: String
	val defaultValue: Boolean

	init {
		val configPrefix = "body.model."
		this.configKey = configPrefix + configKey
		this.defaultValue = defaultValue
	}

	companion object {
		val values = values()
		private val byStringVal: MutableMap<String, SkeletonConfigToggle> = HashMap()
		fun getByStringValue(stringVal: String?): SkeletonConfigToggle? {
			return if (stringVal == null) null else byStringVal[stringVal.lowercase(Locale.getDefault())]
		}

		init {
			for (configVal in values()) {
				byStringVal[configVal.stringVal.lowercase(Locale.getDefault())] =
					configVal
			}
		}
	}
}
