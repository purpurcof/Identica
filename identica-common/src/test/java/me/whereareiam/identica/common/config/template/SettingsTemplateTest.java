package me.whereareiam.identica.common.config.template;

import me.whereareiam.configura.Config;
import me.whereareiam.configura.type.Format;
import me.whereareiam.identica.common.config.IdenticaModule;
import me.whereareiam.identica.model.config.Settings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Settings Template")
class SettingsTemplateTest {
	@DisplayName("Generated routing scenarios are empty by default")
	@Test
	void generatedRoutingScenariosAreEmptyByDefault() {
		Settings settings = new SettingsTemplate().supply(new Settings());

		assertNotNull(settings.getConnection());
		assertNotNull(settings.getConnection().getRouting());
		assertNotNull(settings.getConnection().getRouting().getScenarios());
		assertTrue(settings.getConnection().getRouting().getScenarios().isEmpty());
	}

	@DisplayName("Generated settings file writes an empty scenarios map")
	@Test
	void generatedSettingsFileWritesEmptyScenariosMap(@TempDir Path tempDir) throws Exception {
		Path settingsPath = tempDir.resolve("settings.yml");
		Config config = Config.builder()
				.format(Format.YAML)
				.module(new IdenticaModule())
				.template(SettingsTemplate.class)
				.build();

		Settings settings = config.update(settingsPath, Settings.class);

		assertTrue(settings.getConnection().getRouting().getScenarios().isEmpty());
		String generated = Files.readString(settingsPath);
		assertTrue(generated.contains("scenarios: {}"));
		assertFalse(generated.contains("authentication:\n        step:"));
		assertFalse(generated.contains("registration:\n        step:"));
		assertFalse(generated.contains("migration:\n        step:"));
	}
}
