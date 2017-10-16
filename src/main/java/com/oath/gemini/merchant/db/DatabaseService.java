package com.oath.gemini.merchant.db;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.sql.Date;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

/**
 * @author tong on 10/1/2017
 */
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

    public <T> void update(T o) {
        Session session = sessionFactory.openSession();
        Transaction tx;

        try {
            tx = session.beginTransaction();
            session.update(o);
            tx.commit();
        } finally {
            if (session != null) {
                session.close();
            }
        }
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
    public StoreCampaignEntity findStoreCampaignById(long id) {
        Session session = sessionFactory.openSession();

        try {
            Criteria criteria = session.createCriteria(StoreCampaignEntity.class);
            criteria.add(Restrictions.eq("campaignId", id));
            List<StoreCampaignEntity> list = criteria.list();
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
    public <T> T findByAny(T entity) throws Exception {
        Criterion criterion = null;
        Field[] fields = entity.getClass().getDeclaredFields();

        for (Field f : fields) {
            if (!Date.class.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                Object value = f.get(entity);
                String name = f.getName();

                if (value != null) {
                    Criterion c = Restrictions.eq(name, value);
                    criterion = (criterion == null ? c : Restrictions.or(criterion, c));
                }
            }
        }
        if (criterion == null) {
            return null;
        }

        Session session = sessionFactory.openSession();
        try {
            Criteria criteria = session.createCriteria(entity.getClass());
            List<T> list = criteria.add(criterion).list();
            return (!list.isEmpty() ? list.get(0) : null);

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
            return (T) criteria.uniqueResult();

        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T findByAcctId(Class<T> entityClass, Integer id) {
        Session session = sessionFactory.openSession();

        try {
            Criteria criteria = session.createCriteria(entityClass);
            criteria.add(Restrictions.eq("storeAcctId", id));
            return (T) criteria.uniqueResult();

        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> listAll(Class<T> entity) {
        Session session = sessionFactory.openSession();
        try {
            Criteria criteria = session.createCriteria(entity);
            return criteria.list();
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    /**
     * Note: this is HSQLDB specific backup. It is used only during the development
     */
    public void backup(String directory) {
        Session session = sessionFactory.openSession();
        try {
            if (!directory.endsWith("/")) {
                directory += "/";
            }
            Query q = session.createSQLQuery("BACKUP DATABASE TO '" + directory + "' BLOCKING");
            q.executeUpdate();
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    public <T> T replaceIfDummy(T entity, String fieldName, String replacingBy) {
        try {
            Field field = entity.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(entity);

            if ((value instanceof String) && ((String) value).contains("dummy")) {
                field.set(entity, replacingBy);
            }
        } catch (Exception e) {
            // ignored
        }
        return entity;
    }
}
