package dev.slimevr.unit

import com.jme3.math.FastMath
import com.jme3.math.Quaternion
import dev.slimevr.vr.processor.TransformNode
import dev.slimevr.vr.trackers.ComputedTracker
import dev.slimevr.vr.trackers.ReferenceAdjustedTracker
import dev.slimevr.vr.trackers.Tracker
import io.eiren.math.FloatMath
import io.eiren.util.StringUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.function.Executable
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.IntConsumer
import java.util.function.IntFunction
import java.util.stream.IntStream
import java.util.stream.Stream

/**
 * Tests [ReferenceAdjustedTracker.resetFull]
 */
class ReferenceAdjustmentsTests() {
	@get:TestFactory
	val testsYaw: Stream<DynamicTest>
		get() = anglesSet.map { p: AnglesSet ->
			DynamicTest.dynamicTest("Adjustment Yaw Test of Tracker(" + p.pitch + "," + p.yaw + "," + p.roll + ")"
			) {
				IntStream.of(*yaws).forEach { refYaw: Int ->
					checkReferenceAdjustmentYaw(
						q(
							p.pitch.toFloat(),
							p.yaw.toFloat(),
							p.roll.toFloat()
						), 0, refYaw, 0
					)
				}
			}
		}

	@get:TestFactory
	val testsFull: Stream<DynamicTest>
		get() {
			return anglesSet.map { p: AnglesSet ->
				DynamicTest.dynamicTest("Adjustment Full Test of Tracker(" + p.pitch + "," + p.yaw + "," + p.roll + ")"
				) {
					anglesSet.forEach { ref: AnglesSet ->
						checkReferenceAdjustmentFull(
							q(
								p.pitch.toFloat(),
								p.yaw.toFloat(),
								p.roll.toFloat()
							),
							ref.pitch,
							ref.yaw,
							ref.roll
						)
					}
				}
			}
		}

	// TODO : Test is not passing because the test is wrong
	// See issue https://github.com/SlimeVR/SlimeVR-Server/issues/55
	//@TestFactory
	val testsForRotation: Stream<DynamicTest>
		get() {
			return anglesSet.map { p: AnglesSet ->
				IntStream.of(*yaws).mapToObj { refYaw: Int ->
					DynamicTest.dynamicTest("Adjustment Rotation Test of Tracker(" + p.pitch + "," + p.yaw + "," + p.roll + "), Ref " + refYaw
					) {
						testAdjustedTrackerRotation(
							q(
								p.pitch.toFloat(),
								p.yaw.toFloat(),
								p.roll.toFloat()
							), 0, refYaw, 0
						)
					}
				}
			}.flatMap(Function.identity())
		}

	private fun checkReferenceAdjustmentFull(trackerQuat: Quaternion?, refPitch: Int, refYaw: Int, refRoll: Int) {
		val referenceQuat: Quaternion = q(refPitch.toFloat(), refYaw.toFloat(), refRoll.toFloat())
		val tracker: ComputedTracker = ComputedTracker(Tracker.getNextLocalTrackerId(), "test", true, false)
		tracker.rotation.set(trackerQuat)
		val adj: ReferenceAdjustedTracker<ComputedTracker> = ReferenceAdjustedTracker(tracker)
		adj.resetFull(referenceQuat)
		val read: Quaternion = Quaternion()
		Assertions.assertTrue(adj.getRotation(read), "Adjusted tracker didn't return rotation")

		// Use only yaw HMD rotation
		val targetTrackerRotation: Quaternion = Quaternion(referenceQuat)
		val angles: FloatArray = FloatArray(3)
		targetTrackerRotation.toAngles(angles)
		targetTrackerRotation.fromAngles(0f, angles.get(1), 0f)
		Assertions.assertEquals(
			QuatEqualFullWithEpsilon(read), QuatEqualFullWithEpsilon(targetTrackerRotation),
			"Adjusted quat is not equal to reference quat (" + toDegs(targetTrackerRotation) + " vs " + toDegs(read) + ")"
		)
	}

	private fun checkReferenceAdjustmentYaw(trackerQuat: Quaternion?, refPitch: Int, refYaw: Int, refRoll: Int) {
		val referenceQuat: Quaternion = q(refPitch.toFloat(), refYaw.toFloat(), refRoll.toFloat())
		val tracker = ComputedTracker(Tracker.getNextLocalTrackerId(), "test", true, false)
		tracker.rotation.set(trackerQuat)
		val adj: ReferenceAdjustedTracker<ComputedTracker> = ReferenceAdjustedTracker(tracker)
		adj.resetYaw(referenceQuat)
		val read = Quaternion()
		Assertions.assertTrue(adj.getRotation(read), "Adjusted tracker didn't return rotation")
		Assertions.assertEquals(
			QuatEqualYawWithEpsilon(referenceQuat), QuatEqualYawWithEpsilon(read),
			"Adjusted quat is not equal to reference quat (" + toDegs(referenceQuat) + " vs " + toDegs(read) + ")"
		)
	}

	private fun testAdjustedTrackerRotation(trackerQuat: Quaternion, refPitch: Int, refYaw: Int, refRoll: Int) {
		val referenceQuat: Quaternion = q(refPitch.toFloat(), refYaw.toFloat(), refRoll.toFloat())
		val tracker: ComputedTracker = ComputedTracker(Tracker.getNextLocalTrackerId(), "test", true, false)
		tracker.rotation.set(trackerQuat)
		val adj: ReferenceAdjustedTracker<ComputedTracker> = ReferenceAdjustedTracker(tracker)
		adj.resetFull(referenceQuat)

		// Use only yaw HMD rotation
		val targetTrackerRotation: Quaternion = Quaternion(referenceQuat)
		val angles: FloatArray = FloatArray(3)
		targetTrackerRotation.toAngles(angles)
		targetTrackerRotation.fromAngles(0f, angles.get(1), 0f)
		val read: Quaternion = Quaternion()
		val rotation: Quaternion = Quaternion()
		val rotationCompare: Quaternion = Quaternion()
		val diff: Quaternion = Quaternion()
		val anglesAdj: FloatArray = FloatArray(3)
		val anglesDiff: FloatArray = FloatArray(3)
		val trackerNode: TransformNode = TransformNode("Tracker", true)
		val rotationNode: TransformNode = TransformNode("Rot", true)
		rotationNode.attachChild(trackerNode)
		trackerNode.localTransform.rotation = tracker.rotation
		var yaw: Int = 0
		while (yaw <= 360) {
			var pitch: Int = -90
			while (pitch <= 90) {
				var roll: Int = -90
				while (roll <= 90) {
					rotation.fromAngles(pitch * FastMath.DEG_TO_RAD, yaw * FastMath.DEG_TO_RAD, roll * FastMath.DEG_TO_RAD)
					rotationCompare.fromAngles(
						pitch * FastMath.DEG_TO_RAD,
						(yaw + refYaw) * FastMath.DEG_TO_RAD,
						roll * FastMath.DEG_TO_RAD
					)
					rotationNode.localTransform.rotation = rotation
					rotationNode.update()
					tracker.rotation.set(trackerNode.worldTransform.rotation)
					tracker.rotation.toAngles(angles)
					adj.getRotation(read)
					read.toAngles(anglesAdj)
					diff.set(read).inverseLocal().multLocal(rotationCompare)
					diff.toAngles(anglesDiff)
					if (!PRINT_TEST_RESULTS) {
						Assertions.assertTrue(
							FloatMath.equalsToZero(anglesDiff.get(0)) && FloatMath.equalsToZero(anglesDiff.get(1)) && FloatMath.equalsToZero(
								anglesDiff.get(2)
							),
							name(yaw, pitch, roll, angles, anglesAdj, anglesDiff)
						)
					} else {
						if (FloatMath.equalsToZero(anglesDiff.get(0)) && FloatMath.equalsToZero(anglesDiff.get(1)) && FloatMath.equalsToZero(
								anglesDiff.get(2)
							)
						) successes++ else errors++
						println(name(yaw, pitch, roll, angles, anglesAdj, anglesDiff))
					}
					roll += 15
				}
				pitch += 15
			}
			yaw += 30
		}
		if (PRINT_TEST_RESULTS) println("Errors: " + errors + ", successes: " + successes)
	}

	private class QuatEqualYawWithEpsilon(private val q: Quaternion) {
		override fun toString(): String {
			return q.toString()
		}

		override fun hashCode(): Int {
			return q.hashCode()
		}

		override fun equals(obj: Any?): Boolean {
			var obj: Any? = obj
			if (obj is Quaternion) obj = QuatEqualYawWithEpsilon(
				obj
			)
			if (obj !is QuatEqualYawWithEpsilon) return false
			val q2: Quaternion = obj.q
			val degs1: FloatArray = FloatArray(3)
			q.toAngles(degs1)
			val degs2: FloatArray = FloatArray(3)
			q2.toAngles(degs2)
			if (degs1.get(1) < -FloatMath.ANGLE_EPSILON_RAD) degs1[1] += FastMath.TWO_PI
			if (degs2.get(1) < -FloatMath.ANGLE_EPSILON_RAD) degs2[1] += FastMath.TWO_PI
			return FloatMath.equalsWithEpsilon(degs1.get(1), degs2.get(1))
		}
	}

	class QuatEqualFullWithEpsilon(private val q: Quaternion) {
		override fun toString(): String {
			return q.toString()
		}

		override fun hashCode(): Int {
			return q.hashCode()
		}

		override fun equals(obj: Any?): Boolean {
			var obj: Any? = obj
			if (obj is Quaternion) obj = QuatEqualFullWithEpsilon(
				obj
			)
			if (!(obj is QuatEqualFullWithEpsilon)) return false
			val q2: Quaternion = obj.q
			val degs1: FloatArray = FloatArray(3)
			q.toAngles(degs1)
			val degs2: FloatArray = FloatArray(3)
			q2.toAngles(degs2)
			if (degs1.get(1) < -FloatMath.ANGLE_EPSILON_RAD) degs1[1] += FastMath.TWO_PI
			if (degs2.get(1) < -FloatMath.ANGLE_EPSILON_RAD) degs2[1] += FastMath.TWO_PI
			return (FloatMath.equalsWithEpsilon(degs1.get(0), degs2.get(0))
					&& FloatMath.equalsWithEpsilon(degs1.get(1), degs2.get(1))
					&& FloatMath.equalsWithEpsilon(degs1.get(2), degs2.get(2)))
		}
	}

	class AnglesSet(val pitch: Int, val yaw: Int, val roll: Int)
	companion object {
		private val yaws: IntArray = intArrayOf(0, 45, 90, 180, 270)
		private val pitches: IntArray = intArrayOf(0, 15, 35, -15, -35)
		private val rolls: IntArray = intArrayOf(0, 15, 35, -15, -35)
		private val PRINT_TEST_RESULTS: Boolean = false
		private var errors: Int = 0
		private var successes: Int = 0
		val anglesSet: Stream<AnglesSet>
			get() {
				return IntStream.of(*yaws).mapToObj({ yaw: Int ->
					IntStream.of(*pitches).mapToObj(
						IntFunction { pitch: Int ->
							IntStream.of(*rolls).mapToObj(
								IntFunction { roll: Int ->
									AnglesSet(
										pitch,
										yaw,
										roll
									)
								}
							)
						})
				}).flatMap(Function.identity()).flatMap(Function.identity())
			}

		private fun name(
			yaw: Int,
			pitch: Int,
			roll: Int,
			angles: FloatArray,
			anglesAdj: FloatArray,
			anglesDiff: FloatArray
		): String {
			return ("Rot: " + yaw + "/" + pitch + "/" + roll + ". "
					+ "Angles: " + StringUtils.prettyNumber(
				angles.get(0) * FastMath.RAD_TO_DEG,
				1
			) + "/" + StringUtils.prettyNumber(anglesAdj.get(0) * FastMath.RAD_TO_DEG, 1) + ", "
					+ StringUtils.prettyNumber(
				angles.get(1) * FastMath.RAD_TO_DEG,
				1
			) + "/" + StringUtils.prettyNumber(anglesAdj.get(1) * FastMath.RAD_TO_DEG, 1) + ", "
					+ StringUtils.prettyNumber(
				angles.get(2) * FastMath.RAD_TO_DEG,
				1
			) + "/" + StringUtils.prettyNumber(anglesAdj.get(2) * FastMath.RAD_TO_DEG, 1) + ". Diff: "
					+ StringUtils.prettyNumber(anglesDiff.get(0) * FastMath.RAD_TO_DEG, 1) + ", "
					+ StringUtils.prettyNumber(anglesDiff.get(1) * FastMath.RAD_TO_DEG, 1) + ", "
					+ StringUtils.prettyNumber(anglesDiff.get(2) * FastMath.RAD_TO_DEG, 1))
		}

		fun q(pitch: Float, yaw: Float, roll: Float): Quaternion {
			return Quaternion().fromAngles(pitch * FastMath.DEG_TO_RAD, yaw * FastMath.DEG_TO_RAD, roll * FastMath.DEG_TO_RAD)
		}

		fun toDegs(q: Quaternion): String {
			val degs: FloatArray = FloatArray(3)
			q.toAngles(degs)
			return StringUtils.prettyNumber(
				degs.get(0) * FastMath.RAD_TO_DEG,
				0
			) + "," + StringUtils.prettyNumber(
				degs.get(1) * FastMath.RAD_TO_DEG,
				0
			) + "," + StringUtils.prettyNumber(degs.get(2) * FastMath.RAD_TO_DEG, 0)
		}
	}
}
