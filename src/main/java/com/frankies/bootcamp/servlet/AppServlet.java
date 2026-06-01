package com.frankies.bootcamp.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(name = "appHome", value = "/app")
public class AppServlet extends BootcampServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            req.getRequestDispatcher("/app/index.jsp").forward(req, resp);
        } catch (jakarta.servlet.ServletException e) {
            throw new IOException("Unable to render app home", e);
        }
    }
}
