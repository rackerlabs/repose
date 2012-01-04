package com.rackspace.papi.httpx.processor.common;

import java.io.InputStream;

public interface InputStreamProcessor {

   InputStream process(InputStream sourceStream) throws PreProcessorException;
   
}
