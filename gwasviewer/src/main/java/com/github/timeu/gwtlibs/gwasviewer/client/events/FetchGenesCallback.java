package com.github.timeu.gwtlibs.gwasviewer.client.events;

import com.github.timeu.gwtlibs.geneviewer.client.event.Gene;
import com.google.gwt.core.client.JsArrayMixed;

import java.util.List;

/**
 * Created by uemit.seren on 9/17/15.
 */
@FunctionalInterface
public interface FetchGenesCallback {
    void onFetchGenes(JsArrayMixed genes);
}
