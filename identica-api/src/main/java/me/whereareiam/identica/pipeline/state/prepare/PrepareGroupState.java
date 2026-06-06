package me.whereareiam.identica.pipeline.state.prepare;

import lombok.Getter;
import lombok.Setter;
import me.whereareiam.identica.model.pipeline.prepare.PrepareRequest;
import me.whereareiam.identica.pipeline.state.AbstractGroupState;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
public class PrepareGroupState extends AbstractGroupState {
	private @Nullable PrepareRequest request;
}
