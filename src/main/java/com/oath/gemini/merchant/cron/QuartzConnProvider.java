package com.oath.gemini.merchant.cron;

import java.sql.Connection;
import java.sql.SQLException;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionImplementor;
import org.quartz.utils.ConnectionProvider;

/**
 * @author tong on 10/1/2017
 */
public class QuartzConnProvider implements ConnectionProvider {
    public static SessionFactory sessionFactory;

    @Override
    public Connection getConnection() {
        try {
            return ((SessionImplementor) sessionFactory).getJdbcConnectionAccess().obtainConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void initialize() throws SQLException {
        // TODO Auto-generated method stub
    }

    @Override
    public void shutdown() throws SQLException {
        // TODO Auto-generated method stub
    }
}
