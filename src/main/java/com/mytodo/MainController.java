package com.mytodo;

import com.mytodo.util.JsonDataManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
// 导入 DialogPane
import javafx.scene.control.DialogPane;
import java.util.stream.Collectors;

public class MainController {

    // --- FXML BINDINGS ---
    @FXML private TextField searchField, quickAddField;
    @FXML private Button quickAddBtn, detailAddBtn, filterBtn;
    @FXML private Button searchClearBtn; // <-- 修正已应用
    @FXML private Button btnToday, btnImportant, btnAll, btnFinished, btnPending;
    @FXML private ListView<Task> taskList;
    @FXML private VBox sidebar;
    @FXML private HBox floatingAddBox;
    @FXML private VBox root;

    // --- DATA & FILTERING ---
    private final ObservableList<Task> masterTasks = FXCollections.observableArrayList();
    private final FilteredList<Task> filteredTasks = new FilteredList<>(masterTasks, t -> true);
    private String currentFilterType = "ALL";

    // --- I/O SETUP ---
    private static final File DATA_FILE = new File("tasks.json");
    private final JsonDataManager dataManager = new JsonDataManager();
    private final LocalTime DEFAULT_END_OF_DAY_TIME = LocalTime.of(23, 59);

    // spacer 常量
    private static final String SPACER_TITLE = "(SPACER_ITEM)";

    // --- INITIALIZATION ---
    @FXML
    private void initialize() {
        // (您所有的 initialize 代码保持不变)
        try {
            loadTasks();
        } catch (Exception ex) {
            System.err.println("[ERROR] loadTasks failed: " + ex.getMessage());
            ex.printStackTrace();
        }
        ensureSpacerExists();
        taskList.setItems(filteredTasks);
        taskList.setCellFactory(list -> new TaskListCell(this));
        VBox.setVgrow(taskList, Priority.ALWAYS);
        HBox.setHgrow(taskList, Priority.ALWAYS);
        if (searchField != null) {
            searchField.setOnAction(e -> performSearch());
        }
        if (filterBtn != null) {
            filterBtn.setOnAction(e -> performSearch());
        }
        if (searchClearBtn != null) {
            searchClearBtn.setOnAction(e -> {
                searchField.clear();
                applyFilters();
                taskList.refresh();
                System.out.println("[DEBUG] Search cleared and filters reapplied.");
            });
        }
        if (btnAll != null) btnAll.setOnAction(e -> setNavFilter("ALL", btnAll));
        if (btnToday != null) btnToday.setOnAction(e -> setNavFilter("TODAY", btnToday));
        if (btnImportant != null) btnImportant.setOnAction(e -> setNavFilter("IMPORTANT", btnImportant));
        if (btnFinished != null) btnFinished.setOnAction(e -> setNavFilter("FINISHED", btnFinished));
        if (btnPending != null) btnPending.setOnAction(e -> setNavFilter("PENDING", btnPending));
        setNavFilter("ALL", btnAll);
        if (quickAddBtn != null) quickAddBtn.setOnAction(e -> addQuickTask());
        if (quickAddField != null) quickAddField.setOnAction(e -> addQuickTask());
        if (detailAddBtn != null) detailAddBtn.setOnAction(e -> openTaskDetailDialog(null));
    }

    // (ensureSpacerExists, performSearch, addQuickTask 保持不变)
    private void ensureSpacerExists() {
        masterTasks.removeIf(t -> t != null && SPACER_TITLE.equals(t.getTitle()));
        Task spacer = new Task(SPACER_TITLE, "", null, null, "Normal");
        masterTasks.add(spacer);
    }
    private void performSearch() {
        applyFilters();
        System.out.println("[DEBUG] performSearch done. results=" + filteredTasks.size());
    }
    private void addQuickTask() {
        String text = quickAddField.getText();
        if (text == null || text.isBlank()) return;
        int insertPos = Math.max(0, masterTasks.size() - 1);
        Task task = new Task(text.trim(), "", LocalDate.now(), DEFAULT_END_OF_DAY_TIME, "Normal");
        masterTasks.add(insertPos, task);
        quickAddField.clear();
        saveTasks();
        applyFilters();
        taskList.refresh();
    }


    // --- 🌟 showCustomAlert 方法 (保持不变) 🌟 ---
    private ButtonType showCustomAlert(String title, String header, String content) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mytodo/CustomAlertView.fxml"));
            DialogPane pane = loader.load();
            CustomAlertController controller = loader.getController();
            controller.setMessage(header, content);
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle(title);
            dialog.setDialogPane(pane);
            pane.getButtonTypes().clear();
            dialog.showAndWait();
            return controller.getResult();
        } catch (IOException ex) {
            ex.printStackTrace();
            Alert fallback = new Alert(AlertType.ERROR, "Failed to load custom dialog: " + ex.getMessage());
            fallback.showAndWait();
            return ButtonType.CANCEL;
        }
    }


    // --- 🌟 [已修改] 打开详情对话框 (使用 DialogPane 的简单版本) 🌟 ---
    public void openTaskDetailDialog(Task taskToEdit) {
        try {
            // 1. 加载 FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mytodo/TaskDetailDialog.fxml"));

            // 2. 加载为 DialogPane
            DialogPane pane = loader.load();

            // 3. 获取 Controller
            TaskDetailController controller = loader.getController();

            // 4. 传递数据
            controller.loadData(taskToEdit);

            // 5. 创建 Dialog
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle(taskToEdit == null ? "Add Task" : "Edit Task");

            // 6. 设置 DialogPane
            dialog.setDialogPane(pane);

            // 7. [关键] 移除 DialogPane 默认按钮
            // 这样它就只会显示我们在 FXML 中添加的按钮
            pane.getButtonTypes().clear();

            // 8. 应用 CSS
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/mytodo/Main.css").toExternalForm());
            // (样式类已在 FXML 中设置)

            // 9. 显示并等待
            // 代码会在这里暂停，直到 TaskDetailController 调用 closeDialog()
            dialog.showAndWait();

            // 10. 检查是否点击了 OK
            if (controller.isOkClicked()) {
                Task updatedTask = controller.getTask();
                if (updatedTask != null) {

                    // [修改] 移除了 Emoji
                    String msg = taskToEdit == null ? "Task added: " : "Task updated: ";
                    showCustomAlert("Success", null, msg + updatedTask.getTitle());

                    if (taskToEdit == null) {
                        int insertPos = Math.max(0, masterTasks.size() - 1);
                        masterTasks.add(insertPos, updatedTask);
                    } else {
                        // 只需要刷新, 因为 taskToEdit 是引用, 已被修改
                        taskList.refresh();
                    }
                    saveTasks();
                    applyFilters();
                }
            }
            // 如果 isOkClicked() == false (用户点了Cancel), 我们什么也不做

        } catch (IOException ex) {
            ex.printStackTrace();
            showCustomAlert("Error", "Unexpected error", "Failed to open task dialog: " + ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            showCustomAlert("Error", "Unexpected error", "Unexpected error: " + ex.getMessage());
        }
    }

    // --- (您所有其他的方法... deleteTask, toggleCompletion, setNavFilter, etc... 保持不变) ---
    public void deleteTask(Task task) {
        if (task == null || SPACER_TITLE.equals(task.getTitle())) return;
        ButtonType confirmResult = showCustomAlert(
                "Delete Confirmation",
                "Are you sure to delete: " + task.getTitle() + " ?",
                "This action cannot be undone."
        );
        if (confirmResult == ButtonType.OK) {
            masterTasks.remove(task);
            saveTasks();
            applyFilters();
            taskList.refresh();
        }
    }
    public void toggleCompletion(Task task) {
        if (task == null || SPACER_TITLE.equals(task.getTitle())) return;
        task.setCompleted(!task.isCompleted());
        saveTasks();
        applyFilters();
        taskList.refresh();
    }
    private void setNavFilter(String filterType, Button selectedButton) {
        currentFilterType = filterType;
        sidebar.getChildren().stream()
                .filter(node -> node instanceof Button)
                .map(node -> (Button) node)
                .forEach(btn -> btn.getStyleClass().remove("selected"));
        selectedButton.getStyleClass().add("selected");
        applyFilters();
    }
    private void applyFilters() {
        String searchText = (searchField != null && searchField.getText() != null)
                ? searchField.getText().toLowerCase().trim() : "";
        filteredTasks.setPredicate(task -> {
            if (task == null) return false;
            if (SPACER_TITLE.equals(task.getTitle())) return true;
            if (!isNavFilterMatch(task)) return false;
            if (searchText.isEmpty()) return true;
            String title = task.getTitle() == null ? "" : task.getTitle().toLowerCase();
            String desc = task.getDescription() == null ? "" : task.getDescription().toLowerCase();
            return title.contains(searchText) || desc.contains(searchText);
        });
        System.out.println("[DEBUG] applyFilters -> " + currentFilterType + " search='" + searchText + "' remaining=" + filteredTasks.size());
    }
    private boolean isNavFilterMatch(Task task) {
        if (SPACER_TITLE.equals(task.getTitle())) return true;
        boolean isToday = task.getDueDate() != null && task.getDueDate().isEqual(LocalDate.now());
        switch (currentFilterType) {
            case "TODAY": return isToday;
            case "IMPORTANT": return task.isImportant();
            case "FINISHED": return task.isCompleted();
            case "PENDING": return !task.isCompleted();
            default: return true;
        }
    }
    private void loadTasks() {
        try {
            var loaded = dataManager.load(DATA_FILE);
            if (loaded != null) masterTasks.addAll(loaded);
        } catch (Exception ex) {
            System.err.println("[ERROR] dataManager.load failed: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    private void saveTasks() {
        try {
            var toSaveList = masterTasks.stream()
                    .filter(t -> t != null && !SPACER_TITLE.equals(t.getTitle()))
                    .collect(Collectors.toList());
            ObservableList<Task> toSave = FXCollections.observableArrayList(toSaveList);
            dataManager.save(DATA_FILE, toSave);
        } catch (Exception ex) {
            System.err.println("[ERROR] dataManager.save failed: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    @FXML private void handleExit() { saveAndExit(); }
    @FXML
    private void handleDeleteCompleted() {
        ButtonType confirmResult = showCustomAlert(
                "Clear Completed Tasks",
                "Delete all completed tasks?",
                "This cannot be undone."
        );
        if (confirmResult == ButtonType.OK) {
            masterTasks.removeIf(t -> t != null && t.isCompleted() && !SPACER_TITLE.equals(t.getTitle()));
            applyFilters();
            saveTasks();
            taskList.refresh();
        }
    }
    @FXML
    private void handleHelp() {
        if (root != null && root.getScene() != null && root.getScene().getWindow() != null) {
            AboutDialogController.showAboutDialog(root.getScene().getWindow());
        } else {
            Alert tempAlert = new Alert(AlertType.INFORMATION);
            tempAlert.setTitle("信息");
            tempAlert.setHeaderText(null);
            tempAlert.setContentText("无法加载关于对话框，请检查资源文件是否完整。");
            tempAlert.getButtonTypes().setAll(ButtonType.OK);
            tempAlert.showAndWait();
        }
    }
    @FXML
    public void saveAndExit() {
        try {
            saveTasks();
            Platform.exit();
            System.exit(0);
        } catch (Exception e) {
            Alert errorAlert = new Alert(AlertType.ERROR);
            errorAlert.setTitle("错误");
            errorAlert.setHeaderText(null);
            errorAlert.setContentText("保存任务失败，退出失败：" + e.getMessage());
            errorAlert.showAndWait();
            System.exit(1);
        }
    }
    @FXML
    private void handleToggleTheme() {
        Scene scene = root.getScene();
        String gradientPath = getClass().getResource("/com/mytodo/Main.css").toExternalForm();
        if (scene.getStylesheets().contains(gradientPath)) {
            scene.getStylesheets().remove(gradientPath);
            System.out.println("[UI] Switched to Classic Theme");
        } else {
            scene.getStylesheets().add(gradientPath);
            System.out.println("[UI] Switched to Gradient Theme");
        }
    }
    @FXML public void onQuickAdd() { addQuickTask(); }
    @FXML public void onAddDetails() { openTaskDetailDialog(null); }
    @FXML public void onSearchClicked() { performSearch(); }
    @FXML public void onClearSearch() { searchField.clear(); applyFilters(); }
    @FXML public void onFilterToday() { setNavFilter("TODAY", btnToday); }
    @FXML public void onFilterImportant() { setNavFilter("IMPORTANT", btnImportant); }
    @FXML public void onFilterAll() { setNavFilter("ALL", btnAll); }
    @FXML public void onFilterPending() { setNavFilter("PENDING", btnPending); }
    @FXML public void onFilterFinished() { setNavFilter("FINISHED", btnFinished); }
}