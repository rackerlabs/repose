/*
 *
 */
package com.rackspace.papi.components.translation.xproc;

import com.rackspace.papi.components.translation.xproc.Pipeline;

/**
 *
 * @author Dan Daley
 */
public interface PipelineBuilder {

   Pipeline build(String pipelineUri);
   
}
