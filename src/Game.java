import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;

public class Game extends JPanel implements KeyListener {
    
    // Game state
    private final String difficulty;
    private long gameStartTime;
    private boolean gameStarted = false;
    private Timer gameTimer;
    
    // Images
    private static Image stageLeft, stageRight, yui, hitbar, hitpress;
    private static Image maniaHit300g, maniaHit300, maniaHit200, maniaHit100, maniaHit50, maniaHit0;
    private static Image maniaNoteImage, maniaNoteHead, maniaLongNote, maniaNoteTail;
    private static Image backGroundImage;

    // Game constants
    private static final int FPS = 240;
    private static final long FRAME_TIME = 1000L / FPS; 
    private static final int HITBAR_WIDTH = 125;
    private static final int HITBAR_HEIGHT = 230;
    private static final int HITBAR_Y = 700;
    private static final int NOTE_WIDTH = 125;
    private static final int NOTE_HEIGHT = 125;
    private static int NOTE_SPEED;
    private static double OD;
    private static String artist;
    private static String title;
    private static String mapper;

    // offset for song giving it time to adjust volume
    private static final int songOffset = 2000;
    
    // Game objects & judgement bar
    private final int[] hitbarX = new int[4];
    private final boolean[] keyPressed = new boolean[4];
    private final ArrayList<Note> notes = new ArrayList<>();
    private final ArrayList<LongNote> longNotes = new ArrayList<>();
    

    // Judgement display
    private static int lastJudgement;
    private static int totalNotes;
    private static int combo; 
    private static int maxCombo;
    private static int hits;
    private static double score;
    private static double accuracy = 100.00; // Default accuracy
    private static double accValue;
    private final HashMap<String, Integer> judgements = new LinkedHashMap<>();
    
    // Notes
    private static class Note {
        final int lane;
        final int offset;
        int y = 0;
        boolean active = false;
        boolean hit = false;
        
        Note(int lane, int offset) {
            this.lane = lane;
            this.offset = offset + songOffset;
        }
        
        // Calculate when the note should become active (start falling)
        int getActivationTime() {
            int fallDistance = HITBAR_Y + NOTE_HEIGHT;
            int fallTime = (int)((fallDistance * (1000.0 / FPS)) / NOTE_SPEED);
            int activationTime = offset - fallTime;
            return activationTime;
        }
    }
    
    // Long notes
    private static class LongNote {
        final int lane;
        final int offset; // time when note head overlaps with hitbar
        final int length; // time when note tail overlaps with hitbar after offset
        
        // Position tracking
        int headY = 0; // Head position
        int bodyY = 0; // Body position
        int tailY = 0; // Tail position
        
        // State tracking
        boolean active = false;
        boolean headHit = false;
        boolean tailHit = false;
        boolean isHeld = false;
       
        
        LongNote(int lane, int offset, int endTime) {
            this.lane = lane;            
            this.offset = offset + songOffset; // to sync with song
            this.length = endTime - offset;
        }
        
        // Calculate when the note should become active
        int getActivationTime() {
            int fallDistance = HITBAR_Y + getTotalHeight();
            int fallTime = (int)((fallDistance * (1000.0 / FPS)) / NOTE_SPEED);
            int activationTime = offset - fallTime;
            return activationTime;
        }
        
        // Get total height of the long note
        int getTotalHeight() {
            return getBodyHeight() + NOTE_HEIGHT * 2; // body + head + tail
        }
        
        // Get body height based on length
        int getBodyHeight() {
            return (int)((length * NOTE_SPEED) / (1000.0 / FPS));
        }
        
        // Update positions when falling normally
        void updatePositions(long currentTime) {
            if (currentTime <= getActivationTime()) {
                headY = -getTotalHeight();
                bodyY = headY - getBodyHeight(); // Body above head
                tailY = bodyY; // Tail above body
                return;
            }
            
            long timeSinceActivation = currentTime - getActivationTime();
            int pixelsFallen = (int)((timeSinceActivation * NOTE_SPEED) / (1000.0 / FPS));
            
            headY = -getTotalHeight() + pixelsFallen;
            bodyY = headY - getBodyHeight(); // Body above head
            tailY = bodyY; // Tail above body
        }
        
        // Update positions when held (head stays at hitbar, body shrinks from top)
        void updateHeldPositions(long currentTime) {
            if (!isHeld) return;
            
            // Head stays at hitbar
            headY = HITBAR_Y;
            
            // Calculate remaining body height based on time left
            long timeSinceHeadHit = currentTime - offset;
            long remainingTime = length - timeSinceHeadHit;
            int remainingBodyHeight = Math.max(0, (int)((remainingTime * NOTE_SPEED) / (1000.0 / FPS)));
            
            // Body shrinks from top, bottom stays with head
            bodyY = headY - remainingBodyHeight;
            tailY = bodyY; // Tail stays above body
        }
       
        
        // Get end time of the long note
        long getEndTime() {
            return offset + length;
        }
        
        // Check if note is over-held (not released in time)
    
    }

    private static class EndGamePanel extends JPanel {
        private final HashMap<String, Integer> judgements;
        private final int maxCombo;
        private final String difficulty;
        private Image rankingPanel;
        private Image rank;
        private int rankingX;
        private int rankingY;

        EndGamePanel(HashMap<String, Integer> judgements, int maxCombo, String difficulty) {
            this.judgements = judgements;
            this.maxCombo = maxCombo;
            this.difficulty = difficulty;
            if (accuracy >= 100.00) {
                rank = new ImageIcon("src/assets/ranking-X.png").getImage();
                rankingX = 1020;
                rankingY = 189;
            } else if (accuracy >= 95.00) {
                rank = new ImageIcon("src/assets/ranking-S.png").getImage();
                rankingX = 1019;
                rankingY = 190;
            } else if (accuracy >= 90.00) {
                rank = new ImageIcon("src/assets/ranking-A.png").getImage();
                rankingX = 1017;
                rankingY = 193;
            } else if (accuracy >= 80.00) {
                rank = new ImageIcon("src/assets/ranking-B.png").getImage();
                rankingX = 1018;
                rankingY = 195;               
            } else if (accuracy >= 70.00) {
                rank = new ImageIcon("src/assets/ranking-C.png").getImage();
                rankingX = 1019;
                rankingY = 197;
            } else {
                rank = new ImageIcon("src/assets/ranking-D.png").getImage();
                rankingX = 1020;
                rankingY = 198;
            }
            
            setBackground(Color.BLACK);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            // Draw background if Novice and image is loaded
            renderBackground(g);
            
            
        }

        void renderBackground(Graphics g) {
            renderImage(g, backGroundImage, 0, 0, getWidth(), getHeight(), 1.0f);
            
            
            rankingPanel = new ImageIcon("src/assets/ranking-panel.png").getImage();
            renderImage(g, rankingPanel, 0, 165, rankingPanel.getWidth(null), rankingPanel.getHeight(null), 1.0f);
            renderImage(g, rank, rankingX, rankingY, rank.getWidth(null), rank.getHeight(null), 1.0f);
            

            renderImage(g, maniaHit300g, 10, 275, maniaHit300g.getWidth(null) , maniaHit300g.getHeight(null) , 0.8f);
            renderImage(g, maniaHit300, 300, 275, maniaHit300.getWidth(null) , maniaHit300.getHeight(null) , 0.8f);
            renderImage(g, maniaHit200, 10, 375, maniaHit200.getWidth(null), maniaHit200.getHeight(null), 0.8f);
            renderImage(g, maniaHit100,305, 375, maniaHit100.getWidth(null), maniaHit100.getHeight(null), 0.8f);
            renderImage(g, maniaHit50, 10, 475, maniaHit50.getWidth(null), maniaHit50.getHeight(null), 0.8f);
            renderImage(g, maniaHit0, 305, 475, maniaHit0.getWidth(null), maniaHit0.getHeight(null), 0.8f);
            int i = 0;
            int x = 0;
            int y = 230;
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 34));
            for (String judgement : judgements.keySet()) { 
                if (i % 2 == 0) {
                    x = 10 + maniaHit300g.getWidth(null) + 125 - g.getFontMetrics().stringWidth(Integer.toString(judgements.get(judgement)));
                    y += 100;  
                } else {
                    x = 305 + maniaHit300g.getWidth(null) + 125 - g.getFontMetrics().stringWidth(Integer.toString(judgements.get(judgement)));
                }
                g.drawString(Integer.toString(judgements.get(judgement)), x, y);
                i++;
            }

            
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight() - rankingPanel.getHeight(null) - 50);

            if (this.maxCombo == totalNotes) {
                Image perfect = new ImageIcon("src/assets/ranking-perfect.png").getImage();
                renderImage(g, perfect, 290, 709, perfect.getWidth(null), perfect.getHeight(null), 1.0f);
            }
            

            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 28));
            g.drawString(this.maxCombo + " x", 50 - g.getFontMetrics().stringWidth(Integer.toString(this.maxCombo)) / 2, 600);
            g.drawString(String.format("%.2f", accuracy) + "%", 400, 600);
            g.setFont(new Font("Arial", Font.PLAIN, 30));
            g.drawString(artist + " - " + title + " [" + difficulty + "]", 20, 35);
            g.setFont(new Font("Arial", Font.PLAIN, 24));
            g.setColor(Color.lightGray);
            g.drawString("Mapped by " + mapper, 20, 70);          
            g.drawString("Played by Guest on " + getTime(), 20, 100);
            
            g.setColor(Color.GRAY);
            g.setFont(new Font("Arial", Font.PLAIN, 34));
            g.drawString((String.format("%.0f", score)), 400 - g.getFontMetrics().stringWidth(String.format("%.0f", score)) / 2 , 225);
        }

        String getTime() {
            DateTimeFormatter year = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            DateTimeFormatter exactTime = DateTimeFormatter.ofPattern("HH:mm:ss a");
            LocalDateTime now = LocalDateTime.now();
            return now.format(year) + " at " + now.format(exactTime);
        }

    }

    private void loadGameFromFile(String filename, String difficulty) {
        boolean find = false;
        try (Scanner scanner = new Scanner(new File(filename))) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (!find) {
                    if (line.equals("[HitObjects]")) {
                        find = true;
                    } 
                    if (line.startsWith("Artist:")) artist = line.substring(7).trim();
                    else if (line.startsWith("Title:")) title = line.substring(6).trim();
                    else if (line.startsWith("Creator:")) mapper = line.substring(8).trim();
                    else if (line.startsWith("OverallDifficulty:")) OD = Double.parseDouble(line.substring(18).trim());
                    continue;
                }
                // Now, after [HitObjects], process every line
                String[] parts = line.split("[,:]");
                int lane = Integer.parseInt(parts[0].trim());
                int offset = Integer.parseInt(parts[2].trim());
                if (lane == 64) {
                    lane = 0;
                } else if (lane == 192) {
                    lane = 1;
                } else if (lane == 320) {
                    lane = 2;
                } else if (lane == 448) {
                    lane = 3;
                }
                // Check if it's a long note (has length parameter)
                if (Integer.parseInt(parts[5].trim()) != 0) {   
                    int length = Integer.parseInt(parts[5].trim()); // length in milliseconds
                    synchronized (longNotes) {
                        longNotes.add(new LongNote(lane, offset, length));
                    }
                    totalNotes += 2;
                } else {
                    notes.add(new Note(lane, offset));
                    totalNotes++;
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading notes: " + e.getMessage());
        }

        
    }
    
    public Game(String difficulty) {
        this.difficulty = difficulty;
        try {
            Scanner scan = new Scanner((new File("src/config.txt")));
            String line;
            while (scan.hasNextLine()) {
                line = scan.nextLine();
                if (line.startsWith("Note Speed:")) NOTE_SPEED = Integer.valueOf(line.substring(11).trim());
                scan.close();
            }
        } catch (Exception e) {
           
        }

        OD = 0; // Default OD value
        initializeUI();
        loadImages();
        initializeJudgements();
        startGame();
    }
    
    private void initializeUI() {
        setLayout(null);
        setFocusable(true);
        addKeyListener(this);
    }
    
    private void loadImages() {
        try {
            stageLeft = new ImageIcon("src/assets/mania-stage-left.png").getImage();
            stageRight = new ImageIcon("src/assets/mania-stage-right.png").getImage();
            yui = new ImageIcon("src/assets/yui.png").getImage();
            hitbar = new ImageIcon("src/assets/hitbar.png").getImage();
            hitpress = new ImageIcon("src/assets/hitpress.png").getImage();
            maniaNoteImage = new ImageIcon("src/assets/mania-note.png").getImage();
            maniaHit300g = new ImageIcon("src/assets/mania-hit300g.png").getImage();
            maniaHit300 = new ImageIcon("src/assets/mania-hit300.png").getImage();
            maniaHit200 = new ImageIcon("src/assets/mania-hit200.png").getImage();
            maniaHit100 = new ImageIcon("src/assets/mania-hit100.png").getImage();
            maniaHit50 = new ImageIcon("src/assets/mania-hit50.png").getImage();
            maniaHit0 = new ImageIcon("src/assets/mania-hit0.png").getImage();
            maniaNoteHead = new ImageIcon("src/assets/mania-notehead.png").getImage();
            maniaNoteTail = new ImageIcon("src/assets/mania-notetail.png").getImage();
            maniaLongNote = new ImageIcon("src/assets/mania-longnote.png").getImage();
            
            // Calculate hitbar positions
            int startX = 450;
            int spacing = 150;
            for (int i = 0; i < 4; i++) {
                hitbarX[i] = startX + (i * spacing);
            }
        } catch (Exception e) {
            System.err.println("Error loading images: " + e.getMessage());
        }
    }
    
    private void initializeJudgements() {
        judgements.put("300g", 0);
        judgements.put("300", 0);
        judgements.put("200", 0);
        judgements.put("100", 0);
        judgements.put("50", 0);
        judgements.put("miss", 0);      
    }

    
    
    private void startGame() {
        switch (difficulty) {
            case "Beginner":
                startBeginnerGame();
                break;
            case "Easy":
                startEasyGame();
                break;
            case "Normal":
                startNormalGame();
                break;
            case "Hard":
                startHardGame();
                break;
            case "Insane":
                startInsaneGame();
                break;
            case "Extra1":
                startExtra1Game();
                break;
            case "Extra2":
                startExtra2Game();
                break;
        }
    }
    
    private void startGameTimer() {
        gameStartTime = System.currentTimeMillis();
        gameStarted = true;
        gameTimer = new Timer();
        gameTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateGame();
                repaint();
            }
        }, 0, FRAME_TIME);
    }
    
    private long getGameTime() {
        return gameStarted ? System.currentTimeMillis() - gameStartTime : 0;
    }
    
    private static void renderImage(Graphics g, Image image, int x, int y, int width, int height, float alpha) {
        Graphics2D g2D = (Graphics2D) g;
        Composite oldComposite = g2D.getComposite();
        g2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2D.drawImage(image, x, y, width, height, null);
        g2D.setComposite(oldComposite);
    }
    
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawBackground(g);
        drawStage(g);
        drawHitbars(g);  
        drawLongNotes(g);
        drawNotes(g);       
        drawCombo(g);
        drawJudgement(g);
        drawAccuracy(g);
        drawScore(g);
    }
    
    private void drawBackground(Graphics g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());
                
        if (backGroundImage != null) {
            renderImage(g, backGroundImage, 0, 0, getWidth(), getHeight(), 0.5f);
            g.setColor(Color.BLACK);
            g.fillRect(427, 0, 620, getHeight());
        }
    }
    
    private void drawStage(Graphics g) {
        renderImage(g, stageLeft, 425, 0, stageLeft.getWidth(null), 900, 1.0f);
        renderImage(g, stageRight, 1040, 0, stageRight.getWidth(null), 900, 1.0f);
        renderImage(g, yui, 950, 300, yui.getWidth(null) - 30, yui.getHeight(null) - 75, 1.0f);
    }
    
    private void drawHitbars(Graphics g) {
        for (int i = 0; i < 4; i++) {
            Image hitbarImage = keyPressed[i] ? hitpress : hitbar;
            renderImage(g, hitbarImage, hitbarX[i], HITBAR_Y, HITBAR_WIDTH, HITBAR_HEIGHT, 1.0f);
        }
    }
    
    private void drawJudgement(Graphics g) {
        if (lastJudgement == 0) return; // No judgement to display
        Image judgeImg = getJudgementImage(lastJudgement);
        int judgeX = getWidth() / 2 - judgeImg.getWidth(null) / 2;
        int judgeY = HITBAR_Y - 500;
        renderImage(g, judgeImg, judgeX, judgeY, judgeImg.getWidth(null), judgeImg.getHeight(null), 0.8f);        
    }

    private void drawCombo(Graphics g) {
        if (combo != 0) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 26));
            g.drawString(Integer.toString(combo), getWidth() / 2 - g.getFontMetrics().stringWidth(Integer.toString(combo)) / 2, HITBAR_Y - 400);
        }
    }

    private void drawAccuracy(Graphics g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 30)); 
        g.drawString("Accuracy: " + String.format("%.2f%%", accuracy), 1400 - g.getFontMetrics().stringWidth("Accuracy: " + String.format("%.2f%%", accuracy)), 100);
    }

    private void drawScore(Graphics g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 30));
        g.drawString("Score: " + String.format("%.0f", score), 1400 - g.getFontMetrics().stringWidth("Score: " + String.format("%.0f", score)), 50);
    }
    
    private Image getJudgementImage(int judgement) {
        switch (judgement) {
            case 1: return maniaHit0;
            case 2: return maniaHit50;
            case 3: return maniaHit100;
            case 4: return maniaHit200;
            case 5: return maniaHit300;
            case 6: return maniaHit300g;
            default: return null;
        }
    }
    
    private void drawNotes(Graphics g) {
        for (Note note : notes) {
            if (note.active) {
                renderImage(g, maniaNoteImage, hitbarX[note.lane], note.y, NOTE_WIDTH, NOTE_HEIGHT, 1.0f);
            }
        }
    }
    
    private void drawLongNotes(Graphics g) {
        for (LongNote longNote : longNotes) {
            if (longNote.active) {
                if (longNote.isHeld) {
                    // When held: head stays at hitbar, body shrinks from top, tail moves with body
                    // Draw the body (shrinking from top)
                    if (maniaLongNote != null) {
                        int remainingBodyHeight = Math.max(0, HITBAR_Y - longNote.bodyY);
                        if (remainingBodyHeight > 0) {
                            renderImage(g, maniaLongNote, hitbarX[longNote.lane], longNote.bodyY + NOTE_HEIGHT/2, NOTE_WIDTH, remainingBodyHeight, 1.0f);
                        }
                    }
                    // Draw the head at hitbar
                    if (maniaNoteHead != null) {
                        renderImage(g, maniaNoteHead, hitbarX[longNote.lane], HITBAR_Y, NOTE_WIDTH, NOTE_HEIGHT, 1.0f);
                    }
                    
                    
                    
                    
                    // Draw the tail (moving with body)
                    if (maniaNoteTail != null && longNote.tailY < HITBAR_Y) {
                        renderImage(g, maniaNoteTail, hitbarX[longNote.lane], longNote.tailY, NOTE_WIDTH, NOTE_HEIGHT/2, 1.0f);
                    }
                } else {
                    // Normal rendering when not held
                     // Draw the body 
                    if (maniaLongNote != null) {
                        renderImage(g, maniaLongNote, hitbarX[longNote.lane], longNote.bodyY + NOTE_HEIGHT/2, NOTE_WIDTH, longNote.getBodyHeight(), 1.0f);
                        
                    }
                    // Draw the head (bottom)
                    if (maniaNoteHead != null) {
                        renderImage(g, maniaNoteHead, hitbarX[longNote.lane], longNote.headY, NOTE_WIDTH, NOTE_HEIGHT, 1.0f);
                    }
                    
                   
                    
                    
                    // Draw the tail (top, above body)
                    if (maniaNoteTail != null) {
                        renderImage(g, maniaNoteTail, hitbarX[longNote.lane], longNote.tailY, NOTE_WIDTH, NOTE_HEIGHT/2, 1.0f);
                    }
                }
            }
        }
    }
    
private void updateGame() {
    long currentTime = getGameTime();
    
    // Null check for notes collection
    if (notes == null) {
        return;
    }
    
    // Activate notes based on timing and remove hit notes safely
    synchronized (notes) {
        Iterator<Note> noteIterator = notes.iterator();
        while (noteIterator.hasNext()) {
            Note note = noteIterator.next();
            
            // Null check for individual note
            if (note == null) {
                noteIterator.remove();
                continue;
            }
            
            if (note.hit) {
                noteIterator.remove();
                continue;
            }
            
            if (!note.active && currentTime >= note.getActivationTime()) {
                note.active = true;
            }
            
            // Update note position
            if (note.active && !note.hit) {
                long timeSinceActivation = currentTime - note.getActivationTime();
                int pixelsFallen = (int)((timeSinceActivation * NOTE_SPEED) / (1000.0 / FPS));
                note.y = -NOTE_HEIGHT + pixelsFallen;
                
                // Check if note passed hitbar using milliseconds
                if (currentTime > note.offset + ((188 - 3 * OD) * 2)) {
                    note.hit = true;
                    updateJudgement(188 - 3 * OD); // Count as a miss
                }
            }
        }
    }
    
    // Null check for longNotes collection
    if (longNotes == null) {
        return;
    }
    
    // Update long notes and remove safely
    synchronized (longNotes) {
        Iterator<LongNote> longNoteIterator = longNotes.iterator();
        while (longNoteIterator.hasNext()) {
            LongNote longNote = longNoteIterator.next();
            
            // Null check for individual long note
            if (longNote == null) {
                longNoteIterator.remove();
                continue;
            }
            
            if (!longNote.active && currentTime >= longNote.getActivationTime()) {
                longNote.active = true;
            }
            
            if (longNote.active) {
                if (!longNote.isHeld) {
                    // Update positions normally
                    longNote.updatePositions(currentTime);
                    
                    // Check if entire long note passed hitbar using milliseconds
                    if (currentTime - longNote.getEndTime() > ((188 - 3 * OD) * 2) && 
                        !longNote.tailHit && !longNote.headHit) {
                        longNote.headHit = true;
                        longNote.tailHit = true;
                        
                        updateJudgement(188 - 3 * OD);
                        updateJudgement(188 - 3 * OD); // Count as 2 misses
                    }
                } else {
                    // Head has been hit, handle holding and tail logic
                    if (currentTime - longNote.getEndTime() >= ((188 - 3 * OD) * 1.5) && 
                        longNote.headHit) {
                        // Late release is considered a miss
                        longNote.tailHit = true;
                        updateJudgement(188 - 3 * OD);
                    } else {
                        longNote.updateHeldPositions(currentTime);
                    }
                }
                
                // Remove if both head and tail are hit
                if (longNote.tailHit && longNote.headHit) {
                    longNoteIterator.remove();
                }
            }
        }
    }
    
    if (totalNotes == hits) {
        end();
    }
}

    
    
    private void startBeginnerGame() {
        backGroundImage = new ImageIcon("src/assets/yuibg.png").getImage();
        try {
            SimpleAudioPlayer song = new SimpleAudioPlayer("src/music/Harumachi.wav", false);
            song.setVolume(-10.0f);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    song.clip.start();
                }
            }, songOffset);
        } catch (Exception e) {
            System.err.println("Error loading audio: " + e.getMessage());
        }
        
        loadGameFromFile("src/harumachi.txt", difficulty);
                
        startGameTimer();
        
    }
    
    private void startEasyGame() {
        backGroundImage = new ImageIcon("src/assets/Runengon.png").getImage();
        // Play audio
        try {
            SimpleAudioPlayer song = new SimpleAudioPlayer("src/music/Runengon.wav", false);
            song.setVolume(-10.0f);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    song.clip.start();
                }
            }, songOffset);
        } catch (Exception e) {
            System.err.println("Error loading audio: " + e.getMessage());
        }
        
        loadGameFromFile("src/Runengon.txt", difficulty);
        
        // Start game timer after song offset to synchronize with audio
       
        startGameTimer();
        
        // Add advanced difficulty specific logic here
    }
    
    private void startNormalGame() {
        backGroundImage = new ImageIcon("src/assets/O2i3.png").getImage();
        try {
            SimpleAudioPlayer song = new SimpleAudioPlayer("src/music/TSLove.wav", false);
            song.setVolume(-10.0f);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    song.clip.start();
                }
            }, songOffset);
        } catch (Exception e) {
            System.err.println("Error loading audio: " + e.getMessage());
        }
        
        loadGameFromFile("src/TSLove.txt", difficulty);
        
        // Start game timer after song offset to synchronize with audio
       
        startGameTimer();

        // Add maximum difficulty specific logic here
    }

    private void startHardGame() {
        backGroundImage = new ImageIcon("src/assets/rkc.png").getImage();
        try {
            SimpleAudioPlayer song = new SimpleAudioPlayer("src/music/Six trillion years and overnight story.wav", false);
            song.setVolume(-10.0f);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    song.clip.start();
                }
            }, songOffset);
        } catch (Exception e) {
            System.err.println("Error loading audio: " + e.getMessage());
        }
        loadGameFromFile("src/Six trillion years and overnight story.txt", difficulty);
        startGameTimer();
    }
    
    private void startInsaneGame() {
        backGroundImage = new ImageIcon("src/assets/cyanine.png").getImage();
        try {
            SimpleAudioPlayer song = new SimpleAudioPlayer("src/music/cyanine.wav", false);
            song.setVolume(-10.0f);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    song.clip.start();
                }
            }, songOffset);
        } catch (Exception e) {
            System.err.println("Error loading audio: " + e.getMessage());
        }
        loadGameFromFile("src/cyanine.txt", difficulty);
        startGameTimer();
    }

    private void startExtra1Game() {
        backGroundImage = new ImageIcon("src/assets/galaxy collapse.png").getImage();
        try {
            SimpleAudioPlayer song = new SimpleAudioPlayer("src/music/Galaxy Collapse.wav", false);
            song.setVolume(-20.0f);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    song.clip.start();
                }
            }, songOffset);
        } catch (Exception e) {
            System.err.println("Error loading audio: " + e.getMessage());
        }
        loadGameFromFile("src/Galaxy Collapse.txt", difficulty);
        startGameTimer();
    }

    private void startExtra2Game() {
        backGroundImage = new ImageIcon("src/assets/yoru.png").getImage();
        try {
            SimpleAudioPlayer song = new SimpleAudioPlayer("src/music/yoru.wav", false);
            song.setVolume(-20.0f);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    song.clip.start();
                }
            }, songOffset);
        } catch (Exception e) {
            System.err.println("Error loading audio: " + e.getMessage());
        }
        loadGameFromFile("src/yoru.txt", difficulty);
        startGameTimer();

    }
    private void handleKeyPress(int lane) {
    // Null check for keyPressed array
    if (keyPressed == null || lane < 0 || lane >= keyPressed.length) {
        return;
    }
    
    if (keyPressed[lane]) return; // Prevent repeated registration
    keyPressed[lane] = true;
    
    // Check for long notes (both over-holding and new head hits)
    LongNote closestLongNote = null;
    long minTimeDistance = Long.MAX_VALUE;

    // Null check for longNotes collection and synchronize access
    if (longNotes != null) {
        synchronized (longNotes) {
            for (LongNote longNote : longNotes) {
                // Null check for individual long note
                if (longNote != null && longNote.active && !longNote.headHit && longNote.lane == lane) {
                    long currentTime = getGameTime();
                    long timeDistance = currentTime - longNote.offset;
                    if (Math.abs(timeDistance) < Math.abs(minTimeDistance)) {
                        minTimeDistance = timeDistance;
                        closestLongNote = longNote;
                    }
                }
            }
        }
    }
    
    // Check for regular notes
    Note closestNote = null;
    long minNoteTimeDistance = Long.MAX_VALUE;
    
    // Null check for notes collection and synchronize access
    if (notes != null) {
        synchronized (notes) {
            for (Note note : notes) {
                // Null check for individual note
                if (note != null && note.active && !note.hit && note.lane == lane) {
                    long currentTime = getGameTime();
                    long timeDistance = Math.abs(currentTime - note.offset);
                    
                    // Prevent hitting a note that is after the closest long note's tail
                    if (closestLongNote != null && note.offset > closestLongNote.getEndTime()) {
                        continue;
                    }
                    
                    if (timeDistance < minNoteTimeDistance) {
                        minNoteTimeDistance = timeDistance;
                        closestNote = note;
                    }
                }
            }
        }
    }
    
    // Process regular note hit
    if (closestNote != null && minNoteTimeDistance <= (188 - 3 * OD)) {
        closestNote.hit = true;
        updateJudgement((int)minNoteTimeDistance);
    }
    
    // Process long note hit
    if (closestLongNote != null && closestNote == null) {
        if (Math.abs(minTimeDistance) <= ((188 - 3 * OD))) {
            // Head hit within acceptable range
            closestLongNote.headHit = true;
            closestLongNote.isHeld = true;
            updateJudgement((int)Math.abs(minTimeDistance));
        } else if (minTimeDistance < closestLongNote.length && minTimeDistance > (188 - 3 * OD)) {
            // Late 50 hit result in miss
            closestLongNote.headHit = true;
            closestLongNote.isHeld = true;
            updateJudgement(188 - 3 * OD);
        } else {
            return;
        }
    }
}
    
    private void handleKeyRelease(int lane) {
    keyPressed[lane] = false;
    LongNote closestLongNote = null;
    long minTimeDistance = Long.MAX_VALUE;
    
    if (longNotes != null) {
        synchronized (longNotes) {
            for (LongNote longNote : longNotes) {
                if (longNote.active && longNote.isHeld && longNote.lane == lane) {
                    long currentTime = getGameTime();
                    long timeDistance = Math.abs(currentTime - longNote.getEndTime());
                    if (timeDistance < minTimeDistance) {
                        minTimeDistance = timeDistance;
                        closestLongNote = longNote;
                    }
                }
            }
        }
        
        if (closestLongNote != null && minTimeDistance <= ((188 - 3 * OD)*1.5)) {
            long currentTime = getGameTime();            
            closestLongNote.tailHit = true;
            if (minTimeDistance <= ((151 - 3 * OD)*1.5)) {
                long timeDistance = Math.abs(currentTime - closestLongNote.getEndTime());
                updateJudgement(Math.round(timeDistance / 1.5)); // Hold note tail release windows become 1.5x longer.           
            } else {
                updateJudgement(151 - 3 * OD); 
            }
            // Remove after release
        } else if (closestLongNote != null) {
            // Early release
            closestLongNote.tailHit = true;
            updateJudgement(151 - 3 * OD);      
        }
    }
}   
    
    private void updateJudgement(double timeDistance) {
        String judgement;
        if (timeDistance <= (OD <= 5 ? (22.4 - 0.6 * OD):(24.9 - 1.1 * OD))) {
            judgement = "300g";
            lastJudgement = 6;
            combo++;
            accValue += 1.0;
            score += (1000000 / totalNotes) * 1.0;       
        } else if (timeDistance <= (64 - 3 * OD)) {
            judgement = "300";
            lastJudgement = 5;
            combo++;
            accValue += 1.0;
            score += (1000000 / totalNotes) * (3.0 / 3.2);
        } else if (timeDistance <= (97 - 3 * OD)) {
            judgement = "200";
            lastJudgement = 4;
            combo++;
            accValue += 2.0/3.0;
            score += (1000000 / totalNotes) * (2.0 / 3.2);
        } else if (timeDistance <= (127 - 3 * OD)) {
            judgement = "100";
            lastJudgement = 3;
            combo++;
            accValue += 1.0/3.0;
            score += (1000000 / totalNotes) * (1.0 / 3.2);
        } else if (timeDistance <= (151 - 3 * OD)) {
            judgement = "50";
            lastJudgement = 2;
            combo++;
            accValue += 1.0/6.0;
            score += (1000000 / totalNotes) * (0.5 / 3.2);
        } else if (timeDistance <= (188 - 3 * OD)) {
            judgement = "miss";
            lastJudgement = 1;
            combo = 0;
            accValue += 0.0;      
        } else {
            return;
        }

        judgements.put(judgement, judgements.get(judgement) + 1);
        maxCombo = Math.max(maxCombo, combo);       
        // Update accuracy
        updateAccuracy(accValue);
        
    }

    private void updateAccuracy(double accValue) {
        hits++;
        accuracy = (double)(accValue / hits * 100.0);    
    }

    private void end() {
        try {
            Thread.sleep(1000); // Wait for 1 second before ending the game
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        frame.getContentPane().removeAll();
        EndGamePanel endGamePanel = new EndGamePanel(judgements, maxCombo, difficulty);
        frame.getContentPane().add(endGamePanel);
        frame.revalidate();
        frame.repaint();
        gameTimer.cancel(); // Stop the game timer
        gameStarted = false; // Mark game as not started
    }

  
    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_D:
                handleKeyPress(0);
                
                break;
            case KeyEvent.VK_F:
                handleKeyPress(1);
                
                break;
            case KeyEvent.VK_J:
                handleKeyPress(2);
                
                break;
            case KeyEvent.VK_K:
                handleKeyPress(3);
                
                break;
        }
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_D:
                handleKeyRelease(0);
                break;
            case KeyEvent.VK_F:
                handleKeyRelease(1);
                break;
            case KeyEvent.VK_J:
                handleKeyRelease(2);
                break;
            case KeyEvent.VK_K:
                handleKeyRelease(3);
                break;
        }
    }
    
    @Override
    public void keyTyped(KeyEvent e) {
        // Not used
    }
}