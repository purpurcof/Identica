package me.whereareiam.identica.integration.bstats.chart.type;

import org.bstats.charts.CustomChart;
import org.jetbrains.annotations.NotNull;

public interface Chart {
	@NotNull CustomChart getChart();
}
