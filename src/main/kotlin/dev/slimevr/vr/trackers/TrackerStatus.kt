package dev.slimevr.vr.trackers

enum class TrackerStatus(val id: Int, val sendData: Boolean) {
	DISCONNECTED(0, false), OK(1, true), BUSY(2, true), ERROR(3, false), OCCLUDED(4, false);

	companion object {
		private val byId = arrayOfNulls<TrackerStatus>(5)
		fun getById(id: Int): TrackerStatus? {
			return if (id < 0 || id >= byId.size) null else byId[id]
		}

		init {
			for (st in values()) byId[st.id] = st
		}
	}
}
