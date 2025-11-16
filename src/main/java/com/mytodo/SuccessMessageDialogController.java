package com.mytodo;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import java.net.URL;

// 1. Class renamed
public class SuccessMessageDialogController {

    @FXML private DialogPane alertPane;
    @FXML private Label headerLabel;
    @FXML private Label contentLabel;
    @FXML private HBox separatorBox;
    @FXML private Button okButton;

    /**
     * This method is called automatically after FXML is loaded
     */
    @FXML
    public void initialize() {

        // 1. Load CSS
        try {
            URL cssUrl = getClass().getResource("/com/mytodo/Main.css");
            if (cssUrl != null) {
                alertPane.getStylesheets().add(cssUrl.toExternalForm());
            } else {
                System.err.println("FATAL (Alert): Main.css not found!");
            }
        } catch (Exception e) {
            System.err.println("Error loading CSS in SuccessMessageDialogController: " + e.getMessage());
            e.printStackTrace();
        }

        // 2. Set click handler for the "OK" button
        okButton.setOnAction(event -> closeDialog());
    }

    /**
     * Method to manually close the dialog
     */
    private void closeDialog() {
        Stage stage = (Stage) alertPane.getScene().getWindow();
        stage.close();
    }

    /**
     * Set the header and content for the success dialog
     */
    public void setSuccessMessage(String header, String content) {
        if (header != null && !header.isEmpty()) {
            headerLabel.setText(header);
            headerLabel.setVisible(true);
            separatorBox.setVisible(true);
        } else {
            headerLabel.setVisible(false);
            separatorBox.setVisible(false);
        }
        contentLabel.setText(content);
    }
}