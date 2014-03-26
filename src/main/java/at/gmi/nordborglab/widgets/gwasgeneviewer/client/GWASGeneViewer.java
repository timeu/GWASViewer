package at.gmi.nordborglab.widgets.gwasgeneviewer.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.ajaxloader.client.Properties;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.visualization.client.DataView;
import com.google.gwt.visualization.client.Range;
import org.danvk.dygraphs.client.DygraphOptions;
import org.danvk.dygraphs.client.DygraphOptions.HighlightSeriesOptions;
import org.danvk.dygraphs.client.Dygraphs;
import org.danvk.dygraphs.client.DygraphsJS;
import org.danvk.dygraphs.client.events.Canvas;
import org.danvk.dygraphs.client.events.DrawHighlightPointHandler;
import org.danvk.dygraphs.client.events.DrawPointHandler;
import org.danvk.dygraphs.client.events.HightlightHandler;
import org.danvk.dygraphs.client.events.SelectHandler;
import org.danvk.dygraphs.client.events.UnderlayHandler;
import org.danvk.dygraphs.client.events.UnhighlightHandler;
import org.danvk.dygraphs.client.events.ZoomHandler;

import at.gmi.nordborglab.widgets.geneviewer.client.GeneViewer;
import at.gmi.nordborglab.widgets.geneviewer.client.datasource.DataSource;
import at.gmi.nordborglab.widgets.geneviewer.client.datasource.Gene;
import at.gmi.nordborglab.widgets.geneviewer.client.event.ClickGeneHandler;
import at.gmi.nordborglab.widgets.geneviewer.client.event.ZoomResizeEvent;
import at.gmi.nordborglab.widgets.geneviewer.client.event.ZoomResizeHandler;
import at.gmi.nordborglab.widgets.gwasgeneviewer.client.resources.MyResources;
import at.gmi.nordborglab.widgets.ldviewer.client.LDViewer;
import at.gmi.nordborglab.widgets.ldviewer.client.datasource.LDDataSource;
import at.gmi.nordborglab.widgets.ldviewer.client.datasource.impl.LDDataPoint;
import at.gmi.nordborglab.widgets.ldviewer.client.event.HighlightLDEvent;
import at.gmi.nordborglab.widgets.ldviewer.client.event.HighlightLDHandler;
import at.gmi.nordborglab.widgets.ldviewer.client.event.MiddleMouseClickEvent;
import at.gmi.nordborglab.widgets.ldviewer.client.event.MiddleMouseClickHandler;
import at.gmi.nordborglab.widgets.ldviewer.client.event.UnhighlightLDEvent;
import at.gmi.nordborglab.widgets.ldviewer.client.event.UnhighlightLDHandler;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayInteger;
import com.google.gwt.core.client.JsArrayNumber;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.DOM;
import com.google.gwt.visualization.client.DataTable;
import com.google.gwt.visualization.client.Selection;

public class GWASGeneViewer extends Composite implements RequiresResize{

	private static ScatterGenomeChartUiBinder uiBinder = GWT
			.create(ScatterGenomeChartUiBinder.class);
    private DataView filteredView;

    interface ScatterGenomeChartUiBinder extends UiBinder<Widget, GWASGeneViewer> {	}

    public interface FilterChangeHandler {
        void onChange();
    }

	@UiField Dygraphs scatterChart;
	@UiField HTMLPanel geneViewerContainer;
	@UiField Label chromosome_label;
	@UiField GeneViewer geneViewer;
	@UiField LDViewer ldviewer;
	@UiField ToggleButton settings_btn;
	@UiField PopupPanel settings_popup;
	@UiField(provided=true) MyResources mainRes;
    @UiField
    TextBox macTb;
    @UiField
    TextBox mafTb;
    @UiField(provided=true)
    SimpleRadioButton mafRd;
    @UiField(provided=true)
    SimpleRadioButton macRd;
    @UiField
    SpanElement macValue;
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
    @UiField
    SpanElement displayAllLb;
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

    private final ScheduledCommand layoutCmd = new ScheduledCommand() {
    	public void execute() {
    		layoutScheduled = false;
		    forceLayout();
		}
    };
	private boolean layoutScheduled = false;
	
	//Scatterchart settings
	private static int DYGRAPHOFFSET = 31;
	protected double max_value;
	protected String[] color;
	
	protected String gene_marker_color;
	protected int pointSize =2;
    protected int highlightCircleSize = 4;
	protected int scatterChartHeight=200;
	protected DataTable dataTable;
	protected boolean isScatterChartLoaded = false;
	protected int snpPosX = -1;
	protected double pvalThreshold = -1 ;
	
	// use instance because getSelection() does not work in onUnderlay event, because date_graph is not properly initialized
	protected JsArray<Selection> selections =JsArray.createArray().cast(); 
	
	
	protected SelectHandler selectHandler=null;
	protected ClickGeneHandler clickGeneHandler = null;
    protected FilterChangeHandler filterChangeHandler = null;
	
	//GenomeView settings
	protected Integer minZoomLevelForGenomeView = 1500000;
	protected boolean isGeneViewerLoaded = false;
	
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
	protected int geneViewerHeight = 326;
	protected DataSource datasource = null;
	protected int viewStart = 0;
	protected int viewEnd = 0;
	protected HashMap<Gene, DivElement> displayGenes = new HashMap<Gene, DivElement>();
    protected double minMAC = 15;
    protected double minMAF = 0.1;
    public static enum MINOR_FILTER {NO,MAC,MAF};
    public static enum DISPLAY_FILTER {ALL,SYNONYMOUS,NONSYNONYMOUS};
    protected DISPLAY_FILTER displayFilter = DISPLAY_FILTER.ALL;
    protected MINOR_FILTER minorFilter = MINOR_FILTER.NO;
    protected DygraphOptions scatterChartOptions;

	public GWASGeneViewer() {
		initWidget();
		initGenomeView();
		initLDViewer();
	}
	
	public GWASGeneViewer(String chromosome,String[] color,String gene_marker_color,DataSource datasource,LDDataSource ldDataSource) {
		this.chromosome = chromosome;
		this.color = color;
		//this.width=width;
		this.datasource = datasource;
		this.gene_marker_color = gene_marker_color;
		initWidget();
		initGenomeView();
		initLDViewer();
	}
	
	private void initWidget() {
		mainRes = GWT.create(MyResources.class);
		mainRes.style().ensureInjected();
        macRd = new SimpleRadioButton("filterType");
        mafRd = new SimpleRadioButton("filterType");
        showAllRd = new SimpleRadioButton("displayType");
        showSynRd = new SimpleRadioButton("displayType");
        showNonSynRd = new SimpleRadioButton("displayType");
		initWidget(uiBinder.createAndBindUi(this));
        macValue.setInnerText(String.valueOf(minMAC));
        mafValue.setInnerText(String.valueOf(minMAF));
        macTb.getElement().setAttribute("type","range");
        mafTb.getElement().setAttribute("type","range");
        mafTb.getElement().setAttribute("step","0.01");
		settings_popup.removeFromParent();
		settings_popup.setAnimationEnabled(true);
		settings_btn.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
			
			@Override
			public void onValueChange(ValueChangeEvent<Boolean> event) {
				if (!event.getValue())
					settings_popup.hide();
				else
					settings_popup.showRelativeTo(settings_btn);
			}
		});
        updateFilterControls();
        settingsTabPanel.selectTab(0);
	}
	
	@Override
	public void setWidth(String width) {
		scatterChart.setWidth(width);
	}
	
	@Override
	public void setHeight(String height) {
		scatterChart.setHeight(height);
	}
	
	@Override
	public void setSize(String width,String height ) {
		scatterChart.setSize(width, height);
	}
	
	
	
	public String getChromosome() {
		return chromosome;
	}

	public void addDisplayGene(Gene gene)
	{
		if (gene.getChromosome().equals(this.chromosome)) {
			if (!displayGenes.containsKey(gene)) {
				DivElement elem = DOM.createDiv().cast();
				elem.setInnerHTML(gene.getName());
				elem.setId(gene.getName()+"_label");
				displayGenes.put(gene,elem);
			}
		}
	}
	
	public void clearDisplayGenes() 
	{
		for (DivElement elem: displayGenes.values()) {
			scatterChart.getElement().removeChild(elem);
		}
		displayGenes.clear();
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
					ldviewer.addUnhighlightHandler(new UnhighlightLDHandler() {
						
						@Override
						public void onUnhighlight(UnhighlightLDEvent event) {
							//clearSelection();
							//scatterChart.setSelections(null);
							//scatterChart.redraw();
							DygraphOptions options = DygraphOptions.create();
							//options.setFile(dataTable);
							highlightedLDDataPoint = null;
							highlightedLDDataPoints = null;
							options.setColors(color);
							scatterChart.getDygraphsJS().updateOptions(options);
						}
					});
					ldviewer.addHighlightLDHandler(new HighlightLDHandler() {
						
						@Override
						public void onHighlight(HighlightLDEvent event) {
							LDDataPoint dataPoint = event.getLDDataPoint();
							//DataView view = DataView.create(dataTable);
							highlightedLDDataPoint = dataPoint;
							highlightedLDDataPoints = null;
							Selection selection = null;
							//int[] rowsToFilter = new int[2];
							/*int index = 0;
							for (int i=0;i<dataTable.getNumberOfRows();i++) {
								if (selections.length() == 2)
									break;
								if (dataTable.getValueInt(i, 0) == dataPoint.getPosX() || dataTable.getValueInt(i, 0) == dataPoint.getPosY()) {
									selection = Selection.createRowSelection(i);
									addSelection(selection);
									//rowsToFilter[index] = i;
									//index ++;
								}
							}*/
							//view.setRows(rowsToFilter);
							DygraphOptions options = DygraphOptions.create();
							//options.setFile(view);
							String[] colors = new String[1];
							colors[0] = getTransparentColor();
							options.setColors(colors);
							scatterChart.getDygraphsJS().updateOptions(options);
						}
					});
					
					ldviewer.addMiddleMouseClickHandler(new MiddleMouseClickHandler() {
						
						@Override
						public void onMiddleMouseClick(MiddleMouseClickEvent event) {
							ldviewer.resetZoom();
							setZoom(ldviewer.getZoomStart(),ldviewer.getZoomEnd());
						}
					});
				}
				
			});
		}
		catch (Exception e)
		{
		}
	}
	
	private void initGenomeView()
	{
		try
		{
			scatterChart.setSize("100%", scatterChartHeight+"px");
			geneViewer.setWidthOffset(DYGRAPHOFFSET);
			geneViewer.setViewRegion(viewStart, viewEnd);
			geneViewer.setWidth("100%");
			geneViewer.setHeight(geneViewerHeight+"px");
			//geneViewer.setSize(width - DYGRAPHOFFSET, geneViewerHeight);
			geneViewer.load(new Runnable() {
				@Override
				public void run() {
					isGeneViewerLoaded = true;
					geneViewer.addZoomResizeHandler(new ZoomResizeHandler() {
						
						@Override
						public void onZoomResize(ZoomResizeEvent event) {
							if (event.stop-event.start > minZoomLevelForGenomeView)
								toggleGenomeViewVisible(false);
							if (isScatterChartLoaded)
								scatterChart.setValueRangeX(event.start,event.stop);
						}
					});
					if (clickGeneHandler != null) 
						geneViewer.addClickGeneHandler(clickGeneHandler);
					
					if (minZoomLevelForGenomeView >= (viewEnd- viewStart) && (viewEnd - viewStart) > 0) {
						toggleGenomeViewVisible(true);
						geneViewer.redraw(true);
					}
				}
				
			});
		}
		catch (Exception e)
		{
		}
	}
	
	public void refresh() {
		scatterChart.redraw();
	}
	
	public void setZoom(int start, int end) {
		if (end-start > minZoomLevelForGenomeView)
			toggleGenomeViewVisible(false);
		else 
			toggleGenomeViewVisible(true);
		if (isScatterChartLoaded)
			scatterChart.setValueRangeX(start, end);
		geneViewer.updateZoom(start, end);
	}
	
	public void loadLDPlot(JsArrayInteger snps,
			JsArray<JsArrayNumber> r2Values,int startRegion,int endRegion) {
		setZoom(startRegion, endRegion);
		isNotPairWise = false;
		ldviewer.setVisible(true);
		ldviewer.onResize();
		ldviewer.showLDValues(snps, r2Values, startRegion, endRegion);
	}
	
	public void draw(DataTable dataTable,double max_value, int start,int end) {
		draw(dataTable,max_value,start,end,-1);
	}
	
	public void draw(DataTable dataTable,double max_value, int start,int end,double pvalThreshold)
	{
		this.dataTable = dataTable;
		this.max_value = max_value;
		this.viewStart = start;
		this.viewEnd = end;
		this.pvalThreshold = pvalThreshold;
		geneViewer.setViewRegion(start,end);
		geneViewer.setChromosome(chromosome);
		geneViewer.setDataSource(datasource);
		//geneViewer.setSize(width - DYGRAPHOFFSET, geneViewerHeight);
		this.drawScatterChart();
		if (minZoomLevelForGenomeView >= (viewEnd- viewStart)) {
			toggleGenomeViewVisible(true);
		}
		if (selectHandler != null)
			scatterChart.addSelectHandler(selectHandler);
		scatterChart.addZoomHandler(new ZoomHandler() {
			
			@Override
			public void onZoom(ZoomEvent event) {
				if (!ldviewer.isDataValid(event.minX, event.maxX)) {
					ldviewer.setHighlightPosition(null);
					ldviewer.setVisible(false);
					DygraphOptions options = DygraphOptions.create();
					if (!isNotPairWise) {
						highlightedLDDataPoint = null;
						highlightedLDDataPoints = null;
					}
					options.setColors(color);
					scatterChart.getDygraphsJS().updateOptions(options);
				}
				else {
					ldviewer.setZoom(event.minX, event.maxX);
				}
				int zoomLength = event.maxX - event.minX;
				if (zoomLength > viewEnd - viewStart)
				{
					scatterChart.setValueRangeX(viewStart, viewEnd);
					geneViewer.updateZoom(event.minX, event.maxX);
				}
				else
				{
					
					//Bugfix for http://code.google.com/p/dygraphs/issues/detail?id=280&thanks=280&ts=1328714824
					/*if (event.minX < 0)
						scatterChart.setValueRangeX(0, event.maxX);
					else if (event.maxX > viewEnd)  
						scatterChart.setValueRangeX(event.minX, viewEnd);*/
					
					if (event.maxX - event.minX<= minZoomLevelForGenomeView)
					{
						toggleGenomeViewVisible(true);
						geneViewer.updateZoom(event.minX, event.maxX);
					}
					else	{
                        //Bugfix: if the valueRange is not set when filtering values (based on MAC for example) the range get changed automatically.
                        scatterChart.setValueRangeX(event.minX,event.maxX);
						toggleGenomeViewVisible(false);
					}
				}
			}
		});
		
		scatterChart.addHighlightHandler(new HightlightHandler() {
			
				@Override
			public void onHighlight(HighlightEvent event) {
				geneViewer.setSelectionLine(event.xVal);
				if (ldviewer.isVisible()) {
					ldviewer.setHighlightPosition(event.xVal);
					highlightedLDDataPoints = getHighlightedDataPointMap();
					highlightedLDDataPoint = null;
					int row = scatterChart.getDygraphsJS().getSelection();
					scatterChart.redraw();
					scatterChart.getDygraphsJS().setSelection(row, null);
				}
			}
		});
		
		scatterChart.addUnhighlightHandler(new UnhighlightHandler() {
			
			@Override
			public void onUnhighlight(UnhighlightEvent event) {
				geneViewer.hideSelectionLine();
				if (ldviewer.isVisible()) {
					ldviewer.setHighlightPosition(null);
					highlightedLDDataPoints = null;
					highlightedLDDataPoint = null;
					scatterChart.getDygraphsJS().clearSelection();
					scatterChart.redraw();
				}
			}
		});
		ldviewer.setVisible(false);
	}
	
	protected void drawScatterChart()
	{
		chromosome_label.setText(chromosome);
		scatterChartOptions = DygraphOptions.create();
        scatterChartOptions = setOptions(scatterChartOptions);
		if(GWT.isScript()) {
			scatterChart.addDrawPointHandler(new DrawPointHandler() {
				
				@Override
				public void onDrawPoint(DrawPointEvent event) {
					event.canvas.setLineWidth(1);
					String color = event.color;
					if (highlightedLDDataPoints != null) {
						int x = (int)scatterChart.getDygraphsJS().toDataXCoord(event.cx);
						LDDataPoint point = highlightedLDDataPoints.get(x);
						if (point != null) {
							int hue =  point.getR2Color(threshold, maxColor);
							color = "rgb(255,"+hue+",0)";
						}
						else
							color = "blue";
					}
					else if (highlightedLDDataPoint != null){
						int x = (int)scatterChart.getDygraphsJS().toDataXCoord(event.cx);
						if (x == highlightedLDDataPoint.getPosX() || x == highlightedLDDataPoint.getPosY()) {
							int hue =  highlightedLDDataPoint.getR2Color(threshold, maxColor);
							color = "rgb(255,"+hue+",0)";
							scatterChart.getDygraphsJS().drawDEFAULT(event.dygraph, event.seriesName, event.canvas, event.cx, event.cy, color, event.radius+2);
							return;
						}
					}
                    else {
                        int idx = event.idx;
                        int annotationColIdx = dataTable.getColumnIndex("annotation");
                        if ( annotationColIdx>= 0) {
                            String annotation = dataTable.getValueString(filteredView.getTableRowIndex(idx),annotationColIdx);
                            if ("NS".equalsIgnoreCase(annotation)) {
                                 //scatterChart.getDygraphsJS().drawTRIANGLE(event.dygraph, event.seriesName, event.canvas, event.cx, event.cy, color, event.radius);
                                 scatterChart.getDygraphsJS().drawTRIANGLE(event.dygraph, event.seriesName, event.canvas, event.cx, event.cy, event.color, 3);
                                 return;
                            }
                            else if ("S".equalsIgnoreCase(annotation)) {
                                 scatterChart.getDygraphsJS().drawSQUARE(event.dygraph, event.seriesName, event.canvas, event.cx, event.cy, color, 3);
                                 return;
                            }
                        }
                    }
				    //scatterChart.getDygraphsJS().drawDEFAULT(event.dygraph, event.seriesName, event.canvas, event.cx, event.cy, color, event.radius);
					scatterChart.getDygraphsJS().drawCIRCLE(event.dygraph, event.seriesName, event.canvas, event.cx, event.cy, event.color, 2);
				}
			},scatterChartOptions);
			scatterChart.addDrawHighlightPointCallback(new DrawHighlightPointHandler() {

                @Override
                public void onDrawHighlightPoint(DrawHighlightPointEvent event) {
                    event.canvas.setLineWidth(1);
                    String color = event.color;
                    if (highlightedLDDataPoints != null) {
                        int x = (int) scatterChart.getDygraphsJS().toDataXCoord(event.cx);
                        LDDataPoint point = highlightedLDDataPoints.get(x);
                        if (point != null) {
                            if (isNotPairWise) {
                                int hue = point.getR2Color(threshold, maxColor);
                                color = "rgb(255," + hue + ",0)";
                            } else
                                color = "rgb(255,0,0)";
                        } else if (isNotPairWise) {
                            color = "blue";
                        }
                    }
                    else {
                        int idx = event.idx;
                        int annotationColIdx = dataTable.getColumnIndex("annotation");
                        if ( annotationColIdx>= 0) {
                            String annotation = dataTable.getValueString(filteredView.getTableRowIndex(idx),annotationColIdx);
                            if ("NS".equalsIgnoreCase(annotation)) {
                                //scatterChart.getDygraphsJS().drawTRIANGLE(event.dygraph, event.seriesName, event.canvas, event.cx, event.cy, color, event.radius);
                                scatterChart.getDygraphsJS().drawTRIANGLE(event.dygraph, event.seriesName, event.canvas, event.cx, event.cy, event.color, 5);
                                return;
                            }
                            else if ("S".equalsIgnoreCase(annotation)) {
                                scatterChart.getDygraphsJS().drawSQUARE(event.dygraph, event.seriesName, event.canvas, event.cx, event.cy, color, 5);
                                return;
                            }
                        }
                    }
                    scatterChart.getDygraphsJS().drawCIRCLE(event.dygraph, event.seriesName, event.canvas, event.cx, event.cy, event.color, 4);
                    //scatterChart.getDygraphsJS().drawDEFAULT(event.dygraph, event.seriesName, event.canvas, event.cx, event.cy, color, event.radius);
                }
            }, scatterChartOptions);
		}
		scatterChart.addUnderlayHandler(new UnderlayHandler() {
			
			@Override
			public void onUnderlay(UnderlayEvent event) {
				
				Canvas ctx = event.canvas;
				DygraphsJS dygraphjs = event.dygraph;
				for (Map.Entry<Gene, DivElement> entry : displayGenes.entrySet()) {
				    Gene gene = entry.getKey();
				    DivElement geneLabel = entry.getValue();
					if (gene.getChromosome().equals(chromosome) && (gene.getStart() >= viewStart || gene.getEnd() <= viewEnd))
					{
						double left = dygraphjs.toDomXCoord(gene.getStart());
						double right = dygraphjs.toDomXCoord(gene.getEnd());
						double length = right - left;
						if (length < 1)
						{
							left = left -0.5;
							length = 1;
						}
						ctx.save();
						ctx.setFillStyle(gene_marker_color);
						ctx.setStrokeStyle("#000000");
						ctx.fillRect(left, event.area.getY(), length, event.area.getH());
						ctx.restore();
						String color=gene_marker_color;
						double x = Math.round(left)+3;
						if (x+60 > scatterChart.getOffsetWidth())
							x = x - 60;
						int y = 2; 
						geneLabel.setAttribute("style", "position: absolute; font-size: 11px; z-index: 10; color: "+color+"; line-height: normal; overflow-x: hidden; overflow-y: hidden; top: "+y+"px; left: "+x+"px; text-align: right; width:60px");
						if (DOM.getElementById(geneLabel.getId()) == null)
							scatterChart.getElement().appendChild(geneLabel);
					}
				}
				
				for (int i =0;i<selections.length();i++) 
				{
					Selection selection = selections.get(i);
					if (selection != null) {
						double posX = dygraphjs.toDomXCoord(dataTable.getValueInt(selection.getRow(), 0));
						double posY = dygraphjs.toDomYCoord(dataTable.getValueDouble(selection.getRow(), 1), 0);
						ctx.save();
						ctx.setFillStyle(gene_marker_color);
						ctx.setStrokeStyle("#000000");
						ctx.beginPath();
						ctx.arc(posX,posY,4,0,Math.PI*2,true);
						ctx.fillRect(posX-0.5, posY, 1, dygraphjs.getArea().getH());
						ctx.closePath();
						ctx.fill();
						ctx.restore();
					}
				}
				
				if (pvalThreshold != -1) {
					double posY = (int)event.dygraph.toDomYCoord(pvalThreshold, 0)-0.5;
					int width = ctx.getCanvas().getWidth();
					ctx.save();
					ctx.beginPath();
					ctx.setStrokeStyle(gene_marker_color);
					ctx.dashedLine(0, posY, width, posY);
  				    ctx.closePath();
                    ctx.stroke();
					ctx.restore();
				}
			}
				
		},scatterChartOptions);
		scatterChart.setID(chromosome);
		if (snpPosX > -1) {
			Selection selection = null;
			for (int i=0;i<dataTable.getNumberOfRows();i++) {
				if (dataTable.getValueInt(i, 0) == snpPosX) {
					selection = Selection.createRowSelection(i);
					break;
				}
			}
			selections.set(0, selection);
		}

        initFilterControls();
        scatterChart.draw(getFilteredView(),scatterChartOptions);
		//if (selections.length() > 0)
		scatterChart.setSelections(selections);
		
		//scatterChart.setSelections()
		isScatterChartLoaded = true;
	}

    public void filterAndDraw() {
        scatterChart.getDygraphsJS().updateData(getFilteredView());
    }

    private DataView getFilteredView() {
        if (dataTable == null)
            return null;
        filteredView = DataView.create(dataTable);
        JsArray<Properties> filter = JsArray.createArray().cast();
        Properties minorfilterProperty = Properties.create();
        Properties displayfilterProperty = Properties.create();
        if (minorFilter != null ) {
            switch (minorFilter) {
                case MAC:
                    minorfilterProperty.set("column", (double)dataTable.getColumnIndex("mac"));
                    minorfilterProperty.set("minValue", minMAC);
                    break;
                case MAF:
                    minorfilterProperty.set("column", (double)dataTable.getColumnIndex("maf"));
                    minorfilterProperty.set("minValue", minMAF);
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
                    displayfilterProperty.set("column", (double)dataTable.getColumnIndex("annotation"));
                    displayfilterProperty.set("value", "S");
                    break;
                case NONSYNONYMOUS:
                    displayfilterProperty.set("column", (double)dataTable.getColumnIndex("annotation"));
                    displayfilterProperty.set("value", "NS");
                    break;
                default:
                    displayfilterProperty = null;
                    break;
            }
        }
        if (displayfilterProperty != null) {
            filter.set(filter.length(),displayfilterProperty);
        }

        if (showInGenes.getValue()) {
            Properties inGeneFilter = Properties.create();
            inGeneFilter.set("column",(double)dataTable.getColumnIndex("inGene"));
            inGeneFilter.set("value",true);
            filter.set(filter.length(),inGeneFilter);
        }
        if (filter.length() > 0) {
            filteredView.setRows(CustomDataView.getFilteredRows(filteredView, filter));
        }
        filteredView.setColumns(new int[]{0, 1});
        updateFilterCountLables();
        return filteredView;
    }

    private void updateFilterCountLables() {
        int count = filteredView.getNumberOfRows();
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
	
	private HashMap<Integer, LDDataPoint> getHighlightedDataPointMap() {
		LDDataPoint[] dataPoints = ldviewer.getHighlightedDataPoints();
		if (dataPoints != null) {
			HashMap<Integer, LDDataPoint> map = new HashMap<Integer, LDDataPoint>();
			for (int i = 0;i<dataPoints.length;i++) {
				LDDataPoint point = dataPoints[i];
				map.put(point.getPosX(),point);
				map.put(point.getPosY(),point);
			}
			return map;
		}
		return null;
	}
	
	protected void setHighlightedDataPoints(int position,JsArrayInteger snps,JsArrayNumber r2Values)  {
		highlightedLDDataPoints = new HashMap<Integer, LDDataPoint>();
		for (int i = 0; i <r2Values.length() ; i++) {
              LDDataPoint dataPoint = LDDataPoint.createObject().cast();
              dataPoint.setR2(r2Values.get(i));
              dataPoint.setPosX(snps.get(i));
              if (dataPoint.getR2() > threshold)
            	  highlightedLDDataPoints.put(dataPoint.getPosX(), dataPoint);
        }
		
	}
	
	public void showColoredLDValues(int position,JsArrayInteger snps,JsArrayNumber r2Values) {
		isNotPairWise  = true;
		setHighlightedDataPoints(position, snps, r2Values);
		ldviewer.setVisible(false);
	}
	
	public void hideColoredLDValues() {
		highlightedLDDataPoints = null;
		highlightedLDDataPoint = null;
		if (isNotPairWise) {
			isNotPairWise = false;
			refresh();
		}
	}
	
	protected DygraphOptions setOptions(DygraphOptions options){
		double maxValue = max_value;
		if (maxValue < pvalThreshold)
			maxValue = pvalThreshold;
		options.setStrokeWidth(0.000000001);
		options.setDrawPoints(true);
		options.setPointSize(pointSize);
        options.setHighlightCircleSize(highlightCircleSize);
		options.setIncludeZero(true);
		options.setYlabel("-log10(p)");
		options.setYLabelWidth(13.0);
		options.setXLabelHeight(13.0);
		options.setXlabel("Position");
		options.setAxisLabelFontSize(11);
		options.setValueRange(0,(int)maxValue + 2);
		options.setYAxisLabelWidth(20);
		options.setColors(color);
		HighlightSeriesOptions highlightSeriesOptions = HighlightSeriesOptions.create();
		options.setHighlightSeriesOpts(highlightSeriesOptions);
		options.setDateWindow(viewStart, viewEnd);
		options.setAnimatedZooms(true);
		return options;
	}
	
	
	public void toggleGenomeViewVisible(boolean visible) {
			//geneViewerContainer.setVisible(visible);
			if (geneViewer.isVisible() == visible)
				return;
			geneViewer.setVisible(visible);
			if (visible) {
				//geneViewer.setSize(width - DYGRAPHOFFSET, geneViewerHeight);
				geneViewer.onResize();
			}
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
		this.viewStart = start;
		this.viewEnd = end;
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
	
	public void addSelectionHandler(SelectHandler handler) {
		if (isScatterChartLoaded) {
				scatterChart.addSelectHandler(handler);
		}
		else
			this.selectHandler = handler;
	}
	
	public void addClickGeneHandler(ClickGeneHandler handler) {
		if (isGeneViewerLoaded) {
			geneViewer.addClickGeneHandler(handler);
		}
		else
			this.clickGeneHandler = handler;
	}
	
	public void setSnpPosX(int snpPosX) {
		this.snpPosX = snpPosX;
	}

	public void setGeneInfoUrl(String geneInfoUrl) {
		geneViewer.setGeneInfoUrl(geneInfoUrl);
	}
	
	public static Selection getSelectionFromPos(DataTable data,int pos) {
		Selection selection = null;
		for (int i=0;i<data.getNumberOfRows();i++) {
			if (data.getValueInt(i, 0) == pos) {
				selection = Selection.createRowSelection(i);
				break;
			}
		}
		return selection;
	}
	
	public void addSelection(Selection selection) {
		if (selection != null) {
			selections.push(selection);
		}
	}
	
	public void clearSelection() {
		selections = JsArray.createArray().cast(); 
	}
	
	public static Selection getTopSNP(DataTable data) {
		Selection selection = null;
		double top_pValue = -1;
		for (int i=0;i<data.getNumberOfRows();i++) {
			if (!data.isValueNull(i, 1))
			{
				double pValue = data.getValueDouble(i, 1);
				if ( pValue > top_pValue)	{
					selection = Selection.createRowSelection(i);
					top_pValue = pValue;
				}
			}
		}
		return selection;
	}
	
	public void setScatterChartVisibilityForSeries(int id,boolean isVisible) {
		scatterChart.setVisibility(id, isVisible);
	}
	
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
		scheduledLayout();
	}
	
	
	public void forceLayout() {
		if (!isAttached())
			return;
		int width = getParent().getParent().getElement().getClientWidth();
		getElement().getStyle().setWidth(width, Unit.PX);
		geneViewer.onResize();
		scatterChart.onResize();
		ldviewer.onResize();
	}
	
	private void scheduledLayout() {
	    if (isAttached() && !layoutScheduled) {
	      layoutScheduled = true;
	      Scheduler.get().scheduleDeferred(layoutCmd);
	    }
	}
	
	public void destroy() {
		scatterChart.destroy();
		geneViewer.destroy();
	}
	
	public void setUploadGenomeStatsFormUrl(String url,String urlParameters) {
		geneViewer.setUploadGenomeStatsFormUrl(url,urlParameters);
	}

    private void updateFilterControls() {
        macTb.setEnabled(minorFilter == MINOR_FILTER.MAC);
        mafTb.setEnabled(minorFilter == MINOR_FILTER.MAF);
        macLb.getStyle().setColor(minorFilter == MINOR_FILTER.MAC ? "black" : "#ccc");
        mafLb.getStyle().setColor(minorFilter == MINOR_FILTER.MAF ? "black" : "#ccc");

        macRd.setValue(minorFilter == MINOR_FILTER.MAC);
        mafRd.setValue(minorFilter == MINOR_FILTER.MAF);

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

    private void initFilterControls() {

        if (dataTable.getColumnIndex("maf") >=0) {
            Range range = dataTable.getColumnRange(3);
            mafTb.getElement().setAttribute("min",String.valueOf(range.getMin()));
            mafTb.getElement().setAttribute("max",String.valueOf(range.getMax()));
            mafContainer.getStyle().setDisplay(Style.Display.BLOCK);
            minorFilter = MINOR_FILTER.MAF;
        }
        else {
            mafContainer.getStyle().setDisplay(Style.Display.NONE);
        }
        if (dataTable.getColumnIndex("mac")>=0) {
            Range range = dataTable.getColumnRange(2);
            macTb.getElement().setAttribute("min",String.valueOf(range.getMin()));
            macTb.getElement().setAttribute("max",String.valueOf(range.getMax()));
            macContainer.getStyle().setDisplay(Style.Display.BLOCK);

            if (minorFilter != MINOR_FILTER.MAF) {
                minorFilter = MINOR_FILTER.MAC;
            }
        }
        else {
            macContainer.getStyle().setDisplay(Style.Display.NONE);
        }
        updateFilterValues();
        updateFilterControls();
    }

    @UiHandler({"macRd","mafRd"})
    public void onClickFilterType(ClickEvent event) {
        minorFilter = mafRd.getValue() ? MINOR_FILTER.MAF : MINOR_FILTER.MAC;
        updateFilterControls();
        filterAndDraw();
        if (filterChangeHandler != null) {
            filterChangeHandler.onChange();
        }
    }

    @UiHandler({"showSynRd","showNonSynRd","showAllRd","showInGenes"})
    public void onClickDisplayFilterType(ClickEvent e) {
        updateFilterControls();
        filterAndDraw();
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
        updateFilterValues();
        updateFilterControls();
    }

    public void setMinMAC(double mac) {
        this.minMAC = mac;
        macTb.setText(String.valueOf(minMAC));
        updateFilterValues();
        updateFilterControls();
    }


    @UiHandler("macTb")
    public void onChangeMACTb(ValueChangeEvent<String> event) {
        try {
            minMAC = Double.parseDouble(event.getValue());
            updateFilterValues();
            filterAndDraw();
            if (filterChangeHandler != null) {
                filterChangeHandler.onChange();
            }
        }
        catch (Exception e ) {}
    }

    @UiHandler("mafTb")
    public void onChangeMAFTb(ValueChangeEvent<String> event) {
        try {
            minMAF = Double.parseDouble(event.getValue());
            updateFilterValues();
            filterAndDraw();
            if (filterChangeHandler != null) {
                filterChangeHandler.onChange();
            }
        }
        catch (Exception e ) {}
    }

    @UiHandler("defaultFilterBtn")
    public void onClickDefaultFilterBtn(ClickEvent e) {
        minMAC = 15;
        minMAF = 0.1;
        macTb.setText(String.valueOf(minMAC));
        mafTb.setText(String.valueOf(minMAF));
        updateFilterValues();
        updateFilterControls();
        filterAndDraw();
    }

    public void setFilterChangeHandler(FilterChangeHandler handler){
       this.filterChangeHandler = handler;
    }

    public double getMinMAF() {
        return minMAF;
    }

    public double getMinMAC() {
        return minMAC;
    }

    public MINOR_FILTER getFilterType() {
        return minorFilter;
    }


}
