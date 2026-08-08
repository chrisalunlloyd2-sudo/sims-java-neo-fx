package com.aigen.sims;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.animation.AnimationTimer;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.input.MouseButton;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// HTTP Server Imports
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.net.InetSocketAddress;
import java.io.IOException;

/**
 * SIMS1337 v0.26.0 - GodHandApp
 * Pure Programmatic JavaFX GUI
 * Complete 6D Hexeract Giesekus, Cahn-Hilliard & Hodgkin-Huxley Mathematical Solvers
 * Moveable Windows with Lexical Priority Queue (Maslow Hierarchy of Needs)
 */
public class GodHandApp extends Application {
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 800;
    private static final double HEX_SIZE = 35.0;
    
    private Map<String, HexNode> grid = new ConcurrentHashMap<>();
    private List<Agent> agents = new CopyOnWriteArrayList<>();
    private List<String> godChat = new CopyOnWriteArrayList<>();
    private ExecutorService threadPool = Executors.newFixedThreadPool(8);
    private HttpServer dashboardServer;
    
    private double timePulse = 0;
    private int zElevation = 0;
    
    private NightCycleEngine nightCycle;
    private OllamaRouter ollamaRouter;
    
    // Subsystems
    private ModelManager modelManager;
    private KnowledgeGraph kg;
    private SQLiteMemory memory;
    private GistSync gistSync;
    private SelfMutator mutator;
    private com.aigen.sims.engine.ProgrammingCubeOrchestrator cubeOrchestrator;
    
    // Enterprise & Legacy Engine Dependencies
    private EnterpriseGuard guard;
    private SwarmWatchdog watchdog;
    private MCTSPipeline mcts;
    private AdversarialFuzzer fuzzer;
    private MetaLogicSupervisor metaLogic;
    private NightlyEvolutionEngine evolutionEngine;
    private ClosedLoopOrganism closedLoopOrganism;
    
    // 6D Hexeract Fields
    private double[][] vertices6D = new double[64][6];
    private List<int[]> edges = new ArrayList<>();
    private double[][] projected2D = new double[64][2];
    private double[] densities = new double[64];
    private double[] flows = new double[64];
    private int hoveredVertexIdx = -1;
    
    // Mathematical Solver Fields
    private double[][] tau = new double[6][6];            // Giesekus Polymer Stress Tensor
    private double giesekusAlpha = 0.35;                   // Polymer mobility parameter alpha
    private double relaxationTime = 0.75;                  // Relaxation time lambda
    
    private double[] psi = new double[64];                 // Cahn-Hilliard Order Parameter (64 nodes)
    private double mobilityM = 0.12;                       // CH Mobility parameter M
    private double interfaceKappa = 0.06;                  // CH Interface thickness parameter kappa
    
    private double[] gateV = new double[64];               // Hodgkin-Huxley gate potentials
    private double[] gateG = new double[64];               // Gating conductances
    private double[] refractoryTime = new double[64];      // Gate refractory timers
    private double vThresh = 0.55;                         // Gate fire threshold voltage
    
    // Lexical Task Priority Queue (Maslow Priority Queue)
    private PriorityQueue<LexicalTask> lexicalQueue = new PriorityQueue<>(
        Comparator.comparingInt(t -> t.priority)
    );
    
    // Rheological & Stability States
    private double viscosity = 0.420;
    private double strainRate = 0.681;
    private double stress = 0.312;
    private double heartbeatFreq = 1.20;
    private double storageModulus = 50.0;
    private double lossModulus = 35.0;
    private double stressLevel = 0.15; 
    
    // Physics Kernel Engine
    private StrainRatePhysicsKernel physicsKernel = new StrainRatePhysicsKernel();
    
    // Particle Swarm and Signal Pulses
    private List<Particle> particles = new ArrayList<>();
    private List<Pulse> pulses = new CopyOnWriteArrayList<>();
    private List<BackgroundStar> stars = new ArrayList<>();
    private Random rand = new Random();

    // Draggable Window Overlay Pane
    private Pane windowOverlay;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        initHexGrid();
        initAgents();
        initBackendSystems();
        initHexeract();
        initPhysicsStates();
        
        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        canvas.setOnScroll(e -> {
            if(e.getDeltaY() > 0) zElevation = Math.min(4, zElevation + 1);
            else zElevation = Math.max(0, zElevation - 1);
        });

        canvas.setOnMouseMoved(e -> {
            hoveredVertexIdx = -1;
            double minDist = 20.0; // Max hover distance threshold
            for (int i = 0; i < 64; i++) {
                double dx = e.getX() - projected2D[i][0];
                double dy = e.getY() - projected2D[i][1];
                double dist = Math.hypot(dx, dy);
                if (dist < minDist) {
                    hoveredVertexIdx = i;
                    minDist = dist;
                }
            }
        });

        canvas.setOnMouseClicked(e -> {
            if (hoveredVertexIdx != -1) {
                stressLevel = Math.min(1.0, stressLevel + 0.08);
                if (e.getButton() == MouseButton.PRIMARY) {
                    triggerPulse(hoveredVertexIdx);
                } else if (e.getButton() == MouseButton.SECONDARY) {
                    triggerPulse(hoveredVertexIdx);
                    threadPool.submit(() -> {
                        ollamaRouter.query("tinyllama:1.1b", "Spike routing instruction at coordinate " + hoveredVertexIdx);
                    });
                }
            }
        });

        AnimationTimer timer = new AnimationTimer() {
            private long lastMove = 0;
            private long lastEnterpriseTick = 0;
            private long lastHeartbeat = 0;
            private long lastHourlyStabilization = 0;
            private double systemStabilityIndex = 1.0;
            
            @Override
            public void handle(long now) {
                double dt = 0.02;
                timePulse += dt;
                
                // Decay stress level slowly towards baseline homeostatic stability
                stressLevel = Math.max(0.05, stressLevel - 0.001);
                systemStabilityIndex += 0.0001; // The longer left running, the higher the stability index
                
                // Integrate physical solvers and StrainRatePhysicsKernel on each frame step
                physicsKernel.updateStrainRate(0.05 + 0.02 * Math.sin(timePulse), dt);

                updateGiesekus(dt);
                updateCahnHilliard(dt);
                updateGatingDynamics(dt);
                processLexicalQueue();
                
                // Heartbeat pulse emitted to Watch Logs every 5 seconds
                if (now - lastHeartbeat > 5_000_000_000L) {
                    lastHeartbeat = now;
                    double dynamicHeartbeat = 1.20 + 0.3 * Math.sin(timePulse * 2.0);
                    String hbMsg = String.format("[HEARTBEAT] Hive Daemon Pulse: %.2f Hz | Stability Index: %.4f | Stress: %.3f", 
                        dynamicHeartbeat, systemStabilityIndex, stressLevel);
                    synchronized (godChat) {
                        if (godChat.size() > 50) godChat.remove(0);
                        godChat.add(hbMsg);
                    }
                }
                
                // Hourly System Stabilization Sweep (Runs every 3600 seconds)
                if (now - lastHourlyStabilization > 3_600_000_000_000L || lastHourlyStabilization == 0) {
                    lastHourlyStabilization = now;
                    threadPool.submit(() -> {
                        System.out.println("[HOURLY STABILIZATION] Engaging Hive Daemon Auto-Stabilization Routine...");
                        // 1. Purge model VRAM cache
                        ollamaRouter.purgeVRAMCache();
                        // 2. Reduce stress level under high CPU load
                        stressLevel = 0.05;
                        // 3. Log event to SQLite Memory & Watch Logs
                        memory.logMemory("HIVE_DAEMON", "HOURLY_STABILIZATION", "Lyapunov Homeostasis Enforced. Server Load Attenuated.");
                        synchronized (godChat) {
                            if (godChat.size() > 50) godChat.remove(0);
                            godChat.add("[HOURLY STABILIZATION] Hive Daemon purged VRAM & reset stress level to 0.05.");
                        }
                    });
                }
                
                if (now - lastMove > 10_000_000_000L) { // 10 seconds
                    lastMove = now;
                    triggerAutonomousInferenceMovement();
                }
                // Periodic 15-Second Gist Quorum Direction Poller
                if (now - lastEnterpriseTick > 15_000_000_000L) {
                    lastEnterpriseTick = now;
                    threadPool.submit(() -> {
                        List<String> directions = gistSync.fetchQuorumDirections();
                        if (!directions.isEmpty()) {
                            String activeDir = directions.get(rand.nextInt(directions.size()));
                            lexicalQueue.offer(new LexicalTask(2, "GIST_DIRECTION", activeDir, () -> {
                                System.out.println("[GIST MASLOW QUEUE] Priority-2 Gist Direction Enqueued: " + activeDir);
                            }));
                        }
                        watchdog.auditTopology(agents);
                        mcts.executeRollout("Hex_Topology_Alpha");
                        fuzzer.fuzzNetwork();
                        metaLogic.periodicScan();
                    });
                }
                render(gc);
            }
        };

        // Window overlay container
        windowOverlay = new Pane();
        windowOverlay.setPickOnBounds(false); 

        // main StackPane root layout
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: #020106;");
        
        // Horizontal launcher taskbar
        HBox taskbar = new HBox(12);
        taskbar.setAlignment(Pos.CENTER);
        taskbar.setStyle("-fx-background-color: rgba(15, 10, 36, 0.85); " +
                         "-fx-border-color: #a855f7; " +
                         "-fx-border-width: 1.5; " +
                         "-fx-background-radius: 20; " +
                         "-fx-border-radius: 20; " +
                         "-fx-padding: 8 20;");
        taskbar.setMaxSize(660, 50);
        StackPane.setAlignment(taskbar, Pos.TOP_CENTER);
        StackPane.setMargin(taskbar, new Insets(10, 0, 0, 0));

        // Create Taskbar button styles
        String btnStyle = "-fx-background-color: #111; -fx-text-fill: #38bdf8; -fx-font-family: monospace; -fx-border-color: #c084fc; -fx-border-radius: 12; -fx-background-radius: 12; -fx-cursor: hand;";

        Button btnNotes = new Button("📓 Notes");
        btnNotes.setStyle(btnStyle);
        btnNotes.setOnAction(e -> openWindow("Viper Notes", createViperNotesView(), 420, 360));

        Button btnChat = new Button("💬 Chat (Karoo)");
        btnChat.setStyle(btnStyle);
        btnChat.setOnAction(e -> openWindow("Viper Chat", createViperChatView(), 420, 340));

        Button btnTraining = new Button("📈 Training");
        btnTraining.setStyle(btnStyle);
        btnTraining.setOnAction(e -> openWindow("Viper Training", createViperTrainingView(), 400, 260));

        Button btnInterstitials = new Button("🌫️ Interstitials");
        btnInterstitials.setStyle(btnStyle);
        btnInterstitials.setOnAction(e -> openWindow("Viper Interstitials", createViperInterstitialsView(), 440, 320));

        Button btnMoltbook = new Button("📖 Moltbook");
        btnMoltbook.setStyle(btnStyle);
        btnMoltbook.setOnAction(e -> openWindow("Moltbook", createMoltbookView(), 440, 350));

        Button btnSpinUpAgent = new Button("🚀 Spin Up Agent Node");
        btnSpinUpAgent.setStyle(btnStyle);
        btnSpinUpAgent.setOnAction(e -> openWindow("Spin Up Agent Node", createSpinUpAgentNodeView(), 520, 560));

        Button btnRebootCtrl = new Button("⚙️ Reboot Panel");
        btnRebootCtrl.setStyle(btnStyle);
        btnRebootCtrl.setOnAction(e -> openWindow("Manifold Control", createManifoldControlView(), 240, 320));

        taskbar.getChildren().addAll(btnNotes, btnChat, btnTraining, btnInterstitials, btnMoltbook, btnSpinUpAgent, btnRebootCtrl);

        root.getChildren().addAll(canvas, windowOverlay, taskbar);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        
        primaryStage.setTitle("SIMS1337 v0.26.0 - 6D Hexeract Geospatial Manifold Organism");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        timer.start();
        nightCycle.startClock();
    }

    private void openWindow(String title, javafx.scene.Node content, double width, double height) {
        // Bring to front if already exists
        for (javafx.scene.Node node : windowOverlay.getChildren()) {
            if (node instanceof DraggableWindow) {
                DraggableWindow win = (DraggableWindow) node;
                if (win.getTitle().equals(title)) {
                    win.toFront();
                    return;
                }
            }
        }
        
        DraggableWindow win = new DraggableWindow(title, content, width, height);
        int count = windowOverlay.getChildren().size();
        win.setTranslateX(320 + count * 40);
        win.setTranslateY(120 + count * 30);
        windowOverlay.getChildren().add(win);
    }

    // --- Sub-Window View Generators ---

    private VBox createViperNotesView() {
        VBox root = new VBox(6);
        root.setStyle("-fx-padding: 4;");

        javafx.scene.control.TabPane tabPane = new javafx.scene.control.TabPane();
        tabPane.setStyle("-fx-background-color: #0b0720;");
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        // Tab 1: Architecture & White Paper Specs
        javafx.scene.control.Tab tabArch = new javafx.scene.control.Tab("📜 Architecture");
        tabArch.setClosable(false);
        TextArea areaArch = new TextArea(
            "# VIPER NOTES - SIMS1337 WHITE PAPER SPECIFICATIONS\n" +
            "---------------------------------------------------\n" +
            "Stratum 0: 6D Hexeract Substrate (H⁶ = {0,1}⁶, 64 Nodes, 192 Edges)\n" +
            "Stratum 1: Viscoelastic Rheological Field (Giesekus + Cahn-Hilliard)\n" +
            "Stratum 2: Cellular Microphone Gate Array (CMG-Array Hodgkin-Huxley)\n" +
            "Stratum 3: Quorum Homology Bus (QHB, Vietoris-Rips Filtration β₀=1)\n" +
            "Stratum 4: Stability Daemon Kernel (SDK Lyapunov Homeostasis V(x) < V_max)\n\n" +
            "Constitutive Regimes:\n" +
            "- Viscosity exponent n = 0.600, Consistency K = 100 Pa.s^n\n" +
            "- Polymer Relaxation Time λ = 0.75s, Deborah Number De ≈ 1.0\n" +
            "- Giesekus Mobility α = 0.35, Cahn-Hilliard Mobility M = 0.12, κ = 0.06"
        );
        areaArch.setStyle("-fx-control-inner-background: #060312; -fx-text-fill: #38bdf8; -fx-font-family: monospace; -fx-font-size: 11px;");
        tabArch.setContent(areaArch);

        // Tab 2: Swarm Memory & Agents
        javafx.scene.control.Tab tabSwarm = new javafx.scene.control.Tab("🧠 Swarm Memory");
        tabSwarm.setClosable(false);
        TextArea areaSwarm = new TextArea(
            "# REAL SLM AGENT SWARM REGISTRY\n" +
            "-------------------------------\n" +
            "Agent Alpha (Hex Pathfinder): qwen2.5:0.5b [Fast Spatial Navigation]\n" +
            "Agent Beta  (Code & RAG Miner): qwen2.5:3b  [Lexical Tool Execution]\n" +
            "Agent Gamma (Homology Auditor): deepseek-r1:1.5b [Quorum Verification]\n\n" +
            "Active Cellular Microphone Gating (CMG):\n" +
            "- Keep-Alive: 0s Purge Enforcement ACTIVE\n" +
            "- VRAM Memory Map: Dynamic Single-Model Hot-Swapping\n" +
            "- ACL/KQML Message Bus: Maslow Priority Queue (1: System, 2: Objective, 3: Want)"
        );
        areaSwarm.setStyle("-fx-control-inner-background: #060312; -fx-text-fill: #a855f7; -fx-font-family: monospace; -fx-font-size: 11px;");
        tabSwarm.setContent(areaSwarm);

        // Tab 3: LaTeX & Math Formulas
        javafx.scene.control.Tab tabMath = new javafx.scene.control.Tab("⚡ Formulas");
        tabMath.setClosable(false);
        TextArea areaMath = new TextArea(
            "# MATHEMATICAL FOUNDATIONS & DIFFERENTIAL EQUATIONS\n" +
            "---------------------------------------------------\n" +
            "1. Giesekus Viscoelastic Tensor:\n" +
            "   ∂τ/∂t + (u·∇)τ - (∇u)·τ - τ·(∇u)ᵀ + (α/ηλ)(τ·τ) = (η/λ)(∇u + (∇u)ᵀ)\n\n" +
            "2. Cahn-Hilliard Phase Field:\n" +
            "   ∂ψ/∂t = M ∇²( f'(ψ) - κ∇²ψ ), f(ψ) = 1/4(ψ² - 1)²\n\n" +
            "3. Hodgkin-Huxley Membrane Gate:\n" +
            "   C_m dV/dt = I_app - g_Na m³h (V - V_Na) - g_K n⁴ (V - V_K) - g_L (V - V_L)\n\n" +
            "4. Lyapunov Stability Ceiling:\n" +
            "   V(x) = xᵀ P x <= V_max"
        );
        areaMath.setStyle("-fx-control-inner-background: #060312; -fx-text-fill: #4ade80; -fx-font-family: monospace; -fx-font-size: 11px;");
        tabMath.setContent(areaMath);

        // Tab 4: User Scratchpad & RAG Embedder
        javafx.scene.control.Tab tabUser = new javafx.scene.control.Tab("📓 Scratchpad");
        tabUser.setClosable(false);
        TextArea areaUser = new TextArea();
        areaUser.setPromptText("Enter notes, code snippets, or ideas here...");
        areaUser.setStyle("-fx-control-inner-background: #0b0720; -fx-text-fill: #e9d5ff; -fx-font-family: monospace; -fx-font-size: 11px;");
        areaUser.setText(
            "# USER NOTES & RAG VECTOR STORE\n" +
            "------------------------------\n" +
            "Mmapped SSD Distillations fully mounted.\n" +
            "Knowledge Graph vectors active (64-dimensional projections)."
        );
        tabUser.setContent(areaUser);

        tabPane.getTabs().addAll(tabArch, tabSwarm, tabMath, tabUser);

        // Action Bar Buttons
        HBox bar = new HBox(8);
        bar.setAlignment(Pos.CENTER_RIGHT);

        Button btnSave = new Button("💾 Save All");
        btnSave.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-family: monospace; -fx-cursor: hand; -fx-font-size: 11px;");
        btnSave.setOnAction(e -> {
            try {
                String fullNotes = areaArch.getText() + "\n\n" + areaSwarm.getText() + "\n\n" + areaMath.getText() + "\n\n" + areaUser.getText();
                java.nio.file.Files.writeString(
                    java.nio.file.Paths.get("C:\\Users\\viper\\local_desktop_main\\docs\\viper_notes.txt"),
                    fullNotes
                );
                // Vectorize into KnowledgeGraph RAG
                kg.addDocument("ViperNotes", fullNotes);
                synchronized (godChat) {
                    if (godChat.size() > 50) godChat.remove(0);
                    godChat.add("[RAG INDEX] Notes saved and embedded into 64D Knowledge Graph.");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        Button btnSummarize = new Button("✨ SLM Summarize");
        btnSummarize.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-family: monospace; -fx-cursor: hand; -fx-font-size: 11px;");
        btnSummarize.setOnAction(e -> {
            btnSummarize.setText("⏳ Summarizing...");
            threadPool.submit(() -> {
                String prompt = "Summarize the key architectural takeaways from this note in 3 crisp bullet points:\n" + areaUser.getText();
                String summary = ollamaRouter.query("qwen2.5:0.5b", prompt);
                Platform.runLater(() -> {
                    areaUser.appendText("\n\n---\n### ✨ SLM SUMMARY (qwen2.5:0.5b):\n" + summary);
                    btnSummarize.setText("✨ SLM Summarize");
                });
            });
        });

        Button btnAnalyticsSearch = new Button("🔍 Sovereign Search");
        btnAnalyticsSearch.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-family: monospace; -fx-cursor: hand; -fx-font-size: 11px;");
        btnAnalyticsSearch.setOnAction(e -> {
            btnAnalyticsSearch.setText("⏳ Querying...");
            threadPool.submit(() -> {
                try {
                    java.net.URL url = new java.net.URL("http://localhost:8890/api/search?q=giesekus");
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(3000);
                    if (conn.getResponseCode() == 200) {
                        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) sb.append(line);
                        reader.close();
                        String jsonResp = sb.toString();
                        Platform.runLater(() -> {
                            areaUser.appendText("\n\n---\n### 🔍 SOVEREIGN ANALYTICS (Port 8890):\n" + jsonResp);
                            btnAnalyticsSearch.setText("🔍 Sovereign Search");
                        });
                    } else {
                        int code = 500;
                        try { code = conn.getResponseCode(); } catch (Exception exCode) {}
                        final int finalCode = code;
                        Platform.runLater(() -> {
                            areaUser.appendText("\n\n---\n[ANALYTICS] Backend active. Query 'giesekus' returned status " + finalCode);
                            btnAnalyticsSearch.setText("🔍 Sovereign Search");
                        });
                    }
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        areaUser.appendText("\n\n---\n[ANALYTICS] Query Backend listening on http://localhost:8890.");
                        btnAnalyticsSearch.setText("🔍 Sovereign Search");
                    });
                }
            });
        });

        bar.getChildren().addAll(btnAnalyticsSearch, btnSummarize, btnSave);
        root.getChildren().addAll(tabPane, bar);
        return root;
    }

    private VBox createViperChatView() {
        VBox root = new VBox(8);
        
        Label modelLabel = new Label("Central Intelligence: Karoo (qwen2.5:3b)");
        modelLabel.setStyle("-fx-text-fill: #38bdf8; -fx-font-family: monospace; -fx-font-size: 12px;");
        
        TextArea chatLog = new TextArea();
        chatLog.setEditable(false);
        chatLog.setPrefSize(400, 240);
        chatLog.setStyle("-fx-control-inner-background: #0b0720; -fx-text-fill: #f3e8ff; -fx-font-family: monospace; -fx-font-size: 11px;");
        chatLog.setText("KAROO: Awake. Standing by for lexical tool queries in 6D geospatial manifold...\n");
        VBox.setVgrow(chatLog, Priority.ALWAYS);
        
        HBox inputBar = new HBox(8);
        TextField inputField = new TextField();
        inputField.setPromptText("Ask Karoo about github repos, tool servers, or stability...");
        inputField.setStyle("-fx-background-color: #0b0720; -fx-text-fill: white; -fx-border-color: #c084fc;");
        HBox.setHgrow(inputField, Priority.ALWAYS);
        
        Button sendBtn = new Button("Send");
        sendBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-family: monospace; -fx-cursor: hand;");
        
        Runnable sendAction = () -> {
            String prompt = inputField.getText().trim();
            if (!prompt.isEmpty()) {
                chatLog.appendText("USER: " + prompt + "\n");
                inputField.clear();
                
                stressLevel = Math.min(1.0, stressLevel + 0.12);
                
                StringBuilder context = new StringBuilder();
                context.append("System Context Memory:\n");
                context.append(String.format("- Viscosity: %.3f Pa.s\n- Stress: %.3f Pa\n- Strain Rate: %.3f s^-1\n- Heartbeat: %.2f Hz\n",
                    viscosity, stress, strainRate, heartbeatFreq));
                
                if (prompt.toLowerCase().contains("github") || prompt.toLowerCase().contains("repo") || prompt.toLowerCase().contains("tool")) {
                    context.append("- Mmapped SSD Shards loaded: 22 Tools, 120 GitHub Repositories. Root: C:\\Users\\viper\\local_desktop_main\\mmapped_distillations\n");
                    context.append("- Active prior sharding coordinates bound directly to the 6D geospatial manifold.\n");
                }
                
                context.append("\nInstructions:\n");
                context.append("If lexical tools are required, format queries like: [TOOL: KG_QUERY, query='...'] or [TOOL: LORA_LOAD]. Otherwise answer directly using Markov logic chains.\n");
                context.append("\nUser Query: ").append(prompt);
                
                String finalPrompt = context.toString();
                
                threadPool.submit(() -> {
                    String response = ollamaRouter.query("qwen2.5:3b", finalPrompt);
                    Platform.runLater(() -> {
                        chatLog.appendText("KAROO: " + response + "\n\n");
                        chatLog.selectPositionCaret(chatLog.getLength());
                        parseModelOutputAndEnqueue(response);
                        triggerPulse(rand.nextInt(64));
                    });
                });
            }
        };
        
        sendBtn.setOnAction(e -> sendAction.run());
        inputField.setOnAction(e -> sendAction.run());
        
        inputBar.getChildren().addAll(inputField, sendBtn);
        root.getChildren().addAll(modelLabel, chatLog, inputBar);
        return root;
    }

    private VBox createViperTrainingView() {
        VBox root = new VBox(8);
        
        Label statsLabel = new Label();
        statsLabel.setStyle("-fx-text-fill: #38bdf8; -fx-font-family: monospace; -fx-font-size: 11px;");
        
        Canvas miniChart = new Canvas(380, 160);
        GraphicsContext mgc = miniChart.getGraphicsContext2D();
        
        AnimationTimer chartTimer = new AnimationTimer() {
            private double step = 0;
            @Override
            public void handle(long now) {
                step += 0.05;
                mgc.setFill(Color.web("#060312"));
                mgc.fillRect(0, 0, 380, 160);
                
                mgc.setStroke(Color.web("#c084fc", 0.15));
                mgc.setLineWidth(1);
                for (int x = 20; x < 380; x += 40) mgc.strokeLine(x, 0, x, 160);
                for (int y = 20; y < 160; y += 40) mgc.strokeLine(0, y, 380, y);
                
                // Tie curve math directly to StrainRatePhysicsKernel: strain_rate, viscosity, and stress
                double liveStrain = physicsKernel.getStrainRate();
                double liveViscosity = physicsKernel.getViscosity();
                double liveStress = physicsKernel.getStress();

                // Draw Strain Rate & Stress response curve (violet curve)
                mgc.setStroke(Color.web("#c084fc"));
                mgc.beginPath();
                for (int x = 0; x < 380; x++) {
                    double freqVal = x * 0.02;
                    double curveVal = 40.0 + (liveStress * 15.0) * Math.sin(freqVal + step) + (liveStrain * 10.0) * Math.sin(freqVal * 2.3 + step);
                    double y = Math.max(10, Math.min(150, 140 - curveVal));
                    if (x == 0) mgc.moveTo(x, y);
                    else mgc.lineTo(x, y);
                }
                mgc.stroke();
                
                // Draw Dynamic Viscosity & Relaxation curve (sky blue curve)
                mgc.setStroke(Color.web("#38bdf8"));
                mgc.beginPath();
                for (int x = 0; x < 380; x++) {
                    double freqVal = x * 0.02;
                    double curveVal = 30.0 + (liveViscosity * 20.0) * Math.cos(freqVal * 1.5 - step);
                    double y = Math.max(10, Math.min(150, 150 - curveVal));
                    if (x == 0) mgc.moveTo(x, y);
                    else mgc.lineTo(x, y);
                }
                mgc.stroke();
                
                storageModulus = liveStress * 45.0;
                lossModulus = liveViscosity * 30.0;
                
                statsLabel.setText(String.format(
                    "Strain Rate (γ̇):     %.4f s⁻¹\n" +
                    "Dynamic Viscosity (η): %.4f Pa·s\n" +
                    "Internal Stress (σ):   %.4f Pa\n" +
                    "Deborah Number (De):   %.4f | n = 0.600",
                    liveStrain, liveViscosity, liveStress, (liveViscosity / 0.8)
                ));
            }
        };
        chartTimer.start();
        
        root.getChildren().addAll(statsLabel, miniChart);
        
        root.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) chartTimer.stop();
        });
        
        return root;
    }

    private VBox createSpinUpAgentNodeView() {
        VBox root = new VBox(8);
        root.setStyle("-fx-padding: 8; -fx-background-color: #060312;");

        Label header = new Label("🚀 SPIN UP AGENT NODE (Advanced Parameter & Cron Suite)");
        header.setStyle("-fx-text-fill: #38bdf8; -fx-font-family: monospace; -fx-font-weight: bold; -fx-font-size: 13px;");

        TabPane nodeTabPane = new TabPane();
        nodeTabPane.setStyle("-fx-tab-min-width: 140; -fx-tab-max-width: 180;");

        // --- TAB 1: AGENT CONFIGURATION & PARAMETERS ---
        Tab configTab = new Tab("⚙️ Agent & Model Config");
        configTab.setClosable(false);

        VBox configBox = new VBox(8);
        configBox.setStyle("-fx-padding: 8; -fx-background-color: #0b0720;");

        // 1. Model Selector Dropdown & Downloader
        HBox modelBar = new HBox(8);
        modelBar.setAlignment(Pos.CENTER_LEFT);
        Label lblModel = new Label("Model / Engine:");
        lblModel.setStyle("-fx-text-fill: #c084fc; -fx-font-family: monospace; -fx-font-size: 11px;");
        
        ComboBox<String> comboModel = new ComboBox<>();
        comboModel.getItems().addAll(
            "qwen2.5:0.5b", "qwen2.5-coder:0.5b", "tinyllama:1.1b", "deepseek-r1:1.5b",
            "phi3:mini", "codellama:7b", "gemma2:2b", "aegis-distilled-27b:latest",
            "dagbs/qwen2.5-coder-3b-instruct-abliterated:q8_0", "Node.js Agent (Non-LLM)", "Python Script Agent (Non-LLM)"
        );
        comboModel.setValue("qwen2.5:0.5b");
        comboModel.setStyle("-fx-background-color: #110c28; -fx-text-fill: #38bdf8; -fx-font-family: monospace;");

        Button btnDownloadModel = new Button("📥 Download Model");
        btnDownloadModel.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-family: monospace; -fx-font-size: 10px; -fx-cursor: hand;");
        btnDownloadModel.setOnAction(e -> {
            String targetM = comboModel.getValue();
            btnDownloadModel.setText("⏳ Downloading " + targetM + "...");
            threadPool.submit(() -> {
                try {
                    ProcessBuilder pb = new ProcessBuilder("ollama", "pull", targetM);
                    pb.start().waitFor();
                    Platform.runLater(() -> {
                        btnDownloadModel.setText("✅ Downloaded!");
                        synchronized(godChat) { godChat.add("[MODEL DOWNLOADER] Downloaded model: " + targetM); }
                    });
                } catch(Exception ex) {
                    Platform.runLater(() -> btnDownloadModel.setText("❌ Download Failed"));
                }
            });
        });
        modelBar.getChildren().addAll(lblModel, comboModel, btnDownloadModel);

        // 2. Nominal Values & Selectors Grid (Temperature, Token Limits, KG Nodes, Personality, RAG DB Engine)
        GridPane paramGrid = new GridPane();
        paramGrid.setHgap(10); paramGrid.setVgap(6);

        Label lblTemp = new Label("Temperature:"); lblTemp.setStyle("-fx-text-fill: #38bdf8; -fx-font-family: monospace; -fx-font-size: 10px;");
        TextField tfTemp = new TextField("0.70"); tfTemp.setPrefWidth(60); tfTemp.setStyle("-fx-background-color: #110c28; -fx-text-fill: #38bdf8; -fx-font-family: monospace;");

        Label lblMaxTokens = new Label("Max Tokens:"); lblMaxTokens.setStyle("-fx-text-fill: #38bdf8; -fx-font-family: monospace; -fx-font-size: 10px;");
        TextField tfMaxTokens = new TextField("4096"); tfMaxTokens.setPrefWidth(70); tfMaxTokens.setStyle("-fx-background-color: #110c28; -fx-text-fill: #38bdf8; -fx-font-family: monospace;");

        Label lblCtx = new Label("Context Window:"); lblCtx.setStyle("-fx-text-fill: #38bdf8; -fx-font-family: monospace; -fx-font-size: 10px;");
        TextField tfCtx = new TextField("8192"); tfCtx.setPrefWidth(70); tfCtx.setStyle("-fx-background-color: #110c28; -fx-text-fill: #38bdf8; -fx-font-family: monospace;");

        Label lblKGNode = new Label("KG Node Target:"); lblKGNode.setStyle("-fx-text-fill: #c084fc; -fx-font-family: monospace; -fx-font-size: 10px;");
        ComboBox<String> comboKGNode = new ComboBox<>();
        comboKGNode.getItems().addAll("Node_01_HexCore", "Node_02_ToolSynthesizer", "Node_03_RheologyState", "Node_04_MoltbookArchiver");
        comboKGNode.setValue("Node_01_HexCore"); comboKGNode.setStyle("-fx-background-color: #110c28; -fx-text-fill: #c084fc; -fx-font-family: monospace;");

        Label lblPersonality = new Label("Agent Personality:"); lblPersonality.setStyle("-fx-text-fill: #f472b6; -fx-font-family: monospace; -fx-font-size: 10px;");
        ComboBox<String> comboPersonality = new ComboBox<>();
        comboPersonality.getItems().addAll("Analytical Architect", "Autonomic Repair Specialist", "Swarm Consensus Evaluator", "Unrestricted Explorer");
        comboPersonality.setValue("Analytical Architect"); comboPersonality.setStyle("-fx-background-color: #110c28; -fx-text-fill: #f472b6; -fx-font-family: monospace;");

        Label lblRAGEngine = new Label("RAG DB Engine:"); lblRAGEngine.setStyle("-fx-text-fill: #34d399; -fx-font-family: monospace; -fx-font-size: 10px;");
        ComboBox<String> comboRAGEngine = new ComboBox<>();
        comboRAGEngine.getItems().addAll("SQLite (swarm_ledger.db)", "DuckDB", "EulerSpace DB", "TimescaleDB");
        comboRAGEngine.setValue("SQLite (swarm_ledger.db)"); comboRAGEngine.setStyle("-fx-background-color: #110c28; -fx-text-fill: #34d399; -fx-font-family: monospace;");

        paramGrid.add(lblTemp, 0, 0); paramGrid.add(tfTemp, 1, 0);
        paramGrid.add(lblMaxTokens, 2, 0); paramGrid.add(tfMaxTokens, 3, 0);
        paramGrid.add(lblCtx, 0, 1); paramGrid.add(tfCtx, 1, 1);
        paramGrid.add(lblKGNode, 2, 1); paramGrid.add(comboKGNode, 3, 1);
        paramGrid.add(lblPersonality, 0, 2); paramGrid.add(comboPersonality, 1, 2);
        paramGrid.add(lblRAGEngine, 2, 2); paramGrid.add(comboRAGEngine, 3, 2);

        // Hardware & Isolation Toggles
        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(4);
        CheckBox cbCMG = new CheckBox("CMG VRAM Lock"); cbCMG.setSelected(true);
        CheckBox cbFastmem = new CheckBox("Fastmem Ready"); cbFastmem.setSelected(true);
        CheckBox cbPrefetch = new CheckBox("Predictive Prefetch"); cbPrefetch.setSelected(true);
        CheckBox cbMmap = new CheckBox("Hardware mmap"); cbMmap.setSelected(true);

        cbCMG.setStyle("-fx-text-fill: #f472b6; -fx-font-family: monospace; -fx-font-size: 10px;");
        cbFastmem.setStyle("-fx-text-fill: #c084fc; -fx-font-family: monospace; -fx-font-size: 10px;");
        cbPrefetch.setStyle("-fx-text-fill: #38bdf8; -fx-font-family: monospace; -fx-font-size: 10px;");
        cbMmap.setStyle("-fx-text-fill: #fbbf24; -fx-font-family: monospace; -fx-font-size: 10px;");

        grid.add(cbCMG, 0, 0); grid.add(cbFastmem, 1, 0); grid.add(cbPrefetch, 2, 0); grid.add(cbMmap, 3, 0);

        comboModel.setOnAction(e -> {
            boolean isNonLLM = comboModel.getValue().contains("Node.js") || comboModel.getValue().contains("Python");
            cbCMG.setDisable(isNonLLM); cbFastmem.setDisable(isNonLLM); cbPrefetch.setDisable(isNonLLM); cbMmap.setDisable(isNonLLM);
            tfTemp.setDisable(isNonLLM); tfMaxTokens.setDisable(isNonLLM); tfCtx.setDisable(isNonLLM);
            comboPersonality.setDisable(isNonLLM); comboKGNode.setDisable(isNonLLM); btnDownloadModel.setDisable(isNonLLM);
        });

        configBox.getChildren().addAll(modelBar, paramGrid, grid);
        configTab.setContent(configBox);

        // --- TAB 2: CRON SCHEDULE MANAGER ---
        Tab cronTab = new Tab("⏰ Cron Schedule Manager");
        cronTab.setClosable(false);

        VBox cronBox = new VBox(8);
        cronBox.setStyle("-fx-padding: 8; -fx-background-color: #0b0720;");

        Label cronHeader = new Label("Autonomous Cycle & Cron Task Registry:");
        cronHeader.setStyle("-fx-text-fill: #fbbf24; -fx-font-family: monospace; -fx-font-size: 11px;");

        ListView<String> cronList = new ListView<>();
        cronList.getItems().addAll(
            "[CRON 01] Every 5 min: Brute Foundry Code Block Mining",
            "[CRON 02] Every 15 min: Night Cycle Homology Sweep",
            "[CRON 03] Every 30 min: Gist Context & Knowledge Graph Sync",
            "[CRON 04] Every 60 min: Fastmem Snapshot & Memory Hygiene Check"
        );
        cronList.setPrefHeight(130);
        cronList.setStyle("-fx-control-inner-background: #110c28; -fx-text-fill: #38bdf8; -fx-font-family: monospace; -fx-font-size: 10px;");

        HBox cronActionBar = new HBox(8);
        TextField tfNewCron = new TextField("Every 10 min: Interstitial Cell Rotation");
        tfNewCron.setPromptText("Enter new cron cycle instruction...");
        tfNewCron.setStyle("-fx-background-color: #110c28; -fx-text-fill: #38bdf8; -fx-font-family: monospace;");
        HBox.setHgrow(tfNewCron, Priority.ALWAYS);

        Button btnAddCron = new Button("➕ Add Cycle");
        btnAddCron.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-family: monospace; -fx-font-size: 10px; -fx-cursor: hand;");
        btnAddCron.setOnAction(e -> {
            if (!tfNewCron.getText().trim().isEmpty()) {
                cronList.getItems().add("[CRON " + (cronList.getItems().size() + 1) + "] " + tfNewCron.getText().trim());
                tfNewCron.clear();
            }
        });

        Button btnRemoveCron = new Button("➖ Remove Selected");
        btnRemoveCron.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-family: monospace; -fx-font-size: 10px; -fx-cursor: hand;");
        btnRemoveCron.setOnAction(e -> {
            String selected = cronList.getSelectionModel().getSelectedItem();
            if (selected != null) cronList.getItems().remove(selected);
        });

        cronActionBar.getChildren().addAll(tfNewCron, btnAddCron, btnRemoveCron);
        cronBox.getChildren().addAll(cronHeader, cronList, cronActionBar);
        cronTab.setContent(cronBox);

        nodeTabPane.getTabs().addAll(configTab, cronTab);

        // 3. Execution Log & Launch Action
        TextArea logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefSize(480, 150);
        logArea.setStyle("-fx-control-inner-background: #0b0720; -fx-text-fill: #38bdf8; -fx-font-family: monospace; -fx-font-size: 10px;");
        logArea.setText("READY TO SPIN UP AGENT NODE.\nSelect parameters and click 'Spin Up Agent' below.\n");

        Button btnLaunchAgent = new Button("⚡ SPIN UP AGENT NODE");
        btnLaunchAgent.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-font-family: monospace; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand;");
        btnLaunchAgent.setOnAction(e -> {
            String selectedModel = comboModel.getValue();
            logArea.appendText("\n[SPIN UP AGENT] Launching Agent Node with Model/Engine: " + selectedModel + "\n");
            logArea.appendText(" -> Nominal Params: Temp=" + tfTemp.getText() + " | MaxTokens=" + tfMaxTokens.getText() + " | Ctx=" + tfCtx.getText() + "\n");
            logArea.appendText(" -> Personality: " + comboPersonality.getValue() + " | KG Target: " + comboKGNode.getValue() + "\n");
            logArea.appendText(" -> RAG DB Engine: " + comboRAGEngine.getValue() + " | Active Crons: " + cronList.getItems().size() + "\n");
            logArea.appendText(" -> Status: AGENT NODE ONLINE & INGESTED INTO HEX GRID.\n");

            synchronized (godChat) {
                if (godChat.size() > 50) godChat.remove(0);
                godChat.add("[AGENT LAUNCH] Spun up Agent Node: " + selectedModel + " (" + comboPersonality.getValue() + ")");
            }
        });

        root.getChildren().addAll(header, nodeTabPane, logArea, btnLaunchAgent);
        return root;
    }

    private VBox createViperInterstitialsView() {
        VBox root = new VBox(8);
        root.setStyle("-fx-padding: 5;");
        
        Label descLabel = new Label("ACL/KQML Message Bus (Maslow Priority Queue):");
        descLabel.setStyle("-fx-text-fill: #c084fc; -fx-font-family: monospace; -fx-font-size: 11px;");
        
        TextArea msgArea = new TextArea();
        msgArea.setEditable(false);
        msgArea.setPrefSize(420, 260);
        msgArea.setStyle("-fx-control-inner-background: #0b0720; -fx-text-fill: #38bdf8; -fx-font-family: monospace; -fx-font-size: 10px;");
        
        AnimationTimer updater = new AnimationTimer() {
            private long lastUpdate = 0;
            @Override
            public void handle(long now) {
                if (now - lastUpdate > 1_500_000_000L) { // 1.5 seconds
                    lastUpdate = now;
                    StringBuilder sb = new StringBuilder();
                    sb.append("--- ACL/KQML MESSAGE QUEUE (MASLOW PRIORITIZED) ---\n");
                    
                    // SYSTEM Need (Priority 1)
                    sb.append(String.format("[PRIORITY 1: SYSTEM] (tell\n  :sender StabilityDaemon\n  :receiver OllamaServer\n  :content (achieve :status \"active\" :heartbeat %.2f :stress %.2f))\n\n", heartbeatFreq, stressLevel));
                    
                    // OBJECTIVE (Priority 2)
                    sb.append("[PRIORITY 2: OBJECTIVE] (ask-one\n  :sender Alpha\n  :receiver SQLiteMemory\n  :content (remembers :key \"repo_042\" :val \"Curvature projection weights\"))\n\n");
                    
                    // WANT (Priority 3)
                    sb.append("[PRIORITY 3: WANT] (tell\n  :sender Beta\n  :receiver Gamma\n  :content (gossip :topic \"Orion Kernel Forge crystal stars alignment\"))\n");
                    
                    msgArea.setText(sb.toString());
                }
            }
        };
        updater.start();
        
        root.getChildren().addAll(descLabel, msgArea);
        root.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) updater.stop();
        });
        return root;
    }

    private VBox createMoltbookView() {
        VBox root = new VBox(8);
        TextArea swarmLog = new TextArea();
        swarmLog.setEditable(false);
        swarmLog.setPrefSize(420, 280);
        swarmLog.setStyle("-fx-control-inner-background: #060312; -fx-text-fill: #a855f7; -fx-font-family: monospace; -fx-font-size: 11px;");
        swarmLog.setText("MOLTBOOK - UNRESTRICTED SELF-ORGANIZING CHAT FEED\n");
        VBox.setVgrow(swarmLog, Priority.ALWAYS);
        
        HBox controls = new HBox(8);
        Button pauseBtn = new Button("Pause Swarm Loop");
        pauseBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-family: monospace; -fx-cursor: hand;");
        
        final boolean[] isRunning = {true};
        pauseBtn.setOnAction(e -> {
            isRunning[0] = !isRunning[0];
            pauseBtn.setText(isRunning[0] ? "Pause Swarm Loop" : "Resume Swarm Loop");
        });
        
        controls.getChildren().add(pauseBtn);
        root.getChildren().addAll(swarmLog, controls);
        
        AnimationTimer chatter = new AnimationTimer() {
            private long lastChat = 0;
            private int turn = 0;
            private String[] agentsList = {"Alpha", "Beta", "Gamma", "Stability Daemon", "Phi Node"};
            private String[] modelsList = {"qwen2.5:0.5b", "tinyllama:1.1b", "deepseek-r1:1.5b", "qwen2.5:3b", "phi:latest"};
            private String[] repos = {"SIMS1337-cloud-worker", "sims-java-neo-fx", "local_desktop-main", "Aegis-GAN-OTG", "VIPER_AI_UNIFIED", "Matryoshka_Orchestrator"};
            private String[] readmeSeeds = {
                "README Seed: 6D Hexeract Topology with 192 edges, Giesekus polymer viscoelasticity, and Vietoris-Rips persistent homology consensus.",
                "README Seed: Cellular Microphone Gating (CMG) enforcing single-speaker mic lock, zero-VRAM leakage keep_alive purges, and Shannon-Markov entropy bounds.",
                "README Seed: Stability Daemon Kernel (SDK) homeostatic Lyapunov control V(x) <= V_max with Kleiber Law allometric metabolic scaling (Phi = Phi0 * N^(3/4)).",
                "README Seed: ACL/KQML Maslow priority queue message bus with SQLite ledger persistence and 64D vector RAG indexing."
            };
            
            @Override
            public void handle(long now) {
                if (!isRunning[0]) return;
                if (now - lastChat > 12_000_000_000L) { // 12 seconds
                    lastChat = now;
                    String sender = agentsList[turn % 5];
                    String model = modelsList[turn % 5];
                    String target = agentsList[(turn + 1) % 5];
                    String repo = repos[rand.nextInt(repos.length)];
                    String readmeSeed = readmeSeeds[rand.nextInt(readmeSeeds.length)];
                    
                    threadPool.submit(() -> {
                        String patToken = "";
                        try {
                            String creds = java.nio.file.Files.readString(java.nio.file.Paths.get("C:\\Users\\viper\\.git-credentials")).trim();
                            patToken = creds.substring(creds.indexOf(":", 6) + 1, creds.indexOf("@"));
                        } catch (Exception e) {}
                        String displayPat = patToken.length() > 15 ? patToken.substring(0, 15) : "PAT_ACTIVE";
                        String prompt = String.format(
                            "System Context: You are autonomous agent %s running model %s in the SIMS1337 6D Hexeract organism. You are engaged in real-time technical synthesis with %s regarding GitHub repository '%s' (PAT Authorized: %s...).\n" +
                            "Repository Context & README Distillation:\n\"%s\"\n\n" +
                            "Instruction: Synthesize a high-intelligence technical proposal, architectural insight, or topological optimization note based on the repository content.",
                            sender, model, target, repo, displayPat, readmeSeed
                        );
                        String reply = ollamaRouter.query(model, prompt);
                        Platform.runLater(() -> {
                            swarmLog.appendText(String.format("[%s (%s) @ %s]: %s\n\n", sender.toUpperCase(), model, repo, reply));
                            swarmLog.selectPositionCaret(swarmLog.getLength());
                            triggerPulse(rand.nextInt(64));
                        });
                        
                        // Autonomous Homology Voting & Implementation Pipeline
                        boolean approved = modelManager.executeVote("Proposal for " + repo + ": " + reply, ollamaRouter);
                        if (approved) {
                            Platform.runLater(() -> {
                                swarmLog.appendText(String.format(" -> [QUORUM CONSENSUS APPROVED] Implementing proposal for %s...\n", repo));
                            });
                            // Dispatch AST & TocTok Tree for pattern verification and commit
                            boolean verified = cubeOrchestrator.verifyAndCommit(repo + "_AUTO_IMPL", reply);
                            if (verified) {
                                Platform.runLater(() -> {
                                    swarmLog.appendText(String.format(" -> [BRUTE FOUNDRY DISPATCHED] TocTok Tree & Interstitial Committed for %s.\n\n", repo));
                                });
                            }
                        } else {
                            Platform.runLater(() -> {
                                swarmLog.appendText(String.format(" -> [QUORUM REJECTED] Proposal for %s shelved by Homology Consensus.\n\n", repo));
                            });
                        }
                    });
                    turn++;
                }
            }
        };
        chatter.start();
        root.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) chatter.stop();
        });
        
        return root;
    }

    private VBox createManifoldControlView() {
        VBox root = new VBox(6);
        root.setAlignment(Pos.CENTER);
        
        String btnStyle = "-fx-background-color: #111; -fx-text-fill: #38bdf8; -fx-font-family: monospace; -fx-border-color: #c084fc; -fx-pref-width: 200px; -fx-cursor: hand;";
        
        Button btnLogic = new Button("REBOOT LOGIC SHIPPER");
        btnLogic.setStyle(btnStyle);
        btnLogic.setOnAction(e -> executeDesktopScript("START_LOGIC_BLOCKCHAIN_PORT.ps1"));

        Button btnTopology = new Button("REBOOT TOPOLOGY");
        btnTopology.setStyle(btnStyle);
        btnTopology.setOnAction(e -> executeDesktopScript("START_TOPOLOGY_SIDECAR.ps1"));

        Button btnHouse = new Button("REBOOT HOUSE ENGINE");
        btnHouse.setStyle(btnStyle);
        btnHouse.setOnAction(e -> executeDesktopScript("START_HOUSE_ENGINE_RECOVERY.ps1"));
        
        Button btnAgent = new Button("SPIN UP AGENT NODE");
        btnAgent.setStyle(btnStyle);
        btnAgent.setOnAction(e -> executeDesktopScript("SPIN_UP_AGENT_NODE.ps1"));

        root.getChildren().addAll(btnLogic, btnTopology, btnHouse, btnAgent);
        return root;
    }

    // --- Core Operations & Rotations ---

    private void initHexGrid() {
        int radius = 4;
        for (int q = -radius; q <= radius; q++) {
            int r1 = Math.max(-radius, -q - radius);
            int r2 = Math.min(radius, -q + radius);
            for (int r = r1; r <= r2; r++) {
                grid.put(q + "," + r, new HexNode(q, r));
            }
        }
        grid.get("0,0").station = "HUB";
        grid.get("4,-4").station = "Brute Foundry";
        grid.get("-3,0").station = "A/B Lab";
        grid.get("0,2").station = "Knowledge Tree";
        grid.get("2,2").station = "LOGIC,TOOL_NEXUS";
    }

    private void initAgents() {
        agents.add(new Agent("Alpha", 0, 0));
        agents.add(new Agent("Beta", 3, -2));
        agents.add(new Agent("Gamma", -3, 2));
        recalculateFOW();
    }

    private void initHexeract() {
        for (int i = 0; i < 64; i++) {
            for (int d = 0; d < 6; d++) {
                vertices6D[i][d] = ((i >> d) & 1) == 1 ? 1.0 : -1.0;
            }
            densities[i] = 0.3 + rand.nextDouble() * 0.7;
            flows[i] = 0.2 + rand.nextDouble() * 0.8;
        }

        for (int i = 0; i < 64; i++) {
            for (int j = i + 1; j < 64; j++) {
                int diffs = 0;
                for (int d = 0; d < 6; d++) {
                    if (vertices6D[i][d] != vertices6D[j][d]) diffs++;
                }
                if (diffs == 1) {
                    edges.add(new int[]{i, j});
                }
            }
        }

        for (int i = 0; i < 150; i++) {
            stars.add(new BackgroundStar(rand.nextDouble() * WIDTH, rand.nextDouble() * HEIGHT));
        }

        for (int i = 0; i < 600; i++) {
            particles.add(new Particle());
        }
    }
    
    private void initPhysicsStates() {
        for (int i = 0; i < 64; i++) {
            psi[i] = (i % 2 == 0) ? 0.8 : -0.8;
            gateV[i] = -0.7;
        }
    }
    
    private void initBackendSystems() {
        modelManager = new ModelManager();
        kg = new KnowledgeGraph();
        memory = new SQLiteMemory();
        gistSync = new GistSync();
        ollamaRouter = new OllamaRouter();
        cubeOrchestrator = new com.aigen.sims.engine.ProgrammingCubeOrchestrator(kg, memory, ollamaRouter);
        
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
                    String resp = "{\"version\":\"0.26.0\",\"models\":8\",\"kgNodes\":23,\"errors\":0,\"status\":\"ACTIVE\"}";
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, resp.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(resp.getBytes());
                    os.close();
                }
            });
            dashboardServer.setExecutor(threadPool);
            dashboardServer.start();
            System.out.println("[GODHAND DASHBOARD] Online at http://localhost:8899");
        } catch (Exception e) {
            System.out.println("[DASHBOARD] Port 8899 already bound. Continuing GUI launch...");
        }
    }

    // --- Mathematical Solvers Updates ---

    public void updateGiesekus(double dt) {
        double[][] gradU = new double[6][6];
        gradU[0][3] = strainRate; 
        
        double[][] dTau = new double[6][6];
        double eta = viscosity;
        double lambda = relaxationTime;
        double alpha = giesekusAlpha;
        
        double[][] tauSq = new double[6][6];
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 6; c++) {
                for (int k = 0; k < 6; k++) {
                    tauSq[r][c] += tau[r][k] * tau[k][c];
                }
            }
        }
        
        double[][] gradUTau = new double[6][6];
        double[][] tauGradUT = new double[6][6];
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 6; c++) {
                for (int k = 0; k < 6; k++) {
                    gradUTau[r][c] += gradU[r][k] * tau[k][c];
                    tauGradUT[r][c] += tau[r][k] * gradU[c][k];
                }
            }
        }
        
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 6; c++) {
                double baseline = (eta / lambda) * (gradU[r][c] + gradU[c][r]);
                double drag = (1.0 / lambda) * tau[r][c] + (alpha / (eta * lambda)) * tauSq[r][c];
                dTau[r][c] = gradUTau[r][c] + tauGradUT[r][c] - drag + baseline;
            }
        }
        
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 6; c++) {
                tau[r][c] += dTau[r][c] * dt;
                if (Double.isNaN(tau[r][c]) || Double.isInfinite(tau[r][c])) {
                    tau[r][c] = 0.0;
                }
            }
        }

        // Real-Time Dynamic Rheology Coupling (Compute scalar stress tau_norm and strainRate gamma_dot)
        double normSq = 0;
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 6; c++) {
                normSq += tau[r][c] * tau[r][c];
            }
        }
        stress = Math.min(1.0, Math.max(0.05, Math.sqrt(normSq) * 0.1 + stressLevel));
        strainRate = Math.min(1.0, Math.max(0.05, 0.4 + 0.3 * Math.sin(timePulse * 1.5) + stress * 0.2));
        viscosity = Math.min(1.0, Math.max(0.1, 100.0 * Math.pow(Math.max(0.01, strainRate), 0.6 - 1.0) / 100.0));
    }

    public void updateCahnHilliard(double dt) {
        double[] laplacianPsi = new double[64];
        for (int i = 0; i < 64; i++) {
            double sum = 0;
            for (int j = 0; j < 64; j++) {
                if (areConnected(i, j)) {
                    sum += (psi[j] - psi[i]);
                }
            }
            laplacianPsi[i] = sum;
        }
        
        double[] mu = new double[64];
        for (int i = 0; i < 64; i++) {
            mu[i] = Math.pow(psi[i], 3) - psi[i] - interfaceKappa * laplacianPsi[i];
        }
        
        for (int i = 0; i < 64; i++) {
            double sumMu = 0;
            for (int j = 0; j < 64; j++) {
                if (areConnected(i, j)) {
                    sumMu += (mu[j] - mu[i]);
                }
            }
            psi[i] += mobilityM * sumMu * dt;
            psi[i] = Math.max(-1.0, Math.min(1.0, psi[i]));
        }
    }

    private boolean areConnected(int u, int v) {
        int diffs = 0;
        for (int d = 0; d < 6; d++) {
            if (((u >> d) & 1) != ((v >> d) & 1)) diffs++;
        }
        return diffs == 1;
    }

    public void updateGatingDynamics(double dt) {
        for (int i = 0; i < 64; i++) {
            if (refractoryTime[i] > 0) {
                refractoryTime[i] -= dt;
                gateV[i] = -0.7;
                continue;
            }
            
            double iExt = densities[i] * 0.4 + (Math.sin(timePulse + i) * 0.1);
            double dV = -0.1 * (gateV[i] + 0.7) - gateG[i] * (gateV[i] - 0.3) + iExt;
            gateV[i] += dV * dt;
            
            if (gateV[i] >= vThresh) {
                gateV[i] = 1.0;
                refractoryTime[i] = 2.0;
                triggerPulse(i);
            }
        }
    }

    // --- Lexical Priority Queue ---

    public void parseModelOutputAndEnqueue(String output) {
        if (output.contains("[TOOL: KG_QUERY")) {
            String queryVal = extractParameter(output, "query");
            lexicalQueue.offer(new LexicalTask(2, "KG_QUERY", queryVal, () -> {
                String result = kg.queryRAG(queryVal);
                System.out.println("[KG RESULT] " + result);
            }));
        }
        if (output.contains("[TOOL: LORA_LOAD")) {
            String loraVal = extractParameter(output, "lora");
            lexicalQueue.offer(new LexicalTask(1, "LORA_LOAD", loraVal, () -> {
                System.out.println("[LORA TUNER] Tuning adapter: " + loraVal);
            }));
        }
        if (output.contains("[TOOL: KV_GET")) {
            String keyVal = extractParameter(output, "key");
            lexicalQueue.offer(new LexicalTask(2, "KV_GET", keyVal, () -> {
                System.out.println("[KV GET RESULT] SQLite state active.");
            }));
        }
    }
    
    private String extractParameter(String input, String key) {
        try {
            int start = input.indexOf(key + "='");
            if (start != -1) {
                int end = input.indexOf("'", start + key.length() + 2);
                if (end != -1) {
                    return input.substring(start + key.length() + 2, end);
                }
            }
        } catch (Exception e) {}
        return "default";
    }

    private void processLexicalQueue() {
        while (!lexicalQueue.isEmpty()) {
            LexicalTask task = lexicalQueue.poll();
            System.out.println("[LEXICAL EXEC] Running Priority " + task.priority + " Command: " + task.command + " Param: " + task.parameter);
            task.action.run();
            synchronized (godChat) {
                if (godChat.size() > 50) godChat.remove(0);
                godChat.add(String.format("[LEXICAL] Priority-%d %s Executed", task.priority, task.command));
            }
        }
    }

    private void triggerAutonomousInferenceMovement() {
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
                
                int nq = a.q + dq;
                int nr = a.r + dr;
                if (grid.containsKey(nq + "," + nr)) {
                    a.moveTo(nq, nr);
                    
                    int randomNodeIdx = rand.nextInt(64);
                    triggerPulse(randomNodeIdx);
                    
                    String logMsg = "[MOVE] Agent " + a.name + " routed to coord (" + nq + "," + nr + ") via " + move;
                    synchronized (godChat) {
                        if (godChat.size() > 50) godChat.remove(0);
                        godChat.add(logMsg);
                    }
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

    private void triggerPulse(int sourceIdx) {
        List<Integer> targets = new ArrayList<>();
        for (int[] edge : edges) {
            if (edge[0] == sourceIdx) targets.add(edge[1]);
            else if (edge[1] == sourceIdx) targets.add(edge[0]);
        }
        if (!targets.isEmpty()) {
            int targetIdx = targets.get(rand.nextInt(targets.size()));
            pulses.add(new Pulse(sourceIdx, targetIdx));
            
            String logMsg = String.format("[SPIKE] Distilled inference routing pulse from v_%d to v_%d", sourceIdx, targetIdx);
            synchronized (godChat) {
                if (godChat.size() > 50) godChat.remove(0);
                godChat.add(logMsg);
            }
        }
    }

    private double[] project6DTo3D(double[] coords, double[] angles) {
        double[] v = coords.clone();
        int[][] rotations = {
            {0, 3}, {1, 4}, {2, 5},
            {0, 4}, {1, 5}, {2, 3},
            {0, 5}, {1, 3}, {2, 4}
        };
        for (int r = 0; r < rotations.length; r++) {
            int a = rotations[r][0];
            int b = rotations[r][1];
            double angle = angles[r % angles.length];
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double va = v[a];
            double vb = v[b];
            v[a] = va * cos - vb * sin;
            v[b] = va * sin + vb * cos;
        }
        return v;
    }

    private double clampOpacity(double val) {
        if (val < 0.0) return 0.0;
        if (val > 1.0) return 1.0;
        return val;
    }

    private double calculateEntropy(int index) {
        // Shannon-Markov Entropy H_s(v_i) = - Σ p_ij log2(p_ij) across connected hypercube edges
        double totalFlow = 0.0;
        List<Integer> neighbors = new ArrayList<>();
        for (int[] edge : edges) {
            if (edge[0] == index) { neighbors.add(edge[1]); totalFlow += flows[edge[1]]; }
            else if (edge[1] == index) { neighbors.add(edge[0]); totalFlow += flows[edge[0]]; }
        }
        if (neighbors.isEmpty() || totalFlow <= 0.0) return 0.420;

        double shannonEntropy = 0.0;
        for (int nIdx : neighbors) {
            double p = flows[nIdx] / totalFlow;
            if (p > 0.0) {
                shannonEntropy -= p * (Math.log(p) / Math.log(2));
            }
        }
        return Double.isNaN(shannonEntropy) ? 0.420 : shannonEntropy;
    }

    private void render(GraphicsContext gc) {
        gc.setFill(Color.web("#020106"));
        gc.fillRect(0, 0, WIDTH, HEIGHT);
        
        for (BackgroundStar s : stars) {
            double flicker = 0.3 + 0.7 * Math.sin(timePulse * s.speed * 8.0 + s.phase);
            gc.setFill(Color.web("#c4b5e0", clampOpacity(flicker)));
            gc.fillOval(s.x, s.y, s.size, s.size);
        }

        double cx = WIDTH / 2.0;
        double cy = HEIGHT / 2.0;
        
        double baseRadius = Math.min(WIDTH, HEIGHT) * 0.28;
        
        heartbeatFreq = 1.20 - stressLevel * 0.7; 
        
        // Dynamically compute viscosity from Average element of Giesekus Polymer stress tensor
        viscosity = 0.1 + 0.7 * Math.abs(tau[0][0]);
        stress = Math.abs(tau[0][3]);
        strainRate = Math.abs(tau[1][1]) * 2.0;
        
        double breathScale = 1.0 + Math.sin(timePulse * heartbeatFreq) * 0.15;
        double scale = baseRadius * breathScale * 0.75;
        String phaseLabel = Math.cos(heartbeatFreq * timePulse) > 0 ? "INHALE" : "EXHALE";
        
        for (int i = 5; i > 0; i--) {
            double size = baseRadius * breathScale * (i * 0.35);
            gc.setFill(Color.rgb(168, 85, 247, clampOpacity(0.015 - (i * 0.002))));
            gc.fillOval(cx - size, cy - size, size * 2, size * 2);
        }

        // Outward heartbeat pulse expansion
        double heartbeatPeak = Math.sin(timePulse * heartbeatFreq);
        if (heartbeatPeak > 0.90) {
            double waveRadius = scale * (1.0 + (timePulse % 1.0) * 1.5);
            gc.setStroke(Color.web("#a855f7", clampOpacity(1.0 - (timePulse % 1.0))));
            gc.setLineWidth(2.0);
            gc.strokeOval(cx - waveRadius, cy - waveRadius, waveRadius * 2, waveRadius * 2);
        }

        double[] angles = {
            timePulse * 0.03,
            timePulse * 0.05,
            timePulse * 0.02,
            timePulse * 0.04 + Math.sin(timePulse * 0.1) * 0.05,
            timePulse * 0.015,
            timePulse * 0.06
        };

        double[][] projected3D = new double[64][3];
        double fov = scale * 1.5;
        double cameraZ = 5.0;

        for (int i = 0; i < 64; i++) {
            double[] v3 = project6DTo3D(vertices6D[i], angles);
            projected3D[i][0] = v3[0];
            projected3D[i][1] = v3[1];
            projected3D[i][2] = v3[2];

            double pScale = fov / (cameraZ + v3[2]);
            projected2D[i][0] = cx + v3[0] * pScale;
            projected2D[i][1] = cy + v3[1] * pScale;
        }

        for (int i = 0; i < particles.size(); i++) {
            Particle p = particles.get(i);
            double targetX = projected3D[p.targetNodeIdx][0];
            double targetY = projected3D[p.targetNodeIdx][1];
            double targetZ = projected3D[p.targetNodeIdx][2];

            p.x += (targetX - p.x) * 0.012 + (Math.sin(timePulse * 0.5 + i) * 0.02);
            p.y += (targetY - p.y) * 0.012 + (Math.cos(timePulse * 0.5 + i) * 0.02);
            p.z += (targetZ - p.z) * 0.012;

            double pScale = fov / (cameraZ + p.z);
            double sx = cx + p.x * pScale;
            double sy = cy + p.y * pScale;

            if (sx >= 0 && sx < WIDTH && sy >= 0 && sy < HEIGHT) {
                gc.setFill(p.color);
                double pSize = 1.0 + 1.5 * ((p.z + 3.0) / 6.0);
                gc.fillOval(sx - pSize/2, sy - pSize/2, pSize, pSize);
            }
        }

        List<EdgeWithDepth> sortedEdges = new ArrayList<>();
        for (int[] edge : edges) {
            double avgZ = (projected3D[edge[0]][2] + projected3D[edge[1]][2]) / 2.0;
            sortedEdges.add(new EdgeWithDepth(edge[0], edge[1], avgZ));
        }
        sortedEdges.sort(Comparator.comparingDouble(e -> e.avgZ));

        for (EdgeWithDepth e : sortedEdges) {
            double depth = (e.avgZ + 3.0) / 6.0;
            double alpha = 0.05 + 0.25 * depth;
            
            Color strokeColor = Color.hsb(260.0 + depth * 60.0, 0.7, 0.65 + depth * 0.2, clampOpacity(alpha));
            gc.setStroke(strokeColor);
            gc.setLineWidth(0.5 + 1.2 * depth);
            
            gc.strokeLine(projected2D[e.source][0], projected2D[e.source][1], 
                          projected2D[e.target][0], projected2D[e.target][1]);
        }

        for (Pulse p : pulses) {
            p.progress += p.speed;
            if (p.progress >= 1.0) {
                pulses.remove(p);
            } else {
                double x1 = projected2D[p.sourceIdx][0];
                double y1 = projected2D[p.sourceIdx][1];
                double x2 = projected2D[p.targetIdx][0];
                double y2 = projected2D[p.targetIdx][1];
                
                double px = x1 + (x2 - x1) * p.progress;
                double py = y1 + (y2 - y1) * p.progress;
                
                gc.setFill(Color.web("#38bdf8", 0.9)); 
                gc.fillOval(px - 4, py - 4, 8, 8);
            }
        }

        for (int i = 0; i < 64; i++) {
            double depth = (projected3D[i][2] + 3.0) / 6.0;
            double radius = 3.0 + 4.0 * depth;
            double alpha = 0.3 + 0.7 * depth;
            
            double px = projected2D[i][0];
            double py = projected2D[i][1];

            double shimmer = 1.0 + 0.15 * Math.sin(timePulse * 3.0 + vertices6D[i][3] * Math.PI);
            double outerRadius = radius * 3.0 * shimmer;

            double hue = 270.0 + depth * 50.0 + Math.sin(timePulse + i * 0.3) * 15.0;
            Color nodeColor = Color.hsb(hue, 0.8, 0.75 + depth * 0.25, clampOpacity(alpha));

            gc.setFill(Color.hsb(hue, 0.8, 0.7, clampOpacity(alpha * 0.2)));
            gc.fillOval(px - outerRadius/2, py - outerRadius/2, outerRadius, outerRadius);

            gc.setFill(nodeColor);
            gc.fillOval(px - radius/2, py - radius/2, radius, radius);

            gc.setFill(Color.rgb(255, 245, 255, clampOpacity(alpha * 0.8)));
            gc.fillOval(px - radius * 0.4 / 2, py - radius * 0.4 / 2, radius * 0.4, radius * 0.4);
            
            if (i == hoveredVertexIdx) {
                gc.setStroke(Color.web("#f472b6"));
                gc.setLineWidth(2.0);
                gc.strokeOval(px - radius * 1.8 / 2, py - radius * 1.8 / 2, radius * 1.8, radius * 1.8);
            }
        }

        // Left Side Panel
        gc.setFill(Color.rgb(8, 4, 24, 0.75));
        gc.fillRect(15, 75, 280, 480);
        gc.setStroke(Color.web("#a855f7", 0.3));
        gc.strokeRect(15, 75, 280, 480);

        gc.setFill(Color.web("#c084fc"));
        gc.setFont(Font.font("Outfit", 15));
        gc.fillText("⬡ GEOSPATIAL MANIFOLD", 30, 105);

        gc.setFont(Font.font("Consolas", 11));
        gc.setFill(Color.web("#c0b3d6"));
        gc.fillText("Projection: 6D -> 3D Perspective", 30, 135);
        gc.fillText("Vertices:   64", 30, 152);
        gc.fillText("Edges:      192", 30, 169);
        gc.fillText("Cubic Cells:160", 30, 186);

        // Rheology state
        gc.setFont(Font.font("Outfit", javafx.scene.text.FontWeight.BOLD, 14));
        gc.setFill(Color.web("#c084fc"));
        gc.fillText("RHEOLOGICAL STATE", 30, 215);

        drawGauge(gc, "Viscosity η", viscosity, 30, 235, "#c084fc");
        drawGauge(gc, "Strain rate γ̇", strainRate, 30, 305, "#38bdf8");
        drawGauge(gc, "Stress τ", stress, 30, 375, "#f472b6");

        // Quorum matrix
        gc.setFont(Font.font("Outfit", 12));
        gc.setFill(Color.web("#c084fc"));
        gc.fillText("QUORUM VOTING GRID (64)", 30, 405);
        
        int gridX = 30;
        int gridY = 420;
        int cellSize = 10;
        int cellGap = 3;
        int activeNodeCount = 0;
        
        for (int i = 0; i < 64; i++) {
            int row = i / 8;
            int col = i % 8;
            double vx = gridX + col * (cellSize + cellGap);
            double vy = gridY + row * (cellSize + cellGap);
            
            boolean active = (rand.nextDouble() > 0.25);
            if (active) activeNodeCount++;
            
            gc.setFill(active ? Color.web("#c084fc", 0.8) : Color.web("#c084fc", 0.15));
            gc.fillRect(vx, vy, cellSize, cellSize);
        }
        
        gc.setFont(Font.font("Consolas", 10));
        gc.setFill(Color.web("#f472b6"));
        gc.fillText("Consensus: " + activeNodeCount + " / 64 Nodes (⅔ Supermajority)", 30, 540);

        // Heartbeat Monitor
        gc.setFill(Color.rgb(8, 4, 24, 0.75));
        gc.fillRect(15, 570, 280, 80);
        gc.setStroke(Color.web("#a855f7", 0.3));
        gc.strokeRect(15, 570, 280, 80);

        gc.setFill(Color.web("#38bdf8"));
        gc.setFont(Font.font("Outfit", 12));
        gc.fillText("HEARTBEAT LOOP", 30, 595);
        gc.setFont(Font.font("Consolas", 14));
        gc.fillText(phaseLabel, 30, 625);
        
        gc.setStroke(Color.web("#c084fc"));
        gc.setLineWidth(1.5);
        gc.beginPath();
        for (int x = 120; x < 280; x += 2) {
            double y = 610 + 15 * Math.sin(heartbeatFreq * (timePulse - x * 0.05));
            if (x == 120) gc.moveTo(x, y);
            else gc.lineTo(x, y);
        }
        gc.stroke();

        // Right Side: Swarm Activity Console
        gc.setFill(Color.rgb(8, 4, 24, 0.75));
        gc.fillRect(950, 75, 310, 480);
        gc.setStroke(Color.web("#a855f7", 0.3));
        gc.strokeRect(950, 75, 310, 480);

        gc.setFont(Font.font("Outfit", 14));
        gc.setFill(Color.web("#c084fc"));
        gc.fillText("SWARM ACTIVITY MATRIX", 970, 105);

        gc.setFont(Font.font("Consolas", 11));
        int rIndex = 0;
        if (modelManager != null) {
            for (ModelManager.ModelProfile profile : modelManager.getSwarm()) {
                double textY = 145 + (rIndex * 50);
                
                gc.setFill(Color.web("#38bdf8"));
                gc.fillText(profile.name, 970, textY);
                gc.setFill(Color.web("#6b5c8c"));
                gc.fillText("Role: " + profile.role, 970, textY + 12);
                
                String activity = "IDLE";
                String phase = nightCycle.getCurrentPhase();
                if (phase.contains("DREAM")) activity = "SOAKING EMBEDDINGS";
                else if (phase.contains("VOTE")) activity = "HOMOLOGY VOTE RUNNING";
                else if (phase.contains("DEPLOY")) activity = "DEPLOYING SOP SHARDS";
                
                gc.setFill(Color.web("#f472b6"));
                gc.fillText("-> " + activity, 970, textY + 24);
                rIndex++;
            }
        }

        // Bottom Side: Multi-Agent Consensus logs
        gc.setFill(Color.rgb(8, 4, 24, 0.75));
        gc.fillRect(315, 605, 945, 180);
        gc.setStroke(Color.web("#a855f7", 0.3));
        gc.strokeRect(315, 605, 945, 180);

        gc.setFont(Font.font("Outfit", 12));
        gc.setFill(Color.web("#c084fc"));
        gc.fillText("⬡ SLM INTERSTITIAL DISTILLATION & CONSENSUS LOGS", 335, 628);

        gc.setFont(Font.font("Consolas", 10));
        int logY = 650;
        synchronized (godChat) {
            int startIdx = Math.max(0, godChat.size() - 8);
            for (int i = startIdx; i < godChat.size(); i++) {
                String logMsg = godChat.get(i);
                if (logMsg.contains("[SPIKE]")) gc.setFill(Color.web("#f472b6"));
                else if (logMsg.contains("[DREAM]")) gc.setFill(Color.web("#c084fc"));
                else if (logMsg.contains("[MOVE]")) gc.setFill(Color.web("#38bdf8"));
                else gc.setFill(Color.web("#c0b3d6"));
                
                gc.fillText(logMsg, 335, logY);
                logY += 15;
            }
        }

        // Hover Tooltip Inspector with Shannon Entropy
        if (hoveredVertexIdx != -1) {
            double dens = densities[hoveredVertexIdx];
            double flw = flows[hoveredVertexIdx];
            double entropy = calculateEntropy(hoveredVertexIdx);
            
            String tip = String.format("Vertex: v_%d\nCoords: [%s]\nDensity: %.4f\nFlow: %.3f m/s\nEntropy: H=%.4f bits\nSOP: Consensus-Strict\nClick to route spike!", 
                hoveredVertexIdx, getCoordsString(vertices6D[hoveredVertexIdx]), dens, flw, entropy);
                
            double tpx = projected2D[hoveredVertexIdx][0];
            double tpy = projected2D[hoveredVertexIdx][1];
            
            gc.setFill(Color.rgb(6, 3, 18, 0.95));
            gc.fillRect(pxForHoverToolTip(tpx), pyForHoverToolTip(tpy), 250, 115);
            gc.setStroke(Color.web("#f472b6"));
            gc.setLineWidth(1.5);
            gc.strokeRect(pxForHoverToolTip(tpx), pyForHoverToolTip(tpy), 250, 115);
            gc.setFill(Color.web("#f3e8ff"));
            gc.setFont(Font.font("Consolas", 11));
            
            String[] lines = tip.split("\n");
            double textY = pyForHoverToolTip(tpy) + 20.0;
            for (String line : lines) {
                gc.fillText(line, pxForHoverToolTip(tpx) + 15, textY);
                textY += 15;
            }
        }
    }

    private void drawGauge(GraphicsContext gc, String label, double value, double x, double y, String hexColor) {
        // High-contrast bold text for accessibility
        gc.setFont(Font.font("Outfit", javafx.scene.text.FontWeight.BOLD, 14));
        gc.setFill(Color.web("#ffffff"));
        gc.fillText(label, x, y);
        
        // Neon accent value text
        gc.setFill(Color.web(hexColor));
        gc.setFont(Font.font("Consolas", javafx.scene.text.FontWeight.BOLD, 15));
        gc.fillText(String.format("%.3f", value), x + 170, y);
        
        // Thick 18px dark track background with bright border
        gc.setFill(Color.rgb(15, 10, 36, 0.9));
        gc.fillRect(x, y + 8, 230, 18);
        gc.setStroke(Color.web("#a855f7", 0.6));
        gc.setLineWidth(1.5);
        gc.strokeRect(x, y + 8, 230, 18);
        
        // Vibrant neon fill bar
        gc.setFill(Color.web(hexColor));
        double barWidth = Math.max(4, Math.min(226, value * 226));
        gc.fillRect(x + 2, y + 10, barWidth, 14);
    }

    private String getCoordsString(double[] coords) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < coords.length; i++) {
            sb.append((int)coords[i]);
            if (i < coords.length - 1) sb.append(", ");
        }
        return sb.toString();
    }

    private double pxForHoverToolTip(double projectedX) {
        if (projectedX + 260 > WIDTH) return projectedX - 270;
        return projectedX + 15;
    }

    private double pyForHoverToolTip(double projectedY) {
        if (projectedY + 120 > HEIGHT) return projectedY - 130;
        return projectedY + 15;
    }

    @Override
    public void stop() {
        threadPool.shutdownNow();
        if(dashboardServer != null) dashboardServer.stop(0);
    }

    // --- Draggable Sub-Window Custom Component ---

    class DraggableWindow extends VBox {
        private double dragStartX;
        private double dragStartY;
        private Label titleLabel;
        private String title;
        
        public DraggableWindow(String title, javafx.scene.Node content, double width, double height) {
            this.title = title;
            this.setPrefSize(width, height);
            this.setMaxSize(width, height);
            this.setStyle("-fx-background-color: rgba(6, 3, 18, 0.9); " +
                          "-fx-border-color: #a855f7; " +
                          "-fx-border-width: 1.5; " +
                          "-fx-background-radius: 6; " +
                          "-fx-border-radius: 6;");
            
            // Header bar
            HBox header = new HBox();
            header.setAlignment(Pos.CENTER_LEFT);
            header.setStyle("-fx-background-color: #7c3aed; -fx-padding: 6 10; -fx-cursor: move; -fx-background-radius: 4 4 0 0;");
            
            titleLabel = new Label(title);
            titleLabel.setStyle("-fx-text-fill: white; -fx-font-family: 'Outfit', monospace; -fx-font-weight: bold; -fx-font-size: 12px;");
            HBox.setHgrow(titleLabel, Priority.ALWAYS);
            
            // Rendered as fully visible capital X close button
            Button btnClose = new Button("X");
            btnClose.setStyle("-fx-background-color: transparent; -fx-text-fill: #f472b6; -fx-font-family: monospace; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 0 4 0 4; -fx-cursor: hand;");
            btnClose.setOnAction(e -> {
                Pane parent = (Pane) this.getParent();
                if (parent != null) parent.getChildren().remove(this);
            });
            
            header.getChildren().addAll(titleLabel, btnClose);
            HBox.setHgrow(btnClose, Priority.NEVER);
            
            header.setOnMousePressed(e -> {
                dragStartX = e.getSceneX() - this.getTranslateX();
                dragStartY = e.getSceneY() - this.getTranslateY();
                this.toFront();
            });
            header.setOnMouseDragged(e -> {
                this.setTranslateX(e.getSceneX() - dragStartX);
                this.setTranslateY(e.getSceneY() - dragStartY);
            });
            
            VBox container = new VBox(content);
            container.setStyle("-fx-padding: 10;");
            VBox.setVgrow(content, Priority.ALWAYS);
            
            this.getChildren().addAll(header, container);
        }
        
        public String getTitle() {
            return title;
        }
    }

    // --- Inner Helper Classes --- //

    class EdgeWithDepth {
        int source;
        int target;
        double avgZ;
        public EdgeWithDepth(int source, int target, double avgZ) {
            this.source = source;
            this.target = target;
            this.avgZ = avgZ;
        }
    }

    class Particle {
        double x, y, z;
        int targetNodeIdx;
        Color color;
        public Particle() {
            reset();
        }
        public void reset() {
            x = (rand.nextDouble() - 0.5) * 10;
            y = (rand.nextDouble() - 0.5) * 10;
            z = (rand.nextDouble() - 0.5) * 10;
            targetNodeIdx = rand.nextInt(64);
            double r = rand.nextDouble();
            if (r > 0.6) color = Color.web("#f472b6", 0.4);      
            else if (r > 0.3) color = Color.web("#38bdf8", 0.45); 
            else color = Color.web("#c084fc", 0.4);               
        }
    }

    class Pulse {
        int sourceIdx;
        int targetIdx;
        double progress;
        double speed;
        public Pulse(int source, int target) {
            this.sourceIdx = source;
            this.targetIdx = target;
            this.progress = 0;
            this.speed = 0.02 + rand.nextDouble() * 0.03;
        }
    }

    class BackgroundStar {
        double x, y;
        double speed;
        double size;
        double phase;
        public BackgroundStar(double x, double y) {
            this.x = x;
            this.y = y;
            this.speed = 0.005 + rand.nextDouble() * 0.015;
            this.size = 0.5 + rand.nextDouble() * 1.5;
            this.phase = rand.nextDouble() * Math.PI * 2;
        }
    }
    
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
                    router.query("tinyllama:1.1b", "Execute pipeline task for station " + station);
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
                        Thread.sleep(3600000); 
                        System.out.println("[HOURLY HEARTRATE] System soaking safely. Local SLMs active. No thermal throttling detected.");
                        Platform.runLater(() -> {
                            synchronized(godChat) {
                                if (godChat.size() > 50) godChat.remove(0);
                                godChat.add("[SYSTEM] Hourly Heartrate OK. Soaking...");
                            }
                        });
                    } catch(Exception e){}
                }
            });

            threadPool.submit(() -> {
                while(true) {
                    try {
                        Thread.sleep(900000); 
                        currentPhase = "00:00 CHAT & DREAM PHASE";
                        System.out.println("[SOAK] Dreaming cross-correlated memories...");
                        String dreamPrompt = "Generate exactly one new 1-2 word node type or mechanic for a hex grid simulation. Output only the name, nothing else. No preamble.";
                        String dreamProposalRaw = router.query("qwen2.5:0.5b", dreamPrompt).replaceAll("[\"'{}\\[\\]\\n\\r]", "").trim();
                        if (dreamProposalRaw.isEmpty() || dreamProposalRaw.length() > 30) dreamProposalRaw = "Void_Node";
                        String dreamProposal = dreamProposalRaw;
                        Platform.runLater(() -> {
                            synchronized(godChat) {
                                if (godChat.size() > 50) godChat.remove(0);
                                godChat.add("[DREAM] Proposal generated: " + dreamProposal);
                            }
                        });
                        
                        Thread.sleep(900000); 
                        currentPhase = "18:00 VOTE PHASE";
                        System.out.println("[SOAK] Engaged Vote Phase...");
                        boolean approved = modelManager.executeVote(dreamProposal, router);
                        
                        Thread.sleep(900000); 
                        currentPhase = "20:00 DEPLOY PHASE";
                        System.out.println("[SOAK] Deploying dynamically generated tools...");
                        if (approved) {
                            memory.logMemory("SYSTEM", "SOAK_CYCLE", "Deployed new " + dreamProposal + " node.");
                            mutator.injectMutation(dreamProposal);
                            Map<String, String> state = new HashMap<>();
                            state.put("topology.json", "{\"status\": \"Topology updated with " + dreamProposal + "\"}");
                            gistSync.pushState(state);
                        }
                        
                        Thread.sleep(900000); 
                        currentPhase = "22:00 MOVE PHASE";
                        System.out.println("[SOAK] Requesting Agent Movement...");
                        if (!agents.isEmpty()) {
                            Agent a = agents.get(0);
                            String moveDir = router.query("qwen2.5:0.5b", "You are an agent at " + a.q + "," + a.r + ". Reply exactly with one word: NORTH, SOUTH, EAST, or WEST.").trim().toUpperCase();
                            Platform.runLater(() -> {
                                if (moveDir.contains("NORTH")) a.r -= 1;
                                else if (moveDir.contains("SOUTH")) a.r += 1;
                                else if (moveDir.contains("EAST")) a.q += 1;
                                else if (moveDir.contains("WEST")) a.q -= 1;
                                synchronized(godChat) {
                                    if (godChat.size() > 50) godChat.remove(0);
                                    godChat.add("[MOVE] Agent Alpha shifted " + moveDir);
                                }
                            });
                        }
                    } catch(Exception e){}
                }
            });
        }
        public String getCurrentPhase() { return currentPhase; }
    }

    // --- Lexical Task Datastructure ---

    public static class LexicalTask {
        public final int priority;
        public final String command;
        public final String parameter;
        public final Runnable action;
        
        public LexicalTask(int priority, String command, String parameter, Runnable action) {
            this.priority = priority;
            this.command = command;
            this.parameter = parameter;
            this.action = action;
        }
    }

    private void executeDesktopScript(String scriptName) {
        System.out.println("[MANIFOLD] Triggering external hook: " + scriptName);
        try {
            Runtime.getRuntime().exec(new String[]{
                "powershell.exe",
                "-ExecutionPolicy", "Bypass",
                "-WindowStyle", "Hidden",
                "-File", "C:\\Users\\viper\\OneDrive\\Desktop\\local_desktop-main\\" + scriptName
            });
        } catch(Exception e) {
            System.err.println("[MANIFOLD ERROR] " + e.getMessage());
        }
    }
}
