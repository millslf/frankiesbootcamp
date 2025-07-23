package com.frankies.bootcamp.servlet;

import com.frankies.bootcamp.constant.BootcampConstants;
import com.frankies.bootcamp.utils.WildflyUtils;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(name = "scoring", value = "/Scoring")
public class ScoringServlet extends BootcampServlet {
    private static final Logger log = Logger.getLogger(ScoringServlet.class);

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PrintWriter out = response.getWriter();
        // Hello
        out.println("<html><body>");
        out.println(WildflyUtils.escape(BootcampConstants.SCORING));
        out.println("</body></html>");
    }

    public void destroy() {
    }
}