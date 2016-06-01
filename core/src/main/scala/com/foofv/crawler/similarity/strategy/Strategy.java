package com.foofv.crawler.similarity.strategy;

/**
 * Created by msfenn on 15/09/15.
 */
public abstract class Strategy {

    protected double weight = 1;
    protected double confidence;
    protected double similarity;
    protected boolean test_on = false;

    public Strategy(double confidence, double... weight) {

        this.confidence = confidence;
        this.weight = weight.length > 0 ? weight[0] : this.weight;
        format();
    }

    public abstract double calcSimilarity(Object obj1, Object obj2);

    public abstract double calcSimilarity();

    public abstract boolean isSimilar();

    public boolean isSimilar(Object obj1, Object obj2) {

        calcSimilarity(obj1, obj2);

        return isSimilar();
    }

    protected void print(String msg) {

        if (test_on)
            System.out.print(msg);
    }

    protected void println(String msg) {

        if (test_on)
            System.out.println(msg);
    }


    public final void setWeight(double weight) {

        this.weight = weight;
    }

    public final void setConfidence(double confidence) {

        this.confidence = confidence;
    }

    protected boolean isValid(Object obj1, Object obj2) {

        if (obj1 == null || obj2 == null)
            return false;
        else
            return true;
    }

    abstract public boolean isValid();

    private void format() {

        weight = weight < 0 ? 0 : (weight > 1 ? 1 : weight);
        confidence = confidence < 0 ? 0 : (confidence > 1 ? 1 : confidence);
    }
}
