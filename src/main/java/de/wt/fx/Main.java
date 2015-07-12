package de.wt.fx;

import de.wt.view.Controller;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class Main extends Application {

	private static final String MAIN_FXML_FILE = "/main.fxml";
	private static final String CSS_FILE = "/default.css";

	private Controller controller;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void stop() throws Exception {
		controller.onShutDown();
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(
				MAIN_FXML_FILE));
		controller = new Controller(primaryStage);
		fxmlLoader.setController(controller);
		Parent root = fxmlLoader.load();

		Scene scene = new Scene(root);
		scene.getStylesheets().add(
				getClass().getResource(CSS_FILE).toExternalForm());

		primaryStage.setTitle("Working Time");
		primaryStage.setScene(scene);
		primaryStage.centerOnScreen();
		primaryStage.show();
	}

}
