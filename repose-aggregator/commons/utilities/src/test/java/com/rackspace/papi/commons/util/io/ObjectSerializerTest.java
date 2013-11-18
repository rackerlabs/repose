package com.rackspace.papi.commons.util.io;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.Serializable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(Enclosed.class)
public class ObjectSerializerTest {

   @Ignore
   public static class MySerializableObject implements Serializable {
      private final String field1;
      private final String field2;

      public MySerializableObject(String field1, String field2) {
         this.field1 = field1;
         this.field2 = field2;
      }
      
      @Override
      public boolean equals(Object other) {
         boolean result = false;
         
         if (other != null && other instanceof MySerializableObject) {
            MySerializableObject o = (MySerializableObject)other;
            
            result = 
                    (field1 == null && o.field1 == null || 
                     field1.equals(o.field1)) &&
                    (field2 == null && o.field2 == null || 
                     field2.equals(o.field2));
         }
         
         return result;
      }
   }
   
   public static class WhenSerializingObjects {
      private ObjectSerializer serializer;
      private MySerializableObject target;

      @Before
      public void setUp() {
         
         serializer = ObjectSerializer.instance();
         target = new MySerializableObject("x123", "y123");
      }
      
      @Test
      public void shouldSerializeObject() throws IOException {
         byte[] serialized = serializer.writeObject(target);
         assertNotNull(serialized);
      }

      @Test
      public void shouldReadSerializeObject() throws IOException, ClassNotFoundException {
         byte[] serialized = serializer.writeObject(target);
         assertNotNull(serialized);
         Serializable actual = serializer.readObject(serialized);
         assertNotNull(actual);
         assertEquals(target, actual);
      }
   }
}
