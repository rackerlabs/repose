package org.openrepose.core.services.event.common;

public interface EventDispatcher {

   void dispatch();

   Event getEvent();
   
}
