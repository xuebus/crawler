package com.foofv.crawler.similarity.coordinate_request;

import com.foofv.crawler.parse.xpath.JsonMonster;
import com.foofv.crawler.similarity.coordinate.GeodeticCoordinate;

/**
 * Created by msfenn on 16/09/15.
 */
public abstract class CoordinateRequest {

    protected static JsonMonster jsonParser = new JsonMonster(".", true);
    protected static final String ADDRESS_PLACE_HOLDER = "@@ADDRESS";

    public abstract GeodeticCoordinate requestCoordinate(String address);
}
