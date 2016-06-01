package com.foofv.crawler.similarity;

import com.foofv.crawler.similarity.strategy.GroupStrategy;

import java.util.List;

/**
 * Created by msfenn on 23/09/15.
 */
public class MenuFoodSimilarity {

    private GroupStrategy groupStrategy;

    public MenuFoodSimilarity(GroupStrategy groupStrategy) {

        this.groupStrategy = groupStrategy;
    }

    public double getGroupSimilarity(List<String> group1, List<String> group2) {

        double groupSimilarity = groupStrategy.calcSimilarity(group1, group2);

        return groupSimilarity;
    }
}
