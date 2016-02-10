package sample.client;


import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * Created by uemit.seren on 9/23/15.
 */
@JsType(isNative = true,name = "Object",namespace = JsPackage.GLOBAL)
public interface LDGlobalData {

    @JsProperty
    LDGlobal[] getData();

}
