package com.rackspace.papi.commons.util.plugin.archive.jar;

import com.rackspace.papi.commons.util.StringUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: joshualockwood
 * Date: 7/11/11
 * Time: 5:33 PM
 *
 * This class creates the directory into which Power API artifacts will be deployed (target directory).  First the
 * class checks to ensure the root deployment directory exists, is writable, and executable.  Next the class creates
 * the deployment directory inside the root directory.  Finally, the class sets the permissions on the deployment
 * directory so that only the user can read, write, and execute against the deployment directory (i.e. chmod 700).
 */
public class DirectoryHelper {
    private static final Logger LOG = LoggerFactory.getLogger(DirectoryHelper.class);

    private final File rootDirectory;
    private final String directoryName;

    public DirectoryHelper(File rootDirectory, String directoryName) {
        this.rootDirectory = rootDirectory;
        this.directoryName = formatPrefix(directoryName);
    }

    public File createTargetDirectory() {
        final File targetDir = new File(rootDirectory, directoryName);
                        
        if (!targetDir.mkdirs()) {
            LOG.warn("Failed to create directory [" + getRootDirectory() + ", " + getDirectoryName() + "]");
        }

        targetDir.setReadable(false, false);        
        targetDir.setReadable(true, true);

        targetDir.setExecutable(false, false);
        targetDir.setExecutable(true, true);

        return targetDir;
    }

    private static String formatPrefix(String prefix) {
        return StringUtilities.isBlank(prefix) ? "" : prefix;
    }

    public File getRootDirectory() {
        return rootDirectory;
    }

    public String getDirectoryName() {
        return directoryName;
    }
}
