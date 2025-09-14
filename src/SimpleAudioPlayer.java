// this class is taken from a forum and modified by me
// all credit goes to the original author

import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class SimpleAudioPlayer {

    // to store current position
    Long currentFrame;
    Clip clip;
    
    // current status of clip
    String status;
    
    AudioInputStream audioInputStream;
    private String filePath;

    // constructor to initialize streams and clip
    public SimpleAudioPlayer(String filePath, Boolean loop) throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        this.filePath = filePath;
        // create AudioInputStream object
        audioInputStream = AudioSystem.getAudioInputStream(new File(this.filePath).getAbsoluteFile());
        
        // create clip reference
        clip = AudioSystem.getClip();
        
        // open audioInputStream to the clip
        clip.open(audioInputStream);
        
        if (loop) {
            // loop the clip continuously
            clip.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }
    
    // Method to play the audio
    public void play() 
    {
        //start the clip
        clip.start();
        
        status = "play";
    }
    
    // Method to pause the audio
    public void pause() 
    {
        if (status.equals("paused")) {
            System.out.println("audio is already paused");
            return;
        }
        this.currentFrame = 
        this.clip.getMicrosecondPosition();
        clip.stop();
        status = "paused";
    }
    
    // Method to resume the audio
    public void resume() throws UnsupportedAudioFileException,
                                IOException, LineUnavailableException 
    {
        if (status.equals("play")) 
        {
            System.out.println("Audio is already "+
            "being played");
            return;
        }
        clip.close();
        resetAudioStream();
        clip.setMicrosecondPosition(currentFrame);
        this.play();
    }
    
    // Method to restart the audio
    public void restart() throws IOException, LineUnavailableException,
                                            UnsupportedAudioFileException 
    {
        clip.stop();
        clip.close();
        resetAudioStream();
        currentFrame = 0L;
        clip.setMicrosecondPosition(0);
        this.play();
    }
    
    // Method to stop the audio
    public void stop() throws UnsupportedAudioFileException,
    IOException, LineUnavailableException 
    {
        currentFrame = 0L;
        clip.stop();
        clip.close();
    }
    

    
    // Method to reset audio stream
    public void resetAudioStream() throws UnsupportedAudioFileException, IOException,
                                            LineUnavailableException 
    {
        audioInputStream = AudioSystem.getAudioInputStream(
        new File(this.filePath).getAbsoluteFile());
        clip.open(audioInputStream);
        clip.loop(Clip.LOOP_CONTINUOUSLY);
    }

    public void setVolume(float volume) {
        if (clip.isControlSupported(javax.sound.sampled.FloatControl.Type.MASTER_GAIN)) {
            javax.sound.sampled.FloatControl gainControl = 
                (javax.sound.sampled.FloatControl) clip.getControl(javax.sound.sampled.FloatControl.Type.MASTER_GAIN);
            gainControl.setValue(volume);
        } else {
            System.out.println("Volume control not supported");
        }
    }

    public boolean isEnded() {
        if (clip.getMicrosecondPosition() == clip.getMicrosecondLength()) {
            return true;
        }
        return false;
    }

}