package com.foofv.crawler.similarity.address;

import com.foofv.crawler.parse.xpath.JsonMonster;
import com.foofv.crawler.similarity.coordinate_request.HttpSynchronousRequest;
import com.foofv.crawler.similarity.structure.DistrictInfo;
import com.foofv.crawler.storage.MongoStorage;
import org.apache.avro.generic.GenericData;
import scala.collection.JavaConversions;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by msfenn on 24/09/15.
 */
public class ChinaDistrictRequest {

    private static JsonMonster jsonParser = new JsonMonster(".", true);
    private static final ChinaDistrictRequest CHINA_DISTRICT_REQUEST = new ChinaDistrictRequest();
    private static final String QQ_API_DISTRICT_URL_TEMPLATE = "http://apis.map.qq.com/ws/district/v1/list?key=OB4BZ-D4W3U-B7VVO-4PJWW-6TKDJ-WPB77";
    private static String qqApiKey = "OB4BZ-D4W3U-B7VVO-4PJWW-6TKDJ-WPB77";
    private static String qqApiDistrictURL = "http://apis.map.qq.com/ws/district/v1/list?key=Y7VBZ-4H63F-AEKJK-JVIG3-GZGK2-DABIN";

    private ChinaDistrictRequest() {
        
    }

    public static ChinaDistrictRequest getInstance() {

        return CHINA_DISTRICT_REQUEST;
    }

    public List<String> requestDistricts() {

        return requestDistricts(QQ_API_DISTRICT_URL_TEMPLATE);
    }

    public List<String> requestDistricts(String url) {

        if (url == null || url.trim() == "")
            return null;

        List<String> districtList = null;
        HttpSynchronousRequest request = new HttpSynchronousRequest();
        String districtJson = request.getHttpRequest(url);
        scala.collection.immutable.List<String> scala_list = jsonParser.getValuesFromJson(districtJson, "result", 2);
        if (scala_list.isEmpty() == false)
            districtList = JavaConversions.asJavaList(scala_list);

        return districtList;
    }

    public List<DistrictInfo> requestDistrictInfoList() {

        return requestDistrictInfoList(qqApiDistrictURL);

    }

    public List<DistrictInfo> requestDistrictInfoList(String url) {

        List<String> districtList = requestDistricts(url);
        List<DistrictInfo> districtInfoList = null;

        if (districtList != null) {
            districtInfoList = new ArrayList<>();
            DistrictInfo districtInfo;
            int level = Integer.parseInt(districtList.get(0));
            int offset = level + 1;
            List<Boolean>[] hasSubDistrictLists = new List[level];
            List<Integer>[] subDistrictIndexLists = new List[level];
            int[] diffLevelDistrictCountArray = new int[level];
            String jsonItem;
            String subDistrictIndexRangeStr;
            String[] subDistrictIndexRangeArray;
            int begIdx;
            int endIdx;
            int ascendingIdxBased0;
            for (int i = level; i >= 1; --i) {
                ascendingIdxBased0 = level - i;
                int len = Integer.parseInt(districtList.get(i));
                diffLevelDistrictCountArray[ascendingIdxBased0] = len;
                hasSubDistrictLists[ascendingIdxBased0] = new ArrayList<>();
                subDistrictIndexLists[ascendingIdxBased0] = new ArrayList<>();
                for (int j = offset; j < offset + len; ++j) {
                    districtInfo = new DistrictInfo();
                    jsonItem = districtList.get(j);
                    districtInfo.id = jsonParser.getValueFromJson(jsonItem, "id");
                    districtInfo.name = jsonParser.getValueFromJson(jsonItem, "name");
                    districtInfo.fullName = jsonParser.getValueFromJson(jsonItem, "fullname");
                    districtInfo.lat = jsonParser.getValueFromJson(jsonItem, "location.lat");
                    districtInfo.lng = jsonParser.getValueFromJson(jsonItem, "location.lng");

                    subDistrictIndexRangeStr = jsonParser.getValueFromJson(jsonItem, "cidx");
                    if (subDistrictIndexRangeStr != null) {
                        hasSubDistrictLists[ascendingIdxBased0].add(true);
                        subDistrictIndexRangeStr = subDistrictIndexRangeStr.substring(1, subDistrictIndexRangeStr.length() - 1);
                        subDistrictIndexRangeArray = subDistrictIndexRangeStr.split(",");
                        begIdx = Integer.parseInt(subDistrictIndexRangeArray[0]);
                        endIdx = Integer.parseInt(subDistrictIndexRangeArray[1]);
                        subDistrictIndexLists[ascendingIdxBased0].add(begIdx);
                        subDistrictIndexLists[ascendingIdxBased0].add(endIdx);
                    } else
                        hasSubDistrictLists[ascendingIdxBased0].add(false);

                    districtInfo.level = ascendingIdxBased0;
                    districtInfoList.add(districtInfo);
                }
                offset += len;
            }

            offset = 0;
            for (int i = 0; i < level - 1; ++i) {
                int subDistrictIndex = 0;
                for (int j = offset; j < offset + diffLevelDistrictCountArray[i]; ++j) {
                    ascendingIdxBased0 = j - offset;
                    if (hasSubDistrictLists[i].get(ascendingIdxBased0)) {
                        districtInfo = districtInfoList.get(j);
                        begIdx = subDistrictIndexLists[i].get(subDistrictIndex++);
                        endIdx = subDistrictIndexLists[i].get(subDistrictIndex++);
                        districtInfo.subDistricts = endIdx - begIdx + 1;
                        districtInfo.subDistrictIds = new String[districtInfo.subDistricts];
                        int nextLevelOffset = offset + diffLevelDistrictCountArray[i];
                        for (int k = begIdx; k <= endIdx; ++k) {
                            districtInfo.subDistrictIds[k - begIdx] = districtInfoList.get(nextLevelOffset + k).id;
                            districtInfoList.get(nextLevelOffset + k).parentDistrictId = districtInfo.id;
                        }
                    }

                }
                offset += diffLevelDistrictCountArray[i];
            }
        }

        return districtInfoList;
    }

    public static void main(String[] args) {

        List<DistrictInfo> districtInfos = CHINA_DISTRICT_REQUEST.requestDistrictInfoList();
        System.out.println(districtInfos.size());
        MongoStorage.saveEntities(districtInfos, true);
//        System.out.println(CHINA_DISTRICT_REQUEST.requestDistricts().size());
    }
}
