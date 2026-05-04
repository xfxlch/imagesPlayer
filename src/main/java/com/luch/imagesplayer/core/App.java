package com.luch.imagesplayer.core;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.SplashScreen;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

/**
 * Image viewer with background music playback (Swing).
 */
public class App {

    // ── Constants ──
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final String TITLE = "零五计算机-PictureViewer";
    private static final int SLIDE_INTERVAL_MS = 1500;
    private static final String IMAGE_DIR_NAME = "image";
    private static final String MP3_FILE_NAME = "icanplay.mp3";
    private static final int AUDIO_BUFFER_SIZE = 4096;
    private static final Logger log = Logger.getLogger(App.class);

    // ── UI Components ──
    private JFrame frame;
    private JPanel imagePanel;
    private JLabel imageLabel;
    private JScrollPane scrollPane;

    // ── State ──
    private File[] imageFiles;

    // ── Paths ──
    private final String imageDirPath;
    private final String mp3Path;

    // ── Executors (daemon threads so they don't block JVM exit) ──
    private final ExecutorService slideshowExecutor =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "slideshow");
                t.setDaemon(true);
                return t;
            });
    private final ExecutorService musicExecutor =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "music");
                t.setDaemon(true);
                return t;
            });

    // ── Splash ──
    private final SplashScreen splash = SplashScreen.getSplashScreen();
    private Rectangle splashBounds;
    private Graphics2D splashGraphics;

    // ═══════════════════════════════════════════════════
    //  Entry point
    // ═══════════════════════════════════════════════════

    public static void main(String[] args) {
        SwingUtilities.invokeLater(App::new);
    }

    // ═══════════════════════════════════════════════════
    //  Constructor
    // ═══════════════════════════════════════════════════

    public App() {
        // Resolve paths (cross-platform)
        String userDir = System.getProperty("user.dir");
        this.imageDirPath = userDir + File.separator + IMAGE_DIR_NAME;
        this.mp3Path = imageDirPath + File.separator + MP3_FILE_NAME;

        drawSplashScreen();
        initUI();
        startSlideshow();
        startMusic();
    }

    // ═══════════════════════════════════════════════════
    //  Splash Screen
    // ═══════════════════════════════════════════════════

    private void drawSplashScreen() {
        initSplash();
        if (splash == null) return;

        String[] stages = {"stage 1", "stage 2", "stage 3"};
        int stage = 0;
        for (int i = 0; i <= 100; i += 5) {
            String status = "Loading " + stages[stage] + "...";
            updateSplash(status, i);
            try {
                Thread.sleep(100);
                if (i == 30) stage = 1;
                else if (i == 60) stage = 2;
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        splash.close();
    }

    private void initSplash() {
        if (splash == null) {
            log.warn("No splash screen found (use -splash:path/to/image to enable)");
            return;
        }
        splashBounds = splash.getBounds();
        splashGraphics = splash.createGraphics();
        if (splashGraphics == null) {
            log.error("Cannot create Graphics2D for splash screen");
        } else {
            splashGraphics.setColor(Color.GREEN);
            splashGraphics.drawRect(0, 0, splashBounds.width - 1, splashBounds.height - 1);
        }
    }

    private void updateSplash(String status, int progress) {
        if (splash == null || splashGraphics == null) return;
        drawSplashProgress(splashGraphics, status, progress);
        splash.update();
    }

    private void drawSplashProgress(Graphics2D g, String status, int progress) {
        int barWidth = splashBounds.width * 50 / 100;
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(1, 10, splashBounds.width - 2, 20);
        g.setPaintMode();
        g.setColor(Color.BLACK);
        g.drawString(status, 10, 20);
        g.setColor(Color.BLACK);
        g.drawRect(10, 25, barWidth + 2, 10);
        g.setColor(Color.YELLOW);
        int w = progress * barWidth / 100;
        g.fillRect(11, 26, w + 1, 9);
        g.setColor(Color.WHITE);
        g.fillRect(11 + w + 1, 26, barWidth - w, 9);
    }

    // ═══════════════════════════════════════════════════
    //  UI Initialization
    // ═══════════════════════════════════════════════════

    private void initUI() {
        log.info("Initializing UI");

        // Load image files
        File dir = new File(imageDirPath);
        imageFiles = (dir.isDirectory() && dir.canRead()) ? dir.listFiles(new PicFilter()) : null;
        if (imageFiles == null || imageFiles.length == 0) {
            log.warn("No images found in: " + imageDirPath);
            imageFiles = new File[0];
        } else {
            log.info("Found " + imageFiles.length + " images");
        }

        // Build frame
        frame = new JFrame();
        frame.setSize(WIDTH, HEIGHT);
        frame.setLocationRelativeTo(null);
        frame.setTitle(TITLE);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);

        // Menu bar
        JMenuBar menuBar = new JMenuBar();
        frame.setJMenuBar(menuBar);

        JMenu fileMenu = new JMenu("File");
        JMenu aboutMenu = new JMenu("About");
        menuBar.add(fileMenu);
        menuBar.add(aboutMenu);

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> {
            log.info("Exiting application");
            shutdown();
        });
        fileMenu.add(exitItem);

        JMenuItem aboutItem = new JMenuItem("About Me");
        aboutItem.addActionListener(e ->
            JOptionPane.showMessageDialog(frame, "作者：Jack \nEmail： luch2046@163.com")
        );
        aboutMenu.add(aboutItem);

        // Image display area
        imageLabel = new JLabel();
        imagePanel = new JPanel();
        imagePanel.add(imageLabel);
        scrollPane = new JScrollPane(imagePanel);
        frame.add(scrollPane, BorderLayout.CENTER);

        frame.setVisible(true);
    }

    // ═══════════════════════════════════════════════════
    //  Slideshow
    // ═══════════════════════════════════════════════════

    private void startSlideshow() {
        slideshowExecutor.submit(this::slideshowLoop);
    }

    private void slideshowLoop() {
        if (imageFiles.length == 0) {
            log.warn("No images to display");
            return;
        }
        while (!Thread.currentThread().isInterrupted()) {
            for (File file : imageFiles) {
                if (Thread.currentThread().isInterrupted()) return;
                log.info("Showing: " + file.getName());
                SwingUtilities.invokeLater(() -> setImage(file));
                try {
                    Thread.sleep(SLIDE_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════
    //  Music Playback
    // ═══════════════════════════════════════════════════

    private void startMusic() {
        musicExecutor.submit(this::musicLoop);
    }

    private void musicLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                playMusicOnce();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.error("Music playback error, will retry", e);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private void playMusicOnce() throws Exception {
        File mp3File = new File(mp3Path);
        if (!mp3File.exists()) {
            log.warn("MP3 file not found: " + mp3Path);
            Thread.sleep(10000);
            return;
        }

        AudioInputStream ais = null;
        SourceDataLine line = null;
        try {
            ais = AudioSystem.getAudioInputStream(mp3File);
            AudioFormat baseFormat = ais.getFormat();
            AudioFormat targetFormat = baseFormat;

            // Convert to PCM_SIGNED if needed (for MP3/WAV etc.)
            if (baseFormat.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
                targetFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.getSampleRate(), 16,
                    baseFormat.getChannels(),
                    baseFormat.getChannels() * 2,
                    baseFormat.getSampleRate(), false
                );
                ais = AudioSystem.getAudioInputStream(targetFormat, ais);
            }

            DataLine.Info info = new DataLine.Info(SourceDataLine.class, targetFormat);
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(targetFormat);
            line.start();

            byte[] buffer = new byte[AUDIO_BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = ais.read(buffer, 0, buffer.length)) != -1) {
                if (Thread.currentThread().isInterrupted()) break;
                line.write(buffer, 0, bytesRead);
            }
            line.drain();
        } finally {
            if (line != null) {
                line.stop();
                line.close();
            }
            if (ais != null) {
                ais.close();
            }
        }
    }

    // ═══════════════════════════════════════════════════
    //  Image Display Helpers
    // ═══════════════════════════════════════════════════

    private void setImage(File file) {
        ImageIcon icon = new ImageIcon(file.getAbsolutePath());
        int containerW = scrollPane.getWidth();
        int containerH = scrollPane.getHeight();
        int imgW = icon.getIconWidth();
        int imgH = icon.getIconHeight();

        if (containerW <= 0 || containerH <= 0) {
            // Frame not yet laid out; just display at original size
            imagePanel.setLayout(new java.awt.FlowLayout());
            imageLabel.setIcon(icon);
            return;
        }

        // Scale down if larger than container
        if (imgW > containerW || imgH > containerH) {
            double scale = Math.min(
                (double) (containerW - 20) / imgW,
                (double) (containerH - 20) / imgH
            );
            int scaledW = Math.max(1, (int) (imgW * scale));
            int scaledH = Math.max(1, (int) (imgH * scale));
            Image scaled = scaleImage(file, scaledW, scaledH);
            if (scaled != null) {
                icon.setImage(scaled);
            }
            imagePanel.setLayout(null);
            imageLabel.setBounds((containerW - scaledW) / 2, (containerH - scaledH) / 2, scaledW, scaledH);
        } else {
            imagePanel.setLayout(new java.awt.FlowLayout());
        }
        imageLabel.setIcon(icon);
    }

    private Image scaleImage(File file, int width, int height) {
        try {
            BufferedImage bi = javax.imageio.ImageIO.read(file);
            if (bi == null) {
                log.warn("Unable to read image: " + file.getAbsolutePath());
                return null;
            }
            return bi.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        } catch (IOException e) {
            log.error("Failed to read image: " + file.getAbsolutePath(), e);
            return null;
        }
    }

    // ═══════════════════════════════════════════════════
    //  Shutdown
    // ═══════════════════════════════════════════════════

    private void shutdown() {
        slideshowExecutor.shutdownNow();
        musicExecutor.shutdownNow();
        System.exit(0);
    }

    // ═══════════════════════════════════════════════════
    //  Image Filter
    // ═══════════════════════════════════════════════════

    private static final class PicFilter implements FilenameFilter {
        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith("jpg") || name.endsWith("gif") || name.endsWith("png");
        }
    }
}
