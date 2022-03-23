package dev.slimevr.vr.processor.skeleton

import com.jme3.math.Vector3f
import io.eiren.util.logging.LogManager
import io.eiren.yaml.YamlFile
import java.util.*

class SkeletonConfig {
	protected val configs = EnumMap<SkeletonConfigValue, Float>(
		SkeletonConfigValue::class.java
	)
	protected val toggles = EnumMap<SkeletonConfigToggle, Boolean>(
		SkeletonConfigToggle::class.java
	)
	protected val nodeOffsets = EnumMap<SkeletonNodeOffset, Vector3f>(
		SkeletonNodeOffset::class.java
	)
	protected val autoUpdateOffsets: Boolean
	protected val callback: SkeletonConfigCallback?

	constructor(autoUpdateOffsets: Boolean) {
		this.autoUpdateOffsets = autoUpdateOffsets
		callback = null
		callCallbackOnAll(true)
		if (autoUpdateOffsets) {
			computeAllNodeOffsets()
		}
	}

	constructor(autoUpdateOffsets: Boolean, callback: SkeletonConfigCallback?) {
		this.autoUpdateOffsets = autoUpdateOffsets
		this.callback = callback
		callCallbackOnAll(true)
		if (autoUpdateOffsets) {
			computeAllNodeOffsets()
		}
	}

	@JvmOverloads
	constructor(
		configs: Map<SkeletonConfigValue?, Float>?,
		toggles: Map<SkeletonConfigToggle, Boolean>?,
		autoUpdateOffsets: Boolean,
		callback: SkeletonConfigCallback? = null
	) {
		this.autoUpdateOffsets = autoUpdateOffsets
		this.callback = callback
		setConfigs(configs, toggles)
		callCallbackOnAll(true)
	}

	@JvmOverloads
	constructor(skeletonConfig: SkeletonConfig, autoUpdateOffsets: Boolean, callback: SkeletonConfigCallback? = null) {
		this.autoUpdateOffsets = autoUpdateOffsets
		this.callback = callback
		setConfigs(skeletonConfig)
		callCallbackOnAll(true)
	}

	private fun callCallbackOnAll(defaultOnly: Boolean) {
		if (callback == null) {
			return
		}
		for (config in SkeletonConfigValue.values) {
			try {
				val `val` = configs[config]
				if (!defaultOnly || `val` == null) {
					callback.updateConfigState(config, `val` ?: config.defaultValue)
				}
			} catch (e: Exception) {
				LogManager.log.severe("[SkeletonConfig] Exception while calling callback", e)
			}
		}
		for (config in SkeletonConfigToggle.values) {
			try {
				val `val` = toggles[config]
				if (!defaultOnly || `val` == null) {
					callback.updateToggleState(config, `val` ?: config.defaultValue)
				}
			} catch (e: Exception) {
				LogManager.log.severe("[SkeletonConfig] Exception while calling callback", e)
			}
		}
	}

	fun setConfig(config: SkeletonConfigValue?, newValue: Float?, computeOffsets: Boolean): Float? {
		val origVal = if (newValue != null) configs.put(config, newValue) else configs.remove(config)

		// Re-compute the affected offsets
		if (computeOffsets && autoUpdateOffsets && config?.affectedOffsets != null) {
			for (offset in config.affectedOffsets) {
				computeNodeOffset(offset)
			}
		}
		if (callback != null) {
			try {
				if (config != null) {
					callback.updateConfigState(config, newValue ?: config.defaultValue)
				}
			} catch (e: Exception) {
				LogManager.log.severe("[SkeletonConfig] Exception while calling callback", e)
			}
		}
		return origVal
	}

	fun setConfig(config: SkeletonConfigValue?, newValue: Float?): Float? {
		return setConfig(config, newValue, true)
	}

	fun setConfig(config: String, newValue: Float?): Float? {
		return setConfig(SkeletonConfigValue.getByStringValue(config), newValue)
	}

	fun getConfig(config: SkeletonConfigValue?): Float {
		if (config == null) {
			return 0f
		}

		// IMPORTANT!! This null check is necessary, getOrDefault seems to randomly decide to return null at times, so this is a secondary check
		val `val` = configs.getOrDefault(config, config.defaultValue)
		return `val` ?: config.defaultValue
	}

	fun getConfig(config: String?): Float {
		return if (config == null) {
			0f
		} else getConfig(SkeletonConfigValue.getByStringValue(config))
	}

	fun setToggle(config: SkeletonConfigToggle?, newValue: Boolean?): Boolean? {
		val origVal = if (newValue != null) toggles.put(config, newValue) else toggles.remove(config)
		if (callback != null) {
			try {
				if (config != null) {
					callback.updateToggleState(config, newValue ?: config.defaultValue)
				}
			} catch (e: Exception) {
				LogManager.log.severe("[SkeletonConfig] Exception while calling callback", e)
			}
		}
		return origVal
	}

	fun setToggle(config: String, newValue: Boolean?): Boolean? {
		return setToggle(SkeletonConfigToggle.getByStringValue(config), newValue)
	}

	fun getToggle(config: SkeletonConfigToggle?): Boolean {
		if (config == null) {
			return false
		}

		// IMPORTANT!! This null check is necessary, getOrDefault seems to randomly decide to return null at times, so this is a secondary check
		val `val` = toggles.getOrDefault(config, config.defaultValue)
		return `val` ?: config.defaultValue
	}

	fun getToggle(config: String?): Boolean {
		return if (config == null) {
			false
		} else getToggle(SkeletonConfigToggle.getByStringValue(config))
	}

	protected fun setNodeOffset(nodeOffset: SkeletonNodeOffset, x: Float, y: Float, z: Float) {
		var offset = nodeOffsets[nodeOffset]
		if (offset == null) {
			offset = Vector3f(x, y, z)
			nodeOffsets[nodeOffset] = offset
		} else {
			offset[x, y] = z
		}
		if (callback != null) {
			try {
				callback.updateNodeOffset(nodeOffset, offset)
			} catch (e: Exception) {
				LogManager.log.severe("[SkeletonConfig] Exception while calling callback", e)
			}
		}
	}

	protected fun setNodeOffset(nodeOffset: SkeletonNodeOffset, offset: Vector3f?) {
		if (offset == null) {
			setNodeOffset(nodeOffset, 0f, 0f, 0f)
			return
		}
		setNodeOffset(nodeOffset, offset.x, offset.y, offset.z)
	}

	fun getNodeOffset(nodeOffset: SkeletonNodeOffset): Vector3f {
		return nodeOffsets.getOrDefault(nodeOffset, Vector3f.ZERO)
	}

	fun computeNodeOffset(nodeOffset: SkeletonNodeOffset?) {
		when (nodeOffset) {
			SkeletonNodeOffset.HEAD -> setNodeOffset(nodeOffset, 0f, 0f, getConfig(SkeletonConfigValue.HEAD))
			SkeletonNodeOffset.NECK -> setNodeOffset(nodeOffset, 0f, -getConfig(SkeletonConfigValue.NECK), 0f)
			SkeletonNodeOffset.CHEST -> setNodeOffset(nodeOffset, 0f, -getConfig(SkeletonConfigValue.CHEST), 0f)
			SkeletonNodeOffset.CHEST_TRACKER -> setNodeOffset(
				nodeOffset,
				0f,
				0f,
				-getConfig(SkeletonConfigValue.SKELETON_OFFSET)
			)
			SkeletonNodeOffset.WAIST -> setNodeOffset(
				nodeOffset,
				0f,
				getConfig(SkeletonConfigValue.CHEST) - getConfig(SkeletonConfigValue.TORSO) + getConfig(SkeletonConfigValue.WAIST),
				0f
			)
			SkeletonNodeOffset.HIP -> setNodeOffset(nodeOffset, 0f, -getConfig(SkeletonConfigValue.WAIST), 0f)
			SkeletonNodeOffset.HIP_TRACKER -> setNodeOffset(
				nodeOffset,
				0f,
				getConfig(SkeletonConfigValue.HIP_OFFSET),
				-getConfig(SkeletonConfigValue.SKELETON_OFFSET)
			)
			SkeletonNodeOffset.LEFT_HIP -> setNodeOffset(nodeOffset, -getConfig(SkeletonConfigValue.HIPS_WIDTH) / 2f, 0f, 0f)
			SkeletonNodeOffset.RIGHT_HIP -> setNodeOffset(nodeOffset, getConfig(SkeletonConfigValue.HIPS_WIDTH) / 2f, 0f, 0f)
			SkeletonNodeOffset.KNEE -> setNodeOffset(
				nodeOffset,
				0f,
				-(getConfig(SkeletonConfigValue.LEGS_LENGTH) - getConfig(SkeletonConfigValue.KNEE_HEIGHT)),
				0f
			)
			SkeletonNodeOffset.KNEE_TRACKER -> setNodeOffset(
				nodeOffset,
				0f,
				0f,
				-getConfig(SkeletonConfigValue.SKELETON_OFFSET)
			)
			SkeletonNodeOffset.ANKLE -> setNodeOffset(
				nodeOffset,
				0f,
				-getConfig(SkeletonConfigValue.KNEE_HEIGHT),
				-getConfig(SkeletonConfigValue.FOOT_OFFSET)
			)
			SkeletonNodeOffset.FOOT -> setNodeOffset(nodeOffset, 0f, 0f, -getConfig(SkeletonConfigValue.FOOT_LENGTH))
			SkeletonNodeOffset.FOOT_TRACKER -> setNodeOffset(
				nodeOffset,
				0f,
				0f,
				-getConfig(SkeletonConfigValue.SKELETON_OFFSET)
			)
			SkeletonNodeOffset.HAND -> setNodeOffset(
				nodeOffset,
				0f,
				getConfig(SkeletonConfigValue.CONTROLLER_DISTANCE_Y),
				getConfig(SkeletonConfigValue.CONTROLLER_DISTANCE_Z)
			)
			SkeletonNodeOffset.ELBOW -> setNodeOffset(nodeOffset, 0f, getConfig(SkeletonConfigValue.ELBOW_DISTANCE), 0f)
			SkeletonNodeOffset.ELBOW_TRACKER -> setNodeOffset(nodeOffset, 0f, 0f, 0f)
		}
	}

	fun computeAllNodeOffsets() {
		for (offset in SkeletonNodeOffset.values) {
			computeNodeOffset(offset)
		}
	}

	fun setConfigs(configs: Map<SkeletonConfigValue?, Float?>?, toggles: Map<SkeletonConfigToggle, Boolean>?) {
		configs?.forEach { (key: SkeletonConfigValue?, value: Float?) ->
			// Do not recalculate the offsets, these are done in bulk at the end
			setConfig(key, value, false)
		}
		toggles?.forEach { (config: SkeletonConfigToggle, newValue: Boolean?) ->
			this.setToggle(
				config,
				newValue
			)
		}
		if (autoUpdateOffsets) {
			computeAllNodeOffsets()
		}
	}

	fun setStringConfigs(configs: Map<String?, Float?>?, toggles: Map<String?, Boolean?>?) {
		configs?.forEach { (key: String?, value: Float?) ->
			// Do not recalculate the offsets, these are done in bulk at the end
			setConfig(SkeletonConfigValue.getByStringValue(key), value, false)
		}
		toggles?.forEach { (key: String?, value: Boolean?) ->
			setToggle(
				SkeletonConfigToggle.getByStringValue(key),
				value
			)
		}
		if (autoUpdateOffsets) {
			computeAllNodeOffsets()
		}
	}

	fun setConfigs(skeletonConfig: SkeletonConfig) {
		setConfigs(skeletonConfig.configs, skeletonConfig.toggles)
	}

	//#endregion
	fun loadFromConfig(config: YamlFile) {
		for (configValue in SkeletonConfigValue.values) {
			val `val` = config.getProperty(configValue.configKey) as Float?
			if (`val` != null) {
				// Do not recalculate the offsets, these are done in bulk at the end
				setConfig(configValue, `val`, false)
			}
		}
		for (configValue in SkeletonConfigToggle.values) {
			val `val` = config.getProperty(configValue.configKey) as Boolean?
			`val`?.let { setToggle(configValue, it) }
		}
		if (autoUpdateOffsets) {
			computeAllNodeOffsets()
		}
	}

	fun saveToConfig(config: YamlFile) {
		// Write all possible values, this keeps configs consistent even if defaults were changed
		for (value in SkeletonConfigValue.values) {
			config.setProperty(value.configKey, getConfig(value))
		}
		for (value in SkeletonConfigToggle.values) {
			config.setProperty(value.configKey, getToggle(value))
		}
	}

	fun resetConfigs() {
		configs.clear()
		toggles.clear()
		callCallbackOnAll(false)
		if (autoUpdateOffsets) {
			computeAllNodeOffsets()
		}
	}
}
