package at.gmi.nordborglab.widgets.gwasgeneviewer.client;

import java.util.ArrayList;

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
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.visualization.client.DataTable;

public class GWASGeneViewer extends Composite {

	private static ScatterGenomeChartUiBinder uiBinder = GWT
			.create(ScatterGenomeChartUiBinder.class);
	
	interface ScatterGenomeChartUiBinder extends UiBinder<Widget, GWASGeneViewer> {	}

	@UiField Dygraphs scatterChart;
	@UiField HTMLPanel geneViewerContainer;
	@UiField Label chromosome_label;
	@UiField GeneViewer geneViewer;
	
	//Scatterchart settings
	private static int DYGRAPHOFFSET = 31;
	protected double max_value;
	protected String color;
	protected String gene_marker_color;
	protected int pointSize =2;
	protected int chr_length;
	protected int scatterChartHeight=200;
	protected DataTable dataTable;
	protected boolean isScatterChartLoaded = false;
	
	protected SelectHandler selectHandler=null;
	protected ClickGeneHandler clickGeneHandler = null;
	
	//GenomeView settings
	protected Integer minZoomLevelForGenomeView = 1500000;
	protected boolean isGeneViewerLoaded = false;
	
	//General settings
	protected String chromosome;
	protected int width = 1000;
	protected int geneViewerHeight = 200;
	protected DataSource datasource = null;
	
	protected ArrayList<Gene> displayGenes = new ArrayList<Gene>();
	
	

	public GWASGeneViewer() {
		initWidget(uiBinder.createAndBindUi(this));
		initGenomeView();
	}
	
	public GWASGeneViewer(String chromosome,String color,String gene_marker_color,int width,DataSource datasource) {
		this.chromosome = chromosome;
		this.color = color;
		this.width=width;
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
			geneViewer.setLength(chr_length);
			geneViewer.setSize(width - DYGRAPHOFFSET, geneViewerHeight);
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
	
	public void draw(DataTable dataTable,double max_value, int chr_length)
	{
		this.dataTable = dataTable;
		this.max_value = max_value;
		this.chr_length = chr_length;
		geneViewer.setLength(chr_length);
		geneViewer.setChromosome(chromosome);
		geneViewer.setDataSource(datasource);
		geneViewer.setSize(width - DYGRAPHOFFSET, geneViewerHeight);
		this.drawScatterChart();
		if (selectHandler != null)
			scatterChart.addSelectHandler(selectHandler);
		scatterChart.addZoomHandler(new ZoomHandler() {
			
			@Override
			public void onZoom(ZoomEvent event) {
				
				if (event.maxX - event.minX<= minZoomLevelForGenomeView)
				{
					geneViewer.updateZoom(event.minX, event.maxX);
					toggleGenomeViewVisible(true);
				}
				else
					toggleGenomeViewVisible(false);
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
					if (gene.getChromosome().equals(chromosome))
					{
						double left = event.dygraph.toDomXCoord(gene.getStart());
						double right = event.dygraph.toDomXCoord(gene.getEnd());
						double length = right - left;
						if (length < 1)
						{
							left = left -0.5;
							length = 1;
						}
						event.canvas.setFillStyle(gene_marker_color);
						event.canvas.fillRect(left, event.area.getY(), length, event.area.getH());
					}
				}
			}
		},options);
		scatterChart.setID(chromosome);
		scatterChart.draw(dataTable,setOptions(options));
		isScatterChartLoaded = true;
	}
	
	protected Options setOptions(Dygraphs.Options options){
		options.setStrokeWidth(0.000000001);
		options.setDrawPoints(true);
		options.setPointSize(pointSize);
		options.setIncludeZero(true);
		options.setWidth(width);
		options.setHeight(scatterChartHeight);
		options.setAxisLabelFontSize(12);
		options.setValueRange(0,(int)max_value + 2);
		options.setxAxisLabelWidth(100);
		options.setyAxisLabelWidth(20);
		options.setColors(new String[] {color});
		options.setMinimumDistanceForHighlight(10);
		options.setIncludeYPositionForHightlight(true);
		return options;
	}
	
	
	public void toggleGenomeViewVisible(boolean visible) {
			geneViewerContainer.setVisible(visible);
	}
	
	public void setMinZoomLevelForGenomeView(Integer minZoomLevelForGenomeView) {
		this.minZoomLevelForGenomeView = minZoomLevelForGenomeView;
	}
	
	public void setChromosome(String chromosome) {
		this.chromosome = chromosome;
	}
	
	public void setColor(String color) {
		this.color = color;
	}
	
	public void setPointSize(int pointSize) {
		this.pointSize = pointSize;
		
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
}
