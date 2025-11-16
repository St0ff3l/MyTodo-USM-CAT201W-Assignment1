package com.mytodo; // <-- Correct package declaration

// ---------------------------------------------------------------------
// Imports
// ---------------------------------------------------------------------

// JavaFX core
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.*;
//  FlowPane has been removed
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

// Java standard library
import java.time.format.DateTimeFormatter;


/**
 * Custom list cell (TaskListCell), used to display a single Task in a ListView.
 */
public class TaskListCell extends ListCell<Task> {

    // --- Layout and controls (Fields) ---
    private final HBox rootLayout = new HBox(10); // Root HBox with spacing = 10
    private final CheckBox completedCheckbox = new CheckBox();
    private final Text titleText = new Text();
    private final Label detailLabel = new Label();

    //  tagContainer has been removed

    //  Vertical text stack now only contains title and detail
    private final VBox textStack = new VBox(2, titleText, detailLabel);

    // --- State and constants ---
    private final MainController controller; // Reference to the main controller
    private boolean bindingDone = false;     // Flag for width binding

    private static final double SIDE_MARGIN = 50;
    private static final double SPACER_HEIGHT = 100;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Constructor
     * (This part remains )
     * @param controller the MainController instance passed in
     */
    public TaskListCell(MainController controller) {
        this.controller = controller;

        // (Load CSS... )
        try {
            rootLayout.getStylesheets().add(
                    getClass().getResource("/com/mytodo/Main.css").toExternalForm()
            );
        } catch (Exception e) {
            System.err.println("Could not load stylesheet: Main.css");
            e.printStackTrace();
        }

        // (Layout setup... )
        completedCheckbox.setAllowIndeterminate(false);
        completedCheckbox.setStyle("-fx-mark-color: transparent;");
        completedCheckbox.setGraphic(null);
        rootLayout.setAlignment(Pos.CENTER_LEFT);
        rootLayout.setMaxWidth(Double.MAX_VALUE);
        this.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(textStack, Priority.ALWAYS);
        VBox.setVgrow(textStack, Priority.ALWAYS);

        // (Event listeners... )
        completedCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            Task task = getItem();
            if (task != null && task.isCompleted() != newVal) {
                controller.toggleCompletion(task);
                Platform.runLater(() -> {
                    if (getListView() != null) getListView().refresh();
                });
            }
        });

        // (Edit button... )
        Button editBtn = new Button("Edit");
        // CSS styleClasses for edit button color have already been defined
        editBtn.getStyleClass().addAll("flat-ghost", "edit");
        editBtn.setOnAction(e -> {
            Task t = getItem();
            if (t != null) controller.openTaskDetailDialog(t);
        });

        // (Delete button... )
        Button deleteBtn = new Button("Delete");
        // CSS styleClasses for delete button color have already been defined
        deleteBtn.getStyleClass().addAll("flat-ghost", "delete");
        deleteBtn.setOnAction(e -> {
            Task t = getItem();
            if (t != null) controller.deleteTask(t);
        });

        // (Assemble layout... )
        HBox actionBox = new HBox(5, editBtn, deleteBtn);
        actionBox.setAlignment(Pos.CENTER_RIGHT);
        rootLayout.getChildren().addAll(completedCheckbox, textStack, actionBox);

        // (Style settings... )
        titleText.setFont(Font.font("System", FontWeight.NORMAL, 16));
        detailLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 11px;");
        rootLayout.setStyle("-fx-padding: 10px 15px 10px 15px; -fx-background-color: #ffffff; -fx-background-radius: 8;");
        this.setPrefWidth(Region.USE_COMPUTED_SIZE);
    }

    /**
     * Core method: called when the cell is reused or when its data is updated.
     */
    @Override
    protected void updateItem(Task task, boolean empty) {
        super.updateItem(task, empty);

        // 1. Empty cell handling (Bug fix)
        if (empty || task == null) {
            setGraphic(null);
            setText(null);
            // Must explicitly reset style to transparent
            setStyle("-fx-background-color: transparent; -fx-padding: 0;");
            return;
        }

        // 2. "Ghost" item (Spacer Item)
        if ("(SPACER_ITEM)".equals(task.getTitle())) {
            Region spacer = new Region();
            spacer.setMinHeight(SPACER_HEIGHT);
            spacer.setPrefHeight(SPACER_HEIGHT);
            spacer.setMaxHeight(SPACER_HEIGHT);
            setGraphic(spacer);
            setText(null);
            // Transparent style here is correct
            setStyle("-fx-background-color: transparent; -fx-padding: 0;");
            return;
        }

        // 3. Normal task rendering
        // (Title and description text logic... )
        String title = task.getTitle() == null ? "(No title)" : task.getTitle().trim();
        String desc = task.getDescription() == null ? "" : task.getDescription().trim();
        String combined = desc.isEmpty() ? title : title + " • " + desc;
        titleText.setText(combined);

        // 4. Detail label
        String dateStr = task.getDueDate() != null ? task.getDueDate().format(DATE_FORMATTER) : "No due date";
        String timeStr = (task.getTime() != null) ? task.getTime().format(TIME_FORMATTER) : "No time";
        String priority = task.getPriority() == null ? "Normal" : task.getPriority();

        detailLabel.setText("Due: " + dateStr + " " + timeStr + " | Priority: " + priority);

        String listName = task.getListName();
        if (listName != null && !listName.isBlank()) {
            String listStr = " | List: " + listName;
            detailLabel.setText(detailLabel.getText() + listStr);
        }

        // (Checkbox and strikethrough logic... )
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

        // (Final setup... )
        setGraphic(rootLayout);
        setStyle("-fx-padding: 4px 0; -fx-background-color: transparent;");

        // (Width binding logic...)
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