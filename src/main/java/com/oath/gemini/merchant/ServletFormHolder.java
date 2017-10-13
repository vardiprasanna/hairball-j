package com.oath.gemini.merchant;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.UnavailableException;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;

public class ServletFormHolder extends ServletHolder {
    public ServletFormHolder(ServletContainer container) {
        super(container);
    }

    @Override
    protected void prepare(Request baseRequest, ServletRequest request, ServletResponse response)
            throws ServletException, UnavailableException {

        // Shopify passes a wrong media type at GET. Must terminate it to avoid 415 error code
        if ("GET".equals(baseRequest.getMethod()) && "application/x-www-form-urlencoded".equals(baseRequest.getContentType())) {
            baseRequest.setContentType("text/plain");
        }
        super.prepare(baseRequest, request, response);
    }
}
