package de.wt.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import de.wt.model.Configuration;
import de.wt.model.WorkingLog;

public class JsonSerializer {

	public void writeLogFile(WorkingLog log, File file) {
		write(file, log);

	}

	public Optional<WorkingLog> readLogFile(File file) {
		return read(file, WorkingLog.class);
	}

	public void writeConfig(Configuration configuration) {
		write(getConfigLocation(), configuration);
	}

	public Optional<Configuration> readConfig() {
		return read(getConfigLocation(), Configuration.class);
	}

	private void write(File file, Object object) {
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		mapper.findAndRegisterModules();

		try {
			mapper.writeValue(file, object);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private <T> Optional<T> read(File file, Class<T> valueType) {
		ObjectMapper mapper = new ObjectMapper();
		mapper.findAndRegisterModules();

		try {
			T log = mapper.readValue(file, valueType);
			return Optional.of(log);
		} catch (FileNotFoundException e) {
			return Optional.empty();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private File getUserDirectory() {
		return new File(System.getProperty("user.home"));
	}

	private File getConfigLocation() {
		return new File(getUserDirectory(), "workingLog-config.json");
	}
}
