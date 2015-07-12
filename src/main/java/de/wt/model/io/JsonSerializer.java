package de.wt.model.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JsonSerializer {

	public <T> Optional<T> read(File file, Class<T> valueType) {
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

	public void write(File file, Object object) {
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		mapper.findAndRegisterModules();

		try {
			mapper.writeValue(file, object);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
