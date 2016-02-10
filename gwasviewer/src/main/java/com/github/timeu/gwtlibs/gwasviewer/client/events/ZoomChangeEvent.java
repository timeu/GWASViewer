package com.github.timeu.gwtlibs.gwasviewer.client.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Created by uemit.seren on 9/23/15.
 */
public class ZoomChangeEvent extends GwtEvent<ZoomChangeEvent.Handler> {

    public interface Handler extends EventHandler {
        void onZoomResize(ZoomChangeEvent event);
    }

    private static Type<Handler> TYPE;

    public final int start;
    public final int stop;

    public ZoomChangeEvent(int start,int stop){
        this.start = start;
        this.stop = stop;
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
        handler.onZoomResize(this);
    }



}