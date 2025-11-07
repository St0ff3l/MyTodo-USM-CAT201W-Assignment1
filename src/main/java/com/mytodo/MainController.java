package com.mytodo;

import com.mytodo.util.JsonDataManager;
import javafx.application.Platform; // 导入 Platform
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.VBox;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

public class MainController {

    // --- FXML BINDINGS ---
    @FXML private TextField searchField, quickAddField;
    @FXML private Button quickAddBtn, detailAddBtn, filterBtn;
    @FXML private Button btnToday, btnImportant, btnAll, btnFinished, btnPending;
    @FXML private ListView<Task> taskList;
    @FXML private VBox sidebar;

    // --- DATA & FILTERING ---
    private final ObservableList<Task> masterTasks = FXCollections.observableArrayList();
    private final FilteredList<Task> filteredTasks = new FilteredList<>(masterTasks, t -> true);
    private String currentFilterType = "ALL";

    // --- I/O SETUP (Part 4) ---
    private static final File DATA_FILE = new File("tasks.json");
    private final JsonDataManager dataManager = new JsonDataManager();
    private final LocalTime DEFAULT_END_OF_DAY_TIME = LocalTime.of(23, 59);


    @FXML
    private void initialize() {
        loadTasks();
        taskList.setItems(filteredTasks);
        taskList.setCellFactory(list -> new TaskListCell(this));

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        btnAll.setOnAction(e -> setNavFilter("ALL", btnAll));
        btnToday.setOnAction(e -> setNavFilter("TODAY", btnToday));
        btnImportant.setOnAction(e -> setNavFilter("IMPORTANT", btnImportant));
        btnFinished.setOnAction(e -> setNavFilter("FINISHED", btnFinished));
        btnPending.setOnAction(e -> setNavFilter("PENDING", btnPending));
        setNavFilter("ALL", btnAll);

        quickAddBtn.setOnAction(e -> addQuickTask());
        quickAddField.setOnAction(e -> addQuickTask());
        detailAddBtn.setOnAction(e -> openTaskDetailDialog(null));
    }

    // --- CRUD 操作 ---

    private void addQuickTask() {
        String text = quickAddField.getText();
        if (text == null || text.isBlank()) return;
        Task task = new Task(text.trim(), "", LocalDate.now(), DEFAULT_END_OF_DAY_TIME, "普通");
        masterTasks.add(task);
        quickAddField.clear();
        saveTasks();
    }

    public void openTaskDetailDialog(Task taskToEdit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("TaskDetailDialog.fxml"));
            DialogPane pane = loader.load();
            TaskDetailController controller = loader.getController();

            controller.loadData(taskToEdit);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle(taskToEdit == null ? "添加详细任务" : "编辑任务");
            dialog.setDialogPane(pane);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == ButtonType.OK) {
                    controller.onOK();
                    return dialogButton;
                }
                return null;
            });

            Optional<ButtonType> result = dialog.showAndWait();

            if (result.isPresent() && result.get() == ButtonType.OK) {
                Task updatedTask = controller.getTask();
                if (updatedTask != null) {
                    if (taskToEdit == null) {
                        masterTasks.add(updatedTask);
                        new Alert(AlertType.INFORMATION, "✅ 已添加任务: " + updatedTask.getTitle()).show();
                    } else {
                        taskList.refresh();
                        new Alert(AlertType.INFORMATION, "✏️ 已更新任务: " + updatedTask.getTitle()).show();
                    }
                    saveTasks();
                }
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void deleteTask(Task task) {
        if (task != null) {
            Alert confirm = new Alert(AlertType.CONFIRMATION);
            confirm.setTitle("删除确认");
            confirm.setHeaderText(null);
            confirm.setContentText("确定要删除任务: " + task.getTitle() + " 吗？");

            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    masterTasks.remove(task);
                    saveTasks();
                }
            });
        }
    }

    public void toggleCompletion(Task task) {
        task.setCompleted(!task.isCompleted());
        saveTasks();
        applyFilters();
    }

    // --- 筛选逻辑 (Part 3) ---

    private void setNavFilter(String filterType, Button selectedButton) {
        currentFilterType = filterType;
        sidebar.getChildren().stream()
                .filter(node -> node instanceof Button)
                .map(node -> (Button)node)
                .forEach(btn -> btn.getStyleClass().remove("selected"));

        selectedButton.getStyleClass().add("selected");
        applyFilters();
    }

    private void applyFilters() {
        String searchText = searchField.getText() != null ? searchField.getText().toLowerCase() : "";

        filteredTasks.setPredicate(task -> {
            if (!isNavFilterMatch(task)) {
                return false;
            }
            if (!searchText.isEmpty() &&
                    !task.getTitle().toLowerCase().contains(searchText) &&
                    !task.getDescription().toLowerCase().contains(searchText)) {
                return false;
            }
            return true;
        });
    }

    private boolean isNavFilterMatch(Task task) {
        boolean isToday = task.getDueDate().isEqual(LocalDate.now());

        switch (currentFilterType) {
            case "ALL": return true;
            case "TODAY": return isToday;
            case "IMPORTANT": return task.isImportant();
            case "FINISHED": return task.isCompleted();
            case "PENDING": return !task.isCompleted();
            default: return true;
        }
    }

    // --- I/O (JSON) ---

    private void loadTasks() {
        masterTasks.addAll(dataManager.load(DATA_FILE));
    }

    private void saveTasks() {
        dataManager.save(DATA_FILE, masterTasks);
    }

    // 供 Main.java 调用，确保在关闭时保存
    public void saveAndExit() {
        saveTasks();
        // --- 最终修正：确保应用退出 ---
        Platform.exit();
        System.exit(0);
    }

    // --- 菜单栏功能实现 ---

    // File -> Exit 菜单绑定
    @FXML private void handleExit() {
        saveAndExit();
    }

    // Edit -> Delete All Completed 菜单绑定
    @FXML
    private void handleDeleteCompleted() {
        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle("清理任务");
        confirm.setHeaderText("确认删除所有已完成的任务吗？");
        confirm.setContentText("此操作不可撤销。");

        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            // 移除所有已完成的任务
            masterTasks.removeIf(Task::isCompleted);

            // 重新应用筛选和保存
            applyFilters();
            saveTasks();
        }
    }

    // Help -> About 菜单绑定
    @FXML
    private void handleHelp() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("关于 MyTodo");
        alert.setHeaderText("CAT201 Integrated Software Development Workshop Assignment I");
        alert.setContentText("版本: v2.1 (JavaFX)\n" +
                "功能: Task Management, Search & Filter, JSON I/O\n" +
                "组员: [在此处填写您的组员姓名/学号]");
        alert.showAndWait();
    }
}