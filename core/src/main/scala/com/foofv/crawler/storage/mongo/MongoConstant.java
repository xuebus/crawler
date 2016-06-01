package com.foofv.crawler.storage.mongo;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by msfenn on 29/09/15.
 */
public class MongoConstant {

    public static final String COMPARATOR_REGEX = "\\s*>=\\s*|\\s*>\\s*|\\s*<=\\s*|\\s*<\\s*|\\s*=\\s*|\\s*!=\\s*";
    public static final String AND_OR_REGEX = "\\s+and\\s+|\\s+or\\s+|\\s+AND\\s+|\\s+OR\\s+";

    public static final Map<String, MongoFilterObject> operatorFilterMap = new HashMap<>();

    public static final String EQUAL = "=";
    public static final String NOT_EQUAL = "!=";
    public static final String LESS_THAN = "<";
    public static final String LESS_THAN_EQUAL = "<=";
    public static final String GREATER_THAN = ">";
    public static final String GREATER_THAN_EQUAL = ">=";

    public static final String AND = "&";
    public static final String OR = "|";

    static {
        operatorFilterMap.put(EQUAL, EQ.getInstance());
        operatorFilterMap.put(NOT_EQUAL, NE.getInstance());
        operatorFilterMap.put(LESS_THAN, LT.getInstance());
        operatorFilterMap.put(LESS_THAN_EQUAL, LTE.getInstance());
        operatorFilterMap.put(GREATER_THAN, GT.getInstance());
        operatorFilterMap.put(GREATER_THAN_EQUAL, GTE.getInstance());

        operatorFilterMap.put(AND, com.foofv.crawler.storage.mongo.AND.getInstance());
        operatorFilterMap.put(OR, com.foofv.crawler.storage.mongo.OR.getInstance());
    }
}
