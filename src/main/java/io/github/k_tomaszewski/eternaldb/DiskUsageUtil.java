package io.github.k_tomaszewski.eternaldb;

public class DiskUsageUtil {

	/**
	 * Disk usage for given directory in megabytes given as a `float` number.
	 */
	public static float getDiskUsageMB(String dir) {
		return getDiskUsageKB(dir) / 1024.0f;
	}

	/**
	 * Disk usage for given directory in kilobytes. This calls `du` command (coreutils). See:
	 * https://www.gnu.org/software/coreutils/manual/html_node/du-invocation.html
	 */
	public static long getDiskUsageKB(String path) {
		try {
			Process process = Runtime.getRuntime().exec(new String[]{"du", "-sk", path});		// size in KB
			process.waitFor();
			try (var processStdOut = process.inputReader()) {
				return processStdOut.lines()
						.filter(line -> line.endsWith(path))
						.mapToLong(DiskUsageUtil::extractNumber)
						.findFirst()
						.orElseThrow();
			}
		} catch (Exception e) {
			throw new RuntimeException("Getting disk usage of '%s' failed.".formatted(path), e);
		}
	}
	
	private static long extractNumber(String duOutput) {
		int tabPos = duOutput.indexOf('\t');
		if (tabPos > 0) {
			return Long.parseLong(duOutput.substring(0, tabPos));
		}
		throw new RuntimeException("Unexpected output from 'du' command: " + duOutput);
	}
}
