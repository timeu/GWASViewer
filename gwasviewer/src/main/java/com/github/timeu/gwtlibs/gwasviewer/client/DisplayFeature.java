package com.github.timeu.gwtlibs.gwasviewer.client;

/**
 * Created by uemit.seren on 10/2/15.
 */
public class DisplayFeature {

    public final String name;
    public final int start;
    public final int end;
    public final String color;


    public DisplayFeature(String name, int start, int end,String color) {
        this.name = name;
        this.start = start;
        this.end = end;
        this.color = color;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DisplayFeature that = (DisplayFeature) o;

        if (start != that.start) return false;
        if (end != that.end) return false;
        return name != null ? name.equals(that.name) : that.name == null;

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + start;
        result = 31 * result + end;
        return result;
    }
}
