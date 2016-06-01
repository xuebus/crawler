package com.foofv.crawler.storage;

import com.foofv.crawler.similarity.structure.Pair;
import com.foofv.crawler.storage.mongo.MongoConstant;
import com.foofv.crawler.storage.mongo.SQLQueryParsedContext;
import com.foofv.crawler.storage.mongo.SQLQueryParserForMongo;
import com.mongodb.*;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.InsertManyOptions;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.*;

/**
 * Created by msfenn
 */
public class MongoStorage {

    private static Datastore datastore;
    private static MongoDatabase db;
    private static Set<Class<?>> primitiveTypeSet = createPrimitiveTypeSet();
    private static Set<Class<?>> collectionTypeSet = createCollectionTypeSet();
    private static List<Object> savedObjects;

    static {
        init();
    }

    private static Set<Class<?>> createPrimitiveTypeSet() {

        Set<Class<?>> primitiveTypeSet = new HashSet<Class<?>>();
        primitiveTypeSet.add(Boolean.class);
        primitiveTypeSet.add(Byte.class);
        primitiveTypeSet.add(Character.class);
        primitiveTypeSet.add(Short.class);
        primitiveTypeSet.add(Integer.class);
        primitiveTypeSet.add(Long.class);
        primitiveTypeSet.add(Float.class);
        primitiveTypeSet.add(Double.class);
        primitiveTypeSet.add(String.class);

        return primitiveTypeSet;
    }

    private static Set<Class<?>> createCollectionTypeSet() {

        Set<Class<?>> collectionTypeSet = new HashSet<Class<?>>();
        collectionTypeSet.add(List.class);
        collectionTypeSet.add(Set.class);
        collectionTypeSet.add(Map.class);

        return collectionTypeSet;
    }

    private static <T> boolean isCollectionType(Class<T> clazz) {

        for (Class<?> superClazz : collectionTypeSet) {
            if (superClazz.isAssignableFrom(clazz)) {
                return true;
            }
        }
        return false;
    }

    private static void init() {

        Properties properties = new Properties();
        InputStream is = MongoStorageManager.class.getResourceAsStream("/mongodb.properties");
        String host = "";
        int port = 0;
        String dbname = "";
        try {
            properties.load(is);
            host = properties.getProperty("host");
            port = Integer.valueOf(properties.getProperty("port"));
            dbname = properties.getProperty("dbname");
            is.close();
        } catch (IOException e) {
            // TODO: Log
            e.printStackTrace();
        }
        MongoClient client = new MongoClient(host, port);
        Morphia morphia = new Morphia();

        datastore = morphia.createDatastore(client, dbname);
        db = client.getDatabase(dbname);
        datastore.ensureIndexes();
    }

    private static <T, K> Query<T> findCaseInsensitive(Class<T> clazz, String property, K key) {

        Query<T> query = datastore.createQuery(clazz);
        Object value = key;
        if (key != null && key.getClass() == String.class) {
            value = Pattern.compile((String) key, Pattern.CASE_INSENSITIVE);
        }
        query.field(property).equal(value);

        return query;
    }

    public static <T, K> T getByKey(Class<T> clazz, K key) {

        return datastore.get(clazz, key);
    }

    public static <T, K> T getByKeyFiltered(Class<T> clazz, K key, String... projection) {

        return datastore.createQuery(clazz).filter("_id", key).retrievedFields(true, projection).get();
    }

    public static <T, F> T getByValue(Class<T> clazz, String property, F key) {

        Query<T> query = findCaseInsensitive(clazz, property, key);
        return query.get();
    }

    public static <T> long getCollectionSize(Class<T> clazz) {

        return datastore.getCount(clazz);
    }

    public static long getCollectionSize(String collName) {

        return datastore.getDB().getCollection(collName).count();
    }

    public static <T> List<T> getListLimited(Class<T> clazz, int offset, int limit, String... properties) {


        Query<T> query = datastore.createQuery(clazz).retrievedFields(true, properties).offset(offset).limit(limit);

        return query.asList();
    }

    public static <T, K> List<T> getListLimitedByValue(Class<T> clazz, String property, K value, int offset, int limit, String... properties) {

        Query<T> query = datastore.find(clazz, property, value, offset, limit).retrievedFields(true, properties);

        return query.asList();
    }

    public static <T, K> DBRef getDBReferenceByKey(Class<T> clazz, K keyValue, String projection) {

        return getDBReferenceByValue(clazz, "_id", keyValue, projection);
    }

    public static <T, K> DBRef getDBReferenceByValue(Class<T> clazz, String property, K value, String projection) {

        DBObject dbObj = datastore.getCollection(clazz).find(new BasicDBObject(property, new BasicDBObject("$eq", value)), new BasicDBObject(projection, 1)).one();
        DBRef dbRef = null;
        if (dbObj != null) {
            Object obj = dbObj.get(projection);
            if (obj instanceof BasicDBList)
                dbRef = (DBRef) ((BasicDBList) obj).get(0);
            else if (obj instanceof DBRef)
                dbRef = (DBRef) obj;
        }

        return dbRef;
    }

    public static <T, K> List<DBRef> getDBReferenceListByKey(Class<T> clazz, K keyValue, String projection) {

        return getDBReferenceListByValue(clazz, "_id", keyValue, projection);
    }

    public static <T, K> List<DBRef> getDBReferenceListByValue(Class<T> clazz, String property, K value, String projection) {

        DBObject dbObj = datastore.getCollection(clazz).find(new BasicDBObject(property, new BasicDBObject("$eq", value)), new BasicDBObject(projection, 1)).one();
        List<DBRef> dbRefList = null;
        if (dbObj != null) {
            Object obj = dbObj.get(projection);
            if (obj instanceof BasicDBList)
                dbRefList = (List<DBRef>) obj;
            else if (obj instanceof DBRef) {
                dbRefList = new ArrayList<>(1);
                dbRefList.add((DBRef) obj);
            }
        }

        return dbRefList;
    }

    public static <T> List<List<DBRef>> getDBReferenceListLimited(Class<T> clazz, String projection, int limit) {

        List<List<DBRef>> dbRefLists = null;
        DBCursor dbCursor = datastore.getCollection(clazz).find(null, new BasicDBObject(projection, 1)).limit(limit);
        if (dbCursor.count() > 0)
            dbRefLists = new ArrayList<>();
        while (dbCursor.hasNext()) {
            Object obj = dbCursor.next().get(projection);
            if (obj instanceof BasicDBList)
                dbRefLists.add((List<DBRef>) obj);
            else if (obj instanceof DBRef) {
                List<DBRef> dbRefList = new ArrayList<>(1);
                dbRefList.add((DBRef) obj);
                dbRefLists.add(dbRefList);
            }
        }

        return dbRefLists;
    }

    public static <T> List<T> getList(Class<T> clazz, String... projections) {

        Query<T> query = datastore.createQuery(clazz).retrievedFields(true, projections);
        return query.asList();
    }

    public static <T, V> List<T> getListByValue(Class<T> clazz, String property, V value, String... projections) {

        Query<T> query = datastore.find(clazz, property, value).retrievedFields(true, projections);
        return query.asList();
    }

    public static <T> List<T> getAllValueByField(Class<T> clazz, String property) {

        Query<T> query = datastore.createQuery(clazz).retrievedFields(true, property);
        return query.asList();
    }

    public static <T> List<T> getAll(Class<T> clazz) {

        Query<T> query = datastore.find(clazz);
        return query.asList();
    }

    private static <T> void saveEntity(T t, List<Object> savedObjects) throws MongoException {

        if (t == null)
            return;
        Class<?> clazz = t.getClass();
        if (t instanceof ObjectId || primitiveTypeSet.contains(clazz)) {
            return;
        } else if (isCollectionType(clazz)) {
            Collection<?> collection = (Collection<?>) t;
            for (Object object : collection) {
                saveEntity(object, savedObjects);
            }
        } else if (clazz.isArray()) {
            for (int i = 0; i < Array.getLength(t); ++i) {
                saveEntity(Array.get(t, i), savedObjects);
            }
        } else {
            try {
                for (Field field : clazz.getDeclaredFields()) {
                    field.setAccessible(true);
                    saveEntity(field.get(t), savedObjects);
                }
                while ((clazz = clazz.getSuperclass()) != Object.class) {
                    for (Field field : clazz.getDeclaredFields()) {
                        field.setAccessible(true);
                        saveEntity(field.get(t), savedObjects);
                    }
                }
                /*if (datastore.exists(t) == null)*/
                {
                    datastore.save(t);
                    savedObjects.add(t);
                }
            } catch (Exception e) {
                throw new MongoException(savedObjects, e);
            }
        }
    }

    public static <T> boolean saveEntity(T t) {

        boolean isSuccessful = false;
        if (t == null) {
            return isSuccessful;
        }
        try {
            List<Object> list = new ArrayList<>();
            saveEntity(t, list);
            savedObjects = list;
            isSuccessful = true;
        } catch (MongoException e) {
            deleteObjects(e.getObjects());
        }
        return isSuccessful;
    }

    private static List<String> getIdAnnotationFieldsName(List<Object> objects) {

        List<String> fieldsName = new ArrayList<>();
        for (Object object : objects) {
            Class<?> clazz = object.getClass();
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                if (field.getAnnotation(Id.class) != null)
                    fieldsName.add(field.getName());
            }
        }

        return fieldsName;
    }

    private static void deleteRedundantEntries(Map<Object, String> objectToPurge2IdNameMap, Map<Object, String> objectUpdated2IdNameMap) {

        boolean isNotFound = true;
        for (Object objectToPurge : objectToPurge2IdNameMap.keySet()) {
            isNotFound = true;
            for (Object objectUpdated : objectUpdated2IdNameMap.keySet()) {
                try {
                    String objectToPurgeIdFieldName = objectToPurge2IdNameMap.get(objectToPurge);
                    Field idFieldOfObjectToPurge = objectToPurge.getClass().getDeclaredField(objectToPurgeIdFieldName);
                    idFieldOfObjectToPurge.setAccessible(true);
                    Object idOfObjectToPurge = idFieldOfObjectToPurge.get(objectToPurge);

                    String objectUpdatedIdFieldName = objectUpdated2IdNameMap.get(objectUpdated);
                    Field idFieldOfObjectUpdated = objectUpdated.getClass().getDeclaredField(objectUpdatedIdFieldName);
                    idFieldOfObjectUpdated.setAccessible(true);
                    Object idOfObjectUpdated = idFieldOfObjectUpdated.get(objectUpdated);

                    if (idOfObjectToPurge.equals(idOfObjectUpdated)) {
                        isNotFound = false;
                        break;
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                }
            }
            if (isNotFound)
                datastore.delete(objectToPurge);
        }
    }

    private static void getObjectsUpdated(Object object, List<Object> objects) {

        Class<?> clazz = object.getClass();
        if (object instanceof ObjectId || primitiveTypeSet.contains(clazz)) {
            return;
        } else if (isCollectionType(clazz)) {
            Collection<?> collection = (Collection<?>) object;
            for (Object obj : collection)
                getObjectsUpdated(obj, objects);
        } else {
            try {
                for (Field field : clazz.getDeclaredFields()) {
                    field.setAccessible(true);
                    getObjectsUpdated(field.get(object), objects);
                }
                while ((clazz = clazz.getSuperclass()) != Object.class) {
                    for (Field field : clazz.getDeclaredFields()) {
                        field.setAccessible(true);
                        getObjectsUpdated(field.get(object), objects);
                    }
                }
                objects.add(object);
            } catch (Exception e) {
            }
        }
    }

    private static List<Object> getObjectsUpdated(Object[] values) {

        List<Object> objects = new LinkedList<>();
        for (Object object : values)
            getObjectsUpdated(object, objects);

        return objects;
    }

    private static void deleteRedundantEntries(List<Object> objectsToPurge, List<Object> objectsUpdated) {

        if (objectsToPurge == null)
            return;

        Map<Object, String> objectUpdated2IdNameMap = new HashMap<>();
        List<String> objectUpdatedFieldsName = getIdAnnotationFieldsName(objectsUpdated);
        if (objectsUpdated.size() != objectUpdatedFieldsName.size())
            return;
        for (int i = 0; i < objectsUpdated.size(); ++i) {
            objectUpdated2IdNameMap.put(objectsUpdated.get(i), objectUpdatedFieldsName.get(i));
        }

        Map<Object, String> objectToPurge2IdNameMap = new HashMap<>();
        List<String> objectToPurgeFieldsName = getIdAnnotationFieldsName(objectsToPurge);
        if (objectsToPurge.size() != objectToPurgeFieldsName.size())
            return;
        for (int i = 0; i < objectsToPurge.size(); ++i) {
            objectToPurge2IdNameMap.put(objectsToPurge.get(i), objectToPurgeFieldsName.get(i));
        }

        deleteRedundantEntries(objectToPurge2IdNameMap, objectUpdated2IdNameMap);
    }

    public static <T, KF> boolean updateFieldsByKey(Class<T> clazz, Object key, String[] properties, Object[] values) {

        Query<T> query = datastore.createQuery(clazz).field("_id").equal(key);
        T entity = query.retrievedFields(true, properties).get();
        List<Object> objectsToPurge = entity != null ? getEntriesAffected(entity, properties) : null;
        UpdateOperations<T> ops = datastore.createUpdateOperations(clazz);
        boolean isSuccessful = true;
        for (int i = 0; i < properties.length; ++i) {
            ops.set(properties[i], values[i]);
            if ((isSuccessful = saveEntity(values[i])) == false) {
                break;
            }
        }
        if (isSuccessful) {
            if ((datastore.update(query, ops, true).getWriteResult().getN() == 0)) {
                isSuccessful = false;
                deleteObjects(savedObjects); //TODO: Unless the reference count is 0, just subtract 1
            } else {
                List<Object> objectsUpdated = getObjectsUpdated(values);
                deleteRedundantEntries(objectsToPurge, objectsUpdated);
            }
        }
        savedObjects.clear();

        return isSuccessful;
    }

    public static <T, KF, F> void updateFieldByKey(Class<T> clazz, String keyProperty, KF key, String property, F value) {

        Query<T> query = datastore.createQuery(clazz).field(keyProperty).equal(key);
        UpdateOperations<T> ops = datastore.createUpdateOperations(clazz).disableValidation().set(property, value);
        datastore.update(query, ops);
    }

    public static <T, K> boolean updateEntityByKey(Class<T> clazz, String keyProperty, K key, T t) {

//        Query<T> query = datastore.createQuery(clazz).field(keyProperty).equal(key);
//        return datastore.updateFirst(query, t, true).getWriteResult().isUpdateOfExisting();
        return saveEntity(t);
    }

    public static <T, KF, F> void addFieldByKey(Class<T> clazz, String keyProperty, KF key, String property, F value) {

        Query<T> query = datastore.createQuery(clazz).field(keyProperty).equal(key);
        UpdateOperations<T> ops = datastore.createUpdateOperations(clazz).disableValidation().set(property, value);
        datastore.update(query, ops);
    }

    public static <T, F> boolean deleteByKey(Class<T> clazz, String property, F key) {

        Query<T> query = datastore.find(clazz, property, key);
        return datastore.delete(query).getN() != 0;
    }

    public static void deleteObjects(List<Object> objects) {

        for (Object object : objects) {
            if (object != null) {
                datastore.delete(object);
            }
        }
    }

    public static <T> boolean updateFieldsByKey(Class<T> clazz, Object[] keys, String[] properties, Object[][] values, boolean isEmbedded) {

        if (keys == null || properties == null || values == null || keys.length != values.length || properties.length != values[0].length)
            return false;
        boolean isSuccessful = true;
        if (isEmbedded) {
            for (int i = 0; i < keys.length; ++i) {
                Query<T> query = datastore.createQuery(clazz).field("_id").equal(keys[i]);
                UpdateOperations<T> ops = datastore.createUpdateOperations(clazz);
                for (int j = 0; j < properties.length; ++j) {
                    ops.set(properties[j], values[i][j]);
                }
                if (datastore.update(query, ops, true).getWriteResult().getN() == 0) {
                    isSuccessful = false;
                }
            }
        } else {
            for (int i = 0; i < keys.length; ++i) {
                if (updateFieldsByKey(clazz, keys[i], properties, values[i]) == false) {
                    isSuccessful = false;
                }
            }
        }

        return isSuccessful;
    }

    private static void getEntriesAffected(Object object, List<Object> objects) {

        if (object == null)
            return;
        Class<?> clazz = object.getClass();
        if (object instanceof ObjectId || primitiveTypeSet.contains(clazz)) {
            return;
        } else if (isCollectionType(clazz)) {
            Collection<?> collection = (Collection<?>) object;
            for (Object obj : collection)
                getEntriesAffected(obj, objects);
        } else {
            try {
                for (Field field : clazz.getDeclaredFields()) {
                    field.setAccessible(true);
                    getEntriesAffected(field.get(object), objects);
                }
                while ((clazz = clazz.getSuperclass()) != Object.class) {
                    for (Field field : clazz.getDeclaredFields()) {
                        field.setAccessible(true);
                        getEntriesAffected(field.get(object), objects);
                    }
                }
                objects.add(object);
//                datastore.delete(object);
            } catch (Exception e) {
            }
        }
    }

    private static List<Object> getEntriesAffected(Object object, String[] properties) {

        List<Object> objects = new LinkedList<>();
        for (String property : properties) {
            try {
                Field field = object.getClass().getDeclaredField(property);
                field.setAccessible(true);
                getEntriesAffected(field.get(object), objects);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return objects;
    }

    public static boolean saveEntities(List<?> entities, boolean isEmbedded) {

        if (entities == null || entities.size() == 0)
            return false;

        boolean isSuccessful = true;
        if (isEmbedded)
            try {
                datastore.save(entities);
            } catch (Exception e) {
                isSuccessful = false;
            }
        else {
            for (Object entity : entities) {
                isSuccessful &= saveEntity(entity);
            }
        }

        return isSuccessful;
    }

    public static boolean saveEntities(Object[] entities, boolean isEmbedded) {

        if (entities == null)
            return false;
        boolean isSuccessful = true;
        if (isEmbedded)
            try {
                datastore.save(entities);
            } catch (Exception e) {
                isSuccessful = false;
            }
        else {
            for (Object entity : entities) {
                isSuccessful &= saveEntity(entity);
            }
        }

        return isSuccessful;
    }

    /*Pure Mongo Driver, NO ORM*/

    private static Map<String, Boolean> isExisted(String... collectionNames) {

        if (collectionNames == null)
            return null;

        Map<String, Boolean> collectionExistedMap = new HashMap<>();
        db.listCollectionNames().forEach(new Block<String>() {

            @Override
            public void apply(String s) {

                for (String collectionName : collectionNames) {
                    if (s.equals(collectionName)) {
                        collectionExistedMap.put(collectionName, true);
                        break;
                    }
                }
            }
        });
        for (String collectionName : collectionNames) {
            if (!collectionExistedMap.containsKey(collectionName)) {
                collectionExistedMap.put(collectionName, false);
            }
        }

        return collectionExistedMap;
    }


    public static void createCollections(String... collectionNames) {

        if (collectionNames != null) {
            Map<String, Boolean> collectionExistedMap = isExisted(collectionNames);
            for (String collectionName : collectionNames) {
                if (!collectionExistedMap.get(collectionName))
                    db.createCollection(collectionName);
            }
        }
    }

    public static void saveDocuments(String collName, List<Document> docsToSave) {

        MongoCollection coll = db.getCollection(collName);
        try {
            coll.insertMany(docsToSave, new InsertManyOptions().ordered(false));
        } catch (com.mongodb.MongoException e) {
            e.printStackTrace();
        }
    }


    public static void saveDocument(String collName, Document doc) {

        MongoCollection coll = db.getCollection(collName);
        try {
            coll.insertOne(doc);
        } catch (com.mongodb.MongoException e) {
            e.printStackTrace();
        }
    }


    public static void saveMapObjs(String collName, List<Map<String, Object>> docs) {

        MongoCollection coll = db.getCollection(collName);
        List<Document> docsToSave = new ArrayList<>();
        for (Map<String, Object> doc : docs) {
            docsToSave.add(new Document(doc));
        }
        try {
            coll.insertMany(docsToSave, new InsertManyOptions().ordered(false));
        } catch (com.mongodb.MongoException e) {
            e.printStackTrace();
        }
    }

    public static Document getDocumentByKey(String collName, Object id) {

        MongoCollection coll = db.getCollection(collName);
        Document doc = (Document) coll.find(eq("_id", id)).first();

        return doc;
    }

    public static SQLQueryParsedContext getSQLQueryParsedResult(String sqlQueryStatement) {

        Map<String, Object> map = SQLQueryParserForMongo.extractQuerySQLStatement(sqlQueryStatement);
        String[] projections = (String[]) map.get(SQLQueryParserForMongo.SELECT);
        String collName = (String) map.get(SQLQueryParserForMongo.FROM);
        Map<Character, Object> operatorOperandsMap = (Map<Character, Object>) map.get(SQLQueryParserForMongo.WHERE);
        Bson conditions = null;
        if (operatorOperandsMap != null)
            conditions = parsePrefixExpression2MongoFilter(operatorOperandsMap);

        return new SQLQueryParsedContext(collName, conditions, projections);
    }

    public static List<Map<String, Object>> getDocumentList(SQLQueryParsedContext sqlQueryParsedContext, int offset, int limit) {

        String collName = sqlQueryParsedContext.collName;
        Bson conditions = sqlQueryParsedContext.conditions;
        String[] projections = sqlQueryParsedContext.projections;

        return getDocumentList(collName, conditions, projections, offset, limit);
    }

    public static List<Map<String, Object>> getDocumentList(String sqlQueryStatement) {

        SQLQueryParsedContext sqlQueryParsedContext = getSQLQueryParsedResult(sqlQueryStatement);

        return getDocumentList(sqlQueryParsedContext, 0, 0);
    }

    private static List<Map<String, Object>> getDocumentList(String collName, Bson conditions, String[] projections, int offset, int limit) {

        final List<Map<String, Object>> documentList = new ArrayList<>();
        MongoCollection coll = db.getCollection(collName);


        Bson projectionsBson = projections == null ? null : fields(excludeId(), include(projections));
        FindIterable<Document> iterable;


        if (conditions == null)
            iterable = coll.find();
        else
            iterable = coll.find(conditions);

        iterable.skip(offset).limit(limit).projection(projectionsBson).forEach(new Block<Document>() {

            @Override
            public void apply(Document doc) {

                documentList.add(doc);
            }
        });

        return documentList;
    }

    public static Bson parsePrefixExpression2MongoFilter(Map<Character, Object> operatorOperandsMap) {

        Pair<Object> operandPair = null;
        char operator = '\0';
        Bson conditionsBson1;
        Bson conditionsBson2;
        Bson conditionsBson;

        for (Character ch : operatorOperandsMap.keySet()) {
            operator = ch;
            if (ch == '\0') {
                conditionsBson = getMongoSimpleConditionExpressionFilter((String) operatorOperandsMap.get(ch));
                return conditionsBson;
            } else {
                operandPair = (Pair) operatorOperandsMap.get(ch);
            }
            break;
        }

        if (operandPair.first.getClass() == String.class && operandPair.second.getClass() == String.class) {

            conditionsBson1 = getMongoSimpleConditionExpressionFilter((String) operandPair.first);
            conditionsBson2 = getMongoSimpleConditionExpressionFilter((String) operandPair.second);

        } else if (operandPair.first.getClass() != String.class && operandPair.second.getClass() != String.class) {

            conditionsBson1 = parsePrefixExpression2MongoFilter((Map<Character, Object>) operandPair.first);
            conditionsBson2 = parsePrefixExpression2MongoFilter((Map<Character, Object>) operandPair.second);

        } else if (operandPair.first.getClass() != String.class) {

            conditionsBson1 = parsePrefixExpression2MongoFilter((Map<Character, Object>) operandPair.first);
            conditionsBson2 = getMongoSimpleConditionExpressionFilter((String) operandPair.second);

        } else {

            conditionsBson1 = getMongoSimpleConditionExpressionFilter((String) operandPair.first);
            conditionsBson2 = parsePrefixExpression2MongoFilter((Map<Character, Object>) operandPair.second);
        }

        conditionsBson = MongoConstant.operatorFilterMap.get("" + operator).apply(conditionsBson1, conditionsBson2);

        return conditionsBson;
    }

    private static Bson getMongoSimpleConditionExpressionFilter(String simpleConditionExpression) {

        String[] operands = simpleConditionExpression.split(MongoConstant.COMPARATOR_REGEX);
        if (operands.length != 2) {
            //TODO: log
            return null;
        }
        String comparator = simpleConditionExpression.replace(operands[0], "").replace(operands[1], "").trim();

        return MongoConstant.operatorFilterMap.get(comparator).apply(operands[0].trim(), operands[1].trim());
    }

    static class Addr {

        @Id
        String id;
        String addr;

        public Addr() {
        }

        public Addr(String id, String addr) {
            this.id = id;
            this.addr = addr;
        }
    }

    static class Student {
        @Id
        String id;
        String name;
        int age;
        @Reference
        Addr addr;

        public Student() {
        }

        public Student(String id, String name, int age, Addr addr) {
            this.id = id;
            this.name = name;
            this.age = age;
            this.addr = addr;
        }
    }

    public static void main(String[] args) {

        String sqlQuery = "select name,fullName from DistrictInfo";// or _id=12000.0
        System.out.println(getDocumentList(getSQLQueryParsedResult(sqlQuery), 3224, 3));

//        MongoCollection<Document> coll = db.getCollection("DistrictInfo");
//
//        coll.find(eq("_id", "120000")).forEach(new Block<Document>() {
//
//            @Override
//            public void apply(Document doc) {
//
//                System.out.println(doc);
//            }
//        });

//        System.out.println(iterable.first());
//        Document doc = (Document) (coll.find(and(or(eq("_id", "654326"), eq("_id", "110000")), gte("subDistricts", 0))).projection(fields(include("name", "fullName"), excludeId())).skip(1).first());
//        Document doc = (Document) (coll.find(/*eq("_id", "110000")*/).projection(fields(include("name", "fullName"), excludeId())));
//        System.out.println(doc);
//        System.out.println(getDocumentList("DistrictInfo", null, null/*new String[]{"name", "fullName"}*/));

//        System.out.println(getDBReferenceByKey(TakeoutMerchant.class, "waimai.baidu.com-5EC023A1D19747678E5D8CE7214EC48B-1", "menus"));
//        System.out.println(getDBReferenceListByKey(TakeoutMerchant.class, "waimai.baidu.com-5EC023A1D19747678E5D8CE7214EC48B-1", "menus"));
//        System.out.println(getDBReferenceListLimited(TakeoutMerchant.class, "menus", 2));

//        datastore.save(new Addr("aid_1","USA"));
//        datastore.save(new Student("id-1", "bill", 40, new Addr("aid_1", "USA")));
//        Object obj = datastore.getDB().getCollection("Student").find(null, new BasicDBObject("addr", 1)).one();
//        DBRef key = (DBRef) datastore.getDB().getCollection("Student").find(null, new BasicDBObject("addr", "")).one().get("addr");
//        System.out.println(datastore.getDB().getCollection("Student").find(null, new BasicDBObject("addr", "")).one());
//        System.out.println(datastore.find(Addr.class, "_id", key.getId()).get().addr);

//        System.out.println(getByKey(DistrictInfo.class, "110000"));
//        System.out.println(getListByValue(TakeoutMerchant.class, "originSite", "waimai.meituan.com", "originSite").size());

//        createCollections("coll1", "coll2");
//        List<Map<String, Object>> list = new ArrayList<>();
//        list.add(new Document("_id", "id_2").append("name", "d:\\jobs").append("age", "60").append("country", "/USA"));
//        saveDocuments("Student", list);
//        System.out.println(getDocumentByKey("Student", "id_1"));

//        System.out.println(getCollectionSize(AddressInfo.class));
//
//        for (int i = 0; i < 12; ++i) {
//
//            for (AddressInfo info : getListLimited(AddressInfo.class, i * 20, 20)) {
//                System.out.println(info.address + "\t" + info.latitude);
//            }
//            System.out.println("\n");
//        }

//        ObjectId[] ids = {new ObjectId("55b72cde6fe44368c2ec9d0b"), new ObjectId("55b72cde6fe44368c2ec9d0c")};
//        String[] properties = {"name", "age"};
//        Object[][] values = {{"BILL", 1}, {"JOBS", 2}};
//        updateFieldsByKey(User.class, ids, properties, values, true);
//
//        System.out.println(values instanceof Object[][]);

        // Address address1 = new Address("USA");
        // Address address2 = new Address("CHINA");
        // A a = new A();
        // a.addAddress(address1);
        // a.addAddress(address2);
        //
        // B b = new B();
        // b.addAddress(address1);
        // b.addAddress(address2);
        //
        // saveEntity(a);
        // saveEntity(b);
        //
        // // A a = new A();
        // // // a.name = "AA";
        // // // a.number = 2501;
        // // a.addAddress(new Address("UK"));
        // // saveEntity(a);
        // // addFieldByKey(A.class, "name", "A", "bool", false);
        // // updateByKey(A.class, "name", "AA", "number", 1234);
        // // updateByKey(A.class, "name", "A", "number", 12);
        // // System.out.println(getByKey(A.class, "name", "AA").number);
        // // System.out.println(getByKey(A.class, "name", "a").number);
        // // System.out.println(getByKey(A.class, "name", "A").companyList);
        // // System.out.println(getByKey(A.class, "name",
        // "A").address.country);
        // // System.out.println(getAll(A.class));
        // // deleteByKey(Address.class, "country", "usa");


        User user = new User();
        ObjectId id = new ObjectId("55b72cde6fe44368c2ec9d0c");
        user.id = id;
        user.name = "jobs";
        user.age = 251;

        Company company1 = new Company();
        company1.name = "MICROSOFT";
        company1.stuffs = 11;
        company1.age = 30;
        Company company2 = new Company();
        company2.name = "GOOGLE";
        company2.stuffs = 10;
        company2.age = 17;

        Address address1 = new Address();
        address1.country = "USA";
        address1.company = new ArrayList<>();
        address1.company.add(company1);
        address1.company.add(company2);

        Company company3 = new Company();
        company3.name = "Baidu";
        company3.stuffs = 8;
        company3.age = 16;
        Company company4 = new Company();
        company4.name = "Ali";
        company4.stuffs = 9;
        company4.age = 16;

        Address address2 = new Address();
        address2.country = "CHINA";
        address2.company = new ArrayList<>();
        address2.company.add(company3);
        address2.company.add(company4);

        user.address = new ArrayList<Address>();
        user.address.add(address1);
        user.address.add(address2);

//        updateFieldByKey(User.class, "_id", id, "refCount", 3);
//        datastore.delete(user);
//        System.out.println(saveEntity(user));
//        System.out.println(updateFieldsByKey(User.class, id, new String[]{"name", "age", "address"}, new Object[]{"BILL", 1, user.address}));
//        Object[][] values = new Object[1][3];
//        values[0][0] = "BILL";
//        values[0][1] = 1;
//        values[0][2] = user.address;
//        deleteObjects(objectsToPurge, new String[]{"name", "age", "address"}, values);
////        System.out.println(updateEntityByKey(User.class, "_id", id, user));
//        for (Company com : getList(Company.class, "age", 16)) {
//
//            System.out.println("name:" + com.name);
//            System.out.println("age:" + com.age);
//            System.out.println("stuffs:" + com.stuffs);
//            System.out.println();
//        }


//        datastore.delete(user.address.get(0).company.get(0));
//        datastore.delete(user.address.get(0).company.get(1));
//        datastore.delete(user.address.get(0));

//        user = getByKey(User.class, id);
//        System.out.println(user.address.size());
//        System.out.println(user.name);
//        System.out.println(user.age);
//        System.out.println(getByKey(User.class, id).address.get(0).country);
//        System.out.println(getByKey(User.class, id).address.get(1).country);
//        System.out.println(getByKey(User.class, id).address.get(0).company.get(0).stuffs);
//        System.out.println(getByKey(User.class, id).address.get(0).company.get(1).stuffs);
//        System.out.println(getByKey(User.class, id).address.get(1).company.get(0).stuffs);
//        System.out.println(getByKey(User.class, id).address.get(1).company.get(1).stuffs);
    }
}
