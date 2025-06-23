package com.demo51.demo51;

import java.util.HashMap;
import java.util.Map;

public class NoteUtilities {
    static final Map<String, String> solfaToChromatic = new HashMap();
    static final Map<String, String> enharmonicEquivalents;

    static String transposeTo(String sourceNote, String targetNote, String note) {
        String[] chromaticScale = new String[]{"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        String[] enharmonicScale = new String[]{"C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B"};
        int sourceIndex = -1;
        int targetIndex = -1;

        for(int i = 0; i < chromaticScale.length; ++i) {
            if (chromaticScale[i].equals(sourceNote) || enharmonicScale[i].equals(sourceNote)) {
                sourceIndex = i;
            }

            if (chromaticScale[i].equals(targetNote) || enharmonicScale[i].equals(targetNote)) {
                targetIndex = i;
            }
        }

        if (sourceIndex != -1 && targetIndex != -1) {
            int interval = targetIndex - sourceIndex;
            if (interval < 0) {
                interval += chromaticScale.length;
            }

            int noteIndex = -1;

            for(int i = 0; i < chromaticScale.length; ++i) {
                if (chromaticScale[i].equals(note) || enharmonicScale[i].equals(note)) {
                    noteIndex = i;
                    break;
                }
            }

            if (noteIndex == -1) {
                throw new IllegalArgumentException("Invalid note: " + note);
            } else {
                int transposedIndex = noteIndex + interval;
                boolean octaveUp = false;
                if (transposedIndex >= chromaticScale.length) {
                    transposedIndex %= chromaticScale.length;
                    octaveUp = true;
                }

                String transposedNote = chromaticScale[transposedIndex];
                if (enharmonicEquivalents.containsKey(transposedNote)) {
                    transposedNote = (String)enharmonicEquivalents.get(transposedNote);
                }

                if (octaveUp) {
                    transposedNote = transposedNote + "'";
                }

                return transposedNote;
            }
        } else {
            throw new IllegalArgumentException("Invalid source or target note.");
        }
    }

    static void adjustOctave(StringBuilder[] patternBuilders, int octaveAdjustment) {
        for(int i = 0; i < patternBuilders.length; ++i) {
            String pattern = patternBuilders[i].toString();
            String[] notes = pattern.split(" ");
            StringBuilder adjustedPattern = new StringBuilder();

            for(String note : notes) {
                if (note.matches("^[A-G][b#]?[0-9]$")) {
                    int originalOctave = Character.getNumericValue(note.charAt(note.length() - 1));
                    int adjustedOctave = originalOctave + octaveAdjustment;
                    String noteWithoutOctave = note.substring(0, note.length() - 1);
                    adjustedPattern.append(noteWithoutOctave).append(adjustedOctave).append(" ");
                } else {
                    adjustedPattern.append(note).append(" ");
                }
            }

            patternBuilders[i] = new StringBuilder(adjustedPattern.toString().trim());
        }

    }

    static int countOccurrences(String str, String substr) {
        return str.length() - str.replace(substr, "").length();
    }

    static {
        solfaToChromatic.put("d", "C");
        solfaToChromatic.put("di", "C#");
        solfaToChromatic.put("r", "D");
        solfaToChromatic.put("ri", "D#");
        solfaToChromatic.put("m", "E");
        solfaToChromatic.put("f", "F");
        solfaToChromatic.put("fi", "F#");
        solfaToChromatic.put("s", "G");
        solfaToChromatic.put("si", "G#");
        solfaToChromatic.put("l", "A");
        solfaToChromatic.put("ta", "A#");
        solfaToChromatic.put("t", "B");
        enharmonicEquivalents = new HashMap();
        enharmonicEquivalents.put("C#", "Db");
        enharmonicEquivalents.put("Db", "C#");
        enharmonicEquivalents.put("D#", "Eb");
        enharmonicEquivalents.put("Eb", "D#");
        enharmonicEquivalents.put("F#", "Gb");
        enharmonicEquivalents.put("Gb", "F#");
        enharmonicEquivalents.put("G#", "Ab");
        enharmonicEquivalents.put("Ab", "G#");
        enharmonicEquivalents.put("A#", "Bb");
        enharmonicEquivalents.put("Bb", "A#");
    }

}
