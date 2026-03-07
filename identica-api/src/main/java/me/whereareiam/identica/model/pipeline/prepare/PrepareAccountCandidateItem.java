package me.whereareiam.identica.model.pipeline.prepare;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.whereareiam.identica.model.identity.Account;
import me.whereareiam.identica.model.identity.provider.AccountProviderLink;
import me.whereareiam.identica.model.identity.provider.AccountProviderProfile;
import me.whereareiam.identica.pipeline.state.PipelineStateItem;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrepareAccountCandidateItem implements PipelineStateItem {
	private @Nullable UUID uniqueId;
	private @Nullable String effectiveUsername;

	private @Nullable Account account;
	private @Nullable AccountProviderLink link;
	private @Nullable AccountProviderProfile profile;

	private boolean created;
}
