package com.frankies.bootcamp.auth;

import com.frankies.bootcamp.model.AuthenticatedUser;
import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.service.AuthSessionService;
import com.frankies.bootcamp.servlet.LoginServlet;
import jakarta.servlet.ServletConnection;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.Part;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthSessionServiceTest {

    @Test
    void storeAuthenticatedUserWritesExpectedSessionAttributes() {
        AuthSessionService service = new AuthSessionService();
        FakeHttpServletRequest request = new FakeHttpServletRequest("", new FakeHttpSession());

        AuthenticatedUser user = new AuthenticatedUser();
        user.setEmail("athlete@example.com");
        user.setDisplayName("Athlete Example");

        BootcampAthlete athlete = new BootcampAthlete();
        athlete.setId("athlete-1");

        service.storeAuthenticatedUser(request, user, athlete);

        assertSame(user, request.session.getAttribute(AuthSessionService.AUTH_USER_SESSION_KEY));
        assertSame(athlete, request.session.getAttribute("athlete"));
        assertEquals("athlete@example.com", request.session.getAttribute("athleteEmail"));
        assertEquals("Athlete Example", request.session.getAttribute("athleteName"));
    }

    @Test
    void getAuthenticatedUserReturnsNullWithoutSession() {
        AuthSessionService service = new AuthSessionService();

        assertNull(service.getAuthenticatedUser(new FakeHttpServletRequest("", null)));
    }

    @Test
    void clearInvalidatesExistingSession() {
        AuthSessionService service = new AuthSessionService();
        FakeHttpSession session = new FakeHttpSession();
        FakeHttpServletRequest request = new FakeHttpServletRequest("", session);

        service.clear(request);

        assertTrue(session.invalidated);
    }

    @Test
    void loginServletRedirectsStraightToExternalLogin() throws Exception {
        TestableLoginServlet servlet = new TestableLoginServlet();
        FakeHttpServletRequest request = new FakeHttpServletRequest("/bootcamp", new FakeHttpSession());
        FakeHttpServletResponse response = new FakeHttpServletResponse();

        servlet.handleGet(request, response);

        assertEquals("/bootcamp/auth/external/login", response.redirectedTo);
    }

    @Test
    void loginServletPreservesErrorQueryString() throws Exception {
        TestableLoginServlet servlet = new TestableLoginServlet();
        FakeHttpServletRequest request = new FakeHttpServletRequest("/bootcamp", new FakeHttpSession());
        request.parameters.put("error", "external failure");
        FakeHttpServletResponse response = new FakeHttpServletResponse();

        servlet.handleGet(request, response);

        assertEquals("/bootcamp/auth/external/login?error=external+failure", response.redirectedTo);
    }

    private static final class TestableLoginServlet extends LoginServlet {
        void handleGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            super.doGet(request, response);
        }
    }

    private static final class FakeHttpServletRequest implements HttpServletRequest {
        private final String contextPath;
        private FakeHttpSession session;
        private final Map<String, String> parameters = new HashMap<>();

        private FakeHttpServletRequest(String contextPath, FakeHttpSession session) {
            this.contextPath = contextPath;
            this.session = session;
        }

        @Override
        public HttpSession getSession(boolean create) {
            if (session == null && create) {
                session = new FakeHttpSession();
            }
            return session;
        }

        @Override
        public HttpSession getSession() {
            return getSession(true);
        }

        @Override
        public String getContextPath() {
            return contextPath;
        }

        @Override
        public String getParameter(String name) {
            return parameters.get(name);
        }

        @Override
        public Object getAttribute(String name) { return null; }
        @Override
        public Enumeration<String> getAttributeNames() { return Collections.emptyEnumeration(); }
        @Override
        public String getCharacterEncoding() { return null; }
        @Override
        public void setCharacterEncoding(String env) {}
        @Override
        public int getContentLength() { return 0; }
        @Override
        public long getContentLengthLong() { return 0; }
        @Override
        public String getContentType() { return null; }
        @Override
        public ServletInputStream getInputStream() { return null; }
        @Override
        public BufferedReader getReader() { return null; }
        @Override
        public String getProtocol() { return null; }
        @Override
        public String getScheme() { return null; }
        @Override
        public String getServerName() { return null; }
        @Override
        public int getServerPort() { return 0; }
        @Override
        public String getRemoteAddr() { return null; }
        @Override
        public String getRemoteHost() { return null; }
        @Override
        public void setAttribute(String name, Object o) {}
        @Override
        public void removeAttribute(String name) {}
        @Override
        public Locale getLocale() { return Locale.getDefault(); }
        @Override
        public Enumeration<Locale> getLocales() { return Collections.enumeration(Collections.singletonList(Locale.getDefault())); }
        @Override
        public boolean isSecure() { return false; }
        @Override
        public jakarta.servlet.RequestDispatcher getRequestDispatcher(String path) { return null; }
        public String getRealPath(String path) { return null; }
        @Override
        public int getRemotePort() { return 0; }
        @Override
        public String getLocalName() { return null; }
        @Override
        public String getLocalAddr() { return null; }
        @Override
        public int getLocalPort() { return 0; }
        @Override
        public ServletContext getServletContext() { return null; }
        @Override
        public jakarta.servlet.AsyncContext startAsync() { return null; }
        @Override
        public jakarta.servlet.AsyncContext startAsync(jakarta.servlet.ServletRequest servletRequest, jakarta.servlet.ServletResponse servletResponse) { return null; }
        @Override
        public boolean isAsyncStarted() { return false; }
        @Override
        public boolean isAsyncSupported() { return false; }
        @Override
        public jakarta.servlet.AsyncContext getAsyncContext() { return null; }
        @Override
        public jakarta.servlet.DispatcherType getDispatcherType() { return null; }
        @Override
        public String getAuthType() { return null; }
        @Override
        public Cookie[] getCookies() { return new Cookie[0]; }
        @Override
        public long getDateHeader(String name) { return 0; }
        @Override
        public String getHeader(String name) { return null; }
        @Override
        public Enumeration<String> getHeaders(String name) { return Collections.emptyEnumeration(); }
        @Override
        public Enumeration<String> getHeaderNames() { return Collections.emptyEnumeration(); }
        @Override
        public int getIntHeader(String name) { return 0; }
        @Override
        public String getMethod() { return null; }
        @Override
        public String getPathInfo() { return null; }
        @Override
        public String getPathTranslated() { return null; }
        @Override
        public String getQueryString() { return null; }
        @Override
        public String getRemoteUser() { return null; }
        @Override
        public boolean isUserInRole(String role) { return false; }
        @Override
        public Principal getUserPrincipal() { return null; }
        @Override
        public String getRequestedSessionId() { return null; }
        @Override
        public String getRequestURI() { return null; }
        @Override
        public StringBuffer getRequestURL() { return null; }
        @Override
        public String getServletPath() { return null; }
        @Override
        public Map<String, String[]> getParameterMap() { return Collections.emptyMap(); }
        @Override
        public Enumeration<String> getParameterNames() { return Collections.enumeration(parameters.keySet()); }
        @Override
        public String[] getParameterValues(String name) { return parameters.containsKey(name) ? new String[]{parameters.get(name)} : null; }
        @Override
        public String getProtocolRequestId() { return null; }
        @Override
        public String getRequestId() { return null; }
        @Override
        public ServletConnection getServletConnection() { return null; }
        @Override
        public String changeSessionId() { return "changed-session"; }
        @Override
        public boolean isRequestedSessionIdValid() { return false; }
        @Override
        public boolean isRequestedSessionIdFromCookie() { return false; }
        @Override
        public boolean isRequestedSessionIdFromURL() { return false; }
        public boolean isRequestedSessionIdFromUrl() { return false; }
        @Override
        public boolean authenticate(HttpServletResponse response) { return false; }
        @Override
        public void login(String username, String password) {}
        @Override
        public void logout() {}
        @Override
        public Collection<Part> getParts() { return Collections.emptyList(); }
        @Override
        public Part getPart(String name) { return null; }
        @Override
        public <T extends jakarta.servlet.http.HttpUpgradeHandler> T upgrade(Class<T> handlerClass) { return null; }
    }

    private static final class FakeHttpServletResponse implements HttpServletResponse {
        private String redirectedTo;

        @Override
        public void sendRedirect(String location) {
            redirectedTo = location;
        }

        @Override
        public void sendRedirect(String location, int sc, boolean clearBuffer) {
            redirectedTo = location;
        }

        @Override
        public void addCookie(Cookie cookie) {}
        @Override
        public boolean containsHeader(String name) { return false; }
        @Override
        public String encodeURL(String url) { return url; }
        @Override
        public String encodeRedirectURL(String url) { return url; }
        @Override
        public void sendError(int sc, String msg) {}
        @Override
        public void sendError(int sc) {}
        @Override
        public void setDateHeader(String name, long date) {}
        @Override
        public void addDateHeader(String name, long date) {}
        @Override
        public void setHeader(String name, String value) {}
        @Override
        public void addHeader(String name, String value) {}
        @Override
        public void setIntHeader(String name, int value) {}
        @Override
        public void addIntHeader(String name, int value) {}
        @Override
        public void setStatus(int sc) {}
        @Override
        public int getStatus() { return 0; }
        @Override
        public String getHeader(String name) { return null; }
        @Override
        public Collection<String> getHeaders(String name) { return Collections.emptyList(); }
        @Override
        public Collection<String> getHeaderNames() { return Collections.emptyList(); }
        @Override
        public String getCharacterEncoding() { return null; }
        @Override
        public String getContentType() { return null; }
        @Override
        public jakarta.servlet.ServletOutputStream getOutputStream() { return null; }
        @Override
        public PrintWriter getWriter() { return null; }
        @Override
        public void setCharacterEncoding(String charset) {}
        @Override
        public void setContentLength(int len) {}
        @Override
        public void setContentLengthLong(long len) {}
        @Override
        public void setContentType(String type) {}
        @Override
        public void setBufferSize(int size) {}
        @Override
        public int getBufferSize() { return 0; }
        @Override
        public void flushBuffer() {}
        @Override
        public void resetBuffer() {}
        @Override
        public boolean isCommitted() { return false; }
        @Override
        public void reset() {}
        @Override
        public void setLocale(Locale loc) {}
        @Override
        public Locale getLocale() { return Locale.getDefault(); }
    }

    private static final class FakeHttpSession implements HttpSession, Serializable {
        private final Map<String, Object> attributes = new HashMap<>();
        private boolean invalidated;

        @Override
        public long getCreationTime() { return 0; }
        @Override
        public String getId() { return "session-id"; }
        @Override
        public long getLastAccessedTime() { return 0; }
        @Override
        public ServletContext getServletContext() { return null; }
        @Override
        public void setMaxInactiveInterval(int interval) {}
        @Override
        public int getMaxInactiveInterval() { return 0; }
        @Override
        public Object getAttribute(String name) { return attributes.get(name); }
        public Object getValue(String name) { return getAttribute(name); }
        @Override
        public Enumeration<String> getAttributeNames() { return Collections.enumeration(attributes.keySet()); }
        public String[] getValueNames() { return attributes.keySet().toArray(String[]::new); }
        @Override
        public void setAttribute(String name, Object value) { attributes.put(name, value); }
        public void putValue(String name, Object value) { setAttribute(name, value); }
        @Override
        public void removeAttribute(String name) { attributes.remove(name); }
        public void removeValue(String name) { removeAttribute(name); }
        @Override
        public void invalidate() { invalidated = true; }
        @Override
        public boolean isNew() { return false; }
    }
}
