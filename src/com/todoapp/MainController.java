package com.todoapp;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert.AlertType;

import java.time.LocalDate;
import java.time.LocalTime;

public class MainController {

    @FXML private TextField searchField, quickAddField;
    @FXML private Button quickAddBtn, detailAddBtn, filterBtn;
    @FXML private Button btnToday, btnImportant, btnAll, btnFinished, btnPending;
    @FXML private ListView<Task> taskList;

    private final ObservableList<Task> tasks = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        taskList.setItems(tasks);

        // ListView显示格式：两行
        taskList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Task t, boolean empty) {
                super.updateItem(t, empty);
                if (empty || t == null) setText(null);
                else setText(t.getTitle() + "\n" +
                        "截止: " + t.getDueDate() + " | 优先级: " + t.getPriority());
            }
        });

        // 快速添加
        quickAddBtn.setOnAction(e -> addQuickTask());
        quickAddField.setOnAction(e -> addQuickTask());

        // 打开详细添加弹窗
        detailAddBtn.setOnAction(e -> openTaskDetailDialog());
    }

    private void addQuickTask() {
        String text = quickAddField.getText();
        if (text == null || text.isBlank()) return;
        Task task = new Task(text.trim(), "", LocalDate.now(), LocalTime.now(), "普通");
        tasks.add(task);
        quickAddField.clear();
    }

    private void openTaskDetailDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("TaskDetailDialog.fxml"));
            DialogPane pane = loader.load();

            TaskDetailController controller = loader.getController();

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("添加详细任务");
            dialog.setDialogPane(pane);

            dialog.showAndWait().ifPresent(result -> {
                Task newTask = controller.getTask();
                if (newTask != null) {
                    tasks.add(newTask);
                    new Alert(AlertType.INFORMATION, "✅ 已添加任务: " + newTask.getTitle()).show();
                }
            });

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}