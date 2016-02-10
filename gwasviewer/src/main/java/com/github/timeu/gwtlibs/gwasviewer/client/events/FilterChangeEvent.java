package com.github.timeu.gwtlibs.gwasviewer.client.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Created by uemit.seren on 9/1/15.
 */
public class FilterChangeEvent extends GwtEvent<FilterChangeEvent.Handler> {

    public interface Handler extends EventHandler {
        void onFilterChanged(FilterChangeEvent event);
    }

    private static Type<FilterChangeEvent.Handler> TYPE;

    public FilterChangeEvent() {
    }

    public static Type<FilterChangeEvent.Handler> getType()	{
        if (TYPE == null) {
            TYPE = new Type<>();
        }
        return TYPE;
    }

    @Override
    public Type<FilterChangeEvent.Handler> getAssociatedType() {
        return getType();
    }

    @Override
    protected void dispatch(Handler handler) {
        handler.onFilterChanged(this);
    }
}
