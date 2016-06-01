package com.foofv.crawler.similarity.coordinate_request;

import com.foofv.crawler.similarity.coordinate.GCJCoordinate;
import com.foofv.crawler.similarity.coordinate.GeodeticCoordinate;
import com.foofv.crawler.similarity.strategy.TextMatchStrategy;
import scala.collection.JavaConversions;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by msfenn on 17/09/15.
 */
public class QQCoordinateRequest extends CoordinateRequest {

    private static QQCoordinateRequest qqCoordinateRequest = new QQCoordinateRequest();
    private static final String QQ_API_GEOCODER_URL_TEMPLATE = "http://apis.map.qq.com/ws/geocoder/v1/?address=" + ADDRESS_PLACE_HOLDER + "&key=";
    private static String qqApiKey = "Y7VBZ-4H63F-AEKJK-JVIG3-GZGK2-DABIN"; //TODO: multi key for payload balance

    public static CoordinateRequest getInstance() {

        return qqCoordinateRequest;
    }

    @Override
    public GeodeticCoordinate requestCoordinate(String address) {

        if (address == null || address == "")
            return null;

        GeodeticCoordinate geodeticCoordinate = null;
        String url = QQ_API_GEOCODER_URL_TEMPLATE.replace(ADDRESS_PLACE_HOLDER, address);
        HttpSynchronousRequest request = new HttpSynchronousRequest();
        url += qqApiKey;
        String coordinateJson = request.getHttpRequest(url);
        int status = Integer.parseInt(jsonParser.getValueFromJson(coordinateJson, "status", true));
        if (status == 0) {
            int similarity = Integer.parseInt(jsonParser.getValueFromJson(coordinateJson, "result.similarity", true));
            double deviation = Double.parseDouble(jsonParser.getValueFromJson(coordinateJson, "result.deviation", true));
            int reliability = Integer.parseInt(jsonParser.getValueFromJson(coordinateJson, "result.reliability", true));

            //TODO: save these address info if matches
            String province = jsonParser.getValueFromJson(coordinateJson, "result.address_components.province", true);
            String city = jsonParser.getValueFromJson(coordinateJson, "result.address_components.city", true);
            String district = jsonParser.getValueFromJson(coordinateJson, "result.address_components.district", true);
            String street = jsonParser.getValueFromJson(coordinateJson, "result.address_components.street", true);
            String street_number = jsonParser.getValueFromJson(coordinateJson, "result.address_components.street_number", true);

            double lng = Double.parseDouble(jsonParser.getValueFromJson(coordinateJson, "result.location.lng", true));
            double lat = Double.parseDouble(jsonParser.getValueFromJson(coordinateJson, "result.location.lat", true));

            boolean isPrecise = similarity >= 0.75 ? true : false;
            isPrecise &= reliability >= 7 ? true : false;
            geodeticCoordinate = new GCJCoordinate(lat, lng, isPrecise);

            System.out.println("qq request reply: " + geodeticCoordinate);
        } else {
            String errorMsg = jsonParser.getValueFromJson(coordinateJson, "message", true);
            System.out.println("QQ API -> REQUEST COORDINATE ERROR: " + errorMsg);
        }

        return geodeticCoordinate;
    }

    public static void main(String[] args) throws TextMatchStrategy.StrategyException {

//        GeodeticCoordinate geodeticCoordinate1 = CoordinateRequest.getInstance().requestCoordinate("北京朝阳区北苑路42号易初莲花购物中心首层"/*pair.first*/);
//        GeodeticCoordinate geodeticCoordinate2 = CoordinateRequest.getInstance().requestCoordinate("北京朝阳区北苑路42号K酷广场1层"/*pair.second*/);
//        GeodeticCoordinate geodeticCoordinate1 = QQCoordinateRequest.getInstance().requestCoordinate("北京市朝阳区北苑家园秋实街1号4层001"/*pair.first*/);
//        GeodeticCoordinate geodeticCoordinate2 = QQCoordinateRequest.getInstance().requestCoordinate("易事达四层"/*pair.second*/);
//        System.out.println(geodeticCoordinate1.calcDistance(geodeticCoordinate2));
//        System.out.println(new CoordinateStrategy(0.8, 250).isSimilar(geodeticCoordinate1, geodeticCoordinate2));
//        System.out.println(new CoordinateStrategy(0.8, 250).calcSimilarity(geodeticCoordinate1, geodeticCoordinate2));

        String string = "{\"ID\":1,\"result\":[[{\"name\":\"BILL\"},{\"age\":50}],[{\"name2\":\"bill\"},{\"age2\":50}],[{\"name3\":\"bill\"},{\"age3\":50}]]}";//
//        String string = "{\"ID\":1,\"result\":[{\"name\":\"BILL\"},{\"age\":50},{\"name2\":\"bill\"},{\"age2\":50}]}";//
        scala.collection.immutable.List list = jsonParser.getValuesFromJson(string, "result", 1);
        List<String> ls = JavaConversions.asJavaList(list);
        System.out.println("ls: " + ls);
        System.out.println(jsonParser.getValueFromJson(string, "id"));
        System.out.println(list.size());
        System.out.println(list.apply(0));
    }
}
