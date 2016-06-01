package com.foofv.crawler.similarity.strategy;

import com.foofv.crawler.similarity.structure.Pair;
import com.foofv.crawler.similarity.utility.CommonSequence;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by msfenn on 08/09/15.
 */
public class DiceCoeffTextStrategy extends TextMatchStrategy {

    private double intersectionSize;
    private double unionSize;
    private Pair<String> homogenizedStringPair;
    private static final double[] LOSS_RATE = new double[6];

    static {
        LOSS_RATE[0] = 1.0;
        for (int i = 1; i < LOSS_RATE.length; ++i) {
            LOSS_RATE[i] = 1.0 - 1.0 / getFactorial(i);
        }
    }

    public DiceCoeffTextStrategy(double confidence, double... weight) {

        super(confidence, weight);
    }

    private static int getFactorial(int n) {

        if (n <= 1)
            return 1;
        return n * getFactorial(n - 1);
    }

    private double getLossFunctionValue(int lossRateIndex, double isomorphism, double initSimilarity) {

        double lossDegree = LOSS_RATE[lossRateIndex] * (1 - isomorphism) / (1 + initSimilarity) * (1 - confidence);
        lossDegree = lossDegree > initSimilarity ? initSimilarity : lossDegree;

        return lossDegree;
    }

    public double getIsomorphism(String string1, String string2, Set<String> intersectionOfPosLenPairs) {

        if (intersectionOfPosLenPairs.size() == 0)
            return 0;

        int sameSeqCount = 0;
        for (String posLenPair : intersectionOfPosLenPairs) {
            String[] posLen = posLenPair.split(":");
            int pos = Integer.valueOf(posLen[0]);
            int len = Integer.valueOf(posLen[1]);
            if (string1.substring(pos, pos + len).equalsIgnoreCase(string2.substring(pos, pos + len))) {
                ++sameSeqCount;
            }
        }

        return (double) sameSeqCount / intersectionOfPosLenPairs.size();
    }

    public Pair<Double> getIsomorphismEntropyPair(String string1, String string2, String[] posLenPairs1, String[] posLenPairs2) {

        double collSize = posLenPairs1.length + posLenPairs2.length - 2;
        Set<String> posLenPairSet1 = new HashSet<>();
        Set<String> posLenPairSet2 = new HashSet<>();
        String theLastElem = null;
        for (String posLenPair : posLenPairs1) {
            theLastElem = posLenPair;
            posLenPairSet1.add(posLenPair);
        }
        posLenPairSet1.remove(theLastElem);

        for (String posLenPair : posLenPairs2) {
            theLastElem = posLenPair;
            posLenPairSet2.add(posLenPair);
        }
        posLenPairSet2.remove(theLastElem);

        posLenPairSet1.retainAll(posLenPairSet2);

        double isomorphism = getIsomorphism(string1, string2, posLenPairSet1);
        print("conditional isomorphism = " + isomorphism + "\t");

        double pseudoIsomorphism = 2 * posLenPairSet1.size() / collSize;
        double pseudoEntropy = 1.0 - pseudoIsomorphism;
        print("entropy = " + pseudoEntropy + "\t");
        isomorphism *= pseudoIsomorphism;
        print("isomorphism = " + isomorphism + "\t");

        Pair<Double> isomorphismEntropyPair = new Pair<>(isomorphism, pseudoEntropy);

        return isomorphismEntropyPair;
    }

    private double getEntropyGain(double isomorphism, double entropy, int lossRateIndex) {

        if (isomorphism < 0.1)
            isomorphism = -Math.abs(LOSS_RATE[lossRateIndex] - isomorphism);
        double gain = isomorphism * entropy;
        gain = gain > 0 ? /*Math.sqrt*/(gain) : gain;

        return gain;
    }

    private double getHeaderMatchedGain(String string1, String string2, String posLenPair, double matchRatio, double entropy) {

        String[] posLenArray = posLenPair.split(":");
        int headerLen = Integer.valueOf(posLenArray[1]);
        String subStr1 = string1.substring(0, headerLen);
        String subStr2 = string2.substring(0, headerLen);
        if (posLenArray[0].equals("0") && subStr1.equalsIgnoreCase(subStr2)) {
            return matchRatio * entropy * headerLen / Math.min(string1.length(), string2.length()) /* * subStr1.length() / Math.min(string1.length(), string2.length())*/;
        } else {
            entropy -= 1;
            entropy = entropy == 0 ? (matchRatio - 1) : entropy;
            return -Math.pow(matchRatio * entropy, 2);
        }
    }

    private double getSegregationVSMatchGain(int lossRateIndex, double matchRatio, double similarity, double avgLen) {

        double gain = (1 - LOSS_RATE[lossRateIndex]) * matchRatio * similarity * (1 - 1 / avgLen);

        return gain;
    }


    @Override
    protected double calcTextSimilarity(String string1, String string2) {

        unionSize = string1.length() + string2.length();
        String result = CommonSequence.getCommonSequences(string1, string2);
        result = CommonSequence.selectPrincipalComponent(result);
        if (result.length() == 0 || result.equals(" | "))
            return 0.0;
        String[] posLenStrs = result.split("\\|");
        String[] posLenPairs1 = posLenStrs[0].trim().split("\\s+");
        String[] posLenPairs2 = posLenStrs[1].trim().split("\\s+");
        String[] posLenPairs = null;
        if (Integer.valueOf(posLenPairs1[posLenPairs1.length - 1]) < Integer.valueOf(posLenPairs2[posLenPairs2.length - 1])) {
            posLenPairs = posLenPairs1;
            intersectionSize = Integer.valueOf(posLenPairs1[posLenPairs1.length - 1]);
        } else {
            posLenPairs = posLenPairs2;
            intersectionSize = Integer.valueOf(posLenPairs2[posLenPairs2.length - 1]);
        }

        applyRules(posLenPairs, posLenPairs1, posLenPairs2);

        return similarity;
    }

    private void applyRules(String[] posLenPairs, String[] posLenPairs1, String[] posLenPairs2) {

        if (ruleSet.contains(TEXT_PROCESS_RULE.STRICT_NUMBER_MATCH)) {
            int digitNotMatchedSum = 0;
            String[] posLenArray;
            for (int i = 0; i < posLenPairs.length - 1; ++i) {
                String posLenPair1 = posLenPairs1[i];
                String posLenPair2 = posLenPairs2[i];
                /*if (posLenPair.length() != 0)*/
                {
                    posLenArray = posLenPair1.split(":");
                    int pos = Integer.valueOf(posLenArray[0]);
                    int len = Integer.valueOf(posLenArray[1]);
                    int notMatchedLen1 = getNotMatchedDigitNumber(string1, pos, len);
                    posLenArray = posLenPair2.split(":");
                    pos = Integer.valueOf(posLenArray[0]);
                    len = Integer.valueOf(posLenArray[1]);
                    int notMatchedLen2 = getNotMatchedDigitNumber(string2, pos, len);
                    digitNotMatchedSum += Math.max(notMatchedLen1, notMatchedLen2);
                }
            }
            intersectionSize -= digitNotMatchedSum;
        }
        if (ruleSet.contains(TEXT_PROCESS_RULE.HOMOGENIZATION)) {
            int paddingNumber = getPaddingNumberOfHomogenization(posLenPairs1, posLenPairs2);
            intersectionSize += paddingNumber;
            unionSize += paddingNumber;
        }
        similarity = 2 * intersectionSize / unionSize;
        if (ruleSet.contains(TEXT_PROCESS_RULE.HEURISTIC_MATCH)) {
            int lossRateIndex = posLenPairs.length > LOSS_RATE.length ? 0 : posLenPairs.length - 1;
            Pair<Double> isomorphismEntropyPair = getIsomorphismEntropyPair(string1, string2, posLenPairs1, posLenPairs2);
            double pseudoEntropy = isomorphismEntropyPair.second;
            double matchRatio1 = intersectionSize / string1.length();
            double matchRatio2 = intersectionSize / string2.length();

            double initSimilarity = similarity;
            print("loss rate: " + LOSS_RATE[lossRateIndex] + "\t\t");
            print("init smlrt: " + similarity + "\t\t");
            double loss = getLossFunctionValue(lossRateIndex, isomorphismEntropyPair.first, similarity);
            print("loss : " + loss + "\t\t\t\t");
            similarity -= loss;

            double gain = getEntropyGain(isomorphismEntropyPair.first, pseudoEntropy, lossRateIndex) * (1 - similarity);
            print("entropy add gain: " + gain + "\t\t\t\t");
            similarity += gain;

            gain = getHeaderMatchedGain(string1, string2, posLenPairs[0], /*Math.min(matchRatio1, matchRatio2)*/initSimilarity, isomorphismEntropyPair.second) * (1 - similarity);
            print("header add gain: " + gain + "\t\t\t\t");
            similarity += gain;

            gain = getSegregationVSMatchGain(lossRateIndex, Math.max(matchRatio1, matchRatio2), initSimilarity, unionSize / 2) * (1 - initSimilarity);
            print("LeastSegregationMostMatch add gain: " + gain + "\t\t\t\t");
            similarity += gain;

            similarity *= weight;
            similarity = Math.abs(similarity);
        }
    }

    private int getNotMatchedDigitNumber(String string1, final int pos, final int len) {

        int digitsNotMatched = 0;
        int size = string1.length();
        int tmp_cnt = 0;
        for (int i = pos; i < pos + len; ++i) {
            if (!Character.isDigit(string1.charAt(i))) {
                break;
            }
            ++tmp_cnt;
        }
        if (tmp_cnt == len) {// common sequence is a digit string,so it's ineffective against similarity goal
            digitsNotMatched = len;
            return digitsNotMatched;
        }
        if (pos > 0 && Character.isDigit(string1.charAt(pos)) && Character.isDigit(string1.charAt(pos - 1))) {
            digitsNotMatched = tmp_cnt;
        }
        if (pos + len < size && Character.isDigit(string1.charAt(pos + len - 1)) && Character.isDigit(string1.charAt(pos + len))) {
            tmp_cnt = 1;
            int tmp_pos = pos + len - 1;
            while (Character.isDigit(string1.charAt(--tmp_pos))) {
                ++tmp_cnt;
            }
            digitsNotMatched += tmp_cnt;
        }

        return digitsNotMatched;
    }

    public Pair<String> getHomogenizedStringPair() {

        return homogenizedStringPair;
    }

    private int getPaddingNumberOfHomogenization(String[] posLenPairArray1, String[] posLenPairArray2) {

        int pairs = posLenPairArray1.length - 1;
        int gap1 = 0;
        int gap2 = 0;
        int previousEndPos1 = 0;
        int previousEndPos2 = 0;
        int paddingNumber = 0;
        String[] posAndLen;
        String commonSequence1;
        String commonSequence2;
        homogenizedStringPair = new Pair<>(string1, string2);
        homogenizedStringPair.first = string1;
        homogenizedStringPair.second = string2;
        for (int i = 0; i < pairs; ++i) {
            posAndLen = posLenPairArray1[i].split(":");
            int pos1 = Integer.parseInt(posAndLen[0]);
            int len1 = Integer.parseInt(posAndLen[1]);
            gap1 = pos1 - previousEndPos1;
            commonSequence1 = string1.substring(pos1, pos1 + len1);

            posAndLen = posLenPairArray2[i].split(":");
            int pos2 = Integer.parseInt(posAndLen[0]);
            int len2 = Integer.parseInt(posAndLen[1]);
            gap2 = pos2 - previousEndPos2;
            commonSequence2 = string2.substring(pos2, pos2 + len2);

            if (gap1 * gap2 == 0 && commonSequence1.equalsIgnoreCase(commonSequence2)) {
                paddingNumber += gap1 + gap2;
                String padding1 = string1.substring(previousEndPos1, pos1);
                String padding2 = string2.substring(previousEndPos2, pos2);
                if (gap1 == 0)
                    homogenizedStringPair.first = homogenizeByPadding(pos1, homogenizedStringPair.first, padding2);
                else
                    homogenizedStringPair.second = homogenizeByPadding(pos2, homogenizedStringPair.second, padding1);
            }

            previousEndPos1 = pos1 + len1;
            previousEndPos2 = pos2 + len2;
        }

        System.out.println(homogenizedStringPair.first);
        System.out.println(homogenizedStringPair.second);

        return paddingNumber;
    }

    private String homogenizeByPadding(int pos, String string, String padding) {

        String result;
        String front = string.substring(0, pos);
        result = front + padding;
        result += string.substring(pos);

        return result;
    }

    private List<Integer> extractPartition(String string1, String string2) {

        if (string1 == null || string2 == null)
            return null;

        List<Integer> separatorPosList = new ArrayList<>();

        for (int i = 0; i < string1.length() - 1; ++i) {
            char ch = string1.charAt(i);
            if (isSeparator(ch)) {
                separatorPosList.add(i);
            }
        }
        separatorPosList.add(-1);
        for (int i = 0; i < string2.length() - 1; ++i) {
            char ch = string2.charAt(i);
            if (isSeparator(ch)) {
                separatorPosList.add(i);
            }
        }

        return separatorPosList;
    }

    public static void main(String[] args) throws StrategyException {

        DiceCoeffTextStrategy strategy = new DiceCoeffTextStrategy(0.8);
        strategy.test_on = true;
//        Set<String> set = new HashSet<>();
//        set.add("0:2");
//        set.add("2:2");
//        System.out.println(strategy.getIsomorphism("ABCD", "EFAB", set));
//        System.out.println(strategy.getIsomorphismEntropyPair(new String[]{"1:2", "2:2", "4:4", "6"}, new String[]{"1:2", "2:3", "4:5", "6"}));
        strategy.addRules(TEXT_PROCESS_RULE.STRICT_NUMBER_MATCH);
//        strategy.addRules(TEXT_PROCESS_RULE.CHINESE2DIGITS);
//        strategy.addRules(TEXT_PROCESS_RULE.HOMOGENIZATION);
        strategy.addRules(TEXT_PROCESS_RULE.HEURISTIC_MATCH);
//        System.out.println(strategy.getNotMatchedDigitNumber("12a345a", "", 2, 5));
//        System.out.println("0 " + strategy.calcSimilarity("海淀区大钟寺东路太阳园小区13号楼会所2层", "北京市海淀区大钟寺东路太阳园小区会所二层"));
//        System.out.println("0 " + strategy.calcSimilarity("北京市海淀区大钟寺东路太阳园小区13号楼会所2层", "北京市海淀区大钟寺东路太阳园小区13号楼会所二层"));
//        System.out.println("1 " + strategy.calcSimilarity("麻辣大闸蟹母蟹a4只", "麻辣大闸蟹-4母"));
//        System.out.println("2 " + strategy.calcSimilarity("屈臣氏", "屈臣氏-苏打水"));
//        System.out.println("3 " + strategy.calcSimilarity("炭火烤鱼", "炭火烤鱼-清江鱼-麻辣味"));
//        System.out.println("4 " + strategy.calcSimilarity("B炭火A", "C炭火A"));
//        System.out.println("5 " + strategy.calcSimilarity("x123abc", "y23abc"));
//        System.out.println("1 " + strategy.calcSimilarity("abcdefg", "abcdeAAAcdefg"));
//        System.out.println("2 " + strategy.calcSimilarity("小蜜蜂", "蜂蜜小"));
        System.out.println("3 " + strategy.calcSimilarity("康师傅", "康帅傅"));
//        System.out.println("4 " + strategy.calcSimilarity("醉湘香园", "醉湘香阁"));
        System.out.println("5 " + strategy.calcSimilarity("健康师傅老李", "老李健康师傅"));
//        System.out.println("6 " + strategy.calcSimilarity("健康师傅", "师傅健康"));
//        System.out.println("7 " + strategy.calcSimilarity("康师傅abcfdEF一个", "帅a一个AB师bh康g"));
//        System.out.println("8 " + strategy.calcSimilarity("康师傅abcfdEF一个", "康师帅aABbhg一个"));
//        System.out.println("9 " + strategy.calcSimilarity("康师傅abcdefghAijklmnE一个", "帅a一个ABCDEF师GHIGKLMN康g"));
//        System.out.println("10 " + strategy.calcSimilarity("老李黄焖鸡米饭", "老李家黄焖鸡"));
//        System.out.println("11 " + strategy.calcSimilarity("老李黄焖鸡米饭", "老李家黄焖鸡（北苑店）"));
//        System.out.println("12 " + strategy.calcSimilarity("咿呀米美食工作餐快送", "咿呀米快餐"));
//        System.out.println("13 " + strategy.calcSimilarity("康师傅", "康帅傅"));
//        System.out.println("14 " + strategy.calcSimilarity("康师傅", "康师傅"));
//        System.out.println("15 " + strategy.calcSimilarity("圣焰麻辣小龙虾", "猎艳麻辣小龙虾"));
//        System.out.println("16 " + strategy.calcSimilarity("大虾来了小龙虾外卖", "大虾来了，小龙虾外卖"));
//        System.out.println("17 " + strategy.calcSimilarity("大虾来了小龙虾外卖", "不二江湖小龙虾"));
//        System.out.println("18 " + strategy.calcSimilarity("21cake", "11cake"));
//        System.out.println("19 " + strategy.calcSimilarity("爱尚幸福鲜花礼品", "爱尚幸福鲜花"));
        System.out.println("20 " + strategy.calcSimilarity("吉野家代购", "代购吉野家"));
//        System.out.println("21 " + strategy.calcSimilarity("秘制家小龙虾美食外卖", "秘制家小龙虾"));
//        System.out.println("22 " + strategy.calcSimilarity("麻库&小龙虾外卖", "秘制家小一龙虾"));
//        System.out.println("23 " + strategy.calcSimilarity("醉湘园", "醉湘阁"));
//        System.out.println("24 " + strategy.calcSimilarity("秘制家小龙虾", "麻辣诱惑麻辣小龙虾麻小外卖"));
//        strategy.removeRules(TEXT_PROCESS_RULE.STRICT_NUMBER_MATCH);
//        System.out.println(strategy.calcSimilarity("ebeecake小蜜蜂蛋糕", "ebeecake蛋糕"));
//        System.out.println(strategy.calcSimilarity("21cake", "11cake"));
//        System.out.println(strategy.calcSimilarity("ebeecake蛋糕", "ebeecake蛋糕"));
    }
}
