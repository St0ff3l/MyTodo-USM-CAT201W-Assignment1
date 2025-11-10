package com.mytodo;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.time.format.DateTimeFormatter;

/**
 * Custom ListCell for Task items (with real strike-through effect on completed tasks)
 */
public class TaskListCell extends ListCell<Task> {

    private final HBox rootLayout = new HBox(10);
    private final CheckBox completedCheckbox = new CheckBox();

    // --- changed: use Text instead of Label for title (supports strike-through)
    private final Text titleText = new Text();
    private final Label detailLabel = new Label();
    private final VBox textStack = new VBox(2, titleText, detailLabel);

    private final MainController controller;

    // Date/time formatters
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public TaskListCell(MainController controller) {
        this.controller = controller;

        rootLayout.setAlignment(Pos.CENTER_LEFT);
        VBox.setVgrow(textStack, Priority.ALWAYS);
        HBox.setHgrow(textStack, Priority.ALWAYS);

        // toggle completion
        completedCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            Task task = getItem();
            if (task != null && task.isCompleted() != newVal) {
                controller.toggleCompletion(task);
            }
        });

        // Edit/Delete buttons
        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().add("flat-ghost");
        editBtn.setOnAction(e -> {
            Task t = getItem();
            if (t != null) controller.openTaskDetailDialog(t);
        });

        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("flat-ghost");
        deleteBtn.setOnAction(e -> {
            Task t = getItem();
            if (t != null) controller.deleteTask(t);
        });

        HBox actionBox = new HBox(5, editBtn, deleteBtn);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        rootLayout.getChildren().addAll(completedCheckbox, textStack, actionBox);
        HBox.setHgrow(actionBox, Priority.ALWAYS);

        // initial style
        titleText.setFont(Font.font("System", FontWeight.NORMAL, 16));
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
            String title = task.getTitle() == null ? "(No title)" : task.getTitle();
            titleText.setText(title);

            String dateStr = task.getDueDate() != null ? task.getDueDate().format(DATE_FORMATTER) : "No due date";
            String timeStr = (task.getTime() != null) ? task.getTime().format(TIME_FORMATTER) : "No time";
            String priority = task.getPriority() == null ? "Normal" : task.getPriority();

            detailLabel.setText("Due: " + dateStr + " " + timeStr + " | Priority: " + priority);

            completedCheckbox.setSelected(task.isCompleted());

            if (task.isCompleted()) {
                // ✅ real strike-through effect
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
        }
    }
}
