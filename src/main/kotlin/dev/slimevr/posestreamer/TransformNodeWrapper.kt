package dev.slimevr.posestreamer

import com.jme3.math.Quaternion
import com.jme3.math.Transform
import dev.slimevr.vr.processor.TransformNode
import io.eiren.util.collections.FastList

class TransformNodeWrapper @JvmOverloads constructor(
	val wrappedNode: TransformNode,
	var name: String = wrappedNode.name,
	reversedHierarchy: Boolean = false,
	initialChildCapacity: Int = 5
) {
	val localTransform: Transform
	val worldTransform: Transform
	private var reversedHierarchy = false
	var parent: TransformNodeWrapper? = null
		protected set
	val children: MutableList<TransformNodeWrapper>

	init {
		localTransform = wrappedNode.localTransform
		worldTransform = wrappedNode.worldTransform
		this.reversedHierarchy = reversedHierarchy
		children = FastList(initialChildCapacity)
	}

	constructor(nodeToWrap: TransformNode, name: String, initialChildCapacity: Int) : this(
		nodeToWrap,
		name,
		false,
		initialChildCapacity
	) {
	}

	constructor(nodeToWrap: TransformNode, reversedHierarchy: Boolean, initialChildCapacity: Int) : this(
		nodeToWrap,
		nodeToWrap.name,
		reversedHierarchy,
		initialChildCapacity
	) {
	}

	constructor(nodeToWrap: TransformNode, reversedHierarchy: Boolean) : this(
		nodeToWrap,
		nodeToWrap.name,
		reversedHierarchy,
		5
	) {
	}

	constructor(nodeToWrap: TransformNode, initialChildCapacity: Int) : this(
		nodeToWrap,
		nodeToWrap.name,
		initialChildCapacity
	) {
	}

	fun hasReversedHierarchy(): Boolean {
		return reversedHierarchy
	}

	fun setReversedHierarchy(reversedHierarchy: Boolean) {
		this.reversedHierarchy = reversedHierarchy
	}

	fun hasLocalRotation(): Boolean {
		return wrappedNode.localRotation
	}

	fun calculateLocalRotation(relativeTo: Quaternion, result: Quaternion?): Quaternion {
		return calculateLocalRotationInverse(relativeTo.inverse(), result)
	}

	fun calculateLocalRotationInverse(inverseRelativeTo: Quaternion, result: Quaternion?): Quaternion {
		var result = result
		if (result == null) {
			result = Quaternion()
		}
		return inverseRelativeTo.mult(worldTransform.rotation, result)
	}

	fun attachChild(node: TransformNodeWrapper) {
		require(node.parent == null) { "The child node must not already have a parent" }
		children.add(node)
		node.parent = this
	}

	companion object {
		fun wrapFullHierarchyWithFakeRoot(root: TransformNode): TransformNodeWrapper {
			// Allocate a "fake" root with appropriate size depending on connections the root has
			val fakeRoot = TransformNodeWrapper(root, if (root.parent != null) 2 else 1)

			// Attach downwards hierarchy to the fake root
			wrapNodeHierarchyDown(root, fakeRoot)

			// Attach upwards hierarchy to the fake root
			fakeRoot.attachChild(wrapHierarchyUp(root))
			return fakeRoot
		}

		fun wrapFullHierarchy(root: TransformNode): TransformNodeWrapper {
			return wrapNodeHierarchyUp(wrapHierarchyDown(root))
		}

		fun wrapHierarchyDown(root: TransformNode): TransformNodeWrapper {
			return wrapNodeHierarchyDown(root, TransformNodeWrapper(root, root.children.size))
		}

		fun wrapNodeHierarchyDown(root: TransformNode, target: TransformNodeWrapper): TransformNodeWrapper {
			for (child in root.children) {
				target.attachChild(wrapHierarchyDown(child))
			}
			return target
		}

		fun wrapHierarchyUp(root: TransformNode): TransformNodeWrapper {
			return wrapNodeHierarchyUp(TransformNodeWrapper(root, true, if (root.parent != null) 1 else 0))
		}

		fun wrapNodeHierarchyUp(root: TransformNodeWrapper): TransformNodeWrapper {
			return wrapNodeHierarchyUp(root.wrappedNode, root)
		}

		fun wrapNodeHierarchyUp(root: TransformNode, target: TransformNodeWrapper): TransformNodeWrapper {
			val parent = root.parent ?: return target

			// Flip the offset for these reversed nodes
			val wrapper = TransformNodeWrapper(
				parent,
				true,
				(if (parent.parent != null) 1 else 0) + Math.max(0, parent.children.size - 1)
			)
			target.attachChild(wrapper)

			// Re-attach other children
			if (parent.children.size > 1) {
				for (child in parent.children) {
					// Skip the original node
					if (child == target.wrappedNode) {
						continue
					}
					wrapper.attachChild(wrapHierarchyDown(child))
				}
			}

			// Continue up the hierarchy
			wrapNodeHierarchyUp(wrapper)
			// Return original node
			return target
		}
	}
}
