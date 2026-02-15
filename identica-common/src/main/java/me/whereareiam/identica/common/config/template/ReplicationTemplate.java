package me.whereareiam.identica.common.config.template;

import com.google.inject.Singleton;
import me.whereareiam.configura.TemplateProvider;
import me.whereareiam.identica.model.config.Replication;

import java.util.UUID;

@Singleton
public class ReplicationTemplate implements TemplateProvider<Replication> {
	@Override
	public Replication supply(Replication replication) {
		replication.setEnabled(false);
		replication.setServerId(UUID.randomUUID().toString());

		Replication.Redis redis = new Replication.Redis();
		redis.setHost("localhost");
		redis.setPort(6379);
		redis.setPassword("");

		Replication.Channels channels = new Replication.Channels();
		channels.setAccountUpdates("identica:accounts");
		channels.setSessions("identica:sessions");
		channels.setConflicts("identica:conflicts");
		redis.setChannels(channels);

		Replication.Cache cache = new Replication.Cache();
		cache.setReservations("identica:reservation");
		cache.setInstructions("identica:handshake-instructions");
		cache.setAttempts("identica:provider-attempts");

		Replication.Sessions sessions = new Replication.Sessions();
		sessions.setUser("identica:sessions:user");
		sessions.setSession("identica:sessions:session");
		sessions.setSubject("identica:sessions:subject");
		cache.setSessions(sessions);
		cache.setPipelineState("identica:pipeline-state");

		replication.setRedis(redis);
		replication.setCache(cache);
		return replication;
	}
}
