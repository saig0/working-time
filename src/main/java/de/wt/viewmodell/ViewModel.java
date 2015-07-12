package de.wt.viewmodell;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import de.wt.model.Configuration;
import de.wt.model.WorkLogEntry;
import de.wt.model.WorkingLog;

public class ViewModel {

	private final BooleanProperty automaticSaveProperty = new SimpleBooleanProperty();
	private Configuration config;

	private ObjectProperty<Instant> currentDateProperty = new SimpleObjectProperty<Instant>(
			Instant.now());

	private ObjectProperty<Instant> currentTimeProperty = new SimpleObjectProperty<Instant>(
			Instant.now());

	private final ScheduledExecutorService executorService = Executors
			.newSingleThreadScheduledExecutor();

	private WorkingLog log;

	private final ObservableList<File> recentlyOpenedLogFilesProperty = FXCollections
			.observableArrayList();

	private final BooleanProperty runningProperty = new SimpleBooleanProperty(
			false);

	private ObjectProperty<Duration> workedTimeDayProperty = new SimpleObjectProperty<Duration>(
			Duration.ZERO);

	private ObjectProperty<Duration> workedTimeInWeekProperty = new SimpleObjectProperty<Duration>(
			Duration.ZERO);

	private DoubleProperty workedTimeOfDayProperty = new SimpleDoubleProperty(0);

	private DoubleProperty workedTimeOfWeekProperty = new SimpleDoubleProperty(
			0);

	private final ObservableList<WorkLogEntry> workLogEntryProperty = FXCollections
			.observableArrayList();

	public ViewModel() {
	}

	public BooleanProperty automaticSaveProperty() {
		return automaticSaveProperty;
	}

	public void cleanup() {
		executorService.shutdown();
	}

	public void createNewWorkingLog() {
		log = new WorkingLog();

		runningProperty.set(false);
		update();

		config.setLastOpenedLogFile(null);
		onChangedConfig();
	}

	public ReadOnlyObjectProperty<Instant> currentDateProperty() {
		return currentDateProperty;
	}

	public ReadOnlyObjectProperty<Instant> currentTimeProperty() {
		return currentTimeProperty;
	}

	public File getDefaultLogFileLocation() {
		return config.getDefaultLogFileLocation();
	}

	public File getLastOpenedLogFile() {
		return config.getLastOpenedLogFile();
	}

	public void init() {
		config = loadConfig();

		automaticSaveProperty.set(config.isAutomaticSave());
		automaticSaveProperty.addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable,
					Boolean oldValue, Boolean newValue) {
				config.setAutomaticSave(newValue);
				onChangedConfig();
			}
		});

		recentlyOpenedLogFilesProperty.addAll(config
				.getRecentlyOpenedLogFiles());

		executorService.scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				Platform.runLater(new Runnable() {

					@Override
					public void run() {
						Instant now = Instant.now();

						currentTimeProperty.set(now);
						currentDateProperty.set(now);

						if (runningProperty.get()) {
							WorkLogEntry logEntry = getLogEntryForToday();
							if (logEntry.getEndTime() != null) {
								throw new IllegalStateException();
							}
							Instant startTime = logEntry.getStartTime();

							Duration duration = Duration
									.between(startTime, now);
							workedTimeDayProperty.set(duration);

							log.getWorkLogEntriesForToday()
									.stream()
									.filter(log -> log.getEndTime() != null)
									.forEach(
											l -> {
												workedTimeDayProperty
														.set(workedTimeDayProperty
																.get()
																.plus(Duration
																		.between(
																				l.getStartTime(),
																				l.getEndTime())));
											});

							workedTimeOfDayProperty.set(Double
									.valueOf(workedTimeDayProperty.get()
											.toMinutes())
									/ Duration.ofHours(8).toMinutes());

							workedTimeInWeekProperty.set(Duration.ZERO);
							log.getWorkLogEntriesForWeek()
									.stream()
									.forEach(
											l -> {
												Duration d = Duration.between(
														l.getStartTime(),
														Optional.ofNullable(
																l.getEndTime())
																.orElse(now));
												workedTimeInWeekProperty
														.set(workedTimeInWeekProperty
																.get().plus(d));
											});

							workedTimeOfWeekProperty.set(Double
									.valueOf(workedTimeInWeekProperty.get()
											.toMinutes())
									/ Duration.ofHours(40).toMinutes());
						}

					}

				});
			}
		}, 0, 1, TimeUnit.SECONDS);

		log = loadInitialWorkingLog();

		log.getLastWorkLogEntryForToday()
				.filter(logEntry -> logEntry.getEndTime() == null)
				.ifPresent(logEntry -> {
					runningProperty.set(true);

					System.out.println("running");
				});
	}

	public void openLog(File file) {
		config.setLastOpenedLogFile(file);
		onChangedConfig();

		recentlyOpenedLogFilesProperty.add(file);

		log = WorkingLog.load(file).orElseThrow(() -> new RuntimeException());

		log.getLastWorkLogEntryForToday()
				.filter(logEntry -> logEntry.getEndTime() == null)
				.ifPresent(logEntry -> {
					runningProperty().set(true);

					System.out.println("running");
				});

		update();
	}

	public ObservableList<File> recentlyOpenedLogFilesProperty() {
		return recentlyOpenedLogFilesProperty;
	}

	public BooleanProperty runningProperty() {
		return runningProperty;
	}

	public void saveLog(File file) {
		log.save(file);

		config.setLastOpenedLogFile(file);
		onChangedConfig();
	}

	public void start() {
		runningProperty.set(true);

		Instant startTime = Instant.now();
		log.getWorkLogEntries().add(new WorkLogEntry(startTime));

		// TODO
		update();
	}

	public void stop() {
		runningProperty.set(false);

		WorkLogEntry logEntry = getLogEntry();
		if (logEntry.getEndTime() != null) {
			throw new IllegalStateException();
		}
		logEntry.setEndTime(Instant.now());

		// TODO
		update();
	}

	// TODO
	public void update() {
		updateWorkTimeOfDay();
		updateWorkTimeOfWeek();

		workLogEntryProperty.clear();
		workLogEntryProperty.addAll(log.getWorkLogEntries());
	}

	public ReadOnlyObjectProperty<Duration> workedTimeDayProperty() {
		return workedTimeDayProperty;
	}

	public DoubleProperty workedTimeOfDayProperty() {
		return workedTimeOfDayProperty;
	}

	public DoubleProperty workedTimeOfWeekProperty() {
		return workedTimeOfWeekProperty;
	}

	public ReadOnlyObjectProperty<Duration> workedTimeWeekProperty() {
		return workedTimeInWeekProperty;
	}

	public ObservableList<WorkLogEntry> workLogEntryProperty() {
		return workLogEntryProperty;
	}

	private WorkLogEntry getLogEntry() {
		WorkLogEntry logEntry = log.getLastWorkLogEntryForToday().orElseThrow(
				() -> new IllegalStateException());
		return logEntry;
	}

	private WorkLogEntry getLogEntryForToday() {
		WorkLogEntry logEntry = log.getLastWorkLogEntryForToday().orElseThrow(
				() -> new IllegalStateException());
		return logEntry;
	}

	private Configuration loadConfig() {
		return Configuration.load().orElse(new Configuration());
	}

	private WorkingLog loadInitialWorkingLog() {
		if (config.getLastOpenedLogFile().exists()) {
			return WorkingLog.load(config.getLastOpenedLogFile()).orElseThrow(
					() -> new RuntimeException());
		} else {
			return new WorkingLog();
		}
	}

	private void onChangedConfig() {
		// TODO check if it's updated
		config.save();
	}

	private void updateWorkTimeOfDay() {
		// TODO DRY

		workedTimeDayProperty.set(Duration.ZERO);

		log.getWorkLogEntriesForToday()
				.stream()
				.filter(log -> log.getEndTime() != null)
				.forEach(
						logEntry -> {
							workedTimeDayProperty.set(workedTimeDayProperty
									.get().plus(
											Duration.between(
													logEntry.getStartTime(),
													logEntry.getEndTime())));
						});

		workedTimeOfDayProperty.set(Double.valueOf(workedTimeDayProperty.get()
				.toMinutes()) / Duration.ofHours(8).toMinutes());
	}

	private void updateWorkTimeOfWeek() {
		// TODO DRY
		workedTimeInWeekProperty.set(Duration.ZERO);
		log.getWorkLogEntriesForWeek()
				.stream()
				.filter(log -> log.getEndTime() != null)
				.forEach(
						l -> {
							workedTimeInWeekProperty
									.set(workedTimeInWeekProperty.get().plus(
											Duration.between(l.getStartTime(),
													l.getEndTime())));
						});

		workedTimeOfWeekProperty.set(Double.valueOf(workedTimeInWeekProperty
				.get().toMinutes()) / Duration.ofHours(40).toMinutes());
	}

}
