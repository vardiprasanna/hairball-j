package com.oath.gemini.merchant;

import com.oath.gemini.merchant.db.DatabaseService;
import org.apache.commons.configuration.Configuration;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.hibernate.SessionFactory;

public class AppBinder extends AbstractBinder {
    private final SessionFactory sessionFactory;

    public AppBinder(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    protected void configure() {
        // singleton binding
        bind(AppConfiguration.getConfig()).to(Configuration.class);
        bind(sessionFactory).to(SessionFactory.class);
        bind(DatabaseService.class).to(DatabaseService.class);
    }
}
