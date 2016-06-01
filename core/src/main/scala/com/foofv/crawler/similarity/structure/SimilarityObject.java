package com.foofv.crawler.similarity.structure;

import com.foofv.crawler.similarity.coordinate.GeodeticCoordinate;

import java.util.List;

/**
 * Created by msfenn on 24/09/15.
 */
public class SimilarityObject {

    public String restaurantId;
    public String restaurantName;
    public String address;
    public GeodeticCoordinate coordinate;
    public List<String> menuNameList;
    public List<String> foodNameList;

//    public Pair<String> restaurantNamePair;
//    public Pair<String> addressPair;
//    public Pair<GeodeticCoordinate> coordinatePair;
//    public Pair<List<String>> menuNameListPair;
//    public Pair<List<String>> foodNameListPair;
}
