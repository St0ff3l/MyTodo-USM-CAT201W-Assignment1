package com.mytodo;

// ---------------------------------------------------------------------
// 导入 (Imports)
// ---------------------------------------------------------------------

// JavaFX 核心
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.*;
// 🌟 [已移除] 移除了 FlowPane
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

// Java 标准库
import java.time.format.DateTimeFormatter;


/**
 * 自定义列表单元格 (TaskListCell)，用于在 ListView 中显示单个任务。
 */
public class TaskListCell extends ListCell<Task> {

    // --- 布局和控件 (Fields) ---
    private final HBox rootLayout = new HBox(10); // 根 HBox，间距 10
    private final CheckBox completedCheckbox = new CheckBox();
    private final Text titleText = new Text();
    private final Label detailLabel = new Label();

    // 🌟 [已移除] 移除了 tagContainer

    // 🌟 [已修改] 垂直文本堆栈，现在只包含 标题 和 详情
    private final VBox textStack = new VBox(2, titleText, detailLabel);

    // --- 状态与常量 ---
    private final MainController controller; // 对主控制器的引用
    private boolean bindingDone = false; // 宽度绑定的标志

    private static final double SIDE_MARGIN = 50;
    private static final double SPACER_HEIGHT = 100;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * 构造函数 (Constructor)
     * (此部分保持不变)
     * @param controller 传入的 MainController 实例
     */
    public TaskListCell(MainController controller) {
        this.controller = controller;

        // (加载 CSS... 保持不变)
        try {
            rootLayout.getStylesheets().add(
                    getClass().getResource("/com/mytodo/Main.css").toExternalForm()
            );
        } catch (Exception e) {
            System.err.println("Could not load stylesheet: Main.css");
            e.printStackTrace();
        }

        // (布局设置... 保持不变)
        completedCheckbox.setAllowIndeterminate(false);
        completedCheckbox.setStyle("-fx-mark-color: transparent;");
        completedCheckbox.setGraphic(null);
        rootLayout.setAlignment(Pos.CENTER_LEFT);
        rootLayout.setMaxWidth(Double.MAX_VALUE);
        this.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(textStack, Priority.ALWAYS);
        VBox.setVgrow(textStack, Priority.ALWAYS);

        // (事件监听... 保持不变)
        completedCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            Task task = getItem();
            if (task != null && task.isCompleted() != newVal) {
                controller.toggleCompletion(task);
                Platform.runLater(() -> {
                    if (getListView() != null) getListView().refresh();
                });
            }
        });

        // (编辑按钮... 保持不变)
        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().add("btn-edit");
        editBtn.setOnAction(e -> {
            Task t = getItem();
            if (t != null) controller.openTaskDetailDialog(t);
        });

        // (删除按钮... 保持不变)
        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("btn-delete");
        deleteBtn.setOnAction(e -> {
            Task t = getItem();
            if (t != null) controller.deleteTask(t);
        });

        // (组装布局... 保持不变)
        HBox actionBox = new HBox(5, editBtn, deleteBtn);
        actionBox.setAlignment(Pos.CENTER_RIGHT);
        rootLayout.getChildren().addAll(completedCheckbox, textStack, actionBox);

        // (样式设置... 保持不变)
        titleText.setFont(Font.font("System", FontWeight.NORMAL, 16));
        detailLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 11px;");
        rootLayout.setStyle("-fx-padding: 10px 15px 10px 15px; -fx-background-color: #ffffff; -fx-background-radius: 8;");
        this.setPrefWidth(Region.USE_COMPUTED_SIZE);
    }

    /**
     * 核心方法：当单元格被重用或数据更新时调用。
     */
    @Override
    protected void updateItem(Task task, boolean empty) {
        super.updateItem(task, empty);

        // 🌟 1. [已修改] 空单元格 (Bug 修复)
        if (empty || task == null) {
            setGraphic(null);
            setText(null);
            // 🌟 [关键修复] 必须显式重置样式为透明
            setStyle("-fx-background-color: transparent; -fx-padding: 0;");
            return;
        }

        // 2. "幽灵"项 (Spacer Item)
        if ("(SPACER_ITEM)".equals(task.getTitle())) {
            Region spacer = new Region();
            spacer.setMinHeight(SPACER_HEIGHT);
            spacer.setPrefHeight(SPACER_HEIGHT);
            spacer.setMaxHeight(SPACER_HEIGHT);
            setGraphic(spacer);
            setText(null);
            // (这里的透明样式是正确的)
            setStyle("-fx-background-color: transparent; -fx-padding: 0;");
            return;
        }

        // 3. 正常任务渲染
        // (标题和描述设置保持不变)
        String title = task.getTitle() == null ? "(No title)" : task.getTitle().trim();
        String desc = task.getDescription() == null ? "" : task.getDescription().trim();
        String combined = desc.isEmpty() ? title : title + " • " + desc;
        titleText.setText(combined);

        // 4. 详情标签 (Detail Label)
        String dateStr = task.getDueDate() != null ? task.getDueDate().format(DATE_FORMATTER) : "No due date";
        String timeStr = (task.getTime() != null) ? task.getTime().format(TIME_FORMATTER) : "No time";
        String priority = task.getPriority() == null ? "Normal" : task.getPriority();

        detailLabel.setText("Due: " + dateStr + " " + timeStr + " | Priority: " + priority);

        String listName = task.getListName();
        if (listName != null && !listName.isBlank()) {
            String listStr = " | List: " + listName;
            detailLabel.setText(detailLabel.getText() + listStr);
        }

        // (复选框和删除线逻辑... 保持不变)
        completedCheckbox.setSelected(task.isCompleted());
        if (task.isCompleted()) {
            titleText.setStrikethrough(true);
            titleText.setStyle("-fx-fill: gray;");
            detailLabel.setStyle("-fx-text-fill: #8a8a8a; -fx-opacity: 0.9;");
        } else {
            titleText.setStrikethrough(false);
            titleText.setStyle("-fx-fill: black;");
            detailLabel.setStyle("-fx-text-fill: gray; -fx-opacity: 1.0;");
        }

        // (最终设置... 保持不变)
        setGraphic(rootLayout);
        setStyle("-fx-padding: 4px 0; -fx-background-color: transparent;");

        // (宽度绑定逻辑... 保持不变)
        if (!bindingDone && getListView() != null) {
            Platform.runLater(() -> {
                try {
                    double totalMargin = SIDE_MARGIN * 2;
                    this.prefWidthProperty().bind(getListView().widthProperty().subtract(totalMargin));
                    rootLayout.prefWidthProperty().bind(getListView().widthProperty().subtract(totalMargin));
                    titleText.wrappingWidthProperty().bind(getListView().widthProperty().subtract(totalMargin + 200));
                } catch (Exception ignored) {}
            });
            bindingDone = true;
        }
    }
}