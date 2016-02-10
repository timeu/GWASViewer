package sample.client;

import com.googlecode.gwt.charts.client.DataTable;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * Created by uemit.seren on 11/30/15.
 */

@JsType(isNative = true,namespace = JsPackage.GLOBAL,name="Object")
public interface TracksData {

    @JsProperty
    String getGenecount();
    @JsProperty
    String getClr();
    @JsProperty
    String getPhs();

}
