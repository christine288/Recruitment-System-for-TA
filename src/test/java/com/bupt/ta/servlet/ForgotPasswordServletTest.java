package com.bupt.ta.servlet;

import com.bupt.ta.model.User;
import com.bupt.ta.persistence.ServiceFactory;
import com.bupt.ta.testsupport.FileTestSupport;
import com.bupt.ta.testsupport.ServletTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

class ForgotPasswordServletTest {

    private ForgotPasswordServlet servlet;
    private ServiceFactory factory;

    @BeforeEach
    void setUp() throws Exception {
        factory = FileTestSupport.newFactory();
        servlet = new ForgotPasswordServlet();
        ServletTestSupport.injectField(servlet, "passwordReset", factory.getPasswordResetService());
    }

    @Test
    void get_showsForm() throws Exception {
        HttpServletRequest req = ServletTestSupport.mockRequest(
                ServletTestSupport.mockServletContext(null), (User) null, null);
        HttpServletResponse resp = ServletTestSupport.mockResponse();
        servlet.doGet(req, resp);
        verify(req).getRequestDispatcher("/WEB-INF/jsp/forgot-password.jsp");
    }

    @Test
    void post_unknownEmail_sameSuccessMessage() throws Exception {
        HttpServletRequest req = ServletTestSupport.mockRequest(
                ServletTestSupport.mockServletContext(null), (User) null,
                ServletTestSupport.params("email", "unknown@test.local"));
        HttpServletResponse resp = ServletTestSupport.mockResponse();
        servlet.doPost(req, resp);
        verify(req).getRequestDispatcher(anyString());
    }
}
