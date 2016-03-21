package demos.gui.uicomponents;

import javax.annotation.PostConstruct;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialog;
import com.jfoenix.controls.JFXDialog.DialogTransition;

import io.datafx.controller.FXMLController;
import io.datafx.controller.flow.FlowException;
import io.datafx.controller.flow.context.FXMLViewFlowContext;
import io.datafx.controller.flow.context.ViewFlowContext;
import io.datafx.controller.util.VetoException;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

@FXMLController(value = "/resources/fxml/ui/Dialog.fxml" , title = "Material Design Example")
public class DialogController {

	@FXMLViewFlowContext
	private ViewFlowContext context;

	@FXML private JFXButton centerButton;

	@FXML private JFXButton topButton;

	@FXML private JFXButton rightButton;

	@FXML private JFXButton bottomButton;

	@FXML private JFXButton leftButton;

	@FXML private JFXButton acceptButton;

	@FXML
	private JFXDialog dialog;

	@PostConstruct
	public void init() throws FlowException, VetoException {
		if(((Pane) context.getRegisteredObject("ContentPane")).getChildren().size() > 0)
			Platform.runLater(()-> ((Pane)((Pane) context.getRegisteredObject("ContentPane")).getChildren().get(0)).getChildren().remove(1));

		centerButton.setOnMouseClicked((e)->{
			dialog.setTransitionType(DialogTransition.CENTER);
			dialog.show((StackPane) context.getRegisteredObject("ContentPane"));
		});

		topButton.setOnMouseClicked((e)->{
			dialog.setTransitionType(DialogTransition.TOP);
			dialog.show((StackPane) context.getRegisteredObject("ContentPane"));
		});

		rightButton.setOnMouseClicked((e)->{
			dialog.setTransitionType(DialogTransition.RIGHT);
			dialog.show((StackPane) context.getRegisteredObject("ContentPane"));
		});

		bottomButton.setOnMouseClicked((e)->{
			dialog.setTransitionType(DialogTransition.BOTTOM);
			dialog.show((StackPane) context.getRegisteredObject("ContentPane"));
		});

		leftButton.setOnMouseClicked((e)->{
			dialog.setTransitionType(DialogTransition.LEFT);
			dialog.show((StackPane) context.getRegisteredObject("ContentPane"));
		});

		acceptButton.setOnMouseClicked((e)->{
			dialog.close();
		});
	}

}
