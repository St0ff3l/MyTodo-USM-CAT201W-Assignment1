package com.mytodo;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import java.net.URL;

// 🌟 1. 类重命名
public class SuccessMessageDialogController {

    @FXML private DialogPane alertPane;
    @FXML private Label headerLabel;
    @FXML private Label contentLabel;
    @FXML private HBox separatorBox;
    @FXML private Button okButton;

    /**
     * FXML 加载后自动调用此方法
     */
    @FXML
    public void initialize() {

        // 1. 加载 CSS
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

        // 2. 为 "OK" 按钮设置点击事件
        okButton.setOnAction(event -> closeDialog());
    }

    /**
     * 手动关闭弹窗的方法
     */
    private void closeDialog() {
        Stage stage = (Stage) alertPane.getScene().getWindow();
        stage.close();
    }

    /**
     * 设置成功弹窗的内容和标题
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