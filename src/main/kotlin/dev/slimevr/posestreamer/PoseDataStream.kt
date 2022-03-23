package dev.slimevr.posestreamer

import dev.slimevr.vr.processor.skeleton.HumanSkeleton
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

abstract class PoseDataStream protected constructor(protected val outputStream: OutputStream) : AutoCloseable {
	var isClosed = false
		protected set

	protected constructor(file: File) : this(FileOutputStream(file)) {}
	protected constructor(file: String) : this(FileOutputStream(file)) {}

	@Throws(IOException::class)
	open fun writeHeader(skeleton: HumanSkeleton?, streamer: PoseStreamer?) {
	}

	@Throws(IOException::class)
	abstract fun writeFrame(skeleton: HumanSkeleton?)
	@Throws(IOException::class)
	open fun writeFooter(skeleton: HumanSkeleton?) {
	}

	@Throws(IOException::class)
	override fun close() {
		outputStream.close()
		isClosed = true
	}
}
