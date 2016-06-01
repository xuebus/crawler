package com.foofv.crawler.similarity.locale;

import java.util.regex.Pattern;

/**
 * Created by msfenn on 08/09/15.
 */
public class CharacterUtil {

    public static boolean isChinese(char c) {

        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION) {
            return true;
        }
        return false;
    }

    public static boolean isChineseWithSymbolExcluded(char str) {

        Pattern pattern = Pattern.compile("[\\u4E00-\\u9FBF]+");
        return pattern.matcher("" + str).find();
    }

    public static void main(String[] args) {

        System.out.println(isChineseWithSymbolExcluded('。'));
        System.out.println(isChineseWithSymbolExcluded(','));
        System.out.println(isChineseWithSymbolExcluded('中'));
        System.out.println(isChineseWithSymbolExcluded('，'));
        System.out.println(isChineseWithSymbolExcluded('·'));
    }
}
