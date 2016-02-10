package com.github.timeu.gwtlibs.gwasviewer.client.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Created by uemit.seren on 12/4/15.
 */
public class UploadTrackEvent extends GwtEvent<UploadTrackEvent.Handler> {

    public interface Handler extends EventHandler {

        void onUploadTack(UploadTrackEvent event);
    }
    private static Type<UploadTrackEvent.Handler> TYPE;

    private final String result;

    public UploadTrackEvent(String result) {
        this.result = result;
    }

    public static Type<UploadTrackEvent.Handler> getType()	{
        if (TYPE == null) {
            TYPE = new Type<>();
        }
        return TYPE;
    }

    @Override
    public Type<UploadTrackEvent.Handler> getAssociatedType() {
        return getType();
    }

    @Override
    protected void dispatch(Handler handler) {
        handler.onUploadTack(this);
    }

    public String getResult() {
        return result;
    }
}
