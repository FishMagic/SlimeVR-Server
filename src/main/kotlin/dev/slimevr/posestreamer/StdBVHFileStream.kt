package dev.slimevr.posestreamer

import dev.slimevr.vr.processor.TransformNode
import java.io.File
import java.io.OutputStream

class StdBVHFileStream : BVHFileStream {
	constructor(outputStream: OutputStream?) : super(outputStream) {}
	constructor(file: File?) : super(file) {}
	constructor(file: String?) : super(file) {}

	override fun wrapSkeletonNodes(rootNode: TransformNode?): TransformNodeWrapper? {
		val newRoot = getNodeFromHierarchy(rootNode, "Hip") ?: return null
		return TransformNodeWrapper.wrapFullHierarchy(newRoot)
	}

	private fun getNodeFromHierarchy(node: TransformNode?, name: String): TransformNode? {
		if (node!!.name.equals(name, ignoreCase = true)) {
			return node
		}
		for (child in node.children) {
			val result = getNodeFromHierarchy(child, name)
			if (result != null) {
				return result
			}
		}
		return null
	}
}
