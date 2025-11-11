package com.mytodo.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mytodo.Task;
import javafx.collections.ObservableList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class for saving and loading task data to/from a JSON file using Jackson.
 */
public class JsonDataManager {
    private final ObjectMapper mapper;

    public JsonDataManager() {
        mapper = new ObjectMapper();
        // Register module to support Java 8 time (LocalDate, LocalTime, etc.)
        mapper.registerModule(new JavaTimeModule());
    }

    /**
     * Saves the given list of tasks to a JSON file.
     *
     * @param file  The destination JSON file.
     * @param tasks The list of tasks to save.
     */
    public void save(File file, ObservableList<Task> tasks) {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, new ArrayList<>(tasks));
        } catch (IOException e) {
            System.err.println("Failed to save tasks to JSON file: " + e.getMessage());
        }
    }

    /**
     * Loads task data from the given JSON file.
     *
     * @param file The source JSON file.
     * @return A list of loaded Task objects, or an empty list if file does not exist or an error occurs.
     */
    public List<Task> load(File file) {
        if (!file.exists()) {
            return new ArrayList<>();
        }
        try {
            Task[] tasksArray = mapper.readValue(file, Task[].class);
            return Arrays.asList(tasksArray);
        } catch (IOException e) {
            System.err.println("Failed to load tasks from JSON file: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}
