package com.mytodo;

import javafx.fxml.FXML;
import javafx.scene.control.Button;       // 1. Added Import
import javafx.scene.control.ButtonType;   // 2. Added Import
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;             // 3. Added Import
import java.net.URL;

public class CustomAlertController {

    @FXML private DialogPane alertPane;
    @FXML private Label headerLabel;
    @FXML private Label contentLabel;
    @FXML private HBox separatorBox;

    // 4. Add @FXML references for the new buttons inside FXML
    @FXML private Button okButton;
    @FXML private Button cancelButton;

    // 5. Store the button click result (default = Cancel)
    private ButtonType result = ButtonType.CANCEL;

    /**
     * Called automatically after FXML is loaded
     */
    @FXML
    public void initialize() {

        // 1. Load CSS for this alertPane
        try {
            // Load CSS using an absolute resource path
            URL cssUrl = getClass().getResource("/com/mytodo/Main.css");

            if (cssUrl != null) {
                alertPane.getStylesheets().add(cssUrl.toExternalForm());
            } else {
                System.err.println("FATAL (Alert): Main.css not found!");
            }
        } catch (Exception e) {
            System.err.println("Error loading CSS in CustomAlertController: " + e.getMessage());
            e.printStackTrace();
        }

        // 6. Set click handlers for our new buttons

        // "OK" button
        okButton.setOnAction(event -> {
            this.result = ButtonType.OK; // Set result to OK
            closeDialog();
        });

        // "Cancel" button
        cancelButton.setOnAction(event -> {
            // Result is already defaulted to Cancel, so no need to set it
            // this.result = ButtonType.CANCEL;
            closeDialog();
        });
    }

    /**
     * 7. Method to manually close the dialog
     */
    private void closeDialog() {
        // Get the current Stage (window) from the dialogPane and close it
        Stage stage = (Stage) alertPane.getScene().getWindow();
        stage.close();
    }

    /**
     * 8. Public method for MainController to retrieve the clicked result
     */
    public ButtonType getResult() {
        return this.result;
    }

    /**
     * Set the alert header and content message
     */
    public void setMessage(String header, String content) {
        if (header != null && !header.isEmpty()) {
            headerLabel.setText(header);
            headerLabel.setVisible(true);
            separatorBox.setVisible(true);
        } else {
            headerLabel.setVisible(false);
            separatorBox.setVisible(false);
            if (alertPane.getContent() != null) {
                // Note: FXML VBox padding is now 20 20 20 20
                // You may not need this line anymore
                // alertPane.getContent().setStyle("-fx-padding: 20 20 10 20;");
            }
        }
        contentLabel.setText(content);
    }
}