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
}
