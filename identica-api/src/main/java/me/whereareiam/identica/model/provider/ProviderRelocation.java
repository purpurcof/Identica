package me.whereareiam.identica.model.provider;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class ProviderRelocation {
	private String pattern;
	private String relocatedPattern;
	private List<String> includes;
	private List<String> excludes;
}
