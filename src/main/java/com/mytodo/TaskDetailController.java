package com.mytodo;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class TaskDetailController {

    @FXML private TextField titleField;
    @FXML private DatePicker dueDatePicker;
    @FXML private Spinner<LocalTime> dueTimeSpinner;
    @FXML private ChoiceBox<String> priorityBox;
    @FXML private TextArea descArea;
    @FXML private Button okButton;
    @FXML private Button cancelButton;

    // listName selector
    @FXML private ChoiceBox<String> listSelectorBox;

    private Task resultTask;
    private boolean okClicked = false;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private final LocalTime DEFAULT_END_OF_DAY_TIME = LocalTime.of(23, 59);
    private static final String UNLISTED_PLACEHOLDER = "Unlisted";

    @FXML
    private void initialize() {
        priorityBox.setItems(FXCollections.observableArrayList("Low", "Normal", "High"));
        priorityBox.setValue("Normal");
        dueDatePicker.setValue(LocalDate.now());

        okButton.setOnAction(event -> handleOk());
        cancelButton.setOnAction(event -> handleCancel());
    }

    /**
     * New version of loadData —— accepts ListInfo
     */
    public void loadData(Task task, ObservableList<ListInfo> listInfos) {

        this.resultTask = task;

        // -----------------------------
        // 1) Convert ListInfo → String (list names)
        // -----------------------------
        ObservableList<String> choiceList = FXCollections.observableArrayList();
        choiceList.add(UNLISTED_PLACEHOLDER); // default choice

        for (ListInfo li : listInfos) {
            choiceList.add(li.getName());
        }

        listSelectorBox.setItems(choiceList);

        // -----------------------------
        // 2) If editing an existing task, fill form fields
        // -----------------------------
        if (task != null) {
            titleField.setText(task.getTitle());
            descArea.setText(task.getDescription());
            dueDatePicker.setValue(task.getDueDate());
            priorityBox.setValue(task.getPriority());

            LocalTime time = (task.getTime() == null ? DEFAULT_END_OF_DAY_TIME : task.getTime());
            dueTimeSpinner.setValueFactory(createTimeValueFactory(time));

            if (task.getListName() != null) {
                listSelectorBox.setValue(task.getListName());
            } else {
                listSelectorBox.setValue(UNLISTED_PLACEHOLDER);
            }

        } else {
            // New task
            listSelectorBox.setValue(UNLISTED_PLACEHOLDER);
            dueTimeSpinner.setValueFactory(createTimeValueFactory(DEFAULT_END_OF_DAY_TIME));
        }
    }

    @FXML
    private void handleOk() {
        if (titleField.getText() == null || titleField.getText().isBlank()) return;

        if (resultTask == null) resultTask = new Task();

        resultTask.setTitle(titleField.getText());
        resultTask.setDescription(descArea.getText());
        resultTask.setDueDate(dueDatePicker.getValue());
        resultTask.setTime(getSelectedTime());
        resultTask.setPriority(priorityBox.getValue());
        resultTask.setImportant("High".equalsIgnoreCase(priorityBox.getValue()));

        String selectedList = listSelectorBox.getValue();
        if (UNLISTED_PLACEHOLDER.equals(selectedList)) {
            resultTask.setListName(null);
        } else {
            resultTask.setListName(selectedList);
        }

        okClicked = true;
        closeDialog();
    }

    @FXML
    private void handleCancel() {
        okClicked = false;
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) okButton.getScene().getWindow();
        stage.close();
    }

    public boolean isOkClicked() {
        return okClicked;
    }

    public Task getTask() {
        return resultTask;
    }

    /**
     * Create a 5-minute step Spinner for LocalTime
     */
    private SpinnerValueFactory<LocalTime> createTimeValueFactory(LocalTime initialTime) {
        return new SpinnerValueFactory<>() {
            {
                setValue(initialTime);
                setConverter(new StringConverter<>() {
                    @Override
                    public String toString(LocalTime t) {
                        return (t == null ? "" : TIME_FORMATTER.format(t));
                    }
                    @Override
                    public LocalTime fromString(String s) {
                        try {
                            return LocalTime.parse(s, TIME_FORMATTER);
                        } catch (Exception e) {
                            return initialTime;
                        }
                    }
                });
            }
            @Override public void decrement(int steps) { setValue(getValue().minusMinutes(steps * 5)); }
            @Override public void increment(int steps) { setValue(getValue().plusMinutes(steps * 5)); }
        };
    }

    private LocalTime getSelectedTime() {
        try {
            dueTimeSpinner.commitValue();
            return dueTimeSpinner.getValue();
        } catch (Exception e) {
            return DEFAULT_END_OF_DAY_TIME;
        }
    }
}