package com.frankies.bootcamp.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(name = "manageCompetitions", value = "/app/competitions")
public class ManageCompetitionsServlet extends BootcampServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.getRequestDispatcher("/app/competition-selection.jsp").forward(request, response);
    }
}
