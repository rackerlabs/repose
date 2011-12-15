package com.rackspace.papi.components.translation.xproc;

import javax.xml.transform.URIResolver;

public interface PipelineBuilder {

   Pipeline build(String pipelineUri);
   public Pipeline build(String pipelineUri, URIResolver... resolvers);
   
}
