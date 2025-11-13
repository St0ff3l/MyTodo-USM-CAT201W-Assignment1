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

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MainController.java - 完整版本（含 spacer、过滤、搜索、保存、主题切换）
 */
public class MainController {

    // --- FXML BINDINGS ---
    @FXML private TextField searchField, quickAddField;
    @FXML private Button quickAddBtn, detailAddBtn, filterBtn;
    @FXML private Button searchClearBtn;
    @FXML private Button btnToday, btnImportant, btnAll, btnFinished, btnPending;
    @FXML private ListView<Task> taskList;
    @FXML private VBox sidebar;
    @FXML private HBox floatingAddBox;
    @FXML private VBox root; // 新增：用于主题切换的根节点

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
        // 加载任务
        try {
            loadTasks();
        } catch (Exception ex) {
            System.err.println("[ERROR] loadTasks failed: " + ex.getMessage());
            ex.printStackTrace();
        }

        ensureSpacerExists();

        // ListView
        taskList.setItems(filteredTasks);
        taskList.setCellFactory(list -> new TaskListCell(this));
        VBox.setVgrow(taskList, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(taskList, javafx.scene.layout.Priority.ALWAYS);

        // 搜索逻辑
        if (searchField != null) {
            searchField.setOnAction(e -> performSearch());
        }
        if (filterBtn != null) {
            filterBtn.setOnAction(e -> performSearch());
        }

        // 清空搜索按钮
        if (searchClearBtn != null) {
            searchClearBtn.setOnAction(e -> {
                searchField.clear();
                applyFilters();
                taskList.refresh();
                System.out.println("[DEBUG] Search cleared and filters reapplied.");
            });
        }

        // 左侧导航
        if (btnAll != null) btnAll.setOnAction(e -> setNavFilter("ALL", btnAll));
        if (btnToday != null) btnToday.setOnAction(e -> setNavFilter("TODAY", btnToday));
        if (btnImportant != null) btnImportant.setOnAction(e -> setNavFilter("IMPORTANT", btnImportant));
        if (btnFinished != null) btnFinished.setOnAction(e -> setNavFilter("FINISHED", btnFinished));
        if (btnPending != null) btnPending.setOnAction(e -> setNavFilter("PENDING", btnPending));

        // 默认过滤
        setNavFilter("ALL", btnAll);

        // 快速添加
        if (quickAddBtn != null) quickAddBtn.setOnAction(e -> addQuickTask());
        if (quickAddField != null) quickAddField.setOnAction(e -> addQuickTask());
        if (detailAddBtn != null) detailAddBtn.setOnAction(e -> openTaskDetailDialog(null));
    }

    // 确保 spacer 存在且在最后
    private void ensureSpacerExists() {
        masterTasks.removeIf(t -> t != null && SPACER_TITLE.equals(t.getTitle()));
        Task spacer = new Task(SPACER_TITLE, "", null, null, "Normal");
        masterTasks.add(spacer);
    }

    // --- 搜索 ---
    private void performSearch() {
        applyFilters();
        System.out.println("[DEBUG] performSearch done. results=" + filteredTasks.size());
    }

    // --- 添加任务 ---
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

    // --- 打开详情对话框 ---
    public void openTaskDetailDialog(Task taskToEdit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("TaskDetailDialog.fxml"));
            DialogPane pane = loader.load();
            TaskDetailController controller = loader.getController();
            controller.loadData(taskToEdit);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle(taskToEdit == null ? "Add Task" : "Edit Task");
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
                        int insertPos = Math.max(0, masterTasks.size() - 1);
                        masterTasks.add(insertPos, updatedTask);
                        new Alert(AlertType.INFORMATION, "✅ Task added: " + updatedTask.getTitle()).show();
                    } else {
                        taskList.refresh();
                        new Alert(AlertType.INFORMATION, "✏️ Task updated: " + updatedTask.getTitle()).show();
                    }
                    saveTasks();
                    applyFilters();
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            new Alert(AlertType.ERROR, "Failed to open task dialog: " + ex.getMessage()).showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(AlertType.ERROR, "Unexpected error: " + ex.getMessage()).showAndWait();
        }
    }

    // --- 删除任务 ---
    public void deleteTask(Task task) {
        if (task == null || SPACER_TITLE.equals(task.getTitle())) return;

        Alert confirm = new Alert(AlertType.CONFIRMATION, "Are you sure to delete: " + task.getTitle() + " ?", ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("Delete Confirmation");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                masterTasks.remove(task);
                saveTasks();
                applyFilters();
                taskList.refresh();
            }
        });
    }

    // --- 切换完成状态 ---
    public void toggleCompletion(Task task) {
        if (task == null || SPACER_TITLE.equals(task.getTitle())) return;
        task.setCompleted(!task.isCompleted());
        saveTasks();
        applyFilters();
        taskList.refresh();
    }

    // --- 左侧过滤逻辑 ---
    private void setNavFilter(String filterType, Button selectedButton) {
        currentFilterType = filterType;
        sidebar.getChildren().stream()
                .filter(node -> node instanceof Button)
                .map(node -> (Button) node)
                .forEach(btn -> btn.getStyleClass().remove("selected"));

        selectedButton.getStyleClass().add("selected");
        applyFilters();
    }

    // --- 应用过滤条件 ---
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

    // --- JSON I/O ---
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

    // --- 菜单功能 ---
    @FXML private void handleExit() { saveAndExit(); }

    @FXML
    private void handleDeleteCompleted() {
        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle("Clear Completed Tasks");
        confirm.setHeaderText("Delete all completed tasks?");
        confirm.setContentText("This cannot be undone.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            masterTasks.removeIf(t -> t != null && t.isCompleted() && !SPACER_TITLE.equals(t.getTitle()));
            applyFilters();
            saveTasks();
            taskList.refresh();
        }
    }

    @FXML
    private void handleHelp() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("About MyTodo");
        alert.setHeaderText("CAT201 Integrated Software Development Workshop Assignment I");
        alert.setContentText("Version: v2.1 (JavaFX)\nFeatures: Task Management, Search & Filter, JSON I/O\nTeam: [add team members here]");
        alert.showAndWait();
    }

    public void saveAndExit() {
        saveTasks();
        Platform.exit();
        System.exit(0);
    }

    // --- 主题切换功能 ---
    @FXML
    private void handleToggleTheme() {
        Scene scene = root.getScene();
        String gradientPath = getClass().getResource("gradient.css").toExternalForm();

        if (scene.getStylesheets().contains(gradientPath)) {
            scene.getStylesheets().remove(gradientPath);
            System.out.println("[UI] Switched to Classic Theme");
        } else {
            scene.getStylesheets().add(gradientPath);
            System.out.println("[UI] Switched to Gradient Theme");
        }
    }

    // --- FXML wrapper ---
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