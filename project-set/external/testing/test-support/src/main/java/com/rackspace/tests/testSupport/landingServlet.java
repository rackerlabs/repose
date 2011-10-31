/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.tests.testSupport;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author malconis
 */
public class landingServlet extends HttpServlet {

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
        Enumeration<String> headers = request.getHeaderNames();
        String headerName, value, queryString, queryParam;
        queryString = request.getQueryString();
        try {
            out.println("<html>");
            out.println("\t<head>");
            out.println("\t\t<title>Servlet version</title>");
            out.println("\t</head>");
            out.println("\t<body>");
            out.println("\t\t<h1>Servlet version at " + request.getRequestURI() + "</h1>");
            out.println("\t\t<h2>HEADERS</h2>");
            while (headers.hasMoreElements()) {
                headerName = headers.nextElement();
                Enumeration<String> headerValues = request.getHeaders(headerName);
                while(headerValues.hasMoreElements()){
                    value = headerValues.nextElement();
                    out.println("\t\t<h2>" + headerName + " : " + value  +"</h2>");
                }
                
            }
            //TODO: DISPLAY Query parameters
            
            if(queryString != null){
                out.println("\t\t<h3>QUERY PARAMETERS</h3>");
                String[] query = queryString.split("&");
                String[] params;
                //for(int i=0;i<query.length/2;i=i+2){
                for(String q: query){
                    params = q.split("=");
                    out.println("\t\t<h3>"+params[0]+" : "+params[1]+"</h3>");
                }
            }
            //out.println("\t\t<h3>"+request.getQueryString()+"</h3>");
            out.println("\t</body>");
            out.println("</html>");
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
