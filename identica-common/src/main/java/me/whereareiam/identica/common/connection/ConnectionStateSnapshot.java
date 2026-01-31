package me.whereareiam.identica.common.connection;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.model.auth.StepResult;
import me.whereareiam.identica.type.HandshakeMode;
import me.whereareiam.identica.type.RoutingTargetType;
import me.whereareiam.identica.type.step.AuthFlowType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionStateSnapshot {
	private @NotNull UUID connectionUniqueId;
	private @NotNull AuthFlowType flow;
	private int stageIndex;
	private @NotNull String stageId;
	private @NotNull ContextData context;
	private @NotNull PendingStageData pendingStage;
	private @Nullable StepResultData completionResult;
	private @Nullable RoutingTargetData routingTarget;

	@Getter
	@Setter
	@ToString
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ContextData {
		private @Nullable UUID identicaUniqueId;
		private @Nullable String username;
		private @Nullable String ip;
		private @Nullable String intendedServer;
		private @Nullable ProviderData provider;
	}

	@Getter
	@Setter
	@ToString
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ProviderData {
		private @Nullable String providerId;
		private @Nullable String providerSubject;
		private @Nullable String providerUsername;
	}

	@Getter
	@Setter
	@ToString
	@NoArgsConstructor
	@AllArgsConstructor
	public static class PendingStageData {
		private @Nullable String providerId;
		private int stepIndex;
		private @Nullable String stepName;
	}

	@Getter
	@Setter
	@ToString
	@NoArgsConstructor
	@AllArgsConstructor
	public static class StepResultData {
		private @Nullable StepResult.StepStatus status;
		private @Nullable String message;
		private @Nullable HandshakeMode handshakeMode;
	}

	@Getter
	@Setter
	@ToString
	@NoArgsConstructor
	@AllArgsConstructor
	public static class RoutingTargetData {
		private @Nullable RoutingTargetType type;
		private @Nullable String server;
		private @Nullable String providerId;
		private @Nullable String stepName;
	}
}
