package com.foofv.crawler.similarity.coordinate;

import java.text.DecimalFormat;


/**
 * Created by msfenn on 07/09/15.
 */
public class GeodeticCoordinate {

    static DecimalFormat decimalFormat = new DecimalFormat("0.000000");

    protected double longitude;
    protected double latitude;

    protected boolean isPrecise = true;
    private static final String STANDARD = "unknown";

    public String getStandard() {

        return STANDARD;
    }

    public boolean isPrecise() {

        return isPrecise;
    }

    public void setIsPrecise(boolean isPrecise) {

        this.isPrecise = isPrecise;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    @Override
    public boolean equals(Object another) {

        if (another == this) {
            return true;
        } else {
            if (another instanceof GeodeticCoordinate) {
                GeodeticCoordinate otherPointer = (GeodeticCoordinate) another;
                return decimalFormat.format(latitude).equals(decimalFormat.format(otherPointer.latitude))
                        && decimalFormat.format(longitude).equals(decimalFormat.format(otherPointer.longitude));
            } else {
                return false;
            }
        }
    }

    public String toString() {

        StringBuilder sb = new StringBuilder("latitude: " + latitude + "\t\t");
        sb.append(" longitude: " + longitude + "\t\t");

        return sb.toString();
    }

    public boolean isCalculable() {

        return false;
    }

    public double calcDistance(GeodeticCoordinate target) {

        if (!isCalculable() || getStandard().equalsIgnoreCase(target.getStandard()) == false) {
            return -1;
        }
        double earthR = 6371000;
        double x = Math.cos(this.latitude * Math.PI / 180) * Math.cos(target.latitude * Math.PI / 180)
                * Math.cos((this.longitude - target.longitude) * Math.PI / 180);
        double y = Math.sin(this.latitude * Math.PI / 180) * Math.sin(target.latitude * Math.PI / 180);
        double s = x + y;
        if (s > 1) {
            s = 1;
        }
        if (s < -1) {
            s = -1;
        }
        double alpha = Math.acos(s);
        double distance = alpha * earthR;

        return distance;
    }

    public static void main(String[] args) {

        System.out.println(new BD09LLCoordinate(0, 0).calcDistance(new GCJCoordinate(0, 0)));
        System.out.println(new BD09LLCoordinate(0, 0).equals(null));
    }
}
