package com.mytodo;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Controller for Task Detail Dialog
 * Handles form initialization, data binding, and returning the resulting Task.
 */
public class TaskDetailController {

    @FXML private TextField titleField;
    @FXML private DatePicker dueDatePicker;
    @FXML private Spinner<LocalTime> dueTimeSpinner;
    @FXML private ChoiceBox<String> priorityBox;
    @FXML private TextArea descArea;

    private Task resultTask;

    // Time format and default values
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private final LocalTime DEFAULT_END_OF_DAY_TIME = LocalTime.of(23, 59);

    @FXML
    private void initialize() {
        // Initialize ChoiceBox (Priority) and DatePicker
        priorityBox.setItems(FXCollections.observableArrayList("Low", "Normal", "High"));
        priorityBox.setValue("Normal");
        dueDatePicker.setValue(LocalDate.now());

        // Spinner will be initialized later in loadData()
    }

    /**
     * Called by MainController right after FXML is loaded.
     * Initializes Spinner ValueFactory and loads task data if editing.
     */
    public void loadData(Task task) {
        this.resultTask = task;

        // Determine initial time (if editing -> use existing time; otherwise -> 23:59)
        LocalTime initialTime = task != null && task.getTime() != null
                ? task.getTime() : DEFAULT_END_OF_DAY_TIME;

        // Initialize Spinner
        SpinnerValueFactory<LocalTime> timeValueFactory = createTimeValueFactory(initialTime);
        dueTimeSpinner.setValueFactory(timeValueFactory);

        // Load existing task data if editing
        if (task != null) {
            titleField.setText(task.getTitle());
            descArea.setText(task.getDescription());
            dueDatePicker.setValue(task.getDueDate());
            priorityBox.setValue(task.getPriority());
        }
    }

    /**
     * Helper method to create a SpinnerValueFactory for time selection.
     * Allows increment/decrement by 5 minutes.
     */
    private SpinnerValueFactory<LocalTime> createTimeValueFactory(LocalTime initialTime) {
        return new SpinnerValueFactory<>() {
            {
                setValue(initialTime);
                setConverter(new StringConverter<LocalTime>() {
                    @Override
                    public String toString(LocalTime time) {
                        return (time == null) ? "" : TIME_FORMATTER.format(time);
                    }

                    @Override
                    public LocalTime fromString(String string) {
                        try {
                            return LocalTime.parse(string, TIME_FORMATTER);
                        } catch (java.time.format.DateTimeParseException e) {
                            return initialTime;
                        }
                    }
                });
            }

            @Override
            public void decrement(int steps) {
                setValue(getValue().minusMinutes(steps * 5));
            }

            @Override
            public void increment(int steps) {
                setValue(getValue().plusMinutes(steps * 5));
            }
        };
    }

    /**
     * Get the final selected time from Spinner.
     * Ensures a valid LocalTime even if user input is incomplete.
     */
    private LocalTime getSelectedTime() {
        try {
            if (dueTimeSpinner == null) return DEFAULT_END_OF_DAY_TIME;
            dueTimeSpinner.commitValue();
            return dueTimeSpinner.getValue();
        } catch (Exception e) {
            return DEFAULT_END_OF_DAY_TIME;
        }
    }

    /**
     * Called when user clicks OK button in dialog.
     * Collects form data and populates resultTask.
     */
    @FXML
    public void onOK() {
        if (resultTask == null) {
            resultTask = new Task();
        }

        // Collect form data
        resultTask.setTitle(titleField.getText());
        resultTask.setDescription(descArea.getText());
        resultTask.setDueDate(dueDatePicker.getValue());
        resultTask.setTime(getSelectedTime());
        resultTask.setPriority(priorityBox.getValue());

        // Determine if the task should be marked as important
        resultTask.setImportant("High".equalsIgnoreCase(resultTask.getPriority()));
    }

    /**
     * Returns the filled Task object.
     * If the user didn't explicitly press OK but filled the title, collects data automatically.
     */
    public Task getTask() {
        if (resultTask == null && titleField != null && !titleField.getText().isBlank()) {
            onOK();
        }
        return resultTask;
    }
}
