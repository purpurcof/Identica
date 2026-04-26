package me.whereareiam.identica.integration.bstats.chart.verification;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.integration.bstats.chart.type.NamedDrilldownPieChart;
import me.whereareiam.identica.model.verification.VerificationMethodDescriptor;
import me.whereareiam.identica.verification.VerificationMethod;
import me.whereareiam.identica.verification.VerificationRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

@Singleton
public final class VerificationMethodsChart extends NamedDrilldownPieChart {
	private final VerificationRegistry verificationRegistry;

	@Inject
	public VerificationMethodsChart(@NotNull VerificationRegistry verificationRegistry) {
		this.verificationRegistry = verificationRegistry;
	}

	@Override
	protected @NotNull String chartId() {
		return "verification_methods";
	}

	@Override
	protected @NotNull Map<String, Integer> officialEntries() {
		return collect(true);
	}

	@Override
	protected @NotNull Map<String, Integer> customEntries() {
		return collect(false);
	}

	private @NotNull Map<String, Integer> collect(boolean builtIn) {
		Map<String, Integer> values = new LinkedHashMap<>();
		for (VerificationMethod method : verificationRegistry.values()) {
			VerificationMethodDescriptor descriptor = method.descriptor();
			if (descriptor.isBuiltIn() != builtIn)
				continue;

			values.put(displayName(descriptor), 1);
		}
		return values;
	}

	private @NotNull String displayName(@NotNull VerificationMethodDescriptor descriptor) {
		String displayName = descriptor.getDisplayName();
		if (!displayName.isBlank())
			return displayName.trim();

		String id = descriptor.getId();
		return id.isBlank() ? "Unknown" : id.trim();
	}
}
