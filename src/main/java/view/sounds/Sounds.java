package view.sounds;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class Sounds {
    public static void playSound(String filePath) {
        try {
            File soundFile = new File(filePath);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundFile);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            clip.start();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("Erreur lors de la lecture du son : " + e.getMessage());
        }
    }
} // Example use : Sounds.playSound("src/main/java/view/sounds/example_Sounds.wav");