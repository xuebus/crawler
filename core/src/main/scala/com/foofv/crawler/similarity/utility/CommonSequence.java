package com.foofv.crawler.similarity.utility;

import com.foofv.crawler.similarity.locale.CharacterUtil;
import org.apache.commons.collections.FastTreeMap;

import java.util.*;

/**
 * Created by msfenn on 08/09/15.
 */
public class CommonSequence {

    private List<Integer> separatorPosList1 = new ArrayList<>();
    private List<Integer> separatorPosList2 = new ArrayList<>();

    public List<Integer> getSeparatorPosList1() {
        return separatorPosList1;
    }

    public List<Integer> getSeparatorPosList2() {
        return separatorPosList2;
    }

    public CommonSequence extractPartition(String string1, String string2) {

        if (string1 == null || string2 == null)
            return this;

        separatorPosList1.clear();
        separatorPosList2.clear();
        for (int i = 0; i < string1.length() - 1; i++) {
            char ch = string1.charAt(i);
            if (isSeparator(ch)) {
                separatorPosList1.add(i);
            }
        }
        for (int i = 0; i < string2.length() - 1; i++) {
            char ch = string2.charAt(i);
            if (isSeparator(ch)) {
                separatorPosList2.add(i);
            }
        }

        return this;
    }

    private boolean isSeparator(char ch) {

        return !CharacterUtil.isChineseWithSymbolExcluded(ch) && !Character.isAlphabetic(ch) && !Character.isDigit(ch);
    }

    private static String generatePrincipalComponent(String[] posLenPairs, Set<Integer> indexOfRemovalElemSet) {

        String[] posLenArray = null;
        Map<Integer, Integer> posLenMap = new FastTreeMap();
        Set<String> removalElemSet = new HashSet<>();
        String result = "";
        int tmp_len;
        for (String posLenPair : posLenPairs) {
            if (posLenPair.length() != 0) {
                posLenArray = posLenPair.split(":");
                int pos = Integer.valueOf(posLenArray[0]);
                int len = Integer.valueOf(posLenArray[1]);
                if (!posLenMap.containsKey(pos)) {
                    posLenMap.put(pos, len);
                } else if ((tmp_len = posLenMap.get(pos)) < len) {
                    removalElemSet.add(pos + ":" + tmp_len);
                    posLenMap.put(pos, len);
                }
            }
        }

        int max_pos = 0;
        int len = 0;
        int totalLen = 0;
        for (Integer pos : posLenMap.keySet()) {
            len = posLenMap.get(pos);
            if (pos + len > max_pos) {
                if (pos < max_pos) {
                    len = pos + len - max_pos;
                    pos = max_pos;
                }
                max_pos = pos + len;
                result += pos + ":";
                result += len + " ";
                totalLen += len;
            } else {
                removalElemSet.add(pos + ":" + len);
            }
        }

        if (removalElemSet.size() > 0)
            for (int i = 0; i < posLenPairs.length; ++i) {
                if (removalElemSet.contains(posLenPairs[i])) {
                    indexOfRemovalElemSet.add(i);
                }
            }

        return totalLen == 0 ? result : result + totalLen;
    }

    public static String selectPrincipalComponent(String posLenStr) {

        String[] posLenStrs = posLenStr.split("\\|");
        String[] posLenPairs1 = posLenStrs[0].trim().split("\\s+");
        String[] posLenPairs2 = posLenStrs[1].trim().split("\\s+");
        Set<Integer> indexOfRemovalElemSet = new HashSet<>();
        String result1 = generatePrincipalComponent(posLenPairs1, indexOfRemovalElemSet);
        for (int idx : indexOfRemovalElemSet) {
            posLenPairs2[idx] = "";
        }
        indexOfRemovalElemSet.clear();
        String result2 = generatePrincipalComponent(posLenPairs2, indexOfRemovalElemSet);
        for (int idx : indexOfRemovalElemSet) {
            result1 = result1.replace(posLenPairs1[idx], "");
        }

        return result1.trim() + " | " + result2.trim();
    }

    public static String getCommonSequences(String string1, String string2) {

        if (string1 == null || string2 == null)
            return null;

        int len1 = string1.length();
        int len2 = string2.length();

        if (len1 == 0 || len2 == 0)
            return "";

        int beg_pos1 = 0;
        int beg_pos2 = 0;
        int cnt = 0;
        int i = 1;
        String posLenStr1 = "";
        String posLenStr2 = "";
        String delimiter = ":";

        while (i < len1 + len2) {
            beg_pos1 = i <= len1 ? len1 - i : 0;
            beg_pos2 = i <= len1 ? 0 : i - len1;
            while (beg_pos1 < len1 && beg_pos2 < len2) {
                if (string1.charAt(beg_pos1) == string2.charAt(beg_pos2)) {
                    ++cnt;
                } else if (cnt > 0) {
                    posLenStr1 += beg_pos1 - cnt + delimiter + cnt;
                    posLenStr1 += " ";
                    posLenStr2 += beg_pos2 - cnt + delimiter + cnt;
                    posLenStr2 += " ";
                    cnt = 0;
                }
                ++beg_pos1;
                ++beg_pos2;
            }

            if (cnt > 0) {
                posLenStr1 += beg_pos1 - cnt + delimiter + cnt;
                posLenStr1 += " ";
                posLenStr2 += beg_pos2 - cnt + delimiter + cnt;
                posLenStr2 += " ";
            }
            cnt = 0;
            ++i;
        }

        return posLenStr1.trim() + " | " + posLenStr2.trim();
    }

    public static void main(String[] args) {

        String result = CommonSequence.getCommonSequences("ebeecake小蜜蜂蛋糕", "ebeecake蛋糕");
        System.out.println(result);
        System.out.println("\t" + CommonSequence.selectPrincipalComponent(result));

        result = CommonSequence.getCommonSequences("abcdefg", "abcdeAAAcdefg");
        System.out.println(result);
        System.out.println("\t" + CommonSequence.selectPrincipalComponent(result));

        result = CommonSequence.getCommonSequences("21cake", "11cake");
        System.out.println(result);
        System.out.println("\t" + CommonSequence.selectPrincipalComponent(result));

        result = CommonSequence.getCommonSequences("咿呀米快餐", "咿呀米美食工作餐快送");
        System.out.println(result);
        System.out.println("\t" + CommonSequence.selectPrincipalComponent(result));

        result = CommonSequence.getCommonSequences("大虾来了小龙虾外卖", "不二江湖小龙虾");
        System.out.println(result);
        System.out.println("\t" + CommonSequence.selectPrincipalComponent(result));

//        List<Integer> list = new CommonSequence().extractPartition("卖虾·麻小麻辣海鲜(小龙虾外卖)...", "卖虾·麻小麻辣海鲜(小龙虾外卖)").getSeparatorPosList1();
//        for (int i : list) {
//            System.out.println((i));
//        }
    }
}
