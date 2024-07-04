package io.github.k_tomaszewski.eternaldb;

import io.github.k_tomaszewski.util.DiskUsageUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DiskUsageUtilTest {

    @Test
    void shouldGetDiskUsageOfDirectory() {
        // given
        final long startMillis = System.currentTimeMillis();

        // when
        final float diskUsageMB = DiskUsageUtil.getDiskUsageMB(".");

        final long endMillis = System.currentTimeMillis();
        System.out.format("Current directory disk usage: %f MB. Executed in %d ms.\n", diskUsageMB, endMillis - startMillis);

        // then
        Assertions.assertTrue(diskUsageMB > 0.01f);
    }

    @Test
    void shouldGetDiskUsageOfFile() {
        // given
        final long startMillis = System.currentTimeMillis();

        // when
        final float diskUsageMB = DiskUsageUtil.getDiskUsageMB("pom.xml");

        final long endMillis = System.currentTimeMillis();
        System.out.format("Disk usage of 'pom.xml' file: %f MB. Executed in %d ms.\n", diskUsageMB, endMillis - startMillis);

        // then
        Assertions.assertTrue(diskUsageMB > 0.003f);
    }

    @Test
    void shouldGetFileSystemInfo() throws IOException {
        // when
        long startMillis = System.currentTimeMillis();
        var fsInfo = DiskUsageUtil.getFileSystemInfo(".");
        long endMillis = System.currentTimeMillis();
        System.out.println(fsInfo + "; time (ms): " + (endMillis - startMillis));

        /* uncomment to compare speed of other means for getting available disk space
        startMillis = System.currentTimeMillis();
        var currentDir = new File(".");
        float usableSpaceMB = toMegabytes(currentDir.getUsableSpace());
        endMillis = System.currentTimeMillis();
        System.out.println("Usable space: " + usableSpaceMB + "; time (ms): " + (endMillis - startMillis));

        startMillis = System.currentTimeMillis();
        var currentFileStore = Files.getFileStore(Path.of("."));
        float usableSpaceMBv2 = toMegabytes(currentFileStore.getUsableSpace());
        endMillis = System.currentTimeMillis();
        System.out.println("Usable space: " + usableSpaceMBv2 + "; time (ms): " + (endMillis - startMillis));
        */

        // then
        Assertions.assertNotNull(fsInfo);
        Assertions.assertNotNull(fsInfo.volume());
        Assertions.assertNotNull(fsInfo.type());
        Assertions.assertTrue(fsInfo.freeSpaceMB() >= 0.0f);
    }

    private static float toMegabytes(long bytes) {
        return bytes / (1024.0f * 1024.f);
    }
}
