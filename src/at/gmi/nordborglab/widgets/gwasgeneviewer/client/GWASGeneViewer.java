package at.gmi.nordborglab.widgets.gwasgeneviewer.client;

import org.danvk.dygraphs.client.Dygraphs;
import org.danvk.dygraphs.client.Dygraphs.Options;
import org.danvk.dygraphs.client.events.HightlightHandler;
import org.danvk.dygraphs.client.events.UnhighlightHandler;
import org.danvk.dygraphs.client.events.ZoomHandler;

import at.gmi.nordborglab.widgets.geneviewer.client.GeneViewer;
import at.gmi.nordborglab.widgets.geneviewer.client.datasource.DataSource;
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
	@UiField HTMLPanel genomeViewContainer;
	@UiField Label chromosome_label;
	@UiField GeneViewer genomeView;
	
	//Scatterchart settings
	private static int DYGRAPHOFFSET = 31;
	protected double max_value;
	protected String color;
	protected int pointSize =2;
	protected int chr_length;
	protected int scatterChartHeight=200;
	protected DataTable dataTable;
	protected boolean isScatterChartLoaded = false;
	
	
	//GenomeView settings
	protected Integer minZoomLevelForGenomeView = 1500000;
	protected boolean isGenomeViewLoaded = false;
	
	//General settings
	protected String chromosome;
	protected int width = 1000;
	protected int genomeViewHeight = 200;
	protected DataSource datasource = null;
	
	
	

	public GWASGeneViewer() {
		initWidget(uiBinder.createAndBindUi(this));
		initGenomeView();
	}
	
	public GWASGeneViewer(String chromosome,String color,int width,DataSource datasource) {
		this.chromosome = chromosome;
		this.color = color;
		this.width=width;
		this.datasource = datasource;
		initWidget(uiBinder.createAndBindUi(this));
		initGenomeView();
	}
	

	
	private void initGenomeView()
	{
		try
		{
			genomeView.setLength(chr_length);
			genomeView.setSize(width - DYGRAPHOFFSET, genomeViewHeight);
			genomeView.load(new Runnable() {
				@Override
				public void run() {
					isGenomeViewLoaded = true;
					genomeView.addZoomResizeHandler(new ZoomResizeHandler() {
						
						@Override
						public void onZoomResize(ZoomResizeEvent event) {
							if (event.stop-event.start > minZoomLevelForGenomeView)
								genomeViewContainer.setVisible(false);
							if (isScatterChartLoaded)
								scatterChart.setValueRangeX(event.start,event.stop);
						}
					});
				}
			});
		}
		catch (Exception e)
		{
		}
	}
	
	public void draw(DataTable dataTable,double max_value, int chr_length)
	{
		this.dataTable = dataTable;
		this.max_value = max_value;
		this.chr_length = chr_length;
		genomeView.setLength(chr_length);
		genomeView.setChromosome(chromosome);
		genomeView.setDataSource(datasource);
		genomeView.setSize(width - DYGRAPHOFFSET, genomeViewHeight);
		this.drawScatterChart();
		scatterChart.addZoomHandler(new ZoomHandler() {
			
			@Override
			public void onZoom(ZoomEvent event) {
				
				if (event.maxX - event.minX<= minZoomLevelForGenomeView)
				{
 					genomeView.updateZoom(event.minX, event.maxX);
					toggleGenomeViewVisible(true);
				}
				else
					toggleGenomeViewVisible(false);
			}
		});
		
		scatterChart.addHighlightHandler(new HightlightHandler() {
			
			@Override
			public void onHighlight(HighlightEvent event) {
				genomeView.setSelectionLine(event.xVal);
			}
		});
		
		scatterChart.addUnhighlightHandler(new UnhighlightHandler() {
			
			@Override
			public void onUnhighlight(UnhighlightEvent event) {
				genomeView.hideSelectionLine();
			}
		});
	}
	
	protected void drawScatterChart()
	{
		chromosome_label.setText(chromosome);
		Dygraphs.Options options = Dygraphs.Options.create();
		options = setOptions(options);
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
			genomeViewContainer.setVisible(visible);
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
	
	public void setGenomeViewHeight(int genomeViewHeight) {
		this.genomeViewHeight = genomeViewHeight;
	}
}
