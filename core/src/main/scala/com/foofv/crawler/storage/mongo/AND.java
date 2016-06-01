package com.foofv.crawler.storage.mongo;

import org.bson.conversions.Bson;

import static com.mongodb.client.model.Filters.*;

/**
 * Created by msfenn on 29/09/15.
 */
public class AND extends MongoFilterObject {

    private static AND and = new AND();

    private AND() {

    }

    public static AND getInstance() {

        return and;
    }

    @Override
    public Bson applyString(String operand1, String operand2) {

        return null;
    }

    @Override
    public Bson applyInteger(String operand1, long operand2) {

        return null;
    }

    @Override
    public Bson applyFloat(String operand1, double operand2) {

        return null;
    }

    @Override
    public Bson apply(Bson... bsons) {

        Bson bson = and(bsons);

        return bson;
    }
}
