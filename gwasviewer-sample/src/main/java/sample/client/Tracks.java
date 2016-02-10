package sample.client;

import com.github.timeu.gwtlibs.gwasviewer.client.Track;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * Created by uemit.seren on 11/30/15.
 */

@JsType(isNative = true,namespace = JsPackage.GLOBAL,name="Object")
public interface Tracks {
    @JsProperty
    Track[] getTracks();
}
