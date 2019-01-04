/**
 * Created on 2019/01/03, 14:34:25.
 */
package com.shadowoflies.zip;

import com.shadowoflies.zip.config.UnzipConfig;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;

/**
 * @author Gavin Boshoff
 */
@Mojo(name = "unzip", defaultPhase = LifecyclePhase.INITIALIZE)
public class UnzipMojo extends AbstractMojo {

    /**
     * The configuration sets for archives to be extracted.
     */
    @Parameter(required = true)
    private List<UnzipConfig> unzipSets;

    /**
     * Allows for multi-threaded extraction when multiple configurations are
     * provided.
     */
    @Parameter(property = "unzipInParallel", defaultValue = "true")
    private boolean unzipInParallel;

    /**
     * Will print debug printouts to the info log, if enabled and debug
     * printouts are disabled.
     */
    @Parameter(property = "verbose", defaultValue = "false")
    private boolean verbose;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        validateConfiguration();

        try {
            if (unzipInParallel) {
                unzipSets.parallelStream().forEach(config -> execUnzip(config));
            } else {
                unzipSets.stream().forEach(config -> execUnzip(config));
            }
        } catch (Exception ex) {
            throw new MojoFailureException("Failed to complete the extraction of archive(s). Cause: " + ex.getMessage(), ex);
        }
    }

    private void validateConfiguration() throws MojoExecutionException {
        for (UnzipConfig config : unzipSets) {
            verifyValidSource(config);

            verifyValidDestination(config);
        }
    }

    private void verifyValidSource(UnzipConfig config) throws MojoExecutionException {
        if (Files.notExists(config.getSourceArchive().toPath())) {
            throw new MojoExecutionException("File : " + config.getSourceArchive() + " not found");
        }
    }

    private void verifyValidDestination(UnzipConfig config) throws MojoExecutionException {
        if (!config.getDestination().exists() && !config.getDestination().mkdirs()) {
            throw new MojoExecutionException("Destination does not exist and couldn't be created.");
        } else if (config.getDestination().isFile()) {
            throw new MojoExecutionException("Destination has to be a directory, but is instead a file. Destination : " + config.getDestination());
        }
    }

    private void cleanIfNeeded(UnzipConfig config) throws IOException {
        long startTime;

        if (config.isCleanDestination()) {
            startTime = System.currentTimeMillis();

            FileUtils.cleanDirectory(config.getDestination());

            getLog().info("Cleaned destination [" + config.getDestination()
                    + "] in [" + (System.currentTimeMillis() - startTime) + "]ms");
        }
    }

    private void execUnzip(UnzipConfig config) {
        FileSystem zipFileSystem = null;
        long startTime;

        startTime = System.currentTimeMillis();

        try {
            cleanIfNeeded(config);

            zipFileSystem = FileSystems.newFileSystem(config.getSourceArchive().toPath(), null);

            for (Path rootDir : zipFileSystem.getRootDirectories()) {
                copyFiles(rootDir, config.getDestination(), config);
            }
        } catch (IOException ioEx) {
            throw new RuntimeException("Error while extracting archive "
                    + config.getSourceArchive() + ". Cause: " + ioEx.getMessage(), ioEx);
        } finally {
            try {
                if (zipFileSystem != null) {
                    zipFileSystem.close();
                }
            } catch (IOException ex) {
                getLog().error("Cannot close zipfile-system", ex);
            }
            getLog().info("Completed extraction of [" + config.getSourceArchive() + "] in [" + (System.currentTimeMillis() - startTime) + "]ms");
        }
    }

    private void copyFiles(Path directory, File destinationDir, UnzipConfig config) throws IOException {
        DirectoryStream<Path> directoryStream;

        directoryStream = Files.newDirectoryStream(directory);
        for (Path sourcePath : directoryStream) {
            File destinationPath = new File(destinationDir, sourcePath.getFileName().toString());

            if (Files.isDirectory(sourcePath)) {
                printDebug("Creating directory: " + destinationPath);

                if (!destinationPath.exists() && !destinationPath.mkdir()) {
                    throw new IOException("Sub-directory : " + destinationPath + " cannot be created");
                }

                copyFiles(sourcePath, destinationPath, config);
            } else {
                printDebug("Copying file [" + sourcePath + "] - [" + destinationPath + "]");
                if (config.isReplaceExisting()) {
                    Files.copy(sourcePath, destinationPath.toPath(),
                            StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
                } else if (!destinationPath.exists()) {
                    Files.copy(sourcePath, destinationPath.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
                }
            }
        }
    }

    private void printDebug(String message) {
        if (getLog().isDebugEnabled()) {
            getLog().debug(message);
        } else if (verbose) {
            getLog().info(message);
        }
    }
}
