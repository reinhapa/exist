/*
Copyright (c) 2015, Adam Retter
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of Adam Retter Consulting nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL Adam Retter BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.exist.util.io;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.exist.util.FileUtils;

/**
 * Temporary File Manager.
 * <p>
 * Provides temporary files for use by eXist-db and deals with cleaning them
 * up.
 * <p>
 * Previously when returning a temporary file if it could not be deleted
 * (which often occurred on Microsoft Windows) we would add it to a queue
 * for reuse the next time a temporary file was required.
 * <p>
 * On Microsoft Windows platforms this was shown to be unreliable. If the
 * temporary file had been Memory Mapped, there would be a lingering open file
 * handle which would only be closed when the GC reclaims the ByteBuffer
 * objects resulting from the mapping. This exhibited two problems:
 * 1. The previously memory mapped file could only be reused for further
 * memory mapped I/O. Any traditional I/O or file system operations
 * (e.g. copy, move, etc.) would result in a
 * java.nio.file.FileSystemException.
 * 2. Keeping the previously memory mapped file in a queue, may result in
 * strong indirect references to the ByteBuffer objects meaning that they
 * will never be subject to GC, and therefore the file handles would never
 * be released.
 * As such, we now never recycle temporary file objects. Instead we rely on the
 * GC to eventually close the file handles of any previously memory mapped files
 * and the Operating System to manage it's temporary file space.
 * <p>
 * Relevant articles on the above described problems are:
 * 1.https://bugs.java.com/view_bug.do?bug_id=4715154
 * 2. https://bugs.openjdk.java.net/browse/JDK-8028683
 * 3. https://bugs.java.com/view_bug.do?bug_id=4724038
 *
 * @author Adam Retter <adam.retter@googlemail.com>
 * @version 2.0
 */
public class TemporaryFileManager {
    private static final Log LOG = LogFactory.getLog(TemporaryFileManager.class);
    private static final String FOLDER_PREFIX = "exist-db-temp-file-manager";
    private static final String FILE_PREFIX = "exist-db-temp";
    private static final String LOCK_FILENAME = FOLDER_PREFIX + ".lck";

    private static final TemporaryFileManager instance = new TemporaryFileManager();

    private final Path tmpFolder;
    private final FileChannel lockChannel;
    private final Set<Path> active;
    private final ScheduledExecutorService deleteExecutor;

    public static TemporaryFileManager getInstance() {
        return instance;
    }

    private TemporaryFileManager() {
        /*
        Add hook to JVM to delete the file on exit
        unfortunately this does not always work on all (e.g. Windows) platforms
        will be recovered on restart by cleanupOldTempFolders
         */
        Runtime.getRuntime().addShutdownHook(new Thread(this::cleanUp, "TemporaryFileManagerCleanup"));

        cleanupOldTempFolders();

        try {
            this.tmpFolder = Files.createTempDirectory(FOLDER_PREFIX + '-');
            this.lockChannel = FileChannel.open(tmpFolder.resolve(LOCK_FILENAME), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, StandardOpenOption.DELETE_ON_CLOSE);
            this.active = ConcurrentHashMap.newKeySet();
            this.deleteExecutor = Executors.newSingleThreadScheduledExecutor();
            lockChannel.lock();
        } catch (final IOException ioe) {
            throw new RuntimeException("Unable to create temporary folder", ioe);
        }

        LOG.info("Temporary folder is: " + tmpFolder);
    }

    public final Path getTemporaryFile() throws IOException {
        final Path tempFile = Files.createTempFile(tmpFolder, FILE_PREFIX + '-', ".tmp");
        /*
        add hook to JVM to delete the file on exit
        unfortunately this does not always work on all (e.g. Windows) platforms
         */
        active.add(tempFile);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Created temporary file: " + tempFile);
        }
        return tempFile;
    }

    public void returnTemporaryFile(final Path tempFile) {
        removeFile(tempFile, () -> active.remove(tempFile), this::retryDelete);
    }

    /**
     * Cleans up all open temporary files as well as the temporary root folder and releases the active lock
     */
    private void cleanUp() {
        if (tmpFolder != null) {
            for (Iterator<Path> tempFileIt = active.iterator(); tempFileIt.hasNext(); ) {
                removeFile(tempFileIt.next(), () -> tempFileIt.remove(), this::logError);
            }
        }
        if (lockChannel != null) {
            LOG.info("Release lock temp folder lock");
            try {
                // will release the lock on the lock file, and the lock file should be deleted
                lockChannel.close();
            } catch (IOException e) {
                LOG.error("Unable to release lock", e);
            }
        }
        LOG.info("Release temp folder " + tmpFolder);
        try {
            //try and remove our temporary folder
            Files.delete(tmpFolder);
        } catch (IOException e) {
            LOG.error("Unable to release lock", e);
        }
    }

    /**
     * Removes the given {@code tempFile} and call the given {@code postDeleteAction} after
     * the file has been deleted successfully or the file does no longer exist.
     *
     * @param tempFile         the temporary file to be deleted
     * @param postDeleteAction the action called upon successful deletion
     */
    private void removeFile(Path tempFile, Runnable postDeleteAction, BiConsumer<IOException, Path> failureConsumer) {
        try {
            if (Files.deleteIfExists(tempFile)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Deleted temporary file: " + tempFile);
                }
            }
            if (postDeleteAction != null) {
                postDeleteAction.run();
            }
        } catch (IOException ioe) {
            // this can often occur on Microsoft Windows (especially if the file was memory mapped!) :-/
            failureConsumer.accept(ioe, tempFile);
        }
    }

    private void logError(IOException ioe, Path tempFile) {
        LOG.warn("Unable to delete temporary file: " + tempFile + " due to: " + ioe.getMessage());
    }

    private void retryDelete(IOException ioe, Path tempFile) {
        LOG.warn("Unable to delete temporary file: " + tempFile + " due to: " + ioe.getMessage() + " - retry in 5 seconds");
        deleteExecutor.schedule(() -> removeFile(tempFile, null, this::retryDelete), 5, TimeUnit.SECONDS);
    }

    /**
     * Called at startup to attempt to cleanup
     * any left-over temporary folders
     * from the last time this was run
     */
    private void cleanupOldTempFolders() {
        final Path tmpDir = Paths.get(System.getProperty("java.io.tmpdir"));
        try (Stream<Path> pathStream = Files.list(tmpDir)) {
            pathStream.filter(path -> Files.isDirectory(path) && path.getFileName().toString().startsWith(FOLDER_PREFIX)).forEach(dir -> {
                final Path lockPath = dir.resolve(LOCK_FILENAME);
                try {
                    // check for lock first
                    if (Files.exists(lockPath)) {
                        try (FileChannel otherLockChannel = FileChannel.open(lockPath, StandardOpenOption.WRITE)) {
                            FileLock fileLock = otherLockChannel.tryLock();
                            if (fileLock == null) {
                                // locked... so we now have the lock
                                LOG.warn("Temporary folder " + dir + " still locked - skipping");
                                return;
                            }
                            fileLock.release();
                        }
                    }
                    // delete all files (including closed lock file) now
                    try (Stream<Path> tempPathStream = Files.list(dir)) {
                        tempPathStream.forEach(tempFile -> removeFile(tempFile, null, this::logError));
                    }
                    // delete temporary folder itself
                    Files.delete(dir);
                    LOG.info("Removed old temporary folder " + dir);
                } catch (IOException e) {
                    LOG.warn("Unable to delete old temporary folder " + dir, e);
                }
            });
        } catch (final IOException ioe) {
            LOG.warn("Unable to list temp folder " + tmpDir, ioe);
        }
    }
}
