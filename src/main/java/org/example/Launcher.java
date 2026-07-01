package org.example;

public class Launcher {
    public static void main(String[] args) {
        DatabaseManager.initDatabase();
        MainStarter.main(args);
    }
}
