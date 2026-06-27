package com.frankies.bootcamp.servlet;

import com.frankies.bootcamp.service.AuthSessionService;
import com.frankies.bootcamp.service.ExternalAuthConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogoutServletTest {

    @Test
    void logoutRedirectsToAuth0LogoutAndClearsSession() throws Exception {
        LogoutServlet servlet = new LogoutServlet();
        inject(servlet, "authSessionService", new AuthSessionService());
        inject(servlet, "externalAuthConfig", new TestExternalAuthConfig());

        FakeHttpSession session = new FakeHttpSession();
        FakeHttpServletRequest request = new FakeHttpServletRequest("/bootcamp", session);
        FakeHttpServletResponse response = new FakeHttpServletResponse();

        servlet.doGet(request, response);

        assertTrue(session.invalidated);
        assertEquals(
                "https://auth.example.com/v2/logout?client_id=client-123&returnTo=https%3A%2F%2Fapp.example.com%2Fbootcamp%2F",
                response.redirectLocation
        );
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static final class TestExternalAuthConfig extends ExternalAuthConfig {
        @Override public String getDomain() { return "auth.example.com"; }
        @Override public String getClientId() { return "client-123"; }
        @Override public String getClientSecret() { return "secret"; }
        @Override public String getBaseUrl() { return "https://app.example.com"; }
    }

    private static final class FakeHttpServletRequest implements HttpServletRequest {
        private final String contextPath;
        private final HttpSession session;

        private FakeHttpServletRequest(String contextPath, HttpSession session) {
            this.contextPath = contextPath;
            this.session = session;
        }

        @Override public String getContextPath() { return contextPath; }
        @Override public HttpSession getSession(boolean create) { return session; }
        @Override public HttpSession getSession() { return session; }
        @Override public Object getAttribute(String name) { return null; }
        @Override public Enumeration<String> getAttributeNames() { return Collections.emptyEnumeration(); }
        @Override public String getCharacterEncoding() { return null; }
        @Override public void setCharacterEncoding(String env) {}
        @Override public int getContentLength() { return 0; }
        @Override public long getContentLengthLong() { return 0; }
        @Override public String getContentType() { return null; }
        @Override public jakarta.servlet.ServletInputStream getInputStream() { return null; }
        @Override public String getParameter(String name) { return null; }
        @Override public Enumeration<String> getParameterNames() { return Collections.emptyEnumeration(); }
        @Override public String[] getParameterValues(String name) { return new String[0]; }
        @Override public Map<String, String[]> getParameterMap() { return new HashMap<>(); }
        @Override public String getProtocol() { return null; }
        @Override public String getScheme() { return null; }
        @Override public String getServerName() { return null; }
        @Override public int getServerPort() { return 0; }
        @Override public BufferedReader getReader() { return null; }
        @Override public String getRemoteAddr() { return null; }
        @Override public String getRemoteHost() { return null; }
        @Override public void setAttribute(String name, Object o) {}
        @Override public void removeAttribute(String name) {}
        @Override public Locale getLocale() { return Locale.getDefault(); }
        @Override public Enumeration<Locale> getLocales() { return Collections.enumeration(Collections.singletonList(Locale.getDefault())); }
        @Override public boolean isSecure() { return false; }
        @Override public jakarta.servlet.RequestDispatcher getRequestDispatcher(String path) { return null; }
        @Override public int getRemotePort() { return 0; }
        @Override public String getLocalName() { return null; }
        @Override public String getLocalAddr() { return null; }
        @Override public int getLocalPort() { return 0; }
        @Override public jakarta.servlet.ServletContext getServletContext() { return null; }
        @Override public jakarta.servlet.AsyncContext startAsync() { return null; }
        @Override public jakarta.servlet.AsyncContext startAsync(jakarta.servlet.ServletRequest servletRequest, jakarta.servlet.ServletResponse servletResponse) { return null; }
        @Override public boolean isAsyncStarted() { return false; }
        @Override public boolean isAsyncSupported() { return false; }
        @Override public jakarta.servlet.AsyncContext getAsyncContext() { return null; }
        @Override public jakarta.servlet.DispatcherType getDispatcherType() { return null; }
        @Override public String getAuthType() { return null; }
        @Override public Cookie[] getCookies() { return new Cookie[0]; }
        @Override public long getDateHeader(String name) { return 0; }
        @Override public String getHeader(String name) { return null; }
        @Override public Enumeration<String> getHeaders(String name) { return Collections.emptyEnumeration(); }
        @Override public Enumeration<String> getHeaderNames() { return Collections.emptyEnumeration(); }
        @Override public int getIntHeader(String name) { return 0; }
        @Override public String getMethod() { return "GET"; }
        @Override public String getPathInfo() { return null; }
        @Override public String getPathTranslated() { return null; }
        @Override public String getQueryString() { return null; }
        @Override public String getRemoteUser() { return null; }
        @Override public boolean isUserInRole(String role) { return false; }
        @Override public Principal getUserPrincipal() { return null; }
        @Override public String getRequestedSessionId() { return null; }
        @Override public String getServletPath() { return null; }
        @Override public StringBuffer getRequestURL() { return new StringBuffer("http://localhost" + contextPath + "/logout"); }
        @Override public String getRequestURI() { return contextPath + "/logout"; }
        @Override public boolean isRequestedSessionIdValid() { return false; }
        @Override public boolean isRequestedSessionIdFromCookie() { return false; }
        @Override public boolean isRequestedSessionIdFromURL() { return false; }
        @Override public boolean authenticate(HttpServletResponse response) { return false; }
        @Override public void login(String username, String password) {}
        @Override public void logout() {}
        @Override public Collection<jakarta.servlet.http.Part> getParts() { return Collections.emptyList(); }
        @Override public jakarta.servlet.http.Part getPart(String name) { return null; }
        @Override public <T extends jakarta.servlet.http.HttpUpgradeHandler> T upgrade(Class<T> handlerClass) { return null; }
        @Override public String changeSessionId() { return null; }
        @Override public String getRequestId() { return null; }
        @Override public String getProtocolRequestId() { return null; }
        @Override public jakarta.servlet.ServletConnection getServletConnection() { return null; }
    }

    private static final class FakeHttpSession implements HttpSession {
        private boolean invalidated;

        @Override public long getCreationTime() { return 0; }
        @Override public String getId() { return "session"; }
        @Override public long getLastAccessedTime() { return 0; }
        @Override public jakarta.servlet.ServletContext getServletContext() { return null; }
        @Override public void setMaxInactiveInterval(int interval) {}
        @Override public int getMaxInactiveInterval() { return 0; }
        @Override public Object getAttribute(String name) { return null; }
        @Override public Enumeration<String> getAttributeNames() { return Collections.emptyEnumeration(); }
        @Override public void setAttribute(String name, Object value) {}
        @Override public void removeAttribute(String name) {}
        @Override public void invalidate() { invalidated = true; }
        @Override public boolean isNew() { return false; }
    }

    private static final class FakeHttpServletResponse implements HttpServletResponse {
        private String redirectLocation;

        @Override public void sendRedirect(String location) { redirectLocation = location; }
        @Override public void sendRedirect(String location, int sc, boolean clearBuffer) { redirectLocation = location; }
        @Override public void addCookie(Cookie cookie) {}
        @Override public boolean containsHeader(String name) { return false; }
        @Override public String encodeURL(String url) { return url; }
        @Override public String encodeRedirectURL(String url) { return url; }
        @Override public void sendError(int sc, String msg) {}
        @Override public void sendError(int sc) {}
        @Override public void setDateHeader(String name, long date) {}
        @Override public void addDateHeader(String name, long date) {}
        @Override public void setHeader(String name, String value) {}
        @Override public void addHeader(String name, String value) {}
        @Override public void setIntHeader(String name, int value) {}
        @Override public void addIntHeader(String name, int value) {}
        @Override public void setStatus(int sc) {}
        @Override public int getStatus() { return 0; }
        @Override public String getHeader(String name) { return null; }
        @Override public Collection<String> getHeaders(String name) { return Collections.emptyList(); }
        @Override public Collection<String> getHeaderNames() { return Collections.emptyList(); }
        @Override public String getCharacterEncoding() { return null; }
        @Override public String getContentType() { return null; }
        @Override public jakarta.servlet.ServletOutputStream getOutputStream() { return null; }
        @Override public PrintWriter getWriter() { return null; }
        @Override public void setCharacterEncoding(String charset) {}
        @Override public void setContentLength(int len) {}
        @Override public void setContentLengthLong(long len) {}
        @Override public void setContentType(String type) {}
        @Override public void setBufferSize(int size) {}
        @Override public int getBufferSize() { return 0; }
        @Override public void flushBuffer() {}
        @Override public void resetBuffer() {}
        @Override public boolean isCommitted() { return false; }
        @Override public void reset() {}
        @Override public void setLocale(Locale loc) {}
        @Override public Locale getLocale() { return Locale.getDefault(); }
    }
}
