package ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class CustomDialog {

    public static void showMessage(String titleText, String messageText, boolean isError) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UNDECORATED); // Clean, borderless look

        VBox layout = createBaseLayout(titleText, messageText, isError ? "#ff4d4d" : "#004aad");

        Button btnClose = new Button("OK");
        btnClose.setStyle("-fx-background-color: " + (isError ? "#ff4d4d" : "linear-gradient(to right, #004aad, #cb6ce6)") + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 30; -fx-cursor: hand;");
        btnClose.setOnAction(e -> stage.close());

        layout.getChildren().add(btnClose);
        
        Scene scene = new Scene(layout);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.showAndWait();
    }

    public static boolean showConfirmation(String titleText, String messageText) {
        final boolean[] result = {false};
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UNDECORATED);

        VBox layout = createBaseLayout(titleText, messageText, "#cb6ce6");

        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        Button btnCancel = new Button("Cancel");
        btnCancel.setStyle("-fx-background-color: #e5e7eb; -fx-text-fill: #6b7280; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20; -fx-cursor: hand;");
        btnCancel.setOnAction(e -> stage.close());

        Button btnConfirm = new Button("Confirm");
        btnConfirm.setStyle("-fx-background-color: linear-gradient(to right, #004aad, #cb6ce6); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20; -fx-cursor: hand;");
        btnConfirm.setOnAction(e -> {
            result[0] = true;
            stage.close();
        });

        buttonBox.getChildren().addAll(btnCancel, btnConfirm);
        layout.getChildren().add(buttonBox);

        Scene scene = new Scene(layout);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.showAndWait();

        return result[0];
    }

    private static VBox createBaseLayout(String titleText, String messageText, String titleColor) {
        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(30));
        layout.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-border-color: #d1d5db; -fx-border-radius: 15; -fx-border-width: 2;");
        layout.setPrefWidth(350);

        Text title = new Text(titleText);
        title.setFont(Font.font("Poppins", FontWeight.BOLD, 22));
        title.setFill(Color.web(titleColor));

        Label message = new Label(messageText);
        message.setWrapText(true);
        message.setStyle("-fx-font-family: 'ITC Avant Garde Gothic', sans-serif; -fx-text-fill: #555555; -fx-font-size: 14px;");
        message.setAlignment(Pos.CENTER);
        message.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        layout.getChildren().addAll(title, message);
        return layout;
    }
}