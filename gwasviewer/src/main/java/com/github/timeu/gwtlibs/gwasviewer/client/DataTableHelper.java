package com.github.timeu.gwtlibs.gwasviewer.client;

import com.googlecode.gwt.charts.client.DataTable;
import jsinterop.annotations.JsType;

/**
 * Created by uemit.seren on 12/1/15.
 */
@JsType(isNative = true,namespace = "google.visualization",name = "data")
public final class DataTableHelper {

    public static native DataTable join(DataTable dt1,DataTable dt2,String joinMethod,int[][]keys,int[] dt1Columns,int[]dt2Columns);
}
