package com.rackspace.papi.service.event.common;

public interface EventDispatcher {

   void dispatch();

   Event getEvent();
   
}
