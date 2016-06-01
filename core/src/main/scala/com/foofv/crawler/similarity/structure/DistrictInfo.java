package com.foofv.crawler.similarity.structure;

import org.mongodb.morphia.annotations.Id;

/**
 * Created by msfenn on 24/09/15.
 */
public class DistrictInfo {

    @Id
    public String id;
    public String name;
    public String fullName;
    public String lat;
    public String lng;
    public int level;
    //    public int subDistrictIndexBeg;
//    public int subDistrictIndexEnd;
    public String[] subDistrictIds;
    public int subDistricts;
    public String parentDistrictId;
}
