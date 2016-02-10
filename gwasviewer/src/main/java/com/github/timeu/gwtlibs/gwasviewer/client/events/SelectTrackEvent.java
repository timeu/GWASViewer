package com.github.timeu.gwtlibs.gwasviewer.client.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Created by uemit.seren on 12/1/15.
 */
public class SelectTrackEvent extends GwtEvent<SelectTrackEvent.Handler> {

    public interface Handler extends EventHandler {
        void onSelectTrack(SelectTrackEvent event);
    }

    private static Type<SelectTrackEvent.Handler> TYPE;
    private final String id;
    private final boolean isStacked;

    public SelectTrackEvent(String id, boolean isStacked) {
        this.id = id;
        this.isStacked = isStacked;
    }

    public static Type<SelectTrackEvent.Handler> getType()	{
        if (TYPE == null) {
            TYPE = new Type<>();
        }
        return TYPE;
    }

    @Override
    public Type<SelectTrackEvent.Handler> getAssociatedType() {
        return getType();
    }

    @Override
    protected void dispatch(Handler handler) {
        handler.onSelectTrack(this);
    }

    public String getId() {
        return id;
    }

    public boolean isStacked() {
        return isStacked;
    }
}
