package me.whereareiam.identica.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import me.whereareiam.identica.model.provider.dependency.ProviderLibraries;

import java.util.List;

@Getter
@Setter
@ToString
public class ProviderDescriptor {
	private String id;
	private String name;
	private String version;

	private String main;
	private List<String> authors;
	private List<String> supportedPlatforms;

	private int priorityDefault = 0;

	private ProviderLibraries libraries;
}
