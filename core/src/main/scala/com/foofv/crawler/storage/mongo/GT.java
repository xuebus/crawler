package com.foofv.crawler.storage.mongo;

import org.bson.conversions.Bson;

import static com.mongodb.client.model.Filters.gt;

/**
 * Created by msfenn on 29/09/15.
 */
public class GT extends MongoFilterObject {

    private static GT gt = new GT();

    private GT() {

    }

    public static GT getInstance() {

        return gt;
    }

    @Override
    public Bson applyString(String operand1, String operand2) {

        return gt(operand1, operand2);
    }

    @Override
    public Bson applyInteger(String operand1, long operand2) {

        return gt(operand1, operand2);
    }

    @Override
    public Bson applyFloat(String operand1, double operand2) {

        return gt(operand1, operand2);
    }

    @Override
    public Bson apply(Bson... bsons) {

        return null;
    }
}
