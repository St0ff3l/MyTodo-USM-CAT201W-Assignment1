package com.mytodo;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.stage.Stage; // 导入 Stage
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

    // 1. 注入自定义按钮
    @FXML private Button okButton;
    @FXML private Button cancelButton;

    private Task resultTask;
    private boolean okClicked = false; // 标记是否点击了OK

    // Time format and default values
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private final LocalTime DEFAULT_END_OF_DAY_TIME = LocalTime.of(23, 59);

    @FXML
    private void initialize() {
        // Initialize ChoiceBox (Priority) and DatePicker
        priorityBox.setItems(FXCollections.observableArrayList("Low", "Normal", "High"));
        priorityBox.setValue("Normal");
        dueDatePicker.setValue(LocalDate.now());

        // 2. 为自定义按钮添加事件
        okButton.setOnAction(event -> handleOk());
        cancelButton.setOnAction(event -> handleCancel());
    }

    /**
     * Called by MainController right after FXML is loaded.
     * Initializes Spinner ValueFactory and loads task data if editing.
     */
    public void loadData(Task task) {
        this.resultTask = task;

        // Determine initial time (if editing -> use existing time; otherwise -> 23:59)
        LocalTime initialTime = DEFAULT_END_OF_DAY_TIME;

        // Load existing task data if editing
        if (task != null) {
            titleField.setText(task.getTitle());
            descArea.setText(task.getDescription());
            dueDatePicker.setValue(task.getDueDate());
            priorityBox.setValue(task.getPriority());

            if (task.getTime() != null) {
                initialTime = task.getTime();
            }
        }

        // Initialize Spinner
        SpinnerValueFactory<LocalTime> timeValueFactory = createTimeValueFactory(initialTime);
        dueTimeSpinner.setValueFactory(timeValueFactory);
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

    // 3. 按钮事件处理器
    /**
     * 当用户点击 OK 时调用。
     */
    @FXML
    private void handleOk() {
        // 验证 (示例)
        if (titleField.getText() == null || titleField.getText().isBlank()) {
            System.out.println("Title is required."); // 实际应用中应显示警告
            return;
        }

        if (resultTask == null) {
            resultTask = new Task();
        }

        // 收集表单数据
        resultTask.setTitle(titleField.getText());
        resultTask.setDescription(descArea.getText());
        resultTask.setDueDate(dueDatePicker.getValue());
        resultTask.setTime(getSelectedTime());
        resultTask.setPriority(priorityBox.getValue());
        resultTask.setImportant("High".equalsIgnoreCase(resultTask.getPriority()));

        okClicked = true; // 标记成功
        closeDialog(); // 关闭窗口
    }

    /**
     * Cancel 按钮的事件处理器
     */
    @FXML
    private void handleCancel() {
        okClicked = false;
        closeDialog(); // 直接关闭窗口
    }

    /**
     * 4. 关闭窗口的辅助方法
     */
    private void closeDialog() {
        // 从按钮获取 Scene，再获取 Window (Stage)
        Stage stage = (Stage) okButton.getScene().getWindow();
        stage.close();
    }

    /**
     * 5. 辅助方法，供 MainController 调用
     * 允许 MainController 检查是否点击了 "OK"。
     */
    public boolean isOkClicked() {
        return okClicked;
    }

    /**
     * 返回被编辑或创建的 Task 对象。
     */
    public Task getTask() {
        return resultTask;
    }
}