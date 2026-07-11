package me.whereareiam.identica.common.config.defaults;

import me.whereareiam.configura.Config;
import me.whereareiam.configura.Configura;
import me.whereareiam.configura.type.Format;
import me.whereareiam.identica.model.config.Replication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Replication Defaults")
class ReplicationDefaultsTest {
	@DisplayName("Generated replication file no longer includes sentinel-owned cache namespaces")
	@Test
	void generatedReplicationFileNoLongerIncludesSentinelOwnedCacheNamespace(@TempDir Path tempDir) throws Exception {
		Path replicationPath = tempDir.resolve("replication.yml");
		Configura yaml = Config.builder()
				.format(Format.YAML)
				.defaults(ReplicationDefaults.class)
				.build();

		yaml.update(replicationPath, Replication.class);

		String generated = Files.readString(replicationPath);
		assertTrue(generated.contains("attempts:"));
		assertTrue(generated.contains("identica:provider-attempts"));
		assertFalse(generated.contains("sentinels: identica:sentinels"));
	}
}
