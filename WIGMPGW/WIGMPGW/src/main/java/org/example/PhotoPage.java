package org.example;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class PhotoPage {
    private final Stage stage;
    private final List<File> photoFiles;
    private int currentIndex;

    private ImageView imageView;
    private Label noteLabel;

    public PhotoPage(Stage stage, List<File> photoFiles, int startIndex) {
        this.stage = stage;
        this.photoFiles = photoFiles;
        this.currentIndex = startIndex;
    }

    public void show() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        Button backBtn = new Button("Back");
        backBtn.setOnAction(e -> {
            File photosDir = new File("photos");
            File[] files = photosDir.listFiles((dir, name) -> {
                String lower = name.toLowerCase();
                return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png");
            });
            List<File> photoFiles = files != null ? Arrays.asList(files) : new ArrayList<>();
            MainPage mainPage = new MainPage(stage, photoFiles);
            mainPage.show();
        });



        Button editBtn = new Button("Edit");
        editBtn.setOnAction(e -> showEditOptions());

        HBox topBar = new HBox(10, backBtn, new Region(), editBtn);
        HBox.setHgrow(topBar.getChildren().get(1), Priority.ALWAYS);
        topBar.setPadding(new Insets(0, 0, 10, 0));
        root.setTop(topBar);

        imageView = new ImageView();
        imageView.setFitWidth(600);
        imageView.setPreserveRatio(true);
        updateImage();

        imageView.setOnMouseClicked(event -> {
            double clickX = event.getX();
            double width = imageView.getBoundsInLocal().getWidth();
            if (clickX < width / 2) {
                showPreviousImage();
            } else {
                showNextImage();
            }
        });

        noteLabel = new Label();
        noteLabel.setPadding(new Insets(10, 0, 0, 0));
        updateNote();

        VBox centerBox = new VBox(10, imageView, noteLabel);
        centerBox.setPadding(new Insets(0, 0, 10, 0));
        root.setCenter(centerBox);

        Scene scene = new Scene(root, 800, 700);
        stage.setScene(scene);
        stage.setTitle("Photo Viewer");
        stage.show();
    }

    private void updateImage() {
        File currentFile = photoFiles.get(currentIndex);
        Image image = new Image(currentFile.toURI().toString());
        imageView.setImage(image);
    }

    private void showPreviousImage() {
        if (currentIndex > 0) {
            currentIndex--;
            updateImage();
            updateNote();
        }
    }

    private void showNextImage() {
        if (currentIndex < photoFiles.size() - 1) {
            currentIndex++;
            updateImage();
            updateNote();
        }
    }

    private void updateNote() {
        File currentFile = photoFiles.get(currentIndex);
        File noteFile = new File(currentFile.getAbsolutePath() + ".txt");
        if (noteFile.exists()) {
            try (Scanner scanner = new Scanner(noteFile)) {
                StringBuilder sb = new StringBuilder();
                while (scanner.hasNextLine()) {
                    sb.append(scanner.nextLine()).append("\n");
                }
                noteLabel.setText(sb.toString());
            } catch (Exception e) {
                noteLabel.setText("");
            }
        } else {
            noteLabel.setText("");
        }
    }

    private void showEditOptions() {
        Stage dialog = new Stage();
        dialog.initOwner(stage);
        dialog.setTitle("Edit Options");

        Button editNoteBtn = new Button("Edit Your Note");
        Button editPhotoBtn = new Button("Edit Your Photo");

        editNoteBtn.setOnAction(e -> {
            dialog.close();
            EditDialog editDialog = new EditDialog(photoFiles.get(currentIndex));
            editDialog.showAndWait();
            updateNote();
        });

        editPhotoBtn.setOnAction(e -> {
            dialog.close();
            EditPhotoPage editor = new EditPhotoPage(stage, photoFiles.get(currentIndex));
            editor.show();
        });


        VBox vbox = new VBox(10, editNoteBtn, editPhotoBtn);
        vbox.setPadding(new Insets(20));
        vbox.setPrefWidth(200);
        vbox.setPrefHeight(150);
        vbox.setStyle("-fx-background-color: white;");
        vbox.setSpacing(10);
        vbox.setFillWidth(true);
        vbox.setMaxWidth(Double.MAX_VALUE);


        dialog.setScene(new Scene(vbox));
        dialog.show();
    }
}
