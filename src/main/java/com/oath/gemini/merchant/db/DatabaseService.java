package com.oath.gemini.merchant.db;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.hsqldb.lib.tar.DbBackupMain;
import org.hsqldb.lib.tar.TarMalformatException;
import lombok.extern.slf4j.Slf4j;

/**
 * @author tong on 10/1/2017
 */
@Slf4j
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

    public <T> void delete(T o) {
        Session session = sessionFactory.openSession();
        Transaction tx;

        try {
            tx = session.beginTransaction();
            session.delete(o);
            tx.commit();
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    public <T> void add(T o) {
        Session session = sessionFactory.openSession();
        Transaction tx;

        try {
            tx = session.beginTransaction();
            session.save(o);
            tx.commit();
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public StoreAcctEntity findStoreAcctByDomain(String domain) {
        Session session = sessionFactory.openSession();

        try {
            Criteria criteria = session.createCriteria(StoreAcctEntity.class);
            criteria.add(Restrictions.eq("domain", domain));
            List<StoreAcctEntity> list = criteria.list();
            return (list != null && list.size() == 1 ? list.get(0) : null);

        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public StoreCampaignEntity findStoreCampaignByStoreAccountID(long id) {
        Session session = sessionFactory.openSession();

        try {
            Criteria criteria = session.createCriteria(StoreCampaignEntity.class);
            criteria.add(Restrictions.eq("storeAcctId", id));
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

    public <T> T findByAny(T entity) throws Exception {
        List<T> list = findAllByAny(entity);
        return (!list.isEmpty() ? list.get(0) : null);
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> findAllByAny(T entity) throws Exception {
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
            return criteria.add(criterion).list();

        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T findByEntityId(Class<T> entityClass, Integer id) {
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

    /**
     * Note: this is HSQLDB specific restore. It is used only during the development
     */
    public void restore(String directory) throws IOException {
        // extract application path
        String appPath = System.getProperty("user.dir");
        File f1 = new File(appPath + "/db/dev.lobs");
        File f2 = new File(appPath + "/db/dev.properties");
        File f3 = new File(appPath + "/db/dev.script");
        // delete existing db files to restore them from backup
        if (f1.delete()) {
            log.info("Able to delete file dev.lobs");
        }
        if (f2.delete()) {
            log.info("Able to delete file dev.properties");
        }
        if (f3.delete()) {
            log.info("Able to delete file dev.script");
        }

        // Restoring the backed up files
        try {
            if (!directory.endsWith("/")) {
                directory += "/";
            }
            DbBackupMain.main(new String[] { "--extract", directory, appPath + "/db/" });
        } catch (TarMalformatException e) {
            e.printStackTrace();
        }
    }

    /**
     * Allow to replace an entity's field value provided it is null or it contains a "dummy"
     */
    public <T> T replaceIfDummyOrBlank(T entity, String fieldName, Object targetValue) {
        try {
            Field field = entity.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(entity);

            if (value == null || (value instanceof String) && ((String) value).contains("dummy")) {
                field.set(entity, targetValue);
            }
        } catch (Exception e) {
            // ignored
        }
        return entity;
    }

    /**
     * Copy named properties from source to target except those excluded fields
     */
    @SuppressWarnings(value = { "unchecked", "rawtypes" })
    public static <T> boolean copyNonNullProperties(T targetEntity, T sourceEntity, String... excludedFields) throws Exception {
        List<String> excluded = new ArrayList<>(Arrays.asList("class", "createdDate", "updatedDate", "id"));

        if (excludedFields != null) {
            for (String ef : excludedFields) {
                excluded.add(ef);
            }
        }

        Map<String, ?> sourcePropertyMap = PropertyUtils.describe(sourceEntity);
        boolean isCopied = false;

        for (Map.Entry<String, ?> e : sourcePropertyMap.entrySet()) {
            String propName = e.getKey();
            Object newValue = e.getValue();

            if (newValue != null && !excluded.contains(propName)) {
                Object oldValue = PropertyUtils.getProperty(targetEntity, propName);
                if (oldValue == null) {
                    continue;
                }

                boolean isDifferent = false;
                if (newValue != null && oldValue instanceof Comparable) {
                    isDifferent = (((Comparable) oldValue).compareTo((Comparable) newValue) != 0);
                } else {
                    isDifferent = !oldValue.equals(newValue);
                }
                if (isDifferent) {
                    PropertyUtils.setProperty(targetEntity, propName, newValue);
                    isCopied = true;
                }
            }
        }
        return isCopied;
    }
}
