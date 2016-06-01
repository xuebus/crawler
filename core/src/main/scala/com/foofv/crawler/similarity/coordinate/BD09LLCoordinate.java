package com.foofv.crawler.similarity.coordinate;

import static java.lang.Math.*;

/**
 * Created by msfenn on 07/09/15.
 */
public class BD09LLCoordinate extends GeodeticCoordinate {

    private final double X_PI = 3.14159265358979324 * 3000.0 / 180.0;
    private static final String STANDARD = "bd09-ll";

    public BD09LLCoordinate(double latitude, double longitude, boolean... isPrecise) {

        this.latitude = latitude;
        this.longitude = longitude;
        this.isPrecise = isPrecise.length > 0 ? isPrecise[0] : this.isPrecise;
    }

    public static String standard() {

        return STANDARD;
    }

    public String getStandard() {

        return STANDARD;
    }

    public GCJCoordinate toGCJCoordinate() {

        double x = this.longitude - 0.0065;
        double y = this.latitude - 0.006;
        double z = sqrt(x * x + y * y) - 0.00002 * sin(y * X_PI);
        double theta = atan2(y, x) - 0.000003 * cos(x * X_PI);
        double gcjLon = z * cos(theta);
        double gcjLat = z * sin(theta);

        return new GCJCoordinate(gcjLat, gcjLon);
    }

    @Override
    public boolean isCalculable() {

        return true;
    }
}
