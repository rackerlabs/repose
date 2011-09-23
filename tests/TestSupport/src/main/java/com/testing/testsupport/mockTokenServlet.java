/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.testing.testsupport;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author malconis
 */
public class mockTokenServlet extends HttpServlet {

  /** 
   * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
   * @param request servlet request
   * @param response servlet response
   * @throws ServletException if a servlet-specific error occurs
   * @throws IOException if an I/O error occurs
   */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {
    response.setContentType("text/html;charset=UTF-8");
    PrintWriter out = response.getWriter();
    String DATE_FORMAT = "yyyy-MM-dd'T'HH:MM:ss-5:00";

    SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
    Calendar c1 = Calendar.getInstance();
    Calendar c2 = (Calendar) c1.clone();
    c1.add(Calendar.DAY_OF_YEAR, -1);
    c2.add(Calendar.DAY_OF_YEAR, 4);
    String queryParams = request.getQueryString();
    String[] params = queryParams.split("&");
    String user="", type="", tmp, token=request.getPathInfo();
    try {
      
      for(String s: params){
        tmp =s.split("=")[1];
        if(tmp.equals("cmarin1"))
          user = tmp;
        if(tmp.trim().equals("CLOUD"))
          type = tmp;
      }
      if(!user.isEmpty() && !type.isEmpty() && token.equals("/asdasdasd-adsasdads-asdasdasd-adsadsasd")){
        //out.println(queryString);
        String responseBody = "<token xmlns=\"http://docs.rackspacecloud.com/auth/api/v1.1\" "
                + "id=\"asdasdasd-adsasdads-asdasdasd-adsadsasd\" "
                + "userId=\"cmarin1\" "
                + "userURL=\"/users/cmarin1\" "
                //+ "created=\""+sdf.format(c1.getTime().toString()) +"\" "
                //+ "expires=\""+sdf.format(c2.getTime()) +"\"/>";
                + "created=\"2010-09-14T03:32:15-05:00\" "
                + "expires=\"2012-09-16T03:32:15-05:00\"/>";
        out.println(responseBody);
      }else{
        response.setStatus(400);
      }
    } finally {      
      out.close();
    }
  }

  // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
  /** 
   * Handles the HTTP <code>GET</code> method.
   * @param request servlet request
   * @param response servlet response
   * @throws ServletException if a servlet-specific error occurs
   * @throws IOException if an I/O error occurs
   */
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {
    processRequest(request, response);
  }

  /** 
   * Handles the HTTP <code>POST</code> method.
   * @param request servlet request
   * @param response servlet response
   * @throws ServletException if a servlet-specific error occurs
   * @throws IOException if an I/O error occurs
   */
  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {
    processRequest(request, response);
  }

  /** 
   * Returns a short description of the servlet.
   * @return a String containing servlet description
   */
  @Override
  public String getServletInfo() {
    return "Short description";
  }// </editor-fold>
}
