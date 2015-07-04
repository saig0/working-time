package de.wt.fx;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;
import java.util.Timer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.wt.io.JsonSerializer;
import de.wt.model.Example;
import de.wt.model.WorkLogEntry;
import de.wt.model.WorkingLog;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

public class Controller {

	@FXML
	private Label timeField;

	@FXML
	private Label dateField;

	@FXML
	private ProgressBar progressDay;

	@FXML
	private ProgressBar progressWeek;

	@FXML
	private Label dayField;

	@FXML
	private Label weekField;

	private Duration workedTime = Duration.ZERO;
	private Duration workedTimeInWeek = Duration.ZERO;

	private DoubleProperty workedTimeOfDay = new SimpleDoubleProperty(0);
	private DoubleProperty workedTimeOfWeek = new SimpleDoubleProperty(0);

	private StringProperty currentTime = new SimpleStringProperty();
	private StringProperty currentDate = new SimpleStringProperty();

	private StringProperty workedTimeDay = new SimpleStringProperty("00:00");
	private StringProperty workedTimeWeek = new SimpleStringProperty("00:00");

	private boolean running = false;

	private final ScheduledExecutorService executorService = Executors
			.newSingleThreadScheduledExecutor();

	private final DateTimeFormatter timeFormatter = DateTimeFormatter
			.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.GERMAN)
			.withZone(ZoneId.systemDefault());
	private final DateTimeFormatter dateFormatter = DateTimeFormatter
			.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.GERMAN)
			.withZone(ZoneId.systemDefault());

	private WorkingLog log = loadInitialWoringLog();

	private WorkingLog loadInitialWoringLog() {
		return new WorkingLog();
		// return new Example().create();
	}

	private final Stage primaryStage;

	public Controller(Stage primaryStage) {
		this.primaryStage = primaryStage;
	}

	@FXML
	private void initialize() {
		executorService.scheduleAtFixedRate(new Runnable() {

			public void run() {
				Platform.runLater(new Runnable() {

					@Override
					public void run() {
						Instant now = Instant.now();

						currentTime.set(timeFormatter.format(now));
						currentDate.set(dateFormatter.format(now));

						if (running) {
							Instant startTime = getLogEntry().getStartTime();

							Duration duration = Duration
									.between(startTime, now);

							workedTime = duration;

							workedTimeOfDay.set(Double.valueOf(workedTime
									.toMinutes())
									/ Duration.ofHours(8).toMinutes());
							workedTimeDay.set(getFormattedWorkTime(workedTime));

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

							workedTimeOfWeek.set(Double
									.valueOf(workedTimeInWeek.toMinutes())
									/ Duration.ofHours(40).toMinutes());
							workedTimeWeek
									.set(getFormattedWorkTime(workedTimeInWeek));
						}

					}

				});
			}
		}, 0, 1, TimeUnit.SECONDS);

		timeField.textProperty().bind(currentTime);
		dateField.textProperty().bind(currentDate);

		dayField.textProperty().bind(workedTimeDay);
		weekField.textProperty().bind(workedTimeWeek);

		progressDay.progressProperty().bind(workedTimeOfDay);
		progressWeek.progressProperty().bind(workedTimeOfWeek);

		updateWorkTimeOfDay();
		updateWorkTimeOfWeek();
	}

	private void updateWorkTimeOfWeek() {
		// DRY
		workedTimeInWeek = Duration.ZERO;
		log.getWorkLogEntriesForWeek()
				.stream()
				.forEach(
						l -> {
							workedTimeInWeek = workedTimeInWeek.plus(Duration
									.between(l.getStartTime(), l.getEndTime()));
						});

		workedTimeOfWeek.set(Double.valueOf(workedTimeInWeek.toMinutes())
				/ Duration.ofHours(40).toMinutes());
		workedTimeWeek.set(getFormattedWorkTime(workedTimeInWeek));
	}

	private void updateWorkTimeOfDay() {
		// DRY
		log.getWorkLogEntryForToday().ifPresent(
				logEntry -> {
					workedTime = Duration.between(logEntry.getStartTime(),
							logEntry.getEndTime());

					workedTimeOfDay.set(Double.valueOf(workedTime.toMinutes())
							/ Duration.ofHours(8).toMinutes());
					workedTimeDay.set(getFormattedWorkTime(workedTime));
				});
	}

	private String getFormattedWorkTime(Duration workedTime) {
		return String.format("%02d:%02d", workedTime.toHours(),
				workedTime.toMinutes() % 60);
	}

	@FXML
	public void start(ActionEvent event) {
		System.out.println("start");

		running = true;

		Instant startTime = Instant.now();
		log.getWorkLogEntries().add(new WorkLogEntry(startTime));
	}

	@FXML
	public void stop(ActionEvent event) {
		System.out.println("stop");
		running = false;

		WorkLogEntry logEntry = getLogEntry();
		logEntry.setEndTime(Instant.now());
	}

	private WorkLogEntry getLogEntry() {
		WorkLogEntry logEntry = log.getWorkLogEntryForToday().orElseThrow(
				() -> new IllegalStateException());
		return logEntry;
	}

	public void onShutDown() {
		executorService.shutdown();
	}

	@FXML
	public void saveLog() {
		System.out.println("save");

		File file = new File(getDefaultDirectory(), "workingLog.json");
		new JsonSerializer().write(log, file);
	}

	@FXML
	public void openLog() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Öffne Working Log");
		fileChooser.getExtensionFilters().add(
				new ExtensionFilter("JSON", "*.json"));
		fileChooser.setInitialDirectory(getDefaultDirectory());

		Optional.ofNullable(fileChooser.showOpenDialog(primaryStage))
				.ifPresent(file -> {
					System.out.println("open");

					log = new JsonSerializer().read(file);
					updateWorkTimeOfDay();
					updateWorkTimeOfWeek();
				});
	}

	private File getDefaultDirectory() {
		return new File(System.getProperty("user.home"));
	}
}
