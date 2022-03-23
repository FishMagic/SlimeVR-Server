package dev.slimevr.vr.processor

import com.jme3.math.Transform
import io.eiren.util.collections.FastList
import java.util.function.Consumer

class TransformNode @JvmOverloads constructor(var name: String, localRotation: Boolean = true) {
	val localTransform = Transform()
	val worldTransform = Transform()
	val children: MutableList<TransformNode> = FastList()
	var localRotation = false
	var parent: TransformNode? = null
		protected set

	init {
		this.localRotation = localRotation
	}

	fun attachChild(node: TransformNode) {
		require(node.parent == null) { "The child node must not already have a parent" }
		children.add(node)
		node.parent = this
	}

	fun update() {
		updateWorldTransforms() // Call update on each frame because we have relatively few nodes
		for (i in children.indices) children[i].update()
	}

	@Synchronized
	protected fun updateWorldTransforms() {
		if (parent == null) {
			worldTransform.set(localTransform)
		} else {
			worldTransform.set(localTransform)
			if (localRotation) worldTransform.combineWithParent(parent!!.worldTransform) else combineWithParentGlobalRotation(
				parent!!.worldTransform
			)
		}
	}

	fun depthFirstTraversal(visitor: Consumer<TransformNode>) {
		for (i in children.indices) {
			children[i].depthFirstTraversal(visitor)
		}
		visitor.accept(this)
	}

	fun combineWithParentGlobalRotation(parent: Transform) {
		worldTransform.scale.multLocal(parent.scale)
		worldTransform.translation.multLocal(parent.scale)
		parent.rotation.multLocal(worldTransform.translation).addLocal(parent.translation)
	}
}
