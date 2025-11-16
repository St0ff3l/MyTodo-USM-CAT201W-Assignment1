package com.mytodo;

// ---------------------------------------------------------------------
// 导入 (Imports)
// ---------------------------------------------------------------------

// JavaFX 核心
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.control.ContextMenu; // 🌟 1. [新增] 导入 ContextMenu
import javafx.scene.control.MenuItem;   // 🌟 2. [新增] 导入 MenuItem

// Java 标准库
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// 本项目特定类
import com.mytodo.util.JsonDataManager;
import com.mytodo.SuccessMessageDialogController;
import com.mytodo.AddNewListDialogController;


/**
 * 主界面的控制器 (MainController)。
 * 负责处理所有用户交互、数据管理和UI更新。
 */
public class MainController {

    // (所有 FXML 绑定 和 字段 保持不变)
    @FXML private VBox root;
    @FXML private VBox sidebar;
    @FXML private Button btnToday, btnImportant, btnAll, btnFinished, btnPending;
    @FXML private VBox listContainer;
    @FXML private Button addNewListButton;
    @FXML private ListView<Task> taskList;
    @FXML private TextField searchField;
    @FXML private Button filterBtn;
    @FXML private Button searchClearBtn;
    @FXML private HBox floatingAddBox;
    @FXML private TextField quickAddField;
    @FXML private Button quickAddBtn;
    @FXML private Button detailAddBtn;

    private final ObservableList<Task> masterTasks = FXCollections.observableArrayList();
    private final ObservableList<String> masterLists = FXCollections.observableArrayList();
    private final FilteredList<Task> filteredTasks = new FilteredList<>(masterTasks, t -> true);
    private String currentFilterType = "ALL";
    private String activeListFilter = null;

    private static final File DATA_FILE = new File("tasks.json");
    private static final File LISTS_DATA_FILE = new File("lists.json");
    private final JsonDataManager dataManager = new JsonDataManager();
    private final LocalTime DEFAULT_END_OF_DAY_TIME = LocalTime.of(23, 59);
    private static final String SPACER_TITLE = "(SPACER_ITEM)";


    // =========================================================================
    // 4. 初始化
    // =========================================================================

    @FXML
    private void initialize() {
        System.out.println("[DEBUG] MainController initializing...");
        loadLists();
        try {
            loadTasks();
            System.out.println("[DEBUG] Tasks loaded. Count: " + masterTasks.size());
        } catch (Exception ex) {
            System.err.println("[ERROR] loadTasks failed during initialization: " + ex.getMessage());
            ex.printStackTrace();
        }
        ensureSpacerExists();
        taskList.setItems(filteredTasks);
        taskList.setCellFactory(list -> new TaskListCell(this));
        VBox.setVgrow(taskList, Priority.ALWAYS);
        HBox.setHgrow(taskList, Priority.ALWAYS);
        bindActionEvents();
        updateListSidebar();
        setNavFilter("ALL", btnAll);
        System.out.println("[DEBUG] Initialization complete.");
    }

    /**
     * 辅助方法：集中管理所有 FXML 元素的事件绑定。
     */
    private void bindActionEvents() {
        // (所有绑定保持不变)
        if (searchField != null) searchField.setOnAction(e -> performSearch());
        if (filterBtn != null) filterBtn.setOnAction(e -> performSearch());
        if (searchClearBtn != null) {
            searchClearBtn.setOnAction(e -> {
                searchField.clear();
                applyFilters();
                taskList.refresh();
                System.out.println("[DEBUG] Search cleared.");
            });
        }
        if (btnAll != null) btnAll.setOnAction(e -> setNavFilter("ALL", btnAll));
        if (btnToday != null) btnToday.setOnAction(e -> setNavFilter("TODAY", btnToday));
        if (btnImportant != null) btnImportant.setOnAction(e -> setNavFilter("IMPORTANT", btnImportant));
        if (btnFinished != null) btnFinished.setOnAction(e -> setNavFilter("FINISHED", btnFinished));
        if (btnPending != null) btnPending.setOnAction(e -> setNavFilter("PENDING", btnPending));
        if (quickAddBtn != null) quickAddBtn.setOnAction(e -> addQuickTask());
        if (quickAddField != null) quickAddField.setOnAction(e -> addQuickTask());
        if (detailAddBtn != null) detailAddBtn.setOnAction(e -> openTaskDetailDialog(null));
        if (addNewListButton != null) addNewListButton.setOnAction(e -> handleAddNewList());
    }


    // =========================================================================
    // 5. 核心任务操作 (增 / 删 / 改)
    // =========================================================================

    /**
     * 快速添加任务 (从底部浮动栏)
     */
    private void addQuickTask() {
        String text = quickAddField.getText();
        if (text == null || text.isBlank()) return;

        int insertPos = Math.max(0, masterTasks.size() - 1);
        Task task = new Task(
                text.trim(), "", LocalDate.now(), DEFAULT_END_OF_DAY_TIME, "Normal"
        );

        // (无 "Inbox" 默认值逻辑)
        if ("LIST".equals(currentFilterType) && activeListFilter != null) {
            task.setListName(activeListFilter);
        }

        masterTasks.add(insertPos, task);
        quickAddField.clear();
        saveTasks();
        applyFilters();
        taskList.refresh();
    }

    /**
     * [PUBLIC] 删除一个任务。
     */
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
            System.out.println("[DEBUG] Task deleted: " + task.getTitle());
        }
    }

    /**
     * [PUBLIC] 切换任务的完成状态。
     */
    public void toggleCompletion(Task task) {
        if (task == null || SPACER_TITLE.equals(task.getTitle())) return;
        task.setCompleted(!task.isCompleted());
        saveTasks();
        applyFilters();
        taskList.refresh();
    }

    /**
     * [PUBLIC] 打开任务详情对话框 (用于添加或编辑)。
     */
    public void openTaskDetailDialog(Task taskToEdit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mytodo/TaskDetailDialog.fxml"));
            DialogPane pane = loader.load();
            TaskDetailController controller = loader.getController();

            // 传递 masterLists
            controller.loadData(taskToEdit, masterLists);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle(taskToEdit == null ? "Add Task" : "Edit Task");
            dialog.setDialogPane(pane);
            pane.getButtonTypes().clear();
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/mytodo/Main.css").toExternalForm());
            dialog.showAndWait();

            if (controller.isOkClicked()) {
                Task updatedTask = controller.getTask();
                if (updatedTask != null) {
                    String msg = (taskToEdit == null) ? "Task added: " : "Task updated: ";
                    showSuccessAlert(msg + updatedTask.getTitle(), null);

                    if (taskToEdit == null) {
                        int insertPos = Math.max(0, masterTasks.size() - 1);
                        masterTasks.add(insertPos, updatedTask);
                    } else {
                        taskList.refresh();
                    }
                    saveTasks();
                    applyFilters();
                    taskList.refresh();
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            showCustomAlert("Error", "Unexpected error", "Failed to open task dialog: " + ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            showCustomAlert("Error", "Unexpected error", "Unexpected error: " + ex.getMessage());
        }
    }


    // =========================================================================
    // 6. 弹窗与对话框管理 (Alerts & Dialogs)
    // =========================================================================

    /**
     * [新] 显示一个只带 "OK" 按钮的成功消息弹窗。
     */
    private void showSuccessAlert(String header, String content) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mytodo/successMessageDialogView.fxml"));
            DialogPane pane = loader.load();
            SuccessMessageDialogController controller = loader.getController();
            controller.setSuccessMessage(header, content);
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Success");
            dialog.setDialogPane(pane);
            pane.getButtonTypes().clear();
            dialog.showAndWait();
        } catch (IOException ex) {
            ex.printStackTrace();
            Alert fallback = new Alert(AlertType.INFORMATION, content);
            fallback.setTitle("Success");
            fallback.setHeaderText(header);
            fallback.showAndWait();
        }
    }

    /**
     * 显示一个带 "OK" 和 "Cancel" 按钮的通用确认弹窗。
     */
    private ButtonType showCustomAlert(String title, String header, String content) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mytodo/CustomAlertDialogView.fxml"));
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

    /**
     * [已重构] 使用我们的自定义 FXML 弹窗 (AddNewListDialogView.fxml)
     */
    @FXML
    private void handleAddNewList() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mytodo/AddNewListDialogView.fxml"));
            DialogPane pane = loader.load();
            pane.getStylesheets().add(getClass().getResource("/com/mytodo/Main.css").toExternalForm());
            AddNewListDialogController controller = loader.getController();
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("New List");
            dialog.setDialogPane(pane);
            pane.getButtonTypes().clear();
            dialog.showAndWait();

            if (controller.isOkClicked()) {
                String newName = controller.getNewListName();
                if (masterLists.stream().anyMatch(list -> list.equalsIgnoreCase(newName))) {
                    showCustomAlert("Error", "List already exists.", "A list with this name already exists.");
                    return;
                }
                masterLists.add(newName);
                saveLists();
                updateListSidebar();
                System.out.println("[DEBUG] New list added: " + newName);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            showCustomAlert("Error", "Load Error", "Failed to load the 'Add New List' dialog.");
        }
    }


    // =========================================================================
    // 7. 过滤与搜索逻辑 (Filtering & Search)
    // =========================================================================

    /**
     * 执行搜索 (由搜索框回车或点击按钮触发)
     */
    private void performSearch() {
        applyFilters();
        System.out.println("[DEBUG] performSearch done. results=" + filteredTasks.size());
    }

    /**
     * [已修改] 设置侧边栏的导航过滤器 (例如 "Today", "Important")
     */
    private void setNavFilter(String filterType, Button selectedButton) {
        activeListFilter = null;
        currentFilterType = filterType;
        clearAllSidebarSelections();
        selectedButton.getStyleClass().add("selected");
        applyFilters();
    }

    /**
     * [已重命名/重构] 核心方法：设置侧边栏的列表过滤器
     */
    private void setListFilter(String listName, Button selectedButton) {
        currentFilterType = "LIST";
        activeListFilter = listName;
        clearAllSidebarSelections();
        selectedButton.getStyleClass().add("selected");
        applyFilters();
        System.out.println("[DEBUG] List filter set: " + listName);
    }

    /**
     * [已重命名] 辅助方法：清除所有侧边栏按钮的选中状态
     */
    private void clearAllSidebarSelections() {
        sidebar.getChildren().stream()
                .filter(node -> node instanceof Button)
                .map(node -> (Button) node)
                .forEach(btn -> btn.getStyleClass().remove("selected"));

        if (listContainer != null) {
            listContainer.getChildren().stream()
                    .filter(node -> node instanceof Button)
                    .map(node -> (Button) node)
                    .forEach(btn -> btn.getStyleClass().remove("selected"));
        }
    }

    /**
     * 核心过滤方法。
     */
    private void applyFilters() {
        String searchText = (searchField != null && searchField.getText() != null)
                ? searchField.getText().toLowerCase().trim() : "";
        filteredTasks.setPredicate(task -> {
            if (task == null) return false;
            if (SPACER_TITLE.equals(task.getTitle())) return true;
            if (!isNavFilterMatch(task)) return false;
            if (searchText.isEmpty()) return true;
            String title = (task.getTitle() == null) ? "" : task.getTitle().toLowerCase();
            String desc = (task.getDescription() == null) ? "" : task.getDescription().toLowerCase();
            return title.contains(searchText) || desc.contains(searchText);
        });
        System.out.println("[DEBUG] applyFilters -> " + currentFilterType + " search='" + searchText + "' remaining=" + filteredTasks.size());
    }

    /**
     * [已修改] 辅助方法：检查任务是否匹配过滤器
     */
    private boolean isNavFilterMatch(Task task) {
        if (SPACER_TITLE.equals(task.getTitle())) return true;
        boolean isToday = task.getDueDate() != null && task.getDueDate().isEqual(LocalDate.now());
        switch (currentFilterType) {
            case "TODAY":     return isToday;
            case "IMPORTANT": return task.isImportant();
            case "FINISHED":  return task.isCompleted();
            case "PENDING":   return !task.isCompleted();
            case "LIST":
                if (activeListFilter == null) return true;
                return activeListFilter.equals(task.getListName());
            case "ALL":
            default:
                return true;
        }
    }


    // =========================================================================
    // 8. 数据持久化 (Load / Save)
    // =========================================================================

    /**
     * 从 tasks.json 文件加载任务到 `masterTasks` 列表。
     */
    private void loadTasks() {
        try {
            var loaded = dataManager.load(DATA_FILE);
            if (loaded != null) {
                masterTasks.addAll(loaded);
            }
        } catch (Exception ex) {
            System.err.println("[ERROR] dataManager.load failed: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * 将 `masterTasks` 列表中的所有真实任务保存到 tasks.json 文件。
     */
    private void saveTasks() {
        try {
            var toSaveList = masterTasks.stream()
                    .filter(t -> t != null && !SPACER_TITLE.equals(t.getTitle()))
                    .collect(Collectors.toList());
            ObservableList<Task> toSave = FXCollections.observableArrayList(toSaveList);
            dataManager.save(DATA_FILE, toSave);
            System.out.println("[DEBUG] Tasks saved. Count: " + toSave.size());
        } catch (Exception ex) {
            System.err.println("[ERROR] dataManager.save failed: " + ex.getMessage());
            ex.printStackTrace();
            showCustomAlert("Save Error", "Failed to save tasks", "Your changes might be lost. Error: " + ex.getMessage());
        }
    }

    /**
     * 确保 "幽灵" 项始终存在于 `masterTasks` 列表的末尾。
     */
    private void ensureSpacerExists() {
        masterTasks.removeIf(t -> t != null && SPACER_TITLE.equals(t.getTitle()));
        Task spacer = new Task(SPACER_TITLE, "", null, null, "Normal");
        masterTasks.add(spacer);
    }

    /**
     * [全新/已修改] 加载 lists.json (无默认值)
     */
    private void loadLists() {
        if (!LISTS_DATA_FILE.exists()) {
            System.out.println("[DEBUG] lists.json not found. No lists loaded.");
            return;
        }
        try {
            List<String> loaded = Files.readAllLines(LISTS_DATA_FILE.toPath());
            masterLists.clear();
            masterLists.addAll(loaded);
            System.out.println("[DEBUG] Lists loaded from lists.json. Count: " + loaded.size());
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to load lists.json: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * [全新] 保存 lists.json
     */
    private void saveLists() {
        try {
            Files.write(LISTS_DATA_FILE.toPath(), masterLists);
            System.out.println("[DEBUG] Lists saved to lists.json.");
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to save lists.json: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 🌟 3. [已修改] 核心方法：更新侧边栏的动态列表 (添加右键删除)
     */
    private void updateListSidebar() {
        if (listContainer == null) {
            System.err.println("[ERROR] listContainer is null. Cannot update list.");
            return;
        }

        listContainer.getChildren().clear();

        for (String listName : masterLists) {
            Button listButton = new Button(listName);
            listButton.setMaxWidth(Double.MAX_VALUE);
            listButton.getStyleClass().add("nav-item");
            listButton.setOnAction(event -> setListFilter(listName, listButton));

            // 🌟 [新增] 添加右键删除功能
            ContextMenu contextMenu = new ContextMenu();
            MenuItem deleteItem = new MenuItem("Delete List");
            deleteItem.setOnAction(event -> deleteList(listName));
            contextMenu.getItems().add(deleteItem);

            listButton.setContextMenu(contextMenu);
            // 🌟 [新增结束]

            listContainer.getChildren().add(listButton);
        }

        System.out.println("[DEBUG] List sidebar updated. Found " + masterLists.size() + " lists.");
    }

    /**
     * 🌟 4. [全新] 删除一个列表的完整逻辑
     */
    private void deleteList(String listName) {
        // 1. 确认
        ButtonType confirmResult = showCustomAlert(
                "Delete List",
                "Are you sure to delete the list: " + listName + "?",
                "All tasks in this list will be moved to 'Unlisted'."
        );

        if (confirmResult != ButtonType.OK) {
            return;
        }

        // 2. 从 masterLists 中删除
        masterLists.remove(listName);

        // 3. 将所有关联的任务 "孤立" (将其 listName 设为 null)
        for (Task task : masterTasks) {
            if (listName.equals(task.getListName())) {
                task.setListName(null);
            }
        }

        // 4. 保存所有更改
        saveLists();     // 保存 "lists.json"
        saveTasks();     // 保存 "tasks.json" (因为任务的 listName 已更改)

        // 5. 刷新 UI
        updateListSidebar(); // 刷新侧边栏

        // 6. 如果删除的是当前正在查看的列表，则重置视图到 "All"
        if (listName.equals(activeListFilter)) {
            setNavFilter("ALL", btnAll);
        } else {
            // 否则，只需刷新当前视图
            applyFilters();
        }

        System.out.println("[DEBUG] List deleted: " + listName);
    }


    // =========================================================================
    // 9. FXML 事件处理器 (菜单栏 & 快捷方式)
    // =========================================================================

    @FXML private void handleExit() {
        saveAndExit();
    }

    /**
     * [已修改] 菜单栏 Edit -> Delete All Completed
     */
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
            System.out.println("[DEBUG] All completed tasks deleted.");
        }
    }

    /**
     * (handleToggleTheme 保持不变)
     */
    @FXML
    private void handleToggleTheme() {
        Scene scene = root.getScene();
        if (scene == null) return;

        String gradientPath = getClass().getResource("/com/mytodo/Main.css").toExternalForm();
        if (scene.getStylesheets().contains(gradientPath)) {
            scene.getStylesheets().remove(gradientPath);
            System.out.println("[UI] Switched to Classic Theme (Default JavaFX)");
        } else {
            scene.getStylesheets().add(gradientPath);
            System.out.println("[UI] Switched to Custom Theme (Main.css)");
        }
    }

    /**
     * (handleHelp 保持不变)
     */
    @FXML
    private void handleHelp() {
        if (root != null && root.getScene() != null && root.getScene().getWindow() != null) {
            AboutDialogController.showAboutDialog(root.getScene().getWindow());
        } else {
            Alert tempAlert = new Alert(AlertType.INFORMATION);
            tempAlert.setTitle("About");
            tempAlert.setHeaderText(null);
            tempAlert.setContentText("MyTodo Application v1.0");
            tempAlert.showAndWait();
        }
    }

    /**
     * [PUBLIC] 保存任务并安全退出应用程序。
     */
    @FXML
    public void saveAndExit() {
        System.out.println("[DEBUG] Save and Exit requested...");
        try {
            saveTasks();
            saveLists(); // 🌟 5. [新增] 退出时也要保存列表
            Platform.exit();
            System.exit(0);
        } catch (Exception e) {
            Alert errorAlert = new Alert(AlertType.ERROR);
            errorAlert.setTitle("Error");
            errorAlert.setHeaderText("Failed to save tasks on exit");
            errorAlert.setContentText("Error: " + e.getMessage());
            errorAlert.showAndWait();
            System.exit(1);

        }
    }

    // --- FXML 快捷方式 (用于 SceneBuilder 'onAction'，避免使用 lambda) ---

    @FXML public void onQuickAdd() { addQuickTask(); }
    @FXML public void onAddDetails() { openTaskDetailDialog(null); }
    @FXML public void onSearchClicked() { performSearch(); }
    @FXML public void onClearSearch() {
        if(searchField != null) searchField.clear();
        applyFilters();
    }
    @FXML public void onFilterToday() { setNavFilter("TODAY", btnToday); }
    @FXML public void onFilterImportant() { setNavFilter("IMPORTANT", btnImportant); }
    @FXML public void onFilterAll() { setNavFilter("ALL", btnAll); }
    @FXML public void onFilterPending() { setNavFilter("PENDING", btnPending); }
    @FXML public void onFilterFinished() { setNavFilter("FINISHED", btnFinished); }
}