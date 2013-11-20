package com.rackspace.papi.mocks;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ResponseSizeServlet extends HttpServlet {

    private Pattern pattern = Pattern.compile(".*/([\\d]+)");

    private String getSize(String uri) {
        Matcher matcher = pattern.matcher(uri);
        if (matcher.matches() && matcher.groupCount() > 0) {
            return matcher.group(1);
        }

        return null;
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String size = getSize(request.getRequestURI());

        if (size != null) {
            OutputStream out = response.getOutputStream();
            response.setContentLength(-1);
            int len = Integer.valueOf(size);
            for (int i = 0; i < len; i++) {
                out.write((byte) (i % 128));
            }

            out.flush();
            out.close();
        }

        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Servlet ErrorServlet</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>Servlet ErrorServlet at " + request.getContextPath() + "</h1>");
            out.println("<h2>Request URL " + request.getRequestURL() + "</h2>");
            out.println("</body>");
            out.println("</html>");
        } finally {
            out.close();
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP
     * <code>GET</code> method.
     *
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
     * Handles the HTTP
     * <code>POST</code> method.
     *
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
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
}
