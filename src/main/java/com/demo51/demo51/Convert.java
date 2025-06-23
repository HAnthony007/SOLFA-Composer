package com.demo51.demo51;

import org.jfugue.pattern.Pattern;
import org.jfugue.player.Player;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import java.io.File;
import java.io.IOException;

public class Convert {
    public void convertToMidi(String filename, Pattern pattern) throws IOException {
        if (filename.isEmpty()) {
            System.out.println("Please enter both file name and pattern.");
            HelloController.showAlert("EMPTY TITLE", "File name not found", "Please enter the file name");
        } else {
            File midiFile = new File(filename, ".mid");
            if (midiFile.exists()) {
                HelloController.showAlert("DUPLICATE FILE", "File already exists", "Please rename the file");
                System.out.println("File already exist. Please rename it.");
            } else {
                Player player = new Player();
                Sequence sequence = player.getSequence(pattern);
                MidiSystem.write(sequence, 1, midiFile);
                System.out.println("Midi file created to: " + midiFile.getAbsolutePath());
            }
        }
    }
}
