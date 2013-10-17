package com.rackspace.papi.components.translation.postprocessor;

import javax.xml.transform.Source;
import java.io.InputStream;

public interface InputStreamPostProcessor {

   InputStream process(Source node) throws PostProcessorException;
   
}
