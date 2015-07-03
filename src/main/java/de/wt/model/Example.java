package de.wt.model;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class Example {

	public WorkingLog create() {
		WorkingLog log = new WorkingLog();
		
		log.getWorkLogEntries().add(new WorkLogEntry(LocalDateTime.of(2015, 7, 1, 9, 0).toInstant(ZoneOffset.UTC),LocalDateTime.of(2015, 7, 1, 18, 15).toInstant(ZoneOffset.UTC)));
		log.getWorkLogEntries().add(new WorkLogEntry(LocalDateTime.of(2015, 7, 2, 7, 30).toInstant(ZoneOffset.UTC),LocalDateTime.of(2015, 7, 2, 16, 10).toInstant(ZoneOffset.UTC)));
		//log.getWorkLogEntries().add(new WorkLogEntry(LocalDateTime.of(2015, 7, 3, 7, 35).toInstant(ZoneOffset.UTC),LocalDateTime.of(2015, 7, 3, 15, 05).toInstant(ZoneOffset.UTC)));
				
		return log;
	}
}
