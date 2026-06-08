package me.whereareiam.identica.pipeline.state.scenario.shared;

import lombok.Getter;
import lombok.Setter;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.pipeline.state.AbstractGroupState;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
public class PreparationState extends AbstractGroupState {
	private @Nullable ScenarioContext context;
}
