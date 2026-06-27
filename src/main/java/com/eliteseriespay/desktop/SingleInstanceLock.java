package com.eliteseriespay.desktop;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SingleInstanceLock implements AutoCloseable {

    static final String LOCK_FILE_NAME = "eliteseriespay.instance.lock";

    private final Path lockFile;
    private RandomAccessFile lockRandomAccessFile;
    private FileChannel lockChannel;
    private FileLock fileLock;

    public SingleInstanceLock(Path lockFile) {
        this.lockFile = lockFile;
    }

    public static SingleInstanceLock forDataDirectory(Path dataDirectory) {
        return new SingleInstanceLock(dataDirectory.resolve(LOCK_FILE_NAME));
    }

    public Path getLockFile() {
        return lockFile;
    }

    public boolean tryAcquire() throws IOException {
        if (fileLock != null) {
            return true;
        }

        Path parentDirectory = lockFile.getParent();
        if (parentDirectory != null) {
            Files.createDirectories(parentDirectory);
        }

        lockRandomAccessFile = new RandomAccessFile(lockFile.toFile(), "rw");
        lockChannel = lockRandomAccessFile.getChannel();
        try {
            fileLock = lockChannel.tryLock();
        } catch (OverlappingFileLockException exception) {
            releaseResources();
            return false;
        }

        if (fileLock == null) {
            releaseResources();
            return false;
        }

        return true;
    }

    @Override
    public void close() throws IOException {
        if (fileLock != null) {
            fileLock.release();
            fileLock = null;
        }
        releaseResources();
    }

    private void releaseResources() throws IOException {
        if (lockChannel != null) {
            lockChannel.close();
            lockChannel = null;
        }
        if (lockRandomAccessFile != null) {
            lockRandomAccessFile.close();
            lockRandomAccessFile = null;
        }
    }
}
