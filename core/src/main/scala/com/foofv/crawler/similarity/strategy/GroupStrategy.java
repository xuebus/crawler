package com.foofv.crawler.similarity.strategy;

import com.foofv.crawler.similarity.structure.StringPartHashContainer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by msfenn on 23/09/15.
 */
public class GroupStrategy extends Strategy {

    private List<String> list1;
    private List<String> list2;
    private StringPartHashContainer stringPartHashContainer;
    private TextMatchStrategy textMatchStrategy;

    public GroupStrategy(double groupConfidence, double textConfidence, double... weight) {

        super(groupConfidence, weight);
        stringPartHashContainer = new StringPartHashContainer();
        textMatchStrategy = new DiceCoeffTextStrategy(textConfidence);
    }

    public GroupStrategy(double groupConfidence, double textConfidence, StringPartHashContainer stringPartHashContainer, double... weight) {

        super(groupConfidence, weight);
        this.stringPartHashContainer = stringPartHashContainer;
        textMatchStrategy = new DiceCoeffTextStrategy(textConfidence);
    }

    @Override
    public double calcSimilarity(Object obj1, Object obj2) {

        if (!isValid(obj1, obj2))
            return -1;

        list1 = (List<String>) obj1;
        list2 = (List<String>) obj2;

        similarity = calcSimilarity();

        return similarity;
    }

    @Override
    public double calcSimilarity() {

        stringPartHashContainer.add(list1);
        List<String> matchList;
        double textSimilarity;
        int matchCount = 0;
        int i = 1;
        for (String text : list2) {
            println(i++ + "\t");
            matchList = stringPartHashContainer.getListToMatch(text);
            if (matchList != null) {
                for (String matchText : matchList) {
                    textSimilarity = textMatchStrategy.calcSimilarity(text, matchText);
                    println(text + "---" + matchText + " : " + textSimilarity);
                    if (textMatchStrategy.isSimilar()) {
                        ++matchCount;
                        break;
                    }
                }
            }
        }

        int totalSize = list1.size() + list2.size();
        similarity = (double) (matchCount * 2) / totalSize;

        print("match: " + matchCount + "\t" + "total: " + totalSize + "\t");

        return similarity;
    }

    @Override
    public boolean isSimilar() {

        if (similarity < confidence)
            return false;
        else
            return true;
    }

    @Override
    public boolean isValid() {

        if (list1 == null || list2 == null)
            return false;
        else
            return true;
    }

    public void setList1(List<String> list1) {

        this.list1 = list1;
    }

    public void setList2(List<String> list2) {

        this.list2 = list2;
    }

    public void setTextMatchStrategy(TextMatchStrategy textMatchStrategy) {

        this.textMatchStrategy = textMatchStrategy;
    }

    public static void main(String[] args) {

        GroupStrategy groupStrategy = new GroupStrategy(0.5, 0.6);
        groupStrategy.test_on = true;
        List<String> list1 = new ArrayList<>();
        List<String> list2 = new ArrayList<>();
        list1.add("麻辣小龙虾");
        list1.add("蒜蓉小龙虾580克");
        list1.add("十三香小龙虾580克");
        list1.add("麻辣蛏子");
        list1.add("麻辣鱿鱼");
        list1.add("香辣蟹");
        list1.add("麻辣花螺");
        list1.add("麻辣花蛤");
        list1.add("清蒸大闸蟹");
        list1.add("香辣大闸蟹");
        list1.add("黑胡椒大闸蟹");

        list2.add("清蒸大闸蟹");
        list2.add("麻辣大闸蟹");
        list2.add("黑胡椒大闸蟹");
        list2.add("麻辣蛏子");
        list2.add("麻辣花蛤");
        list2.add("麻辣鱿鱼须");
        list2.add("麻辣牛蛙");
        list2.add("蒜香小龙虾");
        list2.add("十三香小龙虾");
        list2.add("麻辣小龙虾");
        list2.add("麻辣鲜活虾球");
        list2.add("鲜活虾球");

        for (int i = 0; i < 1; ++i)
            System.out.println(i + ": " + groupStrategy.calcSimilarity(list1, list2));
    }
}
