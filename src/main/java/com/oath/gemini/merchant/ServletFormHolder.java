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
        System.err.println("prepare content type=" + baseRequest.getContentType());
        System.err.println("prepare method=" + baseRequest.getMethod());
        
        super.prepare(baseRequest, request, response);
    }
}
