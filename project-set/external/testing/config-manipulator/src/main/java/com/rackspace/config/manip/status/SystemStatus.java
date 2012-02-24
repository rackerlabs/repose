package com.rackspace.config.manip.status;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * @author fran
 */
@Path("/status")
public class SystemStatus {
   private static final String SYSTEM_UP_MESSAGE = "I am here to serve you.";

   @GET
   @Produces()
   public String get() {
      return SYSTEM_UP_MESSAGE;
   }
}
