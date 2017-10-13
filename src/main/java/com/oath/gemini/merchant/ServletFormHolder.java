package com.oath.gemini.merchant;

import java.io.IOException;
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
    public void handle(Request baseRequest, ServletRequest request, ServletResponse response)
            throws UnavailableException, ServletException, IOException {
        System.err.println("handle content type=" + baseRequest.getContentType());
        System.err.println("handle method=" + baseRequest.getMethod());

        super.handle(baseRequest, request, response);
    }

    @Override
    protected void prepare(Request baseRequest, ServletRequest request, ServletResponse response)
            throws ServletException, UnavailableException {

        if ("GET".equals(baseRequest.getMethod()) && "application/x-www-form-urlencoded".equals(baseRequest.getContentType())) {
            System.err.println("prepare content type before=" + baseRequest.getContentType());
            System.err.println("prepare content type before=" + request.getContentType());
            
            baseRequest.setContentType("text/plain");

            System.err.println("prepare content type after=" + baseRequest.getContentType());
            System.err.println("prepare content type after=" + request.getContentType());
        }

        super.prepare(baseRequest, request, response);
    }
}
