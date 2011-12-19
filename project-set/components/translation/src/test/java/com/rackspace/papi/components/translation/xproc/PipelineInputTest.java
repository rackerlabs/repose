package com.rackspace.papi.components.translation.xproc;

import org.junit.*;
import static org.junit.Assert.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

@RunWith(Enclosed.class)
public class PipelineInputTest {

   public static class WhenConstructingInputs {

      @Before
      public void setUp() {
      }

      @Test
      public void shouldGetParameterInput() {
         String name = "Name";
         String source = "Source";
         PipelineInput result = PipelineInput.parameter(name, source);
         PipelineInputType expected = PipelineInputType.PARAMETER;
         PipelineInputType actual = result.getType();
         assertEquals("Should get a parameter input", expected, actual);

      }

      @Test
      public void shouldGetPortInput() {
         String name = "Name";
         String source = "Source";
         PipelineInput result = PipelineInput.port(name, source);
         PipelineInputType expected = PipelineInputType.PORT;
         PipelineInputType actual = result.getType();
         assertEquals("Should get a port input", expected, actual);

      }

      @Test
      public void shouldGetOptionInput() {
         String name = "Name";
         String source = "Source";
         PipelineInput result = PipelineInput.option(name, source);
         PipelineInputType expected = PipelineInputType.OPTION;
         PipelineInputType actual = result.getType();
         assertEquals("Should get a option input", expected, actual);

      }

      @Test
      public void shouldGetParameterInputSource() {
         String name = "Name";
         String source = "Source";
         PipelineInput<String> result = PipelineInput.parameter(name, source);
         String actual = result.getSource();
         assertEquals("Should get a parameter input source", source, actual);

      }

   }

}
