package me.whereareiam.identica.engine.pipeline.prepare.group;

import lombok.Getter;
import lombok.Setter;
import me.whereareiam.identica.engine.pipeline.scenario.shared.group.AbstractGroupState;
import me.whereareiam.identica.model.pipeline.prepare.PrepareRequest;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
public class PrepareGroupState extends AbstractGroupState {
	private @Nullable PrepareRequest request;
}
