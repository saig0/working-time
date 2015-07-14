package de.wt.view;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Optional;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import de.wt.model.WorkLogEntry;
import de.wt.viewmodell.ViewModel;

public class Controller {

	@FXML
	private CheckMenuItem automaticSaveField;

	@FXML
	private TableColumn<WorkLogEntry, String> dateColumn;

	@FXML
	private Label dateField;

	private final DateTimeFormatter dateFormatter = DateTimeFormatter
			.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.GERMAN)
			.withZone(ZoneId.systemDefault());

	@FXML
	private Label dayField;

	@FXML
	private TableColumn<WorkLogEntry, String> durationColumn;

	@FXML
	private TableColumn<WorkLogEntry, String> endTimeColumn;

	private final Stage primaryStage;
	@FXML
	private ProgressBar progressDay;

	@FXML
	private ProgressBar progressWeek;

	@FXML
	private Menu recentlyOpenedFileMenu;
	@FXML
	private ToggleButton startButton;

	@FXML
	private TableColumn<WorkLogEntry, String> startTimeColumn;
	@FXML
	private ToggleButton stopButton;

	@FXML
	private Label timeField;

	private final DateTimeFormatter timeFormatter = DateTimeFormatter
			.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.GERMAN)
			.withZone(ZoneId.systemDefault());

	private final ViewModel viewModel;

	@FXML
	private Label weekField;

	@FXML
	private TableView<WorkLogEntry> workLogTable;

	public Controller(Stage primaryStage) {
		this.primaryStage = primaryStage;

		this.viewModel = new ViewModel();
	}

	@FXML
	public void close() {
		Platform.exit();
	}

	@FXML
	public void newLog() {
		viewModel.createNewWorkingLog();
	}

	public void onShutDown() {
		viewModel.cleanup();
	}

	@FXML
	public void open() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Working Log öffnen");
		fileChooser.getExtensionFilters().add(
				new ExtensionFilter("JSON", "*.json"));
		fileChooser.setInitialDirectory(viewModel.getDefaultLogFileLocation());

		Optional.ofNullable(fileChooser.showOpenDialog(primaryStage))
				.ifPresent(this::openLogFile);
	}

	@FXML
	public void save() {
		if (viewModel.getLastOpenedLogFile().exists()) {
			viewModel.saveLog(viewModel.getLastOpenedLogFile());
		} else {
			saveAs();
		}
	}

	@FXML
	public void saveAs() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Working Log speichern");
		fileChooser.getExtensionFilters().add(
				new ExtensionFilter("JSON", "*.json"));
		fileChooser.setInitialDirectory(viewModel.getDefaultLogFileLocation());

		Optional.ofNullable(fileChooser.showSaveDialog(primaryStage))
				.ifPresent(file -> {
					viewModel.saveLog(file);
				});
	}

	@FXML
	public void start(ActionEvent event) {
		if (!viewModel.runningProperty().get()) {

			viewModel.start();

			onChangedWorkingLog();
		}
	}

	@FXML
	public void stop(ActionEvent event) {
		if (viewModel.runningProperty().get()) {

			viewModel.stop();

			onChangedWorkingLog();
		}
	}

	private MenuItem createMenuItemForRecentlyOpenedFile(File file) {
		MenuItem menuItem = new MenuItem(file.toString());
		menuItem.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent arg0) {
				openLogFile(file);
			}
		});
		return menuItem;
	}

	private SimpleStringProperty getDateAsProperty(Instant instant) {
		String formattedDateTime = getFormattedDate(instant);
		return new SimpleStringProperty(formattedDateTime);
	}

	private String getFormattedDate(Instant instant) {
		LocalDateTime localDateTime = LocalDateTime.ofInstant(instant,
				ZoneId.systemDefault());
		String formattedDateTime = dateFormatter.format(localDateTime);
		return formattedDateTime;
	}

	private String getFormattedTime(Instant instant) {
		LocalDateTime localDateTime = LocalDateTime.ofInstant(instant,
				ZoneId.systemDefault());
		String formattedDateTime = timeFormatter.format(localDateTime);
		return formattedDateTime;
	}

	private String getFormattedWorkTime(Duration workedTime) {
		return String.format("%02d:%02d", workedTime.toHours(),
				workedTime.toMinutes() % 60);
	}

	private SimpleStringProperty getTimeAsProperty(Instant instant) {
		String formattedDateTime = getFormattedTime(instant);
		return new SimpleStringProperty(formattedDateTime);
	}

	@FXML
	private void initialize() {
		timeField.textProperty().bind(
				Bindings.createStringBinding(() -> getFormattedTime(viewModel
						.currentTimeProperty().get()), viewModel
						.currentTimeProperty()));

		dateField.textProperty().bind(
				Bindings.createObjectBinding(() -> getFormattedDate(viewModel
						.currentDateProperty().get()), viewModel
						.currentDateProperty()));

		dayField.textProperty().bind(
				Bindings.createStringBinding(
						() -> getFormattedWorkTime(viewModel
								.workedTimeDayProperty().get()), viewModel
								.workedTimeDayProperty()));

		weekField.textProperty().bind(
				Bindings.createStringBinding(
						() -> getFormattedWorkTime(viewModel
								.workedTimeWeekProperty().get()), viewModel
								.workedTimeWeekProperty()));

		progressDay.progressProperty()
				.bind(viewModel.workedTimeOfDayProperty());
		progressWeek.progressProperty().bind(
				viewModel.workedTimeOfWeekProperty());

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

		viewModel.workLogEntryProperty().addListener(
				new ListChangeListener<WorkLogEntry>() {

					@Override
					public void onChanged(
							javafx.collections.ListChangeListener.Change<? extends WorkLogEntry> change) {

						workLogTable.getItems().clear();
						workLogTable.getItems().addAll(change.getList());

					}
				});

		automaticSaveField.selectedProperty().bindBidirectional(
				viewModel.automaticSaveProperty());

		viewModel.runningProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable,
					Boolean oldValue, Boolean newValue) {
				startButton.setSelected(newValue);
				stopButton.setSelected(!newValue);

				if (newValue) {
					progressDay.getStyleClass().add("active");
					progressWeek.getStyleClass().add("active");
				} else {
					progressDay.getStyleClass().remove("active");
					progressWeek.getStyleClass().remove("active");
				}
			}
		});

		viewModel.recentlyOpenedLogFilesProperty().addListener(
				new ListChangeListener<File>() {

					@Override
					public void onChanged(
							javafx.collections.ListChangeListener.Change<? extends File> change) {

						recentlyOpenedFileMenu.getItems().clear();
						change.getList()
								.forEach(
										file -> recentlyOpenedFileMenu
												.getItems()
												.add(createMenuItemForRecentlyOpenedFile(file)));
					}
				});

		viewModel.init();
	}

	private void onChangedWorkingLog() {
		if (viewModel.automaticSaveProperty().get()) {
			save();
		}
	}

	private void openLogFile(File file) {
		viewModel.openLog(file);
	}

}
