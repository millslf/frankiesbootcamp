package com.frankies.bootcamp.servlet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ManageCompetitionsServletTest {

    @Test
    void leaveErrorRedirectKeepsUserOnManagePage() {
        assertEquals(
                "/bootcamp/app/competitions?leaveError=last-admin",
                ManageCompetitionsServlet.buildLeaveErrorRedirect("/bootcamp", "last-admin")
        );
    }
}
