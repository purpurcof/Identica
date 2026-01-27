package me.whereareiam.identica.common.config.adapter;

import me.whereareiam.configura.TypeAdapter;
import me.whereareiam.configura.node.Node;
import me.whereareiam.configura.node.NullNode;
import me.whereareiam.configura.node.StringNode;
import org.jetbrains.annotations.NotNull;

/**
 * Pass-through adapter for Configura {@link Node} values.
 */
public class NodeAdapter implements TypeAdapter<Node> {
	@Override
	public @NotNull Node deserialize(String value) {
		if (value == null) return NullNode.instance();
		return new StringNode(value);
	}

	@Override
	public @NotNull String serialize(Node value) {
		return value == null ? "" : value.asText();
	}

	@Override
	public @NotNull Node deserializeNode(Node node) {
		return node != null ? node : NullNode.instance();
	}

	@Override
	public @NotNull Node serializeNode(Node value) {
		return value != null ? value : NullNode.instance();
	}
}
