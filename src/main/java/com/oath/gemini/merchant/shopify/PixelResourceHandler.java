package com.oath.gemini.merchant.shopify;

import javax.annotation.Resource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.glassfish.jersey.server.ResourceConfig;

@Resource
@Path("")
public class PixelResourceHandler extends ResourceConfig {
    public PixelResourceHandler() {
        register(this);
    }

    @GET
    // @Path("sample.js")
    public Response build(@Context UriInfo info) {
        return Response.ok(DEMO_PIXEL_JS).build();
    }

    private final static String DEMO_PIXEL_JS = "(function(b){var a=function(f){var d,g;try{d=new Image();d.onerror=d.onload=function(){d.onerror=d.onload=null;d=null};d.src=f}catch(g){}},c=b.createElement(\\\"iframe\\\");c.style.cssText=\\\"height:0;width:0;frameborder:no;scrolling:no;sandbox:allow-scripts;display:none;\\\";c.src=\\\"https://s.yimg.com/rq/sbox/bv2.html\\\";b.body.appendChild(c);a(\\\"https://pr-bh.ybp.yahoo.com/fac-sync?cb=\\\"+Math.random())})(document);";
}
