package com.mytodo;

import javafx.fxml.FXML;
import javafx.scene.control.Button;       // 🌟 1. 新增 Import
import javafx.scene.control.ButtonType;   // 🌟 2. 新增 Import
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;             // 🌟 3. 新增 Import
import java.net.URL;

public class CustomAlertController {

    @FXML private DialogPane alertPane;
    @FXML private Label headerLabel;
    @FXML private Label contentLabel;
    @FXML private HBox separatorBox;

    // 🌟 4. 为 FXML 中的新按钮添加 @FXML 引用
    @FXML private Button okButton;
    @FXML private Button cancelButton;

    // 🌟 5. 用于存储按钮点击结果 (默认为 Cancel)
    private ButtonType result = ButtonType.CANCEL;

    /**
     * FXML 加载后自动调用此方法
     */
    @FXML
    public void initialize() {

        // 1. 为这个弹窗(alertPane)加载 CSS
        try {
            // 使用绝对路径加载你的 CSS 文件
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

        // 🌟 6. 为我们的新按钮设置点击事件

        // "OK" 按钮
        okButton.setOnAction(event -> {
            this.result = ButtonType.OK; // 设置结果为 OK
            closeDialog();
        });

        // "Cancel" 按钮
        cancelButton.setOnAction(event -> {
            // 结果默认为 Cancel，所以这里也可以不设置
            // this.result = ButtonType.CANCEL;
            closeDialog();
        });
    }

    /**
     * 🌟 7. 新增：手动关闭弹窗的方法
     */
    private void closeDialog() {
        // 获取当前按钮所在的 Stage (窗口) 并关闭它
        Stage stage = (Stage) alertPane.getScene().getWindow();
        stage.close();
    }

    /**
     * 🌟 8. 新增：一个公共方法，让调用者(MainController)可以获取结果
     */
    public ButtonType getResult() {
        return this.result;
    }

    /**
     * 设置弹窗的内容和标题
     * (此方法保持不变)
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
                // 注意：FXML 的 VBox padding 现在是 20 20 20 20
                // 你可能不再需要这行代码了
                // alertPane.getContent().setStyle("-fx-padding: 20 20 10 20;");
            }
        }
        contentLabel.setText(content);
    }
}