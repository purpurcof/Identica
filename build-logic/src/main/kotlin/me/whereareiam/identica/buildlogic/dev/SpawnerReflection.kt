package me.whereareiam.identica.buildlogic.dev

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

internal fun Any.registerScenario(name: String, configure: (Any) -> Unit) {
	javaClass.getMethod("register", String::class.java, Action::class.java)
		.invoke(
			this,
			name,
			object : Action<Any> {
				override fun execute(scenario: Any) = configure(scenario)
			}
		)
}

internal fun Any.addVelocity(name: String, configure: (Any) -> Unit) {
	javaClass.getMethod("velocity", String::class.java, Action::class.java)
		.invoke(
			this,
			name,
			object : Action<Any> {
				override fun execute(velocity: Any) = configure(velocity)
			}
		)
}

internal fun Any.addBungeeCord(name: String, configure: (Any) -> Unit) {
	javaClass.getMethod("bungeecord", String::class.java, Action::class.java)
		.invoke(
			this,
			name,
			object : Action<Any> {
				override fun execute(bungeecord: Any) = configure(bungeecord)
			}
		)
}

internal fun Any.addPaper(name: String, configure: (Any) -> Unit) {
	javaClass.getMethod("paper", String::class.java, Action::class.java)
		.invoke(
			this,
			name,
			object : Action<Any> {
				override fun execute(paper: Any) = configure(paper)
			}
		)
}

internal fun Any.addInstall(into: String, source: Any) {
	javaClass.getMethod("install", Action::class.java)
		.invoke(
			this,
			object : Action<Any> {
				override fun execute(install: Any) {
					install.javaClass.getMethod("from", Array<Any>::class.java).invoke(install, arrayOf(source))
					(install.readProperty("into") as Property<String>).set(into)
				}
			}
		)
}

internal fun Any.addServer(name: String, address: String) {
	javaClass.getMethod("server", String::class.java, String::class.java)
		.invoke(this, name, address)
}

internal fun Any.setTryServers(vararg names: String) {
	javaClass.getMethod("tryServers", Array<String>::class.java).invoke(this, names)
}

internal fun Any.setDirectory(project: Project, propertyName: String, relativePath: String) {
	(readProperty(propertyName) as DirectoryProperty).set(project.layout.projectDirectory.dir(relativePath))
}

internal fun Any.setInt(propertyName: String, value: Int) {
	(readProperty(propertyName) as Property<Int>).set(value)
}

internal fun Any.setJvmArgs(value: List<String>) {
	(readProperty("jvmArgs") as ListProperty<String>).set(value)
}

internal fun Any.setBoolean(propertyName: String, value: Boolean) {
	(readProperty(propertyName) as Property<Boolean>).set(value)
}

internal fun Any.setString(propertyName: String, value: String) {
	(readProperty(propertyName) as Property<String>).set(value)
}

internal fun Any.readProperty(name: String): Any {
	val methodName = "get" + name.replaceFirstChar { it.uppercase() }
	return javaClass.getMethod(methodName).invoke(this)
}
