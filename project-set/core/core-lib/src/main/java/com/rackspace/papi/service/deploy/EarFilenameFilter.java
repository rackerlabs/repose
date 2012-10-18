package com.rackspace.papi.service.deploy;

import java.io.File;
import java.io.FilenameFilter;

public final class EarFilenameFilter implements FilenameFilter {

    private static final EarFilenameFilter INSTANCE = new EarFilenameFilter();
    private static final int EAR_EXTENSION_LENGTH = 4;

    public static FilenameFilter getInstance() {
        return INSTANCE;
    }

    private EarFilenameFilter() {
    }

    @Override
    public boolean accept(File dir, String name) {
        return name.length() > EAR_EXTENSION_LENGTH && name.substring(name.length() - EAR_EXTENSION_LENGTH).equalsIgnoreCase(".ear");
    }
}
