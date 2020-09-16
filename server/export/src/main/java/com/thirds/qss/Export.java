package com.thirds.qss;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class Export {
    public static void main(String[] args) {
        try {
            Files.copy(
                    Paths.get("..", "launcher", "build", "libs", "launcher-0.0.1-SNAPSHOT-all.jar"),
                    Paths.get("..", "..", "client", "launcher", "launcher.jar"),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
