package com.rackspace.papi.httpx.processor;

import java.io.InputStream;

public interface InputStreamProcessor {

   InputStream process(InputStream sourceStream) throws PreProcessorException;
   
}
