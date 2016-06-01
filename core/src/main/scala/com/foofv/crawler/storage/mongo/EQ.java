package com.foofv.crawler.storage.mongo;

import org.bson.conversions.Bson;

import static com.mongodb.client.model.Filters.eq;

/**
 * Created by msfenn on 29/09/15.
 */
public class EQ extends MongoFilterObject {

    private static EQ eq = new EQ();

    private EQ() {

    }

    public static EQ getInstance() {

        return eq;
    }

    @Override
    public Bson applyString(String operand1, String operand2) {

        return eq(operand1, operand2);
    }

    @Override
    public Bson applyInteger(String operand1, long operand2) {

        return eq(operand1, operand2);
    }

    @Override
    public Bson applyFloat(String operand1, double operand2) {

        return eq(operand1, operand2);
    }

    @Override
    public Bson apply(Bson... bsons) {

        return null;
    }
}
