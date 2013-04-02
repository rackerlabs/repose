package com.rackspace.papi.commons.util.transform.json;

import com.rackspace.papi.commons.util.io.charset.CharacterSets;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class JacksonJaxbTransform {
   private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(JacksonJaxbTransform.class);
   private ObjectMapper mapper;
   
   public JacksonJaxbTransform() {
      mapper = new ObjectMapper();
      
      AnnotationIntrospector introspector = new JaxbAnnotationIntrospector();
      mapper.setDeserializationConfig(mapper.getDeserializationConfig().withAnnotationIntrospector(introspector));
      mapper.setSerializationConfig(mapper.getSerializationConfig().withAnnotationIntrospector(introspector));
   }
   
   public JacksonJaxbTransform(ObjectMapper mapper) {
      this.mapper = mapper;
   }

   public <T> T deserialize(String input, Class<T> target) {
      return deserialize(new ByteArrayInputStream(input.getBytes(CharacterSets.UTF_8)), target);
   }
   
   public <T> T deserialize(InputStream input, Class<T> target) {
      T result = null;
      try {
         result = mapper.readValue(input, target);
      } catch(IOException ex) {
         LOG.warn("Unable to deserialize stream", ex);
      }
      
      return result;
   }
}
