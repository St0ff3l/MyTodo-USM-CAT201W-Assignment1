package com.mytodo;

import com.mytodo.util.JsonDataManager;
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


    @FXML
    private void initialize() {
        // 1. 加载数据
        loadTasks();

        // 2. 数据绑定
        taskList.setItems(filteredTasks);

        // 3. 设置自定义单元格工厂 (使用 TaskListCell)
        taskList.setCellFactory(list -> new TaskListCell(this));

        // 4. 监听事件
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        btnAll.setOnAction(e -> setNavFilter("ALL", btnAll));
        btnToday.setOnAction(e -> setNavFilter("TODAY", btnToday));
        btnImportant.setOnAction(e -> setNavFilter("IMPORTANT", btnImportant));
        btnFinished.setOnAction(e -> setNavFilter("FINISHED", btnFinished));
        btnPending.setOnAction(e -> setNavFilter("PENDING", btnPending));

        // 默认选中 ALL
        setNavFilter("ALL", btnAll);

        // 5. 快速添加
        quickAddBtn.setOnAction(e -> addQuickTask());
        quickAddField.setOnAction(e -> addQuickTask());

        // 6. 打开详细添加/编辑弹窗
        detailAddBtn.setOnAction(e -> openTaskDetailDialog(null));
    }

    // --- CRUD 操作 ---

    // 快速添加任务
    private void addQuickTask() {
        String text = quickAddField.getText();
        if (text == null || text.isBlank()) return;
        // 快速添加时，默认时间为 23:59
        Task task = new Task(text.trim(), "", LocalDate.now(), LocalTime.of(23, 59), "普通");
        masterTasks.add(task);
        quickAddField.clear();
        saveTasks();
    }

    // 打开添加/编辑弹窗 (供按钮点击和 TaskListCell 调用)
    public void openTaskDetailDialog(Task taskToEdit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("TaskDetailDialog.fxml"));
            DialogPane pane = loader.load();

            TaskDetailController controller = loader.getController();

            // 关键修正：调用 loadData() 替代 setTask()。
            // loadData() 负责在 FXML 绑定完成后，安全地初始化 Spinner 和加载数据。
            controller.loadData(taskToEdit);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle(taskToEdit == null ? "添加详细任务" : "编辑任务");
            dialog.setDialogPane(pane);

            // 等待用户操作
            Optional<ButtonType> result = dialog.showAndWait();

            if (result.isPresent() && result.get() == ButtonType.OK) {
                Task updatedTask = controller.getTask();
                if (updatedTask != null) {
                    if (taskToEdit == null) {
                        masterTasks.add(updatedTask); // 添加新任务
                        new Alert(AlertType.INFORMATION, "✅ 已添加任务: " + updatedTask.getTitle()).show();
                    } else {
                        // 编辑模式，只需通知视图刷新
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

    // 删除任务 (供 TaskListCell 调用)
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

    // 切换任务完成状态 (供 TaskListCell 调用)
    public void toggleCompletion(Task task) {
        task.setCompleted(!task.isCompleted());
        saveTasks();
        applyFilters(); // 重新应用筛选
    }

    // --- 筛选逻辑 (Part 3) ---

    private void setNavFilter(String filterType, Button selectedButton) {
        currentFilterType = filterType;
        // UI 样式控制
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
            // 1. 导航栏筛选 (最高优先级)
            if (!isNavFilterMatch(task)) {
                return false;
            }

            // 2. 搜索框筛选 (关键字)
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
            case "ALL":
                return true;
            case "TODAY":
                return isToday; // 优先级高：今日任务，无论完成与否都显示
            case "IMPORTANT":
                return task.isImportant(); // 重要性取决于 isImportant 属性
            case "FINISHED":
                return task.isCompleted();
            case "PENDING":
                return !task.isCompleted();
            default:
                return true;
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
    }
}