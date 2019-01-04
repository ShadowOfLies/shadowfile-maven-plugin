/**
 * Created on 2019/01/04, 07:18:15.
 */
package com.shadowoflies.zip.config;

import java.io.File;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * @author Gavin Boshoff
 */
public class UnzipConfig {

    /**
     * Whether or not the destination location should be cleaned before starting
     * the process.
     */
    @Parameter(defaultValue = "false")
    private boolean cleanDestination;

    /**
     * Whether or not existing files should be replaced. When {@code false} and
     * a file already exists, it is simply skipped.
     */
    @Parameter(defaultValue = "true")
    private boolean replaceExisting;

    /**
     * The location of the source archive that will be extracted.
     */
    @Parameter(required = true)
    private File sourceArchive;

    /**
     * The destination directory where the archive will be extracted.
     */
    @Parameter(required = true)
    private File destination;

    public UnzipConfig() {}

    public boolean isCleanDestination() {
        return cleanDestination;
    }

    public void setCleanDestination(boolean cleanDestination) {
        this.cleanDestination = cleanDestination;
    }

    public boolean isReplaceExisting() {
        return replaceExisting;
    }

    public void setReplaceExisting(boolean replaceExisting) {
        this.replaceExisting = replaceExisting;
    }

    public File getSourceArchive() {
        return sourceArchive;
    }

    public void setSourceArchive(File sourceArchive) {
        this.sourceArchive = sourceArchive;
    }

    public File getDestination() {
        return destination;
    }

    public void setDestination(File destination) {
        this.destination = destination;
    }
}
