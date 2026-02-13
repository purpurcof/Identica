package me.whereareiam.identica.type.pipeline;

public enum PipelineStatus {
	CONTINUE,
	WAITING,
	COMPLETE,
	FAILED,
	DENIED,
	REQUIRE_RECONNECT,
	NO_PENDING
}
