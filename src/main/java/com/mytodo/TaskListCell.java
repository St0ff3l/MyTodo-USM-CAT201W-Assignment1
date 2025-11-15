package com.mytodo;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.time.format.DateTimeFormatter;

/**
 * TaskListCell - 最终修复版 (语义化命名)
 * (更新：应用了语义化命名后的 .btn-edit 和 .btn-delete 样式)
 */
public class TaskListCell extends ListCell<Task> {

    private final HBox rootLayout = new HBox(10);
    private final CheckBox completedCheckbox = new CheckBox();
    private final Text titleText = new Text();
    private final Label detailLabel = new Label();
    private final VBox textStack = new VBox(2, titleText, detailLabel);

    private final MainController controller;
    private boolean bindingDone = false;

    private static final double SIDE_MARGIN = 50;
    private static final double SPACER_HEIGHT = 100;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public TaskListCell(MainController controller) {
        this.controller = controller;

        // 加载 CSS (使用绝对路径)
        try {
            rootLayout.getStylesheets().add(
                    getClass().getResource("/com/mytodo/Main.css").toExternalForm()
            );
        } catch (Exception e) {
            System.err.println("Could not load stylesheet: Main.css");
            System.err.println("请确保 Main.css 文件在 src/main/resources/com/mytodo/ 目录下");
            e.printStackTrace();
        }

        completedCheckbox.setAllowIndeterminate(false);
        completedCheckbox.setStyle("-fx-mark-color: transparent;");
        completedCheckbox.setGraphic(null);

        rootLayout.setAlignment(Pos.CENTER_LEFT);
        rootLayout.setMaxWidth(Double.MAX_VALUE);
        this.setMaxWidth(Double.MAX_VALUE);

        HBox.setHgrow(textStack, Priority.ALWAYS);
        VBox.setVgrow(textStack, Priority.ALWAYS);

        // checkbox 切换完成状态
        completedCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            Task task = getItem();
            if (task != null && task.isCompleted() != newVal) {
                controller.toggleCompletion(task);
                Platform.runLater(() -> {
                    if (getListView() != null) getListView().refresh();
                });
            }
        });

        // --- 按钮样式修改 (使用语义化类名) ---
        Button editBtn = new Button("Edit");

        // 🌟 核心修改 1：应用 'btn-edit' 样式类 🌟
        editBtn.getStyleClass().add("btn-edit");

        editBtn.setOnAction(e -> {
            Task t = getItem();
            if (t != null) controller.openTaskDetailDialog(t);
        });

        Button deleteBtn = new Button("Delete");

        // 🌟 核心修改 2：应用 'btn-delete' 样式类 🌟
        deleteBtn.getStyleClass().add("btn-delete");

        deleteBtn.setOnAction(e -> {
            Task t = getItem();
            if (t != null) controller.deleteTask(t);
        });
        // --- 结束修改 ---

        HBox actionBox = new HBox(5, editBtn, deleteBtn);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        rootLayout.getChildren().addAll(completedCheckbox, textStack, actionBox);

        titleText.setFont(Font.font("System", FontWeight.NORMAL, 16));
        detailLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 11px;");

        rootLayout.setStyle("-fx-padding: 10px 15px 10px 15px; -fx-background-color: #ffffff; -fx-background-radius: 8;");

        this.setPrefWidth(Region.USE_COMPUTED_SIZE);
    }

    @Override
    protected void updateItem(Task task, boolean empty) {
        super.updateItem(task, empty);

        if (empty || task == null) {
            setGraphic(null);
            setText(null);
            return;
        }

        if ("(SPACER_ITEM)".equals(task.getTitle())) {
            Region spacer = new Region();
            spacer.setMinHeight(SPACER_HEIGHT);
            spacer.setPrefHeight(SPACER_HEIGHT);
            spacer.setMaxHeight(SPACER_HEIGHT);
            setGraphic(spacer);
            setText(null);
            setStyle("-fx-background-color: transparent;");
            return;
        }

        // 正常任务渲染
        String title = task.getTitle() == null ? "(No title)" : task.getTitle().trim();
        String desc = task.getDescription() == null ? "" : task.getDescription().trim();
        String combined = desc.isEmpty() ? title : title + " • " + desc;
        titleText.setText(combined);

        String dateStr = task.getDueDate() != null ? task.getDueDate().format(DATE_FORMATTER) : "No due date";
        String timeStr = (task.getTime() != null) ? task.getTime().format(TIME_FORMATTER) : "No time";
        String priority = task.getPriority() == null ? "Normal" : task.getPriority();
        detailLabel.setText("Due: " + dateStr + " " + timeStr + " | Priority: " + priority);

        completedCheckbox.setSelected(task.isCompleted());
        if (task.isCompleted()) {
            titleText.setStrikethrough(true);
            titleText.setStyle("-fx-fill: gray;");
            detailLabel.setStyle("-fx-text-fill: #8a8a8a; -fx-opacity: 0.9;");
        } else {
            titleText.setStrikethrough(false);
            titleText.setStyle("-fx-fill: black;");
            detailLabel.setStyle("-fx-text-fill: gray; -fx-opacity: 1.0;");
        }

        setGraphic(rootLayout);
        setStyle("-fx-padding: 4px 0; -fx-background-color: transparent;");

        // 宽度绑定（仅做一次）
        if (!bindingDone && getListView() != null) {
            Platform.runLater(() -> {
                try {
                    double totalMargin = SIDE_MARGIN * 2;
                    this.prefWidthProperty().bind(getListView().widthProperty().subtract(totalMargin));
                    rootLayout.prefWidthProperty().bind(getListView().widthProperty().subtract(totalMargin));
                    titleText.wrappingWidthProperty().bind(getListView().widthProperty().subtract(totalMargin + 200));
                } catch (Exception ignored) {}
            });
            bindingDone = true;
        }
    }
}