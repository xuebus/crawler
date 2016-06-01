package com.foofv.crawler.similarity.utility;

import com.chenlb.mmseg4j.*;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Created by msfenn on 6/09/15.
 */
public class Segmentation {

    private Dictionary dictionary;
    private Seg seg;
    private String sentence;
    private static String[] specialWords = {"路", "街", "弄", "区", "号", "号楼", "线", "广场", "层", "层楼", "楼", "幢", "幢楼", "门", "大厦", "城", "室", "园", "院", "口", "桥",
            "房", "东", "南", "西", "北", "东路", "南路", "西路", "北路"};

    private static Set<String> specialWordSet = new HashSet<>();

    static {
        initStatic();
    }

    private static void initStatic() {

        for (String word : specialWords) {
            specialWordSet.add(word);
        }
    }

    public static Set<String> getSpecialWordSet() {

        return specialWordSet;
    }

    public Segmentation() {

        init();
    }

    public Segmentation(String dictionaryPath) {

        init(dictionaryPath);
    }

    private void init(String... dictionaryPath) {

        System.setProperty("mmseg.dic.path", "/home/msfenn/Downloads/data");
        if (dictionaryPath.length == 0)
            dictionary = Dictionary.getInstance();
        else
            dictionary = Dictionary.getInstance(dictionaryPath[0]);
        seg = new ComplexSeg(dictionary);
    }

    public List<String> segment(String input) {

        sentence = input.toLowerCase();

        return segment(new StringReader(sentence));
    }

    private List<String> segment(StringReader reader) {

        StringBuilder sb = new StringBuilder();
        List<String> wordList = new LinkedList<>();
        MMSeg mmseg = new MMSeg(reader, seg);
        Word word = null;
        String wordStr;
        boolean first = true;
        try {
            while ((word = mmseg.next()) != null) {
                if (!first) {
                    sb.append("/");
                }
                wordStr = word.getString();
                wordList.add(wordStr);
                sb.append(wordStr);
                first = false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(sb.toString());

        catenateWithSpecialWord(wordList);

        return wordList;
    }

    private void catenateWithSpecialWord(List<String> wordList) {

        String word;
        int len = wordList.get(0).length();
        for (int i = 1; i < wordList.size(); ++i) {
            word = wordList.get(i);
            if (sentence.charAt(len) != word.charAt(0)) {
                int pos = sentence.indexOf(word.charAt(0), len + 1);
                if (sentence.charAt(len) == '-' && CharacterProcessUtil.isAlphaNumber(sentence.charAt(len + 1)) && CharacterProcessUtil.isAlphaNumber(sentence.charAt(len - 1))) {
                    wordList.set(i - 1, wordList.get(i - 1) + "-" + word);
                    wordList.remove(i);
                    --i;
                }
                len = pos;
            } else if (specialWordSet.contains(word)) {
                int singleCharacterPos = i - 2;
                while (singleCharacterPos >= 0 && wordList.get(singleCharacterPos).length() == 1)
                    --singleCharacterPos;
                String catenationStr = wordList.get(++singleCharacterPos);
                for (int k = singleCharacterPos + 1; k <= i; ++k) {
                    catenationStr += wordList.get(k);
                }
                for (int k = singleCharacterPos; k < i; ++k) {
                    wordList.remove(singleCharacterPos);
                }
                wordList.set(singleCharacterPos, catenationStr);
                i -= i - singleCharacterPos;
            }
            len += word.length();
        }
    }

    public void refreshDictionary() {

        if (dictionary.wordsFileIsChange())
            dictionary.reload();
    }

    public static void main(String[] args) throws IOException {

//        Map<File, String> map = new HashMap<>();
//        String string = "file:/home/msfenn/Downloads/data";
//        string = string.substring(string.indexOf(":") + 1);
//        File file = new File(string);
//        map.put(file, "");
//        File file1 = new File(string);
//        map.put(file1, "");
//        System.out.println(map.size());
        System.out.println(new Segmentation().segment("北京朝阳区北苑路a~142-05号线立水桥门脸房易初莲花购物中心首层218号-CF1-05"));
        System.out.println(new Segmentation().segment("立路218号~CF1-05"));
        System.out.println(new Segmentation().segment("地铁立水桥南站东南口C口出，往北走30米六号院内"));
        System.out.println(new Segmentation().segment("怀柔区开放路113号南四层409室"));
        System.out.println(new Segmentation().segment("麻辣花螺"));
    }
}
