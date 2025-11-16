package com.mytodo;

/**
 * 保存每个自定义列表的元数据：名称 + 图标路径
 * 图标路径形式类似："/com/mytodo/icons/user1.png"
 */
public class ListInfo {

    private String name;
    private String iconPath; // 可以为 null

    public ListInfo() {
        // Jackson 或其它序列化需要无参构造
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