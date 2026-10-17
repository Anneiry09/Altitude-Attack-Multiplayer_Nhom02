package audio;

import javax.sound.sampled.*;
import gui.AssetUtils;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SoundManager {
    private static SoundManager instance;

    private final ExecutorService sfxExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "SFX-Worker");
        t.setDaemon(true);
        return t;
    });

    private SourceDataLine bgmLine;
    private Thread bgmThread;
    private volatile boolean isBgmRunning = false;
    private volatile boolean isBgmPaused = false;
    private volatile boolean isBgmMuted = false;
    private volatile boolean isSfxMuted = false;
    private volatile float bgmVolume = 0.65f; // Mặc định 65% âm lượng nhạc nền
    private volatile float sfxVolume = 0.85f; // Mặc định 85% âm lượng hiệu ứng
    private String currentBgmFileName = "Darkwave.wav";

    private long lastHoverTime = 0;

    private SoundManager() {}

    public static synchronized SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }

    private File findSoundFile(String fileName) {
        return AssetUtils.findSound(fileName);
    }

    public String getCurrentBgmFileName() {
        return currentBgmFileName;
    }

    public synchronized void switchBGM(String fileName) {
        if (fileName == null || fileName.equals(this.currentBgmFileName)) return;
        this.currentBgmFileName = fileName;
        stopBGM();
        playBGM(fileName);
    }

    // ==========================================
    // 1. NHẠC NỀN BGM (SourceDataLine Streaming)
    // ==========================================
    public synchronized void playBGM(String fileName) {
        if (fileName != null) {
            this.currentBgmFileName = fileName;
        }

        if (bgmThread != null && bgmThread.isAlive()) {
            if (isBgmPaused) {
                resumeBGM();
            }
            return;
        }

        isBgmRunning = true;
        isBgmPaused = false;

        bgmThread = new Thread(() -> {
            byte[] buffer = new byte[16384];
            while (isBgmRunning) {
                File file = findSoundFile(currentBgmFileName);
                if (!file.exists()) {
                    System.err.println("[SoundManager] BGM not found: " + currentBgmFileName);
                    break;
                }

                try (AudioInputStream ais = AudioSystem.getAudioInputStream(file)) {
                    AudioFormat baseFormat = ais.getFormat();
                    AudioFormat decodedFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        baseFormat.getSampleRate(),
                        16,
                        baseFormat.getChannels(),
                        baseFormat.getChannels() * 2,
                        baseFormat.getSampleRate(),
                        false
                    );

                    try (AudioInputStream pcmStream = AudioSystem.getAudioInputStream(decodedFormat, ais)) {
                        synchronized (SoundManager.this) {
                            if (bgmLine == null || !bgmLine.isOpen()) {
                                DataLine.Info info = new DataLine.Info(SourceDataLine.class, decodedFormat);
                                bgmLine = (SourceDataLine) AudioSystem.getLine(info);
                                bgmLine.open(decodedFormat, 65536);
                                bgmLine.start();
                                applyBgmVolume();
                            }
                        }

                        int bytesRead;
                        while (isBgmRunning && (bytesRead = pcmStream.read(buffer, 0, buffer.length)) != -1) {
                            while (isBgmPaused && isBgmRunning) {
                                try {
                                    Thread.sleep(80);
                                } catch (InterruptedException ignored) {}
                            }
                            if (!isBgmRunning) break;
                            if (bgmLine != null && bgmLine.isOpen()) {
                                bgmLine.write(buffer, 0, bytesRead);
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[SoundManager] BGM stream loop error: " + e.getMessage());
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {}
                }
            }

            // Dọn dẹp line khi dừng hẳn
            synchronized (SoundManager.this) {
                if (bgmLine != null) {
                    try {
                        bgmLine.stop();
                        bgmLine.close();
                    } catch (Exception ignored) {}
                    bgmLine = null;
                }
            }
        }, "BGM-Stream-Thread");

        bgmThread.setDaemon(true);
        bgmThread.start();
        System.out.println("[SoundManager] Started BGM streaming: " + currentBgmFileName);
    }

    public synchronized void pauseBGM() {
        isBgmPaused = true;
        if (bgmLine != null && bgmLine.isRunning()) {
            bgmLine.stop();
        }
    }

    public synchronized void resumeBGM() {
        isBgmPaused = false;
        if (bgmLine != null && !bgmLine.isRunning()) {
            bgmLine.start();
            applyBgmVolume();
        } else if (bgmThread == null || !bgmThread.isAlive()) {
            playBGM(currentBgmFileName);
        }
    }

    public synchronized void stopBGM() {
        isBgmRunning = false;
        isBgmPaused = false;
        if (bgmLine != null) {
            try {
                bgmLine.stop();
                bgmLine.close();
            } catch (Exception ignored) {}
            bgmLine = null;
        }
        if (bgmThread != null) {
            bgmThread.interrupt();
            bgmThread = null;
        }
    }

    public synchronized void toggleBgmMute() {
        isBgmMuted = !isBgmMuted;
        applyBgmVolume();
    }

    public boolean isBgmMuted() {
        return isBgmMuted;
    }

    public synchronized void setBgmVolume(float vol) {
        this.bgmVolume = Math.max(0.0f, Math.min(1.0f, vol));
        applyBgmVolume();
    }

    public float getBgmVolume() {
        return bgmVolume;
    }

    private void applyBgmVolume() {
        if (bgmLine == null || !bgmLine.isOpen()) return;
        setLineGain(bgmLine, isBgmMuted ? 0.0f : bgmVolume);
    }

    // ==========================================
    // 2. HIỆU ỨNG ÂM THANH SFX (Sound Effects)
    // ==========================================
    public void playSfx(String fileName) {
        if (isSfxMuted || fileName == null) return;

        sfxExecutor.submit(() -> {
            try {
                File file = findSoundFile(fileName);
                if (!file.exists()) return;

                try (AudioInputStream ais = AudioSystem.getAudioInputStream(file)) {
                    AudioFormat baseFormat = ais.getFormat();
                    AudioFormat decodedFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        baseFormat.getSampleRate(),
                        16,
                        baseFormat.getChannels(),
                        baseFormat.getChannels() * 2,
                        baseFormat.getSampleRate(),
                        false
                    );

                    AudioInputStream playStream = ais;
                    AudioInputStream pcmStream = null;
                    try {
                        pcmStream = AudioSystem.getAudioInputStream(decodedFormat, ais);
                        playStream = pcmStream;
                    } catch (Exception ignored) {}

                    Clip clip = AudioSystem.getClip();
                    clip.addLineListener(event -> {
                        if (event.getType() == LineEvent.Type.STOP) {
                            clip.close();
                        }
                    });
                    clip.open(playStream);
                    setClipGain(clip, sfxVolume);
                    clip.start();

                    if (pcmStream != null) {
                        try { pcmStream.close(); } catch (Exception ignored) {}
                    }
                }
            } catch (Exception ignored) {}
        });
    }

    public void playClick() {
        playSfx("ui_click.wav");
    }

    public void playHover() {
        // Chống spam tiếng hover quá dày đặc khi lướt chuột nhanh
        long now = System.currentTimeMillis();
        if (now - lastHoverTime >= 70) {
            lastHoverTime = now;
            playSfx("ui_hover.wav");
        }
    }

    public void playCancel() {
        playSfx("ui_cancel.wav");
    }

    public void playInvite() {
        playSfx("ui_invite.wav");
    }

    public void playCountdown() {
        playSfx("ui_countdown.wav");
    }

    public synchronized void toggleSfxMute() {
        isSfxMuted = !isSfxMuted;
    }

    public synchronized boolean isSfxMuted() {
        return isSfxMuted;
    }

    public synchronized void setSfxVolume(float vol) {
        this.sfxVolume = Math.max(0.0f, Math.min(1.0f, vol));
    }

    public synchronized float getSfxVolume() {
        return sfxVolume;
    }

    private void setLineGain(Line line, float linearVolume) {
        try {
            if (line != null && line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
                if (linearVolume <= 0.001f) {
                    gainControl.setValue(gainControl.getMinimum());
                } else {
                    float dB = (float) (Math.log10(linearVolume) * 20.0);
                    dB = Math.max(gainControl.getMinimum(), Math.min(gainControl.getMaximum(), dB));
                    gainControl.setValue(dB);
                }
            }
        } catch (Exception ignored) {}
    }

    private void setClipGain(Clip clip, float linearVolume) {
        setLineGain(clip, linearVolume);
    }
}
