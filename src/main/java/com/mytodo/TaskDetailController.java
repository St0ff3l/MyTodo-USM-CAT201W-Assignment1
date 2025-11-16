package com.mytodo;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList; // 🌟 1. 导入 ObservableList
import javafx.fxml.FXML;
import javafx.scene.control.*;
// 🌟 2. [已移除] 移除了 FlowPane 和 HBox
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
// 🌟 3. [已移除] 移除了 ArrayList 和 List

public class TaskDetailController {

    // --- 现有 FXML 字段 ---
    @FXML private TextField titleField;
    @FXML private DatePicker dueDatePicker;
    @FXML private Spinner<LocalTime> dueTimeSpinner;
    @FXML private ChoiceBox<String> priorityBox;
    @FXML private TextArea descArea;
    @FXML private Button okButton;
    @FXML private Button cancelButton;

    // 🌟 4. [已修改] FXML 字段重构
    @FXML private ChoiceBox<String> listSelectorBox; // (新增)

    // --- 状态字段 ---
    private Task resultTask;
    private boolean okClicked = false;

    // 🌟 5. [已移除] 移除了 currentTags 列表

    // --- 常量 ---
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private final LocalTime DEFAULT_END_OF_DAY_TIME = LocalTime.of(23, 59);

    // 🌟 6. [新增] 用于 ChoiceBox 的占位符，代表 "无列表"
    private static final String UNLISTED_PLACEHOLDER = "Unlisted";

    @FXML
    private void initialize() {
        // (Priority, Date, Buttons 逻辑保持不变)
        priorityBox.setItems(FXCollections.observableArrayList("Low", "Normal", "High"));
        priorityBox.setValue("Normal");
        dueDatePicker.setValue(LocalDate.now());
        okButton.setOnAction(event -> handleOk());
        cancelButton.setOnAction(event -> handleCancel());
    }

    /**
     * 🌟 7. [重大修改] loadData 方法签名已更改
     *
     * 加载任务数据 (由 MainController 调用)
     *
     * @param task           要编辑的任务 (如果为 null 则为新任务)
     * @param availableLists MainController 传入的可用列表清单
     */
    public void loadData(Task task, ObservableList<String> availableLists) {
        this.resultTask = task;
        LocalTime initialTime = DEFAULT_END_OF_DAY_TIME;

        // 🌟 8. [已修改] 创建一个包含 "Unlisted" 选项的新列表
        // 1. 创建一个新列表
        ObservableList<String> choiceBoxLists = FXCollections.observableArrayList();
        // 2. 添加 "Unlisted" 占位符
        choiceBoxLists.add(UNLISTED_PLACEHOLDER);
        // 3. 添加所有真实的列表
        choiceBoxLists.addAll(availableLists);

        // 4. 将 ChoiceBox 设置为使用这个新列表
        listSelectorBox.setItems(choiceBoxLists);

        if (task != null) {
            // (加载 Title, Desc, Date, Priority, Time... 保持不变)
            titleField.setText(task.getTitle());
            descArea.setText(task.getDescription());
            dueDatePicker.setValue(task.getDueDate());
            priorityBox.setValue(task.getPriority());
            if (task.getTime() != null) {
                initialTime = task.getTime();
            }

            // 🌟 9. [已修改] 设置 ChoiceBox 的选中项
            if (task.getListName() != null) {
                // 如果任务有一个列表 (e.g., "Work"), 选中它
                listSelectorBox.setValue(task.getListName());
            } else {
                // 如果任务的 listName 为 null, 选中 "Unlisted"
                listSelectorBox.setValue(UNLISTED_PLACEHOLDER);
            }

        } else {
            // 这是一个新任务, 默认选中 "Unlisted"
            listSelectorBox.setValue(UNLISTED_PLACEHOLDER);
        }

        // (Spinner 初始化... 保持不变)
        SpinnerValueFactory<LocalTime> timeValueFactory = createTimeValueFactory(initialTime);
        dueTimeSpinner.setValueFactory(timeValueFactory);
    }

    // 🌟 10. [已移除] 移除了所有 "Tags" 相关方法

    /**
     * 当用户点击 OK 时调用
     */
    @FXML
    private void handleOk() {
        // (验证... 保持不变)
        if (titleField.getText() == null || titleField.getText().isBlank()) {
            System.out.println("Title is required.");
            return;
        }

        if (resultTask == null) {
            resultTask = new Task();
        }

        // (收集数据... 保持不变)
        resultTask.setTitle(titleField.getText());
        resultTask.setDescription(descArea.getText());
        resultTask.setDueDate(dueDatePicker.getValue());
        resultTask.setTime(getSelectedTime());
        resultTask.setPriority(priorityBox.getValue());
        resultTask.setImportant("High".equalsIgnoreCase(resultTask.getPriority()));

        // 🌟 11. [已修改] 保存所选的 "List" (列表)
        String selectedList = listSelectorBox.getValue();

        if (UNLISTED_PLACEHOLDER.equals(selectedList)) {
            // 如果用户选择了 "Unlisted", 我们保存 null
            resultTask.setListName(null);
        } else {
            // 否则, 保存所选的列表名称 (e.g., "Work")
            resultTask.setListName(selectedList);
        }

        okClicked = true;
        closeDialog();
    }

    /**
     * 当用户点击 Cancel 时调用
     */
    @FXML
    private void handleCancel() {
        okClicked = false;
        closeDialog(); // 直接关闭窗口
    }

    // -----------------------------------------------------------------
    // 辅助方法 (Helpers)
    // -----------------------------------------------------------------

    /**
     * 辅助方法：关闭当前对话框窗口
     */
    private void closeDialog() {
        Stage stage = (Stage) okButton.getScene().getWindow();
        stage.close();
    }

    /**
     * 辅助方法：供 MainController 检查是否点击了 "OK"
     */
    public boolean isOkClicked() {
        return okClicked;
    }

    /**
     * 辅助方法：供 MainController 获取最终的 Task 对象
     */
    public Task getTask() {
        return resultTask;
    }

    /**
     * 辅助方法：创建时间微调器 (Spinner) 的工厂
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
            @Override public void decrement(int steps) { setValue(getValue().minusMinutes(steps * 5)); }
            @Override public void increment(int steps) { setValue(getValue().plusMinutes(steps * 5)); }
        };
    }

    /**
     * 辅助方法：从 Spinner 安全地获取时间
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
}