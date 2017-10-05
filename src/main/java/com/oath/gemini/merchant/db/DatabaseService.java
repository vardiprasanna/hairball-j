package com.oath.gemini.merchant.db;

import java.io.Serializable;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

@Singleton
public class DatabaseService {
    @Inject
    protected SessionFactory sessionFactory;

    public <T> Serializable save(T o) {
        Session session = sessionFactory.openSession();
        Transaction tx;
        Serializable id = null;

        try {
            tx = session.beginTransaction();
            id = session.save(o);
            tx.commit();
        } finally {
            if (session != null) {
                session.close();
            }
        }
        return id;
    }

    @SuppressWarnings("unchecked")
    public StoreAcctEntity findStoreAcctByAccessToken(String accessToken) {
        Session session = sessionFactory.openSession();

        try {
            Criteria criteria = session.createCriteria(StoreAcctEntity.class);
            criteria.add(Restrictions.eq("storeAccessToken", accessToken));
            List<StoreAcctEntity> list = criteria.list();
            return (list != null && list.size() == 1 ? list.get(0) : null);

        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public StoreSysEntity findStoreSysByDoman(String domain) {
        Session session = sessionFactory.openSession();

        try {
            Criteria criteria = session.createCriteria(StoreSysEntity.class);
            criteria.add(Restrictions.eq("domain", domain));
            List<StoreSysEntity> list = criteria.list();
            return (list != null && list.size() == 1 ? list.get(0) : null);

        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T findById(Class<T> entityClass, Integer id) {
        Session session = sessionFactory.openSession();

        try {
            Criteria criteria = session.createCriteria(entityClass);
            criteria.add(Restrictions.eq("id", id));
            List<T> list = criteria.list();
            return (list != null && list.size() == 1 ? list.get(0) : null);

        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
}
