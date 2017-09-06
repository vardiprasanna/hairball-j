package com.oath.gemini.merchant.shopify;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.apache.commons.configuration.Configuration;
import org.eclipse.jetty.client.HttpRequest;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.glassfish.jersey.server.ResourceConfig;

@Singleton
@Path("")
public class OAuthResource extends ResourceConfig {
    private Configuration config;

    public OAuthResource(Configuration config) {
        this.config = config;
        register(this);
    }

    /**
     * Redirect a user to Yahoo oauth server, and request the user's access permission
     */
    @GET
    @Path("signon")
    public Response signOn() throws UnsupportedEncodingException {
        StringBuilder buf = new StringBuilder();

        buf.append("https://api.login.yahoo.com/oauth2/request_auth?response_type=code&language=en-us");
        buf.append("&client_id=dj0yJmk9NEJVRHRaRnpWa09SJmQ9WVdrOVREQktiREUzTjJrbWNHbzlNQS0tJnM9Y29uc3VtZXJzZWNyZXQmeD1iYQ--");
        buf.append("&redirect_uri=").append(URLEncoder.encode("https://hairball.herokuapp.com/oauth/approval", "UTF-8"));

        Response.ResponseBuilder builder = Response.temporaryRedirect(URI.create(buf.toString()));
        builder.header("incognito", true);
        return builder.build();
    }

    /**
     * Oauth server redirects to here with the user's permission response
     */
    @POST
    @Path("approval")
    public Response approve(@Context HttpRequest req) {
        StringBuilder buf = new StringBuilder();
        HttpFields headers = req.getHeaders();

        buf.append("<div>");
        if (headers != null) {
            for (HttpField f : headers) {
                buf.append("<p>").append(f.getName()).append('=').append(f.getValue()).append("</p>");
            }
        }

        buf.append("</div><div>");
        try {
            String content = req.getContent().toString();
            buf.append(content);
        } catch (Exception e) {
            buf.append("content error: " + e.toString());
        }
        buf.append("</div>");
        return Response.ok(buf.toString()).build();
    }
}
