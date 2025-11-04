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
import java.util.List;

public class SelectPage {

    private Stage stage;
    private List<File> photoFiles;
    private List<CheckBox> checkBoxes = new ArrayList<>();
    private VBox root;
    private Button createVideoBtn;
    private Button createMosaicBtn;
    private Button selectAllBtn;

    // 构造函数接收 List<File>
    public SelectPage(Stage stage, List<File> photoFiles) {
        this.stage = stage;
        this.photoFiles = photoFiles;
        initUI();
    }

    private void initUI() {
        root = new VBox(15);
        root.setPadding(new Insets(20));

        HBox topBar = new HBox(10);

        selectAllBtn = new Button("Select All");
        selectAllBtn.setOnAction(e -> selectAllPhotos());

        topBar.getChildren().addAll(selectAllBtn);

        FlowPane photosPane = new FlowPane();
        photosPane.setHgap(10);
        photosPane.setVgap(10);

        for (File photo : photoFiles) {
            VBox photoBox = new VBox(5);

            Image img = new Image(photo.toURI().toString(), 100, 100, true, true);
            ImageView iv = new ImageView(img);

            CheckBox cb = new CheckBox();
            checkBoxes.add(cb);

            photoBox.getChildren().addAll(iv, cb);
            photosPane.getChildren().add(photoBox);
        }

        HBox bottomBar = new HBox(20);

        createVideoBtn = new Button("Create Your Video");
        createVideoBtn.setDisable(true);
        createVideoBtn.setOnAction(e -> openVideoCreator());

        createMosaicBtn = new Button("Create Image Mosaic");
        createMosaicBtn.setDisable(true);
        createMosaicBtn.setOnAction(e -> openMosaicCreator());

        bottomBar.getChildren().addAll(createVideoBtn, createMosaicBtn);

        // 监听checkbox变化，控制按钮是否可用
        checkBoxes.forEach(cb -> cb.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            updateActionButtonsState();
        }));

        root.getChildren().addAll(topBar, photosPane, bottomBar);

        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.setTitle("Select Photos");
    }

    private void selectAllPhotos() {
        boolean allSelected = checkBoxes.stream().allMatch(CheckBox::isSelected);
        boolean newState = !allSelected;
        checkBoxes.forEach(cb -> cb.setSelected(newState));
    }

    private void updateActionButtonsState() {
        boolean anySelected = checkBoxes.stream().anyMatch(CheckBox::isSelected);
        createVideoBtn.setDisable(!anySelected);
        createMosaicBtn.setDisable(!anySelected);
    }

    private void openVideoCreator() {
        List<File> selectedFiles = getSelectedFiles();
        if (selectedFiles.isEmpty()) {
            showAlert("Please select at least one image to create video.");
            return;
        }
        VideoCreatorModule videoCreator = new VideoCreatorModule(selectedFiles);
        try {
            videoCreator.start(new Stage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openMosaicCreator() {
        List<File> selectedFiles = getSelectedFiles();
        if (selectedFiles.isEmpty()) {
            showAlert("Please select at least one image to create mosaic.");
            return;
        }
        MosaicPage mosaicPage = new MosaicPage(stage, selectedFiles);
        mosaicPage.show();
    }

    private List<File> getSelectedFiles() {
        List<File> selectedFiles = new ArrayList<>();
        for (int i = 0; i < checkBoxes.size(); i++) {
            if (checkBoxes.get(i).isSelected()) {
                selectedFiles.add(photoFiles.get(i));
            }
        }
        return selectedFiles;
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message);
        alert.showAndWait();
    }

    public void show() {
        stage.show();
    }
}
