package com.oath.gemini.merchant;

import com.oath.gemini.merchant.db.DatabaseService;
import com.oath.gemini.merchant.ews.EWSAuthenticationService;
import com.oath.gemini.merchant.security.SigningService;
import org.apache.commons.configuration.Configuration;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.hibernate.SessionFactory;
import com.yahoo.bouncer.sso.CookieValidator;

/**
 * Bind instantces/classes so that they will be available for injection
 * 
 * @author tong on 10/1/2017
 */
public class AppInjectionBinder extends AbstractBinder {
    private final SessionFactory sessionFactory;
    private final CookieValidator cookieValidator;

    public AppInjectionBinder(SessionFactory sessionFactory, CookieValidator cookieValidator) {
        this.sessionFactory = sessionFactory;
        this.cookieValidator = cookieValidator;
    }

    @Override
    protected void configure() {
        // singleton binding
        bind(AppConfiguration.getConfig()).to(Configuration.class);
        bind(SigningService.class).to(SigningService.class);
        bind(sessionFactory).to(SessionFactory.class);
        bind(DatabaseService.class).to(DatabaseService.class);
        bind(EWSAuthenticationService.class).to(EWSAuthenticationService.class);
        bind(cookieValidator).to(CookieValidator.class);
    }
}
