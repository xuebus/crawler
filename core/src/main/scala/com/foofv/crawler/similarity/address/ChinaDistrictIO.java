package com.foofv.crawler.similarity.address;

import com.foofv.crawler.similarity.structure.DistrictInfo;
import com.foofv.crawler.storage.MongoStorage;

import java.util.List;

/**
 * Created by msfenn on 24/09/15.
 */
public class ChinaDistrictIO {

    private static List<DistrictInfo> districtInfoList;
    private static final ChinaDistrictRequest DISTRICT_REQUEST = ChinaDistrictRequest.getInstance();

    public static List<DistrictInfo> readDistricts() {

        districtInfoList = MongoStorage.getAll(DistrictInfo.class);

        return districtInfoList;
    }

    public static List<DistrictInfo> getDistricts() {

        if (districtInfoList == null)
            districtInfoList = MongoStorage.getAll(DistrictInfo.class);

        return districtInfoList;
    }

    public static void saveDistricts() {

        districtInfoList = DISTRICT_REQUEST.requestDistrictInfoList();
        MongoStorage.saveEntities(districtInfoList, true);
    }

    public static void main(String[] args) {

        List<DistrictInfo> list = getDistricts();
        String city="北京";
        System.out.println(list.stream().anyMatch(info -> info.name.equals(city) || info.fullName.equals(city)));
        System.out.println(list.size());
    }
}
