package io.github.k_tomaszewski.eternaldb;

public class DiskUsageUtil {

	/**
	 * Disk usage for given directory in megabytes given as a `float` number.
	 */
	public static float getDiskUsageMB(String dir) {
		return getDiskUsageKB(dir) / 1024.0f;
	}

	/**
	 * Disk usage for given directory in kilobytes.
	 */
	public static long getDiskUsageKB(String dir) {
		try {
			Process process = Runtime.getRuntime().exec(new String[]{"du", "-sk", dir});		// size in KB
			process.waitFor();
			try (var processStdOut = process.inputReader()) {
				return processStdOut.lines()
						.filter(line -> line.endsWith(dir))
						.mapToLong(DiskUsageUtil::extractNumber)
						.findFirst()
						.orElseThrow();
			}
		} catch (Exception e) {
			throw new RuntimeException("Getting disk usage of '%s' failed.".formatted(dir), e);
		}
	}
	
	private static long extractNumber(String duOutput) {
		int mPos = duOutput.indexOf('\t');
		if (mPos > 0) {
			return Long.parseLong(duOutput.substring(0, mPos));
		}
		throw new RuntimeException("Unexpected output from 'du' command.");
	}
}
