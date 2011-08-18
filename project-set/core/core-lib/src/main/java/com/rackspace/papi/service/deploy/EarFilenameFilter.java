package com.rackspace.papi.service.deploy;

import java.io.File;
import java.io.FilenameFilter;

public final class EarFilenameFilter implements FilenameFilter {

    private static final EarFilenameFilter INSTANCE = new EarFilenameFilter();

    public static FilenameFilter getInstance() {
        return INSTANCE;
    }

    private EarFilenameFilter() {
    }

    @Override
    public boolean accept(File dir, String name) {
        return name.length() > 4 && name.substring(name.length() - 4).equalsIgnoreCase(".ear");
    }
}
