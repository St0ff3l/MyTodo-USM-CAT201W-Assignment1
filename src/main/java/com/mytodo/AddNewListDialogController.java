package com.mytodo;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * 🌟 2. 这是新弹窗的 Controller
 */
public class AddNewListDialogController {

    @FXML private DialogPane dialogPane;
    @FXML private TextField listNameField;
    @FXML private Button okButton;
    @FXML private Button cancelButton;

    private boolean okClicked = false;
    private String newListName = null;

    @FXML
    private void initialize() {
        // 让"OK"按钮在按回车键时触发
        listNameField.setOnAction(event -> handleOk());
    }

    @FXML
    private void handleOk() {
        String name = listNameField.getText();
        if (name != null && !name.isBlank()) {
            this.newListName = name.trim();
            this.okClicked = true;
            closeDialog();
        } else {
            // (你可以在这里加一个红色边框或提示)
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

    //--- 供 MainController 调用的公共方法 ---

    public boolean isOkClicked() {
        return okClicked;
    }

    public String getNewListName() {
        return newListName;
    }
}