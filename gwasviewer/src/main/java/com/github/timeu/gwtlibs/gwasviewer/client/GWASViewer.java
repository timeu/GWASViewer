package com.github.timeu.gwtlibs.gwasviewer.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.timeu.dygraphsgwt.client.callbacks.PointClickCallback;
import com.github.timeu.dygraphsgwt.client.extras.DrawingShapes;
import com.github.timeu.dygraphsgwt.client.Dygraphs;
import com.github.timeu.dygraphsgwt.client.DygraphsJs;
import com.github.timeu.dygraphsgwt.client.DygraphsOptions;
import com.github.timeu.dygraphsgwt.client.ScriptInjector;
import com.github.timeu.dygraphsgwt.client.callbacks.Area;
import com.github.timeu.dygraphsgwt.client.callbacks.DrawPointCallback;
import com.github.timeu.dygraphsgwt.client.callbacks.HighlightCallback;
import com.github.timeu.dygraphsgwt.client.callbacks.Point;
import com.github.timeu.dygraphsgwt.client.callbacks.UnHighlightCallback;
import com.github.timeu.dygraphsgwt.client.callbacks.UnderlayCallback;
import com.github.timeu.dygraphsgwt.client.callbacks.ZoomCallback;
import com.github.timeu.dygraphsgwt.client.options.AxesOptions;
import com.github.timeu.dygraphsgwt.client.options.AxisOptions;
import com.github.timeu.dygraphsgwt.client.options.HighlightSeriesOptions;
import com.github.timeu.dygraphsgwt.client.options.OptFunction;
import com.github.timeu.dygraphsgwt.client.options.ValueFormatter;
import com.github.timeu.gwtlibs.geneviewer.client.GeneViewer;
import com.github.timeu.gwtlibs.geneviewer.client.event.ClickGeneEvent;
import com.github.timeu.gwtlibs.geneviewer.client.event.FetchGeneEvent;
import com.github.timeu.gwtlibs.geneviewer.client.event.HighlightGeneEvent;
import com.github.timeu.gwtlibs.geneviewer.client.event.UnhighlightGeneEvent;
import com.github.timeu.gwtlibs.geneviewer.client.event.ZoomResizeEvent;
import com.github.timeu.gwtlibs.gwasviewer.client.events.PointClickEvent;
import com.github.timeu.gwtlibs.gwasviewer.client.events.ColorChangeEvent;
import com.github.timeu.gwtlibs.gwasviewer.client.events.DeleteTrackEvent;
import com.github.timeu.gwtlibs.gwasviewer.client.events.FetchGeneInfoCallback;
import com.github.timeu.gwtlibs.gwasviewer.client.events.FetchGenesCallback;
import com.github.timeu.gwtlibs.gwasviewer.client.events.FilterChangeEvent;
import com.github.timeu.gwtlibs.gwasviewer.client.events.GeneDataSource;
import com.github.timeu.gwtlibs.gwasviewer.client.events.HighlightPointEvent;
import com.github.timeu.gwtlibs.gwasviewer.client.events.SelectTrackEvent;
import com.github.timeu.gwtlibs.gwasviewer.client.events.UploadTrackEvent;
import com.github.timeu.gwtlibs.gwasviewer.client.events.ZoomChangeEvent;
import com.github.timeu.gwtlibs.gwasviewer.client.resources.Resources;
import com.github.timeu.gwtlibs.ldviewer.client.LDDataPoint;
import com.github.timeu.gwtlibs.ldviewer.client.LDViewer;
import com.github.timeu.gwtlibs.ldviewer.client.event.HighlightLDEvent;
import com.github.timeu.gwtlibs.ldviewer.client.event.MiddleMouseClickEvent;
import com.github.timeu.gwtlibs.ldviewer.client.event.UnhighlightLDEvent;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.core.client.JsArrayMixed;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.web.bindery.event.shared.HandlerRegistration;
import com.googlecode.gwt.charts.client.ColumnRange;
import com.googlecode.gwt.charts.client.DataTable;
import com.googlecode.gwt.charts.client.DataView;
import com.googlecode.gwt.charts.client.RowFilter;
import com.googlecode.gwt.charts.client.Selection;


public class GWASViewer extends Composite implements RequiresResize {

	private static Binder uiBinder = GWT.create(Binder.class);
    interface Binder extends UiBinder<Widget, GWASViewer> {	}

    private static Logger logger = Logger.getLogger(GWASViewer.class.getCanonicalName());

	@UiField SimplePanel scatterChartContainer;


    @UiField SimplePanel geneViewerContainer;

    @UiField Label chromosomeLabel;


    @UiField GeneViewer geneViewer;
    @UiField LDViewer ldviewer;
    @UiField FlowPanel ldviewerContainer;
    @UiField ToggleButton settingsBtn;
    @UiField SettingsPanel settingsPanel;
    @UiField TracksPanel tracksPanel;
    protected Dygraphs scatterChart;
    @UiField(provided=true) Resources mainRes;
    private DataView filteredView;
    private final ScheduledCommand layoutCmd = new ScheduledCommand() {
    	public void execute() {
    		layoutScheduled = false;
		    forceLayout();
		}
    };

    private boolean layoutScheduled = false;
    protected double maxValue;
    protected String[] color;


    protected String geneMarkerColor ="green";

    protected int pointSize =3;

    protected int highlightCircleSize = 4;
    protected int scatterChartHeight=200;
    protected DataTable dataTable;
    protected boolean isScatterChartLoaded = false;
    protected double pvalThreshold = -1 ;
    // use instance because getSelection() does not work in onUnderlay event, because date_graph is not properly initialized
	protected List<Integer> selections = new ArrayList<>();
    //GenomeView settings
	protected Integer minZoomLevelForGenomeView = 1500000;

    protected boolean isGeneViewerLoaded = false;
    //private List<Track> tracks = new ArrayList<>();
    private String geneInfoUrl;


    //LDViewer settings
	protected Integer maximumNumberOfSNPs = 500;

    protected boolean isLDViewerLoaded = false;
    protected HashMap<Integer,LDDataPoint> highlightedLDDataPoints = null;

    protected LDDataPoint highlightedLDDataPoint = null;
    protected double threshold = 0.3;
    protected int maxColor = 255;
    protected boolean isNotPairWise=false;
    //General settings
	protected String chromosome;
    protected int width = 0;
    protected int geneViewerHeight = 213;
    protected GeneDataSource geneDataSource = null;
    protected HashMap<DisplayFeature, DivElement> displayFeatures = new HashMap<>();
    protected DygraphsOptions scatterChartOptions;
    protected Map<String,ColumnRange> key2Range = new HashMap<>();

    //TODO replace with bitmask
    private int annotationColIdx = -1;
    private int colorColumnIdx = -1;
    private int mafColIdx= -1;
    private int macColIdx=-1;

    private String colorType = null;

    public GWASViewer() {
		initWidget();
		initGenomeView();
		initLDViewer();
	}
    public GWASViewer(String chromosome, String[] color, String geneMarkerColor, GeneDataSource geneDataSource) {
		this.chromosome = chromosome;
		this.color = color;
		//this.width=width;
		this.geneDataSource = geneDataSource;
		this.geneMarkerColor = geneMarkerColor;
		initWidget();
		initGenomeView();
		initLDViewer();
	}
	private void initWidget() {
        ScriptInjector.injectScript(true);
        ScriptInjector.injectExtra(ScriptInjector.EXTRAS.SHAPES);
		mainRes = GWT.create(Resources.class);
		mainRes.style().ensureInjected();
		initWidget(uiBinder.createAndBindUi(this));
        settingsPanel.setChromosome(chromosome);
        colorType = settingsPanel.getColorType();
        chromosomeLabel.setText(chromosome);
        settingsBtn.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event) {
                settingsPanel.setVisible(event.getValue());
            }
        });

        settingsPanel.addHandler(new FilterChangeEvent.Handler() {
            @Override
            public void onFilterChanged(FilterChangeEvent event) {
                filterAndDraw();
                fireEvent(event);
            }
        }, FilterChangeEvent.getType());

        settingsPanel.addHandler(new DeleteTrackEvent.Handler() {
            @Override
            public void onDeleteTrack(DeleteTrackEvent event) {
                fireEvent(event);
                if (tracksPanel.isTrackSelected(event.getId())) {
                    tracksPanel.addTrackToDisplay(event.getId(),true);
                }
                settingsPanel.removeTrack(event.getId());
            }
        }, DeleteTrackEvent.getType());

        settingsPanel.addHandler(new SelectTrackEvent.Handler() {

            @Override
            public void onSelectTrack(SelectTrackEvent event) {
                if (tracksPanel.isTrackLoaded(event.getId())) {
                    tracksPanel.addTrackToDisplay(event.getId(),event.isStacked());
                    settingsPanel.addActivateTrack(event.getId(),event.isStacked());
                }
                else {
                    fireEvent(event);
                }
            }
        }, SelectTrackEvent.getType());
        settingsPanel.addHandler(new ColorChangeEvent.Handler() {

            @Override
            public void onColorChange(ColorChangeEvent event) {
                colorType = settingsPanel.getColorType();
                refresh();
            }
        },ColorChangeEvent.getType());

        settingsPanel.addHandler(new UploadTrackEvent.Handler() {
            @Override
            public void onUploadTack(UploadTrackEvent event) {
                fireEvent(event);
            }
        },UploadTrackEvent.getType());

	}

	@Override
	public void setWidth(String width) {
		scatterChartContainer.setWidth(width);
	}

	@Override
	public void setHeight(String height) {
		scatterChartContainer.setHeight(height);
	}

	@Override
	public void setSize(String width,String height ) {
		scatterChartContainer.setSize(width, height);
	}

	public String getChromosome() {
		return chromosome;
	}

    public void addDisplayFeature(DisplayFeature feature, boolean blockRedraw) {
        if (!displayFeatures.containsKey(feature)) {
            DivElement elem = DOM.createDiv().cast();
            elem.setInnerHTML(feature.name);
            elem.setId(feature.name+"_label");
            displayFeatures.put(feature, elem);
        }
        if (!blockRedraw) {
            this.refresh();
        }
    }

    public void removeDisplayFeature(DisplayFeature feature,boolean blockRedraw) {
        if (displayFeatures.containsKey(feature)) {
            scatterChart.getElement().removeChild(displayFeatures.get(feature));
            displayFeatures.remove(feature);
            if (!blockRedraw) {
                this.refresh();
            }
        }
    }

    public void removeDisplayFeatures(Collection<DisplayFeature> features) {
        for (DisplayFeature feature: features) {
            removeDisplayFeature(feature,true);
        }
        this.refresh();
    }

	public void clearDisplayFeatures()
	{
		for (DivElement elem: displayFeatures.values()) {
			scatterChart.getElement().removeChild(elem);
		}
		displayFeatures.clear();
        this.refresh();
	}

	private String getTransparentColor() {
		String transColor = color[0];
		String alpha = "0.4";
		if (color[0] == "green")
			transColor = "rgba(0,128,0,"+alpha+")";
		else if (color[0] == "red")
			transColor = "rgba(255,0,0,"+alpha+")";
		else if (color[0] == "blue")
			transColor = "rgba(0,0,255,"+alpha+")";
		else if (color[0] == "purple")
			transColor = "rgba(128,0,128,"+alpha+")";
		else if (color[0] == "cyan")
			transColor = "rgba(0,255,255,"+alpha+")";
		return transColor;
	}

	private void initLDViewer() {
		try
		{
			ldviewer.load(new Runnable() {
				@Override
				public void run() {
					isLDViewerLoaded = true;
					ldviewer.addUnhighlightHandler(new UnhighlightLDEvent.Handler() {

                        @Override
                        public void onUnhighlight(UnhighlightLDEvent event) {
                            DygraphsOptions options = new DygraphsOptions();
                            highlightedLDDataPoint = null;
                            highlightedLDDataPoints = null;
                            options.colors = color;
                            scatterChart.getJSO().updateOptions(options, false);
                        }
                    });
					ldviewer.addHighlightLDHandler(new HighlightLDEvent.Handler() {

                        @Override
                        public void onHighlight(HighlightLDEvent event) {
                            LDDataPoint dataPoint = event.getLDDataPoint();
                            highlightedLDDataPoint = dataPoint;
                            highlightedLDDataPoints = null;
                            Selection selection = null;
                            DygraphsOptions options = new DygraphsOptions();
                            String[] colors = new String[1];
                            colors[0] = getTransparentColor();
                            options.colors = colors;
                            scatterChart.getJSO().updateOptions(options, false);
                        }
                    });

					ldviewer.addMiddleMouseClickHandler(new MiddleMouseClickEvent.Handler() {

                        @Override
                        public void onMiddleMouseClick(MiddleMouseClickEvent event) {
                            ldviewer.resetZoom();
                            setZoom(ldviewer.getZoomStart(), ldviewer.getZoomEnd());
                            fireEvent(new ZoomChangeEvent(ldviewer.getZoomStart(), ldviewer.getZoomEnd()));
                        }
                    });
				}

			});
		}
		catch (Exception e)
		{
            logger.log(Level.SEVERE,"Error loading LDViewer: "+e.getMessage());
		}
	}

	private void initGenomeView()
	{
		try
		{
			scatterChartContainer.setSize("100%", scatterChartHeight + "px");
			//geneViewer.setViewRegion(viewStart, viewEnd);
			geneViewerContainer.setHeight(geneViewerHeight+"px");
			geneViewer.load(new Runnable() {
				@Override
				public void run() {
					isGeneViewerLoaded = true;
                    geneViewer.setChromosome(chromosome);
					geneViewer.addZoomResizeHandler(new ZoomResizeEvent.Handler() {
                        @Override
                        public void onZoomResize(ZoomResizeEvent event) {
                            if (event.stop - event.start > minZoomLevelForGenomeView) {
                                toggleGenomeViewVisible(false);
                            }
                            if (isScatterChartLoaded) {
                                DygraphsOptions options = new DygraphsOptions();
                                options.setDateWindow(event.start, event.stop);
                                scatterChart.getJSO().updateOptions(options, false);
                            }
                            tracksPanel.setZoom(event.start,event.stop);
                            fireEvent(new ZoomChangeEvent(event.start,event.stop));
                        }
                    });
                    geneViewer.addFetchGeneHandler(new FetchGeneEvent.Handler() {
                        @Override
                        public void onFetchGenes(FetchGeneEvent event) {
                            fireEvent(event);
                            if (geneDataSource != null) {
                                geneDataSource.fetchGenes(chromosome, event.getStart(), event.getEnd(), isFetchGeneFeatures(), new FetchGenesCallback() {

                                    @Override
                                    public void onFetchGenes(JsArrayMixed genes) {
                                        geneViewer.setGeneData(genes);
                                    }
                                });
                            }
                        }
                    });
                    geneViewer.addHighlightGeneHandler(new HighlightGeneEvent.Handler() {
                        @Override
                        public void onHightlightGene(HighlightGeneEvent event) {
                            fireEvent(event);
                            if (geneDataSource != null) {
                                geneDataSource.fetchGeneInfo(event.getGene().name, new FetchGeneInfoCallback() {
                                    @Override
                                    public void onFetchGeneInfo(String info) {
                                        geneViewer.setGeneInfo(info);
                                    }
                                });
                            }
                        }
                    });
                    geneViewer.addUnhighlightGeneHandlers(new UnhighlightGeneEvent.Handler() {
                        @Override
                        public void onUnhighlightGene(UnhighlightGeneEvent event) {
                            fireEvent(event);
                        }
                    });
                    geneViewer.addClickGeneHandler(new ClickGeneEvent.Handler() {
                        @Override
                        public void onClickGene(ClickGeneEvent event) {
                            fireEvent(event);
                            if (geneInfoUrl != null) {
                                Window.open(geneInfoUrl.replace("{0}",event.getGene().name),"Gene Info","");
                            }

                        }
                    });
					/*if (minZoomLevelForGenomeView >= (viewEnd- viewStart) && (viewEnd - viewStart) > 0) {
						toggleGenomeViewVisible(true);
						geneViewer.redraw(true);
					} */
				}

			});
		}
		catch (Exception e)
		{
            logger.log(Level.SEVERE,"Error loading GeneViewer: "+e.getMessage());
		}
	}

    private boolean isFetchGeneFeatures() {
        if (geneViewer == null)
            return false;
        return geneViewer.shouldDrawFeatures();
    }

    public void refresh() {
		scatterChart.redraw();
	}

	public void setZoom(int start, int end) {
  	    toggleGenomeViewVisible(!(end - start > minZoomLevelForGenomeView));
		if (isScatterChartLoaded) {
            DygraphsOptions options = new DygraphsOptions();
            options.setDateWindow(start,end);
            scatterChart.getJSO().updateOptions(options,false);
        }
		geneViewer.updateZoom(start, end);
	}

	public void loadLDPlot(int[] snps,
			float[][] r2Values,int startRegion,int endRegion) {
		setZoom(startRegion, endRegion);
		isNotPairWise = false;
		ldviewer.setVisible(true);
		ldviewer.onResize();
		ldviewer.showLDValues(snps, r2Values, startRegion, endRegion);
	}

    public void draw(DataTable dataTable,double maxValue,double pvalThreshold,int chrSize)
	{
        ldviewer.setVisible(false);
        toggleGenomeViewVisible(false);
		this.dataTable = dataTable;
		this.maxValue = maxValue;
		this.pvalThreshold = pvalThreshold;
        geneViewer.setViewRegion(0, chrSize);
        // TODO update filter settings
        mafColIdx = getColumnIndexFromName("maf");
        macColIdx = getColumnIndexFromName("mac");
        annotationColIdx = getColumnIndexFromName("annotation");
        settingsPanel.setMacEnabled(macColIdx >= 0);
        settingsPanel.setMafEnabled(mafColIdx >= 0);
        key2Range.clear();
        if (mafColIdx >=0) {
            ColumnRange range = dataTable.getColumnRange(mafColIdx);
            settingsPanel.setMafRange(range.getMinNumber(), range.getMaxNumber());
            key2Range.put("maf",range);
        }
        if (macColIdx >=0) {
            ColumnRange range = dataTable.getColumnRange(macColIdx);
            settingsPanel.setMacRange((int) range.getMinNumber(), (int) range.getMaxNumber());
            key2Range.put("mac",range);
        }
		this.drawManhattanPlot();
		/*if (selectHandler != null)
			scatterChart.addSelectHandler(selectHandler);*/
	}

	protected void drawManhattanPlot()
	{
		scatterChartOptions = new DygraphsOptions();
        scatterChartOptions = setOptions(scatterChartOptions);
        scatterChartOptions.drawPointCallback = new DrawPointCallback() {
            @Override
            public void onDraw(DygraphsJs dygraphs, String seriesName, Context2d context, double cx, double cy, String color, int pointSize, int idx) {
                onDrawManhattan(dygraphs, seriesName, context, cx, cy, color, pointSize, idx,false);
            }
        };
        scatterChartOptions.drawHighlightPointCallback =new DrawPointCallback() {
            @Override
            public void onDraw(DygraphsJs dygraphs, String seriesName, Context2d context, double cx, double cy, String color, int pointSize, int idx) {
                onDrawManhattan(dygraphs, seriesName, context, cx, cy, color, pointSize, idx,true);
            }
        };
        scatterChartOptions.zoomCallback = new ZoomCallback() {
            @Override
            public void onZoom(long minDate, long maxDate, double[] yRange) {
                onZoomManhattan(minDate, maxDate, yRange);
                fireEvent(new ZoomChangeEvent((int) minDate, (int) maxDate));
            }
        };
        scatterChartOptions.highlightCallback = new HighlightCallback() {
            @Override
            public void onHighlight(NativeEvent event, long x, Point[] points, int row, String seriesName) {
                onHighlightManhattan(event, x, points, row, seriesName);
                fireEvent(new HighlightPointEvent(event,x,points,row,seriesName));
            }
        };
        scatterChartOptions.unhighlightCallback = new UnHighlightCallback() {
            @Override
            public void onUnhighlight(NativeEvent event) {
                onUnhighlightManhattan(event);
            }
        };
        scatterChartOptions.pointClickCallback = new PointClickCallback() {
            @Override
            public void onClick(NativeEvent event, Point point) {
                fireEvent(new PointClickEvent(event,point));
            }
        };
		scatterChartOptions.underlayCallback = new UnderlayCallback() {
            @Override
            public void onUnderlay(Context2d canvas, Area area, DygraphsJs dygraphjs) {
                onUnderlayManhattan(canvas, area, dygraphjs);
            }
        };
        AxisOptions xAxis = new AxisOptions.Builder().valueFormatter(new ValueFormatter() {
              @Override
              public String onValueFormatter(long value, OptFunction opts, String seriesName, DygraphsJs dygraphjs, int row, int col) {
                  String annotation = null;
                  if (row <0)
                      return null;
                  if ( annotationColIdx>= 0) {
                      annotation = dataTable.getValueString(filteredView.getTableRowIndex(row),annotationColIdx);
                  }
                  String formattedValue = String.valueOf(value);
                  if (annotation != null) {
                      formattedValue += " [" + annotation + "]";
                  }
                  if (mafColIdx >=0) {
                      double maf = dataTable.getValueNumber(filteredView.getTableRowIndex(row),mafColIdx);
                      formattedValue+=" | MAF: " +maf;
                  }
                  if (macColIdx >= 0) {
                      int mac = (int) dataTable.getValueNumber(filteredView.getTableRowIndex(row),macColIdx);
                      formattedValue+=" | MAC: " +mac;
                  }
                  return formattedValue;
              }
          }).build();
        scatterChartOptions.axes.x = xAxis;
        scatterChart = new Dygraphs(getFilteredView(),scatterChartOptions);
        scatterChart.setHeight("100%");
        scatterChart.setWidth("100%");
        scatterChartContainer.setWidget(scatterChart);
		isScatterChartLoaded = true;
	}

    public void filterAndDraw() {
        DygraphsOptions options = new DygraphsOptions();
        options.setDataTableFile(getFilteredView());
        scatterChart.getJSO().updateOptions(options, false);
    }


    private DataView getFilteredView() {
        if (dataTable == null)
            return null;
        filteredView = DataView.create(dataTable);
        JsArray<RowFilter> filter = JsArray.createArray().cast();
        RowFilter minorfilterProperty = RowFilter.create();
        RowFilter displayfilterProperty = RowFilter.create();
        SettingsPanel.MINOR_FILTER minorFilter = settingsPanel.getFilterType();
        SettingsPanel.DISPLAY_FILTER displayFilter = settingsPanel.getDisplayFilter();
        double minMAC = settingsPanel.getMinMAC();
        double minMAF = settingsPanel.getMinMAF();
        if (minorFilter != null ) {
            switch (minorFilter) {
                case MAC:
                    minorfilterProperty.setColumn(getColumnIndexFromName("mac"));
                    minorfilterProperty.setMinValue(minMAC);
                    break;
                case MAF:
                    minorfilterProperty.setColumn(getColumnIndexFromName("maf"));
                    minorfilterProperty.setMinValue(minMAF);
                    break;
                default:
                    minorfilterProperty = null;
                    break;
            }
        }
        if (minorfilterProperty != null) {
            filter.set(0,minorfilterProperty);
        }
        if (displayFilter != null) {
            switch (displayFilter) {
                case SYNONYMOUS:
                    displayfilterProperty.setColumn(getColumnIndexFromName("annotation"));
                    displayfilterProperty.setValue("S");
                    break;
                case NONSYNONYMOUS:
                    displayfilterProperty.setColumn(getColumnIndexFromName("annotation"));
                    displayfilterProperty.setValue("NS");
                    break;
                default:
                    displayfilterProperty = null;
                    break;
            }
        }
        if (displayfilterProperty != null) {
            filter.set(filter.length(),displayfilterProperty);
        }

        if (settingsPanel.showInGenes()) {
            RowFilter inGeneFilter = RowFilter.create();
            inGeneFilter.setColumn(getColumnIndexFromName("inGene"));
            setInGeneFilter(inGeneFilter, true);
            filter.set(filter.length(),inGeneFilter);
        }
        if (filter.length() > 0) {
            filteredView.setRows(filteredView.getFilteredRows(filter).cast());
        }
        JsArrayMixed columns = JsArrayMixed.createArray().cast();
        columns.push(0);
        columns.push(1);
        filteredView.setColumns(columns);
        settingsPanel.updateFilterCount(filteredView.getNumberOfRows());
        return filteredView;
    }

    private int getColumnIndexFromName(String name) {
       for (int i=0;i< dataTable.getNumberOfColumns();i++) {
           if (dataTable.getColumnId(i).equalsIgnoreCase(name)) {
               return i;
           }
       }
        return -1;
    }

	private HashMap<Integer, LDDataPoint> getHighlightedDataPointMap() {
		LDDataPoint[] dataPoints = ldviewer.getHighlightedDataPoints();
		if (dataPoints != null) {
			HashMap<Integer, LDDataPoint> map = new HashMap<Integer, LDDataPoint>();
			for (int i = 0;i<dataPoints.length;i++) {
				LDDataPoint point = dataPoints[i];
				map.put(point.posX,point);
				map.put(point.posY,point);
			}
			return map;
		}
		return null;
	}

	protected void setHighlightedDataPoints(int position,int[] snps,float[] r2Values)  {
		highlightedLDDataPoints = new HashMap<>();
		for (int i = 0; i <r2Values.length ; i++) {
              LDDataPoint dataPoint = new LDDataPoint();
              dataPoint.r2 = r2Values[i];
              dataPoint.posX = snps[i];
              if (dataPoint.r2 > threshold)
            	  highlightedLDDataPoints.put(dataPoint.posX, dataPoint);
        }

	}

	public void showColoredLDValues(int position,int[] snps,float[] r2Values) {
		isNotPairWise  = true;
		setHighlightedDataPoints(position, snps, r2Values);
		ldviewer.setVisible(false);
        scatterChart.redraw();
	}



	public void hideColoredLDValues() {
		highlightedLDDataPoints = null;
		highlightedLDDataPoint = null;
		if (isNotPairWise) {
			isNotPairWise = false;
			refresh();
		}
	}

	protected DygraphsOptions setOptions(DygraphsOptions options){
		double maxValue = this.maxValue;
		if (maxValue < pvalThreshold)
			maxValue = pvalThreshold;
		options.strokeWidth = 0;
		options.drawPoints = true;
		options.pointSize = pointSize;
        options.highlightCircleSize = highlightCircleSize;
		options.includeZero = true;
		options.ylabel = "-log10(p)";
		options.xLabelHeight = 13.0;
        options.yLabelWidth = 13.0;
        options.xlabel = "Position";
        options.axes = new AxesOptions.Builder().y(new AxisOptions.Builder().axisLabelWidth(20).axisLabelFontSize(11).build()).build();
        options.axisLabelFontSize = 11;
        options.setValueRange(0, (int) maxValue + 2);
		//options.setYAxisLabelWidth(20);
		options.colors = color;
		HighlightSeriesOptions highlightSeriesOptions = new HighlightSeriesOptions();
		options.highlightSeriesOpts = highlightSeriesOptions;
        options.highlightSeriesBackgroundAlpha = 1.0;
		//options.setDateWindow(viewStart, viewEnd);
		options.animatedZooms = true;
		return options;
	}

	public void toggleGenomeViewVisible(boolean visible) {
        if (geneViewerContainer.isVisible() == visible)
            return;
        geneViewerContainer.setVisible(visible);
        onResize();
	}

	public void setMinZoomLevelForGenomeView(Integer minZoomLevelForGenomeView) {
		this.minZoomLevelForGenomeView = minZoomLevelForGenomeView;
	}

	public void setChromosome(String chromosome) {
		this.chromosome = chromosome;
	}


	public void setColor(String[] color) {
		this.color = color;
	}



	public void setPointSize(int pointSize) {
		this.pointSize = pointSize;
	}

	public void setViewRegion(int start, int end) {
		/*this.viewStart = start;
		this.viewEnd = end;*/
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public void setScatterChartHeight(int scatterChartHeight) {
		this.scatterChartHeight = scatterChartHeight;
	}

	public void setGeneViewerHeight(int geneViewerHeight) {
		this.geneViewerHeight = geneViewerHeight;
	}

	public void setGeneInfoUrl(String geneInfoUrl) {
		this.geneInfoUrl = geneInfoUrl;
	}


	public static Selection getSelectionFromPos(DataTable data,int pos) {
		Selection selection = null;
		for (int i=0;i<data.getNumberOfRows();i++) {
			if (data.getValueNumber(i, 0) == pos) {
				selection = Selection.create();
                selection.setRow(i);
				break;
			}
		}
		return selection;
	}


	public void setSelections(List<Integer> selections, boolean blockRedraw) {
        if (this.selections == null) {
            throw new RuntimeException("Can't use null for selections");
        }
        this.selections = selections;
        if (!blockRedraw) {
            this.refresh();
        }
    }

	public void clearSelection() {
		setSelections(new ArrayList<>(),false);
	}

	public static Selection getTopSNP(DataTable data) {
		Selection selection = null;
		double top_pValue = -1;
		for (int i=0;i<data.getNumberOfRows();i++) {
			if (!data.isValueNull(i, 1))
			{
				double pValue = data.getValueNumber(i, 1);
				if ( pValue > top_pValue)	{
					selection = Selection.create();
                    selection.setRow(i);
					top_pValue = pValue;
				}
			}
		}
		return selection;
	}

	/*public void setScatterChartVisibilityForSeries(int id,boolean isVisible) {
		scatterChart.setVisibility(id, isVisible);
	} */

	public Selection getTopSNP() {
		if (dataTable == null)
			return null;
		return getTopSNP(dataTable);
	}

	@Override
	public void onResize() {
		scheduledLayout();
	}

	@Override
	public void onAttach() {
		super.onAttach();
        onResize();
	}

    public void forceLayout() {
		if (!isAttached())
			return;
		int width = getParent().getParent().getElement().getClientWidth();
		getElement().getStyle().setWidth(width, Unit.PX);
        geneViewerContainer.setWidth(width-30+"px");
		geneViewer.onResize();
        if (scatterChart != null) {
            scatterChart.onResize();
        }
        ldviewerContainer.setWidth(width - 30+"px");
		ldviewer.onResize();
	}

	private void scheduledLayout() {
	    if (isAttached() && !layoutScheduled) {
	      layoutScheduled = true;
	      Scheduler.get().scheduleDeferred(layoutCmd);
	    }
	}

    public void setDefaultTrackUploadUrl(String url) {
        settingsPanel.setDefaultTrackUploadUrl(url);
    }

    public void setUploadTrackWidget(UploadTrackWidget uploadTrackWidget) {
        settingsPanel.setUploadTrackWidget(uploadTrackWidget);
    }


    private static int getR2Color(LDDataPoint point,double threshold,int maxColor) {
        return point.getR2Color(threshold,maxColor);
    }

    private void onDrawManhattan(DygraphsJs dygraphs, String seriesName, Context2d context, double cx, double cy, String color, int pointSize, int idx, boolean isHighlight) {
        context.setLineWidth(1);

        if (highlightedLDDataPoints != null) {
            int x = (int) filteredView.getValueNumber(idx,0);
            LDDataPoint point = highlightedLDDataPoints.get(x);
            if (point != null) {
                // for the LD triangle always asume that highlighted point has r2 = 1
                if (isHighlight && !isNotPairWise) {
                    color = "rgb(255,0,0)";
                }
                else {
                    int hue = getR2Color(point, threshold, maxColor);
                    color = "rgb(255," + hue + ",0)";
                }
            }
            else
                color = "blue";
        }
        else if (highlightedLDDataPoint != null){
            int x = (int)scatterChart.getJSO().toDataXCoord(cx);
            if (x == highlightedLDDataPoint.posX || x == highlightedLDDataPoint.posY) {
                int hue = getR2Color(highlightedLDDataPoint,threshold,maxColor);
                color = "rgb(255,"+hue+",0)";
                pointSize+=1;
            }
        }
        else if (colorType != null) {
            // Cache to improve performance
            if (colorColumnIdx < 0) {
                colorColumnIdx = getColumnIndexFromName(colorType);
            }
            double columnValue = dataTable.getValueNumber(filteredView.getTableRowIndex(idx),colorColumnIdx);
            color = getColorFromRange(columnValue,key2Range.get(colorType));
        }
        if ( annotationColIdx>= 0) {
            String annotation = dataTable.getValueString(filteredView.getTableRowIndex(idx),annotationColIdx);
            if ("NS".equalsIgnoreCase(annotation)) {
                //scatterChart.getDygraphsJS().drawTRIANGLE(event.dygraph, event.seriesName, event.canvas, event.cx, event.cy, color, event.radius);
                DrawingShapes.drawTRIANGLE(dygraphs, seriesName, context, cx, cy, color, pointSize);
                return;
            }
            else if ("S".equalsIgnoreCase(annotation)) {
                DrawingShapes.drawSQUARE(dygraphs, seriesName, context, cx, cy, color, pointSize);
                return;
            }
        }
        DrawingShapes.drawCIRCLE(dygraphs, seriesName, context, cx, cy, color, pointSize-1);
    }

    private String getColorFromRange(double columnValue, ColumnRange columnRange) {
        double scaledValue = (1-columnRange.getMinNumber())/columnRange.getMaxNumber() * columnValue + columnRange.getMinNumber();
        return getColorForRange(scaledValue,0.6);
    }

    public String getColorForRange(double power,double threshold)
    {
        double H = power * threshold;
        double S = 1.0; // Saturation
        double B = 1.0; // Brightness
        int rgb = HSBtoRGB((float)H, (float)S, (float)B);
        //0xff000000 | (r << 16) | (g << 8) | (b << 0);
        return "rgb("+ (rgb >> 16 & 0xFF)+","+(rgb >> 8 & 0xFF)+","+(rgb >>0 & 0xFF)+")";
    }

    public static int HSBtoRGB(float hue, float saturation, float brightness) {
        int r = 0, g = 0, b = 0;
        if (saturation == 0) {
            r = g = b = (int) (brightness * 255.0f + 0.5f);
        } else {
            float h = (hue - (float)Math.floor(hue)) * 6.0f;
            float f = h - (float)java.lang.Math.floor(h);
            float p = brightness * (1.0f - saturation);
            float q = brightness * (1.0f - saturation * f);
            float t = brightness * (1.0f - (saturation * (1.0f - f)));
            switch ((int) h) {
                case 0:
                    r = (int) (brightness * 255.0f + 0.5f);
                    g = (int) (t * 255.0f + 0.5f);
                    b = (int) (p * 255.0f + 0.5f);
                    break;
                case 1:
                    r = (int) (q * 255.0f + 0.5f);
                    g = (int) (brightness * 255.0f + 0.5f);
                    b = (int) (p * 255.0f + 0.5f);
                    break;
                case 2:
                    r = (int) (p * 255.0f + 0.5f);
                    g = (int) (brightness * 255.0f + 0.5f);
                    b = (int) (t * 255.0f + 0.5f);
                    break;
                case 3:
                    r = (int) (p * 255.0f + 0.5f);
                    g = (int) (q * 255.0f + 0.5f);
                    b = (int) (brightness * 255.0f + 0.5f);
                    break;
                case 4:
                    r = (int) (t * 255.0f + 0.5f);
                    g = (int) (p * 255.0f + 0.5f);
                    b = (int) (brightness * 255.0f + 0.5f);
                    break;
                case 5:
                    r = (int) (brightness * 255.0f + 0.5f);
                    g = (int) (p * 255.0f + 0.5f);
                    b = (int) (q * 255.0f + 0.5f);
                    break;
            }
        }
        return 0xff000000 | (r << 16) | (g << 8) | (b << 0);
    }


    private void onUnderlayManhattan(Context2d ctx, Area area, DygraphsJs dygraphjs) {
        for (Map.Entry<DisplayFeature, DivElement> entry : displayFeatures.entrySet()) {
            DisplayFeature feature = entry.getKey();
            DivElement geneLabel = entry.getValue();
            if (true
                    //&& (gene.getStart() >= viewStart || gene.getEnd() <= viewEnd)
                    ) {
                double left = dygraphjs.toDomXCoord(feature.start);
                double right = dygraphjs.toDomXCoord(feature.end);
                double length = right - left;
                if (length < 1) {
                    left = left - 0.5;
                    length = 1;
                }
                ctx.save();
                ctx.setFillStyle(feature.color);
                ctx.setStrokeStyle("#000000");
                ctx.fillRect(left, area.getY(), length, area.getH());
                ctx.restore();
                double x = Math.round(left) + 3;
                if (x + 60 > scatterChart.getOffsetWidth())
                    x = x - 60;
                int y = 22;
                geneLabel.setAttribute("style", "position: absolute; font-size: 11px; z-index: 10; color: " + feature.color + "; line-height: normal; overflow-x: hidden; overflow-y: hidden; top: " + y + "px; left: " + x + "px; text-align: right;");
                if (DOM.getElementById(geneLabel.getId()) == null)
                    scatterChart.getElement().appendChild(geneLabel);
            }
        }

        for (Integer row: selections) {
            double posX = dygraphjs.toDomXCoord(dataTable.getValueNumber(row, 0));
            double posY = dygraphjs.toDomYCoord(dataTable.getValueNumber(row, 1), 0);
            ctx.save();
            ctx.setFillStyle(geneMarkerColor);
            ctx.setStrokeStyle("#000000");
            ctx.beginPath();
            ctx.arc(posX, posY, 4, 0, Math.PI * 2, true);
            ctx.fillRect(posX - 0.5, posY, 1, dygraphjs.getArea().getH());
            ctx.closePath();
            ctx.fill();
            ctx.restore();
        }

        if (pvalThreshold != -1) {
            double posY = (int) dygraphjs.toDomYCoord(pvalThreshold, 0) - 0.5;
            int width = ctx.getCanvas().getWidth();
            ctx.save();
            installPattern(ctx, new int[]{10, 5});
            ctx.beginPath();
            ctx.setStrokeStyle(geneMarkerColor);
            ctx.moveTo(0, posY);
            ctx.lineTo(width, posY);
            ctx.stroke();
            uninstallPattern(ctx);
            ctx.restore();

        }
    }


    private void onHighlightManhattan(NativeEvent event, long x, Point[] points, int row, String seriesName) {
        geneViewer.setSelectionLine((int) x);
        if (ldviewer.isVisible()) {
            ldviewer.setHighlightPosition((int)x);
            highlightedLDDataPoints = getHighlightedDataPointMap();
            highlightedLDDataPoint = null;
            // FIXME because onHighlightManhatten is called after drawing we have to redraw to keep the highlightedLDDataPoints in sync
            scatterChart.redraw();
            // FIXME because scatterChart.redraw() will unselect the highlighted point, we have to remember the selection and then set it after the redraw
            scatterChart.getJSO().setSelection(row, null,false);
        }
    }


    private void onUnhighlightManhattan(NativeEvent event) {
        geneViewer.hideSelectionLine();
        if (ldviewer.isVisible()) {
            ldviewer.setHighlightPosition(null);
            highlightedLDDataPoints = null;
            highlightedLDDataPoint = null;
            scatterChart.getJSO().clearSelection();
            scatterChart.redraw();
        }
    }


    private void onZoomManhattan(long minDate, long maxDate, double[] yRange) {
        tracksPanel.setZoom((int)minDate,(int)maxDate);
        if (!ldviewer.isRangeValid((int)minDate, (int)maxDate)) {
            ldviewer.setHighlightPosition(null);
            ldviewer.setVisible(false);
            DygraphsOptions options = new DygraphsOptions();
            if (!isNotPairWise) {
                highlightedLDDataPoint = null;
                highlightedLDDataPoints = null;
            }
            options.colors = color;
            scatterChart.getJSO().updateOptions(options,false);
        } else {
            ldviewer.setZoom((int)minDate, (int)maxDate);
        }
        int zoomLength = (int)maxDate - (int)minDate;
        if (zoomLength <= minZoomLevelForGenomeView) {
            toggleGenomeViewVisible(true);
            geneViewer.updateZoom((int)minDate, (int)maxDate);
        } else {
            //Bugfix: if the valueRange is not set when filtering values (based on MAC for example) the range get changed automatically.
            DygraphsOptions options = new DygraphsOptions();
            options.setDateWindow(minDate, maxDate);
            scatterChart.getJSO().updateOptions(options,false);
            toggleGenomeViewVisible(false);
        }
    }


    private static final native void installPattern(Context2d ctx, int[] pattern) /*-{
        ctx.setLineDash(pattern);
    }-*/;


    private static final native void uninstallPattern(Context2d ctx) /*-{
        ctx.setLineDash([]);
    }-*/;

    private static final native void setInGeneFilter(RowFilter filter, boolean val) /*-{
        filter.value=  val;
    }-*/;

    public void setTracks(Track[] tracks) {
        settingsPanel.setTracks(tracks);
    }

    public void setTrackData(String id,boolean isStacked, DataTable data) {
        settingsPanel.addActivateTrack(id,isStacked);
        tracksPanel.addTrackToDisplay(id,data,isStacked);
    }

    public void setMinMAC(double minMac) {
        settingsPanel.setMinMAC(minMac);
    }

    public void setMinMAF(double minMaf) {
        settingsPanel.setMinMAF(minMaf);
    }

    public double getMinMAC() {
        return settingsPanel.getMinMAC();
    }

    public double getMinMAF() {
        return settingsPanel.getMinMAF();
    }

    public SettingsPanel.MINOR_FILTER getMinorFilterType() {
        return settingsPanel.getFilterType();
    }

    public void setMinorFilterType(SettingsPanel.MINOR_FILTER filterType) {
        settingsPanel.setFilterType(filterType);
    }

    public HandlerRegistration addFilterChangeHandler(FilterChangeEvent.Handler handler) {
        return addHandler(handler,FilterChangeEvent.getType());
    }

    public HandlerRegistration addDeleteTrackHandler(DeleteTrackEvent.Handler handler) {
        return addHandler(handler,DeleteTrackEvent.getType());
    }

    public HandlerRegistration addSelectTrackHandler(SelectTrackEvent.Handler handler) {
        return addHandler(handler,SelectTrackEvent.getType());
    }

    public HandlerRegistration addColorChangeHandler(ColorChangeEvent.Handler handler) {
        return addHandler(handler,ColorChangeEvent.getType());
    }

    public HandlerRegistration addUploadTrackHandler(UploadTrackEvent.Handler handler) {
        return addHandler(handler,UploadTrackEvent.getType());
    }

    public HandlerRegistration addZoomChangeHandler(ZoomChangeEvent.Handler handler) {
        return addHandler(handler,ZoomChangeEvent.getType());
    }

    public HandlerRegistration addFetchGeneHandler(FetchGeneEvent.Handler handler) {
        return addHandler(handler,FetchGeneEvent.getType());
    }
    public HandlerRegistration addHighlightGeneHandler(HighlightGeneEvent.Handler handler) {
        return addHandler(handler,HighlightGeneEvent.getType());
    }
    public HandlerRegistration addUnhighlightGeneHandler(UnhighlightGeneEvent.Handler handler) {
        return addHandler(handler,UnhighlightGeneEvent.getType());
    }
    public HandlerRegistration addClickGeneHandler(ClickGeneEvent.Handler handler) {
        return addHandler(handler,ClickGeneEvent.getType());
    }
    public HandlerRegistration addHighlightPointHandler(HighlightPointEvent.Handler handler) {
        return addHandler(handler,HighlightPointEvent.getType());
    }
    public HandlerRegistration addPointClickHandler(PointClickEvent.Handler handler) {
        return addHandler(handler,PointClickEvent.getType());
    }


}
