package at.gmi.nordborglab.widgets.gwasgeneviewer.client;

import java.util.ArrayList;
import java.util.List;

import org.danvk.dygraphs.client.Dygraphs;
import org.danvk.dygraphs.client.Dygraphs.Options;
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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayInteger;
import com.google.gwt.core.client.JsArrayMixed;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.uibinder.client.UiBinder;
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
	protected ArrayList<Gene> displayGenes = new ArrayList<Gene>();
	
	
	

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
		if (gene.getChromosome().equals(this.chromosome))
			displayGenes.add(gene);
	}
	
	public void clearDisplayGenes() 
	{
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
		Dygraphs.Options options = Dygraphs.Options.create();
		options = setOptions(options);
		scatterChart.addUnderlayHandler(new UnderlayHandler() {
			
			@Override
			public void onUnderlay(UnderlayEvent event) {
				for (Gene gene:displayGenes) {
					if (gene.getChromosome().equals(chromosome) && (gene.getStart() >= viewStart || gene.getEnd() <= viewEnd))
					{
						double left = event.dygraph.toDomXCoord(gene.getStart());
						double right = event.dygraph.toDomXCoord(gene.getEnd());
						double length = right - left;
						if (length < 1)
						{
							left = left -0.5;
							length = 1;
						}
						event.canvas.save();
						event.canvas.setFillStyle(gene_marker_color);
						event.canvas.fillRect(left, event.area.getY(), length, event.area.getH());
						event.canvas.restore();
					}
				}
				
				for (int i =0;i<selections.length();i++) 
				{
					Selection selection = selections.get(i);
					if (selection != null) {
						double posX = event.dygraph.toDomXCoord(dataTable.getValueInt(selection.getRow(), 0));
						double posY = event.dygraph.toDomYCoord(dataTable.getValueDouble(selection.getRow(), 1), 0);
						event.canvas.save();
						event.canvas.beginPath();
						event.canvas.setFillStyle(gene_marker_color);
						event.canvas.fillRect(posX-0.5, posY, 1, event.area.getH());
						event.canvas.arc(posX, posY, 3, 0, 2*Math.PI, false);
						event.canvas.fill();
						event.canvas.restore();
					}
				}
				
				if (bonferroniThreshold != -1) {
					double posY = (int)event.dygraph.toDomYCoord(bonferroniThreshold, 0)-0.5;
					int width = event.canvas.getCanvas().getWidth();
					event.canvas.save();
					event.canvas.beginPath();
					event.canvas.setStrokeStyle(gene_marker_color);
					event.canvas.dashedLine(0, posY, width, posY);
  				    event.canvas.closePath();
                    event.canvas.stroke();
					event.canvas.restore();
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
		if (selections.length() > 0)
			scatterChart.setSelections(selections);
		
		//scatterChart.setSelections()
		isScatterChartLoaded = true;
	}
	
	protected Options setOptions(Dygraphs.Options options){
		double maxValue = max_value;
		if (maxValue < bonferroniThreshold)
			maxValue = bonferroniThreshold;
		options.setStrokeWidth(0.000000001);
		options.setDrawPoints(true);
		options.setPointSize(pointSize);
		options.setIncludeZero(true);
		//options.setWidth(width);
		//options.setHeight(scatterChartHeight);
		options.setAxisLabelFontSize(11);
		options.setValueRange(0,(int)maxValue + 2);
		options.setyAxisLabelWidth(20);
		options.setColors(color);
		options.setMinimumDistanceForHighlight(10);
		options.setIncludeYPositionForHightlight(true);
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
			selections.set(selections.length(), selection);
		}
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
