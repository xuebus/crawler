package com.foofv.crawler.similarity.coordinate;

/**
 * Created by msfenn on 07/09/15.
 */
public class GeodeticCoordinateTest {

    static WGSCoordinate shanghaiWGSCoordinate = new WGSCoordinate(31.1774276, 121.5272106);
    static GCJCoordinate shanghaiGCJCoordinate = new GCJCoordinate(31.17530398364597, 121.531541859215);

    static WGSCoordinate shenzhenWGSCoordinate = new WGSCoordinate(22.543847, 113.912316);
    static GCJCoordinate shenzhenGCJCoordinate = new GCJCoordinate(22.540796131694766, 113.9171764808363);

    static WGSCoordinate beijingWGSCoordinate = new WGSCoordinate(39.911954, 116.377817);
    static GCJCoordinate beijingGCJCoordinate = new GCJCoordinate(39.91334545536069, 116.38404722455657);

    static BD09LLCoordinate beijingBD09LLCoordinate = new BD09LLCoordinate(39.934659629858, 116.4605429299);

    public static void testWSG2GCJ() {

        System.out.println(shanghaiGCJCoordinate.equals(shanghaiWGSCoordinate.toGCJCoordinate()));
        System.out.println(shenzhenGCJCoordinate.equals(shenzhenWGSCoordinate.toGCJCoordinate()));
        System.out.println(beijingGCJCoordinate.equals(beijingWGSCoordinate.toGCJCoordinate()));
    }

    public static void testGCJ2WSG() {

        System.out.println(shanghaiWGSCoordinate.equals(shanghaiGCJCoordinate.toWGSCoordinate()));
        System.out.println(shenzhenWGSCoordinate.equals(shenzhenGCJCoordinate.toWGSCoordinate()));
        System.out.println(beijingWGSCoordinate.equals(beijingGCJCoordinate.toWGSCoordinate()));
    }

    public static void testExactGCJ2WSG() {

        System.out.println(shanghaiWGSCoordinate.equals(shanghaiGCJCoordinate.toExactWGSCoordinate()));
        System.out.println(shenzhenWGSCoordinate.equals(shenzhenGCJCoordinate.toExactWGSCoordinate()));
        System.out.println(beijingWGSCoordinate.equals(beijingGCJCoordinate.toExactWGSCoordinate()));
    }

    public static void testGCJ2WSGDistance() {

        System.out.println(shanghaiWGSCoordinate.calcDistance(shanghaiGCJCoordinate.toWGSCoordinate()) /*< 5*/);
        System.out.println(shanghaiWGSCoordinate.calcDistance(shenzhenGCJCoordinate.toWGSCoordinate()) /*< 5*/);
        System.out.println(shanghaiWGSCoordinate.calcDistance(beijingGCJCoordinate.toWGSCoordinate()) /*< 5*/);
    }

    public static void testExactGCJ2WSGDistance() {

        System.out.println(shanghaiWGSCoordinate.calcDistance(shanghaiGCJCoordinate.toExactWGSCoordinate()) /*< 0.5*/);
        System.out.println(shanghaiWGSCoordinate.calcDistance(shenzhenGCJCoordinate.toExactWGSCoordinate()) /*< 0.5*/);
        System.out.println(shanghaiWGSCoordinate.calcDistance(beijingGCJCoordinate.toExactWGSCoordinate()) /*< 0.5*/);
    }

    public static void testBD09LL2GCJ() {

//        System.out.println(beijingGCJCoordinate.equals(beijingBD09LLCoordinate.toGCJCoordinate()));
        System.out.println("conv" + "\t" + beijingBD09LLCoordinate.toGCJCoordinate().toString());
        System.out.println(new BD09LLCoordinate(39.948763524987, 116.45903588946).toGCJCoordinate().toWGSCoordinate().calcDistance(beijingBD09LLCoordinate.toGCJCoordinate().toWGSCoordinate()));
    }

    public static void main(String[] args) {

        GCJCoordinate gcjCoordinate = new GCJCoordinate(40.0422300000, 116.4216870000);
        System.out.println(new BD09LLCoordinate(40.0484707747,116.4281088851).toGCJCoordinate()/*.calcDistance(gcjCoordinate)*/);
//        testBD09LL2GCJ();

//        testWSG2GCJ();
//        System.out.println();
//        testGCJ2WSG();
//        System.out.println();
//        testExactGCJ2WSG();
//        System.out.println();
//        testGCJ2WSGDistance();
//        System.out.println();
//        testExactGCJ2WSGDistance();
//        System.out.println();
    }
}
