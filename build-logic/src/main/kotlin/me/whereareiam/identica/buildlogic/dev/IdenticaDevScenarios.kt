package me.whereareiam.identica.buildlogic.dev

import org.gradle.api.Project

private val velocityJvmArgs = listOf("-Xms256M", "-Xmx256M")
private val bungeecordJvmArgs = listOf("-Xms256M", "-Xmx256M")
private val paperJvmArgs = listOf("-Xms512M", "-Xmx512M")

internal fun registerIdenticaDevScenarios(
	project: Project,
	scenarios: Any,
	artifacts: IdenticaDevArtifacts
) {
	registerNormalVelocity(project, scenarios, artifacts)
	registerNormalBungeecord(project, scenarios, artifacts)
	registerExtendedVelocity(project, scenarios, artifacts)
	registerExtendedBungeecord(project, scenarios, artifacts)
	registerReplicationVelocity(project, scenarios, artifacts)
	registerReplicationBungeecord(project, scenarios, artifacts)
}

private fun registerNormalVelocity(project: Project, scenarios: Any, artifacts: IdenticaDevArtifacts) {
	scenarios.registerScenario("normal-velocity") { scenario ->
		scenario.addVelocity("proxy") { velocity ->
			configureVelocityProxy(
				project = project,
				velocity = velocity,
				artifacts = artifacts,
				rootOverlayDir = "dev/scenarios/normal/velocity/proxy",
				servers = listOf("lobby" to "127.0.0.1:25566"),
				tryServers = listOf("lobby")
			)
		}

		scenario.addPaper("lobby") { paper ->
			configurePaper(project, paper, 25566)
		}
	}
}

private fun registerNormalBungeecord(project: Project, scenarios: Any, artifacts: IdenticaDevArtifacts) {
	scenarios.registerScenario("normal-bungeecord") { scenario ->
		scenario.addBungeeCord("proxy") { bungeecord ->
			configureBungeeCordProxy(
				project = project,
				bungeecord = bungeecord,
				artifacts = artifacts,
				rootOverlayDir = "dev/scenarios/normal/bungeecord/proxy",
				servers = listOf("lobby" to "127.0.0.1:25566"),
				tryServers = listOf("lobby")
			)
		}

		scenario.addPaper("lobby") { paper ->
			configurePaper(project, paper, 25566)
		}
	}
}

private fun registerExtendedVelocity(project: Project, scenarios: Any, artifacts: IdenticaDevArtifacts) {
	scenarios.registerScenario("extended-velocity") { scenario ->
		scenario.addVelocity("proxy") { velocity ->
			configureVelocityProxy(
				project = project,
				velocity = velocity,
				artifacts = artifacts,
				rootOverlayDir = "dev/scenarios/extended/velocity/proxy",
				servers = extendedProxyServers(),
				tryServers = listOf("lobby")
			)
		}

		addExtendedPaperBackends(project, scenario)
	}
}

private fun registerExtendedBungeecord(project: Project, scenarios: Any, artifacts: IdenticaDevArtifacts) {
	scenarios.registerScenario("extended-bungeecord") { scenario ->
		scenario.addBungeeCord("proxy") { bungeecord ->
			configureBungeeCordProxy(
				project = project,
				bungeecord = bungeecord,
				artifacts = artifacts,
				rootOverlayDir = "dev/scenarios/extended/bungeecord/proxy",
				servers = extendedProxyServers(),
				tryServers = listOf("lobby")
			)
		}

		addExtendedPaperBackends(project, scenario)
	}
}

private fun registerReplicationVelocity(project: Project, scenarios: Any, artifacts: IdenticaDevArtifacts) {
	scenarios.registerScenario("replication-velocity") { scenario ->
		scenario.addVelocity("proxy") { velocity ->
			configureVelocityProxy(
				project = project,
				velocity = velocity,
				artifacts = artifacts,
				rootOverlayDir = "dev/scenarios/replication/velocity/proxy",
				servers = extendedProxyServers(),
				tryServers = listOf("lobby")
			)
		}

		addExtendedPaperBackends(project, scenario)
	}
}

private fun registerReplicationBungeecord(project: Project, scenarios: Any, artifacts: IdenticaDevArtifacts) {
	scenarios.registerScenario("replication-bungeecord") { scenario ->
		scenario.addBungeeCord("proxy") { bungeecord ->
			configureBungeeCordProxy(
				project = project,
				bungeecord = bungeecord,
				artifacts = artifacts,
				rootOverlayDir = "dev/scenarios/replication/bungeecord/proxy",
				servers = extendedProxyServers(),
				tryServers = listOf("lobby")
			)
		}

		addExtendedPaperBackends(project, scenario)
	}
}

private fun configureVelocityProxy(
	project: Project,
	velocity: Any,
	artifacts: IdenticaDevArtifacts,
	rootOverlayDir: String,
	servers: List<Pair<String, String>>,
	tryServers: List<String>
) {
	velocity.setInt("port", 25565)
	velocity.setJvmArgs(velocityJvmArgs)
	velocity.setBoolean("onlineMode", false)
	velocity.setString("forwardingMode", "legacy")
	velocity.setDirectory(project, "rootOverlayDir", rootOverlayDir)
	for ((name, address) in servers) velocity.addServer(name, address)
	velocity.setTryServers(*tryServers.toTypedArray())
	artifacts.installProxyArtifacts("plugins/identica") { into, source ->
		velocity.addInstall(into, source)
	}
}

private fun configureBungeeCordProxy(
	project: Project,
	bungeecord: Any,
	artifacts: IdenticaDevArtifacts,
	rootOverlayDir: String,
	servers: List<Pair<String, String>>,
	tryServers: List<String>
) {
	bungeecord.setString("downloadProvider", "spigot-jenkins")
	bungeecord.setInt("port", 25565)
	bungeecord.setJvmArgs(bungeecordJvmArgs)
	bungeecord.setBoolean("onlineMode", false)
	bungeecord.setBoolean("ipForward", true)
	bungeecord.setDirectory(project, "rootOverlayDir", rootOverlayDir)
	for ((name, address) in servers) bungeecord.addServer(name, address)
	bungeecord.setTryServers(*tryServers.toTypedArray())
	artifacts.installProxyArtifacts("plugins/Identica") { into, source ->
		bungeecord.addInstall(into, source)
	}
}

private fun addExtendedPaperBackends(project: Project, scenario: Any) {
	scenario.addPaper("auth") { paper ->
		configurePaper(project, paper, 25566, "dev/scenarios/extended/auth")
	}
	scenario.addPaper("migration") { paper ->
		configurePaper(project, paper, 25567, "dev/scenarios/extended/migration")
	}
	scenario.addPaper("registration") { paper ->
		configurePaper(project, paper, 25568, "dev/scenarios/extended/registration")
	}
	scenario.addPaper("lobby") { paper ->
		configurePaper(project, paper, 25569, "dev/scenarios/extended/lobby")
	}
}

private fun configurePaper(
	project: Project,
	paper: Any,
	port: Int,
	rootOverlayDir: String? = null
) {
	paper.setInt("port", port)
	paper.setJvmArgs(paperJvmArgs)
	paper.setBoolean("onlineMode", false)
	if (rootOverlayDir != null) {
		paper.setDirectory(project, "rootOverlayDir", rootOverlayDir)
	}
}

private fun extendedProxyServers(): List<Pair<String, String>> = listOf(
	"auth" to "127.0.0.1:25566",
	"migration" to "127.0.0.1:25567",
	"registration" to "127.0.0.1:25568",
	"lobby" to "127.0.0.1:25569"
)
