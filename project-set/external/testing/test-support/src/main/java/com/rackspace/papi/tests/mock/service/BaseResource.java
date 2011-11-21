package com.rackspace.papi.tests.mock.service;

import java.util.Calendar;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

public class BaseResource {
   private final DatatypeFactory dataTypeFactory;

   public BaseResource() throws DatatypeConfigurationException {
      dataTypeFactory = DatatypeFactory.newInstance();
   }
   
   protected XMLGregorianCalendar getCalendar() {
      return getCalendar(Calendar.DAY_OF_YEAR, 0);
   }

   protected XMLGregorianCalendar getCalendar(int field, int value) {
      Calendar calendar = Calendar.getInstance();
      if (value != 0) {
         calendar.setLenient(true);
         calendar.add(field, value);
      }
      int year = calendar.get(Calendar.YEAR);
      int month = calendar.get(Calendar.MONTH) + 1;
      int day = calendar.get(Calendar.DAY_OF_MONTH);
      int hour = calendar.get(Calendar.HOUR_OF_DAY);
      int min = calendar.get(Calendar.MINUTE);
      int sec = calendar.get(Calendar.SECOND);
      int milli = calendar.get(Calendar.MILLISECOND);
      int tz = calendar.get(Calendar.ZONE_OFFSET) / 60000;

      return dataTypeFactory.newXMLGregorianCalendar(year, month, day, hour, min, sec, milli, tz);
   }

   
}
