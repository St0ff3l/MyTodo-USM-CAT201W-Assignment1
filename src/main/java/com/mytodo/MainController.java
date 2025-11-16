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
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

// Java 标准库
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.stream.Collectors;

// 本项目特定类
import com.mytodo.util.JsonDataManager;
// 🌟 导入你的新成功弹窗 Controller
import com.mytodo.SuccessMessageDialogController;


/**
 * 主界面的控制器 (MainController)。
 * 负责处理所有用户交互、数据管理和UI更新。
 */
public class MainController {

    // ---------------------------------------------------------------------
    // 1. FXML UI 元素绑定
    // ---------------------------------------------------------------------

    // 根布局
    @FXML private VBox root;

    // 侧边栏
    @FXML private VBox sidebar;
    @FXML private Button btnToday, btnImportant, btnAll, btnFinished, btnPending;

    // 主内容区
    @FXML private ListView<Task> taskList;

    // 顶部搜索/过滤
    @FXML private TextField searchField;
    @FXML private Button filterBtn;
    @FXML private Button searchClearBtn;

    // 底部浮动添加栏
    @FXML private HBox floatingAddBox;
    @FXML private TextField quickAddField;
    @FXML private Button quickAddBtn;
    @FXML private Button detailAddBtn;

    // ---------------------------------------------------------------------
    // 2. 数据与状态管理
    // ---------------------------------------------------------------------

    /** 存储所有任务的原始列表 (数据源) */
    private final ObservableList<Task> masterTasks = FXCollections.observableArrayList();

    /** 经过滤后显示在 ListView 上的列表 (视图) */
    private final FilteredList<Task> filteredTasks = new FilteredList<>(masterTasks, t -> true);

    /** 当前激活的导航过滤器 (例如 "ALL", "TODAY") */
    private String currentFilterType = "ALL";

    // ---------------------------------------------------------------------
    // 3. 常量与 I/O 配置
    // ---------------------------------------------------------------------

    /** 数据存储文件名 */
    private static final File DATA_FILE = new File("tasks.json");

    /** JSON 数据读写管理器 */
    private final JsonDataManager dataManager = new JsonDataManager();

    /** "快速添加" 任务时的默认截止时间 (当天 23:59) */
    private final LocalTime DEFAULT_END_OF_DAY_TIME = LocalTime.of(23, 59);

    /** * "幽灵"项的特殊标题。
     * 这是一个添加到列表末尾的不可见任务，用于美化UI。
     * 它可以防止 ListView 的最后一个真实任务被底部的浮动添加栏遮挡。
     */
    private static final String SPACER_TITLE = "(SPACER_ITEM)";


    // =========================================================================
    // 4. 初始化
    // =========================================================================

    /**
     * FXML 加载后自动调用此方法。
     * 负责初始化所有UI组件、加载数据和绑定事件。
     */
    @FXML
    private void initialize() {
        System.out.println("[DEBUG] MainController initializing...");

        // 1. 加载持久化数据
        try {
            loadTasks();
            System.out.println("[DEBUG] Tasks loaded. Count: " + masterTasks.size());
        } catch (Exception ex) {
            System.err.println("[ERROR] loadTasks failed during initialization: " + ex.getMessage());
            ex.printStackTrace();
        }

        // 2. 确保"幽灵"项存在 (用于UI美化)
        ensureSpacerExists();

        // 3. 将过滤后的列表绑定到 ListView
        taskList.setItems(filteredTasks);

        // 4. 设置自定义的单元格渲染器 (TaskListCell)
        // TaskListCell 会处理每个任务如何显示
        taskList.setCellFactory(list -> new TaskListCell(this));

        // 5. 让 ListView 自动填满可用空间
        VBox.setVgrow(taskList, Priority.ALWAYS);
        HBox.setHgrow(taskList, Priority.ALWAYS);

        // 6. 绑定UI组件的事件监听器
        bindActionEvents();

        // 7. 设置并应用默认的导航过滤器 ("ALL")
        setNavFilter("ALL", btnAll);
        System.out.println("[DEBUG] Initialization complete.");
    }

    /**
     * 辅助方法：集中管理所有 FXML 元素的事件绑定。
     */
    private void bindActionEvents() {
        // 顶部搜索栏
        if (searchField != null) {
            // 在搜索框按回车键 = 执行搜索
            searchField.setOnAction(e -> performSearch());
        }
        if (filterBtn != null) {
            // 点击搜索按钮 = 执行搜索
            filterBtn.setOnAction(e -> performSearch());
        }
        if (searchClearBtn != null) {
            // 点击清除按钮
            searchClearBtn.setOnAction(e -> {
                searchField.clear();
                applyFilters(); // 重新应用过滤器 (移除搜索词)
                taskList.refresh();
                System.out.println("[DEBUG] Search cleared.");
            });
        }

        // 侧边栏导航
        if (btnAll != null) btnAll.setOnAction(e -> setNavFilter("ALL", btnAll));
        if (btnToday != null) btnToday.setOnAction(e -> setNavFilter("TODAY", btnToday));
        if (btnImportant != null) btnImportant.setOnAction(e -> setNavFilter("IMPORTANT", btnImportant));
        if (btnFinished != null) btnFinished.setOnAction(e -> setNavFilter("FINISHED", btnFinished));
        if (btnPending != null) btnPending.setOnAction(e -> setNavFilter("PENDING", btnPending));

        // 底部添加栏
        if (quickAddBtn != null) {
            // 点击 "Add" 按钮
            quickAddBtn.setOnAction(e -> addQuickTask());
        }
        if (quickAddField != null) {
            // 在快速添加框按回车键
            quickAddField.setOnAction(e -> addQuickTask());
        }
        if (detailAddBtn != null) {
            // 点击 "..." 详细添加按钮
            detailAddBtn.setOnAction(e -> openTaskDetailDialog(null));
        }
    }


    // =========================================================================
    // 5. 核心任务操作 (增 / 删 / 改)
    // =========================================================================

    /**
     * 快速添加任务 (从底部浮动栏)
     */
    private void addQuickTask() {
        String text = quickAddField.getText();
        if (text == null || text.isBlank()) {
            // 输入为空，忽略
            return;
        }

        // 新任务插入到 "幽灵" 项之前
        int insertPos = Math.max(0, masterTasks.size() - 1);
        Task task = new Task(
                text.trim(),
                "", // 默认描述为空
                LocalDate.now(), // 默认日期为今天
                DEFAULT_END_OF_DAY_TIME, // 默认时间为 23:59
                "Normal" // 默认优先级
        );

        masterTasks.add(insertPos, task);
        quickAddField.clear(); // 清空输入框

        // 保存并刷新
        saveTasks();
        applyFilters();
        taskList.refresh();
    }

    /**
     * [PUBLIC] 删除一个任务。
     * 此方法为 public，以便 TaskListCell 可以调用它。
     *
     * @param task 要删除的任务
     */
    public void deleteTask(Task task) {
        if (task == null || SPACER_TITLE.equals(task.getTitle())) {
            // 不删除 "幽灵" 项
            return;
        }

        // 1. 显示确认弹窗
        ButtonType confirmResult = showCustomAlert(
                "Delete Confirmation",
                "Are you sure to delete: " + task.getTitle() + " ?",
                "This action cannot be undone."
        );

        // 2. 仅在用户点击 "OK" 时才执行删除
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
     * 此方法为 public，以便 TaskListCell 可以调用它。
     *
     * @param task 要切换状态的任务
     */
    public void toggleCompletion(Task task) {
        if (task == null || SPACER_TITLE.equals(task.getTitle())) {
            // "幽灵" 项不可交互
            return;
        }

        task.setCompleted(!task.isCompleted());

        // 立即保存并刷新UI
        saveTasks();
        applyFilters(); // 重新应用过滤器 (如果当前在 "Pending" 或 "Finished" 视图)
        taskList.refresh();
    }

    /**
     * [PUBLIC] 打开任务详情对话框 (用于添加或编辑)。
     * 此方法为 public，以便 TaskListCell 可以调用它 (用于编辑)。
     *
     * @param taskToEdit 要编辑的任务。如果为 null，则表示创建新任务。
     */
    public void openTaskDetailDialog(Task taskToEdit) {
        try {
            // 1. 加载 "详细任务" 对话框 FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mytodo/TaskDetailDialog.fxml"));
            DialogPane pane = loader.load();

            // 2. 获取其 Controller
            TaskDetailController controller = loader.getController();

            // 3. 传递数据 (如果是编辑，则加载现有任务数据)
            controller.loadData(taskToEdit);

            // 4. 创建一个 Dialog 实例来承载这个 DialogPane
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle(taskToEdit == null ? "Add Task" : "Edit Task");
            dialog.setDialogPane(pane);

            // 5. [关键] 移除 DialogPane 的默认按钮
            // 这样它就只会显示我们在 FXML 中自定义的 "OK" 和 "Cancel" 按钮
            pane.getButtonTypes().clear();

            // 6. 应用 CSS 样式
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/mytodo/Main.css").toExternalForm());

            // 7. 显示对话框并等待用户操作
            // 代码会在这里暂停，直到 TaskDetailController 关闭它
            dialog.showAndWait();

            // 8. (用户已关闭对话框) 检查是否点击了 "OK"
            if (controller.isOkClicked()) {
                Task updatedTask = controller.getTask(); // 获取保存后的任务对象
                if (updatedTask != null) {

                    // 🌟 [关键修改] 🌟
                    // 使用你新创建的 "Success" 弹窗来显示成功消息
                    String msg = (taskToEdit == null) ? "Task added: " : "Task updated: ";

                    // 调换参数：把消息放到第一个参数 (header),
                    // 把 null (或空字符串 "") 放到第二个参数 (content)
                    showSuccessAlert(msg + updatedTask.getTitle(), null);
                    // 🌟 [修改结束] 🌟

                    if (taskToEdit == null) {
                        // --- 这是添加新任务 ---
                        // 插入到 "幽灵" 项之前
                        int insertPos = Math.max(0, masterTasks.size() - 1);
                        masterTasks.add(insertPos, updatedTask);
                    } else {
                        // --- 这是编辑现有任务 ---
                        // 无需操作。因为 `taskToEdit` 是一个对象引用，
                        // controller 内部修改它时，`masterTasks` 里的对象也自动更新了。
                        // 我们只需要刷新列表视图。
                    }

                    // 9. 保存数据并刷新UI
                    saveTasks();
                    applyFilters();
                    taskList.refresh();
                }
            }
            // (如果 isOkClicked() == false，即用户点了 Cancel，我们什么也不做)

        } catch (IOException ex) {
            ex.printStackTrace();
            // 使用通用的确认弹窗来显示错误
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
     * 🌟 [新] 显示一个只带 "OK" 按钮的成功消息弹窗。
     * 使用 'successMessageDialogView.fxml'。
     *
     * @param header  弹窗的粗体标题 (如果为 null 或空，则不显示)
     * @param content 弹窗的主要内容
     */
    private void showSuccessAlert(String header, String content) {
        try {
            // 1. 加载 FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mytodo/successMessageDialogView.fxml"));
            DialogPane pane = loader.load();

            // 2. 获取 Controller
            SuccessMessageDialogController controller = loader.getController();

            // 3. 设置消息
            controller.setSuccessMessage(header, content);

            // 4. 创建 Dialog
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Success"); // 窗口标题固定为 "Success"
            dialog.setDialogPane(pane);

            // 5. 移除默认按钮
            pane.getButtonTypes().clear();

            // 6. 显示并等待 (它只有一个OK按钮，点击后会自行关闭)
            dialog.showAndWait();

        } catch (IOException ex) {
            ex.printStackTrace();
            // 如果自定义弹窗加载失败，显示一个标准的备用弹窗
            Alert fallback = new Alert(AlertType.INFORMATION, content);
            fallback.setTitle("Success");
            fallback.setHeaderText(header);
            fallback.showAndWait();
        }
    }

    /**
     * 显示一个带 "OK" 和 "Cancel" 按钮的通用确认弹窗。
     * (保留此方法用于删除确认等操作)
     *
     * @param title   窗口标题
     * @param header  弹窗的粗体标题
     * @param content 弹窗的主要内容
     * @return 用户点击的按钮 (ButtonType.OK 或 ButtonType.CANCEL)
     */
    private ButtonType showCustomAlert(String title, String header, String content) {
        try {
            // 1. 加载 FXML (注意：这里加载的是旧的确认弹窗)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mytodo/CustomAlertDialogView.fxml"));
            DialogPane pane = loader.load();

            // 2. 获取 Controller
            CustomAlertController controller = loader.getController();
            controller.setMessage(header, content);

            // 3. 创建 Dialog
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle(title);
            dialog.setDialogPane(pane);

            // 4. 移除默认按钮
            pane.getButtonTypes().clear();

            // 5. 显示并等待
            dialog.showAndWait();

            // 6. 返回 Controller 记录的结果
            return controller.getResult();

        } catch (IOException ex) {
            ex.printStackTrace();
            // 加载失败时的备用弹窗
            Alert fallback = new Alert(AlertType.ERROR, "Failed to load custom dialog: " + ex.getMessage());
            fallback.showAndWait();
            return ButtonType.CANCEL; // 默认返回 Cancel
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
     * 设置侧边栏的导航过滤器 (例如 "Today", "Important")
     *
     * @param filterType     过滤类型 ("ALL", "TODAY", ...)
     * @param selectedButton 被点击的按钮 (用于添加 'selected' CSS 类)
     */
    private void setNavFilter(String filterType, Button selectedButton) {
        currentFilterType = filterType;

        // 1. 移除所有按钮的 "selected" 样式
        sidebar.getChildren().stream()
                .filter(node -> node instanceof Button)
                .map(node -> (Button) node)
                .forEach(btn -> btn.getStyleClass().remove("selected"));

        // 2. 为当前点击的按钮添加 "selected" 样式
        selectedButton.getStyleClass().add("selected");

        // 3. 应用新的过滤器
        applyFilters();
    }

    /**
     * 核心过滤方法。
     * 此方法会结合 "导航过滤器" (如 "Today") 和 "搜索框文本" 来设置 `filteredTasks` 的谓词 (predicate)。
     */
    private void applyFilters() {
        // 1. 获取搜索框文本 (统一转为小写并去除首尾空格)
        String searchText = (searchField != null && searchField.getText() != null)
                ? searchField.getText().toLowerCase().trim() : "";

        // 2. 为 FilteredList 设置新的过滤规则
        filteredTasks.setPredicate(task -> {
            if (task == null) return false;

            // 规则 A: 始终显示 "幽灵" 项
            if (SPACER_TITLE.equals(task.getTitle())) return true;

            // 规则 B: 检查是否匹配当前的导航过滤器 (e.g., "Today")
            if (!isNavFilterMatch(task)) return false;

            // 规则 C: 检查是否匹配搜索文本
            if (searchText.isEmpty()) {
                // 如果搜索框为空，则通过
                return true;
            } else {
                // 如果搜索框不为空，检查标题或描述是否包含搜索词
                String title = (task.getTitle() == null) ? "" : task.getTitle().toLowerCase();
                String desc = (task.getDescription() == null) ? "" : task.getDescription().toLowerCase();
                return title.contains(searchText) || desc.contains(searchText);
            }
        });

        System.out.println("[DEBUG] applyFilters -> " + currentFilterType + " search='" + searchText + "' remaining=" + filteredTasks.size());
    }

    /**
     * 辅助方法：检查单个任务是否符合当前的导航过滤器。
     *
     * @param task 要检查的任务
     * @return true 如果任务符合, false 则不符合
     */
    private boolean isNavFilterMatch(Task task) {
        // "幽灵" 项始终匹配 (尽管它在 applyFilters 中已被提前处理)
        if (SPACER_TITLE.equals(task.getTitle())) return true;

        boolean isToday = task.getDueDate() != null && task.getDueDate().isEqual(LocalDate.now());

        switch (currentFilterType) {
            case "TODAY":     return isToday;
            case "IMPORTANT": return task.isImportant();
            case "FINISHED":  return task.isCompleted();
            case "PENDING":   return !task.isCompleted();
            case "ALL":
            default:          return true; // "ALL" 匹配所有
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
            // 即使加载失败，也继续运行 (使用空列表)
        }
    }

    /**
     * 将 `masterTasks` 列表中的所有真实任务保存到 tasks.json 文件。
     */
    private void saveTasks() {
        try {
            // 1. 创建一个不包含 "幽灵" 项的新列表
            var toSaveList = masterTasks.stream()
                    .filter(t -> t != null && !SPACER_TITLE.equals(t.getTitle()))
                    .collect(Collectors.toList());

            // 2. 将这个干净的列表转换为 ObservableList (如果 dataManager 需要)
            ObservableList<Task> toSave = FXCollections.observableArrayList(toSaveList);

            // 3. 执行保存
            dataManager.save(DATA_FILE, toSave);
            System.out.println("[DEBUG] Tasks saved. Count: " + toSave.size());

        } catch (Exception ex) {
            System.err.println("[ERROR] dataManager.save failed: " + ex.getMessage());
            ex.printStackTrace();
            // 显示一个错误弹窗
            showCustomAlert("Save Error", "Failed to save tasks", "Your changes might be lost. Error: " + ex.getMessage());
        }
    }

    /**
     * 确保 "幽灵" 项始终存在于 `masterTasks` 列表的末尾。
     * (先移除所有旧的，再在末尾添加一个新的)
     */
    private void ensureSpacerExists() {
        // 1. 移除所有已存在的 "幽灵" 项
        masterTasks.removeIf(t -> t != null && SPACER_TITLE.equals(t.getTitle()));

        // 2. 在列表末尾添加一个新的 "幽灵" 项
        Task spacer = new Task(SPACER_TITLE, "", null, null, "Normal");
        masterTasks.add(spacer);
    }


    // =========================================================================
    // 9. FXML 事件处理器 (菜单栏 & 快捷方式)
    // =========================================================================

    // --- 菜单栏 File ---
    @FXML private void handleExit() {
        saveAndExit();
    }

    // --- 菜单栏 Edit ---
    @FXML
    private void handleDeleteCompleted() {
        // 1. 显示确认弹窗
        ButtonType confirmResult = showCustomAlert(
                "Clear Completed Tasks",
                "Delete all completed tasks?",
                "This cannot be undone."
        );

        // 2. 仅在 "OK" 时执行
        if (confirmResult == ButtonType.OK) {
            // 移除所有 "已完成" 且 "不是幽灵项" 的任务
            masterTasks.removeIf(t -> t != null && t.isCompleted() && !SPACER_TITLE.equals(t.getTitle()));

            // 刷新并保存
            applyFilters();
            saveTasks();
            taskList.refresh();
            System.out.println("[DEBUG] All completed tasks deleted.");
        }
    }

    // --- 菜单栏 View ---
    @FXML
    private void handleToggleTheme() {
        // (这个方法是示例，你之前的代码里有)
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

    // --- 菜单栏 Help ---
    @FXML
    private void handleHelp() {
        // (这个方法是示例，你之前的代码里有)
        if (root != null && root.getScene() != null && root.getScene().getWindow() != null) {
            // 假设你有一个 AboutDialogController
            AboutDialogController.showAboutDialog(root.getScene().getWindow());
        } else {
            // 备用方案
            Alert tempAlert = new Alert(AlertType.INFORMATION);
            tempAlert.setTitle("About");
            tempAlert.setHeaderText(null);
            tempAlert.setContentText("MyTodo Application v1.0");
            tempAlert.showAndWait();
        }
    }

    /**
     * [PUBLIC] 保存任务并安全退出应用程序。
     * (由菜单 "File -> Exit" 或窗口关闭请求调用)
     */
    @FXML
    public void saveAndExit() {
        System.out.println("[DEBUG] Save and Exit requested...");
        try {
            saveTasks();
            Platform.exit();
            System.exit(0); // 确保进程完全终止
        } catch (Exception e) {
            Alert errorAlert = new Alert(AlertType.ERROR);
            errorAlert.setTitle("Error");
            errorAlert.setHeaderText("Failed to save tasks on exit");
            errorAlert.setContentText("Error: " + e.getMessage());
            errorAlert.showAndWait();
            System.exit(1); // 退出并返回错误码
        }
    }

    // --- FXML 快捷方式 (用于 SceneBuilder 'onAction'，避免使用 lambda) ---
    // (这些方法只是调用了我们已经写好的内部方法)
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