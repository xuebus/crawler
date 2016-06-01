package com.foofv.crawler.similarity.coordinate;


/**
 * Created by msfenn on 07/09/15.
 */
public class WGSCoordinate extends GeodeticCoordinate {

    private static final String STANDARD = "wgs84";

    public String getStandard() {

        return STANDARD;
    }

    public WGSCoordinate(double latitude, double longitude, boolean... isPrecise) {

        this.latitude = latitude;
        this.longitude = longitude;
        this.isPrecise = isPrecise.length > 0 ? isPrecise[0] : this.isPrecise;
    }

    public GCJCoordinate toGCJCoordinate() {

        if (TransformCoordinateUtil.isOutOfChina(this.latitude, this.longitude)) {
            return new GCJCoordinate(this.latitude, this.longitude);
        }
        double[] delta = TransformCoordinateUtil.calcGCJ2WGSDelta(this.latitude, this.longitude);

        return new GCJCoordinate(this.latitude + delta[0], this.longitude + delta[1]);
    }

    @Override
    public boolean isCalculable() {

        return true;
    }
}
