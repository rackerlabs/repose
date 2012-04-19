package com.rackspace.papi.components.translation.xproc;

import java.util.List;
import javax.xml.transform.Source;

public interface Pipeline {

   List<Source> getResultPort(String name);

   void run(List<PipelineInput> inputs) throws PipelineException;

   void run(PipelineInput... inputs);

   void reset();
}
