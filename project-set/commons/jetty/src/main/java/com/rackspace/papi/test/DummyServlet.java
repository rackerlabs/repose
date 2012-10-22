package com.rackspace.papi.test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class DummyServlet extends HttpServlet {
   
   private static final int DEFAULT_STATUS_CODE = 200;

   @Override
   protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      resp.setStatus(DEFAULT_STATUS_CODE);
      resp.getWriter().write(req.getRequestURI());
   }
}
