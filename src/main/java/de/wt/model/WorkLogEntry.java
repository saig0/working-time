package de.wt.model;

import java.time.Instant;

public class WorkLogEntry {
	private Instant startTime;
	private Instant endTime;

	public WorkLogEntry() {
	}

	public WorkLogEntry(Instant startTime) {
		this.startTime = startTime;
	}

	public WorkLogEntry(Instant startTime, Instant endTime) {
		this.startTime = startTime;
		this.endTime = endTime;
	}

	public Instant getStartTime() {
		return startTime;
	}

	public void setStartTime(Instant startTime) {
		this.startTime = startTime;
	}

	public Instant getEndTime() {
		return endTime;
	}

	public void setEndTime(Instant endTime) {
		this.endTime = endTime;
	}

}
