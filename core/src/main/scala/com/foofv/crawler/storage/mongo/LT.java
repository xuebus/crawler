package com.foofv.crawler.storage.mongo;

import org.bson.conversions.Bson;

import static com.mongodb.client.model.Filters.lt;

/**
 * Created by msfenn on 29/09/15.
 */
public class LT extends MongoFilterObject {

    private static LT lt = new LT();

    private LT() {

    }

    public static LT getInstance() {

        return lt;
    }

    @Override
    public Bson applyString(String operand1, String operand2) {

        return lt(operand1, operand2);
    }

    @Override
    public Bson applyInteger(String operand1, long operand2) {

        return lt(operand1, operand2);
    }

    @Override
    public Bson applyFloat(String operand1, double operand2) {

        return lt(operand1, operand2);
    }

    @Override
    public Bson apply(Bson... bsons) {

        return null;
    }
}
