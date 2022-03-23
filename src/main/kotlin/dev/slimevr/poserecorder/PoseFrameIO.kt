package dev.slimevr.poserecorder

import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import dev.slimevr.vr.trackers.TrackerPosition
import io.eiren.util.collections.FastList
import io.eiren.util.logging.LogManager
import java.io.*

object PoseFrameIO {
	fun writeFrames(outputStream: DataOutputStream, frames: PoseFrames?): Boolean {
		try {
			if (frames != null) {
				outputStream.writeInt(frames.trackerCount)
				for (tracker in frames.trackers) {
					outputStream.writeUTF(tracker.name)
					outputStream.writeInt(tracker.frameCount)
					for (i in 0 until tracker.frameCount) {
						val trackerFrame = tracker.safeGetFrame(i)
						if (trackerFrame == null) {
							outputStream.writeInt(0)
							continue
						}
						outputStream.writeInt(trackerFrame.dataFlags)
						if (trackerFrame.hasData(TrackerFrameData.DESIGNATION)) {
							outputStream.writeUTF(trackerFrame.designation.designation)
						}
						if (trackerFrame.hasData(TrackerFrameData.ROTATION)) {
							outputStream.writeFloat(trackerFrame.rotation.x)
							outputStream.writeFloat(trackerFrame.rotation.y)
							outputStream.writeFloat(trackerFrame.rotation.z)
							outputStream.writeFloat(trackerFrame.rotation.w)
						}
						if (trackerFrame.hasData(TrackerFrameData.POSITION)) {
							outputStream.writeFloat(trackerFrame.position.getX())
							outputStream.writeFloat(trackerFrame.position.getY())
							outputStream.writeFloat(trackerFrame.position.getZ())
						}
					}
				}
			} else {
				outputStream.writeInt(0)
			}
		} catch (e: Exception) {
			LogManager.log.severe("Error writing frame to stream", e)
			return false
		}
		return true
	}

	fun writeToFile(file: File?, frames: PoseFrames?): Boolean {
		try {
			DataOutputStream(BufferedOutputStream(FileOutputStream(file))).use { outputStream ->
				writeFrames(
					outputStream,
					frames
				)
			}
		} catch (e: Exception) {
			LogManager.log.severe("Error writing frames to file", e)
			return false
		}
		return true
	}

	fun readFrames(inputStream: DataInputStream): PoseFrames? {
		try {
			val trackerCount = inputStream.readInt()
			val trackers = FastList<PoseFrameTracker>(trackerCount)
			for (i in 0 until trackerCount) {
				val name = inputStream.readUTF()
				val trackerFrameCount = inputStream.readInt()
				val trackerFrames = FastList<TrackerFrame>(trackerFrameCount)
				for (j in 0 until trackerFrameCount) {
					val dataFlags = inputStream.readInt()
					var designation: TrackerPosition? = null
					if (TrackerFrameData.DESIGNATION.check(dataFlags)) {
						designation = TrackerPosition.getByDesignation(inputStream.readUTF())
					}
					var rotation: Quaternion? = null
					if (TrackerFrameData.ROTATION.check(dataFlags)) {
						val quatX = inputStream.readFloat()
						val quatY = inputStream.readFloat()
						val quatZ = inputStream.readFloat()
						val quatW = inputStream.readFloat()
						rotation = Quaternion(quatX, quatY, quatZ, quatW)
					}
					var position: Vector3f? = null
					if (TrackerFrameData.POSITION.check(dataFlags)) {
						val posX = inputStream.readFloat()
						val posY = inputStream.readFloat()
						val posZ = inputStream.readFloat()
						position = Vector3f(posX, posY, posZ)
					}
					trackerFrames.add(TrackerFrame(designation, rotation, position))
				}
				trackers.add(PoseFrameTracker(name, trackerFrames))
			}
			return PoseFrames(trackers)
		} catch (e: Exception) {
			LogManager.log.severe("Error reading frame from stream", e)
		}
		return null
	}

	fun readFromFile(file: File?): PoseFrames? {
		try {
			return readFrames(DataInputStream(BufferedInputStream(FileInputStream(file))))
		} catch (e: Exception) {
			LogManager.log.severe("Error reading frame from file", e)
		}
		return null
	}
}
