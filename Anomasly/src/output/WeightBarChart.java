/**
 * 
 */
package output;

import java.util.Map;
import java.util.Map.Entry;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.ApplicationFrame;

import agent.WeightAgent;

/**
 * @author maxime houssin <maxime.houssin@irit.fr>
 *
 */
public class WeightBarChart extends ApplicationFrame {
    
	private static final String ROW_KEY = "Values";
	Map<String, WeightAgent> refToWeightAgents;
	DefaultCategoryDataset dataset;

	public WeightBarChart(String title, Map<String, WeightAgent> weightAgents) {
		super(title);
		this.refToWeightAgents = weightAgents;
		this.dataset  = new DefaultCategoryDataset();
		
		JFreeChart barChart = ChartFactory.createBarChart(
		         title,           
		         "Category",            
		         "Score",            
		         updateDataset(),          
		         PlotOrientation.VERTICAL,           
		         true, true, false);
		         
		ChartPanel chartPanel = new ChartPanel(barChart);        
		chartPanel.setPreferredSize(new java.awt.Dimension(800, 400));        
		setContentPane(chartPanel); 
		barChart.getCategoryPlot().getRangeAxis().setUpperBound(100.0);
	}

	public CategoryDataset updateDataset() {
		dataset.clear();
		for (Entry<String, WeightAgent> e : refToWeightAgents.entrySet()) {
			dataset.addValue(e.getValue().getMyWeight(), ROW_KEY, e.getKey());
		}
		return this.dataset;
	}
}
