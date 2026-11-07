package com.aigen.sims;

import javafx.application.Platform;
import javafx.stage.Stage;

import java.awt.AWTException;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * 2026-11-06: close-to-tray + one copy per box.
 *
 * - The window's X hides GodHand to the tray; the daemons keep running.
 * - Tray right-click: "Show GodHand" / "Exit GodHand". Double-click = show.
 * - Exit is a real exit (System.exit), because the swarm daemons are
 *   non-daemon threads and used to keep the JVM alive after a close.
 * - 127.0.0.1:7787 is the doorbell. A second launch that finds it taken
 *   sends SHOW to the first copy and quits, so a relaunch brings the GUI up.
 */
public final class ViperTray {

    public static final int PORT = 7787;
    private static ServerSocket bell;
    private static TrayIcon icon;
    private static boolean hintShown;

    private ViperTray() {}

    /** True when another copy answered the doorbell; the caller should quit. */
    public static boolean ringExisting() { return ringExisting(PORT); }

    /** Port-parameterised so tests never touch the live GodHand's :7787 (Hermes t_a59be94b). */
    static boolean ringExisting(int port) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 1500);
            OutputStream o = s.getOutputStream();
            o.write("SHOW\n".getBytes(StandardCharsets.US_ASCII));
            o.flush();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Wire close-to-tray and the doorbell. Call from start() before show(). */
    static void log(String s) { System.out.println("[TRAY] " + s); }

    public static void install(Stage stage) {
        log("install: supported=" + SystemTray.isSupported());
        if (!SystemTray.isSupported()) {
            // No tray: closing must still end the process, never leave a ghost.
            stage.setOnCloseRequest(e -> exit());
            listen(stage);
            return;
        }
        Platform.setImplicitExit(false);
        stage.setOnCloseRequest(e -> {
            e.consume();
            log("close -> hidden to tray (icon " + (icon != null ? "present" : "MISSING") + ")");
            stage.hide();
            if (!hintShown && icon != null) {
                hintShown = true;
                EventQueue.invokeLater(() -> icon.displayMessage("GodHand",
                        "Still running in the tray. Right-click to show or exit.",
                        TrayIcon.MessageType.INFO));
            }
        });
        EventQueue.invokeLater(() -> addIcon(stage));
        listen(stage);
    }

    static void show(Stage stage) {
        Platform.runLater(() -> {
            if (stage.isIconified()) stage.setIconified(false);
            stage.show();
            stage.toFront();
            stage.requestFocus();
        });
    }

    static void exit() {
        log("exit requested");
        try { if (bell != null) bell.close(); } catch (Exception ignored) {}
        EventQueue.invokeLater(() -> {
            if (icon != null) SystemTray.getSystemTray().remove(icon);
        });
        Platform.runLater(() -> {
            try { Platform.exit(); } finally {
                Thread t = new Thread(() -> {
                    try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                    System.exit(0);
                }, "godhand-exit");
                t.setDaemon(true);
                t.start();
            }
        });
    }

    private static void addIcon(Stage stage) {
        try { addIconUnsafe(stage); } catch (Throwable e) {
            log("icon FAILED before add: " + e);
            icon = null;
            Platform.runLater(() -> stage.setOnCloseRequest(ev -> exit()));
        }
    }

    private static void addIconUnsafe(Stage stage) {
        PopupMenu menu = new PopupMenu();
        MenuItem showItem = new MenuItem("Show GodHand");
        showItem.addActionListener(a -> show(stage));
        MenuItem exitItem = new MenuItem("Exit GodHand");
        exitItem.addActionListener(a -> exit());
        menu.add(showItem);
        menu.addSeparator();
        menu.add(exitItem);

        icon = new TrayIcon(image(), "GodHand (Viper)", menu);
        icon.setImageAutoSize(true);
        icon.addActionListener(a -> show(stage));
        try {
            SystemTray.getSystemTray().add(icon);
            log("icon added (Windows 11 may file it under the ^ overflow arrow)");
        } catch (Throwable e) {
            log("icon FAILED: " + e);
            icon = null;
            Platform.runLater(() -> stage.setOnCloseRequest(ev -> exit()));
        }
    }

    private static void listen(Stage stage) {
        try {
            bell = new ServerSocket(PORT, 4, InetAddress.getLoopbackAddress());
        } catch (Exception e) {
            return; // port held: a copy that predates the doorbell; launcher handles it
        }
        Thread t = new Thread(() -> {
            while (!bell.isClosed()) {
                try (Socket c = bell.accept()) {
                    c.setSoTimeout(2000);
                    String line = new BufferedReader(new InputStreamReader(
                            c.getInputStream(), StandardCharsets.US_ASCII)).readLine();
                    if ("SHOW".equals(line)) show(stage);
                    else if ("EXIT".equals(line)) exit();
                } catch (Exception ignored) {
                }
            }
        }, "godhand-doorbell");
        t.setDaemon(true);
        t.start();
    }

    /** Hexagon in the GUI's purple, drawn so no resource file is needed. */
    static BufferedImage image() {
        int n = 32;
        BufferedImage img = new BufferedImage(n, n, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int[] xs = new int[6], ys = new int[6];
        for (int i = 0; i < 6; i++) {
            double a = Math.PI / 3 * i + Math.PI / 6;
            xs[i] = (int) Math.round(16 + 14 * Math.cos(a));
            ys[i] = (int) Math.round(16 + 14 * Math.sin(a));
        }
        g.setColor(new Color(0x7c, 0x3a, 0xed));
        g.fillPolygon(xs, ys, 6);
        g.setColor(new Color(0xa8, 0x55, 0xf7));
        g.setStroke(new BasicStroke(2f));
        g.drawPolygon(xs, ys, 6);
        g.dispose();
        return img;
    }
}
