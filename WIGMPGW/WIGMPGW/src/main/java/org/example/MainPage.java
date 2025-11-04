package org.example;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;

public class MainPage {
    private final Stage stage;
    private final List<File> photoFiles;
    private final Button selectButton;

    public MainPage(Stage stage, List<File> photoFiles) {
        this.stage = stage;
        this.photoFiles = photoFiles;
        this.selectButton = new Button("Select");
    }

    public Button getSelectButton() {
        return selectButton;
    }

    public void show() {
        VBox root = new VBox();
        root.setPadding(new Insets(10));
        root.setSpacing(10);

        HBox topBar = new HBox();
        Label titleLabel = new Label("Collection");

        topBar.getChildren().addAll(titleLabel, new Region(), selectButton);
        HBox.setHgrow(topBar.getChildren().get(1), Priority.ALWAYS);
        topBar.setPadding(new Insets(10, 0, 10, 0));
        topBar.setSpacing(10);
        topBar.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        FlowPane photosPane = new FlowPane();
        photosPane.setHgap(10);
        photosPane.setVgap(10);
        photosPane.setPadding(new Insets(5));

        for (int i = 0; i < photoFiles.size(); i++) {
            File file = photoFiles.get(i);
            Image img = new Image(file.toURI().toString(), 100, 100, true, true);
            ImageView imageView = new ImageView(img);
            VBox photoBox = new VBox();
            photoBox.getChildren().add(imageView);
            photoBox.setSpacing(5);

            File annotationFile = new File(file.getAbsolutePath() + ".txt");
            if (annotationFile.exists()) {
                Label heartLabel = new Label("♥");
                heartLabel.setStyle("-fx-text-fill: red; -fx-font-size: 16px;");
                photoBox.getChildren().add(heartLabel);
            }

            final int index = i;
            photoBox.setOnMouseClicked(ev -> {
                PhotoPage photoPage = new PhotoPage(stage, photoFiles, index);
                photoPage.show();
            });

            photosPane.getChildren().add(photoBox);
        }

        root.getChildren().addAll(topBar, photosPane);

        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.setTitle("Photo Collection");
        stage.show();
    }
}
