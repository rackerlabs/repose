/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.tests.testSupport;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author malconis
 */
@WebServlet(name = "GetTokensServlet", urlPatterns = {"/GetTokensServlet"})
public class GetTokensServlet extends HttpServlet {

    /** 
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    MockUser user1 = new MockUser("usertest1", "CLOUD", "asdasdasd-adsasdads-asdasdasd-adsadsasd");
    MockUser user2 = new MockUser("usertest2", "CLOUD", "now-is-the-time");
    MockUser user3 = new MockUser("usertest3", "CLOUD", "my-third-test-user");
    MockUser user4 = new MockUser("usertest4", "CLOUD", "dkshk-fdjke3-fdfjdk-21342");
    MockUser[] testUsers = {user1, user2, user3, user4}; // new ArrayList<MockUser>();

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        InputStream body = request.getInputStream();
        int read;
        String requestBody ="";
        String passedUser;

        try {
            while ((read = body.read()) != -1) {
                requestBody += (char)read;
            }
            
            passedUser = requestBody.substring(requestBody.indexOf("\"username\": \"")+13, requestBody.indexOf("\", \"password\":"));
            for(MockUser u: testUsers){
                if(u.getName().equals(passedUser)){
                    String myResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                            + "<access xmlns=\"http://docs.openstack.org/identity/api/v2.0\" xmlns:ns2=\"http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0\" "
                            + "xmlns:ns3=\"http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0\" xmlns:ns4=\"http://docs.openstack.org/common/api/v1.0\" "
                            + "xmlns:ns5=\"http://www.w3.org/2005/Atom\">"
                            + "<token id=\""+u.getToken()+"\" expires=\"2011-11-15T15:59:14.000-06:00\"/>"
                            + "<user id=\"24984\" name=\"auth\"><roles><role id=\"identity:admin\" name=\"identity:admin\" description=\"Admin Role.\"/>"
                            + "<role id=\"identity:default\" name=\"identity:default\" description=\"Default Role.\"/></roles>"
                            + "</user><serviceCatalog>"
                            + "<service type=\"compute\" name=\"cloudServers\"><endpoint tenantId=\"-2\" publicURL=\"https://servers.api.rackspacecloud.com/v1.0/-2\">"
                            + "<version id=\"1.0\" info=\"https://servers.api.rackspacecloud.com/v1.0/\" list=\"https://servers.api.rackspacecloud.com/\"/></endpoint>"
                            + "</service><service type=\"object-store\" name=\"cloudFilesCDN\">"
                            + "<endpoint region=\"DFW\" tenantId=\"MossoCloudFS_7bff8f06-e742-4ac2-bb26-4107edae95c7\" "
                            + "publicURL=\"https://cdn1.clouddrive.com/v1/MossoCloudFS_7bff8f06-e742-4ac2-bb26-4107edae95c7\">"
                            + "<version id=\"1\" info=\"https://cdn1.clouddrive.com/v1/\" list=\"https://cdn1.clouddrive.com/\"/></endpoint>"
                            + "</service><service type=\"object-store\" name=\"cloudFiles\">"
                            + "<endpoint region=\"DFW\" tenantId=\"MossoCloudFS_7bff8f06-e742-4ac2-bb26-4107edae95c7\" "
                            + "publicURL=\"https://storage101.dfw1.clouddrive.com/v1/MossoCloudFS_7bff8f06-e742-4ac2-bb26-4107edae95c7\" "
                            + "internalURL=\"https://snet-storage101.dfw1.clouddrive.com/v1/MossoCloudFS_7bff8f06-e742-4ac2-bb26-4107edae95c7\">"
                            + "<version id=\"1\" info=\"https://storage101.dfw1.clouddrive.com/v1/\" list=\"https://storage101.dfw1.clouddrive.com/\"/></endpoint>"
                            + "</service></serviceCatalog></access>";
                    out.println(myResponse);
                }
            }
            
            /* TODO output your page here
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Servlet GetTokensServlet</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>Servlet GetTokensServlet at " + request.getContextPath () + "</h1>");
            out.println("</body>");
            out.println("</html>");
             */
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
