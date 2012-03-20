package at.gmi.nordborglab.widgets.gwasgeneviewer.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.danvk.dygraphs.client.DygraphOptions;
import org.danvk.dygraphs.client.Dygraphs;
import org.danvk.dygraphs.client.DygraphsJS;
import org.danvk.dygraphs.client.DygraphOptions.HighlightSeriesOptions;
import org.danvk.dygraphs.client.events.Canvas;
import org.danvk.dygraphs.client.events.HightlightHandler;
import org.danvk.dygraphs.client.events.SelectHandler;
import org.danvk.dygraphs.client.events.UnderlayHandler;
import org.danvk.dygraphs.client.events.UnhighlightHandler;
import org.danvk.dygraphs.client.events.ZoomHandler;
import org.danvk.dygraphs.client.events.UnderlayHandler.UnderlayEvent;

import at.gmi.nordborglab.widgets.geneviewer.client.GeneViewer;
import at.gmi.nordborglab.widgets.geneviewer.client.datasource.DataSource;
import at.gmi.nordborglab.widgets.geneviewer.client.datasource.Gene;
import at.gmi.nordborglab.widgets.geneviewer.client.event.ClickGeneHandler;
import at.gmi.nordborglab.widgets.geneviewer.client.event.ZoomResizeEvent;
import at.gmi.nordborglab.widgets.geneviewer.client.event.ZoomResizeHandler;

import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.FillStrokeStyle;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayInteger;
import com.google.gwt.core.client.JsArrayMixed;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.visualization.client.DataTable;
import com.google.gwt.visualization.client.Selection;

public class GWASGeneViewer extends Composite implements RequiresResize{

	private static ScatterGenomeChartUiBinder uiBinder = GWT
			.create(ScatterGenomeChartUiBinder.class);
	
	interface ScatterGenomeChartUiBinder extends UiBinder<Widget, GWASGeneViewer> {	}

	@UiField Dygraphs scatterChart;
	@UiField HTMLPanel geneViewerContainer;
	@UiField Label chromosome_label;
	@UiField GeneViewer geneViewer;
	
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
	protected int scatterChartHeight=200;
	protected DataTable dataTable;
	protected boolean isScatterChartLoaded = false;
	protected int snpPosX = -1;
	protected double bonferroniThreshold = -1 ;
	
	// use instance because getSelection() does not work in onUnderlay event, because date_graph is not properly initialized
	protected JsArray<Selection> selections =JsArray.createArray().cast(); 
	
	protected SelectHandler selectHandler=null;
	protected ClickGeneHandler clickGeneHandler = null;
	
	//GenomeView settings
	protected Integer minZoomLevelForGenomeView = 1500000;
	protected boolean isGeneViewerLoaded = false;
	
	//General settings
	protected String chromosome;
	protected int width = 0;
	protected int geneViewerHeight = 326;
	protected DataSource datasource = null;
	protected int viewStart = 0;
	protected int viewEnd = 0;
	protected HashMap<Gene, DivElement> displayGenes = new HashMap<Gene, DivElement>();
	
	
	

	public GWASGeneViewer() {
		initWidget(uiBinder.createAndBindUi(this));
		initGenomeView();
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
	
	public GWASGeneViewer(String chromosome,String[] color,String gene_marker_color,DataSource datasource) {
		
		this.chromosome = chromosome;
		this.color = color;
		//this.width=width;
		this.datasource = datasource;
		this.gene_marker_color = gene_marker_color;
		initWidget(uiBinder.createAndBindUi(this));
		initGenomeView();
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
	
	public void draw(DataTable dataTable,double max_value, int start,int end) {
		draw(dataTable,max_value,start,end,-1);
	}
	
	public void draw(DataTable dataTable,double max_value, int start,int end,double bonferroniThreshold)
	{
		this.dataTable = dataTable;
		this.max_value = max_value;
		this.viewStart = start;
		this.viewEnd = end;
		this.bonferroniThreshold = bonferroniThreshold;
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
				int zoomLength = event.maxX - event.minX;
				if (zoomLength > viewEnd - viewStart)
				{
					scatterChart.setValueRangeX(viewStart, viewEnd);
					geneViewer.updateZoom(event.minX, event.maxX);
				}
				else
				{
					
					//Bugfix for http://code.google.com/p/dygraphs/issues/detail?id=280&thanks=280&ts=1328714824
					if (event.minX < 0)
						scatterChart.setValueRangeX(0, event.maxX);
					else if (event.maxX > viewEnd)  
						scatterChart.setValueRangeX(event.minX, viewEnd);
					
					if (event.maxX - event.minX<= minZoomLevelForGenomeView)
					{
						toggleGenomeViewVisible(true);
						geneViewer.updateZoom(event.minX, event.maxX);
					}
					else	
						toggleGenomeViewVisible(false);
				}
			}
		});
		
		scatterChart.addHighlightHandler(new HightlightHandler() {
			
			@Override
			public void onHighlight(HighlightEvent event) {
				geneViewer.setSelectionLine(event.xVal);
			}
		});
		
		scatterChart.addUnhighlightHandler(new UnhighlightHandler() {
			
			@Override
			public void onUnhighlight(UnhighlightEvent event) {
				geneViewer.hideSelectionLine();
			}
		});
	}
	
	protected void drawScatterChart()
	{
		chromosome_label.setText(chromosome);
		DygraphOptions options = DygraphOptions.create();
		options = setOptions(options);
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
				
				if (bonferroniThreshold != -1) {
					double posY = (int)event.dygraph.toDomYCoord(bonferroniThreshold, 0)-0.5;
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
				
		},options);
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
		scatterChart.draw(dataTable,setOptions(options));
		//if (selections.length() > 0)
		scatterChart.setSelections(selections);
		
		//scatterChart.setSelections()
		isScatterChartLoaded = true;
	}
	
	
	protected DygraphOptions setOptions(DygraphOptions options){
		double maxValue = max_value;
		if (maxValue < bonferroniThreshold)
			maxValue = bonferroniThreshold;
		options.setStrokeWidth(0.000000001);
		options.setDrawPoints(true);
		options.setPointSize(pointSize);
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
}
