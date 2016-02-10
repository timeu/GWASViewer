package com.github.timeu.gwtlibs.gwasviewer.client.events;

import com.github.timeu.dygraphsgwt.client.callbacks.Point;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Created by uemit.seren on 9/23/15.
 */
public class HighlightPointEvent extends GwtEvent<HighlightPointEvent.Handler> {

    public interface Handler extends EventHandler {

        void onHighlight(HighlightPointEvent event);
    }
    private static Type<Handler> TYPE;

    public final long x;
    public final Point[] points;
    public final int row;
    public final String seriesName;
    public final NativeEvent event;

    public HighlightPointEvent(NativeEvent event, long x, Point[] points, int row, String seriesName) {
        this.row = row;
        this.seriesName = seriesName;
        this.x = x;
        this.points = points;
        this.event = event;
    }

    public static Type<Handler> getType()	{
        if (TYPE == null) {
            TYPE = new Type<>();
        }
        return TYPE;
    }

    @Override
    public Type<Handler> getAssociatedType() {
        return getType();
    }

    @Override
    protected void dispatch(Handler handler) {
        handler.onHighlight(this);
    }



}