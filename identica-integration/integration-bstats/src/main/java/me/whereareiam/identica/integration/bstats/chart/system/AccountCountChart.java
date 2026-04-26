package me.whereareiam.identica.integration.bstats.chart.system;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.integration.bstats.chart.type.Chart;
import me.whereareiam.identica.database.AccountPersistenceService;
import org.bstats.charts.CustomChart;
import org.bstats.charts.SingleLineChart;
import org.jetbrains.annotations.NotNull;

@Singleton
public final class AccountCountChart implements Chart {
	private final AccountPersistenceService accountPersistenceService;

	@Inject
	public AccountCountChart(@NotNull AccountPersistenceService accountPersistenceService) {
		this.accountPersistenceService = accountPersistenceService;
	}

	@Override
	public @NotNull CustomChart getChart() {
		return new SingleLineChart("account_count", this::value);
	}

	private int value() {
		long count = accountPersistenceService.count();
		if (count <= 0L) return 0;
		return count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;
	}
}
