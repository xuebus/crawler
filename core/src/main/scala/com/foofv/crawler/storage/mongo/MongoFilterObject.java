package com.foofv.crawler.storage.mongo;

import org.bson.conversions.Bson;

import java.util.regex.Pattern;

/**
 * Created by msfenn on 29/09/15.
 */
public abstract class MongoFilterObject {

    public abstract Bson applyString(String operand1, String operand2);

    public abstract Bson applyInteger(String operand1, long operand2);

    public abstract Bson applyFloat(String operand1, double operand2);

    public abstract Bson apply(Bson... bsons);

    public final Bson apply(String operand1, String operand2) {

        if (isInteger(operand2))
            return applyInteger(operand1, Integer.parseInt(operand2));
        if (isFloat(operand2)) {
            return applyFloat(operand1, Double.parseDouble(operand2));
        } else {
            if (operand2.charAt(0) == '\'' || operand2.charAt(0) == '"')
                operand2 = operand2.substring(1, operand2.length() - 1);
            return applyString(operand1, operand2);
        }
    }

    protected boolean isInteger(String operand) {

        return Pattern.matches("^[-\\+]?[\\d]+$", operand);
    }

    protected boolean isFloat(String operand) {

        return Pattern.matches("^[-\\+]?[\\d]+\\.[\\d]+$", operand);
    }
}
