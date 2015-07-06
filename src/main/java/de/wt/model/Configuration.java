package de.wt.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Configuration {

	private File defaultLogFileLocation = new File(System
			.getProperty("user.home"));

	private File lastOpenedLogFile;

	private List<File> recentlyOpenedLogFiles = new ArrayList<File>();

	private boolean automaticSave = false;
	
	public File getLastOpenedLogFile() {
		return Optional.ofNullable(lastOpenedLogFile).orElse(
				new File(defaultLogFileLocation, "workingLog.json"));
	}

	public File getDefaultLogFileLocation() {
		return defaultLogFileLocation;
	}

	public void setDefaultLogFileLocation(File defaultLogFileLocation) {
		this.defaultLogFileLocation = defaultLogFileLocation;
	}

	public List<File> getRecentlyOpenedLogFiles() {
		return recentlyOpenedLogFiles;
	}

	public void setRecentlyOpenedLogFiles(List<File> recentlyOpenedLogFiles) {
		this.recentlyOpenedLogFiles = recentlyOpenedLogFiles;
	}

	public boolean isAutomaticSave() {
		return automaticSave;
	}

	public void setAutomaticSave(boolean automaticSave) {
		this.automaticSave = automaticSave;
	}

	public void setLastOpenedLogFile(File lastOpenedLogFile) {
		this.lastOpenedLogFile = lastOpenedLogFile;
		if(lastOpenedLogFile != null && !recentlyOpenedLogFiles.contains(lastOpenedLogFile)){
			recentlyOpenedLogFiles.add(lastOpenedLogFile);
		}
	}
	
}
