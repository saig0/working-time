package de.wt.viewmodell;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import de.wt.model.Configuration;
import de.wt.model.WorkLogEntry;
import de.wt.model.WorkingLog;
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

public class ViewModel {

	private final BooleanProperty automaticSaveProperty = new SimpleBooleanProperty();

	private final BooleanProperty editableLog = new SimpleBooleanProperty();

	private Configuration config;

	private ObjectProperty<Instant> currentDateProperty = new SimpleObjectProperty<Instant>(
			Instant.now());

	private ObjectProperty<Instant> currentTimeProperty = new SimpleObjectProperty<Instant>(
			Instant.now());

	private final ScheduledExecutorService executorService = Executors
			.newSingleThreadScheduledExecutor();

	private ObjectProperty<WorkingLog> log = new SimpleObjectProperty<WorkingLog>();

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
		log.set(new WorkingLog());

		runningProperty.set(false);

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

		editableLog.set(config.isEditableLog());
		editableLog.addListener(new ChangeListener<Boolean>() {

      @Override
      public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
        config.setEditableLog(newValue);
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
							updateWorkTimeOfDay();
							updateWorkTimeOfWeek();
						}

					}

				});
			}
		}, 0, 1, TimeUnit.SECONDS);

		log.addListener(new ChangeListener<WorkingLog>() {

			@Override
			public void changed(
					ObservableValue<? extends WorkingLog> observable,
					WorkingLog oldValue, WorkingLog newValue) {

				update(newValue);
			}
		});

		log.set(loadInitialWorkingLog());
	}

	public void openLog(File file) {
		config.setLastOpenedLogFile(file);
		onChangedConfig();

		recentlyOpenedLogFilesProperty.add(file);

		log.set(WorkingLog.load(file).orElseThrow(() -> new RuntimeException()));
	}

	public ObservableList<File> recentlyOpenedLogFilesProperty() {
		return recentlyOpenedLogFilesProperty;
	}

	public BooleanProperty runningProperty() {
		return runningProperty;
	}

	public void saveLog(File file) {
		log.get().save(file);

		config.setLastOpenedLogFile(file);
		onChangedConfig();
	}

	public void start() {
		runningProperty.set(true);

		Instant startTime = Instant.now();

		updateWorkingLog(l -> l.getWorkLogEntries().add(
				new WorkLogEntry(startTime)));
	}

	private void updateWorkingLog(Consumer<WorkingLog> updateFunction) {
		updateFunction.accept(log.get());

		// TODO simulate change event
		update(log.get());
	}

	public void stop() {
		runningProperty.set(false);

		updateWorkingLog(log -> {
			WorkLogEntry logEntry = log.getLastWorkLogEntryForToday()
					.orElseThrow(() -> new IllegalStateException());

			if (logEntry.getEndTime() != null) {
				throw new IllegalStateException();
			}

			logEntry.setEndTime(Instant.now());
		});
	}

	private void update(WorkingLog log) {

		boolean isRunning = log.getLastWorkLogEntryForToday()
				.filter(logEntry -> logEntry.getEndTime() == null).isPresent();
    runningProperty.set(isRunning);

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
		workedTimeDayProperty.set(Duration.ZERO);

		log.get()
				.getWorkLogEntriesForToday()
				.stream()
				.forEach(
						l -> {
							Duration d = Duration.between(
									l.getStartTime(),
									Optional.ofNullable(l.getEndTime()).orElse(
											Instant.now()));
							workedTimeDayProperty.set(workedTimeDayProperty
									.get().plus(d));
						});

		workedTimeOfDayProperty.set(Double.valueOf(workedTimeDayProperty.get()
				.toMinutes()) / Duration.ofHours(8).toMinutes());
	}

	private void updateWorkTimeOfWeek() {
		workedTimeInWeekProperty.set(Duration.ZERO);
		log.get()
				.getWorkLogEntriesForWeek()
				.stream()
				.forEach(
						l -> {
							Duration d = Duration.between(
									l.getStartTime(),
									Optional.ofNullable(l.getEndTime()).orElse(
											Instant.now()));
							workedTimeInWeekProperty
									.set(workedTimeInWeekProperty.get().plus(d));
						});

		workedTimeOfWeekProperty.set(Double.valueOf(workedTimeInWeekProperty
				.get().toMinutes()) / Duration.ofHours(40).toMinutes());
	}

  public BooleanProperty editableLogProperty() {
    return editableLog;
  }

  public void setStartTime(WorkLogEntry logEntry, Instant startTime) {
    updateWorkingLog(log -> {
      int index = log.getWorkLogEntries().indexOf(logEntry);
      WorkLogEntry workLogEntry = log.getWorkLogEntries().get(index);
      workLogEntry.setStartTime(startTime);
    });
  }

  public void setEndTime(WorkLogEntry logEntry, Instant endTime) {
    updateWorkingLog(log -> {
      int index = log.getWorkLogEntries().indexOf(logEntry);
      WorkLogEntry workLogEntry = log.getWorkLogEntries().get(index);
      workLogEntry.setEndTime(endTime);
    });
  }

}
