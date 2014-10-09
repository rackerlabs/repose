package org.openrepose.core.service.event.common;

public interface EventDispatcher {

   void dispatch();

   Event getEvent();
   
}
