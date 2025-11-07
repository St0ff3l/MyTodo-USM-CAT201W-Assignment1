package com.todoapp;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;

import java.time.LocalDate;
import java.time.LocalTime;

public class TaskDetailController {

    @FXML private TextField titleField;
    @FXML private DatePicker dueDatePicker;
    @FXML private ChoiceBox<String> priorityBox;
    @FXML private TextArea descArea;

    private Task resultTask;

    @FXML
    private void initialize() {
        priorityBox.setItems(FXCollections.observableArrayList("低", "普通", "高"));
        priorityBox.setValue("普通");
        dueDatePicker.setValue(LocalDate.now());
    }

    @FXML
    private void onOK() {
        resultTask = new Task(
                titleField.getText(),
                descArea.getText(),
                dueDatePicker.getValue(),
                LocalTime.now(),
                priorityBox.getValue()
        );
    }

    public Task getTask() {
        if (resultTask == null && titleField != null && !titleField.getText().isBlank()) {
            resultTask = new Task(
                    titleField.getText(),
                    descArea.getText(),
                    dueDatePicker.getValue(),
                    LocalTime.now(),
                    priorityBox.getValue()
            );
        }
        return resultTask;
    }
}