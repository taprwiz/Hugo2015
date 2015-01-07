package demos.components;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import com.cctintl.c3dfx.controls.C3DButton;

public class ButtonDemo extends Application {

	
	@Override public void start(Stage stage) {

		FlowPane main = new FlowPane();
		main.setVgap(20);
		main.setHgap(20);
		
		main.getChildren().add(new Button("Java Button"));
		main.getChildren().add(new C3DButton("C3D Button"));
		
		C3DButton button = new C3DButton("Raised Button".toUpperCase());
		button.getStyleClass().add("c3dbutton-raised");
		main.getChildren().add(button);
		
		C3DButton button1 = new C3DButton("DISABLED");
		button1.setDisable(true);
		main.getChildren().add(button1);
		
		StackPane pane = new StackPane();
		pane.getChildren().add(main);
		StackPane.setMargin(main, new Insets(100));
		pane.setStyle("-fx-background-color:WHITE");
		
		final Scene scene = new Scene(pane, 600, 400);
		scene.getStylesheets().add(InputDemo.class.getResource("css/styles.css").toExternalForm());
		stage.setTitle("JavaFX Ripple effect and shadows ");
		stage.setScene(scene);
		stage.setResizable(false);
		stage.show();

	}

	public static void main(String[] args) { launch(args); }
	
}
