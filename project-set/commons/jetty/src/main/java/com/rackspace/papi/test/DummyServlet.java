package com.rackspace.papi.test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class DummyServlet extends HttpServlet {

   @Override
   protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      resp.setStatus(200);
      resp.getWriter().write(req.getRequestURI());
   }
}
