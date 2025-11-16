package com.mytodo;

/**
 * Stores metadata for each custom list: name + icon path
 * Icon path format example: "/com/mytodo/icons/user1.png"
 */
public class ListInfo {

    private String name;
    private String iconPath; // Can be null

    public ListInfo() {
        // Required no-arg constructor for Jackson or other serializers
    }

    public ListInfo(String name, String iconPath) {
        this.name = name;
        this.iconPath = iconPath;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIconPath() {
        return iconPath;
    }

    public void setIconPath(String iconPath) {
        this.iconPath = iconPath;
    }

    @Override
    public String toString() {
        return "ListInfo{name='" + name + "', iconPath='" + iconPath + "'}";
    }
}