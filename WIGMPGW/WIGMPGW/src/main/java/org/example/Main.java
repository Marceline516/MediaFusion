package org.example;

import javafx.application.Application;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main extends Application {

    private Stage primaryStage;
    private File photosDir;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        photosDir = new File("photos");
        if (!photosDir.exists()) photosDir.mkdirs();

        // 读取照片文件列表
        List<File> photoFiles = loadPhotoFiles();

        showMainPage(photoFiles);
    }

    private List<File> loadPhotoFiles() {
        File[] files = photosDir.listFiles((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png");
        });
        if (files != null) {
            return new ArrayList<>(Arrays.asList(files));
        }
        return new ArrayList<>();
    }

    private void showMainPage(List<File> photoFiles) {
        MainPage mainPage = new MainPage(primaryStage, photoFiles);
        mainPage.show();

        // 绑定选择按钮
        mainPage.getSelectButton().setOnAction(e -> {
            SelectPage selectPage = new SelectPage(primaryStage, photoFiles);
            selectPage.show();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
