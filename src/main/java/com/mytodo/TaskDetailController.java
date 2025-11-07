package com.mytodo;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class TaskDetailController {

    @FXML private TextField titleField;
    @FXML private DatePicker dueDatePicker;
    @FXML private Spinner<LocalTime> dueTimeSpinner; // 新增 Spinner
    @FXML private ChoiceBox<String> priorityBox;
    @FXML private TextArea descArea;

    private Task resultTask;

    // 定义时间格式
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    // 默认时间 23:59
    private final LocalTime DEFAULT_END_OF_DAY_TIME = LocalTime.of(23, 59);


    @FXML
    private void initialize() {
        // --- 初始化 ChoiceBox 和 DatePicker ---
        priorityBox.setItems(FXCollections.observableArrayList("低", "普通", "高"));
        priorityBox.setValue("普通");
        dueDatePicker.setValue(LocalDate.now());

        // 关键：不在这里设置 Spinner ValueFactory，只留给 loadData() 方法
    }

    // NEW: 负责初始化 Spinner ValueFactory 和加载数据，由 MainController 在 FXML 加载后立即调用
    public void loadData(Task task) {
        this.resultTask = task;

        // 1. 确定初始时间 (如果是编辑模式，使用任务时间；否则使用默认时间 23:59)
        LocalTime initialTime = task != null && task.getTime() != null ? task.getTime() : DEFAULT_END_OF_DAY_TIME;

        // 2. 初始化 Spinner ValueFactory (现在可以安全地访问 dueTimeSpinner)
        SpinnerValueFactory<LocalTime> timeValueFactory = createTimeValueFactory(initialTime);
        dueTimeSpinner.setValueFactory(timeValueFactory);

        // 3. 加载任务数据 (编辑模式)
        if (task != null) {
            titleField.setText(task.getTitle());
            descArea.setText(task.getDescription());
            dueDatePicker.setValue(task.getDueDate());
            priorityBox.setValue(task.getPriority());
            // 时间已通过 ValueFactory 的 initialTime 设置
        }
    }

    // 辅助方法：创建 Spinner ValueFactory (抽离逻辑，使代码更清晰)
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
                        }
                        catch (java.time.format.DateTimeParseException e) {
                            return initialTime;
                        }
                    }
                });
            }
            @Override public void decrement(int steps) { setValue(getValue().minusMinutes(steps * 5)); }
            @Override public void increment(int steps) { setValue(getValue().plusMinutes(steps * 5)); }
        };
    }

    // 实用方法：从 Spinner 获取最终时间
    private LocalTime getSelectedTime() {
        try {
            if (dueTimeSpinner == null) return DEFAULT_END_OF_DAY_TIME;

            dueTimeSpinner.commitValue();
            return dueTimeSpinner.getValue();
        } catch (Exception e) {
            return DEFAULT_END_OF_DAY_TIME;
        }
    }

    // 在用户点击 OK 后，收集表单数据并保存到 resultTask
    @FXML
    public void onOK() {
        if (resultTask == null) {
            resultTask = new Task();
        }

        // 收集数据并更新对象
        resultTask.setTitle(titleField.getText());
        resultTask.setDescription(descArea.getText());
        resultTask.setDueDate(dueDatePicker.getValue());
        resultTask.setTime(getSelectedTime());
        resultTask.setPriority(priorityBox.getValue());

        // 【关键逻辑】：根据优先级设置 isImportant 状态
        resultTask.setImportant(resultTask.getPriority().equalsIgnoreCase("高"));
    }

    public Task getTask() {
        // 如果未点击OK但需要返回数据，则调用 onOK 确保数据被收集
        if (resultTask == null && titleField != null && !titleField.getText().isBlank()) {
            onOK(); // 确保收集数据
        }
        return resultTask;
    }
}