package com.bupt.ta.testsupport;

import com.bupt.ta.model.User;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Mockito helpers for servlet unit tests without Tomcat. */
public final class ServletTestSupport {

    private ServletTestSupport() {}

    public static ServletContext mockServletContext(Path dataDir) {
        ServletContext ctx = mock(ServletContext.class);
        String path = dataDir != null
                ? dataDir.toAbsolutePath().toString().replace('\\', '/')
                : "/tmp/ta-test-data";
        when(ctx.getRealPath("/WEB-INF/data")).thenReturn(path);
        when(ctx.getRealPath("/WEB-INF/data/jobs.json")).thenReturn(path + "/jobs.json");
        when(ctx.getRealPath("/WEB-INF/data/cv")).thenReturn(path + "/cv");
        when(ctx.getContextPath()).thenReturn("/ta-recruitment");
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(ctx.getRequestDispatcher(anyString())).thenReturn(dispatcher);
        return ctx;
    }

    public static HttpSession mockSession(User user) {
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute("user")).thenReturn(user);
        return session;
    }

    public static HttpServletRequest mockRequest(ServletContext ctx, HttpSession session,
                                                  Map<String, String> params) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getServletContext()).thenReturn(ctx);
        when(req.getSession(false)).thenReturn(session);
        when(req.getSession(true)).thenReturn(session != null ? session : mock(HttpSession.class));
        when(req.getContextPath()).thenReturn("/ta-recruitment");
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(req.getRequestDispatcher(anyString())).thenReturn(dispatcher);
        if (params != null) {
            for (Map.Entry<String, String> e : params.entrySet()) {
                when(req.getParameter(e.getKey())).thenReturn(e.getValue());
            }
        }
        return req;
    }

    public static HttpServletRequest mockRequest(ServletContext ctx, User user, Map<String, String> params) {
        HttpSession session = user != null ? mockSession(user) : null;
        return mockRequest(ctx, session, params);
    }

    public static HttpServletResponse mockResponse() {
        return mock(HttpServletResponse.class);
    }

    public static Map<String, String> params(String... kv) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            m.put(kv[i], kv[i + 1]);
        }
        return m;
    }

    public static void injectField(Object target, String fieldName, Object value) throws Exception {
        Class<?> c = target.getClass();
        Field f = null;
        while (c != null) {
            try {
                f = c.getDeclaredField(fieldName);
                break;
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        if (f == null) {
            throw new NoSuchFieldException(fieldName);
        }
        f.setAccessible(true);
        f.set(target, value);
    }
}
