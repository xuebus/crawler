package com.foofv.crawler.similarity.structure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by msfenn on 14/09/15.
 */
public class StringPartHashContainer {

    private int len = 2;
    private Map<Character, Integer> charPosMap = new HashMap<>();
    private Map<Integer, List<String>> idListMap = new HashMap<>();

    private List<Integer> localPosList = new ArrayList<>();
    private List<String> localStringList = new ArrayList<>();

    public StringPartHashContainer() {
    }

    public StringPartHashContainer(int len) {
        this.len = len;
    }

    public void setLen(int len) {

        this.len = len;
    }

    public void add(List<String> list) {

        list.stream().forEach(item -> add(item)); // maybe parallel isn't proper
    }

    public void add(String text) {

        if (text == null || text.length() < len)
            return;

        int pos = getHashTablePos(text); // always return a value
        for (int i = 0; i < len; ++i) {
            charPosMap.put(text.charAt(i), pos);
        }

        List<String> list = idListMap.get(pos);
        if (list == null) {
            list = new ArrayList<>();
        }
        list.add(text);
        idListMap.put(pos, list);
    }

    private Integer getHashTablePos(String text) {

        boolean isExisted = false;
        Integer pos = null;
        int len = this.len;
        while (!isExisted && --len >= 0) {
            pos = charPosMap.get(text.charAt(len));
            isExisted |= pos != null;
        }
        if (!isExisted) {
            pos = BKDRHash(text);
        }

        return pos;
    }

    private List<Integer> getHashTablePosList(String text) {

        localPosList.clear();
        List<Integer> list = localPosList;
        Integer pos = null;
        int len = this.len;
        while (--len >= 0) {
            pos = charPosMap.get(text.charAt(len));
            if (pos != null)
                list.add(pos);
        }

        return list.size() == 0 ? null : list;
    }

    public List<String> getListToMatch(String text) {

        if (text == null || text.length() < len)
            return null;

        List<Integer> posList = getHashTablePosList(text);
        localStringList.clear();
        final List<String> list = localStringList;//new ArrayList<>();
        posList.stream().forEach(pos -> list.addAll(idListMap.get(pos)));

        return list;
    }

    private int BKDRHash(String text) {

        int seed = 131; // prime 31 131 1313 13131 131313 etc.
        int hash = 0;
        int len = this.len;

        while (--len >= 0) {
            hash = hash * seed + text.charAt(len);
        }

        return (hash & 0x7FFFFFFF);
    }

    public static void main(String[] args) {

        StringPartHashContainer hash = new StringPartHashContainer();
        List<String> list1;
        hash.add("酸辣花螺");
        hash.add("微辣花螺");
        hash.add("好麻花螺");
//        list1 = hash.getListToMatch("酸辣鲍鱼");
//        System.out.println(list1.size());
//        list1 = hash.getListToMatch("麻辣鸭舌");
//        System.out.println(list1.size());
//        list1 = hash.getListToMatch("微辣鸭舌");
//        System.out.println(list1.size());
//        list1 = hash.getListToMatch("好麻鸭舌");
//        System.out.println(list1);
//        list1 = hash.getListToMatch("微麻鸭舌");
//        System.out.println(list1);
        System.out.println(hash.getListToMatch("麻辣鸭舌"));
    }

}
