package de.wt.io;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import de.wt.model.WorkingLog;

public class JsonSerializer {

	public void write(WorkingLog log, File file) {
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		mapper.findAndRegisterModules();

		try {
			mapper.writeValue(file, log);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public WorkingLog read(File file) {
		ObjectMapper mapper = new ObjectMapper();
		mapper.findAndRegisterModules();
		
		try {
			WorkingLog log = mapper.readValue(file, WorkingLog.class);
			return log;
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}
