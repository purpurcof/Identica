package me.whereareiam.identica.conflict.resolver.typed;

import me.whereareiam.configura.Config;
import me.whereareiam.configura.node.Node;
import me.whereareiam.configura.node.ObjectNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility for binding conflict resolver parameters.
 */
public final class ConflictParameters {
	/**
	 * Bind a configuration node to the given config type.
	 *
	 * <pre>{@code
	 * public final class FormatConfig {
	 *     public String format;
	 * }
	 *
	 * FormatConfig config = ConflictParameters.bind(params, FormatConfig.class);
	 * }</pre>
	 *
	 * @param node parameter node
	 * @param type config type
	 * @param <T>  config type
	 * @return bound config instance
	 */
	public static <T> @NotNull T bind(
			@Nullable Node node,
			@NotNull Class<T> type
	) {
		Node safe = node != null ? node : new ObjectNode();
		byte[] bytes = Config.getDefaultWriter().encodeNode(safe);
		return Config.getDefaultReader().load(bytes, type);
	}
}
