package com.rackspace.papi.httpx.processor.json.elements;

import com.rackspace.papi.httpx.processor.common.Element;
import org.codehaus.jackson.JsonToken;

// TODO:Review - Dangerous try-catch statements
public enum ElementFactory {

   // Scalar Elements
   VALUE_TRUE(JsonToken.VALUE_TRUE.name(), "boolean"),
   VALUE_NULL(JsonToken.VALUE_NULL.name(), "null", NullElement.class),
   VALUE_FALSE(JsonToken.VALUE_FALSE.name(), "boolean"),
   VALUE_STRING(JsonToken.VALUE_STRING.name(), "string"),
   VALUE_NUMBER_INT(JsonToken.VALUE_NUMBER_INT.name(), "number"),
   VALUE_NUMBER_FLOAT(JsonToken.VALUE_NUMBER_FLOAT.name(), "number"),
   
   // Non-scalar Elements
   START_OBJECT(JsonToken.START_OBJECT.name(), "object", StartElement.class),
   START_ARRAY(JsonToken.START_ARRAY.name(), "array", StartElement.class),
   END_OBJECT(JsonToken.END_OBJECT.name(), "array", EndElement.class),
   END_ARRAY(JsonToken.END_ARRAY.name(), "array", EndElement.class);
   
   private final String tokenName;
   private final String elementName;
   private final Class elementClass;
   
   ElementFactory(String tokenName, String elementName, Class elementClass) {
      this.tokenName = tokenName;
      this.elementName = elementName;
      this.elementClass = elementClass;
   }
   
   ElementFactory(String tokenName, String elementName) {
      this.tokenName = tokenName;
      this.elementName = elementName;
      this.elementClass = null;
   }
   
   public static <T> Element getElement(String tokenName, String name) {
      Element result = null;
      for (ElementFactory element: values()) {
         if (element.tokenName.equals(tokenName)) {
            
            if (element.elementClass != null) {
               try {
                  result = (Element) element.elementClass.getConstructors()[0].newInstance(element.elementName, name);
               } catch (Exception ex) {
                  result = null;
               }
            }
            break;
         }
      }
      return result;
   }
   
   public static <T> Element getScalarElement(String tokenName, String name, T value) {
      Element result = null;
      for (ElementFactory element: values()) {
         if (element.tokenName.equals(tokenName)) {
               if (element.elementClass != null) {
                  try {
                     result = (Element) element.elementClass.getConstructors()[0].newInstance(element.elementName, name, value);
                  } catch (Exception ex) {
                     result = null;
                  }
               } else {
                  result = new ScalarElement<T>(element.elementName, name, value);
               }
               break;
         }
      }
      return result;
   }
   
}
