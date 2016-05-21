package com.github.timeu.gwtlibs.gwasviewer.client.events;

import com.github.timeu.dygraphsgwt.client.callbacks.Point;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Created by uemit.seren on 9/23/15.
 */
public class PointClickEvent extends GwtEvent<PointClickEvent.Handler> {

    public interface Handler extends EventHandler {
        void onClick(PointClickEvent event);
    }

    private static Type<Handler> TYPE;

    public final Point point;
    public final NativeEvent event;

    public PointClickEvent(NativeEvent event, Point point){
        this.point = point;
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
        handler.onClick(this);
    }



}