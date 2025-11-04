package org.example;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.*;

import java.io.File;
import java.util.List;


public class VideoCreatorModule extends Application {

    private final int width = 640;
    private final int height = 480;
    private String overlayText = "";
    private File overlayGraphic = null;
    private final List<File> selectedImages;

    private MediaView mediaView = new MediaView();
    private VBox root;

    // 构造函数接收选中的图片列表
    public VideoCreatorModule(List<File> selectedImages) {
        this.selectedImages = selectedImages;
    }

    @Override
    public void start(Stage primaryStage) {
        root = new VBox(10);

        Label title = new Label("Video Creator");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        mediaView.setFitWidth(width);
        mediaView.setFitHeight(height);
        mediaView.setPreserveRatio(true);

        HBox buttonBox = new HBox(10);

        Button addTextBtn = new Button("Add Text Overlay");
        Button addGraphicBtn = new Button("Add Graphic Overlay");
        Button generateBtn = new Button("Generate Video");

        addTextBtn.setOnAction(e -> addText());
        addGraphicBtn.setOnAction(e -> addGraphic(primaryStage));
        generateBtn.setOnAction(e -> generateVideo(primaryStage));

        buttonBox.getChildren().addAll(addTextBtn, addGraphicBtn, generateBtn);
        root.getChildren().addAll(title, mediaView, buttonBox);

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Video Creator");
        primaryStage.show();
    }

    private void addText() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Text");
        dialog.setHeaderText("Enter text to overlay on video");
        dialog.setContentText("Text:");
        dialog.showAndWait().ifPresent(text -> overlayText = text);
    }

    private void addGraphic(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Graphic Image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        overlayGraphic = fileChooser.showOpenDialog(stage);
    }

    private void generateVideo(Stage stage) {
        if (selectedImages.isEmpty()) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Video As");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("MP4 files", "*.mp4"));
        fileChooser.setInitialFileName("my_video.mp4");
        File saveFile = fileChooser.showSaveDialog(stage);

        if (saveFile == null) return;
        String outputPath = saveFile.getAbsolutePath();
        try {
            FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputPath, width, height);
            recorder.setFrameRate(1);
            recorder.setVideoCodec(org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_H264);
            recorder.setFormat("mp4");
            recorder.setVideoBitrate(9000);
            recorder.setPixelFormat(0);
            recorder.start();

            OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();

            for (File imageFile : selectedImages) {
                Mat image = opencv_imgcodecs.imread(imageFile.getAbsolutePath());
                opencv_imgproc.resize(image, image, new Size(width, height));

                if (!overlayText.isEmpty()) {
                    opencv_imgproc.putText(
                            image,
                            overlayText,
                            new Point(20, height - 30),
                            opencv_imgproc.FONT_HERSHEY_SIMPLEX,
                            1.0,
                            new Scalar(255, 255, 255, 0),
                            2,
                            opencv_imgproc.LINE_AA,
                            false
                    );
                }

                if (overlayGraphic != null) {
                    Mat graphic = opencv_imgcodecs.imread(overlayGraphic.getAbsolutePath());
                    opencv_imgproc.resize(graphic, graphic, new Size(100, 100));
                    Rect roi = new Rect(width - 110, height - 110, graphic.cols(), graphic.rows());
                    Mat submat = image.apply(roi);
                    graphic.copyTo(submat);
                }

                recorder.record(converter.convert(image));
            }

            recorder.stop();
            playVideo(outputPath);

        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error generating video");
            alert.showAndWait();
        }
    }

    private void playVideo(String filePath) {
        try {
            Media media = new Media(new File(filePath).toURI().toString());
            MediaPlayer mediaPlayer = new MediaPlayer(media);
            mediaView.setMediaPlayer(mediaPlayer);

            HBox controls = new HBox(10);

            Button playBtn = new Button("▶");
            Button pauseBtn = new Button("⏸");
            Slider seekSlider = new Slider();
            seekSlider.setMin(0);

            mediaPlayer.setOnReady(() -> {
                seekSlider.setMax(mediaPlayer.getTotalDuration().toSeconds());
            });

            mediaPlayer.currentTimeProperty().addListener((obs, oldVal, newVal) -> {
                seekSlider.setValue(newVal.toSeconds());
            });

            seekSlider.setOnMousePressed(e -> mediaPlayer.seek(Duration.seconds(seekSlider.getValue())));
            seekSlider.setOnMouseDragged(e -> mediaPlayer.seek(Duration.seconds(seekSlider.getValue())));

            playBtn.setOnAction(e -> mediaPlayer.play());
            pauseBtn.setOnAction(e -> mediaPlayer.pause());

            controls.getChildren().addAll(playBtn, pauseBtn, seekSlider);
            if (!root.getChildren().contains(controls)) {
                root.getChildren().add(controls);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to play video");
            alert.showAndWait();
        }
    }
}