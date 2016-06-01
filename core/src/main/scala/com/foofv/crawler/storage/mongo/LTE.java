package com.foofv.crawler.storage.mongo;

import org.bson.conversions.Bson;

import static com.mongodb.client.model.Filters.lte;

/**
 * Created by msfenn on 29/09/15.
 */
public class LTE extends MongoFilterObject {

    private static LTE lte = new LTE();

    private LTE() {

    }

    public static LTE getInstance() {

        return lte;
    }

    @Override
    public Bson applyString(String operand1, String operand2) {

        return lte(operand1, operand2);
    }

    @Override
    public Bson applyInteger(String operand1, long operand2) {

        return lte(operand1, operand2);
    }

    @Override
    public Bson applyFloat(String operand1, double operand2) {

        return lte(operand1, operand2);
    }

    @Override
    public Bson apply(Bson... bsons) {

        return null;
    }
}
