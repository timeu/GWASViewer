package com.github.timeu.gwtlibs.gwasviewer.client.util;

import jsinterop.annotations.JsFunction;

/**
 * Created by uemit.seren on 2/10/16.
 */
@JsFunction
@FunctionalInterface
public interface LoadEndHandler {

    void onLoadEnd();
}
