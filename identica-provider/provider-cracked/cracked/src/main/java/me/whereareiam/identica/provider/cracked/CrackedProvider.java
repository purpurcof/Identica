package me.whereareiam.identica.provider.cracked;

import com.google.inject.Inject;
import com.google.inject.Module;
import lombok.AllArgsConstructor;
import me.whereareiam.identica.loader.IdenticaProvider;
import me.whereareiam.identica.auth.step.AuthenticationStep;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@SuppressWarnings("unused")
@AllArgsConstructor(onConstructor_ = @Inject)
public class CrackedProvider extends IdenticaProvider {

	@Override
	public @NotNull List<Module> modules() {
		return List.of(new CrackedModule());
	}

	@Override
	public @NotNull List<AuthenticationStep> getAuthenticationSteps() {
		return List.of();
	}

}
