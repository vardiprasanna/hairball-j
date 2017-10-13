package com.oath.gemini.merchant;

import com.oath.gemini.merchant.db.StoreAcctEntity;
import com.oath.gemini.merchant.db.StoreCampaignEntity;
import com.oath.gemini.merchant.db.StoreOwnerEntity;
import com.oath.gemini.merchant.db.StoreSysEntity;
import com.oath.gemini.merchant.shopify.ShopifyOnboardResource;
import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Iterator;
import javax.servlet.DispatcherType;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

public class App extends ResourceConfig {
    public static final String KEYSTORE_PASSWORD = "secret";
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
        config = AppConfiguration.getConfig();
        setFields(config);
    }

    private void setFields(Configuration cfg) {
        SessionFactory sessions = buildSessionFactory(cfg);

        super.register(new AppBinder(sessions));
        super.register(ShopifyOnboardResource.class);
    }

    public static App getInstance() {
        return instance;
    }

    private void initialize(int localPort) throws IOException, Exception {
        HttpConfiguration http_config = new HttpConfiguration();
        ServerConnector http = new ServerConnector(jetty, new HttpConnectionFactory(http_config));

        int port = localPort > 0 ? localPort : config.getInt("port");
        http.setPort(port);
        http.open();

        // If SSL termination is set up, don't listen to SSL port
        if (localPort <= 0) {
            configureSSL(http_config);
        }

        jetty.addConnector(http);
        HandlerCollection handlerCollection = new HandlerCollection();

        // handles js, css, and html resources
        ResourceHandler UIResourceHandler = buildUIResourceHandler();
        handlerCollection.addHandler(UIResourceHandler);

        ServletContextHandler servletContextHandler = new ServletContextHandler(jetty, "/", ServletContextHandler.SESSIONS);
        servletContextHandler.addServlet(new ServletHolder(new ServletContainer(this)), "/g/*");

        // Add the filter, and then use the provided FilterHolder to configure it
        FilterHolder corsFilter = new FilterHolder(CrossOriginFilter.class);
        corsFilter.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
        corsFilter.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "*");
        corsFilter.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "GET,POST,HEAD");
        corsFilter.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, "X-Requested-With,Content-Type,Accept,Origin");

        servletContextHandler.addFilter(corsFilter, "/*", EnumSet.of(DispatcherType.REQUEST));
        handlerCollection.addHandler(servletContextHandler);
        jetty.setHandler(handlerCollection);
    }

    private SessionFactory buildSessionFactory(Configuration cfg) {
        org.hibernate.cfg.Configuration config = new org.hibernate.cfg.Configuration();

        configureDB(cfg, config);
        StandardServiceRegistryBuilder regbuilder = new StandardServiceRegistryBuilder().applySettings(config.getProperties());
        return config.buildSessionFactory(regbuilder.build());
    }

    private void configureDB(Configuration cfg, org.hibernate.cfg.Configuration hcfg) {
        for (Iterator<String> keys = cfg.getKeys("hibernate"); keys.hasNext();) {
            String key = keys.next();
            hcfg.setProperty(key, cfg.getString(key));
        }
        if (cfg.getBoolean("db.ykeykey", false)) {
            // TODO: handle ykeykey
        }
        hcfg.addAnnotatedClass(StoreAcctEntity.class);
        hcfg.addAnnotatedClass(StoreSysEntity.class);
        hcfg.addAnnotatedClass(StoreOwnerEntity.class);
        hcfg.addAnnotatedClass(StoreCampaignEntity.class);
    }

    private void configureSSL(HttpConfiguration httpCfg) throws IOException {
        if (!config.getBoolean("ssl.enabled", false)) {
            return;
        }

        // Dev key is issued via this command: keytool -keystore keystore -alias jetty -genkey -keyalg RSA
        // a password for both keystore and key is "secret"
        String keystorePath = config.getString("ssl.keystore");
        File keystore = new File(keystorePath);

        if (!keystore.canRead()) {
            throw new RuntimeException("Keystore not readable");
        }

        int port = config.getInt("ssl.port");
        httpCfg.setSecureScheme("https");
        httpCfg.setSecurePort(port);

        HttpConfiguration cfg = new HttpConfiguration(httpCfg);
        cfg.addCustomizer(new SecureRequestCustomizer());

        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyStorePath(keystore.getAbsolutePath());
        sslContextFactory.setKeyStorePassword(KEYSTORE_PASSWORD);
        sslContextFactory.setKeyManagerPassword(KEYSTORE_PASSWORD);

        ServerConnector https = new ServerConnector(jetty, new SslConnectionFactory(sslContextFactory, "http/1.1"),
                new HttpConnectionFactory(cfg));
        https.setPort(port);
        https.open();

        jetty.addConnector(https);
    }

    private ResourceHandler buildUIResourceHandler() throws Exception {
        // Create the ResourceHandler. It is the object that will actually handle the request for a given file. It is
        // a Jetty Handler object so it is suitable for chaining with other handlers as you will see in other examples.
        ResourceHandler resourceHandler = new ResourceHandler();

        // Configure the ResourceHandler. Setting the resource base indicates where the files should be served out of.
        // In this example it is the current directory but it can be configured to anything that the jvm has access to.
        resourceHandler.setDirectoriesListed(true);
        resourceHandler.setWelcomeFiles(new String[] { "index.html" });
        ResourceCollection resources;

        /**
         * adding the resource handler only for api
         */
        resources = new ResourceCollection(new String[] { config.getString("asset_base.dir") });
        resourceHandler.setBaseResource(resources);
        return resourceHandler;
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
        int port = -1;

        // Heroku passes a local port via the command line option
        for (int i = 0; i < args.length; i++) {
            if (args[i].endsWith("-port") && (i < args.length - 1)) {
                port = Integer.parseInt(args[i + 1]);
                System.out.println("overriding port=" + port);
                break;
            }
        }

        App app = getInstance();
        app.initialize(port);
        app.start();
    }
}
