package com.foofv.crawler.similarity.coordinate_request;

import com.foofv.crawler.similarity.coordinate.BD09LLCoordinate;
import com.foofv.crawler.similarity.coordinate.GeodeticCoordinate;
import com.foofv.crawler.similarity.strategy.*;
import com.foofv.crawler.similarity.structure.Pair;

/**
 * Created by msfenn on 17/09/15.
 */
public class BaiduCoordinateRequest extends CoordinateRequest {

    private static BaiduCoordinateRequest baiduCoordinateRequest = new BaiduCoordinateRequest();
    private static final String BAIDU_API_GEOCODER_URL_TEMPLATE = "http://api.map.baidu.com/geocoder/v2/?address=" + ADDRESS_PLACE_HOLDER + "&output=json&ak=";
    private static String baiduApiAK = "AOkR59MT4atImiOo3BGee0lL"; //TODO: multi key for payload balance

    public static CoordinateRequest getInstance() {

        return baiduCoordinateRequest;
    }

    @Override
    public GeodeticCoordinate requestCoordinate(String address) {

        if (address == null || address == "")
            return null;

        GeodeticCoordinate geodeticCoordinate = null;
        String url = BAIDU_API_GEOCODER_URL_TEMPLATE.replace(ADDRESS_PLACE_HOLDER, address);
        HttpSynchronousRequest request = new HttpSynchronousRequest();
        url += baiduApiAK;
        String coordinateJson = request.getHttpRequest(url);
        int status = Integer.parseInt(jsonParser.getValueFromJson(coordinateJson, "status", true));
        if (status == 0) {
            boolean isPrecise = Integer.parseInt(jsonParser.getValueFromJson(coordinateJson, "result.precise", true)) == 1 ? true : false;
            int confidence = Integer.parseInt(jsonParser.getValueFromJson(coordinateJson, "result.confidence", true));
            String level = jsonParser.getValueFromJson(coordinateJson, "result.level", true);

            double lng = Double.parseDouble(jsonParser.getValueFromJson(coordinateJson, "result.location.lng", true));
            double lat = Double.parseDouble(jsonParser.getValueFromJson(coordinateJson, "result.location.lat", true));

            geodeticCoordinate = new BD09LLCoordinate(lat, lng, isPrecise);
            System.out.println("bd request reply: " + geodeticCoordinate);
        } else {
            String errorMsg = jsonParser.getValueFromJson(coordinateJson, "msg", true);
            System.out.println("BAIDU API -> REQUEST COORDINATE ERROR: " + errorMsg);
        }

        return geodeticCoordinate;
    }

    public static void main(String[] args) throws TextMatchStrategy.StrategyException {

//        System.out.println(CoordinateRequest.getInstance().requestCoordinate("浦东大道"));
//        System.out.println(CoordinateRequest.getInstance().requestCoordinate("百度大厦"));
        DiceCoeffTextStrategy diceCoeffTextStrategy = new DiceCoeffTextStrategy(0.8);
//        diceCoeffTextStrategy.addRules(TextMatchStrategy.TEXT_PROCESS_RULE.HOMOGENIZATION);
        diceCoeffTextStrategy.calcSimilarity("光华路SOHO底商", "北京市朝阳区光华路22号光华路soho西侧123号");
//        diceCoeffTextStrategy.calcSimilarity("海淀区大钟寺东路太阳园小区13号楼会所2层", "北京市海淀区大钟寺东路太阳园小区会所二层");
        Pair<String> pair = diceCoeffTextStrategy.getHomogenizedStringPair();
        System.out.println(pair);
//        GeodeticCoordinate geodeticCoordinate1 = CoordinateRequest.getInstance().requestCoordinate("北京朝阳区北苑路42号易初莲花购物中心首层"/*pair.first*/);
//        GeodeticCoordinate geodeticCoordinate2 = CoordinateRequest.getInstance().requestCoordinate("北京朝阳区北苑路42号K酷广场1层"/*pair.second*/);
        GeodeticCoordinate geodeticCoordinate1 = BaiduCoordinateRequest.getInstance().requestCoordinate("易事达四层"/*pair.first*/);
        GeodeticCoordinate geodeticCoordinate2 = BaiduCoordinateRequest.getInstance().requestCoordinate("北京市朝阳区北苑家园秋实街1号4层001"/*pair.second*/);
        System.out.println(geodeticCoordinate1.calcDistance(geodeticCoordinate2));
        System.out.println(new CoordinateStrategy(0.8, 250).isSimilar(geodeticCoordinate1, geodeticCoordinate2));
        System.out.println(new CoordinateStrategy(0.8, 250).calcSimilarity(geodeticCoordinate1, geodeticCoordinate2));
    }
}
