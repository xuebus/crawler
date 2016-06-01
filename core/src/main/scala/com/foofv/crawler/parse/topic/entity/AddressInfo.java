package com.foofv.crawler.parse.topic.entity;

import org.mongodb.morphia.annotations.Indexed;

/**
 * Created by msfenn on 13/08/15.
 */
public class AddressInfo extends BaseItem {

    @Indexed(name = "address")
    public String address;
    public String latitude;
    public String longitude;
}
