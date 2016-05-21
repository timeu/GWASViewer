/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package sample.client;


import com.github.timeu.gwtlibs.geneviewer.client.event.ClickGeneEvent;
import com.github.timeu.gwtlibs.geneviewer.client.event.Gene;
import com.github.timeu.gwtlibs.geneviewer.client.event.HighlightGeneEvent;
import com.github.timeu.gwtlibs.geneviewer.client.event.UnhighlightGeneEvent;
import com.github.timeu.gwtlibs.gwasviewer.client.DisplayFeature;
import com.github.timeu.gwtlibs.gwasviewer.client.GWASViewer;
import com.github.timeu.gwtlibs.gwasviewer.client.Track;
import com.github.timeu.gwtlibs.gwasviewer.client.events.PointClickEvent;
import com.github.timeu.gwtlibs.gwasviewer.client.events.DeleteTrackEvent;
import com.github.timeu.gwtlibs.gwasviewer.client.events.FetchGeneInfoCallback;
import com.github.timeu.gwtlibs.gwasviewer.client.events.FetchGenesCallback;
import com.github.timeu.gwtlibs.gwasviewer.client.events.GeneDataSource;
import com.github.timeu.gwtlibs.gwasviewer.client.events.HighlightPointEvent;
import com.github.timeu.gwtlibs.gwasviewer.client.events.SelectTrackEvent;
import com.github.timeu.gwtlibs.gwasviewer.client.events.UploadTrackEvent;
import com.github.timeu.gwtlibs.gwasviewer.client.events.ZoomChangeEvent;
import com.github.timeu.gwtlibs.ldviewer.client.LDData;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayInteger;
import com.google.gwt.core.client.JsArrayMixed;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.googlecode.gwt.charts.client.ChartLoader;
import com.googlecode.gwt.charts.client.ChartPackage;
import com.googlecode.gwt.charts.client.DataTable;
import com.github.timeu.dygraphsgwt.client.callbacks.Point;
import com.googlecode.gwt.charts.client.DataView;
import com.googlecode.gwt.charts.client.RowFilter;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

import java.util.ArrayList;
import java.util.List;


/**
 * Initializes the application. Nothing to see here: everything interesting
 * happens in the presenters.
 */
public class SampleEntryPoint implements EntryPoint {

    public interface DataBundle extends ClientBundle {

        DataBundle INSTANCE = GWT.create(DataBundle.class);
        @Source("data/genes.json")
        TextResource genes();

        @Source("data/genes_with_features.json")
        TextResource genes_with_features();

        @Source("data/gwas_data.json")
        TextResource gwasData();

        @Source("data/ld_global_data.json")
        TextResource globalLDData();

       @Source("data/ld_sample_data.json")
       TextResource getLDData();

       @Source("data/tracks.json")
       TextResource getTracks();

       @Source("data/tracks_data.json")
       TextResource getTracksData();

    }
    @JsType(isNative = true,namespace = JsPackage.GLOBAL,name="Object")
    public interface GWASData  {

        @JsProperty
        double getMaxScore();

        @JsProperty double getBonferroniThreshold();

        @JsProperty String[] getChromosomes();

        @JsProperty int[] getChrLengths();

        @JsProperty String[] getGwasData();

    }
    GWASViewer gwasviewer;


    private String[] colors = {"green","blue",  "red", "cyan", "purple"};
    private String[] gene_mark_colors = {"red","red",  "blue", "red", "green"};
    private boolean showFeatures = false;
    private SNPPopup snpPopup = new SNPPopup();
    GWASData data;
    LDGlobal globabLdData;
    LDData ldData;
    TracksData tracksData;
    Tracks tracks;
    boolean hasSNPSelections = false;
    boolean hasDisplayFeatures = false;
    String statsprefix = "gwasviewerstats_";

    private Storage localStore;

    HTML eventPanel = new HTML();

    final GeneDataSource geneDataSource = new GeneDataSource() {
        @Override
        public void fetchGenes(String chr, int start, int end, boolean getFeatures, FetchGenesCallback callback) {
            if((start>=126508 && start <= 453243) || end>=126508 && end <= 453243 || (start <= 126508 && end >= 126508) || (end >=453243 && start <= 453243)) {
                callback.onFetchGenes(getData(getFeatures));
            }
        }

        @Override
        public void fetchGeneInfo(String name, FetchGeneInfoCallback callback) {
            callback.onFetchGeneInfo("Test description");
        }
    };


    @Override
    public void onModuleLoad() {
        localStore = Storage.getLocalStorageIfSupported();
        ChartLoader chartLoader = new ChartLoader(ChartPackage.CORECHART);
        chartLoader.loadApi(new Runnable() {
            @Override
            public void run() {
                data = JsonUtils.safeEval(DataBundle.INSTANCE.gwasData().getText());
                gwasviewer = new GWASViewer("Chr4", colors, "red", geneDataSource);

                initTracks();

                FlowPanel panel = new FlowPanel();
                //LayoutPanel panel = new LayoutPanel();

                ScrollPanel eventsPanel = new ScrollPanel();

                Button setSelectionBtn = new Button("Select 3 SNPs");
                Button setFeaturesBtn = new Button("Select 2 Features");

                HTML description = new HTML("<h2>How to use:</h2>" +
                        "<p><b>Zooming:</b>Left-Click + hold and drag</p>" +
                        "<p><b>Reset-Zoom:</b>Double-Click</p>" +
                        "<p>Zoom into the peak at the beginning of the plot to display the underlying genes<br />" +
                        "Clicking on a gene will fire the ClickGeneEvent and moving the mouse over a gene will fire the HighlightGeneEvent.<br />" +
                        "Click on the top SNP in the peak in the beginning of the GWAS plot to display popup with additional options (LD View)</p>");
                panel.add(description);
                panel.add(setSelectionBtn);
                panel.add(setFeaturesBtn);

                panel.add(gwasviewer);
                panel.add(eventsPanel);
                eventPanel.setHTML("<p><b>Events:</b></p>");
                eventsPanel.add(eventPanel);

                RootPanel.get().add(panel);

                loadChr(3);
                sinkEvents();
                snpPopup.getGlobalLDHandler().addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent clickEvent) {
                        LDGlobal ldData = getLDGlobalData();
                        gwasviewer.showColoredLDValues(276143, ldData.getSnps(), ldData.getR2());
                    }
                });
                snpPopup.getLDTriangleHandler().addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent clickEvent) {
                        LDData ldData = getLDData();
                        gwasviewer.loadLDPlot(ldData.getSnps(), ldData.getR2(), ldData.getStart(), ldData.getEnd());
                        logEvent("LDTriangle option clicked");
                    }
                });

                setFeaturesBtn.addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent clickEvent) {
                        if (hasDisplayFeatures) {
                            gwasviewer.clearDisplayFeatures();
                            setFeaturesBtn.setText("Select 2 Features");
                            hasDisplayFeatures = false;
                        } else {
                            gwasviewer.addDisplayFeature(new DisplayFeature("AT4G00651.1", 271486, 271879, "red"), true);
                            gwasviewer.addDisplayFeature(new DisplayFeature("Some interesting region", 8753993, 9241760, "green"), true);
                            setFeaturesBtn.setText("Clear Features");
                            hasDisplayFeatures = true;
                            gwasviewer.refresh();
                        }
                    }
                });

                setSelectionBtn.addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent clickEvent) {
                        if (hasSNPSelections) {
                            gwasviewer.clearSelection();
                            setSelectionBtn.setText("Select 3 SNPs");
                            hasSNPSelections = false;
                        } else {
                            List<Integer> selections = new ArrayList<>();
                            selections.add(148);
                            selections.add(1220);
                            selections.add(2240);
                            gwasviewer.setSelections(selections,false);
                            setSelectionBtn.setText("Clearfromat SNP selections");
                            hasSNPSelections = true;
                        }

                    }
                });
            }
        });


    }

    private void initTracks() {
        Track[] defaultTracks = getTracks().getTracks();
        List<Track> tracks = new ArrayList<>();
        for (Track track: defaultTracks) {
            tracks.add(track);
        }
        tracks.addAll(getCustomTracks());
        gwasviewer.setTracks(tracks.toArray(new Track[]{}));
    }

    private void loadChr(int chr) {
        gwasviewer.draw(DataTable.create(data.getGwasData()[chr]), data.getMaxScore(), data.getBonferroniThreshold(), data.getChrLengths()[chr]);
    }


   private void sinkEvents() {
        gwasviewer.addClickGeneHandler(new ClickGeneEvent.Handler() {
            @Override
            public void onClickGene(ClickGeneEvent event) {
                logEvent("ClickGeneEvent called: Gene: " + getMessageFromGene(event.getGene()));
            }
        });

        gwasviewer.addUnhighlightGeneHandler(new UnhighlightGeneEvent.Handler() {

            @Override
            public void onUnhighlightGene(UnhighlightGeneEvent event) {
                logEvent("UnhighlightGeneEvent called");
            }
        });
       gwasviewer.addHighlightGeneHandler(new HighlightGeneEvent.Handler() {
           @Override
           public void onHightlightGene(HighlightGeneEvent event) {
               logEvent("HighlightGeneEvent called: Gene: " + getMessageFromGene(event.getGene()));
           }
       });

       gwasviewer.addZoomChangeHandler(new ZoomChangeEvent.Handler() {
           @Override
           public void onZoomResize(ZoomChangeEvent event) {
               //zoomLabel.setHTML("<b>" + event.start + "</b> - <b>" + event.stop + "</b>");
               logEvent("ZoomResizeEvent called: start: " + event.start + ", end:" + event.stop);
           }
       });
       gwasviewer.addHighlightPointHandler(new HighlightPointEvent.Handler() {

           @Override
           public void onHighlight(HighlightPointEvent event) {
               //logEvent("HighlightPointEvent called: Points: " + getMessageFromPoints(event.points));
           }
       });

       gwasviewer.addPointClickHandler(new PointClickEvent.Handler() {
           @Override
           public void onClick(PointClickEvent event) {
               logEvent("ClickPointEvent called:Point: " + getMessageFromPoint(event.point));
               int x = (int)event.point.getXval();
               snpPopup.setDataPoint(4, x);
               snpPopup.setPopupPosition(event.event.getClientX(), event.event.getClientY() - 47);
               if (x == 276143) {
                   snpPopup.show();
               }
           }
       });

       gwasviewer.addSelectTrackHandler(new SelectTrackEvent.Handler() {
           @Override
           public void onSelectTrack(SelectTrackEvent event) {
               logEvent("SelectTrackEvent called: id: " + event.getId()+", isStacked: " + event.isStacked());
               TracksData data = getTracksData();
               DataTable table = null;
               if (event.getId().equalsIgnoreCase("genecount")) {
                   table = DataTable.create(data.getGenecount());
               }
               else if (event.getId().equalsIgnoreCase("phs")) {
                   table = DataTable.create(data.getPhs());
               }
               else  if (event.getId().equalsIgnoreCase("clr")) {
                   table = DataTable.create(data.getClr());
               }
               else {
                   table = DataTable.create(localStore.getItem(statsprefix+event.getId()));
                   DataView view = DataView.create(table);
                   JsArrayInteger columns = JsArrayInteger.createArray(2).cast();
                   columns.set(0,1);
                   columns.set(1,2);
                   RowFilter chrFilter = RowFilter.create();
                   chrFilter.setColumn(0);
                   chrFilter.setValue("4");
                   JsArray<RowFilter> filter = JsArray.createArray(1).cast();
                   filter.set(0,chrFilter);
                   view.setRows(view.getFilteredRows(filter).cast());
                   view.setColumns(columns);
                   table = toDataTable(view);

               }
               if (table != null) {
                   gwasviewer.setTrackData(event.getId(), event.isStacked(),table);
               }
           }
       });

       gwasviewer.addUploadTrackHandler(new UploadTrackEvent.Handler() {
           @Override
           public void onUploadTack(UploadTrackEvent event) {
               initTracks();
           }
       });

       gwasviewer.addDeleteTrackHandler(new DeleteTrackEvent.Handler() {
           @Override
           public void onDeleteTrack(DeleteTrackEvent event) {
               localStore.removeItem(statsprefix+event.getId());
               initTracks();
           }
       });
    }

    //FIXME until https://github.com/google/gwt-charts/issues/60 is fixed
    private final native DataTable toDataTable(DataView view) /*-{
        return view.toDataTable();
    }-*/;


    private String getMessageFromGene(Gene gene) {
        return "Name: "+gene.name+", start: "+gene.start+", end: " + gene.end+", chr: " + gene.chromosome;
    }

    private String getMessageFromPoints(Point[] points) {
        String pointMessages = "";
        for (Point point: points) {
            pointMessages+="[" + getMessageFromPoint(point) +" ]<br />";
        }
        return pointMessages;
    }

    private String getMessageFromPoint(Point point) {
        return "Name: "+point.getName()+", x: "+point.getX() + ", y: " + point.getY()+", xval: "
                + point.getXval() +", yval: " + point.getYval() + ", idx: " + point.getIdx()+", canvasx: " + point.getCanvasx()+", canvasy: " + point.getCanvasy();
    }


    private JsArrayMixed getData(boolean isFeatures) {
        final String jsonData = isFeatures ? DataBundle.INSTANCE.genes_with_features().getText() : DataBundle.INSTANCE.genes().getText();
        JsArrayMixed data = JsonUtils.safeEval(jsonData);
        return data;
    }

    private LDGlobal getLDGlobalData() {
        if (globabLdData == null) {
            LDGlobalData data = JsonUtils.safeEval(DataBundle.INSTANCE.globalLDData().getText());
            globabLdData = data.getData()[3];
        }
        return globabLdData;
    }

    private void logEvent(String event) {
        eventPanel.setHTML(eventPanel.getHTML()+"<div>"+event+"</div>");
    }


    private LDData getLDData() {
        if (ldData == null) {
            ldData = JsonUtils.safeEval(DataBundle.INSTANCE.getLDData().getText());
        }
        return ldData;
    }

    private Tracks getTracks() {
        if (tracks == null) {
            tracks = JsonUtils.safeEval(DataBundle.INSTANCE.getTracks().getText());
        }
        return tracks;
    }

    private TracksData getTracksData() {
        if (tracksData == null) {
            tracksData = JsonUtils.safeEval(DataBundle.INSTANCE.getTracksData().getText());
        }
        return tracksData;
    }

    private List<Track> getCustomTracks() {
        List<Track> customTracks = new ArrayList<>();
        int lengthOfPrefix = statsprefix.length();
        for (int i=0;i<localStore.getLength();i++) {
            String key = localStore.key(i);
            if (key.substring(0,lengthOfPrefix) == statsprefix) {
                Track track = new Track();
                DataTable data = DataTable.create(localStore.getItem(key));
                track.id= data.getColumnId(2);
                track.name = data.getColumnLabel(2);
                track.canDelete = true;
                track.isStackable=true;
                customTracks.add(track);
            }
        }
        return customTracks;
    }
}
