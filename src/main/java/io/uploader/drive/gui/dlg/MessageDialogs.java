package io.uploader.drive.gui.dlg;

import org.apache.commons.lang3.StringUtils;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

// http://stackoverflow.com/questions/8309981/how-to-create-and-show-common-dialog-error-warning-confirmation-in-javafx-2

public class MessageDialogs {
	
	private MessageDialogs () { super () ; throw new IllegalStateException () ; } ;
	
	public enum Response {
		NO, YES, CANCEL
	};
	
	public enum MessageType {
		NONE, INFO, WARNING, CONFIRM, ERROR
	};

	private static Response buttonSelected = Response.CANCEL;

	private static ImageView icon = new ImageView();

	private final static String logo = "/images/DriveUploader32.png";

	
	static class Dialog extends Stage {
			
		public Dialog(String title, Stage owner, Scene scene, String iconFileName) {
			super () ;
			setTitle(title);
			initStyle(StageStyle.UTILITY);
			initModality(Modality.WINDOW_MODAL);
			initOwner(owner);
			setResizable(false);
			setScene(scene);
			if (StringUtils.isEmpty(iconFileName)) {
				icon.setImage(new Image(getClass().getResourceAsStream(logo)));
			} else {
				icon.setImage(new Image(getClass().getResourceAsStream(iconFileName)));
			}
		}

		public void showDialog() {
			sizeToScene();
			showAndWait();
		}
	}

	
	static class Message extends Text {
		public Message(String msg) {
			super(msg);
			setWrappingWidth(250);
		}
	}

	
	public static Response showConfirmDialog(Stage owner, String message,
			String title) {
		return showConfirmDialog (owner, message, title, MessageType.CONFIRM) ;
	}
	
	
	private static String getIconName (MessageType type) {
		String iconName = null ;
		switch (type) {
		case ERROR:
			iconName = "/icons/Error32.png" ;
			break;
		case INFO:
			iconName = "/icons/Info32.png" ;
			break;
		case WARNING:
			iconName = "/icons/Warning32.png" ;
			break;
		case CONFIRM:
			iconName = "/icons/Confirm32.png" ;
			break;
		default:
			break;
		}
		return iconName ;
	}
	
	
	public static Response showConfirmDialog(Stage owner, String message,
			String title, MessageType type) {
		VBox vb = new VBox();
		Scene scene = new Scene(vb);
		final Dialog dial = new Dialog(title, owner, scene, getIconName (type));
		vb.setPadding(new Insets(10, 10, 10, 10));
		vb.setSpacing(10);
		Button yesButton = new Button("Yes");
		yesButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				dial.close();
				buttonSelected = Response.YES;
			}
		});
		Button noButton = new Button("No");
		noButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				dial.close();
				buttonSelected = Response.NO;
			}
		});
		BorderPane bp = new BorderPane();
		HBox buttons = new HBox();
		buttons.setAlignment(Pos.CENTER);
		buttons.setSpacing(10);
		buttons.getChildren().addAll(yesButton, noButton);
		bp.setCenter(buttons);
		HBox msg = new HBox();
		msg.setSpacing(15);
		msg.getChildren().addAll(icon, new Message(message));
		vb.getChildren().addAll(msg, bp);
		dial.showDialog();
		return buttonSelected;
	}

	
	public static void showMessageDialog(Stage owner, String message,
			String title) {
		showMessageDialog(owner, new Message(message), title, MessageType.NONE);
	}
	
	
	public static void showMessageDialog(Stage owner, String message,
			String title, MessageType type) {
		showMessageDialog(owner, new Message(message), title, type);
	}
	
	
	public static void showMessageDialog(Stage owner, Node message, String title, MessageType type) {
		VBox vb = new VBox();
		Scene scene = new Scene(vb);
		final Dialog dial = new Dialog(title, owner, scene, getIconName (type));
		vb.setPadding(new Insets(10, 10, 10, 10));
		vb.setSpacing(10);
		Button okButton = new Button("OK");
		okButton.setAlignment(Pos.CENTER);
		okButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				dial.close();
			}
		});
		BorderPane bp = new BorderPane();
		bp.setCenter(okButton);
		HBox msg = new HBox();
		msg.setSpacing(5);
		msg.getChildren().addAll(icon, message);
		vb.getChildren().addAll(msg, bp);
		dial.showDialog();
	}
}
