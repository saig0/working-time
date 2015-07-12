package de.wt.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import de.wt.model.io.JsonSerializer;

public class Configuration {

	public static Optional<Configuration> load() {
		return new JsonSerializer().read(getConfigLocation(),
				Configuration.class);
	}

	private static File getConfigLocation() {
		return new File(getUserDirectory(), "workingLog-config.json");
	}

	private static File getUserDirectory() {
		return new File(System.getProperty("user.home"));
	}

	private boolean automaticSave = false;

	private File defaultLogFileLocation = new File(
			System.getProperty("user.home"));

	private File lastOpenedLogFile;

	private List<File> recentlyOpenedLogFiles = new ArrayList<File>();

	public File getDefaultLogFileLocation() {
		return defaultLogFileLocation;
	}

	public File getLastOpenedLogFile() {
		return Optional.ofNullable(lastOpenedLogFile).orElse(
				new File(defaultLogFileLocation, "workingLog.json"));
	}

	public List<File> getRecentlyOpenedLogFiles() {
		return recentlyOpenedLogFiles;
	}

	public boolean isAutomaticSave() {
		return automaticSave;
	}

	public void save() {
		new JsonSerializer().write(getConfigLocation(), this);
	}

	public void setAutomaticSave(boolean automaticSave) {
		this.automaticSave = automaticSave;
	}

	public void setDefaultLogFileLocation(File defaultLogFileLocation) {
		this.defaultLogFileLocation = defaultLogFileLocation;
	}

	public void setLastOpenedLogFile(File lastOpenedLogFile) {
		this.lastOpenedLogFile = lastOpenedLogFile;
		if (lastOpenedLogFile != null
				&& !recentlyOpenedLogFiles.contains(lastOpenedLogFile)) {
			recentlyOpenedLogFiles.add(lastOpenedLogFile);
		}
	}

	public void setRecentlyOpenedLogFiles(List<File> recentlyOpenedLogFiles) {
		this.recentlyOpenedLogFiles = recentlyOpenedLogFiles;
	}
}
