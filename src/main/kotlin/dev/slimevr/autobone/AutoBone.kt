package dev.slimevr.autobone

import com.jme3.math.FastMath
import dev.slimevr.VRServer
import dev.slimevr.poserecorder.*
import dev.slimevr.vr.processor.HumanPoseProcessor
import dev.slimevr.vr.processor.skeleton.HumanSkeleton
import dev.slimevr.vr.processor.skeleton.SkeletonConfig
import dev.slimevr.vr.processor.skeleton.SkeletonConfigValue
import dev.slimevr.vr.trackers.TrackerPosition
import dev.slimevr.vr.trackers.TrackerRole
import dev.slimevr.vr.trackers.TrackerUtils
import io.eiren.util.collections.FastList
import io.eiren.util.logging.LogManager
import java.util.*
import java.util.function.Consumer

class AutoBone(server: VRServer) {
	inner class Epoch(private val epoch: Int, private val epochError: Float) {
		override fun toString(): String {
			return "Epoch: $epoch, Epoch Error: $epochError"
		}
	}

	var cursorIncrement = 1
	var minDataDistance = 2
	var maxDataDistance = 32
	var numEpochs = 5
	var initialAdjustRate = 2.5f
	var adjustRateDecay = 1.01f
	var slideErrorFactor = 1.0f
	var offsetSlideErrorFactor = 0.0f
	var offsetErrorFactor = 0.0f
	var proportionErrorFactor = 0.2f
	var heightErrorFactor = 0.1f
	var positionErrorFactor = 0.0f
	var positionOffsetErrorFactor = 0.0f

	// TODO Needs much more work, probably going to rethink how the errors work to avoid this barely functional workaround @ButterscotchVanilla
	// For scaling distances, since smaller sizes will cause smaller distances
	//private float totalLengthBase = 2f;
	// Human average is probably 1.1235 (SD 0.07)
	var legBodyRatio = 1.1235f

	// SD of 0.07, capture 68% within range
	var legBodyRatioRange = 0.07f

	// kneeLegRatio seems to be around 0.54 to 0.6 after asking a few people in the SlimeVR discord.
	var kneeLegRatio = 0.55f

	// kneeLegRatio seems to be around 0.55 to 0.64 after asking a few people in the SlimeVR discord. TODO : Chest should be a bit shorter (0.54?) if user has an additional hip tracker.
	var chestTorsoRatio = 0.57f

	// TODO hip tracker stuff... Hip tracker should be around 3 to 5 centimeters.
	protected val server: VRServer

	// This is filled by reloadConfigValues()
	val configs = EnumMap<SkeletonConfigValue, Float>(
		SkeletonConfigValue::class.java
	)
	val staticConfigs = EnumMap<SkeletonConfigValue, Float>(
		SkeletonConfigValue::class.java
	)
	val heightConfigs = FastList(
		arrayOf(
			SkeletonConfigValue.NECK, SkeletonConfigValue.TORSO, SkeletonConfigValue.LEGS_LENGTH
		)
	)
	val lengthConfigs = FastList(
		arrayOf(
			SkeletonConfigValue.HEAD,
			SkeletonConfigValue.NECK,
			SkeletonConfigValue.TORSO,
			SkeletonConfigValue.HIPS_WIDTH,
			SkeletonConfigValue.LEGS_LENGTH
		)
	)

	init {
		this.server = server
		reloadConfigValues()
	}

	private fun readFromConfig(configValue: SkeletonConfigValue): Float {
		return server.config.getFloat(configValue.configKey, configValue.defaultValue)
	}

	@JvmOverloads
	fun reloadConfigValues(trackers: List<PoseFrameTracker?>? = null) {
		// Load torso configs
		staticConfigs[SkeletonConfigValue.HEAD] = readFromConfig(SkeletonConfigValue.HEAD)
		staticConfigs[SkeletonConfigValue.NECK] = readFromConfig(SkeletonConfigValue.NECK)
		configs[SkeletonConfigValue.TORSO] = readFromConfig(SkeletonConfigValue.TORSO)
		if (server.config.getBoolean(
				"autobone.forceChestTracker",
				false
			) || trackers != null && TrackerUtils.findTrackerForBodyPosition(trackers, TrackerPosition.CHEST) != null
		) {
			// If force enabled or has a chest tracker
			staticConfigs.remove(SkeletonConfigValue.CHEST)
			configs[SkeletonConfigValue.CHEST] = readFromConfig(SkeletonConfigValue.CHEST)
		} else {
			// Otherwise, make sure it's not used
			configs.remove(SkeletonConfigValue.CHEST)
			staticConfigs[SkeletonConfigValue.CHEST] = readFromConfig(SkeletonConfigValue.CHEST)
		}
		if (server.config.getBoolean(
				"autobone.forceHipTracker",
				false
			) || trackers != null && TrackerUtils.findTrackerForBodyPosition(
				trackers,
				TrackerPosition.HIP
			) != null && TrackerUtils.findTrackerForBodyPosition(trackers, TrackerPosition.WAIST) != null
		) {
			// If force enabled or has a hip tracker and waist tracker
			staticConfigs.remove(SkeletonConfigValue.WAIST)
			configs[SkeletonConfigValue.WAIST] = readFromConfig(SkeletonConfigValue.WAIST)
		} else {
			// Otherwise, make sure it's not used
			configs.remove(SkeletonConfigValue.WAIST)
			staticConfigs[SkeletonConfigValue.WAIST] = readFromConfig(SkeletonConfigValue.WAIST)
		}

		// Load leg configs
		staticConfigs[SkeletonConfigValue.HIPS_WIDTH] = readFromConfig(SkeletonConfigValue.HIPS_WIDTH)
		configs[SkeletonConfigValue.LEGS_LENGTH] = readFromConfig(SkeletonConfigValue.LEGS_LENGTH)
		configs[SkeletonConfigValue.KNEE_HEIGHT] = readFromConfig(SkeletonConfigValue.KNEE_HEIGHT)

		// Keep "feet" at ankles
		staticConfigs[SkeletonConfigValue.FOOT_LENGTH] = 0f
		staticConfigs[SkeletonConfigValue.FOOT_OFFSET] = 0f
		staticConfigs[SkeletonConfigValue.SKELETON_OFFSET] = 0f
	}

	/**
	 * A simple utility method to get the [HumanSkeleton] from the [VRServer]
	 * @return The [HumanSkeleton] associated with the [VRServer], or null if there is none available
	 * @see {@link VRServer}, {@link HumanSkeleton}
	 */
	private val skeleton: HumanSkeleton?
		get() {
			val humanPoseProcessor: HumanPoseProcessor? = if (server != null) server.humanPoseProcessor else null
			return humanPoseProcessor?.skeleton
		}

	fun applyConfig() {
		if (!applyConfigToSkeleton(skeleton)) {
			// Unable to apply to skeleton, save directly
			saveConfigs()
		}
	}

	fun applyConfigToSkeleton(skeleton: HumanSkeleton?): Boolean {
		if (skeleton == null) {
			return false
		}
		val skeletonConfig = skeleton.skeletonConfig
		skeletonConfig!!.setConfigs(configs, null)
		skeletonConfig.saveToConfig(server.config)
		server.saveConfig()
		LogManager.log.info("[AutoBone] Configured skeleton bone lengths")
		return true
	}

	private fun setConfig(config: SkeletonConfigValue) {
		val value = configs[config]
		if (value != null) {
			server.config.setProperty(config.configKey, value)
		}
	}

	// This doesn't require a skeleton, therefore can be used if skeleton is null
	fun saveConfigs() {
		for (config in SkeletonConfigValue.values) {
			setConfig(config)
		}
		server.saveConfig()
	}

	fun getConfig(config: SkeletonConfigValue): Float {
		val configVal = configs[config]
		return configVal ?: staticConfigs[config]!!
	}

	fun getConfig(
		config: SkeletonConfigValue,
		configs: Map<SkeletonConfigValue, Float>?,
		configsAlt: Map<SkeletonConfigValue, Float>?
	): Float? {
		if (configs == null) {
			throw NullPointerException("Argument \"configs\" must not be null")
		}
		val configVal = configs[config]
		return if (configVal != null || configsAlt == null) configVal else configsAlt[config]
	}

	fun sumSelectConfigs(
		selection: List<SkeletonConfigValue>,
		configs: Map<SkeletonConfigValue, Float>?,
		configsAlt: Map<SkeletonConfigValue, Float>?
	): Float {
		var sum = 0f
		for (config in selection) {
			val length = getConfig(config, configs, configsAlt)
			if (length != null) {
				sum += length
			}
		}
		return sum
	}

	fun sumSelectConfigs(selection: List<SkeletonConfigValue>, skeletonConfig: SkeletonConfig?): Float {
		var sum = 0f
		for (config in selection) {
			sum += skeletonConfig!!.getConfig(config)
		}
		return sum
	}

	fun getLengthSum(configs: Map<SkeletonConfigValue, Float>): Float {
		return getLengthSum(configs, null)
	}

	fun getLengthSum(configs: Map<SkeletonConfigValue, Float>, configsAlt: Map<SkeletonConfigValue, Float>?): Float {
		var length = 0f
		if (configsAlt != null) {
			for ((key, value) in configsAlt) {
				// If there isn't a duplicate config
				if (!configs.containsKey(key)) {
					length += value
				}
			}
		}
		for (boneLength in configs.values) {
			length += boneLength
		}
		return length
	}

	fun getMaxHmdHeight(frames: PoseFrames): Float {
		var maxHeight = 0f
		for (frame in frames) {
			val hmd = TrackerUtils.findTrackerForBodyPosition(frame, TrackerPosition.HMD)
			if (hmd != null && hmd.hasData(TrackerFrameData.POSITION) && hmd.position.y > maxHeight) {
				maxHeight = hmd.position.y
			}
		}
		return maxHeight
	}

	fun processFrames(frames: PoseFrames, epochCallback: Consumer<Epoch?>?) {
		processFrames(frames, -1f, epochCallback)
	}

	@JvmOverloads
	fun processFrames(frames: PoseFrames, targetHeight: Float = -1f) {
		processFrames(frames, true, targetHeight)
	}

	fun processFrames(frames: PoseFrames, targetHeight: Float, epochCallback: Consumer<Epoch?>?) {
		processFrames(frames, true, targetHeight, epochCallback)
	}

	@JvmOverloads
	fun processFrames(
		frames: PoseFrames,
		calcInitError: Boolean,
		targetHeight: Float,
		epochCallback: Consumer<Epoch?>? = null
	): Float {
		var targetHeight = targetHeight
		val frameCount = frames.maxFrameCount
		val trackers = frames.trackers
		reloadConfigValues(trackers) // Reload configs and detect chest tracker from the first frame
		val skeleton1 = PoseFrameSkeleton(trackers, null, configs, staticConfigs)
		val skeleton2 = PoseFrameSkeleton(trackers, null, configs, staticConfigs)

		// If target height isn't specified, auto-detect
		if (targetHeight < 0f) {
			// Get the current skeleton from the server
			val skeleton = skeleton
			if (skeleton != null) {
				// If there is a skeleton available, calculate the target height from its configs
				targetHeight = sumSelectConfigs(heightConfigs, skeleton.skeletonConfig)
				LogManager.log.warning("[AutoBone] Target height loaded from skeleton (Make sure you reset before running!): $targetHeight")
			} else {
				// Otherwise if there is no skeleton available, attempt to get the max HMD height from the recording
				val hmdHeight = getMaxHmdHeight(frames)
				if (hmdHeight <= 0.50f) {
					LogManager.log.warning("[AutoBone] Max headset height detected (Value seems too low, did you not stand up straight while measuring?): $hmdHeight")
				} else {
					LogManager.log.info("[AutoBone] Max headset height detected: $hmdHeight")
				}

				// Estimate target height from HMD height
				targetHeight = hmdHeight
			}
		}

		// Epoch loop, each epoch is one full iteration over the full dataset
		for (epoch in (if (calcInitError) -1 else 0) until numEpochs) {
			var sumError = 0f
			var errorCount = 0
			val adjustRate = if (epoch >= 0) initialAdjustRate / FastMath.pow(adjustRateDecay, epoch.toFloat()) else 0f

			// Iterate over the frames using a cursor and an offset for comparing frames a certain number of frames apart
			var cursorOffset = minDataDistance
			while (cursorOffset <= maxDataDistance && cursorOffset < frameCount) {
				var frameCursor = 0
				while (frameCursor < frameCount - cursorOffset) {
					val frameCursor2 = frameCursor + cursorOffset
					skeleton1.skeletonConfig.setConfigs(configs, null)
					skeleton2.skeletonConfig.setConfigs(configs, null)
					skeleton1.cursor = frameCursor
					skeleton1.updatePose()
					skeleton2.cursor = frameCursor2
					skeleton2.updatePose()
					val totalLength = getLengthSum(configs)
					val curHeight = sumSelectConfigs(heightConfigs, configs, staticConfigs)
					//float scaleLength = sumSelectConfigs(lengthConfigs, configs, staticConfigs);
					val errorDeriv =
						getErrorDeriv(frames, frameCursor, frameCursor2, skeleton1, skeleton2, targetHeight - curHeight, 1f)
					val error = errorFunc(errorDeriv)

					// In case of fire
					if (java.lang.Float.isNaN(error) || java.lang.Float.isInfinite(error)) {
						// Extinguish
						LogManager.log.warning("[AutoBone] Error value is invalid, resetting variables to recover")
						reloadConfigValues(trackers)

						// Reset error sum values
						sumError = 0f
						errorCount = 0
						frameCursor += cursorIncrement

						// Continue on new data
						continue
					}

					// Store the error count for logging purposes
					sumError += errorDeriv
					errorCount++
					val adjustVal = error * adjustRate

					// If there is no adjustment whatsoever, skip this
					if (adjustVal == 0f) {
						frameCursor += cursorIncrement
						continue
					}
					for (entry in configs.entries) {
						// Skip adjustment if the epoch is before starting (for logging only)
						if (epoch < 0) {
							break
						}
						val originalLength = entry.value

						// Try positive and negative adjustments
						val isHeightVar = heightConfigs.contains(entry.key)
						//boolean isLengthVar = lengthConfigs.contains(entry.getKey());
						var minError = errorDeriv
						var finalNewLength = -1f
						for (i in 0..1) {
							// Scale by the ratio for smooth adjustment and more stable results
							val curAdjustVal = (if (i == 0) adjustVal else -adjustVal) * originalLength / totalLength
							val newLength = originalLength + curAdjustVal

							// No small or negative numbers!!! Bad algorithm!
							if (newLength < 0.01f) {
								continue
							}
							updateSkeletonBoneLength(skeleton1, skeleton2, entry.key, newLength)
							val newHeight = if (isHeightVar) curHeight + curAdjustVal else curHeight
							//float newScaleLength = isLengthVar ? scaleLength + curAdjustVal : scaleLength;
							val newErrorDeriv =
								getErrorDeriv(frames, frameCursor, frameCursor2, skeleton1, skeleton2, targetHeight - newHeight, 1f)
							if (newErrorDeriv < minError) {
								minError = newErrorDeriv
								finalNewLength = newLength
							}
						}
						if (finalNewLength > 0f) {
							entry.setValue(finalNewLength)
						}

						// Reset the length to minimize bias in other variables, it's applied later
						updateSkeletonBoneLength(skeleton1, skeleton2, entry.key, originalLength)
					}
					frameCursor += cursorIncrement
				}
				cursorOffset++
			}

			// Calculate average error over the epoch
			val avgError = if (errorCount > 0) sumError / errorCount else -1f
			LogManager.log.info("[AutoBone] Epoch " + (epoch + 1) + " average error: " + avgError)
			epochCallback?.accept(Epoch(epoch + 1, avgError))
		}
		val finalHeight = sumSelectConfigs(heightConfigs, configs, staticConfigs)
		LogManager.log.info("[AutoBone] Target height: $targetHeight New height: $finalHeight")
		return FastMath.abs(finalHeight - targetHeight)
	}

	// The change in position of the ankle over time
	protected fun getSlideErrorDeriv(skeleton1: PoseFrameSkeleton, skeleton2: PoseFrameSkeleton): Float {
		val slideLeft = skeleton1.getComputedTracker(TrackerRole.LEFT_FOOT)!!.position.distance(
			skeleton2.getComputedTracker(TrackerRole.LEFT_FOOT)!!.position
		)
		val slideRight = skeleton1.getComputedTracker(TrackerRole.RIGHT_FOOT)!!.position.distance(
			skeleton2.getComputedTracker(TrackerRole.RIGHT_FOOT)!!.position
		)

		// Divide by 4 to halve and average, it's halved because you want to approach a midpoint, not the other point
		return (slideLeft + slideRight) / 4f
	}

	// The change in distance between both of the ankles over time
	protected fun getOffsetSlideErrorDeriv(skeleton1: PoseFrameSkeleton, skeleton2: PoseFrameSkeleton): Float {
		val leftFoot1 = skeleton1.getComputedTracker(TrackerRole.LEFT_FOOT)!!.position
		val rightFoot1 = skeleton1.getComputedTracker(TrackerRole.RIGHT_FOOT)!!.position
		val leftFoot2 = skeleton2.getComputedTracker(TrackerRole.LEFT_FOOT)!!.position
		val rightFoot2 = skeleton2.getComputedTracker(TrackerRole.RIGHT_FOOT)!!.position
		val slideDist1 = leftFoot1.distance(rightFoot1)
		val slideDist2 = leftFoot2.distance(rightFoot2)
		val slideDist3 = leftFoot1.distance(rightFoot2)
		val slideDist4 = leftFoot2.distance(rightFoot1)
		val dist1 = FastMath.abs(slideDist1 - slideDist2)
		val dist2 = FastMath.abs(slideDist3 - slideDist4)
		val dist3 = FastMath.abs(slideDist1 - slideDist3)
		val dist4 = FastMath.abs(slideDist1 - slideDist4)
		val dist5 = FastMath.abs(slideDist2 - slideDist3)
		val dist6 = FastMath.abs(slideDist2 - slideDist4)

		// Divide by 12 to halve and average, it's halved because you want to approach a midpoint, not the other point
		return (dist1 + dist2 + dist3 + dist4 + dist5 + dist6) / 12f
	}

	// The offset between both feet at one instant and over time
	protected fun getOffsetErrorDeriv(skeleton1: PoseFrameSkeleton, skeleton2: PoseFrameSkeleton): Float {
		val leftFoot1 = skeleton1.getComputedTracker(TrackerRole.LEFT_FOOT)!!.position.getY()
		val rightFoot1 = skeleton1.getComputedTracker(TrackerRole.RIGHT_FOOT)!!.position.getY()
		val leftFoot2 = skeleton2.getComputedTracker(TrackerRole.LEFT_FOOT)!!.position.getY()
		val rightFoot2 = skeleton2.getComputedTracker(TrackerRole.RIGHT_FOOT)!!.position.getY()
		val dist1 = FastMath.abs(leftFoot1 - rightFoot1)
		val dist2 = FastMath.abs(leftFoot2 - rightFoot2)
		val dist3 = FastMath.abs(leftFoot1 - rightFoot2)
		val dist4 = FastMath.abs(leftFoot2 - rightFoot1)
		val dist5 = FastMath.abs(leftFoot1 - leftFoot2)
		val dist6 = FastMath.abs(rightFoot1 - rightFoot2)

		// Divide by 12 to halve and average, it's halved because you want to approach a midpoint, not the other point
		return (dist1 + dist2 + dist3 + dist4 + dist5 + dist6) / 12f
	}

	// The distance from average human proportions
	protected fun getProportionErrorDeriv(skeleton: SkeletonConfig): Float {
		val neckLength = skeleton.getConfig(SkeletonConfigValue.NECK)
		val chestLength = skeleton.getConfig(SkeletonConfigValue.CHEST)
		val torsoLength = skeleton.getConfig(SkeletonConfigValue.TORSO)
		val legsLength = skeleton.getConfig(SkeletonConfigValue.LEGS_LENGTH)
		val kneeHeight = skeleton.getConfig(SkeletonConfigValue.KNEE_HEIGHT)
		val chestTorso = FastMath.abs(chestLength / torsoLength - chestTorsoRatio)
		var legBody = FastMath.abs(legsLength / (torsoLength + neckLength) - legBodyRatio)
		val kneeLeg = FastMath.abs(kneeHeight / legsLength - kneeLegRatio)
		if (legBody <= legBodyRatioRange) {
			legBody = 0f
		} else {
			legBody -= legBodyRatioRange
		}
		return (chestTorso + legBody + kneeLeg) / 3f
	}

	// The distance of any points to the corresponding absolute position
	protected fun getPositionErrorDeriv(frames: PoseFrames, cursor: Int, skeleton: PoseFrameSkeleton): Float {
		var offset = 0f
		var offsetCount = 0
		val trackers = frames.trackers
		for (i in trackers.indices) {
			val tracker = trackers[i]
			val trackerFrame = tracker.safeGetFrame(cursor)
			if (trackerFrame == null || !trackerFrame.hasData(TrackerFrameData.POSITION)) {
				continue
			}
			val nodePos = skeleton.getComputedTracker(trackerFrame.designation.trackerRole)!!.position
			if (nodePos != null) {
				offset += FastMath.abs(nodePos.distance(trackerFrame.position))
				offsetCount++
			}
		}
		return if (offsetCount > 0) offset / offsetCount else 0f
	}

	// The difference between offset of absolute position and the corresponding point over time
	protected fun getPositionOffsetErrorDeriv(
		frames: PoseFrames,
		cursor1: Int,
		cursor2: Int,
		skeleton1: PoseFrameSkeleton,
		skeleton2: PoseFrameSkeleton
	): Float {
		var offset = 0f
		var offsetCount = 0
		val trackers = frames.trackers
		for (i in trackers.indices) {
			val tracker = trackers[i]
			val trackerFrame1 = tracker.safeGetFrame(cursor1)
			if (trackerFrame1 == null || !trackerFrame1.hasData(TrackerFrameData.POSITION)) {
				continue
			}
			val trackerFrame2 = tracker.safeGetFrame(cursor2)
			if (trackerFrame2 == null || !trackerFrame2.hasData(TrackerFrameData.POSITION)) {
				continue
			}
			val nodePos1 = skeleton1.getComputedTracker(trackerFrame1.designation.trackerRole)!!.position ?: continue
			val nodePos2 = skeleton2.getComputedTracker(trackerFrame2.designation.trackerRole)!!.position ?: continue
			val dist1 = FastMath.abs(nodePos1.distance(trackerFrame1.position))
			val dist2 = FastMath.abs(nodePos2.distance(trackerFrame2.position))
			offset += FastMath.abs(dist2 - dist1)
			offsetCount++
		}
		return if (offsetCount > 0) offset / offsetCount else 0f
	}

	protected fun getErrorDeriv(
		frames: PoseFrames,
		cursor1: Int,
		cursor2: Int,
		skeleton1: PoseFrameSkeleton,
		skeleton2: PoseFrameSkeleton,
		heightChange: Float,
		distScale: Float
	): Float {
		var totalError = 0f
		var sumWeight = 0f
		if (slideErrorFactor > 0f) {
			// This is the main error function, this calculates the distance between the foot positions on both frames
			totalError += getSlideErrorDeriv(skeleton1, skeleton2) * distScale * slideErrorFactor
			sumWeight += slideErrorFactor
		}
		if (offsetSlideErrorFactor > 0f) {
			// This error function compares the distance between the feet on each frame and returns the offset between them
			totalError += getOffsetSlideErrorDeriv(skeleton1, skeleton2) * distScale * offsetSlideErrorFactor
			sumWeight += offsetSlideErrorFactor
		}
		if (offsetErrorFactor > 0f) {
			// This error function compares the height of each foot in each frame
			totalError += getOffsetErrorDeriv(skeleton1, skeleton2) * distScale * offsetErrorFactor
			sumWeight += offsetErrorFactor
		}
		if (proportionErrorFactor > 0f) {
			// This error function compares the current values to general expected proportions to keep measurements in line
			// Either skeleton will work fine, skeleton1 is used as a default
			totalError += getProportionErrorDeriv(skeleton1.skeletonConfig) * proportionErrorFactor
			sumWeight += proportionErrorFactor
		}
		if (heightErrorFactor > 0f) {
			// This error function compares the height change to the actual measured height of the headset
			totalError += FastMath.abs(heightChange) * heightErrorFactor
			sumWeight += heightErrorFactor
		}
		if (positionErrorFactor > 0f) {
			// This error function compares the position of an assigned tracker with the position on the skeleton
			totalError += (getPositionErrorDeriv(frames, cursor1, skeleton1) + getPositionErrorDeriv(
				frames,
				cursor2,
				skeleton2
			) / 2f) * distScale * positionErrorFactor
			sumWeight += positionErrorFactor
		}
		if (positionOffsetErrorFactor > 0f) {
			// This error function compares the offset of the position of an assigned tracker with the position on the skeleton
			totalError += getPositionOffsetErrorDeriv(
				frames,
				cursor1,
				cursor2,
				skeleton1,
				skeleton2
			) * distScale * positionOffsetErrorFactor
			sumWeight += positionOffsetErrorFactor
		}
		return if (sumWeight > 0f) totalError / sumWeight else 0f
	}

	protected fun updateSkeletonBoneLength(
		skeleton1: PoseFrameSkeleton,
		skeleton2: PoseFrameSkeleton,
		config: SkeletonConfigValue?,
		newLength: Float
	) {
		skeleton1.skeletonConfig.setConfig(config!!, newLength)
		skeleton1.updatePoseAffectedByConfig(config)
		skeleton2.skeletonConfig.setConfig(config, newLength)
		skeleton2.updatePoseAffectedByConfig(config)
	}

	companion object {
		// Mean square error function
		protected fun errorFunc(errorDeriv: Float): Float {
			return 0.5f * (errorDeriv * errorDeriv)
		}
	}
}
