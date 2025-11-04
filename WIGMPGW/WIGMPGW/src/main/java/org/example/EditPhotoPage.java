package org.example;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Stack;

public class EditPhotoPage {

    private final Stage stage;
    private final File imageFile;
    private ImageView imageView;
    private WritableImage currentImage;
    private final Stack<WritableImage> undoStack = new Stack<>();
    private final Stack<WritableImage> redoStack = new Stack<>();

    private Slider brightnessSlider;
    private Slider contrastSlider;

    private Canvas cropCanvas;
    private Point2D cropStart;
    private Point2D cropEnd;
    private boolean cropping = false;

    public EditPhotoPage(Stage owner, File imageFile) {
        this.imageFile = imageFile;

        stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Edit Photo");

        initUI();
        loadImage();
    }

    private void initUI() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        StackPane imagePane = new StackPane();
        imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(600);

        cropCanvas = new Canvas(600, 700);
        cropCanvas.setMouseTransparent(true);
        imagePane.getChildren().addAll(imageView, cropCanvas);

        VBox controlPanel = new VBox(10);
        controlPanel.setPadding(new Insets(10));
        controlPanel.setPrefWidth(220);

        Button undoBtn = new Button("Undo");
        Button redoBtn = new Button("Redo");

        Label brightnessLabel = new Label("Brightness");
        brightnessSlider = new Slider(-100, 100, 0);
        brightnessSlider.setShowTickLabels(true);
        brightnessSlider.setOnMouseReleased(e -> {
            pushUndo();
            adjustBrightness((int) brightnessSlider.getValue());
        });

        Label contrastLabel = new Label("Contrast");
        contrastSlider = new Slider(0.5, 2.0, 1.0);
        contrastSlider.setShowTickLabels(true);
        contrastSlider.setOnMouseReleased(e -> {
            pushUndo();
            adjustContrast(contrastSlider.getValue());
        });

        Button grayscaleBtn = new Button("Grayscale");
        Button rotateBtn = new Button("Rotate 90°");
        Button cropBtn = new Button("Crop");
        Button confirmCropBtn = new Button("Confirm Crop");
        Button cancelCropBtn = new Button("Cancel Crop");
        Button addBorderBtn = new Button("Add Border");
        Button saveBtn = new Button("Save");

        confirmCropBtn.setDisable(true);
        cancelCropBtn.setDisable(true);

        undoBtn.setOnAction(e -> undo());
        redoBtn.setOnAction(e -> redo());
        grayscaleBtn.setOnAction(e -> {
            pushUndo();
            convertToGrayscale();
        });
        rotateBtn.setOnAction(e -> {
            pushUndo();
            rotateImage();
        });
        cropBtn.setOnAction(e -> {
            startCropMode();
            confirmCropBtn.setDisable(false);
            cancelCropBtn.setDisable(false);
        });
        confirmCropBtn.setOnAction(e -> {
            confirmCrop();
            confirmCropBtn.setDisable(true);
            cancelCropBtn.setDisable(true);
        });
        cancelCropBtn.setOnAction(e -> {
            cancelCrop();
            confirmCropBtn.setDisable(true);
            cancelCropBtn.setDisable(true);
        });
        addBorderBtn.setOnAction(e -> {
            pushUndo();
            addBorder();
        });
        saveBtn.setOnAction(e -> saveImage());

        controlPanel.getChildren().addAll(
                undoBtn, redoBtn,
                brightnessLabel, brightnessSlider,
                contrastLabel, contrastSlider,
                grayscaleBtn, rotateBtn,
                cropBtn, confirmCropBtn, cancelCropBtn,
                addBorderBtn, saveBtn
        );

        root.setCenter(imagePane);
        root.setRight(controlPanel);

        Scene scene = new Scene(root, 850, 720);
        stage.setScene(scene);

        cropCanvas.setOnMousePressed(this::onCropStart);
        cropCanvas.setOnMouseDragged(this::onCropDrag);
        cropCanvas.setOnMouseReleased(this::onCropEnd);
    }

    public void show() {
        stage.show();
    }

    private void loadImage() {
        try {
            BufferedImage bufferedImage = ImageIO.read(imageFile);
            currentImage = SwingFXUtils.toFXImage(bufferedImage, null);
            imageView.setImage(currentImage);
        } catch (IOException e) {
            showAlert("Failed to load image.");
        }
    }

    private void pushUndo() {
        undoStack.push(cloneImage(currentImage));
        redoStack.clear();
    }

    private void undo() {
        if (!undoStack.isEmpty()) {
            redoStack.push(cloneImage(currentImage));
            currentImage = undoStack.pop();
            imageView.setImage(currentImage);
        }
    }

    private void redo() {
        if (!redoStack.isEmpty()) {
            undoStack.push(cloneImage(currentImage));
            currentImage = redoStack.pop();
            imageView.setImage(currentImage);
        }
    }

    private WritableImage cloneImage(WritableImage img) {
        return new WritableImage(img.getPixelReader(), (int) img.getWidth(), (int) img.getHeight());
    }

    private void adjustBrightness(int offset) {
        WritableImage newImg = new WritableImage((int) currentImage.getWidth(), (int) currentImage.getHeight());
        PixelReader reader = currentImage.getPixelReader();
        PixelWriter writer = newImg.getPixelWriter();

        for (int y = 0; y < newImg.getHeight(); y++) {
            for (int x = 0; x < newImg.getWidth(); x++) {
                Color c = reader.getColor(x, y);
                double r = clamp(c.getRed() + offset / 255.0);
                double g = clamp(c.getGreen() + offset / 255.0);
                double b = clamp(c.getBlue() + offset / 255.0);
                writer.setColor(x, y, new Color(r, g, b, c.getOpacity()));
            }
        }

        currentImage = newImg;
        imageView.setImage(currentImage);
    }

    private void adjustContrast(double factor) {
        WritableImage newImg = new WritableImage((int) currentImage.getWidth(), (int) currentImage.getHeight());
        PixelReader reader = currentImage.getPixelReader();
        PixelWriter writer = newImg.getPixelWriter();

        for (int y = 0; y < newImg.getHeight(); y++) {
            for (int x = 0; x < newImg.getWidth(); x++) {
                Color c = reader.getColor(x, y);
                double r = clamp((c.getRed() - 0.5) * factor + 0.5);
                double g = clamp((c.getGreen() - 0.5) * factor + 0.5);
                double b = clamp((c.getBlue() - 0.5) * factor + 0.5);
                writer.setColor(x, y, new Color(r, g, b, c.getOpacity()));
            }
        }

        currentImage = newImg;
        imageView.setImage(currentImage);
    }

    private void convertToGrayscale() {
        WritableImage newImg = new WritableImage((int) currentImage.getWidth(), (int) currentImage.getHeight());
        PixelReader reader = currentImage.getPixelReader();
        PixelWriter writer = newImg.getPixelWriter();

        for (int y = 0; y < newImg.getHeight(); y++) {
            for (int x = 0; x < newImg.getWidth(); x++) {
                Color c = reader.getColor(x, y);
                double gray = (c.getRed() + c.getGreen() + c.getBlue()) / 3;
                writer.setColor(x, y, new Color(gray, gray, gray, c.getOpacity()));
            }
        }

        currentImage = newImg;
        imageView.setImage(currentImage);
    }

    private void rotateImage() {
        int w = (int) currentImage.getWidth();
        int h = (int) currentImage.getHeight();
        WritableImage rotated = new WritableImage(h, w);

        PixelReader reader = currentImage.getPixelReader();
        PixelWriter writer = rotated.getPixelWriter();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                writer.setColor(h - y - 1, x, reader.getColor(x, y));
            }
        }

        currentImage = rotated;
        imageView.setImage(currentImage);
    }

    private void addBorder() {
        int width = (int) currentImage.getWidth();
        int height = (int) currentImage.getHeight();
        int border = 20;

        WritableImage bordered = new WritableImage(width + border * 2, height + border * 2);
        PixelWriter writer = bordered.getPixelWriter();

        for (int y = 0; y < bordered.getHeight(); y++) {
            for (int x = 0; x < bordered.getWidth(); x++) {
                if (x < border || y < border || x >= width + border || y >= height + border) {
                    writer.setColor(x, y, Color.BLACK);
                } else {
                    writer.setColor(x, y, currentImage.getPixelReader().getColor(x - border, y - border));
                }
            }
        }

        currentImage = bordered;
        imageView.setImage(currentImage);
    }

    private void saveImage() {
        try {
            BufferedImage bImage = SwingFXUtils.fromFXImage(currentImage, null);
            ImageIO.write(bImage, "png", imageFile);
            showAlert("Image saved successfully.");
        } catch (IOException e) {
            showAlert("Failed to save image.");
        }
    }

    private double clamp(double val) {
        return Math.max(0, Math.min(1, val));
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg);
        alert.showAndWait();
    }

    private void startCropMode() {
        cropping = true;
        cropCanvas.setMouseTransparent(false);
        GraphicsContext gc = cropCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, cropCanvas.getWidth(), cropCanvas.getHeight());
    }

    private void onCropStart(MouseEvent e) {
        if (!cropping) return;
        cropStart = new Point2D(e.getX(), e.getY());
    }

    private void onCropDrag(MouseEvent e) {
        if (!cropping || cropStart == null) return;
        cropEnd = new Point2D(e.getX(), e.getY());
        GraphicsContext gc = cropCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, cropCanvas.getWidth(), cropCanvas.getHeight());
        gc.setStroke(Color.RED);
        gc.setLineWidth(2);
        gc.strokeRect(
                Math.min(cropStart.getX(), cropEnd.getX()),
                Math.min(cropStart.getY(), cropEnd.getY()),
                Math.abs(cropEnd.getX() - cropStart.getX()),
                Math.abs(cropEnd.getY() - cropStart.getY())
        );
    }

    private void onCropEnd(MouseEvent e) {
        if (!cropping) return;
        cropEnd = new Point2D(e.getX(), e.getY());
    }

    private void confirmCrop() {
        if (!cropping || cropStart == null || cropEnd == null) return;
        cropping = false;
        cropCanvas.setMouseTransparent(true);

        int x = (int) Math.min(cropStart.getX(), cropEnd.getX());
        int y = (int) Math.min(cropStart.getY(), cropEnd.getY());
        int w = (int) Math.abs(cropEnd.getX() - cropStart.getX());
        int h = (int) Math.abs(cropEnd.getY() - cropStart.getY());

        if (x + w > currentImage.getWidth() || y + h > currentImage.getHeight()) return;

        pushUndo();
        WritableImage cropped = new WritableImage(currentImage.getPixelReader(), x, y, w, h);
        currentImage = cropped;
        imageView.setImage(currentImage);

        cropStart = null;
        cropEnd = null;
        cropCanvas.getGraphicsContext2D().clearRect(0, 0, cropCanvas.getWidth(), cropCanvas.getHeight());
    }

    private void cancelCrop() {
        cropping = false;
        cropStart = null;
        cropEnd = null;
        cropCanvas.setMouseTransparent(true);
        cropCanvas.getGraphicsContext2D().clearRect(0, 0, cropCanvas.getWidth(), cropCanvas.getHeight());
    }
}
