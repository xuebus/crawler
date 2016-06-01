package com.foofv.crawler.similarity.strategy;

import com.foofv.crawler.similarity.coordinate.GeodeticCoordinate;
import com.foofv.crawler.similarity.structure.Pair;

/**
 * Created by msfenn on 15/09/15.
 */
public class CoordinateStrategy extends Strategy {

    private GeodeticCoordinate geodeticCoordinate1;
    private GeodeticCoordinate geodeticCoordinate2;
    private double distanceLimit;
    private double realDistance;
    private Pair<GeodeticCoordinate> coordinatePair;

    public CoordinateStrategy(double confidence, double distanceLimit, double... weight) {

        super(confidence, weight);
        this.distanceLimit = distanceLimit;
        realDistance = distanceLimit + 1;
    }

    public CoordinateStrategy(double confidence, double distanceLimit, Pair<GeodeticCoordinate> coordinatePair, double... weight) {

        this(confidence, distanceLimit, weight);
        setCoordinatePair(coordinatePair);
    }

    public CoordinateStrategy(double confidence, double distanceLimit, GeodeticCoordinate geodeticCoordinate1, GeodeticCoordinate geodeticCoordinate2, double... weight) {

        this(confidence, distanceLimit, weight);
        setCoordinatePair(geodeticCoordinate1, geodeticCoordinate2);
    }

    public void setCoordinatePair(Pair<GeodeticCoordinate> coordinatePair) {

        if (coordinatePair != null) {
            geodeticCoordinate1 = coordinatePair._1();
            geodeticCoordinate2 = coordinatePair._2();
        }
    }

    public void setCoordinatePair(GeodeticCoordinate geodeticCoordinate1, GeodeticCoordinate geodeticCoordinate2) {

        this.geodeticCoordinate1 = geodeticCoordinate1;
        this.geodeticCoordinate2 = geodeticCoordinate2;
    }

    public void setCoordinateIfAnyNull(GeodeticCoordinate geodeticCoordinate) {

        if (geodeticCoordinate1 == null)
            geodeticCoordinate1 = geodeticCoordinate;
        else if (geodeticCoordinate2 == null)
            geodeticCoordinate2 = geodeticCoordinate;
    }

    public GeodeticCoordinate getCoordinateIfNotNull() {

        if (geodeticCoordinate1 != null)
            return geodeticCoordinate1;
        if (geodeticCoordinate2 != null)
            return geodeticCoordinate2;

        return null;
    }

    @Override
    public double calcSimilarity() {

        realDistance = geodeticCoordinate1.calcDistance(geodeticCoordinate2);
        if (realDistance != -1) {
            similarity = isSimilar() ? confidence + (1 - confidence) * (distanceLimit - realDistance) / distanceLimit : confidence - (realDistance - distanceLimit) / distanceLimit;
            similarity = similarity < 0 ? 0 : similarity;
        } else {
            similarity = -1;
            println("ERROR: Wrong Coordinate Standard");
        }

        return similarity;
    }

    @Override
    public double calcSimilarity(Object obj1, Object obj2) {

        if (!isValid(obj1, obj2))
            return -1;

        geodeticCoordinate1 = (GeodeticCoordinate) obj1;
        geodeticCoordinate2 = (GeodeticCoordinate) obj2;

        return calcSimilarity();

//        realDistance = geodeticCoordinate1.calcDistance(geodeticCoordinate2);
//        if (realDistance != -1) {
//            similarity = isSimilar() ? confidence + (1 - confidence) * (distanceLimit - realDistance) / distanceLimit : confidence - (realDistance - distanceLimit) / distanceLimit;
//            similarity = similarity < 0 ? 0 : similarity;
//        } else {
//            similarity = -1;
//            println("ERROR: Wrong Coordinate Standard");
//        }
//
//        return similarity;
    }

    @Override
    public boolean isSimilar() {

        return realDistance <= distanceLimit;
    }

    @Override
    public boolean isValid() {

        if (geodeticCoordinate1 != null && geodeticCoordinate2 != null)
            return true;
        else
            return false;
    }

    public static void main(String[] args) {

        new CoordinateStrategy(0, 0).calcSimilarity(null, null);
    }
}
