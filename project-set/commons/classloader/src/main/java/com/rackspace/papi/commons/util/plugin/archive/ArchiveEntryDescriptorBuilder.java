package com.rackspace.papi.commons.util.plugin.archive;

import com.rackspace.papi.commons.util.StringUtilities;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public final class ArchiveEntryDescriptorBuilder {
    private static final Pattern JAR_ENTRY_NAMING_PATTERN = Pattern.compile("(.+?/)?([^/]+?)?(\\.([^.]*$)|$)");
    private static final int RAW_PACKAGE_NAME = 1, SIMPLE_NAME = 2, EXTENSION = 4;

    private ArchiveEntryDescriptorBuilder() {
        
    }
    
    public static ArchiveEntryDescriptor build(String archiveName, String entryName) {
        final Matcher nameMatcher = JAR_ENTRY_NAMING_PATTERN.matcher(entryName);
        ArchiveEntryDescriptor archiveEntryDescriptor = null;

        if (nameMatcher.matches()) {
            final String simpleName = nameMatcher.group(SIMPLE_NAME);
            final String rawPackageName = nameMatcher.group(RAW_PACKAGE_NAME);
            final String extension = nameMatcher.group(EXTENSION);

            archiveEntryDescriptor = new ArchiveEntryDescriptor(archiveName, StringUtilities.trim(entryName, "/"), rawPackageName, simpleName, extension);
        }

        return archiveEntryDescriptor;
    }
}
