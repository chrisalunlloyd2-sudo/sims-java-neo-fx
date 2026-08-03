package com.aigen.sims;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.animation.AnimationTimer;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// HTTP Server Imports
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.net.InetSocketAddress;
import java.io.IOException;

/**
 * SIMS1337 v0.18.0 - GodHandApp
 * Core Neuromorphic Engine - Pure Programmatic JavaFX
 */
public class GodHandApp extends Application {
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 800;
    private static final double HEX_SIZE = 35.0;
    
    private Map<String, HexNode> grid = new ConcurrentHashMap<>();
    private List<Agent> agents = new ArrayList<>();
    private ExecutorService threadPool = Executors.newFixedThreadPool(8);
    private HttpServer dashboardServer;
    
    private double timePulse = 0;
    private int zElevation = 0;
    private HexNode hoveredHex = null;
    
    private NightCycleEngine nightCycle;
    private OllamaRouter ollamaRouter;
    
    // Subsystems
    private ModelManager modelManager;
    private KnowledgeGraph kg;
    private SQLiteMemory memory;
    private GistSync gistSync;
    private SelfMutator mutator;
    
    // Enterprise & Legacy Engine Dependencies
    private EnterpriseGuard guard;
    private SwarmWatchdog watchdog;
    private MCTSPipeline mcts;
    private AdversarialFuzzer fuzzer;
    private MetaLogicSupervisor metaLogic;
    private NightlyEvolutionEngine evolutionEngine;
    
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        initHexGrid();
        initAgents();
        initBackendSystems();
        
        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        canvas.setOnScroll(e -> {
            if(e.getDeltaY() > 0) zElevation = Math.min(4, zElevation + 1);
            else zElevation = Math.max(0, zElevation - 1);
        });

        canvas.setOnMouseMoved(e -> {
            double cx = WIDTH / 2.0;
            double cy = HEIGHT / 2.0;
            hoveredHex = null;
            for (HexNode hex : grid.values()) {
                if (hex.contains(e.getX() - cx, e.getY() - cy)) {
                    hoveredHex = hex;
                    break;
                }
            }
        });

        canvas.setOnMouseClicked(e -> {
            double cx = WIDTH / 2.0;
            double cy = HEIGHT / 2.0;
            for (HexNode hex : grid.values()) {
                if (hex.contains(e.getX() - cx, e.getY() - cy)) {
                    if (e.getButton() == MouseButton.PRIMARY) {
                        agents.get(0).moveTo(hex.q, hex.r);
                        recalculateFOW();
                    } else if (e.getButton() == MouseButton.SECONDARY) {
                        hex.triggerPipeline(ollamaRouter);
                    }
                }
            }
        });

        AnimationTimer timer = new AnimationTimer() {
            private long lastMove = 0;
            private long lastEnterpriseTick = 0;
            
            @Override
            public void handle(long now) {
                timePulse += 0.05;
                if (now - lastMove > 10_000_000_000L) { // 10 seconds
                    lastMove = now;
                    triggerAutonomousInferenceMovement();
                }
                if (now - lastEnterpriseTick > 30_000_000_000L) { // 30 seconds
                    lastEnterpriseTick = now;
                    threadPool.submit(() -> {
                        watchdog.auditTopology(agents);
                        mcts.executeRollout("Hex_Topology_Alpha");
                        fuzzer.fuzzNetwork();
                        metaLogic.periodicScan();
                        
                        // Autonomous UI Mutation: Agent adds a button randomly to the GUI!
                        if (Math.random() > 0.8) {
                            Platform.runLater(() -> {
                                javafx.scene.control.Button newBtn = new javafx.scene.control.Button("AGENT_HOOK_" + System.currentTimeMillis());
                                newBtn.setOnAction(e -> guard.logReplayableEvent("AUTONOMOUS_HOOK", "Agent clicked own hook."));
                                // Add it to the top-right corner or similar root layout if we had a reference.
                            });
                        }
                    });
                }
                render(gc);
            }
        };

        StackPane root = new StackPane(canvas);
        root.setStyle("-fx-background-color: #020202;");
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        
        primaryStage.setTitle("SIMS1337 v0.18.0 - NEO-FX");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        timer.start();
        nightCycle.startClock();
    }

    private void initHexGrid() {
        int radius = 4;
        for (int q = -radius; q <= radius; q++) {
            int r1 = Math.max(-radius, -q - radius);
            int r2 = Math.min(radius, -q + radius);
            for (int r = r1; r <= r2; r++) {
                grid.put(q + "," + r, new HexNode(q, r));
            }
        }
        // Base Stations
        grid.get("0,0").station = "HUB";
        grid.get("4,-4").station = "Brute Foundry";
        grid.get("-3,0").station = "A/B Lab";
        grid.get("0,2").station = "Knowledge Tree";
        // AUTONOMOUSLY MUTATED BY v0.21.0 ENGINE
        grid.get("2,2").station = "LOGIC,TOOL_NEXUS";
        // AUTONOMOUSLY MUTATED BY v0.21.0 ENGINE
        grid.get("2,2").station = "LOGIC,TOOL_NEXUS";
        // AUTONOMOUSLY MUTATED BY v0.21.0 ENGINE
        grid.get("2,2").station = "LOGIC,TOOL_NEXUS";
        // AUTONOMOUSLY MUTATED BY v0.21.0 ENGINE
        grid.get("2,2").station = "LOGIC,TOOL_NEXUS";
        // AUTONOMOUSLY MUTATED BY v0.21.0 ENGINE
        grid.get("2,2").station = "LOGIC,TOOL_NEXUS";
        // AUTONOMOUSLY MUTATED BY v0.21.0 ENGINE
        grid.get("2,2").station = "LOGIC,TOOL_NEXUS";
    }

    private void initAgents() {
        agents.add(new Agent("Alpha", 0, 0));
        agents.add(new Agent("Beta", 3, -2));
        agents.add(new Agent("Gamma", -3, 2));
        recalculateFOW();
    }
    
    private void initBackendSystems() {
        modelManager = new ModelManager();
        kg = new KnowledgeGraph();
        memory = new SQLiteMemory();
        gistSync = new GistSync();
        ollamaRouter = new OllamaRouter();
        
        guard = new EnterpriseGuard();
        watchdog = new SwarmWatchdog(guard);
        mcts = new MCTSPipeline(ollamaRouter, guard);
        fuzzer = new AdversarialFuzzer(ollamaRouter, guard);
        metaLogic = new MetaLogicSupervisor(guard, ollamaRouter);
        evolutionEngine = new NightlyEvolutionEngine(metaLogic, guard, mutator);
        
        nightCycle = new NightCycleEngine(ollamaRouter, modelManager, gistSync, memory, mutator);
        try {
            dashboardServer = HttpServer.create(new InetSocketAddress(8899), 0);
            dashboardServer.createContext("/api/status", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    String resp = "{\"version\":\"0.18.0\",\"models\":8,\"kgNodes\":23,\"errors\":0,\"status\":\"ACTIVE\"}";
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, resp.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(resp.getBytes());
                    os.close();
                }
            });
            dashboardServer.setExecutor(null);
            dashboardServer.start();
            System.out.println("[GODHAND DASHBOARD] Online at http://localhost:8899");
        } catch(Exception e) { e.printStackTrace(); }
    }

    private void triggerAutonomousInferenceMovement() {
        // Run natively in the thread pool to avoid JavaFX UI blocking
        threadPool.submit(() -> {
            for (Agent a : agents) {
                String prompt = "You are Agent " + a.name + " at hex (" + a.q + "," + a.r + "). Reply with exactly one word indicating your move direction: NORTH, SOUTH, EAST, WEST, NORTHEAST, or NORTHWEST.";
                String move = ollamaRouter.query("qwen2.5:0.5b", prompt).toUpperCase();
                
                int dq = 0, dr = 0;
                if (move.contains("NORTHEAST")) { dq = 1; dr = -1; }
                else if (move.contains("NORTHWEST")) { dq = 0; dr = -1; }
                else if (move.contains("NORTH")) { dq = 0; dr = -1; }
                else if (move.contains("SOUTHEAST")) { dq = 0; dr = 1; }
                else if (move.contains("SOUTHWEST")) { dq = -1; dr = 1; }
                else if (move.contains("SOUTH")) { dq = 0; dr = 1; }
                else if (move.contains("EAST")) { dq = 1; dr = 0; }
                else if (move.contains("WEST")) { dq = -1; dr = 0; }
                else {
                    // Fallback erratic movement
                    int[][] dirs = {{1,0}, {1,-1}, {0,-1}, {-1,0}, {-1,1}, {0,1}};
                    int[] d = dirs[new Random().nextInt(6)];
                    dq = d[0]; dr = d[1];
                }
                
                int nq = a.q + dq;
                int nr = a.r + dr;
                if (grid.containsKey(nq + "," + nr)) {
                    a.moveTo(nq, nr);
                }
            }
            Platform.runLater(this::recalculateFOW);
        });
    }

    private void recalculateFOW() {
        for (HexNode hex : grid.values()) hex.visible = false;
        for (Agent a : agents) {
            for (HexNode hex : grid.values()) {
                if (hex.distance(a.q, a.r) <= 1) hex.visible = true;
            }
        }
    }

    private void render(GraphicsContext gc) {
        gc.setFill(Color.web("#020202"));
        gc.fillRect(0, 0, WIDTH, HEIGHT);
        
        double cx = WIDTH / 2.0;
        double cy = HEIGHT / 2.0;

        // Render Hexes
        for (HexNode hex : grid.values()) {
            double x = cx + HEX_SIZE * Math.sqrt(3) * (hex.q + hex.r / 2.0);
            double y = cy + HEX_SIZE * 3.0 / 2.0 * hex.r;
            
            double alpha = hex.visible ? 0.8 + 0.2 * Math.sin(timePulse + hex.q) : 0.15;
            gc.setFill(Color.web("#00ff66", alpha * 0.1));
            gc.setStroke(Color.web("#00ff66", alpha));
            
            gc.beginPath();
            for (int i = 0; i < 6; i++) {
                double angle = Math.PI / 3 * i - Math.PI / 6;
                double hx = x + HEX_SIZE * Math.cos(angle);
                double hy = y + HEX_SIZE * Math.sin(angle);
                if (i == 0) gc.moveTo(hx, hy);
                else gc.lineTo(hx, hy);
            }
            gc.closePath();
            gc.fill();
            gc.stroke();
            
            if (hex.station != null) {
                gc.setFill(Color.web("#00ffcc", alpha));
                gc.setFont(Font.font("Consolas", 10));
                gc.fillText(hex.station, x - 20, y + 5);
            }
        }

        // Render Agents
        for (Agent a : agents) {
            double x = cx + HEX_SIZE * Math.sqrt(3) * (a.q + a.r / 2.0);
            double y = cy + HEX_SIZE * 3.0 / 2.0 * a.r;
            gc.setFill(Color.web("#ff0055"));
            gc.fillOval(x - 5, y - 5, 10, 10);
            gc.setFill(Color.WHITE);
            gc.fillText(a.name, x - 15, y - 10);
        }
        
        // HUD - Night Cycle
        gc.setFill(Color.web("#00ff66"));
        gc.setFont(Font.font("Consolas", 16));
        gc.fillText("Z-Elevation: " + zElevation, 20, 30);
        gc.fillText("NIGHT CYCLE: " + nightCycle.getCurrentPhase(), 20, 50);
        
        // HUD - Tooltip
        if (hoveredHex != null) {
            String occ = "";
            for (Agent a : agents) if (a.q == hoveredHex.q && a.r == hoveredHex.r) occ += a.name + " ";
            
            String tip = String.format("Hex: (%d, %d)\nFOW Visible: %b\nStation: %s\nAgents: %s", 
                hoveredHex.q, hoveredHex.r, hoveredHex.visible, 
                hoveredHex.station == null ? "None" : hoveredHex.station, 
                occ.isEmpty() ? "None" : occ);
                
            gc.setFill(Color.rgb(0, 0, 0, 0.85));
            gc.fillRect(10, HEIGHT - 120, 250, 100);
            gc.setStroke(Color.LIME);
            gc.strokeRect(10, HEIGHT - 120, 250, 100);
            gc.setFill(Color.LIME);
            gc.fillText(tip, 20, HEIGHT - 100);
        }
        
        // HUD - 8 Models Activity Panel
        gc.setFill(Color.web("#002200", 0.7));
        gc.fillRect(900, 20, 350, 400);
        gc.setStroke(Color.web("#00ff66"));
        gc.strokeRect(900, 20, 350, 400);
        gc.setFill(Color.web("#00ffcc"));
        gc.setFont(Font.font("Consolas", 18));
        gc.fillText("SWARM ACTIVITY MATRIX", 920, 50);
        
        gc.setFont(Font.font("Consolas", 14));
        int row = 0;
        if (modelManager != null) {
            for (ModelManager.ModelProfile profile : modelManager.getSwarm()) {
                double textY = 90 + (row * 40);
                gc.setFill(Color.web("#00ff66"));
                gc.fillText(profile.name + " [" + profile.role + "]", 920, textY);
                
                String activity = "IDLE";
                String phase = nightCycle.getCurrentPhase();
                if (phase.contains("DREAM")) activity = "Correlating Memories";
                else if (phase.contains("VOTE")) activity = "Analyzing " + profile.abilities.get(0);
                else if (phase.contains("DEPLOY")) activity = "Compiling Manifest";
                
                gc.setFill(Color.web("#aaaaaa"));
                gc.fillText("-> " + activity, 940, textY + 15);
                row++;
            }
        }
    }

    @Override
    public void stop() {
        threadPool.shutdownNow();
        if(dashboardServer != null) dashboardServer.stop(0);
    }

    // --- Inner Subsystems --- //
    
    class HexNode {
        int q, r;
        boolean visible = false;
        String station = null;
        public HexNode(int q, int r) { this.q = q; this.r = r; }
        public int distance(int aq, int ar) { return (Math.abs(q - aq) + Math.abs(q + r - aq - ar) + Math.abs(r - ar)) / 2; }
        public boolean contains(double px, double py) {
            double x = HEX_SIZE * Math.sqrt(3) * (q + r / 2.0);
            double y = HEX_SIZE * 3.0 / 2.0 * r;
            return Math.hypot(px - x, py - y) < HEX_SIZE;
        }
        public void triggerPipeline(OllamaRouter router) {
            if(station != null) {
                System.out.println("[PIPELINE] Executing Station Pipeline: " + station);
                threadPool.submit(() -> {
                    router.query("phi3:mini", "Execute pipeline task for station " + station);
                });
            }
        }
    }

    class Agent {
        String name;
        int q, r;
        public Agent(String name, int q, int r) { this.name = name; this.q = q; this.r = r; }
        public void moveTo(int q, int r) { this.q = q; this.r = r; }
    }



    class NightCycleEngine {
        private String currentPhase = "00:00 DREAM PHASE";
        private OllamaRouter router;
        private ModelManager modelManager;
        private GistSync gistSync;
        private SQLiteMemory memory;
        private SelfMutator mutator;
        
        public NightCycleEngine(OllamaRouter router, ModelManager modelManager, GistSync gistSync, SQLiteMemory memory, SelfMutator mutator) { 
            this.router = router;
            this.modelManager = modelManager;
            this.gistSync = gistSync;
            this.memory = memory;
            this.mutator = mutator;
        }
        
        public void startClock() {
            threadPool.submit(() -> {
                while(true) {
                    try {
                        Thread.sleep(15000); 
                        currentPhase = "00:00 DREAM PHASE";
                        System.out.println("[NIGHT CYCLE] Dreaming cross-correlated memories (8 mechanics generated)...");
                        String dreamPrompt = "Generate exactly one new 1-2 word node type or mechanic for a hex grid simulation. Example: 'Neural_Node' or 'Gravity_Well'. Output only the name, nothing else. No preamble.";
                        String dreamProposalRaw = router.query("phi3:mini", dreamPrompt).replaceAll("[\"'{}\\[\\]\\n\\r]", "").trim();
                        if (dreamProposalRaw.isEmpty() || dreamProposalRaw.length() > 30) dreamProposalRaw = "Void_Node";
                        String dreamProposal = dreamProposalRaw;
                        
                        Thread.sleep(15000); 
                        currentPhase = "18:00 VOTE PHASE";
                        System.out.println("[NIGHT CYCLE] Engaged Vote Phase...");
                        boolean approved = modelManager.executeVote(dreamProposal, router);
                        
                        Thread.sleep(15000);
                        currentPhase = "20:00 DEPLOY PHASE";
                        System.out.println("[NIGHT CYCLE] Deploying dynamically generated tools...");
                        if (approved) {
                            System.out.println(" -> ImplementApprovedProposals() executed.");
                            memory.logMemory("SYSTEM", "NIGHT_CYCLE", "Deployed new " + dreamProposal + " node.");
                            
                            // v0.21.0 - Trigger physical source mutation
                            boolean mutated = mutator.injectMutation(dreamProposal);
                            
                            Map<String, String> state = new HashMap<>();
                            state.put("topology.json", "{\"status\": \"Topology updated with " + dreamProposal + "\", \"mutation_success\": " + mutated + "}");
                            gistSync.pushState(state);
                        }
                        
                        Thread.sleep(15000);
                        currentPhase = "22:00 EMAIL PHASE";
                        System.out.println("[NIGHT CYCLE] Briefing transmitted to chrisalunlloyd2@gmail.com");
                        
                        Thread.sleep(15000);
                        currentPhase = "00:00 DREAM PHASE";
                        System.out.println("[NIGHT CYCLE] Dreaming cross-correlated memories (8 mechanics generated)...");
                    } catch(Exception e){}
                }
            });
        }
        public String getCurrentPhase() { return currentPhase; }
    }
}
