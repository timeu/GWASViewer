package com.github.timeu.gwtlibs.gwasviewer.client.events;

/**
 * Created by uemit.seren on 9/17/15.
 */
public interface GeneDataSource {

    void fetchGenes(String chr, int start, int end, boolean getFeatures, FetchGenesCallback callback);
    void fetchGeneInfo(String name,FetchGeneInfoCallback callback);
}
