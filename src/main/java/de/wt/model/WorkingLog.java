package de.wt.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class WorkingLog {

	private List<WorkLogEntry> workLogEntries = new ArrayList<WorkLogEntry>();

	public List<WorkLogEntry> getWorkLogEntries() {
		return workLogEntries;
	}

	public void setWorkLogEntries(List<WorkLogEntry> workLogEntries) {
		this.workLogEntries = workLogEntries;
	}

	@JsonIgnore
	public Optional<WorkLogEntry> getWorkLogEntryForToday() {
		return workLogEntries
				.stream()
				.filter(log -> LocalDateTime.ofInstant(log.getStartTime(),
						ZoneId.systemDefault()).getDayOfYear() == LocalDateTime
						.now().getDayOfYear()).findAny();
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
}
