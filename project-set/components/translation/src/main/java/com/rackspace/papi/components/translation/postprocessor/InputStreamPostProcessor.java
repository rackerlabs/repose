package com.rackspace.papi.components.translation.postprocessor;

import java.io.InputStream;
import javax.xml.transform.Source;

public interface InputStreamPostProcessor {

   InputStream process(Source node) throws PostProcessorException;
   
}
