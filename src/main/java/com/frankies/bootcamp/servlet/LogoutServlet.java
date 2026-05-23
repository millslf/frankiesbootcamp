package com.frankies.bootcamp.servlet;

import com.frankies.bootcamp.service.AuthSessionService;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(name = "logout", value = "/logout")
public class LogoutServlet extends HttpServlet {
    @Inject
    private AuthSessionService authSessionService;

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        authSessionService.clear(req);
        resp.sendRedirect(req.getContextPath() + "/");
    }
}
