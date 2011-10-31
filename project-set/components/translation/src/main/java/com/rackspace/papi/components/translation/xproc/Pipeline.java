package com.rackspace.papi.components.translation.xproc;

import java.util.List;
import net.sf.saxon.s9api.XdmNode;

public interface Pipeline {

   public List<XdmNode> getResultPort(String name);
   public void run(List<PipelineInput> inputs) throws PipelineException;
   
}
