package com.mytodo;

import javafx.beans.property.*;
import java.time.LocalDate;
import java.time.LocalTime;
// [Removed] Removed java.util.ArrayList and java.util.List

public class Task {
    private final StringProperty title = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final ObjectProperty<LocalDate> dueDate = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalTime> time = new SimpleObjectProperty<>();
    private final StringProperty priority = new SimpleStringProperty();
    private final BooleanProperty completed = new SimpleBooleanProperty(false);
    private final BooleanProperty important = new SimpleBooleanProperty(false);

    // 1. [Updated] "tags" (List<String>) has been replaced by "listName" (String)
    private String listName;


    // (No-argument constructor)
    public Task() {
        setTitle("");
        setDescription("");
        setPriority("Normal");
        // 2. [Updated] Default value is null
        this.listName = null;
    }

    // (Full constructor)
    public Task(String title, String desc, LocalDate due, LocalTime time, String priority) {
        setTitle(title);
        setDescription(desc);
        setDueDate(due);
        setTime(time);
        setPriority(priority);
        // 3. [Updated] Default value is null
        this.listName = null;
    }

    // --- Getters / Setters / Properties (unchanged) ---
    public String getTitle() { return title.get(); }
    public void setTitle(String v) { title.set(v); }
    public StringProperty titleProperty() { return title; }

    public String getDescription() { return description.get(); }
    public void setDescription(String v) { description.set(v); }
    public StringProperty descriptionProperty() { return description; }

    public LocalDate getDueDate() { return dueDate.get(); }
    public void setDueDate(LocalDate v) { dueDate.set(v); }
    public ObjectProperty<LocalDate> dueDateProperty() { return dueDate; }

    public LocalTime getTime() { return time.get(); }
    public void setTime(LocalTime v) { time.set(v); }
    public ObjectProperty<LocalTime> timeProperty() { return time; }

    public String getPriority() { return priority.get(); }
    public void setPriority(String v) { priority.set(v); }
    public StringProperty priorityProperty() { return priority; }

    public boolean isCompleted() { return completed.get(); }
    public void setCompleted(boolean v) { completed.set(v); }
    public BooleanProperty completedProperty() { return completed; }

    public boolean isImportant() { return important.get(); }
    public void setImportant(boolean v) { important.set(v); }
    public BooleanProperty importantProperty() { return important; }

    // 4. [Updated] Removed getTags/setTags
    // Replaced with getListName/setListName

    public String getListName() {
        return listName;
    }

    public void setListName(String listName) {
        this.listName = listName;
    }
}