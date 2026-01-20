package me.whereareiam.identica.model.conflict;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.auth.IdentityClaim;

import java.util.List;

/**
 * Context provided to providers when applying conflict resolution.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class ConflictContext {
	private IdentityClaim claim;
	private List<Session> existingSessions;
	private String conflictKey;
	private String conflictValue;
}
