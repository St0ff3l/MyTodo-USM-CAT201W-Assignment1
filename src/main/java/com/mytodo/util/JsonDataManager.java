package com.mytodo.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mytodo.Task; // 路径已修改
import javafx.collections.ObservableList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JsonDataManager {
    private final ObjectMapper mapper;

    public JsonDataManager() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
    }

    public void save(File file, ObservableList<Task> tasks) {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, new ArrayList<>(tasks));
        } catch (IOException e) {
            System.err.println("保存任务到 JSON 文件失败: " + e.getMessage());
        }
    }

    public List<Task> load(File file) {
        if (!file.exists()) {
            return new ArrayList<>();
        }
        try {
            Task[] tasksArray = mapper.readValue(file, Task[].class);
            return Arrays.asList(tasksArray);
        } catch (IOException e) {
            System.err.println("加载任务从 JSON 文件失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}