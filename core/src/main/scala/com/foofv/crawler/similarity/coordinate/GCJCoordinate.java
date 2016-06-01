package com.foofv.crawler.similarity.coordinate;

/**
 * Created by msfenn on 07/09/15.
 */
public class GCJCoordinate extends GeodeticCoordinate {

    private static final String STANDARD = "gcj02";


    public static String standard() {

        return STANDARD;
    }

    public String getStandard() {

        return STANDARD;
    }

    public GCJCoordinate() {

    }

    public GCJCoordinate(double latitude, double longitude, boolean... isPrecise) {

        this.latitude = latitude;
        this.longitude = longitude;
        this.isPrecise = isPrecise.length > 0 ? isPrecise[0] : this.isPrecise;
    }

    public WGSCoordinate toWGSCoordinate() {

        if (TransformCoordinateUtil.isOutOfChina(this.latitude, this.longitude)) {
            return new WGSCoordinate(this.latitude, this.longitude);
        }
        double[] delta = TransformCoordinateUtil.calcGCJ2WGSDelta(this.latitude, this.longitude);

        return new WGSCoordinate(this.latitude - delta[0], this.longitude - delta[1]);
    }

    public WGSCoordinate toExactWGSCoordinate() {

        final double initDelta = 0.01;
        final double threshold = 0.000001;
        double dLat = initDelta, dLng = initDelta;
        double mLat = this.latitude - dLat, mLng = this.longitude - dLng;
        double pLat = this.latitude + dLat, pLng = this.longitude + dLng;
        double wgsLat, wgsLng;
        WGSCoordinate currentWGSCoordinate = null;
        for (int i = 0; i < 30; i++) {
            wgsLat = (mLat + pLat) / 2;
            wgsLng = (mLng + pLng) / 2;
            currentWGSCoordinate = new WGSCoordinate(wgsLat, wgsLng);
            GCJCoordinate tmp = currentWGSCoordinate.toGCJCoordinate();
            dLat = tmp.getLatitude() - this.getLatitude();
            dLng = tmp.getLongitude() - this.getLongitude();
            if ((Math.abs(dLat) < threshold) && (Math.abs(dLng) < threshold)) {
                return currentWGSCoordinate;
            } else {
                System.out.println(i + ")\t" + dLat + ":" + dLng);
            }
            if (dLat > 0) {
                pLat = wgsLat;
            } else {
                mLat = wgsLat;
            }
            if (dLng > 0) {
                pLng = wgsLng;
            } else {
                mLng = wgsLng;
            }
        }

        return currentWGSCoordinate;
    }

    @Override
    public boolean isCalculable() {

        return true;
    }
}
