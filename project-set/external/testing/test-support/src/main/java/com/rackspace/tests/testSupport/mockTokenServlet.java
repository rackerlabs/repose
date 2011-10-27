/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.tests.testSupport;

import java.io.IOException;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author malconis
 */
public class mockTokenServlet extends HttpServlet {

    // Need to move these somewhere else where they won't be re-initialized everytime we auth
    MockUser user1 = new MockUser("usertest1", "CLOUD", "/asdasdasd-adsasdads-asdasdasd-adsadsasd");
    MockUser user2 = new MockUser("usertest2", "CLOUD", "/now-is-the-time");
    MockUser user3 = new MockUser("usertest3", "CLOUD", "/my-third-test-user");
    MockUser user4 = new MockUser("usertest4", "CLOUD", "/dkshk-fdjke3-fdfjdk-21342");
    MockUser[] testUsers = {user1, user2, user3, user4}; // new ArrayList<MockUser>();
    MockUser passedUser;

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
        passedUser = new MockUser(queryParams, token);
        try {
        
            
            for (MockUser u : testUsers) {
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
