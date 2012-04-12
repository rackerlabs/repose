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

   @Override
   public StringBuilder appendStart(ObjectLocator objectLocator, Object o, StringBuilder stringBuilder) {
      return super.appendStart(objectLocator, o, stringBuilder);
   }

   @Override
   public StringBuilder appendEnd(ObjectLocator objectLocator, Object o, StringBuilder stringBuilder) {
      return super.appendEnd(objectLocator, o, stringBuilder);
   }

   @Override
   public StringBuilder append(ObjectLocator objectLocator, StringBuilder stringBuilder, boolean b) {
      return super.append(objectLocator, stringBuilder, b);    //To change body of overridden methods use File | Settings | File Templates.
   }

   @Override
   public StringBuilder append(ObjectLocator objectLocator, StringBuilder stringBuilder, byte b) {
      return super.append(objectLocator, stringBuilder, b);    //To change body of overridden methods use File | Settings | File Templates.
   }

   @Override
   public StringBuilder append(ObjectLocator objectLocator, StringBuilder stringBuilder, char c) {
      return super.append(objectLocator, stringBuilder, c);    //To change body of overridden methods use File | Settings | File Templates.
   }

   @Override
   public StringBuilder append(ObjectLocator objectLocator, StringBuilder stringBuilder, double v) {
      return super.append(objectLocator, stringBuilder, v);    //To change body of overridden methods use File | Settings | File Templates.
   }

   @Override
   public StringBuilder append(ObjectLocator objectLocator, StringBuilder stringBuilder, float v) {
      return super.append(objectLocator, stringBuilder, v);    //To change body of overridden methods use File | Settings | File Templates.

   }

   @Override
   public StringBuilder append(ObjectLocator objectLocator, StringBuilder stringBuilder, int i) {
      return super.append(objectLocator, stringBuilder, i);    //To change body of overridden methods use File | Settings | File Templates.

   }

   @Override
   public StringBuilder append(ObjectLocator objectLocator, StringBuilder stringBuilder, long l) {
      return super.append(objectLocator, stringBuilder, l);    //To change body of overridden methods use File | Settings | File Templates.

   }

   @Override
   public StringBuilder append(ObjectLocator objectLocator, StringBuilder stringBuilder, short i) {
      return super.append(objectLocator, stringBuilder, i);    //To change body of overridden methods use File | Settings | File Templates.

   }

   @Override
   public StringBuilder append(ObjectLocator objectLocator, StringBuilder stringBuilder, Object o) {
      return super.append(objectLocator, stringBuilder, o);    //To change body of overridden methods use File | Settings | File Templates.

   }

   @Override
   public StringBuilder append(ObjectLocator objectLocator, StringBuilder stringBuilder, boolean[] booleans) {
      return super.append(objectLocator, stringBuilder, booleans);    //To change body of overridden methods use File | Settings | File Templates.

   }

   @Override
   public StringBuilder append(ObjectLocator objectLocator, StringBuilder stringBuilder, byte[] bytes) {
      return super.append(objectLocator, stringBuilder, bytes);    //To change body of overridden methods use File | Settings | File Templates.

   }

   @Override
   public StringBuilder append(ObjectLocator objectLocator, StringBuilder stringBuilder, char[] chars) {
      return super.append(objectLocator, stringBuilder, chars);    //To change body of overridden methods use File | Settings | File Templates.

   }

   @Override
   public StringBuilder append(ObjectLocator objectLocator, StringBuilder stringBuilder, double[] doubles) {
      return super.append(objectLocator, stringBuilder, doubles);    //To change body of overridden methods use File | Settings | File Templates.

   }

   @Override
   public StringBuilder append(ObjectLocator objectLocator, StringBuilder stringBuilder, float[] floats) {
      return super.append(objectLocator, stringBuilder, floats);    //To change body of overridden methods use File | Settings | File Templates.

   }

   @Override
   public StringBuilder append(ObjectLocator objectLocator, StringBuilder stringBuilder, int[] ints) {
      return super.append(objectLocator, stringBuilder, ints);    //To change body of overridden methods use File | Settings | File Templates.

   }

   @Override
   public StringBuilder append(ObjectLocator objectLocator, StringBuilder stringBuilder, long[] longs) {
      return super.append(objectLocator, stringBuilder, longs);    //To change body of overridden methods use File | Settings | File Templates.

   }

   @Override
   public StringBuilder append(ObjectLocator objectLocator, StringBuilder stringBuilder, short[] shorts) {
      return super.append(objectLocator, stringBuilder, shorts);    //To change body of overridden methods use File | Settings | File Templates.

   }

   @Override
   public StringBuilder append(ObjectLocator objectLocator, StringBuilder stringBuilder, Object[] objects) {
      return super.append(objectLocator, stringBuilder, objects);    //To change body of overridden methods use File | Settings | File Templates.

   }

   @Override
   public StringBuilder appendField(ObjectLocator objectLocator, Object o, String s, StringBuilder stringBuilder, boolean b) {
      return super.appendField(objectLocator, o, s, stringBuilder, b);    //To change body of overridden methods use File | Settings | File Templates.

   }

   @Override
   public StringBuilder appendField(ObjectLocator objectLocator, Object o, String s, StringBuilder stringBuilder, byte b) {
      return super.appendField(objectLocator, o, s, stringBuilder, b);    //To change body of overridden methods use File | Settings | File Templates.

   }

   @Override
   public StringBuilder appendField(ObjectLocator objectLocator, Object o, String s, StringBuilder stringBuilder, char c) {
      return super.appendField(objectLocator, o, s, stringBuilder, c);    //To change body of overridden methods use File | Settings | File Templates.

   }

   @Override
   public StringBuilder appendField(ObjectLocator objectLocator, Object o, String s, StringBuilder stringBuilder, double v) {
      return super.appendField(objectLocator, o, s, stringBuilder, v);    //To change body of overridden methods use File | Settings | File Templates.

   }

   @Override
   public StringBuilder appendField(ObjectLocator objectLocator, Object o, String s, StringBuilder stringBuilder, float v) {
      return super.appendField(objectLocator, o, s, stringBuilder, v);    //To change body of overridden methods use File | Settings | File Templates.

   }

   @Override
   public StringBuilder appendField(ObjectLocator objectLocator, Object o, String s, StringBuilder stringBuilder, int i) {
      return super.appendField(objectLocator, o, s, stringBuilder, i);    //To change body of overridden methods use File | Settings | File Templates.

   }

   @Override
   public StringBuilder appendField(ObjectLocator objectLocator, Object o, String s, StringBuilder stringBuilder, long l) {
      return super.appendField(objectLocator, o, s, stringBuilder, l);    //To change body of overridden methods use File | Settings | File Templates.

   }

   @Override
   public StringBuilder appendField(ObjectLocator objectLocator, Object o, String s, StringBuilder stringBuilder, short i) {
      return super.appendField(objectLocator, o, s, stringBuilder, i);    //To change body of overridden methods use File | Settings | File Templates.

   }


   //***********************************************


   @Override
   public StringBuilder appendField(ObjectLocator objectLocator, Object o, String s, StringBuilder stringBuilder, boolean[] booleans) {
      return super.appendField(objectLocator, o, s, stringBuilder, booleans);    //To change body of overridden methods use File | Settings | File Templates.

   }

   @Override
   public StringBuilder appendField(ObjectLocator objectLocator, Object o, String s, StringBuilder stringBuilder, byte[] bytes) {
      return super.appendField(objectLocator, o, s, stringBuilder, bytes);    //To change body of overridden methods use File | Settings | File Templates.

   }

   @Override
   public StringBuilder appendField(ObjectLocator objectLocator, Object o, String s, StringBuilder stringBuilder, char[] chars) {
      return super.appendField(objectLocator, o, s, stringBuilder, chars);    //To change body of overridden methods use File | Settings | File Templates.

   }

   @Override
   public StringBuilder appendField(ObjectLocator objectLocator, Object o, String s, StringBuilder stringBuilder, double[] doubles) {
      return super.appendField(objectLocator, o, s, stringBuilder, doubles);    //To change body of overridden methods use File | Settings | File Templates.

   }

   @Override
   public StringBuilder appendField(ObjectLocator objectLocator, Object o, String s, StringBuilder stringBuilder, float[] floats) {
      return super.appendField(objectLocator, o, s, stringBuilder, floats);    //To change body of overridden methods use File | Settings | File Templates.

   }

   @Override
   public StringBuilder appendField(ObjectLocator objectLocator, Object o, String s, StringBuilder stringBuilder, int[] ints) {
      return super.appendField(objectLocator, o, s, stringBuilder, ints);    //To change body of overridden methods use File | Settings | File Templates.

   }

   @Override
   public StringBuilder appendField(ObjectLocator objectLocator, Object o, String s, StringBuilder stringBuilder, long[] longs) {
      return super.appendField(objectLocator, o, s, stringBuilder, longs);    //To change body of overridden methods use File | Settings | File Templates.

   }

   @Override
   public StringBuilder appendField(ObjectLocator objectLocator, Object o, String s, StringBuilder stringBuilder, short[] shorts) {
      return super.appendField(objectLocator, o, s, stringBuilder, shorts);    //To change body of overridden methods use File | Settings | File Templates.

   }

   @Override
   public StringBuilder appendField(ObjectLocator objectLocator, Object o, String s, StringBuilder stringBuilder, Object[] objects) {
      return super.appendField(objectLocator, o, s, stringBuilder, objects);    //To change body of overridden methods use File | Settings | File Templates.

   }
}
