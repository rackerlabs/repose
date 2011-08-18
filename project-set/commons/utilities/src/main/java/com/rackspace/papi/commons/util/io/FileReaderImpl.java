/*
 *  Copyright 2010 Rackspace.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package com.rackspace.papi.commons.util.io;

import java.io.*;

public class FileReaderImpl extends FileReader {
    private final File file;

    public FileReaderImpl(File file) {
        this.file = file;
    }

    @Override
    protected void checkPreconditions() throws IOException {
        if (!file.exists() || !file.canRead()) {
            throw new FileNotFoundException("File: " + file.getAbsolutePath() + " does not exist or is not readable.");
        }
    }

    @Override
    protected BufferedReader getReader() throws FileNotFoundException {
        return new BufferedReader(new java.io.FileReader(file));
    }
}
