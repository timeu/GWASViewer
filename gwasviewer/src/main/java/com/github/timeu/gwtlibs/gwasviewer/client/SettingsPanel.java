package com.github.timeu.gwtlibs.gwasviewer.client;

import com.github.timeu.gwtlibs.gwasviewer.client.events.ColorChangeEvent;
import com.github.timeu.gwtlibs.gwasviewer.client.events.DeleteTrackEvent;
import com.github.timeu.gwtlibs.gwasviewer.client.events.FilterChangeEvent;
import com.github.timeu.gwtlibs.gwasviewer.client.events.SelectTrackEvent;
import com.github.timeu.gwtlibs.gwasviewer.client.events.UploadTrackEvent;
import com.github.timeu.gwtlibs.gwasviewer.client.resources.Resources;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.SimpleCheckBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.SimpleRadioButton;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.HandlerRegistration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by uemit.seren on 9/1/15.
 */
public class SettingsPanel extends Composite {

    private static Binder uiBinder = GWT.create(Binder.class);

    interface Binder extends UiBinder<Widget, SettingsPanel> {	}

    @UiField
    TextBox macTb;

    @UiField
    TextBox mafTb;

    @UiField(provided=true)
    SimpleRadioButton mafRd;

    @UiField(provided=true)
    SimpleRadioButton macRd;

    @UiField(provided=true)
    SimpleRadioButton noMaFilter;
    @UiField
    SpanElement macValue;
    @UiField
    SpanElement noFilterLb;

    @UiField
    SpanElement mafValue;
    @UiField
    SpanElement macLb;
    @UiField
    SpanElement mafLb;
    @UiField
    DivElement macContainer;
    @UiField
    DivElement mafContainer;
    @UiField
    TabPanel settingsTabPanel;
    @UiField
    Anchor defaultFilterBtn;
    @UiField SpanElement displayAllLb;
    @UiField(provided=true)
    SimpleRadioButton showAllRd;
    @UiField(provided=true)
    SimpleRadioButton showSynRd;
    @UiField
    SpanElement displaySynLb;

    @UiField(provided=true)
    SimpleRadioButton showNonSynRd;
    @UiField
    SpanElement displayNonSynLb;
    @UiField
    SimpleCheckBox showInGenes;
    @UiField
    HTMLPanel trackContainer;
    @UiField
    Resources mainRes;

    @UiField
    HTMLPanel uploadWidgetContainer;
    @UiField(provided=true)
    SimpleRadioButton standardColorRd;
    @UiField(provided=true)
    SimpleRadioButton mafColorRd;
    @UiField
    DisclosurePanel uploadCustomContainer;
    List<HandlerRegistration> handlerRegistrations = new ArrayList<>();

    List<HandlerRegistration> uploadHandlerRegistrations = new ArrayList<>();
    DefaultTrackUploadWidget defaultUploadWidget = new DefaultTrackUploadWidget();

    protected double minMAC = 15;

    protected double minMAF = 0.1;

    private Track[] tracks;

    protected Set<String> activeTracks = new HashSet<>();

    private UploadTrackWidget uploadTrackWidget;


    public enum MINOR_FILTER {NO,MAC,MAF;}

    public enum DISPLAY_FILTER {ALL,SYNONYMOUS,NONSYNONYMOUS}
    protected DISPLAY_FILTER displayFilter = DISPLAY_FILTER.ALL;
    private String chr ="";

    protected MINOR_FILTER minorFilter = MINOR_FILTER.NO;
    public SettingsPanel() {
        macRd = new SimpleRadioButton("filterType");
        mafRd = new SimpleRadioButton("filterType");
        noMaFilter = new SimpleRadioButton("filterType");
        showAllRd = new SimpleRadioButton("displayType");
        showSynRd = new SimpleRadioButton("displayType");
        showNonSynRd = new SimpleRadioButton("displayType");
        mafColorRd = new SimpleRadioButton("colorType");
        standardColorRd = new SimpleRadioButton("colorType");
        initWidget(uiBinder.createAndBindUi(this));
        macValue.setInnerText(String.valueOf(minMAC));
        mafValue.setInnerText(String.valueOf(minMAF));
        macTb.getElement().setAttribute("type", "range");
        mafTb.getElement().setAttribute("type","range");
        mafTb.getElement().setAttribute("step", "0.01");
        macTb.setValue(String.valueOf(minMAC));
        mafTb.setValue(String.valueOf(minMAF));
        updateFilter();
        settingsTabPanel.selectTab(0);
        setUploadTrackWidget(defaultUploadWidget);
    }
    @UiHandler("macTb")
    public void onChangeMACTb(ValueChangeEvent<String> event) {
        try {
            minMAC = Double.parseDouble(event.getValue());
            updateFilterValues();
            fireEvent(new FilterChangeEvent());
        }
        catch (Exception e ) {}
    }
    @UiHandler("mafTb")
    public void onChangeMAFTb(ValueChangeEvent<String> event) {
        try {
            minMAF = Double.parseDouble(event.getValue());
            updateFilterValues();
            fireEvent(new FilterChangeEvent());
        }
        catch (Exception e ) {}
    }


    @UiHandler("defaultFilterBtn")
    public void onClickDefaultFilterBtn(ClickEvent e) {
        minMAC = 15;
        minMAF = 0.1;
        macTb.setText(String.valueOf(minMAC));
        mafTb.setText(String.valueOf(minMAF));
        updateFilter();
        fireEvent(new FilterChangeEvent());
    }

    @UiHandler({"macRd","mafRd","noMaFilter"})
    public void onClickFilterType(ClickEvent event) {
        if (mafRd.getValue()) {
            minorFilter = MINOR_FILTER.MAF;
        }
        else if (macRd.getValue()) {
           minorFilter = MINOR_FILTER.MAC;
        }
        else {
            minorFilter = MINOR_FILTER.NO;
        }
        updateFilterControls();
        fireEvent(new FilterChangeEvent());
    }

    @UiHandler({"standardColorRd","mafColorRd"})
    public void onClickColorType(ClickEvent event) {
        fireEvent(new ColorChangeEvent());
    }

    @UiHandler({"showSynRd","showNonSynRd","showAllRd","showInGenes"})
    public void onClickDisplayFilterType(ClickEvent e) {
        updateFilterControls();
        fireEvent(new FilterChangeEvent());
    }


    public void setFilterType(MINOR_FILTER filterType) {
        if (minorFilter != filterType) {
            minorFilter = filterType;
            updateFilterControls();
        }
    }

    public void setMinMAF(double maf) {
        this.minMAF = maf;
        mafTb.setText(String.valueOf(minMAF));
        updateFilter();
    }

    public void setMinMAC(double mac) {
        this.minMAC = mac;
        macTb.setText(String.valueOf(minMAC));
        updateFilter();
    }


    public double getMinMAF() {
        return minMAF;
    }

    public double getMinMAC() {
        return minMAC;
    }

    public boolean showInGenes() {
        return showInGenes.getValue();
    }

    public MINOR_FILTER getFilterType() {
        return minorFilter;
    }

    private void updateFilterControls() {
        macTb.setEnabled(minorFilter == MINOR_FILTER.MAC);
        mafTb.setEnabled(minorFilter == MINOR_FILTER.MAF);
        macLb.getStyle().setColor(minorFilter == MINOR_FILTER.MAC ? "black" : "#ccc");
        mafLb.getStyle().setColor(minorFilter == MINOR_FILTER.MAF ? "black" : "#ccc");
        noFilterLb.getStyle().setColor(minorFilter == MINOR_FILTER.NO ? "black": "#ccc");

        macRd.setValue(minorFilter == MINOR_FILTER.MAC);
        mafRd.setValue(minorFilter == MINOR_FILTER.MAF);
        noMaFilter.setValue(minorFilter == MINOR_FILTER.NO);

        if (showNonSynRd.getValue()) {
            displayFilter = DISPLAY_FILTER.NONSYNONYMOUS;
        }
        else if (showSynRd.getValue()) {
            displayFilter = DISPLAY_FILTER.SYNONYMOUS;
        }
        else {
            displayFilter = DISPLAY_FILTER.ALL;
        }
    }

    private void updateFilterValues() {
        macValue.setInnerText(String.valueOf(minMAC));
        mafValue.setInnerText(String.valueOf(minMAF));
    }

    public void setMafRange(double minValue, double maxValue) {
        mafTb.getElement().setAttribute("min", String.valueOf(minValue));
        mafTb.getElement().setAttribute("max", String.valueOf(maxValue));
    }


    public void setMacRange(int minValue, int maxValue) {
        macTb.getElement().setAttribute("min",String.valueOf(minValue));
        macTb.getElement().setAttribute("max", String.valueOf(maxValue));
    }

    public void setMacEnabled(boolean enable) {
        if (!enable) {
            macContainer.getStyle().setDisplay(Style.Display.NONE);
        } else {
            macContainer.getStyle().setDisplay(Style.Display.BLOCK);
        }
        updateFilter();
    }

    public void setMafEnabled(boolean enable) {
        if (!enable) {
            mafContainer.getStyle().setDisplay(Style.Display.NONE);
        } else {
            mafContainer.getStyle().setDisplay(Style.Display.BLOCK);
        }
        updateFilter();
    }
    public void updateFilterCount(int count) {
        switch (displayFilter) {
            case NONSYNONYMOUS:
                displayNonSynLb.setInnerText("# "+count);
                break;
            case SYNONYMOUS:
                displaySynLb.setInnerText("# "+count);
                break;
            default:
                displayAllLb.setInnerText("# "+count);
        }
    }

    private void updateFilter() {
        updateFilterValues();
        updateFilterControls();
    }

    public void removeTrack(String id) {
        if (id == null || tracks == null)
            return;
        int ixToRemove = -1;
        for (int i=0;i<tracks.length;i++) {
            if (id.equalsIgnoreCase(tracks[i].id)) {
                ixToRemove = i;
                break;
            }
        }
        if (ixToRemove == -1)
            return;
        Track[] t = new Track[tracks.length-1];
        System.arraycopy(tracks, 0, t, 0, ixToRemove);
        if (tracks.length != ixToRemove) {
            System.arraycopy(tracks, ixToRemove + 1, t, ixToRemove, tracks.length - ixToRemove - 1);
        }
        this.tracks = t;
        initTrackDisplay();
    }

    public void setTracks(Track[] tracks) {
        trackContainer.clear();
        activeTracks.clear();
        if (tracks == null || tracks.length == 0) {
            trackContainer.add(new HTML("<h4>No tracks found</h4>"));
        }
        this.tracks = tracks;
        initTrackDisplay();
    }

    private void initTrackDisplay() {
        for (Track track:tracks) {
            Image checkBoxImage = new Image(mainRes.checkmark());
            checkBoxImage.getElement().setId("cb_"+chr+"_"+track.id);
            checkBoxImage.setTitle("Click to add/remove "+ track.name+" statistics to the chart as new series");
            checkBoxImage.setAltText(track.name);
            checkBoxImage.getElement().setAttribute("data-id",track.id);
            Anchor link = new Anchor(track.name);
            link.setTitle("Click to display "+ track.name+" statistics");
            link.setName(track.id);
            FlowPanel panel = new FlowPanel();
            SimplePanel trackPanel  = new SimplePanel();
            panel.add(checkBoxImage);
            panel.add(trackPanel);
            trackPanel.add(link);
            if (track.canDelete) {
                Image deleteImage = new Image(mainRes.delete());
                deleteImage.setTitle("Click to delete "+track.name+" statistics");
                deleteImage.setAltText(track.id);
                deleteImage.addStyleName(mainRes.style().settingsContentDeleteImage());
                panel.add(deleteImage);
                handlerRegistrations.add(deleteImage.addClickHandler(new ClickHandler() {

                    @Override
                    public void onClick(ClickEvent event) {
                        Image source = (Image)event.getSource();
                        String id = source.getAltText();
                        fireEvent(new DeleteTrackEvent(id));
                    }
                }));
            }
            checkBoxImage.addStyleName(mainRes.style().settingsContentItemCheckbox());
            trackPanel.addStyleName(mainRes.style().settingsContentItemText());
            panel.addStyleName(mainRes.style().settingsContentItem());
            trackContainer.add(panel);
            handlerRegistrations.add(link.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    Anchor source = (Anchor)event.getSource();
                    String id = source.getName();
                    fireEvent(new SelectTrackEvent(id,false));
                }
            }));
            handlerRegistrations.add(checkBoxImage.addClickHandler(new ClickHandler() {
                @Override
                public void onClick
(ClickEvent event) {
                    Image source = (Image)event.getSource();
                    String id = source.getElement().getAttribute("data-id");
                    fireEvent(new SelectTrackEvent(id,true));
                }
            }));
        }
    }

    public DISPLAY_FILTER getDisplayFilter() {
        return displayFilter;
    }

    private void setCheckBoxes() {
        for (int i =0;i<trackContainer.getWidgetCount();i++) {
            Widget widget = trackContainer.getWidget(i);
            if (widget instanceof FlowPanel) {
                Widget cb = ((FlowPanel)widget).getWidget(0);
                cb.removeStyleName(mainRes.style().settingsContentItemCheckboxChecked());
            }
        }
        for (String id : activeTracks) {
            Element element = trackContainer.getElementById("cb_"+chr+"_"+id);
            element.toggleClassName(mainRes.style().settingsContentItemCheckboxChecked());
        }
    }

    public void addActivateTrack(String id, boolean isStacked) {
        if (isStacked) {
            if (activeTracks.contains(id)) {
                activeTracks.remove(id);
            }
            else {
                activeTracks.add(id);
            }
        }
        else {
            if (activeTracks.size() == 1 && activeTracks.contains(id)) {
                activeTracks.remove(id);
            }
            else {
                activeTracks.clear();
                activeTracks.add(id);
            }
        }
        setCheckBoxes();
    }

    public void setDefaultTrackUploadUrl(String url) {
        defaultUploadWidget.setUploadUrl(url);
    }

    public void setUploadTrackWidget(UploadTrackWidget widget) {
        for (HandlerRegistration reg : uploadHandlerRegistrations) {
            reg.removeHandler();
        }
        uploadHandlerRegistrations.clear();
        this.uploadTrackWidget = widget;
        uploadCustomContainer.setVisible(widget != null);
        if (widget != null) {
            uploadWidgetContainer.add(widget);
            uploadHandlerRegistrations.add(widget.asWidget().addHandler(new UploadTrackEvent.Handler() {
                @Override
                public void onUploadTack(UploadTrackEvent event) {
                    uploadCustomContainer.setOpen(false);
                    fireEvent(event);
                }
            },UploadTrackEvent.getType()));
        }
    }


    public String getColorType() {
        if (mafColorRd.getValue()) {
            return "maf";
        }
        return null;
    }

    public void setChromosome(String chr) {
        this.chr = chr;
    }
}
