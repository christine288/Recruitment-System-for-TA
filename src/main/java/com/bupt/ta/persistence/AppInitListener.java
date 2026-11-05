package com.bupt.ta.persistence;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Initializes {@link ServiceFactory} from {@code WEB-INF/data} when the web application starts.
 *
 * <p>Resolves the data directory and CV upload folder from the servlet context, ensures directories
 * exist, and registers the factory for servlet {@code init()} methods. On shutdown, removes the
 * context attribute (no explicit file flush — JSON writes are synchronous per request).
 *
 * @see ServiceFactory#SERVLET_CONTEXT_KEY
 */
public class AppInitListener implements ServletContextListener {

    /** Creates {@link ServiceFactory} and stores it in the servlet context. */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            ServiceFactory factory = ServiceFactory.fromServletContext(sce.getServletContext());
            sce.getServletContext().setAttribute(ServiceFactory.SERVLET_CONTEXT_KEY, factory);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Failed to initialize ServiceFactory", e);
        }
    }

    /** Removes the factory attribute on webapp shutdown. */
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        sce.getServletContext().removeAttribute(ServiceFactory.SERVLET_CONTEXT_KEY);
    }
}
