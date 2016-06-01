package com.foofv.crawler.similarity.address;

import com.foofv.crawler.similarity.structure.Pair;
import com.foofv.crawler.similarity.structure.Tuple;
import com.foofv.crawler.similarity.utility.Segmentation;
import com.foofv.crawler.similarity.utility.TextProcessUtil;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by msfenn on 16/09/15.
 */
public class AddressSplice {

    private String address1;
    private String address2;

    private Pair<String> addressPair;

    private List<String> detailedAddressWordSegList = null;
    private List<String> sketchyAddressWordSegList = null;
    private String sketchyAddress = null;

    public AddressSplice(String address1, String address2) {

        addressPair = new Pair<>(address1, address2);

        this.address1 = address1;
        this.address2 = address2;
    }

    public AddressSplice(Pair<String> addressPair) {

        if (addressPair != null) {
            this.addressPair = addressPair;

            address1 = addressPair._1();
            address2 = addressPair._2();
        }
    }

    public Tuple<List<String>, String> spliceBySegmentation() {

        filter();
        List<String> address1WordSegList = new Segmentation().segment(address1);
        List<String> address2WordSegList = new Segmentation().segment(address2);
        /*List<String>*/
        detailedAddressWordSegList = null;
        /*List<String>*/
        sketchyAddressWordSegList = null;
        /*String*/
        sketchyAddress = null;

        if (!prepareForSplice(address1WordSegList, address2WordSegList, false)) {
            if (address1WordSegList.size() > address2WordSegList.size()) {
                detailedAddressWordSegList = address1WordSegList;
                sketchyAddressWordSegList = address2WordSegList;
                sketchyAddress = address2;
            } else {
                detailedAddressWordSegList = address2WordSegList;
                sketchyAddressWordSegList = address1WordSegList;
                sketchyAddress = address1;
            }
        }

        return spliceBySegmentation(detailedAddressWordSegList, sketchyAddressWordSegList, sketchyAddress);
    }

    public Tuple<List<String>, String> spliceBySegmentation(int baselineIdx) {

        if (baselineIdx != 1 && baselineIdx != 2)
            return null;

        filter();
        List<String> address1WordSegList = new Segmentation().segment(address1);
        List<String> address2WordSegList = new Segmentation().segment(address2);
        Pair<List<String>> addressWordSegListPair = new Pair<>(address1WordSegList, address2WordSegList);
        /*List<String>*/
        detailedAddressWordSegList = null;
        /*List<String>*/
        sketchyAddressWordSegList = null;
        /*String*/
        sketchyAddress = addressPair.getAnother(baselineIdx);

        List<String> referredAddressWordSegList = addressWordSegListPair.get(baselineIdx);
        List<String> referringAddressWordSegList = addressWordSegListPair.getAnother(baselineIdx);
        if (!prepareForSplice(referredAddressWordSegList, referringAddressWordSegList, true))
            if (referredAddressWordSegList.size() >= referringAddressWordSegList.size()) {
                detailedAddressWordSegList = referredAddressWordSegList;//addressWordSegListPair.get(baselineIdx);
                sketchyAddressWordSegList = referringAddressWordSegList;//addressWordSegListPair.getAnother(baselineIdx);
            }

        return spliceBySegmentation(detailedAddressWordSegList, sketchyAddressWordSegList, sketchyAddress);
    }

    private void filter() {

        address1 = TextProcessUtil.filter(address1);
        address2 = TextProcessUtil.filter(address2);
    }

    // address1WordSegList is the referred list if baselineIdx is passed in
    private boolean prepareForSplice(List<String> address1WordSegList, List<String> address2WordSegList, boolean hasReference) {

        boolean canSplice = true;
        String firstWordSegOfAddress1 = null;
        String firstWordSegOfAddress2 = null;
        for (String word : address1WordSegList) {
            if (word.length() > 1) {
                firstWordSegOfAddress1 = word;
                break;
            }
        }
        for (String word : address2WordSegList) {
            if (word.length() > 1) {
                firstWordSegOfAddress2 = word;
                break;
            }
        }

        int address1Level = ChinaDistrictQuery.getDistrictLevel(firstWordSegOfAddress1);
        int address2Level = ChinaDistrictQuery.getDistrictLevel(firstWordSegOfAddress2);
        if (address1Level < address2Level) {
            detailedAddressWordSegList = address1WordSegList;
            sketchyAddressWordSegList = address2WordSegList;
            sketchyAddress = address2;
        } else if (address2Level < address1Level) {
            if (hasReference)
                canSplice = false;
            else {
                detailedAddressWordSegList = address2WordSegList;
                sketchyAddressWordSegList = address1WordSegList;
                sketchyAddress = address1;
            }
        } else
            canSplice = false;

        return canSplice;
    }

    private Tuple<List<String>, String> spliceBySegmentation(List<String> detailedAddressWordSegList, List<String> sketchyAddressWordSegList, String sketchyAddress) {

        if (detailedAddressWordSegList == null || sketchyAddressWordSegList == null)
            return new Tuple<>(null, sketchyAddress);

        LinkedList<String> splicedAddressList = new LinkedList<>();
        Tuple<List<String>, String> prefixSuffixAddressTuple = new Tuple<>(null, sketchyAddress);
        char flag = '\0';

        for (String wordSeg : sketchyAddressWordSegList) {
            if (Segmentation.getSpecialWordSet().contains("" + wordSeg.charAt(wordSeg.length() - 1))) {
                flag = wordSeg.charAt(wordSeg.length() - 1);
                break;
            }
        }
        int idx = -1;
        if (flag != '\0') {
            for (String wordSeg : detailedAddressWordSegList) {
                ++idx;
                if (wordSeg.charAt(wordSeg.length() - 1) == flag || wordSeg.contains(sketchyAddressWordSegList.get(0))) {
                    --idx;
                    break;
                }
            }
        }
        String splicedAddress = "";
        if (idx == detailedAddressWordSegList.size() - 1) {
            idx *= 0.7;// artificial setting value
        }
        for (int i = 0; i <= idx; ++i) {
            splicedAddress += detailedAddressWordSegList.get(i);
            splicedAddressList.addFirst(splicedAddress);
        }
        if (idx != -1) {
            prefixSuffixAddressTuple.first = splicedAddressList;
        }

        return prefixSuffixAddressTuple;
    }

    public static void main(String[] args) {

//        AddressSplice addressSplice = new AddressSplice("北京市海淀区大钟寺东路太阳园小区会所二层", "海淀区大钟寺东路太阳园小区13号楼会所2层");
//        AddressSplice addressSplice = new AddressSplice("海淀区大钟寺东路太阳园小区13号楼会所2层", "北京市海淀区大钟寺东路太阳园小区会所二层");
        AddressSplice addressSplice = new AddressSplice("易事达四层", "北京市朝阳区北苑家园秋实街1号4层001");
//        AddressSplice addressSplice = new AddressSplice("地铁立水桥南站东南口C口出，往北走30米六号院内", "怀柔区开放路113号南四层409室");
        Tuple<List<String>, String> tuple = addressSplice.spliceBySegmentation();
        List<String> list = tuple._1();
        String address = tuple._2();
        System.out.println();
        System.out.println("sketchy addr: " + address);
        for (String addr : list)
            System.out.println(addr);
    }

}
