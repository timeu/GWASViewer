package com.github.timeu.gwtlibs.gwasviewer.client.util;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * Created by uemit.seren on 2/10/16.
 */
@JsType(isNative = true,namespace = JsPackage.GLOBAL)
public interface FileReader {

    void readAsArrayBuffer(Blob blob);
    void readAsText(Blob blob, String label);
    void readAsDataURL(Blob blob);

    @JsProperty
    int getReadyState();

    @JsProperty
    void setOnloadend(LoadEndHandler handler);

    @JsProperty
    String getResult();
}
