package com.eliteseriespay.desktop;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SingleInstanceLockTest {

    @TempDir
    Path tempDir;

    @Test
    void tryAcquire_succeedsForFirstLock() throws IOException {
        try (SingleInstanceLock lock = SingleInstanceLock.forDataDirectory(tempDir)) {
            assertThat(lock.tryAcquire()).isTrue();
            assertThat(Files.exists(lock.getLockFile())).isTrue();
        }
    }

    @Test
    void tryAcquire_failsForSecondLockOnSameFile() throws IOException {
        Path lockFile = tempDir.resolve(SingleInstanceLock.LOCK_FILE_NAME);

        try (SingleInstanceLock firstLock = new SingleInstanceLock(lockFile);
                SingleInstanceLock secondLock = new SingleInstanceLock(lockFile)) {
            assertThat(firstLock.tryAcquire()).isTrue();
            assertThat(secondLock.tryAcquire()).isFalse();
        }
    }

    @Test
    void tryAcquire_allowsIndependentLocksForDifferentFiles() throws IOException {
        Path firstLockFile = tempDir.resolve("first.lock");
        Path secondLockFile = tempDir.resolve("second.lock");

        try (SingleInstanceLock firstLock = new SingleInstanceLock(firstLockFile);
                SingleInstanceLock secondLock = new SingleInstanceLock(secondLockFile)) {
            assertThat(firstLock.tryAcquire()).isTrue();
            assertThat(secondLock.tryAcquire()).isTrue();
        }
    }

    @Test
    void close_allowsAnotherProcessToAcquireLock() throws IOException {
        Path lockFile = tempDir.resolve(SingleInstanceLock.LOCK_FILE_NAME);

        SingleInstanceLock firstLock = new SingleInstanceLock(lockFile);
        assertThat(firstLock.tryAcquire()).isTrue();
        firstLock.close();

        try (SingleInstanceLock secondLock = new SingleInstanceLock(lockFile)) {
            assertThat(secondLock.tryAcquire()).isTrue();
        }
    }
}
