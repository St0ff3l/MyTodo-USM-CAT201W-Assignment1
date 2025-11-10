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
import javafx.scene.layout.VBox;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

public class MainController {

    // --- FXML BINDINGS ---
    @FXML private TextField searchField, quickAddField;
    @FXML private Button quickAddBtn, detailAddBtn, filterBtn; // filterBtn reused as "Search" button in FXML text
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

        // bind ListView
        taskList.setItems(filteredTasks);
        taskList.setCellFactory(list -> new TaskListCell(this));

        // Search: only trigger on Enter or when filterBtn (Search) is clicked
        if (searchField != null) {
            searchField.setOnAction(e -> {
                System.out.println("[DEBUG] search triggered by Enter: '" + searchField.getText() + "'");
                performSearch();
            });
        } else {
            System.err.println("[WARN] searchField is null (FXML not injected?)");
        }

        if (filterBtn != null) {
            filterBtn.setOnAction(e -> {
                System.out.println("[DEBUG] search triggered by filterBtn click: '" + (searchField == null ? "" : searchField.getText()) + "'");
                performSearch();
            });
        } else {
            System.err.println("[WARN] filterBtn is null (FXML not injected?)");
        }

        // keep left navigation buttons (All/Today/Important/Finished/Pending)
        if (btnAll != null) btnAll.setOnAction(e -> setNavFilter("ALL", btnAll));
        else System.err.println("[WARN] btnAll is null (FXML not injected?)");

        if (btnToday != null) btnToday.setOnAction(e -> setNavFilter("TODAY", btnToday));
        else System.err.println("[WARN] btnToday is null (FXML not injected?)");

        if (btnImportant != null) btnImportant.setOnAction(e -> setNavFilter("IMPORTANT", btnImportant));
        else System.err.println("[WARN] btnImportant is null (FXML not injected?)");

        if (btnFinished != null) btnFinished.setOnAction(e -> setNavFilter("FINISHED", btnFinished));
        else System.err.println("[WARN] btnFinished is null (FXML not injected?)");

        if (btnPending != null) btnPending.setOnAction(e -> setNavFilter("PENDING", btnPending));
        else System.err.println("[WARN] btnPending is null (FXML not injected?)");

        // default ALL
        if (btnAll != null) {
            setNavFilter("ALL", btnAll);
        } else {
            currentFilterType = "ALL";
            applyFilters();
        }

        // quick add & detail
        if (quickAddBtn != null) quickAddBtn.setOnAction(e -> addQuickTask());
        if (quickAddField != null) quickAddField.setOnAction(e -> addQuickTask());
        if (detailAddBtn != null) detailAddBtn.setOnAction(e -> openTaskDetailDialog(null));
    }

    // perform search (triggered by Enter or Search button)
    private void performSearch() {
        applyFilters();
        System.out.println("[DEBUG] performSearch completed. results=" + filteredTasks.size());
    }

    // --- CRUD operations ---

    private void addQuickTask() {
        String text = quickAddField.getText();
        if (text == null || text.isBlank()) return;
        Task task = new Task(text.trim(), "", LocalDate.now(), DEFAULT_END_OF_DAY_TIME, "Normal");
        masterTasks.add(task);
        quickAddField.clear();
        saveTasks();
        // refresh current filters/search
        applyFilters();
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
                        masterTasks.add(updatedTask);
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
        }
    }

    public void deleteTask(Task task) {
        if (task != null) {
            Alert confirm = new Alert(AlertType.CONFIRMATION);
            confirm.setTitle("Delete Confirmation");
            confirm.setHeaderText(null);
            confirm.setContentText("Are you sure you want to delete task: " + task.getTitle() + " ?");

            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    masterTasks.remove(task);
                    saveTasks();
                    applyFilters();
                }
            });
        }
    }

    public void toggleCompletion(Task task) {
        task.setCompleted(!task.isCompleted());
        saveTasks();
        applyFilters();
    }

    // --- Filtering logic (left-nav + search box) ---

    private void setNavFilter(String filterType, Button selectedButton) {
        currentFilterType = filterType;

        if (sidebar != null) {
            sidebar.getChildren().stream()
                    .filter(node -> node instanceof Button)
                    .map(node -> (Button)node)
                    .forEach(btn -> btn.getStyleClass().remove("selected"));
        } else {
            System.err.println("[WARN] sidebar is null in setNavFilter()");
        }

        if (selectedButton != null) {
            selectedButton.getStyleClass().add("selected");
        } else {
            System.err.println("[WARN] selectedButton is null for filterType=" + filterType);
        }

        applyFilters();
    }

    private void applyFilters() {
        String searchText = (searchField != null && searchField.getText() != null)
                ? searchField.getText().toLowerCase().trim() : "";

        filteredTasks.setPredicate(task -> {
            try {
                if (task == null) return false;

                // 1) left-nav filter
                if (!isNavFilterMatch(task)) return false;

                // 2) search keyword (if empty => pass)
                if (searchText.isEmpty()) return true;

                String title = task.getTitle() == null ? "" : task.getTitle().toLowerCase();
                String desc = task.getDescription() == null ? "" : task.getDescription().toLowerCase();

                return title.contains(searchText) || desc.contains(searchText);
            } catch (Exception ex) {
                System.err.println("[ERROR] exception in applyFilters predicate: " + ex);
                ex.printStackTrace();
                return false;
            }
        });

        System.out.println("[DEBUG] applyFilters -> currentFilterType=" + currentFilterType + " search='" + searchText + "' remaining=" + filteredTasks.size());
    }

    private boolean isNavFilterMatch(Task task) {
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
        masterTasks.addAll(dataManager.load(DATA_FILE));
    }

    private void saveTasks() {
        dataManager.save(DATA_FILE, masterTasks);
    }

    // used by Main.java to ensure save on exit
    public void saveAndExit() {
        saveTasks();
        Platform.exit();
        System.exit(0);
    }

    // --- Menu actions ---

    @FXML private void handleExit() {
        saveAndExit();
    }

    @FXML
    private void handleDeleteCompleted() {
        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle("Clear Completed Tasks");
        confirm.setHeaderText("Confirm delete all completed tasks?");
        confirm.setContentText("This action cannot be undone.");

        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            masterTasks.removeIf(Task::isCompleted);
            applyFilters();
            saveTasks();
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
}
