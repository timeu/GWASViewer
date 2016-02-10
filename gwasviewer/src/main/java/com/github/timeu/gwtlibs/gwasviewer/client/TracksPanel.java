package com.github.timeu.gwtlibs.gwasviewer.client;

import com.github.timeu.dygraphsgwt.client.Dygraphs;
import com.github.timeu.dygraphsgwt.client.DygraphsJs;
import com.github.timeu.dygraphsgwt.client.DygraphsOptions;
import com.github.timeu.dygraphsgwt.client.callbacks.Area;
import com.github.timeu.dygraphsgwt.client.callbacks.UnderlayCallback;
import com.github.timeu.dygraphsgwt.client.options.AxesOptions;
import com.github.timeu.dygraphsgwt.client.options.AxisOptions;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.core.client.JsArrayInteger;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.googlecode.gwt.charts.client.DataTable;
import com.googlecode.gwt.charts.client.DataView;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by uemit.seren on 9/1/15.
 */
public class TracksPanel extends Composite {

    private Dygraphs dygraphs;

    private DataTable trackData;
    private List<Integer> activeColumns = new ArrayList<>();
    private int[][] joinKeys = {{0, 0}};
    private FlowPanel panel = new FlowPanel();
    private int zoomStart = 0;
    private int zoomEnd = 0;

    public TracksPanel() {
        panel.getElement().getStyle().setPosition(Style.Position.RELATIVE);
        initWidget(panel);
        setHeight("100px");
        trackData = DataTable.create();
    }

    public boolean isTrackLoaded(String id) {
        return getColumnIndex(trackData,id) != -1;
    }

    private final static native int getColumnIndex(DataTable datatable,String id) /*-{
        return datatable.getColumnIndex(id);
    }-*/;




    public void addTrackToDisplay(String id,DataTable dataTable, boolean isStacked) {
        int ix = getColumnIndex(trackData,id);
        if (ix != -1) {
            throw new RuntimeException("Track "+id+" already available");
        }
        if (trackData.getNumberOfColumns() == 0) {
            trackData = dataTable;
            ix = 1;
        }
        else {
            int[] dt1Columns = new int[trackData.getNumberOfColumns()-1];
            for (int i =1;i<trackData.getNumberOfColumns();i++) {
                dt1Columns[i-1] = i;
            }
            trackData = DataTableHelper.join(trackData,dataTable,"full",joinKeys,dt1Columns,new int[]{1});
            ix = trackData.getNumberOfColumns()-1;
        }
        addTrackToDisplay(ix,isStacked);
    }

    public void addTrackToDisplay(String id, boolean isStacked) {
        int ix = getColumnIndex(trackData,id);
        if (ix == -1) {
            throw new RuntimeException("Track "+id+" must be loaded first");
        }
        addTrackToDisplay(ix,isStacked);
    }

    public void addTrackToDisplay(int ix, boolean isStacked) {
        if (!activeColumns.contains(ix) || (isStacked==false && activeColumns.size() > 1)) {
            if (isStacked) {
                activeColumns.add(ix);
            }
            else {
                activeColumns.clear();
                activeColumns.add(ix);
            }
            filterAndDisplay();
        }
        else {
            removeTrackFromDisplay(ix);
        }
    }

    public void removeTrackFromDisplay(String id) {
        int ix = getColumnIndex(trackData,id);
        if (ix == -1) {
            return;
        }
        removeTrackFromDisplay(ix);
    }

    public void removeTrackFromDisplay(int ix) {
        if (!activeColumns.contains(ix))
            return;
        activeColumns.remove(new Integer(ix));
        filterAndDisplay();
    }

    private void filterAndDisplay() {
        JsArrayInteger columns = JsArrayInteger.createArray().cast();
        for (int i=1;i<activeColumns.size()+1;i++) {
            columns.set(i,activeColumns.get(i-1));
        }
        columns.set(0,0);
        DataView view = DataView.create(trackData);
        view.setColumns(columns);
        DygraphsOptions options = createOptions(false);
        if (dygraphs == null) {
            dygraphs = new Dygraphs(trackData,createOptions(false));
            initDygraphElement();
            panel.add(dygraphs);
        }
        else {
            options.file = view;
            dygraphs.getJSO().updateOptions(options,false);
        }
        setVisible(columns.length() > 1);
    }

    private void initDygraphElement() {
        if (dygraphs == null)
            return;
        dygraphs.setHeight("100%");
        dygraphs.setWidth("100%");
        Element element = dygraphs.getElement();
        element.getStyle().setPosition(Style.Position.ABSOLUTE);
        element.getStyle().setTop(0, Style.Unit.PX);
        element.getStyle().setLeft(0, Style.Unit.PX);
        element.getStyle().setRight(0, Style.Unit.PX);
        element.getStyle().setBottom(0, Style.Unit.PX);
    }

    public void setZoom(int zoomStart,int zoomEnd) {
        this.zoomStart = zoomStart;
        this.zoomEnd = zoomEnd;
        if (dygraphs != null) {
            dygraphs.redraw();
        }
    }



    private DygraphsOptions createOptions(boolean stepPlot) {
        DygraphsOptions options = new DygraphsOptions();
        options.showRoller = true;
        options.axes = new AxesOptions.Builder().y(new AxisOptions.Builder().axisLabelWidth(20).axisLabelFontSize(11).build()).build();
        options.pointSize = 0;
        options.includeZero = true;
        options.axisLabelFontSize = 11;
        options.yAxisLabelWidth = 20;
        options.fillGraph = true;
        if (stepPlot) {
            options.stepPlot = stepPlot;
        }
        options.legend = DygraphsOptions.SHOW_LEGEND.always.name();
        options.underlayCallback = new UnderlayCallback() {
            @Override
            public void onUnderlay(Context2d canvas, Area area, DygraphsJs dygraphjs) {
                canvas.save();
                canvas.setFillStyle("#FFFF00");
                canvas.setStrokeStyle("#000000");
                double left = dygraphjs.toDomXCoord(zoomStart);
                double right = dygraphjs.toDomXCoord(zoomEnd);
                double length = right - left;
                if (length < 1)
                {
                    left = left -0.5;
                    length = 1;
                }
                canvas.fillRect(left, area.getY(), length, area.getH());
                canvas.restore();
            }
        };
        return options;

    }

    public boolean isTrackSelected(String id) {
        return activeColumns.contains(id);
    }

}
