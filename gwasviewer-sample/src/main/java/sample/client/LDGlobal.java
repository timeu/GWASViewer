package sample.client;


import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * Created by uemit.seren on 9/23/15.
 */

@JsType(isNative = true,namespace = JsPackage.GLOBAL,name="Object")
public interface LDGlobal {
    @JsProperty
    float[] getR2();
    @JsProperty int[] getSnps();
}
