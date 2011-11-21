/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.mocks.auth;

import java.io.IOException;
import java.io.PrintWriter;


import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author malconis
 */
public class TokenServlet extends HttpServlet {

    // Need to move these somewhere else where they won't be re-initialized everytime we auth
    User user1 = new User("usertest1", "CLOUD", "/asdasdasd-adsasdads-asdasdasd-adsadsasd");
    User user2 = new User("usertest2", "CLOUD", "/now-is-the-time");
    User user3 = new User("usertest3", "CLOUD", "/my-third-test-user");
    User user4 = new User("usertest4", "CLOUD", "/dkshk-fdjke3-fdfjdk-21342");
    User[] testUsers = {user1, user2, user3, user4}; // new ArrayList<MockUser>();
    User passedUser;

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
        


        String queryParams = request.getQueryString();
        String token = request.getPathInfo();
        boolean valid = false;
        passedUser = new User(queryParams, token);
        try {
        
            
            for (User u : testUsers) {
                if (u.equals(passedUser)) {
                    valid = true;
                }
            }

            if (valid) {
                out.println(passedUser);
            } else {
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
