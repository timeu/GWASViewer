package com.github.timeu.gwtlibs.gwasviewer.client.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Created by uemit.seren on 12/3/15.
 */
public class DeleteTrackEvent extends GwtEvent<DeleteTrackEvent.Handler> {

    public interface Handler extends EventHandler {
        void onDeleteTrack(DeleteTrackEvent event);
    }

    private static Type<DeleteTrackEvent.Handler> TYPE;
    private final String id;

    public DeleteTrackEvent(String id) {
        this.id = id;
    }

    public static Type<DeleteTrackEvent.Handler> getType()	{
        if (TYPE == null) {
            TYPE = new Type<>();
        }
        return TYPE;
    }

    @Override
    public Type<DeleteTrackEvent.Handler> getAssociatedType() {
        return getType();
    }

    @Override
    protected void dispatch(Handler handler) {
        handler.onDeleteTrack(this);
    }

    public String getId() {
        return id;
    }
}
