package com.foofv.crawler.similarity.utility;

/**
 * Created by msfenn on 21/09/15.
 */
public class TextProcessUtil {

    private static final String SEPARATORS_REGEX = ",|，|;|；";


    public static String filter(String text) {

        String result = text.split(SEPARATORS_REGEX, 2)[0];

        return result;
    }

    public static String tryToCorrect(String text) {

        return "";
    }

    public static void main(String[] args) {

        System.out.println(filter("地铁立水桥南站东南口C口出，往北走30米六号院内"));
        System.out.println(filter("地铁立水桥南站东南口C口出,往北走30米六号院内"));
        System.out.println(filter("地铁立水桥南站东南口C口出;往北走30米六号院内"));
    }
}
