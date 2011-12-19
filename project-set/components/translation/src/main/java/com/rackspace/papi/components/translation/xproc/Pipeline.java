package com.rackspace.papi.components.translation.xproc;

import java.util.List;
import javax.xml.transform.Source;

public interface Pipeline {

   public List<Source> getResultPort(String name);
   public void run(List<PipelineInput> inputs) throws PipelineException;
   public void run(PipelineInput... inputs);
   public void reset();
   
}
