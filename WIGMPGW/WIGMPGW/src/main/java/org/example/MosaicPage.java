package org.example;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Random;

public class MosaicPage {

    private final Stage stage;                 // 当前 Stage
    private final List<File> selectedFiles;    // 用户勾选的图片
    private ImageView imageView;               // 用于展示马赛克结果

    // ---- 可调参数 ----
    private static final int CANVAS_SIZE = 600;  // 最终马赛克画布尺寸
    private static final int TILE_SIZE   = 40;   // 每个像素块大小
    // ------------------

    public MosaicPage(Stage stage, List<File> selectedFiles) {
        this.stage         = stage;
        this.selectedFiles = selectedFiles;
        initUI();
        createAndShowMosaic();
    }

    //==============================
    // 1. 搭建 UI
    //==============================
    private void initUI() {
        imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(CANVAS_SIZE);

        Button backBtn = new Button("Back");
        backBtn.setOnAction(e -> {
            // 视项目实际签名调整 ↓
            new SelectPage(stage, selectedFiles).show();
        });

        VBox root = new VBox(15, imageView, backBtn);
        root.setPadding(new Insets(15));


        stage.setTitle("Image Mosaic");
        stage.setScene(new Scene(new StackPane(root), CANVAS_SIZE + 60, CANVAS_SIZE + 140));
        stage.show();
    }

    //==============================
    // 2. 生成马赛克
    //==============================
    private void createAndShowMosaic() {
        if (selectedFiles == null || selectedFiles.isEmpty()) return;

        try {
            // 2-1 生成遮罩（心形或星形）
            boolean heart = new Random().nextBoolean();
            BufferedImage mask = createMask(CANVAS_SIZE, heart);

            // 2-2 读入并缩放选中图片为小块
            BufferedImage[] thumbs = new BufferedImage[selectedFiles.size()];
            for (int i = 0; i < selectedFiles.size(); i++) {
                BufferedImage src = ImageIO.read(selectedFiles.get(i));
                thumbs[i] = resize(src, TILE_SIZE, TILE_SIZE);
            }

            // 2-3 在画布上按遮罩铺贴
            BufferedImage canvas = new BufferedImage(
                    CANVAS_SIZE, CANVAS_SIZE, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = canvas.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, CANVAS_SIZE, CANVAS_SIZE);

            int idx = 0;
            for (int y = 0; y < CANVAS_SIZE; y += TILE_SIZE) {
                for (int x = 0; x < CANVAS_SIZE; x += TILE_SIZE) {
                    int alpha = (mask.getRGB(x + TILE_SIZE / 2, y + TILE_SIZE / 2) >>> 24) & 0xFF;
                    if (alpha > 127) {
                        g.drawImage(thumbs[idx % thumbs.length], x, y, null);
                        idx++;
                    }
                }
            }
            g.dispose();

            // 2-4 转成 JavaFX Image 显示
            Image fxImg = SwingFXUtils.toFXImage(canvas, null);
            imageView.setImage(fxImg);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    //==============================
    // 3. 创建遮罩函数
    //==============================
    private BufferedImage createMask(int size, boolean heart) {
        BufferedImage mask = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = mask.createGraphics();
        g.setColor(new Color(0, 0, 0, 0)); // 透明背景
        g.fillRect(0, 0, size, size);
        g.setColor(new Color(0, 0, 0, 255)); // 不透明绘制区域

        GeneralPath path = new GeneralPath();

        if (heart) {
            double cx = size / 2.0;
            double cy = size / 2.0;
            double scale = size / 32.0;
            for (double t = 0; t <= 2 * Math.PI; t += 0.01) {
                double x = 16 * Math.pow(Math.sin(t), 3);
                double y = 13 * Math.cos(t) - 5 * Math.cos(2 * t)
                        - 2 * Math.cos(3 * t) - Math.cos(4 * t);
                double px = cx + x * scale;
                double py = cy - y * scale;
                if (t == 0) path.moveTo(px, py); else path.lineTo(px, py);
            }
        } else { // star
            double cx = size / 2.0, cy = size / 2.0;
            double outer = size * 0.4;
            double inner = outer * 0.5;
            for (int i = 0; i < 10; i++) {
                double r = (i % 2 == 0) ? outer : inner;
                double angle = -Math.PI / 2 + i * Math.PI / 5;
                double px = cx + r * Math.cos(angle);
                double py = cy + r * Math.sin(angle);
                if (i == 0) path.moveTo(px, py); else path.lineTo(px, py);
            }
        }

        path.closePath();
        g.fill(path);
        g.dispose();
        return mask;
    }

    //==============================
    // 4. 图片缩放帮助函数
    //==============================
    private BufferedImage resize(BufferedImage src, int w, int h) {
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return dst;
    }

    //==============================
    // 5. 对外入口
    //==============================
    public void show() {
        stage.show();
    }
}
