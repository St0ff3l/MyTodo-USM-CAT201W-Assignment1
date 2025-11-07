package com.mytodo; // <-- 正确的包声明

// --- 错误行已删除或修正 ---
// import com.example._201assignment12.model.Task; <-- 错误！已删除

import com.mytodo.Task; // <-- CORRECT: 导入正确的 Task 类路径
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

// TaskCell 类（实现了自定义卡片 UI）
public class TaskCell extends ListCell<Task> {

    // ... (布局组件，与之前提供的骨架代码一致) ...
    private final HBox rootLayout = new HBox(10);
    private final CheckBox completedCheckbox = new CheckBox();
    private final Label titleLabel = new Label();
    private final Label detailLabel = new Label();
    private final VBox textStack = new VBox(2, titleLabel, detailLabel);
    private final ToggleButton importantToggle = new ToggleButton("⭐");

    public TaskCell() {
        // ... (布局初始化代码，与之前提供的骨架代码一致) ...
        rootLayout.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        VBox.setVgrow(textStack, Priority.ALWAYS);
        HBox.setHgrow(textStack, Priority.ALWAYS);

        // 绑定 CheckBox 状态变化事件 (核心交互逻辑)
        completedCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            Task task = getItem();
            if (task != null && task.isCompleted() != newVal) {
                // 1. 数据更新：设置任务状态
                task.setCompleted(newVal);

                // 2. 通知列表刷新：重要！
                // 必须通知 ListView 数据已更改，以便触发 applyFilters 和 UI 更新
                getListView().refresh();

                // 3. 实时保存数据 (Part 4 逻辑)
                // 在 TaskListCell 或 MainController 中处理，此处保持原样
            }
        });

        rootLayout.getChildren().addAll(completedCheckbox, textStack, importantToggle);

        titleLabel.setFont(Font.font(16));
        detailLabel.setStyle("-fx-text-fill: gray;");
        rootLayout.setStyle("-fx-padding: 8px 10px; -fx-background-color: white; -fx-border-radius: 5;");
    }

    @Override
    protected void updateItem(Task task, boolean empty) {
        super.updateItem(task, empty);

        if (empty || task == null) {
            setGraphic(null);
        } else {
            // 绑定数据
            titleLabel.setText(task.getTitle());
            // 注意：Task.java 中没有 getDueDate() 方法，如果您在 Task 类中使用的是 LocalDate，此处应能正常运行
            detailLabel.setText(task.getDueDate() + " · " + task.getPriority() + " 优先级");
            completedCheckbox.setSelected(task.isCompleted());

            // UI 划线效果 (根据 isCompleted 状态)
            if (task.isCompleted()) {
                titleLabel.setStyle("-fx-text-fill: gray; -fx-strikethrough: true;");
                detailLabel.setStyle("-fx-text-fill: gray; -fx-strikethrough: true;");
            } else {
                titleLabel.setStyle("-fx-text-fill: black; -fx-strikethrough: false;");
                detailLabel.setStyle("-fx-text-fill: gray; -fx-strikethrough: false;");
            }

            setGraphic(rootLayout);
            setText(null);
        }
    }
}