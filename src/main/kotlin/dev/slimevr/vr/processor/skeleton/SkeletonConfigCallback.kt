package dev.slimevr.vr.processor.skeleton

import com.jme3.math.Vector3f

interface SkeletonConfigCallback {
	fun updateConfigState(config: SkeletonConfigValue?, newValue: Float)
	fun updateToggleState(configToggle: SkeletonConfigToggle?, newValue: Boolean)
	fun updateNodeOffset(nodeOffset: SkeletonNodeOffset?, offset: Vector3f)
}
