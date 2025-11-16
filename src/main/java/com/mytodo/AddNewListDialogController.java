package com.mytodo;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.List;

/**
 * Add New List Dialog Controller:
 *  - Input list name
 *  - Select an icon (circle buttons)
 */
public class AddNewListDialogController {

    @FXML private DialogPane dialogPane;
    @FXML private TextField listNameField;
    @FXML private Button okButton;
    @FXML private Button cancelButton;

    // Five icon buttons
    @FXML private Button iconBtn1;
    @FXML private Button iconBtn2;
    @FXML private Button iconBtn3;
    @FXML private Button iconBtn4;
    @FXML private Button iconBtn5;

    private boolean okClicked = false;
    private String newListName = null;

    // Default selected icon path (resource path)
    private String selectedIconPath = "/com/mytodo/icons/user1.png";

    @FXML
    private void initialize() {
        // Enter key -> OK
        listNameField.setOnAction(event -> handleOk());

        // Setup icon button behaviors
        setupIconButtons();
    }

    private void setupIconButtons() {
        // Put all five buttons into a list at once
        List<Button> buttons = Arrays.asList(
                iconBtn1, iconBtn2, iconBtn3, iconBtn4, iconBtn5
        );

        String[] paths = new String[] {
                "/com/mytodo/icons/user1.png",
                "/com/mytodo/icons/user2.png",
                "/com/mytodo/icons/user3.png",
                "/com/mytodo/icons/user4.png",
                "/com/mytodo/icons/user5.png"
        };

        for (int i = 0; i < buttons.size(); i++) {
            final Button btn = buttons.get(i);
            final String path = paths[i];

            btn.setOnMouseClicked(e -> {
                selectedIconPath = path;

                // Remove selected style from all buttons
                buttons.forEach(b -> b.getStyleClass().remove("selected"));

                // Add selected style to the current button for CSS
                if (!btn.getStyleClass().contains("selected")) {
                    btn.getStyleClass().add("selected");
                }
            });
        }

        // Highlight the first icon by default
        if (!iconBtn1.getStyleClass().contains("selected")) {
            iconBtn1.getStyleClass().add("selected");
        }
    }

    @FXML
    private void handleOk() {
        String name = listNameField.getText();
        if (name != null && !name.isBlank()) {
            this.newListName = name.trim();
            this.okClicked = true;
            closeDialog();
        } else {
            System.err.println("List name cannot be empty");
        }
    }

    @FXML
    private void handleCancel() {
        this.okClicked = false;
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) dialogPane.getScene().getWindow();
        stage.close();
    }

    // --- Methods for MainController to call ---

    public boolean isOkClicked() {
        return okClicked;
    }

    public String getNewListName() {
        return newListName;
    }

    public String getSelectedIconPath() {
        return selectedIconPath;
    }
}