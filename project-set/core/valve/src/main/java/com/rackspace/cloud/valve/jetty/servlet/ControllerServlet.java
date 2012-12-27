package com.rackspace.cloud.valve.jetty.servlet;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ControllerServlet extends HttpServlet {
   
   private static final Logger LOG = LoggerFactory.getLogger(ControllerServlet.class);
   
   @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
  
      resp.sendError(501, "Controller Server Not Yet Implemented");
   }
}
