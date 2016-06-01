package com.foofv.crawler.similarity.utility;

import com.foofv.crawler.parse.master.ParserMasterMsg;
import org.apache.commons.collections.ResettableListIterator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by msfenn on 17/09/15.
 */
public class CharacterProcessUtil {

    private static Map<Character, String> chinese2DigitMap = new HashMap<>();

    static {
        init();
    }

    private static void init() {

        chinese2DigitMap.put('零', "0");
        chinese2DigitMap.put('一', "1");
        chinese2DigitMap.put('二', "2");
        chinese2DigitMap.put('三', "3");
        chinese2DigitMap.put('四', "4");
        chinese2DigitMap.put('五', "5");
        chinese2DigitMap.put('六', "6");
        chinese2DigitMap.put('七', "7");
        chinese2DigitMap.put('八', "8");
        chinese2DigitMap.put('九', "9");
        chinese2DigitMap.put('十', "10");
    }

    private static String calcNumber(List<Character> chineseList) {

        if (chineseList.size() == 0) {
            return "";
        }

        int sum = 0;
        int preDigit = 1;
        for (char ch : chineseList) {
            if (ch == '十') {
                sum += preDigit * 10;
                sum -= preDigit;
            } else {
                preDigit = Integer.parseInt(chinese2DigitMap.get(ch));
                sum += preDigit;
            }
        }
        if (chineseList.size() == 2)
            sum += 1;

        return Integer.toString(sum);
    }

    public static String replaceChineseWithDigit(String string) {

        String result = "";
        char ch;
        List<Character> chineseList = new ArrayList<>(3);
        for (int i = 0; i < string.length(); ++i) {
            ch = string.charAt(i);
            if (chinese2DigitMap.containsKey(ch)) {
                chineseList.add(ch);
            } else {
                result += calcNumber(chineseList);
                result += ch;
                chineseList.clear();
            }
        }

        return result;
    }

    public static boolean isAlphaNumber(char ch) {

        boolean result = Character.isLowerCase(ch) || Character.isUpperCase(ch) || Character.isDigit(ch);

        return result;
    }

    public static void main(String[] args) {

//        System.out.println(replaceChineseWithDigit("易事达零层"));
        System.out.println(isAlphaNumber('易'));
    }
}
