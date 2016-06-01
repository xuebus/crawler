package com.foofv.crawler.similarity.strategy;

import com.foofv.crawler.similarity.locale.CharacterUtil;
import com.foofv.crawler.similarity.utility.CharacterProcessUtil;
import com.foofv.crawler.similarity.structure.Pair;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by msfenn on 08/09/15.
 */
public abstract class TextMatchStrategy extends Strategy {

    public static enum TEXT_PROCESS_RULE {
        FULL_TEXT, FILTER_SPECIAL, HOMOGENIZATION, CHINESE2DIGITS, MULTI_LEVEL, HEURISTIC_MATCH, CASE_INSENSITIVE, SPACE_INSENSITIVE, STRICT_NUMBER_MATCH;
    }

    private static Pattern pattern = Pattern.compile("\\(|（|\\)|）");

    protected String string1;
    protected String string2;

    protected Set<TEXT_PROCESS_RULE> ruleSet = new HashSet<>(TEXT_PROCESS_RULE.values().length);

    public static class StrategyException extends Exception {

        private String reason;

        public StrategyException(String reason) {

            this.reason = reason;
        }

        public String getReason() {

            return reason;
        }

    }

    public TextMatchStrategy(double confidence, double... weight) {

        super(confidence, weight);
        ruleSet.add(TEXT_PROCESS_RULE.CASE_INSENSITIVE);
        ruleSet.add(TEXT_PROCESS_RULE.SPACE_INSENSITIVE);
        ruleSet.add(TEXT_PROCESS_RULE.FILTER_SPECIAL);
    }

    public void setTextPair(String string1, String string2) {

        this.string1 = string1;
        this.string2 = string2;
    }

    public void setTextPair(Pair<String> textPair) {

        if (textPair != null) {
            string1 = textPair._1();
            string2 = textPair._2();
        }
    }

    @Override
    public double calcSimilarity() {

        preProcess();
        similarity = calcTextSimilarity(string1, string2);

        return similarity;
    }

    @Override
    public double calcSimilarity(Object obj1, Object obj2) {

        if (!isValid(obj1, obj2))
            return -1;

        string1 = (String) obj1;
        string2 = (String) obj2;

        return calcSimilarity();
    }

    protected static boolean isSeparator(char ch) {

        return !CharacterUtil.isChineseWithSymbolExcluded(ch) && !Character.isAlphabetic(ch) && !Character.isDigit(ch);
    }

    private static String filterSpecialCharacters(String string) {

        String result = "";
        for (int i = 0; i < string.length(); ++i) {
            if (!isSeparator(string.charAt(i))) {
                result += string.charAt(i);
            }
        }

        return result;
    }

    private static String filterDescription(String string) {

        Matcher matcher = pattern.matcher(string);

        int leftBracketPos = 0;
        int rightBracketPos = -1;
        int beg = 0;
        int end = 0;
        String result = "";
        while (matcher.find(rightBracketPos + 1)) {
            leftBracketPos = matcher.start();
            matcher.find(leftBracketPos + 1);
            rightBracketPos = matcher.start();
            end = leftBracketPos;
            result += string.substring(beg, end);
            beg = rightBracketPos + 1;
        }

        return result.length() == 0 ? string : result;
    }

    protected void preProcess() {

        for (TEXT_PROCESS_RULE rule : ruleSet) {
            switch (rule) {
                case CASE_INSENSITIVE:
                    this.string1 = string1.toLowerCase();
                    this.string2 = string2.toLowerCase();
                    break;
                case SPACE_INSENSITIVE:
                    string1 = string1.replaceAll("\\s+", "");
                    string2 = string2.replaceAll("\\s+", "");
                    break;
                case CHINESE2DIGITS:
                    string1 = CharacterProcessUtil.replaceChineseWithDigit(string1);
                    string2 = CharacterProcessUtil.replaceChineseWithDigit(string2);
                    break;
                case FILTER_SPECIAL:
                    string1 = filterDescription(string1);
                    string2 = filterDescription(string2);

                    string1 = filterSpecialCharacters(string1);
                    string2 = filterSpecialCharacters(string2);
                    break;
            }
        }
    }

    protected abstract double calcTextSimilarity(String string1, String string2);

    public final void addRules(TEXT_PROCESS_RULE... rules) throws StrategyException {

        for (TEXT_PROCESS_RULE rule : rules) {
            ruleSet.add(rule);
        }
        if (ruleSet.contains(TEXT_PROCESS_RULE.FULL_TEXT) && ruleSet.contains(TEXT_PROCESS_RULE.FILTER_SPECIAL)) {
            ruleSet.clear();
            throw new StrategyException("Rule FULL_TEXT and FILTER_SPECIAL can not be coexisting");
        }
    }

    public final void removeRules(TEXT_PROCESS_RULE... rules) {

        for (TEXT_PROCESS_RULE rule : rules) {
            ruleSet.remove(rule);
        }
    }

    @Override
    public final boolean isSimilar() {

        return similarity >= confidence;
    }

    @Override
    public boolean isValid() {

        if (string1 == null || string2 == null)
            return false;
        else
            return true;
    }

    public static void main(String[] args) throws StrategyException {

        System.out.println(TextMatchStrategy.filterSpecialCharacters("老李\"家\",黄焖&鸡，"));
        System.out.println(TextMatchStrategy.filterDescription("老李家,黄焖鸡（北京）（北苑店）"));
    }
}
