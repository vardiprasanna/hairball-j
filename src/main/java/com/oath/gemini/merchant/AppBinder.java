package com.oath.gemini.merchant;

import com.oath.gemini.merchant.db.DatabaseService;
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
        bind(sessionFactory).to(SessionFactory.class);
        bind(DatabaseService.class).to(DatabaseService.class);
    }
}
