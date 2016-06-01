package com.foofv.crawler.storage.mongo;

import org.bson.conversions.Bson;

import static com.mongodb.client.model.Filters.gt;
import static com.mongodb.client.model.Filters.gte;

/**
 * Created by msfenn on 29/09/15.
 */
public class GTE extends MongoFilterObject {

    private static GTE gte = new GTE();

    private GTE() {

    }

    public static GTE getInstance() {

        return gte;
    }

    @Override
    public Bson applyString(String operand1, String operand2) {

        return gte(operand1, operand2);
    }

    @Override
    public Bson applyInteger(String operand1, long operand2) {

        return gte(operand1, operand2);
    }

    @Override
    public Bson applyFloat(String operand1, double operand2) {

        return gte(operand1, operand2);
    }

    @Override
    public Bson apply(Bson... bsons) {

        return null;
    }
}
