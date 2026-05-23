package com.frankies.bootcamp.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(name = "login", value = "/login")
public class LoginServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String destination = req.getContextPath() + "/auth/external/login";
        String error = req.getParameter("error");
        if (error != null && !error.isBlank()) {
            destination += "?error=" + java.net.URLEncoder.encode(error, java.nio.charset.StandardCharsets.UTF_8);
        }
        resp.sendRedirect(destination);
    }
}
