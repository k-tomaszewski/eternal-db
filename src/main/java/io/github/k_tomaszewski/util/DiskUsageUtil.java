package io.github.k_tomaszewski.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiskUsageUtil {

	private static final float KILOBYTES_IN_MEGABYTE = 1024.0f;
	private static final Logger LOG = LoggerFactory.getLogger(DiskUsageUtil.class);

	/**
	 * Disk usage for given directory in megabytes given as a `float` number.
	 */
	public static float getDiskUsageMB(String dir) {
		return getDiskUsageKB(dir) / KILOBYTES_IN_MEGABYTE;
	}

	/**
	 * Disk usage for given directory in kilobytes. This calls `du` command (coreutils). See:
	 * https://www.gnu.org/software/coreutils/manual/html_node/du-invocation.html
	 */
	public static long getDiskUsageKB(String path) {
		try {
			Process process = Runtime.getRuntime().exec(new String[]{"du", "-sk", path});		// size in KB
			final int exitValue = process.waitFor();
			if (exitValue == 0) {
				try (var processStdOut = process.inputReader()) {
					return processStdOut.lines()
							.filter(line -> line.endsWith(path))
							.mapToLong(DiskUsageUtil::extractNumber)
							.findFirst()
							.orElseThrow();
				}
			}
			throw new ProcessFailureException(exitValue, process.errorReader());
		} catch (Exception e) {
			throw new RuntimeException("Getting disk usage of '%s' failed.".formatted(path), e);
		}
	}

	/**
	 * Information about a file system that holds given file/directory. This calls `df` command (coreutils). See:
	 * https://www.gnu.org/software/coreutils/manual/html_node/df-invocation.html <br/>
	 * NOTE: There are lot faster alternatives in JDK for some use cases:
	 * - to get free disk space on filesystem with the given file: {@link java.io.File#getUsableSpace()}
	 * - to get file system type: {@link java.nio.file.FileStore#type()}
	 * <br/>
	 * HINT: In case of getting free disk space this still may be better in case of very large disk spaces. See:
	 * https://bugs.openjdk.org/browse/JDK-8233426
	 */
	public static FileSystemInfo getFileSystemInfo(String path) {
		try {
			Process process = Runtime.getRuntime().exec(new String[]{"df", "-kPT", path});		// size in KB
			final int exitValue = process.waitFor();
			if (exitValue == 0) {
				try (var processStdOut = process.inputReader()) {
					return processStdOut.lines()
							.skip(1)
							.findFirst()
							.map(DiskUsageUtil::toFileSystemInfo)
							.orElseThrow();
				}
			}
			throw new ProcessFailureException(exitValue, process.errorReader());
		} catch (Exception e) {
			LOG.warn("Cannot obtain file system info for '{}'", path, e);
			return null;
		}
	}
	
	private static long extractNumber(String duOutput) {
		int tabPos = duOutput.indexOf('\t');
		if (tabPos > 0) {
			return Long.parseLong(duOutput.substring(0, tabPos));
		}
		throw new RuntimeException("Unexpected output from 'du' command: " + duOutput);
	}

	private static FileSystemInfo toFileSystemInfo(String dfOutputLine) {
		String[] columns = dfOutputLine.split("\\s+");
		return new FileSystemInfo(columns[0], columns[1], Long.parseLong(columns[4]) / KILOBYTES_IN_MEGABYTE);
	}
}
