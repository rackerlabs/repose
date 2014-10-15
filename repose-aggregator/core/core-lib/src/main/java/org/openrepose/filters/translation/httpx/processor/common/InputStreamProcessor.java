package org.openrepose.filters.translation.httpx.processor.common;

import java.io.InputStream;

public interface InputStreamProcessor {

   InputStream process(InputStream sourceStream) throws PreProcessorException;
   
}
