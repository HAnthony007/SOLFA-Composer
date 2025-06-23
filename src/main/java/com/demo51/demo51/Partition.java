package com.demo51.demo51;

import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

import java.util.List;

public class Partition {
    static void setupTextField(TextField textField, List<TextField> textFieldGroup) {
        textField.setPrefWidth((double)20.0F);
        String regex = "^(d|di|r|ri|m|f|fi|s|si|l|ta|t| |-|,|\\.|;|')*$";
        textField.setAlignment(Pos.CENTER);
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches(regex)) {
                textField.setText("");
            }

            updateColumnWidth(textFieldGroup);
        });
    }

    static void adjustedTextFieldWidth(TextField textField) {
        double width = computeTextWidth(String.valueOf(textField));
        textField.setPrefWidth(computeTextWidth(textField.getText()) + (double)20.0F);
    }

    public static void updateColumnWidth(List<TextField> textFieldGroup) {
        double maxWidth = textFieldGroup.stream().mapToDouble((tfx) -> computeTextWidth(tfx.getText()) + (double)20.0F).max().orElse((double)20.0F);

        for(TextField tf : textFieldGroup) {
            tf.setPrefWidth(maxWidth);
        }

    }

    private static double computeTextWidth(String text) {
        Text tempText = new Text(text);
        new Scene(new Group(new Node[]{tempText}));
        tempText.applyCss();
        return tempText.getLayoutBounds().getWidth();
    }

    static void customizeTextField(TextField textField) {
        textField.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-border-width: 0; -fx-text-fill: black;");
        textField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                textField.setStyle("-fx-background-color: lightblue;");
            } else {
                textField.setStyle("-fx-background-color: transparent;");
            }

        });
    }

}
