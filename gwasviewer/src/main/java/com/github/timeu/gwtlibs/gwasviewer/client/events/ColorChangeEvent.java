package com.github.timeu.gwtlibs.gwasviewer.client.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Created by uemit.seren on 2/2/16.
 */
public class ColorChangeEvent extends GwtEvent<ColorChangeEvent.Handler> {

    public interface Handler extends EventHandler {
        void onColorChange(ColorChangeEvent event);
    }

    private static Type<ColorChangeEvent.Handler> TYPE;

    public ColorChangeEvent() {
    }

    public static Type<ColorChangeEvent.Handler> getType()	{
        if (TYPE == null) {
            TYPE = new Type<>();
        }
        return TYPE;
    }

    @Override
    public Type<ColorChangeEvent.Handler> getAssociatedType() {
        return getType();
    }

    @Override
    protected void dispatch(Handler handler) {
        handler.onColorChange(this);
    }
}
