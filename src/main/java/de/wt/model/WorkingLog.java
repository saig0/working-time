package de.wt.model;

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.wt.model.io.JsonSerializer;

public class WorkingLog {

	public static Optional<WorkingLog> load(File file) {
		return new JsonSerializer().read(file, WorkingLog.class);
	}

	private List<WorkLogEntry> workLogEntries = new ArrayList<WorkLogEntry>();

	@JsonIgnore
	public Optional<WorkLogEntry> getLastWorkLogEntryForToday() {
		return workLogEntries
				.stream()
				.filter(log -> LocalDateTime.ofInstant(log.getStartTime(),
						ZoneId.systemDefault()).getDayOfYear() == LocalDateTime
						.now().getDayOfYear())
				.max(Comparator.comparing(log -> log.getStartTime()));
	}

	public List<WorkLogEntry> getWorkLogEntries() {
		return workLogEntries;
	}

	@JsonIgnore
	public List<WorkLogEntry> getWorkLogEntriesForToday() {
		return workLogEntries
				.stream()
				.filter(log -> LocalDateTime.ofInstant(log.getStartTime(),
						ZoneId.systemDefault()).getDayOfYear() == LocalDateTime
						.now().getDayOfYear()).collect(Collectors.toList());
	}

	@JsonIgnore
	public List<WorkLogEntry> getWorkLogEntriesForWeek() {
		List<WorkLogEntry> list = workLogEntries
				.stream()
				.filter(log -> LocalDateTime.ofInstant(log.getStartTime(),
						ZoneId.systemDefault()).get(
						WeekFields.of(Locale.getDefault())
								.weekOfWeekBasedYear()) == LocalDateTime.now()
						.get(WeekFields.of(Locale.getDefault())
								.weekOfWeekBasedYear()))
				.collect(Collectors.toList());
		return list;
	}

	public void save(File file) {
		new JsonSerializer().write(file, this);
	}

	public void setWorkLogEntries(List<WorkLogEntry> workLogEntries) {
		this.workLogEntries = workLogEntries;
	}
}
