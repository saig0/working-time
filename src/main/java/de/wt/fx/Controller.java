package de.wt.fx;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Timer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

	private DoubleProperty workedTimeOfDay = new SimpleDoubleProperty(0);
	private DoubleProperty workedTimeOfWeek = new SimpleDoubleProperty(0);

	private Instant startTime;
	private Instant lastUpdated;

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
							// Duration duration = Duration.between(lastUpdated,
							// now);
							Duration duration = Duration.ofHours(1);

							workedTime = workedTime.plus(duration);

							workedTimeOfDay.set(Double.valueOf(workedTime
									.toMinutes())
									/ Duration.ofHours(8).toMinutes());
							workedTimeDay.set(getFormattedWorkTime());

							workedTimeOfWeek.set(Double.valueOf(workedTime
									.toMinutes())
									/ Duration.ofHours(40).toMinutes());
							workedTimeWeek.set(getFormattedWorkTime());

							lastUpdated = now;
						}

					}

					private String getFormattedWorkTime() {
						return String.format("%02d:%02d", workedTime.toHours(),
								workedTime.toMinutes() % 60);
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
	}

	@FXML
	public void start(ActionEvent event) {
		System.out.println("start");

		lastUpdated = Instant.now();

		running = true;

		startTime = Instant.now();
	}

	@FXML
	public void stop(ActionEvent event) {
		System.out.println("stop");
		running = false;
	}

	public void onShutDown() {
		executorService.shutdown();
	}
}
