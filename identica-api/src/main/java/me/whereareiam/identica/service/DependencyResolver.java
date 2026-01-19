package me.whereareiam.identica.service;

import me.whereareiam.attache.model.LibraryRequest;

import java.util.List;

@SuppressWarnings("unused")
public interface DependencyResolver {
	void resolveDependencies();

	void loadLibraries();

	void addDependency(LibraryRequest library);

	void clearDependencies();

	List<LibraryRequest> getLibraries();
}
