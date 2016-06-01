package com.foofv.crawler.storage.mongo;

import org.bson.conversions.Bson;

/**
 * Created by msfenn on 12/10/15.
 */
public class SQLQueryParsedContext {

    public String collName;
    public Bson conditions;
    public String[] projections;

    public SQLQueryParsedContext() {
    }

    public SQLQueryParsedContext(String collName, Bson conditions, String[] projections) {

        this.collName = collName;
        this.conditions = conditions;
        this.projections = projections;
    }
}
