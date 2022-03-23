package dev.slimevr.posestreamer

import com.jme3.math.FastMath
import com.jme3.math.Quaternion
import dev.slimevr.vr.processor.TransformNode
import dev.slimevr.vr.processor.skeleton.HumanSkeleton
import org.apache.commons.lang3.StringUtils
import java.io.*

open class BVHFileStream : PoseDataStream {
	private var frameCount: Long = 0
	private val writer: BufferedWriter
	private var frameCountOffset: Long = 0
	private var angleBuf = FloatArray(3)
	private var rotBuf = Quaternion()
	private var wrappedSkeleton: HumanSkeleton? = null
	private var rootNode: TransformNodeWrapper? = null

	constructor(outputStream: OutputStream?) : super(outputStream!!) {
		writer = BufferedWriter(OutputStreamWriter(outputStream), 4096)
	}

	constructor(file: File?) : super(file!!) {
		writer = BufferedWriter(OutputStreamWriter(outputStream), 4096)
	}

	constructor(file: String?) : super(file!!) {
		writer = BufferedWriter(OutputStreamWriter(outputStream), 4096)
	}

	private fun getBufferedFrameCount(frameCount: Long): String {
		val frameString = java.lang.Long.toString(frameCount)
		val bufferCount = LONG_MAX_VALUE_DIGITS - frameString.length
		return if (bufferCount > 0) frameString + StringUtils.repeat(' ', bufferCount) else frameString
	}

	private fun wrapSkeletonIfNew(skeleton: HumanSkeleton): TransformNodeWrapper? {
		var wrapper = rootNode

		// If the wrapped skeleton is missing or the skeleton is updated
		if (wrapper == null || skeleton !== wrappedSkeleton) {
			wrapper = wrapSkeleton(skeleton)
		}
		return wrapper
	}

	private fun wrapSkeleton(skeleton: HumanSkeleton): TransformNodeWrapper? {
		val wrapper = wrapSkeletonNodes(skeleton.rootNode)
		wrappedSkeleton = skeleton
		rootNode = wrapper
		return wrapper
	}

	protected open fun wrapSkeletonNodes(rootNode: TransformNode?): TransformNodeWrapper? {
		return TransformNodeWrapper.wrapFullHierarchy(rootNode)
	}

	@Throws(IOException::class)
	private fun writeNodeHierarchy(node: TransformNodeWrapper?, level: Int = 0) {
		// Don't write end sites at populated nodes
		if (node!!.children.isEmpty() && node.getParent().children.size > 1) {
			return
		}
		val indentLevel = StringUtils.repeat("\t", level)
		val nextIndentLevel = indentLevel + "\t"

		// Handle ends
		if (node.children.isEmpty()) {
			writer.write(
				"""
								${indentLevel}End Site
								
								""".trimIndent()
			)
		} else {
			writer.write(
				"""
								${if (level > 0) indentLevel + "JOINT " else "ROOT "}${node.getName()}
								
								""".trimIndent()
			)
		}
		writer.write("$indentLevel{\n")

		// Ignore the root offset and original root offset
		if (level > 0 && node.wrappedNode.parent != null) {
			val offset = node.localTransform.translation
			val reverseMultiplier: Float = if (node.hasReversedHierarchy()) -1f else 1f
			writer.write(
				"""${nextIndentLevel}OFFSET ${java.lang.Float.toString(offset.getX() * OFFSET_SCALE * reverseMultiplier)} ${
					java.lang.Float.toString(
						offset.getY() * OFFSET_SCALE * reverseMultiplier
					)
				} ${java.lang.Float.toString(offset.getZ() * OFFSET_SCALE * reverseMultiplier)}
"""
			)
		} else {
			writer.write(
				"""
								${nextIndentLevel}OFFSET 0.0 0.0 0.0
								
								""".trimIndent()
			)
		}

		// Handle ends
		if (!node.children.isEmpty()) {
			// Only give position for root
			if (level > 0) {
				writer.write(
					"""
										${nextIndentLevel}CHANNELS 3 Zrotation Xrotation Yrotation
										
										""".trimIndent()
				)
			} else {
				writer.write(
					"""
										${nextIndentLevel}CHANNELS 6 Xposition Yposition Zposition Zrotation Xrotation Yrotation
										
										""".trimIndent()
				)
			}
			for (childNode in node.children) {
				writeNodeHierarchy(childNode, level + 1)
			}
		}
		writer.write("$indentLevel}\n")
	}

	@Throws(IOException::class)
	override fun writeHeader(skeleton: HumanSkeleton?, streamer: PoseStreamer?) {
		if (skeleton == null) {
			throw NullPointerException("skeleton must not be null")
		}
		if (streamer == null) {
			throw NullPointerException("streamer must not be null")
		}
		writer.write("HIERARCHY\n")
		writeNodeHierarchy(wrapSkeletonIfNew(skeleton))
		writer.write("MOTION\n")
		writer.write("Frames: ")

		// Get frame offset for finishing writing the file
		if (outputStream is FileOutputStream) {
			// Flush buffer to get proper offset
			writer.flush()
			frameCountOffset = outputStream.channel.position()
		}
		writer.write(
			"""
						${getBufferedFrameCount(frameCount)}
						
						""".trimIndent()
		)

		// Frame time in seconds
		writer.write(
			"""
						Frame Time: ${streamer.frameInterval / 1000.0}
						
						""".trimIndent()
		)
	}

	// Roughly based off code from https://github.com/TrackLab/ViRe/blob/50a987eff4db31036b2ebaeb5a28983cd473f267/Assets/Scripts/BVH/BVHRecorder.cs
	private fun quatToXyzAngles(q: Quaternion, angles: FloatArray): FloatArray {
		var angles: FloatArray? = angles
		if (angles == null) {
			angles = FloatArray(3)
		} else require(angles.size == 3) { "Angles array must have three elements" }
		val x = q.x
		val y = q.y
		val z = q.z
		val w = q.w

		// Roll (X)
		val sinrCosp = -2f * (x * y - w * z)
		val cosrCosp = w * w - x * x + y * y - z * z
		angles[0] = FastMath.atan2(sinrCosp, cosrCosp)

		// Pitch (Y)
		val sinp = 2f * (y * z + w * x)
		// Use 90 degrees if out of range
		angles[1] = if (FastMath.abs(sinp) >= 1f) FastMath.copysign(FastMath.PI / 2f, sinp) else FastMath.asin(sinp)

		// Yaw (Z)
		val sinyCosp = -2f * (x * z - w * y)
		val cosyCosp = w * w - x * x - y * y + z * z
		angles[2] = FastMath.atan2(sinyCosp, cosyCosp)
		return angles
	}

	@Throws(IOException::class)
	private fun writeNodeHierarchyRotation(node: TransformNodeWrapper?, inverseRootRot: Quaternion?) {
		val transform = node!!.worldTransform

		/*
		if (node.hasReversedHierarchy()) {
			for (TransformNodeWrapper childNode : node.children) {
				// If the hierarchy is fully reversed, set the rotation for the upper bone
				if (childNode.hasReversedHierarchy()) {
					transform = childNode.worldTransform;
					break;
				}
			}
		}
		*/rotBuf = transform.getRotation(rotBuf)

		// Adjust to local rotation
		if (inverseRootRot != null) {
			rotBuf = inverseRootRot.mult(rotBuf, rotBuf)
		}

		// Yaw (Z), roll (X), pitch (Y) (intrinsic)
		// angleBuf = rotBuf.toAngles(angleBuf);

		// Roll (X), pitch (Y), yaw (Z) (intrinsic)
		angleBuf = quatToXyzAngles(rotBuf.normalizeLocal(), angleBuf)

		// Output in order of roll (Z), pitch (X), yaw (Y) (extrinsic)
		writer.write(
			java.lang.Float.toString(angleBuf[0] * FastMath.RAD_TO_DEG) + " " + java.lang.Float.toString(
				angleBuf[1] * FastMath.RAD_TO_DEG
			) + " " + java.lang.Float.toString(angleBuf[2] * FastMath.RAD_TO_DEG)
		)

		// Get inverse rotation for child local rotations
		if (!node.children.isEmpty()) {
			val inverseRot = transform.rotation.inverse()
			for (childNode in node.children) {
				if (childNode.children.isEmpty()) {
					// If it's an end node, skip
					continue
				}

				// Add spacing
				writer.write(" ")
				writeNodeHierarchyRotation(childNode, inverseRot)
			}
		}
	}

	@Throws(IOException::class)
	override fun writeFrame(skeleton: HumanSkeleton?) {
		if (skeleton == null) {
			throw NullPointerException("skeleton must not be null")
		}
		val rootNode = wrapSkeletonIfNew(skeleton)
		val rootPos = rootNode!!.worldTransform.translation

		// Write root position
		writer.write(
			java.lang.Float.toString(rootPos.getX() * POSITION_SCALE) + " " + java.lang.Float.toString(rootPos.getY() * POSITION_SCALE) + " " + java.lang.Float.toString(
				rootPos.getZ() * POSITION_SCALE
			) + " "
		)
		writeNodeHierarchyRotation(rootNode, null)
		writer.newLine()
		frameCount++
	}

	@Throws(IOException::class)
	override fun writeFooter(skeleton: HumanSkeleton?) {
		// Write the final frame count for files
		if (outputStream is FileOutputStream) {
			// Flush before anything else
			writer.flush()
			// Seek to the count offset
			outputStream.channel.position(frameCountOffset)
			// Overwrite the count with a new value
			writer.write(java.lang.Long.toString(frameCount))
		}
	}

	@Throws(IOException::class)
	override fun close() {
		writer.close()
		super.close()
	}

	companion object {
		private val LONG_MAX_VALUE_DIGITS = java.lang.Long.toString(Long.MAX_VALUE).length
		private const val OFFSET_SCALE = 100f
		private const val POSITION_SCALE = 100f
	}
}
