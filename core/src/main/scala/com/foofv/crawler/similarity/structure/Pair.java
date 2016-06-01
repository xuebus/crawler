package com.foofv.crawler.similarity.structure;

/**
 * Created by msfenn on 08/09/15.
 */

public class Pair<T> extends Tuple<T, T> {

    public Pair() {
    }

    public Pair(T first, T second) {

        this.first = first;
        this.second = second;
    }

    public T get(int idx) {

        if (idx == 1)
            return first;
        if (idx == 2)
            return second;

        return null;
    }

    public T getAnother(int idx) {

        idx = 3 - idx;

        return get(idx);
    }

    public T getAnother(T elem) {

        if (elem.equals(first))
            return second;
        if (elem.equals(second))
            return first;

        return null;
    }
}
