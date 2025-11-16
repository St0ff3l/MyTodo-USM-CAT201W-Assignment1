package com.mytodo; // <-- Correct package declaration

// --- Incorrect imports removed or fixed ---
// import com.example._201assignment12.model.Task; <-- WRONG! Removed

import com.mytodo.Task; // <-- CORRECT: Importing Task from the right package
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

// TaskCell class (implements custom card-style UI)
public class TaskCell extends ListCell<Task> {

    // ... (layout components, same as previous structure) ...
    private final HBox rootLayout = new HBox(10);
    private final CheckBox completedCheckbox = new CheckBox();
    private final Label titleLabel = new Label();
    private final Label detailLabel = new Label();
    private final VBox textStack = new VBox(2, titleLabel, detailLabel);
    private final ToggleButton importantToggle = new ToggleButton("⭐");

    public TaskCell() {
        // ... (layout initialization, same as previous structure) ...
        rootLayout.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        VBox.setVgrow(textStack, Priority.ALWAYS);
        HBox.setHgrow(textStack, Priority.ALWAYS);

        // Bind checkbox state changes (core interaction logic)
        completedCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            Task task = getItem();
            if (task != null && task.isCompleted() != newVal) {

                // 1. Update the task data
                task.setCompleted(newVal);

                // 2. Notify ListView to refresh — IMPORTANT!
                // Must notify the ListView to trigger applyFilters and UI refresh
                getListView().refresh();

                // 3. Real-time saving (Part 4 I/O logic)
                // Saving is handled in TaskListCell or MainController; unchanged here
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

            // Bind data
            titleLabel.setText(task.getTitle());
            detailLabel.setText(task.getDueDate() + " · " + task.getPriority() + " priority");
            completedCheckbox.setSelected(task.isCompleted());

            // Strikethrough UI based on completion state
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