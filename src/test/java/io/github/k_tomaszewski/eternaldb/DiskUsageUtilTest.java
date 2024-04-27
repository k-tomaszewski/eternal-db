package io.github.k_tomaszewski.eternaldb;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DiskUsageUtilTest {

    @Test
    void shouldGetDiskUsageOfDirectory() {
        // given
        final long startMillis = System.currentTimeMillis();

        // when
        final float diskUsageMB = DiskUsageUtil.getDiskUsageMB(".");

        final long endMillis = System.currentTimeMillis();
        System.out.format("Current directory disk usage: %f MB. Executed in %d ms.", diskUsageMB,
                endMillis - startMillis);

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
        System.out.format("pom.xml disk usage: %f MB. Executed in %d ms.", diskUsageMB,
                endMillis - startMillis);

        // then
        Assertions.assertTrue(diskUsageMB > 0.003f);
    }
}
