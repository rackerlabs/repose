package com.rackspace.papi.mocks;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class DataProviderImpl implements DataProvider {

   private final DatatypeFactory dataTypeFactory;

   public DataProviderImpl() throws DatatypeConfigurationException {
      dataTypeFactory = DatatypeFactory.newInstance();
   }

   @Override
   public XMLGregorianCalendar getCalendar() {
      return getCalendar(Calendar.DAY_OF_YEAR, 0);
   }

   @Override
   public XMLGregorianCalendar getCalendar(int field, int value) {
      Calendar calendar = GregorianCalendar.getInstance();

      if (value != 0) {
         calendar.setLenient(true);
         calendar.add(field, value);
      }

      return dataTypeFactory.newXMLGregorianCalendar((GregorianCalendar) calendar);
   }
   
}
