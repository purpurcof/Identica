package me.whereareiam.identica.model.pipeline.state;

import lombok.Getter;
import lombok.Setter;
import me.whereareiam.identica.model.pipeline.PipelineResult;
import org.jetbrains.annotations.Nullable;

@Getter
public abstract class AbstractGroupState {
	private @Setter @Nullable PipelineResult result;
}
