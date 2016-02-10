package com.github.timeu.gwtlibs.gwasviewer.client;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;


/**
 * Created by uemit.seren on 11/30/15.
 */
@JsType(isNative = true,namespace = JsPackage.GLOBAL,name="Object")
public class Track {
    public String id;
    public String name;
    public boolean isStackable;
    public boolean canDelete;
}
