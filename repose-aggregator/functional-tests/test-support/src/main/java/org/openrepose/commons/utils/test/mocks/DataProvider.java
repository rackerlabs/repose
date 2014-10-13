package org.openrepose.commons.utils.test.mocks;

import javax.xml.datatype.XMLGregorianCalendar;

public interface DataProvider {

   XMLGregorianCalendar getCalendar();

   XMLGregorianCalendar getCalendar(int field, int value);
   
}
