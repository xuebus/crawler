package com.foofv.crawler.similarity.pipeline;

import com.foofv.crawler.similarity.literal.NumericalConstant;
import com.foofv.crawler.storage.MongoStorage;

/**
 * Created by msfenn on 24/09/15.
 */
public class Fetcher implements Runnable {

    private int fetchLimit = NumericalConstant.FETCH_LIMIT;
    private Class<?> clazz;
    private String collName;

    public <T> Fetcher(Class<T> clazz, int... fetchLimit) {

        this.clazz = clazz;
        if (fetchLimit.length > 0)
            this.fetchLimit = fetchLimit[0];
    }

//    public <T> Fetcher(String collName, int... fetchLimit) {
//
//        this.collName = collName;
//        if (fetchLimit.length > 0)
//            this.fetchLimit = fetchLimit[0];
//    }

    @Override
    public void run() {

        long fetchTimes;
        long collSize;
        if (clazz != null)
            collSize = MongoStorage.getCollectionSize(clazz);
        else
            collSize = MongoStorage.getCollectionSize(collName);

        fetchTimes = collSize % fetchLimit == 0 ? collSize / fetchLimit : collSize / fetchLimit + 1;
        for (int i = 0; i < fetchLimit; ++i) {
//            MongoStorage.getListLimited()
        }
    }
}
