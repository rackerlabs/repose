package com.rackspace.papi.components.translation.xproc;

import com.rackspace.papi.components.translation.util.InputStreamUriParameterResolver;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.Source;
import org.junit.*;
import static org.junit.Assert.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class AbstractPipelineTest {

   public static class WhenHandlingInputs {
      private AbstractPipelineImpl pipeline;
      private InputStream inputStream;
      private InputStreamUriParameterResolver uriResolver;
      
      @Before
      public void setUp() {
         inputStream = mock(InputStream.class);
         uriResolver = mock(InputStreamUriParameterResolver.class);

         pipeline = new AbstractPipelineImpl(uriResolver);
      }
      
      @Test
      public void shouldCallAddParameter() {
         String name = "Name";
         String source = "Source";
         PipelineInput param = PipelineInput.parameter(name, source);
         
         pipeline.handleInputs(param);
         
         assertEquals("Should have one parameter", 1, pipeline.params.size());
         
      }

      @Test
      public void shouldCallAddPort() {
         String name = "Name";
         String source = "Source";
         PipelineInput port = PipelineInput.port(name, source);
         
         pipeline.handleInputs(port);
         
         assertEquals("Should have one port", 1, pipeline.ports.size());
         
      }

      @Test
      public void shouldCallAddOption() {
         String name = "Name";
         String source = "Source";
         PipelineInput option = PipelineInput.option(name, source);
         
         pipeline.handleInputs(option);
         
         assertEquals("Should have one port", 1, pipeline.options.size());
         
      }
      
      @Test
      public void shouldCloseInputStreamParameters() throws IOException {
         String name = "someInputStream";
         PipelineInput param = PipelineInput.parameter(name, inputStream);
         PipelineInput stringParam = PipelineInput.parameter("otherParam", "some string");
         
         pipeline.clearParameters(param, stringParam);
         
         verify(inputStream).close();
         verify(uriResolver).removeStream(inputStream);
      }
              
   }

   /*
   @Test
   public void testHandleInputs() {
      System.out.println("handleInputs");
      List<PipelineInput> inputs = null;
      AbstractPipeline instance = null;
      instance.handleInputs(inputs);
      // TODO review the generated test code and remove the default call to fail.
      fail("The test case is a prototype.");
   }

   @Test
   public void testGetUriResolver() {
      System.out.println("getUriResolver");
      AbstractPipeline instance = null;
      InputStreamUriParameterResolver expResult = null;
      InputStreamUriParameterResolver result = instance.getUriResolver();
      assertEquals(expResult, result);
      // TODO review the generated test code and remove the default call to fail.
      fail("The test case is a prototype.");
   }

   @Test
   public void testClearParameter() {
      System.out.println("clearParameter");
      PipelineInput input = null;
      AbstractPipeline instance = null;
      instance.clearParameter(input);
      // TODO review the generated test code and remove the default call to fail.
      fail("The test case is a prototype.");
   }

   @Test
   public void testClearParameters() {
      System.out.println("clearParameters");
      List<PipelineInput> inputs = null;
      AbstractPipeline instance = null;
      instance.clearParameters(inputs);
      // TODO review the generated test code and remove the default call to fail.
      fail("The test case is a prototype.");
   }
   * 
   */

   @Ignore
   public static class AbstractPipelineImpl extends AbstractPipeline {
      private final ArrayList<PipelineInput> params;
      private final ArrayList<PipelineInput> ports;
      private final ArrayList<PipelineInput> options;
      

      public AbstractPipelineImpl() {
         this(null);
      }

      public AbstractPipelineImpl(InputStreamUriParameterResolver resolver) {
         super(resolver);
         params = new ArrayList<PipelineInput>();
         ports = new ArrayList<PipelineInput>();
         options = new ArrayList<PipelineInput>();
      }

      @Override
      public <T> void addParameter(PipelineInput<T> input) {
         params.add(input);
      }

      @Override
      public <T> void addPort(PipelineInput<T> input) {
         ports.add(input);
      }

      @Override
      public <T> void addOption(PipelineInput<T> input) {
         options.add(input);
      }
      
      @Override
      public void handleInputs(PipelineInput... inputs) {
         super.handleInputs(inputs);
      }

      @Override
      public void clearParameters(PipelineInput... inputs) {
         super.clearParameters(inputs);
      }

      @Override
      public List<Source> getResultPort(String name) {
         return new ArrayList<Source>();
      }

      @Override
      public void run(List<PipelineInput> inputs) throws PipelineException {
      }

      @Override
      public void reset() {
      }
   }
}
