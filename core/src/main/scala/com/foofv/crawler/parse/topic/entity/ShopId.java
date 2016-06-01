package com.foofv.crawler.parse.topic.entity;

import org.mongodb.morphia.annotations.Id;

/**
 * Created by msfenn on 30/09/15.
 */
public class ShopId {

    @Id
    public String shop_id;

    public ShopId(String shop_id) {

        this.shop_id = shop_id;
    }
}
