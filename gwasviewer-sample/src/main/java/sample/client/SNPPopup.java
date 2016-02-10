package sample.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.HeadingElement;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.InlineHyperlink;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Created by uemit.seren on 9/23/15.
 */
public class SNPPopup extends PopupPanel {

    interface Binder extends UiBinder<Widget, SNPPopup> {
    }

    interface MyStyle extends CssResource {
        String popup();
    }

    private static Binder ourUiBinder = GWT.create(Binder.class);
    protected Integer chromosome;
    protected Integer position;
    @UiField
    HeadingElement snpInfo;

    @UiField
    Hyperlink showGlobalLD;
    @UiField
    Hyperlink showLDTriangle;

    @UiField
    MyStyle style;

    public SNPPopup() {
        setWidget(ourUiBinder.createAndBindUi(this));
        setStylePrimaryName(style.popup());
        setAnimationEnabled(false);
        setAutoHideEnabled(true);
        setAutoHideOnHistoryEventsEnabled(true);

    }

    public void setDataPoint(Integer chromosome, Integer position) {
        this.chromosome = chromosome;
        this.position = position;
        snpInfo.setInnerText(String.valueOf(chromosome) + " : " + String.valueOf(position));
    }

    public HasClickHandlers getGlobalLDHandler() {
        return showGlobalLD;
    }

    public HasClickHandlers getLDTriangleHandler() {
        return showLDTriangle;
    }


}