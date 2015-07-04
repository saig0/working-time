package de.wt.fx;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
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

	@FXML
	private TableView<WorkLogEntry> workLogTable;

	@FXML
	private TableColumn<WorkLogEntry, String> startTimeColumn;

	@FXML
	private TableColumn<WorkLogEntry, String> endTimeColumn;

	@FXML
	private TableColumn<WorkLogEntry, String> durationColumn;

	@FXML
	private TableColumn<WorkLogEntry, String> dateColumn;

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

	private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter
			.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.GERMAN)
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

				dateColumn.setCellValueFactory(cellData -> getDateAsProperty(cellData
				.getValue().getStartTime()));

		startTimeColumn
				.setCellValueFactory(cellData -> getTimeAsProperty(cellData
						.getValue().getStartTime()));
		endTimeColumn.setCellValueFactory(cellData -> {
			return Optional.ofNullable(cellData.getValue().getEndTime())
					.map(endTime -> getTimeAsProperty(endTime))
					.orElse(new SimpleStringProperty(""));
		});

		durationColumn.setCellValueFactory(cellData -> {
			String formattedWorkTime = Optional
					.ofNullable(cellData.getValue().getEndTime())
					.map(endTime -> getFormattedWorkTime(Duration.between(
							cellData.getValue().getStartTime(), endTime)))
					.orElse("");
			return new SimpleStringProperty(formattedWorkTime);
		});

		update();
	}

	private SimpleStringProperty getTimeAsProperty(Instant instant) {
		LocalDateTime localDateTime = LocalDateTime.ofInstant(instant,
				ZoneId.systemDefault());
		String formattedDateTime = timeFormatter.format(localDateTime);
		return new SimpleStringProperty(formattedDateTime);
	}

	private SimpleStringProperty getDateAsProperty(Instant instant) {
		LocalDateTime localDateTime = LocalDateTime.ofInstant(instant,
				ZoneId.systemDefault());
		String formattedDateTime = dateFormatter.format(localDateTime);
		return new SimpleStringProperty(formattedDateTime);
	}

	private void update() {
		updateWorkTimeOfDay();
		updateWorkTimeOfWeek();
		
		workLogTable.getItems().clear();
		workLogTable.getItems().addAll(log.getWorkLogEntries());
	}

	private void updateWorkTimeOfWeek() {
		// DRY
		workedTimeInWeek = Duration.ZERO;
		log.getWorkLogEntriesForWeek()
				.stream()
				.filter(log -> log.getEndTime() != null)
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
		log.getWorkLogEntryForToday()
				.filter(log -> log.getEndTime() != null)
				.ifPresent(
						logEntry -> {
							workedTime = Duration.between(
									logEntry.getStartTime(),
									logEntry.getEndTime());

							workedTimeOfDay.set(Double.valueOf(workedTime
									.toMinutes())
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

		update();
	}

	@FXML
	public void stop(ActionEvent event) {
		System.out.println("stop");
		running = false;

		WorkLogEntry logEntry = getLogEntry();
		logEntry.setEndTime(Instant.now());

		update();
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
	public void save() {
		System.out.println("save");

		File file = new File(getDefaultDirectory(), "workingLog.json");
		new JsonSerializer().write(log, file);
	}

	@FXML
	public void saveAs() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Working Log speichern");
		fileChooser.getExtensionFilters().add(
				new ExtensionFilter("JSON", "*.json"));
		fileChooser.setInitialDirectory(getDefaultDirectory());

		Optional.ofNullable(fileChooser.showSaveDialog(primaryStage))
				.ifPresent(file -> {
					System.out.println("save");

					new JsonSerializer().write(log, file);
				});
	}

	@FXML
	public void newLog() {
		log = new WorkingLog();

		update();
	}

	@FXML
	public void close() {
		Platform.exit();
	}

	@FXML
	public void open() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Working Log öffnen");
		fileChooser.getExtensionFilters().add(
				new ExtensionFilter("JSON", "*.json"));
		fileChooser.setInitialDirectory(getDefaultDirectory());

		Optional.ofNullable(fileChooser.showOpenDialog(primaryStage))
				.ifPresent(file -> {
					System.out.println("open");

					log = new JsonSerializer().read(file);

					update();
				});
	}

	private File getDefaultDirectory() {
		return new File(System.getProperty("user.home"));
	}
}
