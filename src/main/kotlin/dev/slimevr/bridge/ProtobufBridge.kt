package dev.slimevr.bridge

import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import dev.slimevr.Main
import dev.slimevr.bridge.ProtobufMessages.*
import dev.slimevr.util.ann.VRServerThread
import dev.slimevr.vr.trackers.*
import dev.slimevr.vr.trackers.TrackerStatus
import io.eiren.util.ann.Synchronize
import io.eiren.util.ann.ThreadSafe
import io.eiren.util.collections.FastList
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

abstract class ProtobufBridge(protected val bridgeName: String, private val hmd: HMDTracker) : Bridge {
	private val vec1 = Vector3f()
	private val quat1 = Quaternion()

	@ThreadSafe
	private val inputQueue: Queue<ProtobufMessage> = LinkedBlockingQueue()

	@ThreadSafe
	private val outputQueue: Queue<ProtobufMessage> = LinkedBlockingQueue()

	@VRServerThread
	protected val sharedTrackers: MutableList<ShareableTracker?> = FastList()

	@Synchronize("self")
	private val remoteTrackersBySerial: MutableMap<String, VRTracker?> = HashMap()

	@Synchronize("self")
	private val remoteTrackersByTrackerId: MutableMap<Int, VRTracker?> = HashMap()
	private var hadNewData = false
	private var hmdTracker: VRTracker? = null
	@BridgeThread
	protected abstract fun sendMessageReal(message: ProtobufMessage?): Boolean
	@BridgeThread
	protected fun messageReceived(message: ProtobufMessage) {
		inputQueue.add(message)
	}

	@ThreadSafe
	protected fun sendMessage(message: ProtobufMessage) {
		outputQueue.add(message)
	}

	@BridgeThread
	protected fun updateMessageQueue() {
		var message: ProtobufMessage? = null
		while (outputQueue.poll().also { message = it } != null) {
			if (!sendMessageReal(message)) return
		}
	}

	@VRServerThread
	override fun dataRead() {
		hadNewData = false
		var message: ProtobufMessage? = null
		while (inputQueue.poll().also { message = it } != null) {
			processMessageReceived(message)
			hadNewData = true
		}
		if (hadNewData && hmdTracker != null) {
			trackerOverrideUpdate(hmdTracker!!, hmd)
		}
	}

	@VRServerThread
	protected fun trackerOverrideUpdate(source: VRTracker?, target: ComputedTracker) {
		target.position.set(source!!.position)
		target.rotation.set(source.rotation)
		target.status = source.status
		target.dataTick()
	}

	@VRServerThread
	override fun dataWrite() {
		if (!hadNewData) // Don't write anything if no message were received, we always process at the speed of the other side
			return
		for (i in sharedTrackers.indices) {
			writeTrackerUpdate(sharedTrackers[i])
		}
	}

	@VRServerThread
	protected fun writeTrackerUpdate(localTracker: ShareableTracker?) {
		val builder = Position.newBuilder().setTrackerId(
			localTracker!!.trackerId
		)
		if (localTracker.getPosition(vec1)) {
			builder.x = vec1.x
			builder.y = vec1.y
			builder.z = vec1.z
		}
		if (localTracker.getRotation(quat1)) {
			builder.qx = quat1.x
			builder.qy = quat1.y
			builder.qz = quat1.z
			builder.qw = quat1.w
		}
		sendMessage(ProtobufMessage.newBuilder().setPosition(builder).build())
	}

	@VRServerThread
	protected fun processMessageReceived(message: ProtobufMessage?) {
		//if(!message.hasPosition())
		//	LogManager.log.info("[" + bridgeName + "] MSG: " + message);
		if (message!!.hasPosition()) {
			positionReceived(message.position)
		} else if (message.hasUserAction()) {
			userActionReceived(message.userAction)
		} else if (message.hasTrackerStatus()) {
			trackerStatusReceived(message.trackerStatus)
		} else if (message.hasTrackerAdded()) {
			trackerAddedReceived(message.trackerAdded)
		}
	}

	@VRServerThread
	protected fun positionReceived(positionMessage: Position) {
		val tracker = getInternalRemoteTrackerById(positionMessage.trackerId)
		if (tracker != null) {
			if (positionMessage.hasX()) tracker.position[positionMessage.x, positionMessage.y] = positionMessage.z
			tracker.rotation[positionMessage.qx, positionMessage.qy, positionMessage.qz] = positionMessage.qw
			tracker.dataTick()
		}
	}

	@VRServerThread
	protected abstract fun createNewTracker(trackerAdded: TrackerAdded?): VRTracker
	@VRServerThread
	protected fun trackerAddedReceived(trackerAdded: TrackerAdded) {
		var tracker = getInternalRemoteTrackerById(trackerAdded.trackerId)
		if (tracker != null) {
			// TODO reinit?
			return
		}
		tracker = createNewTracker(trackerAdded)
		synchronized(remoteTrackersBySerial) { remoteTrackersBySerial.put(tracker!!.name, tracker) }
		synchronized(remoteTrackersByTrackerId) { remoteTrackersByTrackerId.put(tracker!!.trackerId, tracker) }
		if (trackerAdded.trackerRole == TrackerRole.HMD.id) {
			hmdTracker = tracker
		} else {
			Main.vrServer!!.registerTracker(tracker)
		}
	}

	@VRServerThread
	protected fun userActionReceived(userAction: UserAction) {
		when (userAction.name) {
			"calibrate" ->      // TODO : Check pose field
				Main.vrServer!!.resetTrackers()
		}
	}

	@VRServerThread
	protected fun trackerStatusReceived(trackerStatus: ProtobufMessages.TrackerStatus) {
		val tracker = getInternalRemoteTrackerById(trackerStatus.trackerId)
		if (tracker != null) {
			tracker.status = TrackerStatus.getById(trackerStatus.statusValue)!!
		}
	}

	@ThreadSafe
	protected fun getInternalRemoteTrackerById(trackerId: Int): VRTracker? {
		synchronized(remoteTrackersByTrackerId) { return remoteTrackersByTrackerId[trackerId] }
	}

	@VRServerThread
	protected fun reconnected() {
		for (i in sharedTrackers.indices) {
			val tracker = sharedTrackers[i]
			val builder = TrackerAdded.newBuilder().setTrackerId(tracker!!.trackerId).setTrackerName(
				tracker.descriptiveName
			).setTrackerSerial(tracker.name).setTrackerRole(tracker.trackerRole!!.id)
			sendMessage(ProtobufMessage.newBuilder().setTrackerAdded(builder).build())
		}
	}

	@VRServerThread
	protected fun disconnected() {
		synchronized(remoteTrackersByTrackerId) {
			val iterator: Iterator<Map.Entry<Int, VRTracker?>> =
				remoteTrackersByTrackerId.entries.iterator()
			while (iterator.hasNext()) {
				iterator.next().value!!.status = TrackerStatus.DISCONNECTED
			}
		}
		if (hmdTracker != null) {
			hmd.status = TrackerStatus.DISCONNECTED
		}
	}

	@VRServerThread
	override fun addSharedTracker(tracker: ShareableTracker?) {
		if (sharedTrackers.contains(tracker)) return
		sharedTrackers.add(tracker)
		val builder = TrackerAdded.newBuilder().setTrackerId(tracker!!.trackerId).setTrackerName(
			tracker.descriptiveName
		).setTrackerSerial(tracker.name).setTrackerRole(tracker.trackerRole!!.id)
		sendMessage(ProtobufMessage.newBuilder().setTrackerAdded(builder).build())
	}

	@VRServerThread
	override fun removeSharedTracker(tracker: ShareableTracker?) {
		sharedTrackers.remove(tracker)
		// No message can be sent to the remote side, protocol doesn't support tracker removal (yet)
	}
}
