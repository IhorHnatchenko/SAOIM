package org.example;

import java.util.ArrayList;
import java.util.List;

public class MenuNode {

    private String title;
    private String icon;
    private Runnable action;
    private List<MenuNode> children = new ArrayList<>();

    public MenuNode(String title, String icon, Runnable action){
        this.title = title;
        this.icon = icon;
        this.action = action;
    }

    public void addChild(MenuNode child) {
        this.children.add(child);
    }

    public String getTitle(){ return title; }
    public String getIcon(){ return icon; }
    public Runnable getAction(){ return action; }
    public List<MenuNode> getChildren(){ return children; }
    public boolean hasChildren(){ return !children.isEmpty(); }

}
