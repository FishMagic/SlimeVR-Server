package dev.slimevr.vr.processor.skeleton

import java.util.*

enum class SkeletonConfigValue(
	val stringVal: String,
	configKey: String,
	label: String,
	defaultValue: Float,
	affectedOffsets: Array<SkeletonNodeOffset>?
) {
	HEAD("Head", "headShift", "Head shift", 0.1f, arrayOf(SkeletonNodeOffset.HEAD)), NECK(
		"Neck",
		"neckLength",
		"Neck length",
		0.1f,
		arrayOf(SkeletonNodeOffset.NECK)
	),
	TORSO("Torso", "torsoLength", "Torso length", 0.56f, arrayOf(SkeletonNodeOffset.WAIST)), CHEST(
		"Chest",
		"chestDistance",
		"Chest distance",
		0.32f,
		arrayOf(SkeletonNodeOffset.CHEST, SkeletonNodeOffset.WAIST)
	),
	WAIST(
		"Waist",
		"waistDistance",
		"Waist distance",
		0.04f,
		arrayOf(SkeletonNodeOffset.WAIST, SkeletonNodeOffset.HIP)
	),
	HIP_OFFSET(
		"Hip offset",
		"hipOffset",
		"Hip offset",
		0.0f,
		arrayOf(SkeletonNodeOffset.HIP_TRACKER)
	),
	HIPS_WIDTH(
		"Hips width",
		"hipsWidth",
		"Hips width",
		0.26f,
		arrayOf(SkeletonNodeOffset.LEFT_HIP, SkeletonNodeOffset.RIGHT_HIP)
	),
	LEGS_LENGTH(
		"Legs length",
		"legsLength",
		"Legs length",
		0.92f,
		arrayOf(SkeletonNodeOffset.KNEE)
	),
	KNEE_HEIGHT(
		"Knee height",
		"kneeHeight",
		"Knee height",
		0.50f,
		arrayOf(SkeletonNodeOffset.KNEE, SkeletonNodeOffset.ANKLE)
	),
	FOOT_LENGTH(
		"Foot length",
		"footLength",
		"Foot length",
		0.05f,
		arrayOf(SkeletonNodeOffset.FOOT)
	),
	FOOT_OFFSET(
		"Foot offset",
		"footOffset",
		"Foot offset",
		-0.05f,
		arrayOf(SkeletonNodeOffset.ANKLE)
	),
	SKELETON_OFFSET(
		"Skeleton offset",
		"skeletonOffset",
		"Skeleton offset",
		0.0f,
		arrayOf(
			SkeletonNodeOffset.CHEST_TRACKER,
			SkeletonNodeOffset.HIP_TRACKER,
			SkeletonNodeOffset.KNEE_TRACKER,
			SkeletonNodeOffset.FOOT_TRACKER
		)
	),
	CONTROLLER_DISTANCE_Z(
		"Controller distance z",
		"controllerDistanceZ",
		"Controller distance z",
		0.15f,
		arrayOf(SkeletonNodeOffset.HAND)
	),
	CONTROLLER_DISTANCE_Y(
		"Controller distance y",
		"controllerDistanceY",
		"Controller distance y",
		0.05f,
		arrayOf(SkeletonNodeOffset.HAND)
	),
	ELBOW_DISTANCE("Elbow distance", "elbowDistance", "Elbow distance", 0.24f, arrayOf(SkeletonNodeOffset.ELBOW));

	val configKey: String
	val label: String
	val defaultValue: Float
	val affectedOffsets: Array<SkeletonNodeOffset>

	init {

		val configPrefix = "body."
		this.configKey = configPrefix + configKey
		this.label = label
		this.defaultValue = defaultValue
		this.affectedOffsets = affectedOffsets ?: arrayOf(SkeletonNodeOffset[0])
	}

	companion object {
		@JvmField
		var values = values()
		private val byStringVal: MutableMap<String, SkeletonConfigValue> = HashMap()
		fun getByStringValue(stringVal: String?): SkeletonConfigValue? {
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
