package com.foofv.crawler.similarity.structure;

/**
 * Created by msfenn on 21/09/15.
 */
public class Tuple<T, C> {

    public Tuple() {
    }

    public Tuple(T first, C second) {

        this.first = first;
        this.second = second;
    }

    public T _1() {

        return first;
    }

    public C _2() {

        return second;
    }

    public T first;
    public C second;
}
