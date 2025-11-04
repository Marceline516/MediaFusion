package org.example;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.*;

public class EditDialog extends Stage {
    private File imageFile;
    private TextArea textArea;

    public EditDialog(File imageFile) {
        this.imageFile = imageFile;

        setTitle("Edit Note");
        initModality(Modality.APPLICATION_MODAL);

        textArea = new TextArea();
        textArea.setPrefRowCount(10);
        loadNote();

        Button saveBtn = new Button("Save");
        saveBtn.setOnAction(e -> {
            saveNote();
            close();
        });

        VBox root = new VBox(10, textArea, saveBtn);
        root.setPadding(new Insets(10));

        setScene(new Scene(root, 400, 300));
    }

    private void loadNote() {
        File noteFile = new File(imageFile.getAbsolutePath() + ".txt");
        if (noteFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(noteFile))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                textArea.setText(sb.toString());
            } catch (IOException e) {
                textArea.setText("");
            }
        }
    }

    private void saveNote() {
        File noteFile = new File(imageFile.getAbsolutePath() + ".txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(noteFile))) {
            writer.write(textArea.getText());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
