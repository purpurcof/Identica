package me.whereareiam.identica.engine.pipeline.scenario.type.migration;

import me.whereareiam.identica.engine.pipeline.PipelineExecutor;
import me.whereareiam.identica.model.auth.request.ConnectionRequest;
import me.whereareiam.identica.pipeline.PipelineRegistry;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

@DisplayName("Migration Pipeline")
class MigrationPipelineTest {
	@DisplayName("Fails to build a context when the request has no account UUID")
	@Test
	void buildContextRequiresPreparedAccountUniqueId() {
		TestMigrationPipeline pipeline = new TestMigrationPipeline();

		ScenarioContext context = pipeline.buildFor(ConnectionRequest.builder()
				.identity(new me.whereareiam.identica.identity.actor.ConnectionIdentity("PlayerOne", "127.0.0.1"))
				.build());

		assertNull(context);
	}

	private static final class TestMigrationPipeline extends MigrationPipeline {
		private TestMigrationPipeline() {
			super(
					mock(PipelineRegistry.class),
					me.whereareiam.identica.model.config.Messages::new,
					me.whereareiam.identica.model.config.Engine::new,
					mock(PipelineStateStore.class),
					mock(PipelineExecutor.class)
			);
		}

		private @Nullable ScenarioContext buildFor(@NotNull ConnectionRequest request) {
			return buildContext(request);
		}
	}
}
