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
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.Region;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.geometry.Pos;

// Java 标准库
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
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

    // ==== FXML 绑定 ====
    @FXML private VBox root;
    @FXML private VBox sidebar;
    @FXML private Button btnToday, btnImportant, btnAll, btnFinished, btnPending, btnOverdue;
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

    // 顶部分类数字 Label
    private Label todayCountLabel;
    private Label importantCountLabel;
    private Label allCountLabel;
    private Label pendingCountLabel;
    private Label overdueCountLabel;
    private Label completedCountLabel;

    private final ObservableList<Task> masterTasks = FXCollections.observableArrayList();
    // 保存所有自定义列表（名称 + 图标路径）
    private final ObservableList<ListInfo> masterLists = FXCollections.observableArrayList();
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
        // 图标管理（如果你之前有）

        // 先加载列表，再加载任务
        loadLists();
        try {
            loadTasks();
            System.out.println("[DEBUG] Tasks loaded. Count: " + masterTasks.size());
        } catch (Exception ex) {
            System.err.println("[ERROR] loadTasks failed during initialization: " + ex.getMessage());
            ex.printStackTrace();
        }

        // 保证幽灵占位符存在
        ensureSpacerExists();

        // ListView 绑定
        taskList.setItems(filteredTasks);
        taskList.setCellFactory(list -> new TaskListCell(this));
        VBox.setVgrow(taskList, Priority.ALWAYS);
        HBox.setHgrow(taskList, Priority.ALWAYS);

        // 顶部分类按钮：包上“图标 + 文本 + 右侧数字”
        setupFixedCategoryButtons();

        // 绑定各种事件
        bindActionEvents();

        // 更新列表区域 + 统计数字
        updateFixedCategoryCounts();
        updateListSidebar();

        // 默认选中 All
        setNavFilter("ALL", btnAll);

        System.out.println("[DEBUG] Initialization complete.");
    }

    /**
     * 顶部 6 个分类按钮，统一改成：
     * [icon] [title] ....... [count]
     */
    private void setupFixedCategoryButtons() {
        todayCountLabel     = buildNavButtonWithCount(btnToday,     "Today");
        importantCountLabel = buildNavButtonWithCount(btnImportant, "Important");
        allCountLabel       = buildNavButtonWithCount(btnAll,       "All");
        pendingCountLabel   = buildNavButtonWithCount(btnPending,   "Pending");
        overdueCountLabel   = buildNavButtonWithCount(btnOverdue,   "Overdue");
        completedCountLabel = buildNavButtonWithCount(btnFinished,  "Completed");
    }

    /**
     * 把一个 Button 变成：
     *  [icon] [title] (spacer) [countLabel]
     */
    private Label buildNavButtonWithCount(Button btn, String title) {
        if (btn == null) return null;

        Node icon = btn.getGraphic();   // FXML 里已经放好的 ImageView
        btn.setText("");                // 不用 Button 本身的文字

        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        if (icon != null) {
            row.getChildren().add(icon);
        }

        Label titleLabel = new Label(title);
        // 可以加一个 class（可选）
        titleLabel.getStyleClass().add("nav-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label countLabel = new Label("0");
        countLabel.getStyleClass().add("list-count"); // 用 CSS 控制颜色和字号

        row.getChildren().addAll(titleLabel, spacer, countLabel);
        btn.setGraphic(row);

        return countLabel;
    }

    /**
     * 辅助方法：集中管理所有 FXML 元素的事件绑定。
     * （保留你原来的写法，只是加上 Overdue）
     */
    private void bindActionEvents() {
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
        if (btnAll != null)       btnAll.setOnAction(e -> setNavFilter("ALL", btnAll));
        if (btnToday != null)     btnToday.setOnAction(e -> setNavFilter("TODAY", btnToday));
        if (btnImportant != null) btnImportant.setOnAction(e -> setNavFilter("IMPORTANT", btnImportant));
        if (btnFinished != null)  btnFinished.setOnAction(e -> setNavFilter("FINISHED", btnFinished));
        if (btnPending != null)   btnPending.setOnAction(e -> setNavFilter("PENDING", btnPending));
        if (btnOverdue != null)   btnOverdue.setOnAction(e -> setNavFilter("OVERDUE", btnOverdue));

        if (quickAddBtn != null)   quickAddBtn.setOnAction(e -> addQuickTask());
        if (quickAddField != null) quickAddField.setOnAction(e -> addQuickTask());
        if (detailAddBtn != null)  detailAddBtn.setOnAction(e -> openTaskDetailDialog(null));
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

        if ("LIST".equals(currentFilterType) && activeListFilter != null) {
            task.setListName(activeListFilter);
        }

        masterTasks.add(insertPos, task);
        quickAddField.clear();
        saveTasks();
        applyFilters();
        taskList.refresh();
        updateFixedCategoryCounts();
        updateListSidebar();
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
            updateFixedCategoryCounts();
            updateListSidebar();
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
        updateFixedCategoryCounts();
        updateListSidebar();
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
                    updateFixedCategoryCounts();
                    updateListSidebar();
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
     * 使用自定义 FXML 弹窗创建新的列表（带图标选择）
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
            pane.getButtonTypes().clear(); // 用我们自己的 OK/Cancel
            dialog.showAndWait();

            if (controller.isOkClicked()) {
                String newName  = controller.getNewListName();
                String iconPath = controller.getSelectedIconPath();

                boolean exists = masterLists.stream()
                        .anyMatch(li -> li.getName().equalsIgnoreCase(newName));
                if (exists) {
                    showCustomAlert("Error", "List already exists.", "A list with this name already exists.");
                    return;
                }

                ListInfo info = new ListInfo(newName, iconPath);
                masterLists.add(info);

                saveLists();
                updateListSidebar();
                System.out.println("[DEBUG] New list added: " + info);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            showCustomAlert("Error", "Load Error", "Failed to load the 'Add New List' dialog.");
        }
    }


    // =========================================================================
    // 7. 过滤与搜索逻辑 (Filtering & Search)
    // =========================================================================

    private void performSearch() {
        applyFilters();
        System.out.println("[DEBUG] performSearch done. results=" + filteredTasks.size());
    }

    private void setNavFilter(String filterType, Button selectedButton) {
        activeListFilter = null;
        currentFilterType = filterType;
        clearAllSidebarSelections();
        if (selectedButton != null) {
            selectedButton.getStyleClass().add("selected");
        }
        applyFilters();
    }

    private void setListFilter(String listName, Button selectedButton) {
        currentFilterType = "LIST";
        activeListFilter = listName;
        clearAllSidebarSelections();
        if (selectedButton != null) {
            selectedButton.getStyleClass().add("selected");
        }
        applyFilters();
        System.out.println("[DEBUG] List filter set: " + listName);
    }

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
     * 导航过滤逻辑 + Overdue
     */
    private boolean isNavFilterMatch(Task task) {
        if (SPACER_TITLE.equals(task.getTitle())) return true;

        LocalDate today = LocalDate.now();
        boolean isToday   = task.getDueDate() != null && task.getDueDate().isEqual(today);
        boolean isOverdue = task.getDueDate() != null
                && task.getDueDate().isBefore(today)
                && !task.isCompleted();

        switch (currentFilterType) {
            case "TODAY":     return isToday;
            case "IMPORTANT": return task.isImportant();
            case "FINISHED":  return task.isCompleted();
            case "PENDING":   return !task.isCompleted();
            case "OVERDUE":   return isOverdue;
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

    private void ensureSpacerExists() {
        masterTasks.removeIf(t -> t != null && SPACER_TITLE.equals(t.getTitle()));
        Task spacer = new Task(SPACER_TITLE, "", null, null, "Normal");
        masterTasks.add(spacer);
    }

    /**
     * 从 lists.json 加载自定义列表（每行：name|iconPath）
     */
    private void loadLists() {
        if (!LISTS_DATA_FILE.exists()) {
            System.out.println("[DEBUG] lists.json not found. No lists loaded.");
            return;
        }

        try {
            List<String> lines = Files.readAllLines(LISTS_DATA_FILE.toPath());
            masterLists.clear();

            for (String line : lines) {
                if (line == null || line.isBlank()) continue;
                String[] parts = line.split("\\|", 2);
                String name = parts[0];
                String iconPath = (parts.length > 1 && !parts[1].isBlank()) ? parts[1] : null;
                masterLists.add(new ListInfo(name, iconPath));
            }

            System.out.println("[DEBUG] Lists loaded from lists.json. Count: " + masterLists.size());
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to load lists.json: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 保存 lists.json：每行一个列表：name|iconPath
     */
    private void saveLists() {
        try {
            List<String> lines = masterLists.stream()
                    .map(li -> li.getName() + "|" + (li.getIconPath() == null ? "" : li.getIconPath()))
                    .collect(Collectors.toList());

            Files.write(LISTS_DATA_FILE.toPath(), lines);
            System.out.println("[DEBUG] Lists saved to lists.json.");
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to save lists.json: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 更新左侧 LISTS 区域（使用 ListInfo：图标 + 名称 + 右侧数量）
     */
    private void updateListSidebar() {
        if (listContainer == null) {
            System.err.println("[ERROR] listContainer is null. Cannot update list.");
            return;
        }

        listContainer.getChildren().clear();

        for (ListInfo li : masterLists) {
            Button listButton = new Button();
            listButton.setMaxWidth(Double.MAX_VALUE);
            listButton.getStyleClass().add("nav-item");

            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);

            // 图标
            if (li.getIconPath() != null && !li.getIconPath().isBlank()) {
                try {
                    var url = getClass().getResource(li.getIconPath());
                    if (url != null) {
                        ImageView iconView = new ImageView(new Image(url.toExternalForm()));
                        iconView.setFitWidth(18);
                        iconView.setFitHeight(18);
                        iconView.setPreserveRatio(true);
                        row.getChildren().add(iconView);
                    }
                } catch (Exception ex) {
                    System.err.println("[WARN] Failed to load icon for list: " + li + " -> " + ex.getMessage());
                }
            }

            // 名称
            Label nameLabel = new Label(li.getName());

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            // 数量
            int count = getTaskCountForList(li.getName());
            Label countLabel = new Label(String.valueOf(count));
            countLabel.getStyleClass().add("list-count");

            row.getChildren().addAll(nameLabel, spacer, countLabel);
            listButton.setGraphic(row);

            listButton.setOnAction(event -> setListFilter(li.getName(), listButton));

            // 右键菜单：删除列表
            ContextMenu contextMenu = new ContextMenu();
            MenuItem deleteItem = new MenuItem("Delete List");
            deleteItem.setOnAction(event -> deleteList(li));
            contextMenu.getItems().add(deleteItem);
            listButton.setContextMenu(contextMenu);

            listContainer.getChildren().add(listButton);
        }

        System.out.println("[DEBUG] List sidebar updated. Found " + masterLists.size() + " lists.");
    }

    private int getTaskCountForList(String listName) {
        int count = 0;
        for (Task t : masterTasks) {
            if (t == null || SPACER_TITLE.equals(t.getTitle())) continue;
            if (listName.equals(t.getListName())) {
                count++;
            }
        }
        return count;
    }

    /**
     * 删除一个自定义列表
     */
    private void deleteList(ListInfo listInfo) {
        String listName = listInfo.getName();

        ButtonType confirmResult = showCustomAlert(
                "Delete List",
                "Are you sure to delete the list: " + listName + "?",
                "All tasks in this list will be moved to 'Unlisted'."
        );

        if (confirmResult != ButtonType.OK) {
            return;
        }

        masterLists.remove(listInfo);

        for (Task task : masterTasks) {
            if (listName.equals(task.getListName())) {
                task.setListName(null);
            }
        }

        saveLists();
        saveTasks();
        updateListSidebar();

        if (listName.equals(activeListFilter)) {
            setNavFilter("ALL", btnAll);
        } else {
            applyFilters();
        }

        System.out.println("[DEBUG] List deleted: " + listName);
    }


    // === 顶部分类数字统计 ===

    private boolean isRealTask(Task t) {
        return t != null && !SPACER_TITLE.equals(t.getTitle());
    }

    private void updateFixedCategoryCounts() {
        LocalDate today = LocalDate.now();

        int allCount = 0;
        int todayCount = 0;
        int importantCount = 0;
        int pendingCount = 0;
        int overdueCount = 0;
        int finishedCount = 0;

        for (Task t : masterTasks) {
            if (!isRealTask(t)) continue;

            allCount++;

            if (t.getDueDate() != null && t.getDueDate().isEqual(today)) {
                todayCount++;
            }
            if (t.isImportant()) {
                importantCount++;
            }
            if (t.isCompleted()) {
                finishedCount++;
            } else {
                pendingCount++;
            }
            if (t.getDueDate() != null && t.getDueDate().isBefore(today) && !t.isCompleted()) {
                overdueCount++;
            }
        }

        if (todayCountLabel != null)     todayCountLabel.setText(String.valueOf(todayCount));
        if (importantCountLabel != null) importantCountLabel.setText(String.valueOf(importantCount));
        if (allCountLabel != null)       allCountLabel.setText(String.valueOf(allCount));
        if (pendingCountLabel != null)   pendingCountLabel.setText(String.valueOf(pendingCount));
        if (overdueCountLabel != null)   overdueCountLabel.setText(String.valueOf(overdueCount));
        if (completedCountLabel != null) completedCountLabel.setText(String.valueOf(finishedCount));


        // ============================================================
        // 🎨 数字颜色 — 完全与图标配色一致（不改布局）
        // ============================================================

        if (todayCountLabel != null)
            todayCountLabel.setStyle("-fx-text-fill: #FFCC00;");     // Today 黄色

        if (importantCountLabel != null)
            importantCountLabel.setStyle("-fx-text-fill: #AF52DE;"); // Important 紫色

        if (allCountLabel != null)
            allCountLabel.setStyle("-fx-text-fill: #007AFF;");       // All 蓝色

        if (pendingCountLabel != null)
            pendingCountLabel.setStyle("-fx-text-fill: #FF3B30;");   // Pending 红色

        if (overdueCountLabel != null)
            overdueCountLabel.setStyle("-fx-text-fill: #FFCC00;");   // Overdue 黄色

        if (completedCountLabel != null)
            completedCountLabel.setStyle("-fx-text-fill: #8E8E93;"); // Completed 灰色
    }


    // =========================================================================
    // 9. FXML 事件处理器 (菜单栏 & 快捷方式)
    // =========================================================================

    @FXML private void handleExit() {
        saveAndExit();
    }

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
            updateFixedCategoryCounts();
            updateListSidebar();
            System.out.println("[DEBUG] All completed tasks deleted.");
        }
    }

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

    @FXML
    public void saveAndExit() {
        System.out.println("[DEBUG] Save and Exit requested...");
        try {
            saveTasks();
            saveLists();
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

    // --- FXML 快捷方式 (用于 SceneBuilder 'onAction') ---
    @FXML public void onQuickAdd()       { addQuickTask(); }
    @FXML public void onAddDetails()     { openTaskDetailDialog(null); }
    @FXML public void onSearchClicked()  { performSearch(); }
    @FXML public void onClearSearch()    {
        if (searchField != null) searchField.clear();
        applyFilters();
    }
    @FXML public void onFilterToday()     { setNavFilter("TODAY",    btnToday); }
    @FXML public void onFilterImportant() { setNavFilter("IMPORTANT",btnImportant); }
    @FXML public void onFilterAll()       { setNavFilter("ALL",      btnAll); }
    @FXML public void onFilterPending()   { setNavFilter("PENDING",  btnPending); }
    @FXML public void onFilterFinished()  { setNavFilter("FINISHED", btnFinished); }
    @FXML public void onFilterOverdue()   { setNavFilter("OVERDUE",  btnOverdue); }
}