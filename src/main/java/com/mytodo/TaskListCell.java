package com.mytodo;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.time.format.DateTimeFormatter;
import com.mytodo.MainController; // <-- 关键修正：导入 MainController 解决构造函数错误

// ListCell<Task> 必须能够访问 Task 和 MainController，因此需要这两个 import
// import com.mytodo.Task; // 假设 Task 类与 TaskListCell 在同一包下，不需要显式导入

public class TaskListCell extends ListCell<Task> {

    private final HBox rootLayout = new HBox(10);
    private final CheckBox completedCheckbox = new CheckBox();
    private final Label titleLabel = new Label();
    private final Label detailLabel = new Label();
    private final VBox textStack = new VBox(2, titleLabel, detailLabel);
    private final MainController controller;

    // 时间格式化工具
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public TaskListCell(MainController controller) {
        this.controller = controller;

        rootLayout.setAlignment(Pos.CENTER_LEFT);
        VBox.setVgrow(textStack, Priority.ALWAYS);
        HBox.setHgrow(textStack, Priority.ALWAYS);

        // --- 交互绑定: 完成状态切换 ---
        completedCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            Task task = getItem();
            if (task != null && task.isCompleted() != newVal) {
                // 回调主控制器，处理数据更新、保存和 applyFilters
                controller.toggleCompletion(task);
            }
        });

        // --- 操作按钮: 编辑和删除 ---
        Button editBtn = new Button("编辑");
        editBtn.getStyleClass().add("flat-ghost");
        editBtn.setOnAction(e -> controller.openTaskDetailDialog(getItem()));

        Button deleteBtn = new Button("删除");
        deleteBtn.getStyleClass().add("flat-ghost");
        deleteBtn.setOnAction(e -> controller.deleteTask(getItem()));

        HBox actionBox = new HBox(5, editBtn, deleteBtn);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        rootLayout.getChildren().addAll(completedCheckbox, textStack, actionBox);
        HBox.setHgrow(actionBox, Priority.ALWAYS);

        // 初始样式设置
        titleLabel.setFont(Font.font("System", FontWeight.NORMAL, 16));
        detailLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 11px;");
        rootLayout.setStyle("-fx-padding: 10px 15px; -fx-background-color: #ffffff; -fx-background-radius: 8;");
    }

    @Override
    protected void updateItem(Task task, boolean empty) {
        super.updateItem(task, empty);

        if (empty || task == null) {
            setGraphic(null);
            setStyle(null);
        } else {
            titleLabel.setText(task.getTitle());

            // --- 详细信息显示日期和时间 (您的修改) ---
            String timeStr = task.getTime() != null ? task.getTime().format(TIME_FORMATTER) : "无时间";
            detailLabel.setText("截止: " + task.getDueDate() + " " + timeStr + " | 优先级: " + task.getPriority());

            completedCheckbox.setSelected(task.isCompleted());

            // --- 划线效果 (已修正为纯 CSS 属性) ---
            if (task.isCompleted()) {
                titleLabel.setStyle("-fx-text-fill: gray; -fx-strikethrough: true;");
                detailLabel.setStyle("-fx-text-fill: gray; -fx-strikethrough: true;");
                rootLayout.getStyleClass().add("task-completed-cell");
            } else {
                titleLabel.setStyle("-fx-text-fill: black; -fx-strikethrough: false;");
                detailLabel.setStyle("-fx-text-fill: gray; -fx-strikethrough: false;");
                rootLayout.getStyleClass().remove("task-completed-cell");
            }

            setGraphic(rootLayout);
            setStyle("-fx-padding: 4px 0; -fx-background-color: transparent;");
        }
    }
}