package dev.slimevr.vr.processor.skeleton

import com.jme3.math.FastMath
import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import dev.slimevr.VRServer
import dev.slimevr.util.ann.VRServerThread
import dev.slimevr.vr.processor.ComputedHumanPoseTracker
import dev.slimevr.vr.processor.ComputedHumanPoseTrackerPosition
import dev.slimevr.vr.processor.TransformNode
import dev.slimevr.vr.trackers.*
import io.eiren.util.collections.FastList

open class SimpleSkeleton protected constructor(computedTrackers: List<ComputedHumanPoseTracker?>?) :
	HumanSkeleton(), SkeletonConfigCallback {
	//#endregion
	//#region Upper body nodes (torso)
	override val rootNode = TransformNode("HMD", false)
	override val allNodes: Array<TransformNode>
		get() {
			val nodeList = FastList<TransformNode>()
			rootNode.depthFirstTraversal {				nodeList.add(it)			}
			leftHandNode.depthFirstTraversal {				nodeList.add(it)			}
			rightHandNode.depthFirstTraversal { nodeList.add(it)}
			return nodeList.toArray(arrayOf<TransformNode>())
		}
	protected val headNode = TransformNode("Head", false)
	protected val neckNode = TransformNode("Neck", false)
	protected val chestNode = TransformNode("Chest", false)
	protected val trackerChestNode = TransformNode("Chest-Tracker", false)
	protected val waistNode = TransformNode("Waist", false)
	protected val hipNode = TransformNode("Hip", false)
	protected val trackerWaistNode = TransformNode("Waist-Tracker", false)

	//#endregion
	//#region Lower body nodes (legs)
	protected val leftHipNode = TransformNode("Left-Hip", false)
	protected val leftKneeNode = TransformNode("Left-Knee", false)
	protected val trackerLeftKneeNode = TransformNode("Left-Knee-Tracker", false)
	protected val leftAnkleNode = TransformNode("Left-Ankle", false)
	protected val leftFootNode = TransformNode("Left-Foot", false)
	protected val trackerLeftFootNode = TransformNode("Left-Foot-Tracker", false)
	protected val rightHipNode = TransformNode("Right-Hip", false)
	protected val rightKneeNode = TransformNode("Right-Knee", false)
	protected val trackerRightKneeNode = TransformNode("Right-Knee-Tracker", false)
	protected val rightAnkleNode = TransformNode("Right-Ankle", false)
	protected val rightFootNode = TransformNode("Right-Foot", false)
	protected val trackerRightFootNode = TransformNode("Right-Foot-Tracker", false)
	protected var minKneePitch = 0f * FastMath.DEG_TO_RAD
	protected var maxKneePitch = 90f * FastMath.DEG_TO_RAD
	protected var kneeLerpFactor = 0.5f

	//#endregion
	//#region Arms (elbows)
	protected val leftHandNode = TransformNode("Left-Hand", false)
	protected val rightHandNode = TransformNode("Right-Hand", false)
	protected val leftWristNode = TransformNode("Left-Wrist", false)
	protected val rightWristNode = TransformNode("Right-Wrist", false)
	protected val leftElbowNode = TransformNode("Left-Elbow", false)
	protected val rightElbowNode = TransformNode("Right-Elbow", false)
	protected val trackerLeftElbowNode = TransformNode("Left-Elbow-Tracker", false)
	protected val trackerRightElbowNode = TransformNode("Right-Elbow-Tracker", false)

	//#endregion
	//#region Tracker Input
	protected var hmdTracker: Tracker? = null
	protected var chestTracker: Tracker? = null
	protected var waistTracker: Tracker? = null
	protected var hipTracker: Tracker? = null
	protected var leftLegTracker: Tracker? = null
	protected var leftAnkleTracker: Tracker? = null
	protected var leftFootTracker: Tracker? = null
	protected var rightLegTracker: Tracker? = null
	protected var rightAnkleTracker: Tracker? = null
	protected var rightFootTracker: Tracker? = null
	protected var leftHandTracker: Tracker? = null
	protected var rightHandTracker: Tracker? = null
	protected var leftElbowTracker: Tracker? = null
	protected var rightElbowTracker: Tracker? = null

	//#endregion
	//#region Tracker Output
	protected var computedChestTracker: ComputedHumanPoseTracker? = null
	protected var computedWaistTracker: ComputedHumanPoseTracker? = null
	protected var computedLeftKneeTracker: ComputedHumanPoseTracker? = null
	protected var computedLeftFootTracker: ComputedHumanPoseTracker? = null
	protected var computedRightKneeTracker: ComputedHumanPoseTracker? = null
	protected var computedRightFootTracker: ComputedHumanPoseTracker? = null
	protected var computedLeftElbowTracker: ComputedHumanPoseTracker? = null
	protected var computedRightElbowTracker: ComputedHumanPoseTracker? = null

	//#endregion
	protected var extendedPelvisModel = true
	protected var extendedKneeModel = false
	final override val skeletonConfig: SkeletonConfig

	//#region Buffers
	private val posBuf = Vector3f()
	private val rotBuf1 = Quaternion()
	private val rotBuf2 = Quaternion()
	protected val hipVector = Vector3f()
	protected val ankleVector = Vector3f()
	protected val kneeRotation = Quaternion()

	//#endregion
	//#region Constructors
	init {
		//#region Assemble skeleton to hip
		rootNode.attachChild(headNode)
		headNode.attachChild(neckNode)
		neckNode.attachChild(chestNode)
		chestNode.attachChild(waistNode)
		waistNode.attachChild(hipNode)
		//#endregion

		//#region Assemble skeleton to feet
		hipNode.attachChild(leftHipNode)
		hipNode.attachChild(rightHipNode)
		leftHipNode.attachChild(leftKneeNode)
		rightHipNode.attachChild(rightKneeNode)
		leftKneeNode.attachChild(leftAnkleNode)
		rightKneeNode.attachChild(rightAnkleNode)
		leftAnkleNode.attachChild(leftFootNode)
		rightAnkleNode.attachChild(rightFootNode)
		//#endregion

		//#region Assemble skeleton arms
		leftHandNode.attachChild(leftWristNode)
		rightHandNode.attachChild(rightWristNode)
		leftWristNode.attachChild(leftElbowNode)
		rightWristNode.attachChild(rightElbowNode)
		//#endregion

		//#region Attach tracker nodes for offsets
		chestNode.attachChild(trackerChestNode)
		hipNode.attachChild(trackerWaistNode)
		leftKneeNode.attachChild(trackerLeftKneeNode)
		rightKneeNode.attachChild(trackerRightKneeNode)
		leftFootNode.attachChild(trackerLeftFootNode)
		rightFootNode.attachChild(trackerRightFootNode)
		leftElbowNode.attachChild(trackerLeftElbowNode)
		rightElbowNode.attachChild(trackerRightElbowNode)
		//#endregion

		// Set default skeleton configuration (callback automatically sets initial offsets)
		skeletonConfig = SkeletonConfig(true, this)
		computedTrackers?.let { setComputedTrackers(it) }
		fillNullComputedTrackers(true)
	}

	constructor(server: VRServer, computedTrackers: List<ComputedHumanPoseTracker?>?) : this(computedTrackers) {
		setTrackersFromServer(server)
		skeletonConfig.loadFromConfig(server.config)
	}

	constructor(trackers: List<Tracker?>?, computedTrackers: List<ComputedHumanPoseTracker?>?) : this(computedTrackers) {
		if (trackers != null) {
			setTrackersFromList(trackers)
		} else {
			setTrackersFromList(FastList(0))
		}
	}

	@JvmOverloads
	constructor(
		trackers: List<Tracker?>?,
		computedTrackers: List<ComputedHumanPoseTracker?>?,
		configs: Map<SkeletonConfigValue?, Float?>?,
		altConfigs: Map<SkeletonConfigValue?, Float?>? = null
	) : this(trackers, computedTrackers) {
		// Initialize

		// Set configs
		if (altConfigs != null) {
			// Set alts first, so if there's any overlap it doesn't affect the values
			skeletonConfig.setConfigs(altConfigs, null)
		}
		skeletonConfig.setConfigs(configs, null)
	}

	//#endregion
	//#region Set Trackers
	fun setTrackersFromList(trackers: List<Tracker?>, setHmd: Boolean) {
		if (setHmd) {
			hmdTracker = TrackerUtils.findTrackerForBodyPosition(trackers, TrackerPosition.HMD)
		}
		chestTracker = TrackerUtils.findTrackerForBodyPositionOrEmpty(
			trackers,
			TrackerPosition.CHEST,
			TrackerPosition.WAIST,
			TrackerPosition.HIP
		)
		waistTracker = TrackerUtils.findTrackerForBodyPositionOrEmpty(
			trackers,
			TrackerPosition.WAIST,
			TrackerPosition.HIP,
			TrackerPosition.CHEST
		)
		hipTracker = TrackerUtils.findTrackerForBodyPositionOrEmpty(
			trackers,
			TrackerPosition.HIP,
			TrackerPosition.WAIST,
			TrackerPosition.CHEST
		)
		leftLegTracker = TrackerUtils.findTrackerForBodyPositionOrEmpty(
			trackers,
			TrackerPosition.LEFT_LEG,
			TrackerPosition.LEFT_ANKLE,
			null
		)
		leftAnkleTracker = TrackerUtils.findTrackerForBodyPositionOrEmpty(
			trackers,
			TrackerPosition.LEFT_ANKLE,
			TrackerPosition.LEFT_LEG,
			null
		)
		leftFootTracker = TrackerUtils.findTrackerForBodyPosition(trackers, TrackerPosition.LEFT_FOOT)
		rightLegTracker = TrackerUtils.findTrackerForBodyPositionOrEmpty(
			trackers,
			TrackerPosition.RIGHT_LEG,
			TrackerPosition.RIGHT_ANKLE,
			null
		)
		rightAnkleTracker = TrackerUtils.findTrackerForBodyPositionOrEmpty(
			trackers,
			TrackerPosition.RIGHT_ANKLE,
			TrackerPosition.RIGHT_LEG,
			null
		)
		rightFootTracker = TrackerUtils.findTrackerForBodyPosition(trackers, TrackerPosition.RIGHT_FOOT)
		leftHandTracker = TrackerUtils.findTrackerForBodyPosition(trackers, TrackerPosition.LEFT_CONTROLLER)
		rightHandTracker = TrackerUtils.findTrackerForBodyPosition(trackers, TrackerPosition.RIGHT_CONTROLLER)
		leftElbowTracker = TrackerUtils.findTrackerForBodyPosition(trackers, TrackerPosition.LEFT_ELBOW)
		rightElbowTracker = TrackerUtils.findTrackerForBodyPosition(trackers, TrackerPosition.RIGHT_ELBOW)
	}

	fun setTrackersFromList(trackers: List<Tracker?>) {
		setTrackersFromList(trackers, true)
	}

	fun setTrackersFromServer(server: VRServer) {
		hmdTracker = server.hmdTracker
		setTrackersFromList(server.getAllTrackers(), false)
	}

	fun setComputedTracker(tracker: ComputedHumanPoseTracker?) {
		when (tracker?.trackerRole) {
			TrackerRole.CHEST -> computedChestTracker = tracker
			TrackerRole.WAIST -> computedWaistTracker = tracker
			TrackerRole.LEFT_KNEE -> computedLeftKneeTracker = tracker
			TrackerRole.LEFT_FOOT -> computedLeftFootTracker = tracker
			TrackerRole.RIGHT_KNEE -> computedRightKneeTracker = tracker
			TrackerRole.RIGHT_FOOT -> computedRightFootTracker = tracker
			TrackerRole.LEFT_ELBOW -> computedLeftElbowTracker = tracker
			TrackerRole.RIGHT_ELBOW -> computedRightElbowTracker = tracker
		}
	}

	fun setComputedTrackers(trackers: List<ComputedHumanPoseTracker?>) {
		for (i in trackers.indices) {
			setComputedTracker(trackers[i])
		}
	}

	fun setComputedTrackersAndFillNull(trackers: List<ComputedHumanPoseTracker>, onlyFillWaistAndFeet: Boolean) {
		setComputedTrackers(trackers)
		fillNullComputedTrackers(onlyFillWaistAndFeet)
	}

	fun fillNullComputedTrackers(onlyFillWaistAndFeet: Boolean) {
		if (computedWaistTracker == null) {
			computedWaistTracker = ComputedHumanPoseTracker(
				Tracker.getNextLocalTrackerId(),
				ComputedHumanPoseTrackerPosition.WAIST,
				TrackerRole.WAIST
			)
			computedWaistTracker!!.status = TrackerStatus.OK
		}
		if (computedLeftFootTracker == null) {
			computedLeftFootTracker = ComputedHumanPoseTracker(
				Tracker.getNextLocalTrackerId(),
				ComputedHumanPoseTrackerPosition.LEFT_FOOT,
				TrackerRole.LEFT_FOOT
			)
			computedLeftFootTracker!!.status = TrackerStatus.OK
		}
		if (computedRightFootTracker == null) {
			computedRightFootTracker = ComputedHumanPoseTracker(
				Tracker.getNextLocalTrackerId(),
				ComputedHumanPoseTrackerPosition.RIGHT_FOOT,
				TrackerRole.RIGHT_FOOT
			)
			computedRightFootTracker!!.status = TrackerStatus.OK
		}
		if (!onlyFillWaistAndFeet) {
			if (computedChestTracker == null) {
				computedChestTracker = ComputedHumanPoseTracker(
					Tracker.getNextLocalTrackerId(),
					ComputedHumanPoseTrackerPosition.CHEST,
					TrackerRole.CHEST
				)
				computedChestTracker!!.status = TrackerStatus.OK
			}
			if (computedLeftKneeTracker == null) {
				computedLeftKneeTracker = ComputedHumanPoseTracker(
					Tracker.getNextLocalTrackerId(),
					ComputedHumanPoseTrackerPosition.LEFT_KNEE,
					TrackerRole.LEFT_KNEE
				)
				computedLeftKneeTracker!!.status = TrackerStatus.OK
			}
			if (computedRightKneeTracker == null) {
				computedRightKneeTracker = ComputedHumanPoseTracker(
					Tracker.getNextLocalTrackerId(),
					ComputedHumanPoseTrackerPosition.RIGHT_KNEE,
					TrackerRole.RIGHT_KNEE
				)
				computedRightKneeTracker!!.status = TrackerStatus.OK
			}
			if (computedLeftElbowTracker == null) {
				computedLeftElbowTracker = ComputedHumanPoseTracker(
					Tracker.getNextLocalTrackerId(),
					ComputedHumanPoseTrackerPosition.LEFT_ELBOW,
					TrackerRole.LEFT_ELBOW
				)
				computedLeftElbowTracker!!.status = TrackerStatus.OK
			}
			if (computedRightElbowTracker == null) {
				computedRightElbowTracker = ComputedHumanPoseTracker(
					Tracker.getNextLocalTrackerId(),
					ComputedHumanPoseTrackerPosition.RIGHT_ELBOW,
					TrackerRole.RIGHT_ELBOW
				)
				computedRightElbowTracker!!.status = TrackerStatus.OK
			}
		}
	}

	//#endregion
	//#region Get Trackers
	fun getComputedTracker(trackerRole: TrackerRole?): ComputedHumanPoseTracker? {
		when (trackerRole) {
			TrackerRole.CHEST -> return computedChestTracker
			TrackerRole.WAIST -> return computedWaistTracker
			TrackerRole.LEFT_KNEE -> return computedLeftKneeTracker
			TrackerRole.LEFT_FOOT -> return computedLeftFootTracker
			TrackerRole.RIGHT_KNEE -> return computedRightKneeTracker
			TrackerRole.RIGHT_FOOT -> return computedRightFootTracker
			TrackerRole.LEFT_ELBOW -> return computedLeftElbowTracker
			TrackerRole.RIGHT_ELBOW -> return computedRightElbowTracker
		}
		return null
	}

	//#endregion
	//#region Processing
	// Useful for sub-classes that need to return a sub-tracker (like PoseFrameTracker -> TrackerFrame)
	protected open fun trackerPreUpdate(tracker: Tracker?): Tracker? {
		return tracker
	}

	// Updates the pose from tracker positions
	@VRServerThread
	override fun updatePose() {
		updateLocalTransforms()
		updateRootTrackers()
		updateComputedTrackers()
	}

	fun updateRootTrackers() {
		rootNode.update()
		leftHandNode.update()
		rightHandNode.update()
	}

	//#region Update the node transforms from the trackers
	protected fun updateLocalTransforms() {
		//#region Pass all trackers through trackerPreUpdate
		val hmdTracker = trackerPreUpdate(hmdTracker)
		val chestTracker = trackerPreUpdate(chestTracker)
		val waistTracker = trackerPreUpdate(waistTracker)
		val hipTracker = trackerPreUpdate(hipTracker)
		val leftLegTracker = trackerPreUpdate(leftLegTracker)
		val leftAnkleTracker = trackerPreUpdate(leftAnkleTracker)
		val leftFootTracker = trackerPreUpdate(leftFootTracker)
		val rightLegTracker = trackerPreUpdate(rightLegTracker)
		val rightAnkleTracker = trackerPreUpdate(rightAnkleTracker)
		val rightFootTracker = trackerPreUpdate(rightFootTracker)
		val leftHandTracker = trackerPreUpdate(leftHandTracker)
		val rightHandTracker = trackerPreUpdate(rightHandTracker)
		val rightElbowTracker = trackerPreUpdate(rightElbowTracker)
		val leftElbowTracker = trackerPreUpdate(leftElbowTracker)
		//#endregion
		if (hmdTracker != null) {
			if (hmdTracker.getPosition(posBuf)) {
				rootNode.localTransform.translation = posBuf
			}
			if (hmdTracker.getRotation(rotBuf1)) {
				rootNode.localTransform.rotation = rotBuf1
				headNode.localTransform.rotation = rotBuf1
			}
		} else {
			// Set to zero
			rootNode.localTransform.translation = Vector3f.ZERO
			rootNode.localTransform.rotation = Quaternion.IDENTITY
			headNode.localTransform.rotation = Quaternion.IDENTITY
		}
		if (chestTracker!!.getRotation(rotBuf1)) {
			neckNode.localTransform.rotation = rotBuf1
		}
		if (waistTracker!!.getRotation(rotBuf1)) {
			chestNode.localTransform.rotation = rotBuf1
			trackerChestNode.localTransform.rotation = rotBuf1
		}
		if (hipTracker!!.getRotation(rotBuf1)) {
			waistNode.localTransform.rotation = rotBuf1
			trackerWaistNode.localTransform.rotation = rotBuf1
			hipNode.localTransform.rotation = rotBuf1
		}

		// Left Leg
		leftLegTracker!!.getRotation(rotBuf1)
		leftAnkleTracker!!.getRotation(rotBuf2)
		if (extendedKneeModel) calculateKneeLimits(
			rotBuf1,
			rotBuf2,
			leftLegTracker.confidenceLevel,
			leftAnkleTracker.confidenceLevel
		)
		leftHipNode.localTransform.rotation = rotBuf1
		leftKneeNode.localTransform.rotation = rotBuf2
		leftAnkleNode.localTransform.rotation = rotBuf2
		leftFootNode.localTransform.rotation = rotBuf2
		trackerLeftKneeNode.localTransform.rotation = rotBuf2
		trackerLeftFootNode.localTransform.rotation = rotBuf2
		if (leftFootTracker != null) {
			leftFootTracker.getRotation(rotBuf2)
			leftAnkleNode.localTransform.rotation = rotBuf2
			leftFootNode.localTransform.rotation = rotBuf2
			trackerLeftFootNode.localTransform.rotation = rotBuf2
		}

		// Right Leg
		rightLegTracker!!.getRotation(rotBuf1)
		rightAnkleTracker!!.getRotation(rotBuf2)
		if (extendedKneeModel) calculateKneeLimits(
			rotBuf1,
			rotBuf2,
			rightLegTracker.confidenceLevel,
			rightAnkleTracker.confidenceLevel
		)
		rightHipNode.localTransform.rotation = rotBuf1
		rightKneeNode.localTransform.rotation = rotBuf2
		rightAnkleNode.localTransform.rotation = rotBuf2
		rightFootNode.localTransform.rotation = rotBuf2
		trackerRightKneeNode.localTransform.rotation = rotBuf2
		trackerRightFootNode.localTransform.rotation = rotBuf2
		if (rightFootTracker != null) {
			rightFootTracker.getRotation(rotBuf2)
			rightAnkleNode.localTransform.rotation = rotBuf2
			rightFootNode.localTransform.rotation = rotBuf2
			trackerRightFootNode.localTransform.rotation = rotBuf2
		}
		if (extendedPelvisModel) {
			// Average pelvis between two legs
			leftHipNode.localTransform.getRotation(rotBuf1)
			rightHipNode.localTransform.getRotation(rotBuf2)
			rotBuf2.nlerp(rotBuf1, 0.5f)
			chestNode.localTransform.getRotation(rotBuf1)
			rotBuf2.nlerp(rotBuf1, 0.3333333f)
			hipNode.localTransform.rotation = rotBuf2
			//trackerWaistNode.localTransform.setRotation(rotBuf2); // <== Provides cursed results from my test in VRChat when sitting or laying down -Erimel
			// TODO : Correct the trackerWaistNode without getting cursed results (only correct yaw?)
			// TODO : Use vectors to add like 50% of waist tracker yaw to waist node to reduce drift and let user take weird poses
		}


		// Left arm
		if (leftHandTracker != null) {
			if (leftHandTracker.getPosition(posBuf)) leftHandNode.localTransform.translation = posBuf
			if (leftHandTracker.getRotation(rotBuf1)) leftHandNode.localTransform.rotation = rotBuf1
		}
		if (leftElbowTracker != null) {
			if (leftElbowTracker.getRotation(rotBuf1)) {
				leftWristNode.localTransform.rotation = rotBuf1
				trackerLeftElbowNode.localTransform.rotation = rotBuf1
			}
		}

		// Right arm
		if (rightHandTracker != null) {
			if (rightHandTracker.getPosition(posBuf)) rightHandNode.localTransform.translation = posBuf
			if (rightHandTracker.getRotation(rotBuf1)) rightHandNode.localTransform.rotation = rotBuf1
		}
		if (rightElbowTracker != null) {
			if (rightElbowTracker.getRotation(rotBuf1)) {
				rightWristNode.localTransform.rotation = rotBuf1
				trackerRightElbowNode.localTransform.rotation = rotBuf1
			}
		}
	}

	//#endregion
	//#region Knee Model
	// Knee basically has only 1 DoF (pitch), average yaw and roll between knee and hip
	protected fun calculateKneeLimits(
		hipBuf: Quaternion,
		kneeBuf: Quaternion,
		hipConfidence: Float,
		kneeConfidence: Float
	) {
		ankleVector[0f, -1f] = 0f
		hipVector[0f, -1f] = 0f
		hipBuf.multLocal(hipVector)
		kneeBuf.multLocal(ankleVector)
		kneeRotation.angleBetweenVectors(hipVector, ankleVector) // Find knee angle

		// Substract knee angle from knee rotation. With perfect leg and perfect
		// sensors result should match hip rotation perfectly
		kneeBuf.multLocal(kneeRotation.inverse())

		// Average knee and hip with a slerp
		hipBuf.slerp(kneeBuf, 0.5f) // TODO : Use confidence to calculate changeAmt
		kneeBuf.set(hipBuf)

		// Return knee angle into knee rotation
		kneeBuf.multLocal(kneeRotation)
	}

	//#endregion
	//#region Update the output trackers
	protected fun updateComputedTrackers() {
		if (computedChestTracker != null) {
			computedChestTracker!!.position.set(trackerChestNode.worldTransform.translation)
			computedChestTracker!!.rotation.set(neckNode.worldTransform.rotation)
			computedChestTracker!!.dataTick()
		}
		if (computedWaistTracker != null) {
			computedWaistTracker!!.position.set(trackerWaistNode.worldTransform.translation)
			computedWaistTracker!!.rotation.set(trackerWaistNode.worldTransform.rotation)
			computedWaistTracker!!.dataTick()
		}
		if (computedLeftKneeTracker != null) {
			computedLeftKneeTracker!!.position.set(trackerLeftKneeNode.worldTransform.translation)
			computedLeftKneeTracker!!.rotation.set(leftHipNode.worldTransform.rotation)
			computedLeftKneeTracker!!.dataTick()
		}
		if (computedLeftFootTracker != null) {
			computedLeftFootTracker!!.position.set(trackerLeftFootNode.worldTransform.translation)
			computedLeftFootTracker!!.rotation.set(trackerLeftFootNode.worldTransform.rotation)
			computedLeftFootTracker!!.dataTick()
		}
		if (computedRightKneeTracker != null) {
			computedRightKneeTracker!!.position.set(trackerRightKneeNode.worldTransform.translation)
			computedRightKneeTracker!!.rotation.set(rightHipNode.worldTransform.rotation)
			computedRightKneeTracker!!.dataTick()
		}
		if (computedRightFootTracker != null) {
			computedRightFootTracker!!.position.set(trackerRightFootNode.worldTransform.translation)
			computedRightFootTracker!!.rotation.set(trackerRightFootNode.worldTransform.rotation)
			computedRightFootTracker!!.dataTick()
		}
		if (computedLeftElbowTracker != null) {
			computedLeftElbowTracker!!.position.set(trackerLeftElbowNode.worldTransform.translation)
			computedLeftElbowTracker!!.rotation.set(trackerLeftElbowNode.worldTransform.rotation)
			computedLeftElbowTracker!!.dataTick()
		}
		if (computedRightElbowTracker != null) {
			computedRightElbowTracker!!.position.set(trackerRightElbowNode.worldTransform.translation)
			computedRightElbowTracker!!.rotation.set(trackerRightElbowNode.worldTransform.rotation)
			computedRightElbowTracker!!.dataTick()
		}
	}

	//#endregion
	//#endregion
	//#region Skeleton Config
	override fun updateConfigState(config: SkeletonConfigValue?, newValue: Float) {
		// Do nothing, the node offset callback handles all that's needed
	}

	override fun updateToggleState(configToggle: SkeletonConfigToggle?, newValue: Boolean) {
		if (configToggle == null) {
			return
		}
		when (configToggle) {
			SkeletonConfigToggle.EXTENDED_PELVIS_MODEL -> extendedPelvisModel = newValue
			SkeletonConfigToggle.EXTENDED_KNEE_MODEL -> extendedKneeModel = newValue
		}
	}

	override fun updateNodeOffset(nodeOffset: SkeletonNodeOffset?, offset: Vector3f) {
		if (nodeOffset == null) {
			return
		}
		when (nodeOffset) {
			SkeletonNodeOffset.HEAD -> headNode.localTransform.translation = offset
			SkeletonNodeOffset.NECK -> neckNode.localTransform.translation = offset
			SkeletonNodeOffset.CHEST -> chestNode.localTransform.translation = offset
			SkeletonNodeOffset.CHEST_TRACKER -> trackerChestNode.localTransform.translation = offset
			SkeletonNodeOffset.WAIST -> waistNode.localTransform.translation = offset
			SkeletonNodeOffset.HIP -> hipNode.localTransform.translation = offset
			SkeletonNodeOffset.HIP_TRACKER -> trackerWaistNode.localTransform.translation = offset
			SkeletonNodeOffset.LEFT_HIP -> leftHipNode.localTransform.translation = offset
			SkeletonNodeOffset.RIGHT_HIP -> rightHipNode.localTransform.translation = offset
			SkeletonNodeOffset.KNEE -> {
				leftKneeNode.localTransform.translation = offset
				rightKneeNode.localTransform.translation = offset
			}
			SkeletonNodeOffset.KNEE_TRACKER -> {
				trackerLeftKneeNode.localTransform.translation = offset
				trackerRightKneeNode.localTransform.translation = offset
			}
			SkeletonNodeOffset.ANKLE -> {
				leftAnkleNode.localTransform.translation = offset
				rightAnkleNode.localTransform.translation = offset
			}
			SkeletonNodeOffset.FOOT -> {
				leftFootNode.localTransform.translation = offset
				rightFootNode.localTransform.translation = offset
			}
			SkeletonNodeOffset.FOOT_TRACKER -> {
				trackerLeftFootNode.localTransform.translation = offset
				trackerRightFootNode.localTransform.translation = offset
			}
			SkeletonNodeOffset.HAND -> {
				leftWristNode.localTransform.translation = offset
				rightWristNode.localTransform.translation = offset
			}
			SkeletonNodeOffset.ELBOW -> {
				leftElbowNode.localTransform.translation = offset
				rightElbowNode.localTransform.translation = offset
			}
		}
	}

	fun updatePoseAffectedByConfig(config: SkeletonConfigValue?) {
		when (config) {
			SkeletonConfigValue.HEAD -> {
				headNode.update()
				updateComputedTrackers()
			}
			SkeletonConfigValue.NECK -> {
				neckNode.update()
				updateComputedTrackers()
			}
			SkeletonConfigValue.TORSO -> {
				hipNode.update()
				updateComputedTrackers()
			}
			SkeletonConfigValue.CHEST -> {
				chestNode.update()
				updateComputedTrackers()
			}
			SkeletonConfigValue.WAIST -> {
				waistNode.update()
				updateComputedTrackers()
			}
			SkeletonConfigValue.HIP_OFFSET -> {
				trackerWaistNode.update()
				updateComputedTrackers()
			}
			SkeletonConfigValue.HIPS_WIDTH -> {
				leftHipNode.update()
				rightHipNode.update()
				updateComputedTrackers()
			}
			SkeletonConfigValue.KNEE_HEIGHT -> {
				leftKneeNode.update()
				rightKneeNode.update()
			}
			SkeletonConfigValue.LEGS_LENGTH -> {
				leftKneeNode.update()
				rightKneeNode.update()
				updateComputedTrackers()
			}
			SkeletonConfigValue.FOOT_LENGTH -> {
				leftFootNode.update()
				rightFootNode.update()
				updateComputedTrackers()
			}
			SkeletonConfigValue.FOOT_OFFSET -> {
				leftAnkleNode.update()
				rightAnkleNode.update()
				updateComputedTrackers()
			}
			SkeletonConfigValue.SKELETON_OFFSET -> {
				trackerChestNode.update()
				trackerWaistNode.update()
				trackerLeftKneeNode.update()
				trackerRightKneeNode.update()
				trackerLeftFootNode.update()
				trackerRightFootNode.update()
				trackerLeftElbowNode.update()
				trackerRightElbowNode.update()
				updateComputedTrackers()
			}
			SkeletonConfigValue.CONTROLLER_DISTANCE_Z, SkeletonConfigValue.CONTROLLER_DISTANCE_Y -> {
				leftWristNode.update()
				rightWristNode.update()
				updateComputedTrackers()
			}
			SkeletonConfigValue.ELBOW_DISTANCE -> {
				leftElbowNode.update()
				rightElbowNode.update()
				updateComputedTrackers()
			}
		}
	}

	override fun resetSkeletonConfig(config: SkeletonConfigValue?) {
		if (config == null) {
			return
		}
		val vec: Vector3f
		val height: Float
		when (config) {
			SkeletonConfigValue.HEAD -> skeletonConfig.setConfig(SkeletonConfigValue.HEAD, null)
			SkeletonConfigValue.NECK -> skeletonConfig.setConfig(SkeletonConfigValue.NECK, null)
			SkeletonConfigValue.TORSO -> {
				vec = Vector3f()
				hmdTracker!!.getPosition(vec)
				height = vec.y
				if (height > 0.5f) { // Reset only if floor level is right, TODO: read floor level from SteamVR if it's not 0
					skeletonConfig.setConfig(
						SkeletonConfigValue.TORSO,
						height * 0.42f - skeletonConfig.getConfig(SkeletonConfigValue.NECK)
					)
				} else  // if floor level is incorrect
				{
					skeletonConfig.setConfig(SkeletonConfigValue.TORSO, null)
				}
			}
			SkeletonConfigValue.CHEST -> skeletonConfig.setConfig(
				SkeletonConfigValue.CHEST,
				skeletonConfig.getConfig(SkeletonConfigValue.TORSO) * 0.57f
			)
			SkeletonConfigValue.WAIST -> skeletonConfig.setConfig(SkeletonConfigValue.WAIST, null)
			SkeletonConfigValue.HIP_OFFSET -> skeletonConfig.setConfig(SkeletonConfigValue.HIP_OFFSET, null)
			SkeletonConfigValue.HIPS_WIDTH -> skeletonConfig.setConfig(SkeletonConfigValue.HIPS_WIDTH, null)
			SkeletonConfigValue.FOOT_LENGTH -> skeletonConfig.setConfig(SkeletonConfigValue.FOOT_LENGTH, null)
			SkeletonConfigValue.FOOT_OFFSET -> skeletonConfig.setConfig(SkeletonConfigValue.FOOT_OFFSET, null)
			SkeletonConfigValue.SKELETON_OFFSET -> skeletonConfig.setConfig(SkeletonConfigValue.SKELETON_OFFSET, null)
			SkeletonConfigValue.LEGS_LENGTH -> {
				vec = Vector3f()
				hmdTracker!!.getPosition(vec)
				height = vec.y
				if (height > 0.5f) { // Reset only if floor level is right, todo: read floor level from SteamVR if it's not 0
					skeletonConfig.setConfig(
						SkeletonConfigValue.LEGS_LENGTH,
						height - skeletonConfig.getConfig(SkeletonConfigValue.NECK) - skeletonConfig.getConfig(SkeletonConfigValue.TORSO) - 0.05f
					)
				} else  //if floor level is incorrect
				{
					skeletonConfig.setConfig(SkeletonConfigValue.LEGS_LENGTH, null)
				}
				resetSkeletonConfig(SkeletonConfigValue.KNEE_HEIGHT)
			}
			SkeletonConfigValue.KNEE_HEIGHT -> skeletonConfig.setConfig(
				SkeletonConfigValue.KNEE_HEIGHT,
				skeletonConfig.getConfig(SkeletonConfigValue.LEGS_LENGTH) * 0.55f
			)
			SkeletonConfigValue.CONTROLLER_DISTANCE_Z -> skeletonConfig.setConfig(
				SkeletonConfigValue.CONTROLLER_DISTANCE_Z,
				null
			)
			SkeletonConfigValue.CONTROLLER_DISTANCE_Y -> skeletonConfig.setConfig(
				SkeletonConfigValue.CONTROLLER_DISTANCE_Y,
				null
			)
			SkeletonConfigValue.ELBOW_DISTANCE -> skeletonConfig.setConfig(SkeletonConfigValue.ELBOW_DISTANCE, null)
		}
	}

	val trackerToReset: Array<Tracker?>
		get() = arrayOf(
			trackerPreUpdate(chestTracker), trackerPreUpdate(waistTracker),
			trackerPreUpdate(hipTracker), trackerPreUpdate(leftLegTracker),
			trackerPreUpdate(leftAnkleTracker), trackerPreUpdate(leftFootTracker),
			trackerPreUpdate(rightLegTracker), trackerPreUpdate(rightAnkleTracker),
			trackerPreUpdate(rightFootTracker), trackerPreUpdate(rightElbowTracker),
			trackerPreUpdate(leftElbowTracker)
		)

	override fun resetTrackersFull() {
		//#region Pass all trackers through trackerPreUpdate
		val hmdTracker = trackerPreUpdate(hmdTracker)
		val trackersToReset = trackerToReset
		//#endregion

		// Resets all axis of the trackers with the HMD as reference.
		val referenceRotation = Quaternion()
		hmdTracker!!.getRotation(referenceRotation)
		for (tracker in trackersToReset) {
			tracker?.resetFull(referenceRotation)
		}
	}

	@VRServerThread
	override fun resetTrackersYaw() {
		//#region Pass all trackers through trackerPreUpdate
		val hmdTracker = trackerPreUpdate(hmdTracker)
		val trackersToReset = trackerToReset
		//#endregion

		// Resets the yaw of the trackers with the HMD as reference.
		val referenceRotation = Quaternion()
		hmdTracker!!.getRotation(referenceRotation)
		for (tracker in trackersToReset) {
			tracker?.resetYaw(referenceRotation)
		}
	}

	companion object {
		fun normalizeRad(angle: Float): Float {
			return FastMath.normalize(angle, -FastMath.PI, FastMath.PI)
		}

		fun interpolateRadians(factor: Float, start: Float, end: Float): Float {
			var start = start
			var end = end
			val angle = FastMath.abs(end - start)
			if (angle > FastMath.PI) {
				if (end > start) {
					start += FastMath.TWO_PI
				} else {
					end += FastMath.TWO_PI
				}
			}
			val `val` = start + (end - start) * factor
			return normalizeRad(`val`)
		}
	}
}
