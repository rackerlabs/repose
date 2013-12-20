package com.rackspace.papi.commons.util.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

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
