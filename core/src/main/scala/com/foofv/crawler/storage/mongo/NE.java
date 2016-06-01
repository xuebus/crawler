package com.foofv.crawler.storage.mongo;

import org.bson.conversions.Bson;

import static com.mongodb.client.model.Filters.ne;

/**
 * Created by msfenn on 29/09/15.
 */
public class NE extends MongoFilterObject {

    private static NE ne = new NE();

    private NE() {

    }

    public static NE getInstance() {

        return ne;
    }

    @Override
    public Bson applyString(String operand1, String operand2) {

        return ne(operand1, operand2);
    }

    @Override
    public Bson applyInteger(String operand1, long operand2) {

        return ne(operand1, operand2);
    }

    @Override
    public Bson applyFloat(String operand1, double operand2) {

        return ne(operand1, operand2);
    }

    @Override
    public Bson apply(Bson... bsons) {

        return null;
    }
}
