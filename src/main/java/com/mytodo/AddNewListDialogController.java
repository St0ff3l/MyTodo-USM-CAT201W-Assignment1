package com.mytodo;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.List;

/**
 * 新建列表对话框控制器：
 *  - 输入列表名
 *  - 选择一个图标（圆形按钮）
 */
public class AddNewListDialogController {

    @FXML private DialogPane dialogPane;
    @FXML private TextField listNameField;
    @FXML private Button okButton;
    @FXML private Button cancelButton;

    // 五个图标按钮
    @FXML private Button iconBtn1;
    @FXML private Button iconBtn2;
    @FXML private Button iconBtn3;
    @FXML private Button iconBtn4;
    @FXML private Button iconBtn5;

    private boolean okClicked = false;
    private String newListName = null;

    // 默认选中的图标路径（资源路径）
    private String selectedIconPath = "/com/mytodo/icons/user1.png";

    @FXML
    private void initialize() {
        // 回车 -> OK
        listNameField.setOnAction(event -> handleOk());

        // 设置图标按钮行为
        setupIconButtons();
    }

    private void setupIconButtons() {
        // 一次性把 5 个按钮放进列表
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
                // 清掉所有按钮的 selected 样式
                buttons.forEach(b -> b.getStyleClass().remove("selected"));
                // 当前按钮加上 selected，给 CSS 用
                if (!btn.getStyleClass().contains("selected")) {
                    btn.getStyleClass().add("selected");
                }
            });
        }

        // 默认第一个高亮
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

    // --- 给 MainController 调用的接口 ---

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