package me.whereareiam.identica.model.pipeline.state.scenario.base;

import lombok.Getter;
import lombok.Setter;
import me.whereareiam.identica.model.pipeline.state.AbstractGroupState;
import me.whereareiam.identica.pipeline.ScenarioContext;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
public class PreparationState extends AbstractGroupState {
	private @Nullable ScenarioContext context;
}
