package io.github.k_tomaszewski.eternaldb;

import java.nio.file.Path;

public class DiskSpaceReclaimer {

    private final Path dataDir;
    private final double minDiskSpace;

    public DiskSpaceReclaimer(Path dataDir, double minDiskSpace) {
        this.dataDir = dataDir;
        this.minDiskSpace = minDiskSpace;
    }

    void start() {
        // TODO
    }
}
