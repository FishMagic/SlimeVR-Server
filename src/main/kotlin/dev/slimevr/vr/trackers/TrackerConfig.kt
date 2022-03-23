package dev.slimevr.vr.trackers

import com.jme3.math.Quaternion
import io.eiren.yaml.YamlNode

class TrackerConfig {
	val trackerName: String?
	var designation: String?
	var description: String?
	var hide = false
	var adjustment: Quaternion? = null
	var mountingRotation: String? = null

	constructor(tracker: Tracker) {
		trackerName = tracker.name
		description = tracker.descriptiveName
		designation = if (tracker.bodyPosition != null) tracker.bodyPosition!!.designation else null
	}

	constructor(node: YamlNode) {
		trackerName = node.getString("name")
		description = node.getString("description")
		designation = node.getString("designation")
		hide = node.getBoolean("hide", false)
		mountingRotation = node.getString("rotation")
		val adjNode = node.getNode("adjustment")
		if (adjNode != null) {
			adjustment = Quaternion(
				adjNode.getFloat("x", 0f),
				adjNode.getFloat("y", 0f),
				adjNode.getFloat("z", 0f),
				adjNode.getFloat("w", 0f)
			)
		}
	}

	fun saveConfig(configNode: YamlNode) {
		configNode.setProperty("name", trackerName)
		if (designation != null) configNode.setProperty(
			"designation",
			designation
		) else configNode.removeProperty("designation")
		if (hide) configNode.setProperty("hide", hide) else configNode.removeProperty("hide")
		if (adjustment != null) {
			configNode.setProperty("adj.x", adjustment!!.x)
			configNode.setProperty("adj.y", adjustment!!.y)
			configNode.setProperty("adj.z", adjustment!!.z)
			configNode.setProperty("adj.w", adjustment!!.w)
		} else {
			configNode.removeProperty("adj")
		}
		if (mountingRotation != null) {
			configNode.setProperty("rotation", mountingRotation)
		} else {
			configNode.removeProperty("rotation")
		}
		if (description != null) {
			configNode.setProperty("description", description)
		} else {
			configNode.removeProperty("description")
		}
	}
}
