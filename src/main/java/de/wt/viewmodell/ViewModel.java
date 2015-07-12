package de.wt.viewmodell;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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
	private StringProperty currentDateProperty = new SimpleStringProperty();

	private StringProperty currentTimeProperty = new SimpleStringProperty();

	private final DateTimeFormatter dateFormatter = DateTimeFormatter
			.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.GERMAN)
			.withZone(ZoneId.systemDefault());

	private final ScheduledExecutorService executorService = Executors
			.newSingleThreadScheduledExecutor();

	private WorkingLog log;

	private final ObservableList<File> recentlyOpenedLogFilesProperty = FXCollections
			.observableArrayList();

	private final BooleanProperty runningProperty = new SimpleBooleanProperty(
			false);

	private final DateTimeFormatter timeFormatter = DateTimeFormatter
			.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.GERMAN)
			.withZone(ZoneId.systemDefault());

	private Duration workedTime = Duration.ZERO;

	private StringProperty workedTimeDayProperty = new SimpleStringProperty(
			"00:00");

	private Duration workedTimeInWeek = Duration.ZERO;

	private DoubleProperty workedTimeOfDayProperty = new SimpleDoubleProperty(0);

	private DoubleProperty workedTimeOfWeekProperty = new SimpleDoubleProperty(
			0);

	private StringProperty workedTimeWeekProperty = new SimpleStringProperty(
			"00:00");

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

	public StringProperty currentDateProperty() {
		return currentDateProperty;
	}

	public StringProperty currentTimeProperty() {
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

						currentTimeProperty.set(timeFormatter.format(now));
						currentDateProperty.set(dateFormatter.format(now));

						if (runningProperty.get()) {
							WorkLogEntry logEntry = getLogEntryForToday();
							if (logEntry.getEndTime() != null) {
								throw new IllegalStateException();
							}
							Instant startTime = logEntry.getStartTime();

							Duration duration = Duration
									.between(startTime, now);
							workedTime = duration;

							log.getWorkLogEntriesForToday()
									.stream()
									.filter(log -> log.getEndTime() != null)
									.forEach(
											l -> {
												workedTime = workedTime.plus(Duration.between(
														l.getStartTime(),
														l.getEndTime()));
											});

							workedTimeOfDayProperty.set(Double
									.valueOf(workedTime.toMinutes())
									/ Duration.ofHours(8).toMinutes());
							workedTimeDayProperty
									.set(getFormattedWorkTime(workedTime));

							workedTimeInWeek = Duration.ZERO;
							log.getWorkLogEntriesForWeek()
									.stream()
									.forEach(
											l -> {
												Duration d = Duration.between(
														l.getStartTime(),
														Optional.ofNullable(
																l.getEndTime())
																.orElse(now));
												workedTimeInWeek = workedTimeInWeek
														.plus(d);
											});

							workedTimeOfWeekProperty.set(Double
									.valueOf(workedTimeInWeek.toMinutes())
									/ Duration.ofHours(40).toMinutes());
							workedTimeWeekProperty
									.set(getFormattedWorkTime(workedTimeInWeek));
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

	public StringProperty workedTimeDayProperty() {
		return workedTimeDayProperty;
	}

	public DoubleProperty workedTimeOfDayProperty() {
		return workedTimeOfDayProperty;
	}

	public DoubleProperty workedTimeOfWeekProperty() {
		return workedTimeOfWeekProperty;
	}

	public StringProperty workedTimeWeekProperty() {
		return workedTimeWeekProperty;
	}

	public ObservableList<WorkLogEntry> workLogEntryProperty() {
		return workLogEntryProperty;
	}

	private SimpleStringProperty getDateAsProperty(Instant instant) {
		LocalDateTime localDateTime = LocalDateTime.ofInstant(instant,
				ZoneId.systemDefault());
		String formattedDateTime = dateFormatter.format(localDateTime);
		return new SimpleStringProperty(formattedDateTime);
	}

	private String getFormattedWorkTime(Duration workedTime) {
		return String.format("%02d:%02d", workedTime.toHours(),
				workedTime.toMinutes() % 60);
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

	private SimpleStringProperty getTimeAsProperty(Instant instant) {
		LocalDateTime localDateTime = LocalDateTime.ofInstant(instant,
				ZoneId.systemDefault());
		String formattedDateTime = timeFormatter.format(localDateTime);
		return new SimpleStringProperty(formattedDateTime);
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

		workedTime = Duration.ZERO;

		log.getWorkLogEntriesForToday()
				.stream()
				.filter(log -> log.getEndTime() != null)
				.forEach(
						logEntry -> {
							workedTime = workedTime.plus(Duration.between(
									logEntry.getStartTime(),
									logEntry.getEndTime()));
						});

		workedTimeOfDayProperty.set(Double.valueOf(workedTime.toMinutes())
				/ Duration.ofHours(8).toMinutes());
		workedTimeDayProperty.set(getFormattedWorkTime(workedTime));
	}

	private void updateWorkTimeOfWeek() {
		// TODO DRY
		workedTimeInWeek = Duration.ZERO;
		log.getWorkLogEntriesForWeek()
				.stream()
				.filter(log -> log.getEndTime() != null)
				.forEach(
						l -> {
							workedTimeInWeek = workedTimeInWeek.plus(Duration
									.between(l.getStartTime(), l.getEndTime()));
						});

		workedTimeOfWeekProperty.set(Double.valueOf(workedTimeInWeek
				.toMinutes()) / Duration.ofHours(40).toMinutes());
		workedTimeWeekProperty.set(getFormattedWorkTime(workedTimeInWeek));
	}

}
