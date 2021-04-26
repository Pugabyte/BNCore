package me.pugabyte.nexus.utils;

import me.pugabyte.nexus.Nexus;

public class TimeUtils extends eden.utils.TimeUtils {

	public static class Timer {
		private static final int IGNORE = 2000;

		public Timer(String id, Runnable runnable) {
			long startTime = System.currentTimeMillis();

			runnable.run();

			long duration = System.currentTimeMillis() - startTime;
			if (duration >= 1)
				if (Nexus.isDebug() || duration > IGNORE)
					Nexus.log("[Timer] " + id + " took " + duration + "ms");
		}
	}

}
