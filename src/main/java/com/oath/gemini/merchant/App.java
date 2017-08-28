package com.oath.gemini.merchant;

import java.io.File;
import java.io.IOException;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.hibernate.SessionFactory;
import com.oath.gemini.merchant.shopify.OnboardResource;

public class App extends ResourceConfig {
    private final Configuration config;
    private final Server jetty = new Server();
    private final static App instance;

    static {
        try {
            instance = new App();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private App() throws ConfigurationException {
        config = loadConfiguration();
        setFields(config, null, null);
    }

    protected void setFields(Configuration cfg, final SessionFactory sessions, final SessionFactory localSessions) {
        super.register(OnboardResource.class);
    }

    public static App getInstance() {
        return instance;
    }

    public void initialize() throws IOException, Exception {
        HttpConfiguration http_config = new HttpConfiguration();
        ServerConnector http = new ServerConnector(jetty, new HttpConnectionFactory(http_config));

        int port = config.getInt("port", 4080);
        http.setPort(port);
        http.open();

        jetty.addConnector(http);
        HandlerCollection handlerCollection = new HandlerCollection();
        // ResourceHandler UIResourceHandler = buildUIResourceHandler();

        ServletContextHandler handler = new ServletContextHandler(jetty, "/", ServletContextHandler.SESSIONS);
        handler.addServlet(new ServletHolder(new ServletContainer(this)), "/API/V1/*");
        handlerCollection.addHandler(handler);
        jetty.setHandler(handlerCollection);
    }

    private Configuration loadConfiguration() throws ConfigurationException {
        final String configDir = System.getProperty("config.dir", "target/conf");
        return new PropertiesConfiguration(new File(configDir, "application.properties"));
    }

    public void start() throws Exception {
        jetty.start();
        jetty.join();
    }

    public void stop() throws Exception {
        jetty.stop();
        jetty.join();
    }

    public static void main(String[] args) throws Exception {
        App app = getInstance();
        app.initialize();
        app.start();
    }
}
