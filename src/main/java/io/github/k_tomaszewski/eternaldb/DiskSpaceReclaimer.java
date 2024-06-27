package io.github.k_tomaszewski.eternaldb;

import io.github.k_tomaszewski.util.DiskUsageUtil;
import io.github.k_tomaszewski.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

public class DiskSpaceReclaimer implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(DiskSpaceReclaimer.class);
    private static final BiPredicate<Path, BasicFileAttributes> ANY_PATH_PREDICATE = (x, y) -> true;

    private final Path dataDir;
    private final double spaceToReclaimMB;
    private final FileNamingStrategy fileNaming;
    private final AtomicBoolean diskSpaceReclaiming;
    private final DoubleAdder diskUsageActual;

    public DiskSpaceReclaimer(Path dataDir, double spaceToReclaimMB, FileNamingStrategy fileNaming, AtomicBoolean diskSpaceReclaiming,
            DoubleAdder diskUsageActual) {
        this.dataDir = dataDir;
        this.spaceToReclaimMB = spaceToReclaimMB;
        this.fileNaming = fileNaming;
        this.diskSpaceReclaiming = diskSpaceReclaiming;
        this.diskUsageActual = diskUsageActual;
    }

    @Override
    public void run() {
        LOG.info("Disk space reclaiming started with target to free {} MB...", spaceToReclaimMB);
        double spaceReclaimedMB = 0.0;
        try (Stream<Path> pathStream = Files.find(dataDir, fileNaming.maxDirectoryDepth(), ANY_PATH_PREDICATE)) {
            var iterator = pathStream.sorted().iterator();

            while (spaceReclaimedMB < spaceToReclaimMB && iterator.hasNext()) {
                Path path = iterator.next();
                if (Files.isDirectory(path)) {
                    if (FileUtils.isEmptyDir(path)) {
                        spaceReclaimedMB += remove(path);
                    }
                } else {
                    spaceReclaimedMB += remove(path);
                }
            }

            LOG.info("Disk space reclaiming started completed. {} MB reclaimed.", spaceReclaimedMB);
            diskUsageActual.add(-spaceReclaimedMB);
        } catch (IOException e) {
            LOG.warn("Disk space reclaiming failure. {} MB reclaimed.", spaceReclaimedMB, e);
        } finally {
            diskSpaceReclaiming.set(false);
        }
    }

    static double remove(Path path) {
        double itemSpaceMB = 0.0;
        try {
            itemSpaceMB = DiskUsageUtil.getDiskUsageMB(path.toString());
            Files.deleteIfExists(path);
            return itemSpaceMB;
        } catch (Exception e) {
            if (itemSpaceMB > 0.0 && !FileUtils.exists(path, true)) {
                return itemSpaceMB;
            }
            LOG.warn("Cannot remove [{}] to reclaim disk space.", path, e);
            return 0.0;
        }
    }
}
