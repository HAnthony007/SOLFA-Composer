package com.demo51.demo51;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Line;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.jfugue.pattern.Pattern;
import org.jfugue.pattern.PatternProducer;
import org.jfugue.player.Player;
import javafx.scene.control.Alert.AlertType;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;

import static com.demo51.demo51.NoteUtilities.*;
import static com.demo51.demo51.Partition.setupTextField;

public class HelloController {
    @FXML
    private FlowPane mainContainer;
    @FXML
    private ComboBox<Tempo> tempo;
    @FXML
    private ComboBox<String> mesure;
    @FXML
    private ComboBox<Instrument> instrument;
    @FXML
    private TextField key;
    @FXML
    private TextField Title;
    @FXML
    private ScrollPane scrollPane;
    private Pattern getPattern;
    private String getSolfa;
    private static final int NUMBER_OF_VOICES = 4;
    private Player player;
    private Thread playThread;
    private final Object pauseLock = new Object();
    private volatile boolean isStopped;
    private volatile boolean isPaused;

    @FXML
    private void initialize() {
        this.tempo.getItems().addAll(new Tempo[]{new Tempo("Grave"), new Tempo("Largo"), new Tempo("Larghetto"), new Tempo("Adagio"), new Tempo("Andante"), new Tempo("Andantino"), new Tempo("Moderato"), new Tempo("Allegretto"), new Tempo("Allegro"), new Tempo("Vivace"), new Tempo("Presto"), new Tempo("Pretissimo")});
        this.tempo.getSelectionModel().select(new Tempo("Allegro"));
        this.instrument.getItems().addAll(new Instrument[]{new Instrument("PIANO"), new Instrument("BRIGHT_ACOUSTIC"), new Instrument("ELECTRIC_GRAND"), new Instrument("CLAVINET"), new Instrument("DRAWBAR_ORGAN"), new Instrument("CHURCH_ORGAN"), new Instrument("REED_ORGAN"), new Instrument("HARMONICA"), new Instrument("STRING_ENSEMBLE_1"), new Instrument("STRING_ENSEMBLE_2"), new Instrument("SYNTH_STRINGS_2"), new Instrument("CHOIR_AAHS"), new Instrument("VOICE_OOHS"), new Instrument("ORCHESTRA_HIT"), new Instrument("GUITAR"), new Instrument("ELECTRIC_JAZZ_GUITAR"), new Instrument("OVERDRIVEN_GUITAR"), new Instrument("GUITAR_HARMONICS"), new Instrument("VIOLIN"), new Instrument("VIOLA"), new Instrument("TREMOLO_STRINGS"), new Instrument("ORCHESTRAL_STRINGS"), new Instrument("TRUMPET"), new Instrument("TROMBONE"), new Instrument("FRENCH_HORN"), new Instrument("SOPRANO_SAX"), new Instrument("ALTO_SAX"), new Instrument("TENOR_SAX"), new Instrument("PICCOLO"), new Instrument("FLUTE")});
        this.instrument.getSelectionModel().select(new Instrument("PIANO"));
        this.mesure.getItems().addAll(new String[]{"2/4", "3/4", "4/4", "6/4"});
        this.mesure.getSelectionModel().select("4/4");
        this.mesure.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-border-width: 0; -fx-text-fill: black;");
        this.mesure.valueProperty().addListener((observable, oldValue, newValue) -> {
            this.mainContainer.getChildren().clear();
            this.handleAddHBox();
        });
        this.Title.textProperty().addListener((observable, oldValue, newValue) -> Partition.adjustedTextFieldWidth(this.Title));
        this.key.textProperty().addListener((observable, oldValue, newValue) -> {
            String regex = "^(C|D|Db|E|Eb|F|G|Gb|A|Ab|B|Bb)$";
            if (!newValue.matches(regex)) {
                this.key.setText("");
            }

            Partition.adjustedTextFieldWidth(this.Title);
        });
        Partition.customizeTextField(this.key);
        Partition.customizeTextField(this.Title);
        this.mainContainer.prefWidthProperty().bind(this.scrollPane.widthProperty());
    }

    private int extractFirstInteger(String input) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(input);
        return matcher.find() ? Integer.parseInt(matcher.group()) : 0;
    }

    @FXML
    private void handleAddHBox() {
        HBox newHBox = new HBox((double)10.0F);
        VBox vBox = new VBox((double)10.0F);
        Line line1 = new Line();
        int textFieldCount = this.extractFirstInteger((String)this.mesure.getValue());
        List<List<TextField>> fieldsLists = new ArrayList();

        for(int i = 0; i < textFieldCount; ++i) {
            fieldsLists.add(new ArrayList());
        }

        for(int i = 0; i < 4; ++i) {
            HBox hbox = new HBox((double)10.0F);

            for(int j = 0; j < textFieldCount; ++j) {
                TextField textField = new TextField();
                Partition.customizeTextField(textField);
                Partition.setupTextField(textField, (List)fieldsLists.get(j));
                ((List)fieldsLists.get(j)).add(textField);
                hbox.getChildren().add(textField);
                if (j < textFieldCount - 1 && j == textFieldCount / 2 - 1 && textFieldCount % 2 == 0) {
                    Label label = new Label("|");
                    hbox.getChildren().add(label);
                } else if (j < textFieldCount - 1 && j != textFieldCount / 2 - 1 || textFieldCount == 3 && j < textFieldCount - 1) {
                    Label label = new Label(":");
                    hbox.getChildren().add(label);
                }
            }

            vBox.getChildren().add(hbox);
        }

        line1.setStartX((double)-100.0F);
        line1.setEndX(-101.7);
        line1.setEndY(-133.3);
        newHBox.getChildren().add(vBox);
        newHBox.getChildren().add(line1);
        this.mainContainer.getChildren().add(newHBox);
    }

    @FXML
    private void handleImportSolfa() {
        this.mainContainer.getChildren().clear();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner une partition Esolfa");
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Esolfa files (*.esolfa)", new String[]{"*.esolfa"});
        fileChooser.getExtensionFilters().add(extFilter);
        File file = fileChooser.showOpenDialog(new Stage());
        if (file != null) {
            List<String> words = new ArrayList();

            String line;
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                while((line = reader.readLine()) != null) {
                    String[] wordsArray = line.split("\\|");
                    System.out.println(Arrays.toString(wordsArray));
                    this.Title.setText(wordsArray[0].toUpperCase());
                    this.key.setText(wordsArray[1]);
                    this.tempo.getSelectionModel().select(new Tempo(wordsArray[2]));
                    this.mesure.getSelectionModel().select(wordsArray[3]);
                    String[] notes = wordsArray[4].split(" ");

                    for(String word : notes) {
                        words.add(word);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            int textFieldCount = this.extractFirstInteger((String)this.mesure.getValue());
            int numberOfWords = words.size();
            int numberOfHBoxes = (int)Math.ceil((double)numberOfWords / (double)(textFieldCount * 4));
            List<List<TextField>> fieldsLists = new ArrayList();

            for(int i = 0; i < textFieldCount; ++i) {
                fieldsLists.add(new ArrayList());
            }

            int wordIndex = 0;

            for(int i = 0; i < numberOfHBoxes; ++i) {
                VBox vBox = new VBox((double)10.0F);

                for(int j = 0; j < 4; ++j) {
                    HBox hbox = new HBox((double)10.0F);
                    List<TextField> currentHBoxFields = new ArrayList();

                    for(int k = 0; k < textFieldCount; ++k) {
                        TextField textField = new TextField();
                        Partition.customizeTextField(textField);
                        if (wordIndex < words.size() && !((String)words.get(wordIndex)).equals("e")) {
                            textField.setText((String)words.get(wordIndex));
                        }

                        ++wordIndex;
                        Partition.setupTextField(textField, (List)fieldsLists.get(k));
                        currentHBoxFields.add(textField);
                        ((List)fieldsLists.get(k)).add(textField);
                        hbox.getChildren().add(textField);
                        if (k < textFieldCount - 1 && k == textFieldCount / 2 - 1 && textFieldCount % 2 == 0) {
                            Label label = new Label("|");
                            hbox.getChildren().add(label);
                        } else if (k < textFieldCount - 1 && k != textFieldCount / 2 - 1 || textFieldCount == 3 && k < textFieldCount - 1) {
                            Label label = new Label(":");
                            hbox.getChildren().add(label);
                        }
                    }

                    vBox.getChildren().add(hbox);
                }

                HBox newHBox = new HBox((double)10.0F);
                newHBox.getChildren().add(vBox);
                Line line1 = new Line();
                line1.setStartX((double)-100.0F);
                line1.setEndX(-101.7);
                line1.setEndY(-133.3);
                newHBox.getChildren().add(line1);
                this.mainContainer.getChildren().add(newHBox);
            }

        }
    }

    @FXML
    private void handleAssembly() {
        Tempo selectedTempo = (Tempo)this.tempo.getValue();
        String selectInstrument = ((Instrument)this.instrument.getValue()).getName();
        this.isPaused = false;
        this.isStopped = false;
        this.player = new Player();
        StringBuilder[] patternBuilders = new StringBuilder[4];
        String var10000 = this.key.getText().substring(0, 1).toUpperCase();
        String targetNote = var10000 + this.key.getText().substring(1);
        if (targetNote.isEmpty()) {
            showAlert("Erreur", "Tonalite non definie.", "Veuillez saisir la tonalite");
        } else {
            if (NoteUtilities.enharmonicEquivalents.containsKey(targetNote)) {
                targetNote = (String)NoteUtilities.enharmonicEquivalents.get(targetNote);
            }

            StringBuilder chromaticPattern = new StringBuilder();
            chromaticPattern.append(this.Title.getText()).append("|").append(targetNote).append("|").append(selectedTempo).append("|").append((String)this.mesure.getValue()).append("|");

            for(int i = 0; i < 4; ++i) {
                patternBuilders[i] = new StringBuilder("V" + i + " I[" + selectInstrument + "] ");
            }

            for(int i = 0; i < this.mainContainer.getChildren().size(); ++i) {
                HBox hbox = (HBox)this.mainContainer.getChildren().get(i);
                VBox vbox = (VBox)hbox.getChildren().getFirst();

                for(int j = 0; j < 4; ++j) {
                    HBox hbox2 = (HBox)vbox.getChildren().get(j);

                    for(int k = 0; k < hbox2.getChildren().size(); k += 2) {
                        TextField textField = (TextField)hbox2.getChildren().get(k);
                        String note = textField.getText();
                        String regex = "^(d|di|r|ri|m|f|fi|s|si|l|ta|t|-|\\.|,|')*$";
                        if (note.isEmpty()) {
                            patternBuilders[j].append(" Rq");
                            chromaticPattern.append("e ");
                        } else {
                            chromaticPattern.append(note).append(" ");
                            if (note.contains(".")) {
                                new StringBuilder();
                                String[] splitByDot = note.split("\\.", -1);

                                for(String segment : splitByDot) {
                                    if (segment.contains(";")) {
                                        String[] split = segment.split(";", -1);

                                        for(String virgule : split) {
                                            if (virgule.isEmpty()) {
                                                patternBuilders[j].append(" Rs");
                                            } else {
                                                int apostropheCount = NoteUtilities.countOccurrences(virgule, "'");
                                                int commaCount = NoteUtilities.countOccurrences(virgule, ",");
                                                String noteWithoutModifiers = virgule.replaceAll("[',']", "");
                                                String chromaticNote = (String)NoteUtilities.solfaToChromatic.get(noteWithoutModifiers);
                                                int octaveModifier = apostropheCount - commaCount;
                                                int adjustedOctave = j > 1 ? 4 + octaveModifier - 1 : 4 + octaveModifier;
                                                if (chromaticNote != null && !chromaticNote.isEmpty()) {
                                                    String transposedNote = NoteUtilities.transposeTo("C", targetNote, chromaticNote);
                                                    if (transposedNote.contains("'")) {
                                                        ++adjustedOctave;
                                                        transposedNote = transposedNote.replace("'", "");
                                                    }

                                                    String adjustedNote = transposedNote + adjustedOctave;
                                                    patternBuilders[j].append(" ").append(adjustedNote).append("s");
                                                }
                                            }
                                        }
                                    } else if (segment.isEmpty()) {
                                        patternBuilders[j].append(" Ri");
                                    } else {
                                        int apostropheCount = NoteUtilities.countOccurrences(segment, "'");
                                        int commaCount = NoteUtilities.countOccurrences(segment, ",");
                                        String noteWithoutModifiers = segment.replaceAll("[',']", "");
                                        String chromaticNote = (String)NoteUtilities.solfaToChromatic.get(noteWithoutModifiers);
                                        int octaveModifier = apostropheCount - commaCount;
                                        int adjustedOctave = j > 1 ? 4 + octaveModifier - 1 : 4 + octaveModifier;
                                        if (chromaticNote != null && !chromaticNote.isEmpty()) {
                                            String transposedNote = NoteUtilities.transposeTo("C", targetNote, chromaticNote);
                                            if (transposedNote.contains("'")) {
                                                ++adjustedOctave;
                                                transposedNote = transposedNote.replace("'", "");
                                            }

                                            String adjustedNote = transposedNote + adjustedOctave;
                                            patternBuilders[j].append(" ").append(adjustedNote).append("i");
                                        } else if (segment.equals("-")) {
                                            patternBuilders[j].append("i");
                                        }
                                    }
                                }
                            }

                            int apostropheCount = NoteUtilities.countOccurrences(note, "'");
                            int commaCount = NoteUtilities.countOccurrences(note, ",");
                            String noteWithoutModifiers = note.replaceAll("[',']", "");
                            String chromaticNote = (String)NoteUtilities.solfaToChromatic.get(noteWithoutModifiers);
                            int octaveModifier = apostropheCount - commaCount;
                            int adjustedOctave = j > 1 ? 4 + octaveModifier - 1 : 4 + octaveModifier;
                            if (chromaticNote != null && !chromaticNote.isEmpty()) {
                                String transposedNote = NoteUtilities.transposeTo("C", targetNote, chromaticNote);
                                if (transposedNote.contains("'")) {
                                    ++adjustedOctave;
                                    transposedNote = transposedNote.replace("'", "");
                                }

                                String adjustedNote = transposedNote + adjustedOctave;
                                patternBuilders[j].append(" ").append(adjustedNote).append("q");
                            } else if (note.equals("-")) {
                                patternBuilders[j].append("q");
                            }
                        }
                    }
                }
            }

            System.out.println(patternBuilders[0]);
            System.out.println(Arrays.toString(patternBuilders));
            NoteUtilities.adjustOctave(patternBuilders, 1);
            System.out.println(patternBuilders[0]);
            System.out.println(Arrays.toString(patternBuilders));
            Pattern pattern = new Pattern("T[" + selectedTempo.getName() + "]");
            pattern.add(new PatternProducer[0]);

            for(StringBuilder patternBuilder : patternBuilders) {
                pattern.add(patternBuilder.toString());
            }

            System.out.println(pattern);
            chromaticPattern.toString().split(" ");
            System.out.println(chromaticPattern);
            this.getPattern = pattern;
            this.getSolfa = chromaticPattern.toString();
        }
    }

    @FXML
    private void handlePlay() {
        this.playThread = new Thread(() -> {
            synchronized(this.pauseLock) {
                while(this.isPaused) {
                    try {
                        this.pauseLock.wait();
                    } catch (InterruptedException var4) {
                        if (this.isStopped) {
                            return;
                        }
                    }
                }
            }

            Platform.runLater(() -> this.handleAssembly());
            this.player.play(this.getPattern);
        });
        this.playThread.start();
    }

    @FXML
    private void midi() throws IOException {
        String fileName = this.Title.getText();
        if (fileName.isEmpty()) {
            System.out.println("Please enter both file name and pattern.");
            showAlert("EMPTY TITLE", "File name not found", "Please enter the file name");
        } else {
            File directory = new File("MIDI Esolfa");
            if (!directory.exists()) {
                directory.mkdir();
            }

            File midiFile = new File(directory, fileName + ".mid");
            if (midiFile.exists()) {
                showAlert("DUPLICATE FILE", "File already exists", "Please rename the file");
                System.out.println("File already exist. Please rename it.");
            } else {
                Platform.runLater(() -> this.handleAssembly());
                Sequence sequence = this.player.getSequence(this.getPattern);
                MidiSystem.write(sequence, 1, midiFile);
                System.out.println("Midi file created to: " + midiFile.getAbsolutePath());
                showInfo("Created successfully", "Midi file created successfully", "Midi file created successfully at: " + midiFile.getAbsolutePath());
            }
        }
    }

    @FXML
    private void handleSaveSolfa() throws IOException {
        String fileName = this.Title.getText();
        if (fileName.isEmpty()) {
            System.out.println("Please enter both file name and pattern.");
            showAlert("EMPTY TITLE", "File name not found", "Please enter the file name");
        } else {
            File directory = new File("Esolfa");
            if (!directory.exists()) {
                directory.mkdir();
            }

            File solfaFile = new File(directory, fileName + ".esolfa");
            if (solfaFile.exists()) {
                showAlert("DUPLICATE FILE", "File already exists", "Please rename the file");
                System.out.println("File already exist. Please rename it.");
            } else {
                Platform.runLater(() -> this.handleAssembly());
                String eSolfa = this.getSolfa;

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(solfaFile))) {
                    writer.write(eSolfa);
                }

                showInfo("Created successfully", "E-Solfa created successfully", "E-Solfa created successfully at: " + solfaFile.getAbsolutePath());
            }
        }
    }

    static void showAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    static void showInfo(String title, String header, String content) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

}
