package com.foofv.crawler.similarity;

import com.foofv.crawler.similarity.coordinate.BD09LLCoordinate;
import com.foofv.crawler.similarity.coordinate.GCJCoordinate;
import com.foofv.crawler.similarity.coordinate.GeodeticCoordinate;
import com.foofv.crawler.similarity.coordinate_request.BaiduCoordinateRequest;
import com.foofv.crawler.similarity.coordinate_request.CoordinateRequest;
import com.foofv.crawler.similarity.strategy.CoordinateStrategy;
import com.foofv.crawler.similarity.coordinate_request.QQCoordinateRequest;
import com.foofv.crawler.similarity.strategy.*;
import com.foofv.crawler.similarity.structure.Pair;
import com.foofv.crawler.similarity.structure.Tuple;
import com.foofv.crawler.similarity.address.AddressSplice;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by msfenn on 21/09/15.
 */
public class AddressSimilarity {

    private Pair<String> addressPair = new Pair<>();

    private Tuple<TextMatchStrategy, CoordinateStrategy> strategyTuple = new Tuple<>();

    private Tuple<String, Boolean> addressHavingCoordinateTuple1;
    private Tuple<String, Boolean> addressHavingCoordinateTuple2;

    static private Pair<CoordinateRequest> coordinateRequestPair;

    static {
        CoordinateRequest bdCoordinateRequest = BaiduCoordinateRequest.getInstance();
        CoordinateRequest qqCoordinateRequest = QQCoordinateRequest.getInstance();
        coordinateRequestPair = new Pair<>(bdCoordinateRequest, qqCoordinateRequest);
    }

    public AddressSimilarity(Tuple<String, Boolean> addressHavingCoordinateTuple1, Tuple<String, Boolean> addressHavingCoordinateTuple2, DiceCoeffTextStrategy textStrategy, CoordinateStrategy coordStrategy) {

        this.addressHavingCoordinateTuple1 = addressHavingCoordinateTuple1;
        this.addressHavingCoordinateTuple2 = addressHavingCoordinateTuple2;

        addressPair.first = addressHavingCoordinateTuple1._1();
        addressPair.second = addressHavingCoordinateTuple2._1();

        strategyTuple.first = textStrategy;
        strategyTuple.second = coordStrategy;

        validate();
    }

    public AddressSimilarity(Pair<Tuple<String, Boolean>> addressHavingCoordinateTuplePair, Tuple<TextMatchStrategy, CoordinateStrategy> strategyTuple) {

        if (addressHavingCoordinateTuplePair != null && strategyTuple != null) {
            addressHavingCoordinateTuple1 = addressHavingCoordinateTuplePair._1();
            addressHavingCoordinateTuple2 = addressHavingCoordinateTuplePair._2();

            addressPair.first = addressHavingCoordinateTuple1._1();
            addressPair.second = addressHavingCoordinateTuple2._1();

            this.strategyTuple = strategyTuple;

            validate();
        }
    }

    private void validate() {

        boolean isValid = addressPair._1() != null && addressPair._2() != null;
//        isValid &= strategyTuple._1() instanceof DiceCoeffTextStrategy && strategyTuple._2() instanceof CoordinateStrategy;
        if (!isValid) {
            throw new RuntimeException("Parameter Error");
        }
    }

    public double getAddressSimilarity() {

        double similarity = 0;
        double textSimilarity = strategyTuple._1().calcSimilarity(addressPair._1(), addressPair._2());
        if (textSimilarity == 1)
            return textSimilarity;

        double coordinateSimilarity;
        if (strategyTuple._2().isValid() == false) {
            int baselineIdx = 0;
            String addressWithNoCoordinate = null;
            GeodeticCoordinate givenCoordinate = strategyTuple._2().getCoordinateIfNotNull();
            if (addressHavingCoordinateTuple1._2() && !addressHavingCoordinateTuple2._2()) {
                baselineIdx = 1;
                addressWithNoCoordinate = addressHavingCoordinateTuple2._1();
            } else if (!addressHavingCoordinateTuple1._2() && addressHavingCoordinateTuple2._2()) {
                baselineIdx = 2;
                addressWithNoCoordinate = addressHavingCoordinateTuple1._1();
            }
            if (givenCoordinate != null) {
                coordinateSimilarity = getCoordinateSimilarity(addressWithNoCoordinate, givenCoordinate);
            }
            if (strategyTuple._2().isSimilar() == false) {
                Tuple<List<String>, String> prefixSuffixAddressTuple = spliceAddress(baselineIdx);
                coordinateSimilarity = getCoordinateSimilarity(/*addressList*/prefixSuffixAddressTuple, givenCoordinate);
            }
        } else
            coordinateSimilarity = strategyTuple._2().calcSimilarity();

        return similarity;
    }

    private double getCoordinateSimilarity(Tuple<List<String>, String> prefixSuffixAddressTuple, GeodeticCoordinate givenCoordinate) {

        CoordinateRequest coordinateRequest;
        GeodeticCoordinate coordinate;
        GeodeticCoordinate lastCoordinate = null;
        List<String> addressList = prefixSuffixAddressTuple._1();
        int coordinateRequestIdx = 0;
        double coordinateSimilarity = 0;
        if (givenCoordinate == null) {
            String address = addressList.get(addressList.size() - 1);//the last one is another origin address
            addressList.remove(addressList.size() - 1);
            if ((coordinate = coordinateRequestPair._2().requestCoordinate(address)).isPrecise()) {
                givenCoordinate = coordinate;
                coordinateRequestIdx = 2;
            } else if ((coordinate = coordinateRequestPair._1().requestCoordinate(address)).isPrecise()) {
                givenCoordinate = coordinate;
                coordinateRequestIdx = 1;
            } else {
                //TODO: save this address for artificial resolution
                return 0;
            }
        } else {
            coordinateRequestIdx = getIndexOfCoordinateType(givenCoordinate);
        }

        String splicedAddress;
        GeodeticCoordinate prefixAddressCoordinate;
        coordinateRequest = coordinateRequestPair.get(coordinateRequestIdx);
        for (String prefixAddress : addressList) {
            prefixAddressCoordinate = coordinateRequest.requestCoordinate(prefixAddress);//if prefixAddress="", return null
            splicedAddress = prefixAddress + prefixSuffixAddressTuple._2();
            coordinate = coordinateRequest.requestCoordinate(splicedAddress);
            if (coordinate.equals(prefixAddressCoordinate) == false) { //if prefix address info doesn't override the origin address
                coordinateSimilarity = strategyTuple._2().calcSimilarity(givenCoordinate, coordinate);
                if (coordinate.isPrecise() && strategyTuple._2().isSimilar())
                    return coordinateSimilarity;
            }
            if (coordinate.equals(lastCoordinate))
                break;
            lastCoordinate = coordinate;
        }

        coordinateRequest = coordinateRequestPair.getAnother(coordinateRequestIdx);// requestCoordinateIdx is either 1 or 2
        givenCoordinate = convertToGCJCoordinate(givenCoordinate, coordinateRequestIdx);
        for (String prefixAddress : addressList) {
            prefixAddressCoordinate = coordinateRequest.requestCoordinate(prefixAddress);//if prefixAddress="", return null
            splicedAddress = prefixAddress + prefixSuffixAddressTuple._2();
            coordinate = coordinateRequest.requestCoordinate(splicedAddress);
            coordinate = convertToGCJCoordinate(coordinate, 3 - coordinateRequestIdx);
            if (coordinate.equals(prefixAddressCoordinate) == false) { //if prefix address info doesn't override the origin address
                coordinateSimilarity = strategyTuple._2().calcSimilarity(givenCoordinate, coordinate);
                if (coordinate.isPrecise() && strategyTuple._2().isSimilar())
                    return coordinateSimilarity;
            }
            if (coordinate.equals(lastCoordinate))
                break;
            lastCoordinate = coordinate;
        }

        return coordinateSimilarity;
    }

    private double getCoordinateSimilarity(String address, GeodeticCoordinate givenCoordinate) {

        GeodeticCoordinate coordinate = null;
        int idx = getIndexOfCoordinateType(givenCoordinate);

        coordinate = coordinateRequestPair.get(idx).requestCoordinate(address);
        double coordinateSimilarity = strategyTuple._2().calcSimilarity(givenCoordinate, coordinate);

        if (strategyTuple._2().isSimilar() == false) {
            coordinate = coordinateRequestPair.getAnother(idx).requestCoordinate(address);
            coordinate = convertToGCJCoordinate(coordinate, 3 - idx);
            givenCoordinate = convertToGCJCoordinate(givenCoordinate, idx);
            coordinateSimilarity = strategyTuple._2().calcSimilarity(givenCoordinate, coordinate);
        }

        return coordinateSimilarity;
    }

    private int getIndexOfCoordinateType(GeodeticCoordinate coordinate) {

        if (coordinate.getStandard().equals(BD09LLCoordinate.standard()))
            return 1;
        if (coordinate.getStandard().equals(GCJCoordinate.standard()))
            return 2;

        return 0;
    }

    private double getCoordinateSimilarityByIteration(List<String> addressList, GeodeticCoordinate givenCoordinate, int idxOfPairElem) {

        return 0;
    }

    private GeodeticCoordinate convertToGCJCoordinate(GeodeticCoordinate coordinate, int coordinateRequestIdx) {

        if (coordinateRequestIdx == 1) {
            return ((BD09LLCoordinate) coordinate).toGCJCoordinate();
        }

        return coordinate;
    }

    private Tuple<List<String>, String> spliceAddress(int baselineIdx) {

        //parameter order can't be changed
        AddressSplice addressSplice = new AddressSplice(addressHavingCoordinateTuple1._1(), addressHavingCoordinateTuple2._1());
        Tuple<List<String>, String> prefixSuffixAddressTuple = null;

        if (baselineIdx == 0)
            prefixSuffixAddressTuple = addressSplice.spliceBySegmentation();
        else
            prefixSuffixAddressTuple = addressSplice.spliceBySegmentation(baselineIdx);

        List<String> prefixAddressList = new ArrayList<>();

        if (baselineIdx == 0)
            prefixAddressList.add("");// the prefix and suffix concatenation is as same as suffix
        if (prefixSuffixAddressTuple._1() != null) {
            prefixSuffixAddressTuple._1().stream().forEach(address -> prefixAddressList.add(address));
//            for (String address : prefixSuffixAddressTuple._1()) {
//                prefixAddressList.add(address);
//            }
        }
        if (baselineIdx == 0)
            prefixAddressList.add(addressPair.getAnother(prefixSuffixAddressTuple._2()));

        return new Tuple<List<String>, String>(prefixAddressList, prefixSuffixAddressTuple._2());
    }

    private double getTextSimilarity() {

        double textSimilarity;
        textSimilarity = strategyTuple._1().calcSimilarity(addressPair._1(), addressPair._2());

        return textSimilarity;
    }

    public static void main(String[] args) throws TextMatchStrategy.StrategyException {

        Tuple<String, Boolean> tuple1 = new Tuple<>("北京市朝阳区北苑家园秋实街1号4层001", true /*false*/);
        Tuple<String, Boolean> tuple2 = new Tuple<>("易事达四层", false);
        DiceCoeffTextStrategy diceCoeffTextStrategy = new DiceCoeffTextStrategy(0);
        diceCoeffTextStrategy.addRules(TextMatchStrategy.TEXT_PROCESS_RULE.CHINESE2DIGITS);
        diceCoeffTextStrategy.addRules(TextMatchStrategy.TEXT_PROCESS_RULE.HOMOGENIZATION);
        CoordinateStrategy coordinateStrategy = new CoordinateStrategy(0.8, 250);
        coordinateStrategy.setCoordinateIfAnyNull(new GCJCoordinate(40.042255, 116.42163, true));
        AddressSimilarity addressSimilarity = new AddressSimilarity(tuple1, tuple2, diceCoeffTextStrategy, coordinateStrategy);
        addressSimilarity.getAddressSimilarity();
    }
}

