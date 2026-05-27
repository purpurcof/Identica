package me.whereareiam.identica.engine.pipeline.scenario.base.preparation;

import lombok.Getter;
import lombok.Setter;
import me.whereareiam.identica.engine.pipeline.scenario.base.AbstractGroupState;
import me.whereareiam.identica.pipeline.ScenarioContext;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
public class PreparationState extends AbstractGroupState {
	private @Nullable ScenarioContext context;
}
