package de.wt.fx;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class Main extends Application {

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
				"/main.fxml"));
		controller = new Controller(primaryStage);
		fxmlLoader.setController(controller);
		Parent root = fxmlLoader.load();
		
		Scene scene = new Scene(root);

		primaryStage.setTitle("Working Time");
		primaryStage.setScene(scene);
		primaryStage.centerOnScreen();
		primaryStage.show();
	}

}
