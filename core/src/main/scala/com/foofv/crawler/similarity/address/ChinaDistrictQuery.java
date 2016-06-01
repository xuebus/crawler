package com.foofv.crawler.similarity.address;

import com.foofv.crawler.similarity.structure.DistrictInfo;

import java.util.List;

/**
 * Created by msfenn on 24/09/15.
 */
public class ChinaDistrictQuery {

    public static int getDistrictLevel(String district) {

        if (district == null)
            return Integer.MAX_VALUE;

        List<DistrictInfo> districtInfoList = ChinaDistrictIO.getDistricts();

        return getDistrictLevel(districtInfoList, district);
    }

    public static int getDistrictLevel(List<DistrictInfo> districtInfoList, String district) {

        if (district == null)
            return Integer.MAX_VALUE;

        int level = Integer.MAX_VALUE;

        for (DistrictInfo districtInfo : districtInfoList) {
            if (district.equals(districtInfo.name) || district.equals(districtInfo.fullName)) {
                level = districtInfo.level;
                break;
            }
        }

        return level;
    }
}
