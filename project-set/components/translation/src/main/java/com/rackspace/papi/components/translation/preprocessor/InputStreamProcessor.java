package com.rackspace.papi.components.translation.preprocessor;

import java.io.InputStream;

public interface InputStreamProcessor {

   InputStream process(InputStream sourceStream) throws PreProcessorException;
   
}
