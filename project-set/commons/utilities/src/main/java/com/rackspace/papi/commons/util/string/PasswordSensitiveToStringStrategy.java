package com.rackspace.papi.commons.util.string;

import org.jvnet.jaxb2_commons.lang.JAXBToStringStrategy;
import org.jvnet.jaxb2_commons.lang.ToStringStrategy;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;

/**
 * @author fran
 */
public class PasswordSensitiveToStringStrategy extends JAXBToStringStrategy implements ToStringStrategy {

   private static final String PASSWORD_FIELD_NAME = "password";

   @Override
   public StringBuilder appendField(ObjectLocator objectLocator, Object o, String s, StringBuilder stringBuilder, Object o1) {
      if (PASSWORD_FIELD_NAME.equalsIgnoreCase(s)) {
         return super.appendField(objectLocator, o, PASSWORD_FIELD_NAME, stringBuilder, "*******");
      } else {
         return super.appendField(objectLocator, o, s, stringBuilder, o1);
      }
   }
}
