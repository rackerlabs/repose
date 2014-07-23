package org.openrepose.core.service.event;

public interface EventDispatcher {

   void dispatch();

   Event getEvent();
   
}
