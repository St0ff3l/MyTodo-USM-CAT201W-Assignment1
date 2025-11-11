package com.mytodo;

import com.mytodo.util.JsonDataManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
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
 * MainController.java - 完整版本（含 spacer 支持与保存过滤）
 */
public class MainController {

    // --- FXML BINDINGS ---
    @FXML private TextField searchField, quickAddField;
    @FXML private Button quickAddBtn, detailAddBtn, filterBtn; // filterBtn 是“搜索”按钮
    @FXML private Button searchClearBtn; // 新增：⟲ 清空搜索按钮
    @FXML private Button btnToday, btnImportant, btnAll, btnFinished, btnPending;
    @FXML private ListView<Task> taskList;
    @FXML private VBox sidebar;
    @FXML private HBox floatingAddBox;

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

    @FXML
    private void initialize() {
        // 加载任务
        try {
            loadTasks();
        } catch (Exception ex) {
            System.err.println("[ERROR] loadTasks failed: " + ex.getMessage());
            ex.printStackTrace();
        }

        // 确保只有一个 spacer，并放在最后（用于滚动缓冲）
        ensureSpacerExists();

        // ListView 绑定
        taskList.setItems(filteredTasks);
        taskList.setCellFactory(list -> new TaskListCell(this));
        VBox.setVgrow(taskList, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(taskList, javafx.scene.layout.Priority.ALWAYS);
        taskList.setFocusTraversable(true);

        // 浮动添加栏（保持悬浮外观）
        if (floatingAddBox != null) {
            floatingAddBox.setPickOnBounds(false);
            floatingAddBox.setVisible(true);
            floatingAddBox.setManaged(true);
        }

        // 搜索（回车或按钮点击触发）
        if (searchField != null) {
            searchField.setOnAction(e -> performSearch());
        }
        if (filterBtn != null) {
            filterBtn.setOnAction(e -> performSearch());
        }

        // ⟲ 清空搜索按钮
        if (searchClearBtn != null) {
            searchClearBtn.setOnAction(e -> {
                if (searchField != null) searchField.clear();
                applyFilters(); // 清空搜索后恢复默认过滤
                if (taskList != null) taskList.refresh();
                System.out.println("[DEBUG] Search cleared and filters reapplied.");
            });
        }

        // 左侧导航按钮
        if (btnAll != null) btnAll.setOnAction(e -> setNavFilter("ALL", btnAll));
        if (btnToday != null) btnToday.setOnAction(e -> setNavFilter("TODAY", btnToday));
        if (btnImportant != null) btnImportant.setOnAction(e -> setNavFilter("IMPORTANT", btnImportant));
        if (btnFinished != null) btnFinished.setOnAction(e -> setNavFilter("FINISHED", btnFinished));
        if (btnPending != null) btnPending.setOnAction(e -> setNavFilter("PENDING", btnPending));

        // 默认过滤
        if (btnAll != null) setNavFilter("ALL", btnAll);

        // 快速添加与详细添加
        if (quickAddBtn != null) quickAddBtn.setOnAction(e -> addQuickTask());
        if (quickAddField != null) quickAddField.setOnAction(e -> addQuickTask());
        if (detailAddBtn != null) detailAddBtn.setOnAction(e -> openTaskDetailDialog(null));
    }

    // 确保 spacer 存在且位于 masterTasks 末尾
    private void ensureSpacerExists() {
        // 移除历史可能遗留的 spacer，然后重新添加一个在末尾
        masterTasks.removeIf(t -> t != null && SPACER_TITLE.equals(t.getTitle()));
        Task spacer = new Task(SPACER_TITLE, "", null, null, "Normal");
        masterTasks.add(spacer);
    }

    // 搜索
    private void performSearch() {
        applyFilters();
        System.out.println("[DEBUG] performSearch done. results=" + filteredTasks.size());
    }

    // --- CRUD ---

    private void addQuickTask() {
        String text = quickAddField.getText();
        if (text == null || text.isBlank()) return;

        // 在 spacer 之前插入新任务（避免把 spacer 挤掉到中间）
        int insertPos = Math.max(0, masterTasks.size() - 1);
        Task task = new Task(text.trim(), "", LocalDate.now(), DEFAULT_END_OF_DAY_TIME, "Normal");
        masterTasks.add(insertPos, task);

        quickAddField.clear();
        saveTasks();
        applyFilters();
        if (taskList != null) taskList.refresh();
    }

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
                        // 新增：插入到 spacer 之前
                        int insertPos = Math.max(0, masterTasks.size() - 1);
                        masterTasks.add(insertPos, updatedTask);
                        new Alert(AlertType.INFORMATION, "✅ Task added: " + updatedTask.getTitle()).show();
                    } else {
                        // 编辑：已在原对象上修改（TaskDetailController 应修改原 task 或返回新的）
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

    public void deleteTask(Task task) {
        if (task == null) return;
        // 防止误删 spacer
        if (SPACER_TITLE.equals(task.getTitle())) return;

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

    public void toggleCompletion(Task task) {
        if (task == null) return;
        if (SPACER_TITLE.equals(task.getTitle())) return;
        task.setCompleted(!task.isCompleted());
        saveTasks();
        applyFilters();
        if (taskList != null) taskList.refresh();
    }

    // --- Filtering logic (left-nav + search box) ---

    private void setNavFilter(String filterType, Button selectedButton) {
        currentFilterType = filterType;

        if (sidebar != null) {
            sidebar.getChildren().stream()
                    .filter(node -> node instanceof Button)
                    .map(node -> (Button) node)
                    .forEach(btn -> btn.getStyleClass().remove("selected"));
        }

        if (selectedButton != null) {
            selectedButton.getStyleClass().add("selected");
        }

        applyFilters();
    }

    private void applyFilters() {
        String searchText = (searchField != null && searchField.getText() != null)
                ? searchField.getText().toLowerCase().trim() : "";

        filteredTasks.setPredicate(task -> {
            if (task == null) return false;
            // spacer 始终通过过滤（保留占位）
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
        boolean isToday = false;
        try {
            if (task.getDueDate() != null) {
                isToday = task.getDueDate().isEqual(LocalDate.now());
            }
        } catch (Exception ex) {
            System.err.println("[WARN] isNavFilterMatch: error checking isToday for task '" + (task.getTitle()==null?"":task.getTitle()) + "': " + ex);
        }

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
        try {
            java.util.List<Task> loaded = dataManager.load(DATA_FILE);
            if (loaded != null) masterTasks.addAll(loaded);
        } catch (Exception ex) {
            System.err.println("[ERROR] dataManager.load failed: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void saveTasks() {
        try {
            // 保存前过滤掉 spacer 项，防止写入 JSON 文件
            java.util.List<Task> toSaveList = masterTasks.stream()
                    .filter(t -> t != null && !SPACER_TITLE.equals(t.getTitle()))
                    .collect(Collectors.toList());

            // 将 java.util.List 转成 ObservableList 传给 dataManager
            ObservableList<Task> toSave = FXCollections.observableArrayList(toSaveList);
            dataManager.save(DATA_FILE, toSave);
        } catch (Exception ex) {
            System.err.println("[ERROR] dataManager.save failed: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // used by Main.java to ensure save on exit
    public void saveAndExit() {
        saveTasks();
        Platform.exit();
        System.exit(0);
    }

    // --- Menu actions ---

    @FXML private void handleExit() { saveAndExit(); }

    @FXML
    private void handleDeleteCompleted() {
        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle("Clear Completed Tasks");
        confirm.setHeaderText("Delete all completed tasks?");
        confirm.setContentText("This cannot be undone.");

        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            // 仅删除真实任务（保留 spacer）
            masterTasks.removeIf(t -> t != null && t.isCompleted() && !SPACER_TITLE.equals(t.getTitle()));
            applyFilters();
            saveTasks();
            if (taskList != null) taskList.refresh();
        }
    }

    @FXML private void handleHelp() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("About MyTodo");
        alert.setHeaderText("CAT201 Integrated Software Development Workshop Assignment I");
        alert.setContentText("Version: v2.1 (JavaFX)\nFeatures: Task Management, Search & Filter, JSON I/O\nTeam: [add team members here]");
        alert.showAndWait();
    }

    // --- FXML-accessible wrappers (if your FXML references these directly) ---
    @FXML public void onQuickAdd() { addQuickTask(); }
    @FXML public void onAddDetails() { openTaskDetailDialog(null); }
    @FXML public void onSearchClicked() { performSearch(); }
    @FXML public void onClearSearch() { if (searchField != null) searchField.clear(); applyFilters(); }
    @FXML public void onFilterToday() { setNavFilter("TODAY", btnToday); }
    @FXML public void onFilterImportant() { setNavFilter("IMPORTANT", btnImportant); }
    @FXML public void onFilterAll() { setNavFilter("ALL", btnAll); }
    @FXML public void onFilterPending() { setNavFilter("PENDING", btnPending); }
    @FXML public void onFilterFinished() { setNavFilter("FINISHED", btnFinished); }
}
