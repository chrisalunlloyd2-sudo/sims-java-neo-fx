package com.viper.notes;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class ViperLabSuiteServer {
    private static final String SDK_VERSION = "0.4.1-training-lab";
    private static final int PORT = 18181;
    private static final Path ROOT = Paths.get("C:\\Users\\viper\\VIPER_JAVA_RISC");
    private static final Path SUITE_ROOT = ROOT.resolve("java_notes_suite");
    private static final Path DATA_DIR = SUITE_ROOT.resolve("data");
    private static final Path SETTINGS_FILE = DATA_DIR.resolve("sdk_settings.json");
    private static final Path TEST_LOG = DATA_DIR.resolve("system_tests.jsonl");
    private static final Path AB_LOG = DATA_DIR.resolve("ab_tests.jsonl");
    private static final Path TRAINING_LOG = DATA_DIR.resolve("training_runs.jsonl");
    private static final Path TRAINING_EPOCH_LOG = DATA_DIR.resolve("recursive_training_epochs.jsonl");
    private static final Path LOIHI_LOG = DATA_DIR.resolve("loihi_experiments.jsonl");
    private static final Path BENCHMARK_LOG = DATA_DIR.resolve("benchmark_snapshots.jsonl");
    private static final Path ASCII_EPOCH_LOG = DATA_DIR.resolve("ascii_epoch_queue.jsonl");
    private static final Path EPOCH_UPGRADE_LOG = DATA_DIR.resolve("epoch_upgrade_proofs.jsonl");
    private static final Path EPOCH_IMPLEMENT_LOG = DATA_DIR.resolve("epoch_implementation_queue.jsonl");
    private static final Path ALGEBRAIC_FLOW_LOG = DATA_DIR.resolve("algebraic_pattern_flows.jsonl");
    private static final Path PERSISTENCE_LOG = DATA_DIR.resolve("persistence_events.jsonl");
    private static final Path WEB_SOURCE_LOG = DATA_DIR.resolve("web_source_manifest.jsonl");
    private static final Path DARWIN_TEST_PROGRAM_LOG = DATA_DIR.resolve("darwin_test_programs.jsonl");
    private static final Path DARWIN_ALGORITHM_REGISTRY_LOG = DATA_DIR.resolve("darwin_algorithm_registry.jsonl");
    private static final Path DARWIN_GENERATION_LOG = DATA_DIR.resolve("darwin_algorithm_generations.jsonl");
    private static final Path DARWIN_WINNER_LOG = DATA_DIR.resolve("darwin_algorithm_winners.jsonl");
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    public static void main(String[] args) throws Exception {
        ensurePersistence();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", PORT), 0);
        server.createContext("/", new PageHandler());
        server.createContext("/health", new HealthHandler());
        server.createContext("/api/state", new StateHandler());
        server.createContext("/api/settings", new SettingsHandler());
        server.createContext("/api/run-test", new RunTestHandler());
        server.createContext("/api/ab-test", new AppendJsonHandler(AB_LOG, "ab_test"));
        server.createContext("/api/training", new TrainingHandler());
        server.createContext("/api/recursive-training", new RecursiveTrainingHandler());
        server.createContext("/api/loihi-experiment", new AppendJsonHandler(LOIHI_LOG, "loihi_experiment"));
        server.createContext("/api/benchmarks", new BenchmarksHandler());
        server.createContext("/api/benchmark-snapshot", new BenchmarkSnapshotHandler());
        server.createContext("/api/ascii-epochs", new AsciiEpochHandler());
        server.createContext("/api/epoch-upgrade-proof", new EpochUpgradeProofHandler());
        server.createContext("/api/epoch-implement", new EpochImplementHandler());
        server.createContext("/api/algebraic-flow", new AlgebraicFlowHandler());
        server.createContext("/api/darwin-lab", new DarwinLabHandler());
        server.createContext("/api/library-growth", new LibraryGrowthHandler());
        server.createContext("/api/log-tail", new LogTailHandler());
        server.createContext("/api/design", new DesignHandler());
        server.setExecutor(null);
        server.start();
        appendJsonLine(PERSISTENCE_LOG, mapOf(
                "event", "lab_suite_start",
                "port", PORT,
                "root", ROOT.toString()
        ));
        System.out.println("VIPER JAVA SDK ACTIVE: http://127.0.0.1:" + PORT);
    }

    private static void ensurePersistence() throws IOException {
        Files.createDirectories(DATA_DIR);
        if (!Files.exists(SETTINGS_FILE)) {
            String defaults = "{\n"
                    + "  \"mode\": \"planning\",\n"
                    + "  \"chatReplyTokens\": 512,\n"
                    + "  \"planningReplyTokens\": 1024,\n"
                    + "  \"buildReplyTokens\": 1536,\n"
                    + "  \"karooProposalOnly\": true,\n"
                    + "  \"loihiMode\": \"simulated_spike_topology_sidecar\",\n"
                    + "  \"heartbeatSeconds\": 300,\n"
                    + "  \"autoAdvanceSuccess\": 99.99,\n"
                    + "  \"autoAdvanceSpeedGain\": 10,\n"
                    + "  \"autoAdvanceResourceDrop\": 10,\n"
                    + "  \"notesDestination\": \"viper_laptop_notes\"\n"
                    + "}\n";
            Files.writeString(SETTINGS_FILE, defaults, StandardCharsets.UTF_8);
        }
        touch(TEST_LOG);
        touch(AB_LOG);
        touch(TRAINING_LOG);
        touch(TRAINING_EPOCH_LOG);
        touch(LOIHI_LOG);
        touch(BENCHMARK_LOG);
        touch(ASCII_EPOCH_LOG);
        touch(EPOCH_UPGRADE_LOG);
        touch(EPOCH_IMPLEMENT_LOG);
        touch(ALGEBRAIC_FLOW_LOG);
        touch(PERSISTENCE_LOG);
        touch(WEB_SOURCE_LOG);
        touch(DARWIN_TEST_PROGRAM_LOG);
        touch(DARWIN_ALGORITHM_REGISTRY_LOG);
        touch(DARWIN_GENERATION_LOG);
        touch(DARWIN_WINNER_LOG);
        ensureWebSourceManifest();
        ensureDarwinTestPrograms();
        ensureDarwinAlgorithmRegistry();
    }

    private static void touch(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.writeString(path, "", StandardCharsets.UTF_8);
        }
    }

    private static void ensureWebSourceManifest() throws IOException {
        if (countLines(WEB_SOURCE_LOG) > 0) {
            return;
        }
        List<Map<String, Object>> defaults = List.of(
                mapOf("sourceId", "github_external_git_crawl", "kind", "code_host", "url", "https://github.com", "status", "planned_primary", "priority", "primary", "crawlMode", "external_git_crawl", "scope", "repositories,tags,releases,trees", "why", "broad open-source code, topology, release lineage, and cross-language template mining"),
                mapOf("sourceId", "huggingface_models", "kind", "model_hub", "url", "https://huggingface.co", "status", "candidate", "priority", "secondary", "crawlMode", "model_and_dataset_scan", "scope", "models,datasets,papers", "why", "open model and dataset ecosystem for distillation later"),
                mapOf("sourceId", "pypi_packages", "kind", "package_registry", "url", "https://pypi.org", "status", "candidate", "priority", "secondary", "crawlMode", "package_registry_scan", "scope", "packages,sdists,wheels", "why", "Python package metadata and source distributions"),
                mapOf("sourceId", "npm_packages", "kind", "package_registry", "url", "https://www.npmjs.com", "status", "candidate", "priority", "secondary", "crawlMode", "package_registry_scan", "scope", "packages,versions,dist-tags", "why", "JavaScript package ecosystems and templates"),
                mapOf("sourceId", "maven_central", "kind", "package_registry", "url", "https://search.maven.org", "status", "candidate", "priority", "secondary", "crawlMode", "package_registry_scan", "scope", "artifacts,versions,poms", "why", "Java ecosystem metadata and library signatures"),
                mapOf("sourceId", "crates_registry", "kind", "package_registry", "url", "https://crates.io", "status", "candidate", "priority", "secondary", "crawlMode", "package_registry_scan", "scope", "crates,versions,docs", "why", "Rust package metadata and cross-language logic patterns")
        );
        for (Map<String, Object> entry : defaults) {
            appendJsonLine(WEB_SOURCE_LOG, entry);
        }
    }

    private static void ensureDarwinTestPrograms() throws IOException {
        appendMissingSeedRows(DARWIN_TEST_PROGRAM_LOG, List.of(
                mapOf("programId", "PY_AUTH_ROUTE", "language", "python", "topologyKind", "service_api_page", "pageCount", 3, "syntaxPressure", 0.68, "dependencyPressure", 0.54, "topologyPressure", 0.78, "performativePressure", 0.62, "routePressure", 0.82, "recursionPressure", 0.32, "repairRisk", 0.36, "latencyPressure", 0.42, "bruteForceFriendliness", 0.74, "acceptanceTarget", "parse_compile_route_fit"),
                mapOf("programId", "JS_DASH_WIDGET", "language", "javascript", "topologyKind", "ui_component_page", "pageCount", 2, "syntaxPressure", 0.52, "dependencyPressure", 0.48, "topologyPressure", 0.66, "performativePressure", 0.44, "routePressure", 0.38, "recursionPressure", 0.26, "repairRisk", 0.28, "latencyPressure", 0.34, "bruteForceFriendliness", 0.86, "acceptanceTarget", "parse_render_contract"),
                mapOf("programId", "JAVA_ORDER_SERVICE", "language", "java", "topologyKind", "service_module_page", "pageCount", 4, "syntaxPressure", 0.72, "dependencyPressure", 0.69, "topologyPressure", 0.81, "performativePressure", 0.58, "routePressure", 0.71, "recursionPressure", 0.38, "repairRisk", 0.41, "latencyPressure", 0.49, "bruteForceFriendliness", 0.63, "acceptanceTarget", "compile_dependency_route_fit"),
                mapOf("programId", "RUST_CLI_STAGE", "language", "rust", "topologyKind", "cli_pipeline_page", "pageCount", 3, "syntaxPressure", 0.64, "dependencyPressure", 0.57, "topologyPressure", 0.73, "performativePressure", 0.47, "routePressure", 0.35, "recursionPressure", 0.44, "repairRisk", 0.33, "latencyPressure", 0.29, "bruteForceFriendliness", 0.69, "acceptanceTarget", "cargo_parse_pipeline_fit"),
                mapOf("programId", "TS_EDGE_GATEWAY", "language", "typescript", "topologyKind", "edge_gateway_page", "pageCount", 5, "syntaxPressure", 0.59, "dependencyPressure", 0.63, "topologyPressure", 0.84, "performativePressure", 0.73, "routePressure", 0.89, "recursionPressure", 0.31, "repairRisk", 0.35, "latencyPressure", 0.58, "bruteForceFriendliness", 0.66, "acceptanceTarget", "typecheck_route_contract_fit"),
                mapOf("programId", "CSHARP_QUEUE_WORKER", "language", "csharp", "topologyKind", "background_worker_page", "pageCount", 4, "syntaxPressure", 0.61, "dependencyPressure", 0.66, "topologyPressure", 0.76, "performativePressure", 0.69, "routePressure", 0.42, "recursionPressure", 0.28, "repairRisk", 0.46, "latencyPressure", 0.53, "bruteForceFriendliness", 0.57, "acceptanceTarget", "compile_queue_contract_fit"),
                mapOf("programId", "GO_EVENT_ROUTER", "language", "go", "topologyKind", "event_router_page", "pageCount", 3, "syntaxPressure", 0.57, "dependencyPressure", 0.45, "topologyPressure", 0.71, "performativePressure", 0.64, "routePressure", 0.83, "recursionPressure", 0.24, "repairRisk", 0.29, "latencyPressure", 0.31, "bruteForceFriendliness", 0.79, "acceptanceTarget", "build_route_dispatch_fit"),
                mapOf("programId", "CPP_PLUGIN_HOST", "language", "cpp", "topologyKind", "plugin_host_page", "pageCount", 5, "syntaxPressure", 0.77, "dependencyPressure", 0.74, "topologyPressure", 0.79, "performativePressure", 0.55, "routePressure", 0.33, "recursionPressure", 0.41, "repairRisk", 0.52, "latencyPressure", 0.37, "bruteForceFriendliness", 0.49, "acceptanceTarget", "compile_link_interface_fit")
        ), "programId");
    }

    private static void ensureDarwinAlgorithmRegistry() throws IOException {
        appendMissingSeedRows(DARWIN_ALGORITHM_REGISTRY_LOG, darwinDefaultAlgorithmSeeds(), "algorithmId");
    }

    private static void appendMissingSeedRows(Path log, List<Map<String, Object>> defaults, String keyField) throws IOException {
        Set<String> existing = new LinkedHashSet<>();
        for (String line : readJsonLines(log, 2000)) {
            String key = extractJsonString(line, keyField, "");
            if (!key.isBlank()) {
                existing.add(key);
            }
        }
        for (Map<String, Object> row : defaults) {
            String key = String.valueOf(row.getOrDefault(keyField, ""));
            if (!key.isBlank() && existing.add(key)) {
                appendJsonLine(log, row);
            }
        }
    }

    private static class PageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                send(exchange, 405, jsonError("method_not_allowed"), "application/json");
                return;
            }
            send(exchange, 200, html(), "text/html; charset=utf-8");
        }
    }

    private static class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            send(exchange, 200, jsonObject(mapOf(
                    "status", "ok",
                    "suite", "viper_java_sdk",
                    "version", SDK_VERSION,
                    "port", PORT,
                    "persistent", true,
                    "timestamp", Instant.now().toString()
            )), "application/json");
        }
    }

    private static class StateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, Object> state = new LinkedHashMap<>();
            state.put("status", "ok");
            state.put("version", SDK_VERSION);
            state.put("timestamp", Instant.now().toString());
            state.put("root", ROOT.toString());
            state.put("dataDir", DATA_DIR.toString());
            state.put("settings", readTextSafe(SETTINGS_FILE, "{}"));
            state.put("counts", mapOf(
                    "systemTests", countLines(TEST_LOG),
                    "abTests", countLines(AB_LOG),
                    "trainingRuns", countLines(TRAINING_LOG),
                    "recursiveTrainingEpochs", countLines(TRAINING_EPOCH_LOG),
                    "loihiExperiments", countLines(LOIHI_LOG),
                    "benchmarkSnapshots", countLines(BENCHMARK_LOG),
                    "asciiEpochs", countLines(ASCII_EPOCH_LOG),
                    "epochUpgradeProofs", countLines(EPOCH_UPGRADE_LOG),
                    "epochImplementations", countLines(EPOCH_IMPLEMENT_LOG),
                    "algebraicFlows", countLines(ALGEBRAIC_FLOW_LOG),
                    "persistenceEvents", countLines(PERSISTENCE_LOG),
                    "darwinTestPrograms", countLines(DARWIN_TEST_PROGRAM_LOG),
                    "darwinAlgorithms", countLines(DARWIN_ALGORITHM_REGISTRY_LOG),
                    "darwinGenerations", countLines(DARWIN_GENERATION_LOG),
                    "darwinWinners", countLines(DARWIN_WINNER_LOG)
            ));
            state.put("services", serviceHealth());
            state.put("logs", mapOf(
                    "system", fileInfo(ROOT.resolve("system_log.txt")),
                    "shipper", fileInfo(ROOT.resolve("logic_blockchain_shipper.log")),
                    "topology", fileInfo(ROOT.resolve("topology_sidecar_loop.log")),
                    "houseStdout", fileInfo(ROOT.resolve("house_inference_stdout.log")),
                    "houseStderr", fileInfo(ROOT.resolve("house_inference_stderr.log"))
            ));
            send(exchange, 200, jsonObject(state), "application/json");
        }
    }

    private static class SettingsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                send(exchange, 200, readTextSafe(SETTINGS_FILE, "{}"), "application/json");
                return;
            }
            if (!"POST".equals(exchange.getRequestMethod())) {
                send(exchange, 405, jsonError("method_not_allowed"), "application/json");
                return;
            }
            String body = readBody(exchange);
            if (body.isBlank()) {
                send(exchange, 400, jsonError("empty_settings_body"), "application/json");
                return;
            }
            Files.writeString(SETTINGS_FILE, body.strip() + "\n", StandardCharsets.UTF_8);
            appendJsonLine(PERSISTENCE_LOG, mapOf("event", "settings_update", "sha256", sha256(body)));
            send(exchange, 200, jsonObject(mapOf("status", "saved", "sha256", sha256(body))), "application/json");
        }
    }

    private static class LibraryGrowthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                send(exchange, 405, jsonError("method_not_allowed"), "application/json");
                return;
            }
            send(exchange, 200, jsonObject(libraryGrowthSummary()), "application/json");
        }
    }

    private static class DarwinLabHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
                int limit = Math.max(1, Math.min(parseInt(query.getOrDefault("limit", "8"), 8), 40));
                send(exchange, 200, jsonObject(mapOf(
                        "status", "ok",
                        "version", SDK_VERSION,
                        "programs", darwinPrograms(12),
                        "algorithms", darwinAlgorithms(12),
                        "recentGenerations", readJsonFragments(DARWIN_GENERATION_LOG, limit),
                        "recentWinners", readJsonFragments(DARWIN_WINNER_LOG, limit),
                        "policy", "bounded darwin lab; brute force and matrix first, optional agent lanes later"
                )), "application/json");
                return;
            }
            if (!"POST".equals(exchange.getRequestMethod())) {
                send(exchange, 405, jsonError("method_not_allowed"), "application/json");
                return;
            }
            String body = readBody(exchange);
            Map<String, Object> run = buildDarwinLabRun(body);
            Map<String, Object> finalWinner = nestedMap(run, "finalWinner");
            if (!finalWinner.isEmpty()) {
                promoteDarwinWinnerToRegistry(finalWinner);
            }
            run.put("sha256", sha256(jsonObject(run)));
            appendJsonLine(DARWIN_WINNER_LOG, mapOf(
                    "kind", "darwin_winner",
                    "timestamp", Instant.now().toString(),
                    "runId", run.get("runId"),
                    "summary", run.get("summary"),
                    "finalWinner", run.get("finalWinner"),
                    "sha256", run.get("sha256")
            ));
            appendJsonLine(PERSISTENCE_LOG, mapOf(
                    "event", "darwin_lab_run",
                    "runId", run.get("runId"),
                    "sha256", run.get("sha256"),
                    "winner", nestedMap(run, "finalWinner").get("algorithmId")
            ));
            send(exchange, 200, jsonObject(run), "application/json");
        }
    }

    private static class RunTestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                send(exchange, 405, jsonError("method_not_allowed"), "application/json");
                return;
            }
            long start = System.currentTimeMillis();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("kind", "system_test");
            result.put("timestamp", Instant.now().toString());
            result.put("request", readBody(exchange));
            result.put("services", serviceHealth());
            result.put("durationMs", System.currentTimeMillis() - start);
            result.put("policy", "one variable per test; end-to-end proof preferred");
            result.put("sha256", sha256(jsonObject(result)));
            appendJsonLine(TEST_LOG, result);
            send(exchange, 200, jsonObject(result), "application/json");
        }
    }

    private static class AppendJsonHandler implements HttpHandler {
        private final Path logPath;
        private final String kind;

        AppendJsonHandler(Path logPath, String kind) {
            this.logPath = logPath;
            this.kind = kind;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                send(exchange, 405, jsonError("method_not_allowed"), "application/json");
                return;
            }
            String body = readBody(exchange);
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("kind", kind);
            event.put("timestamp", Instant.now().toString());
            event.put("body", body);
            event.put("sha256", sha256(kind + body + Instant.now()));
            if ("loihi_experiment".equals(kind)) {
                event.put("contract", "Loihi/Lava is a future sidecar: NLP -> topological codes -> spike topology -> code/logic readback.");
                event.put("safety", "simulation/proposal first; no claim of thinking kernel.");
            }
            appendJsonLine(logPath, event);
            appendJsonLine(PERSISTENCE_LOG, mapOf("event", kind + "_append", "sha256", event.get("sha256")));
            send(exchange, 200, jsonObject(mapOf("status", "logged", "kind", kind, "sha256", event.get("sha256"))), "application/json");
        }
    }

    private static class BenchmarksHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                send(exchange, 405, jsonError("method_not_allowed"), "application/json");
                return;
            }
            Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
            int limit = Math.max(1, Math.min(parseInt(query.getOrDefault("limit", "40"), 40), 200));
            send(exchange, 200, jsonObject(mapOf(
                    "status", "ok",
                    "timestamp", Instant.now().toString(),
                    "current", currentBenchmark("read_only"),
                    "history", readJsonLines(BENCHMARK_LOG, limit),
                    "recursiveTrainingStatus", "active eval training: probes services, asks bridge prefetch, logs candidate/proof; no model-weight mutation submitted here"
            )), "application/json");
        }
    }

    private static class TrainingHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
                int limit = Math.max(1, Math.min(parseInt(query.getOrDefault("limit", "20"), 20), 100));
                send(exchange, 200, jsonObject(mapOf(
                        "status", "ok",
                        "version", SDK_VERSION,
                        "mode", "active_eval_training",
                        "runs", readJsonFragments(TRAINING_LOG, limit),
                        "policy", "runs real service probes and bridge prefetch; model weights remain untouched"
                )), "application/json");
                return;
            }
            if (!"POST".equals(exchange.getRequestMethod())) {
                send(exchange, 405, jsonError("method_not_allowed"), "application/json");
                return;
            }

            long started = System.currentTimeMillis();
            String body = readBody(exchange);
            String dataset = extractJsonString(body, "dataset", "successful_code_and_liked_logic");
            String route = extractJsonString(body, "route", "proposal_only_lens_improvement");
            String changedVariable = extractJsonString(body, "changedVariable",
                    extractJsonString(body, "variable", "retrieval_lens_instruction_card"));
            String objective = extractJsonString(body, "objective", "improve chooser/retrieval usefulness without changing the locked GUI");

            Map<String, Object> before = currentBenchmark("training_before");
            String prompt = trainingPrompt(dataset, route, changedVariable, objective);
            String prefetch = fetchText("http://127.0.0.1:8080/api/predictive/prefetch?q=" + urlEncode(prompt), 8);
            String bridgeBenchmarks = fetchText("http://127.0.0.1:8080/api/benchmarks?limit=5", 8);
            Map<String, Object> afterProbe = currentBenchmark("training_after_probe");
            Map<String, Object> evaluation = evaluateTrainingRun(before, afterProbe, prefetch, bridgeBenchmarks);

            Map<String, Object> run = new LinkedHashMap<>();
            run.put("kind", "training_run");
            run.put("status", "active_eval_logged");
            run.put("version", SDK_VERSION);
            run.put("timestamp", Instant.now().toString());
            run.put("durationMs", System.currentTimeMillis() - started);
            run.put("request", body);
            run.put("dataset", dataset);
            run.put("route", route);
            run.put("changedVariable", changedVariable);
            run.put("objective", objective);
            run.put("phases", List.of(
                    "capture_baseline_benchmark",
                    "probe_bridge_predictive_prefetch",
                    "read_recent_bridge_benchmarks",
                    "score_candidate_against_promotion_gate",
                    "write_training_and_epoch_logs"
            ));
            run.put("candidate", mapOf(
                    "lensDelta", "Prefer real retrieved evidence, concise Qwen chooser lens, and explicit safety gate.",
                    "oneChangedVariable", changedVariable,
                    "trainingPrompt", prompt,
                    "expectedEffect", "fewer thin replies, less raw metadata in lens, clearer task routing"
            ));
            run.put("prefetchProbe", compactStatus(prefetch));
            run.put("bridgeBenchmarkProbe", compactStatus(bridgeBenchmarks));
            run.put("benchmarkBefore", before);
            run.put("benchmarkAfterProbe", afterProbe);
            run.put("evaluation", evaluation);
            run.put("doesMutateModelWeights", false);
            run.put("doesSubmitRecursiveTraining", true);
            run.put("trainingMeaning", "Submits a real Java-lab eval epoch: data is probed, scored, hashed, and logged for the next chooser/Karoo cycle.");
            run.put("promotionGate", "success >= 99.99 and (+10% speed or -10% resources); otherwise record only");
            run.put("promotionDecision", Boolean.TRUE.equals(evaluation.get("promotionEligible")) ? "eligible_for_user_review" : "record_only_more_evidence_needed");
            run.put("sha256", sha256(jsonObject(run)));

            appendJsonLine(TRAINING_LOG, run);

            Map<String, Object> epoch = new LinkedHashMap<>();
            epoch.put("kind", "training_backed_recursive_epoch");
            epoch.put("status", "active_eval_logged");
            epoch.put("timestamp", Instant.now().toString());
            epoch.put("trainingRunSha256", run.get("sha256"));
            epoch.put("changedVariable", changedVariable);
            epoch.put("datasetSlice", dataset);
            epoch.put("scientificMethod", "one changed variable; compare service proof; do not self-apply");
            epoch.put("evaluation", evaluation);
            epoch.put("sha256", sha256(jsonObject(epoch)));
            appendJsonLine(TRAINING_EPOCH_LOG, epoch);

            Map<String, Object> benchmark = currentBenchmark("training_recorded");
            benchmark.put("trainingRunSha256", run.get("sha256"));
            benchmark.put("sha256", sha256(jsonObject(benchmark)));
            appendJsonLine(BENCHMARK_LOG, benchmark);
            appendJsonLine(PERSISTENCE_LOG, mapOf(
                    "event", "active_training_run",
                    "trainingRunSha256", run.get("sha256"),
                    "recursiveEpochSha256", epoch.get("sha256"),
                    "benchmarkSha256", benchmark.get("sha256")
            ));

            send(exchange, 200, jsonObject(mapOf(
                    "status", "trained_eval_logged",
                    "version", SDK_VERSION,
                    "trainingRun", run,
                    "recursiveEpoch", epoch,
                    "benchmark", benchmark
            )), "application/json");
        }
    }

    private static class BenchmarkSnapshotHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                send(exchange, 405, jsonError("method_not_allowed"), "application/json");
                return;
            }
            Map<String, Object> snapshot = currentBenchmark("captured");
            snapshot.put("request", readBody(exchange));
            snapshot.put("sha256", sha256(jsonObject(snapshot)));
            appendJsonLine(BENCHMARK_LOG, snapshot);
            appendJsonLine(PERSISTENCE_LOG, mapOf("event", "benchmark_snapshot", "sha256", snapshot.get("sha256")));
            send(exchange, 200, jsonObject(snapshot), "application/json");
        }
    }

    private static class RecursiveTrainingHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                send(exchange, 405, jsonError("method_not_allowed"), "application/json");
                return;
            }
            String body = readBody(exchange);
            Map<String, Object> epoch = new LinkedHashMap<>();
            epoch.put("kind", "recursive_training_epoch");
            epoch.put("status", "proposal_eval_only");
            epoch.put("timestamp", Instant.now().toString());
            epoch.put("body", body);
            epoch.put("doesSubmitRecursiveTraining", false);
            epoch.put("guard", "This records a recursive training proposal and benchmark context only. It does not mutate model weights.");
            epoch.put("promotionGate", "success >= 99.99 and (speed_gain >= 10 or resource_drop >= 10)");
            epoch.put("scientificMethod", "one changed variable per epoch; compare before/after; end-to-end proof required");
            epoch.put("benchmarkBefore", currentBenchmark("epoch_before"));
            epoch.put("sha256", sha256(jsonObject(epoch)));
            appendJsonLine(TRAINING_EPOCH_LOG, epoch);
            appendJsonLine(PERSISTENCE_LOG, mapOf("event", "recursive_training_epoch_append", "sha256", epoch.get("sha256")));
            send(exchange, 200, jsonObject(epoch), "application/json");
        }
    }

    private static class AsciiEpochHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
                int limit = Math.max(1, Math.min(parseInt(query.getOrDefault("limit", "20"), 20), 100));
                send(exchange, 200, jsonObject(mapOf(
                        "status", "ok",
                        "version", SDK_VERSION,
                        "queue", readJsonLines(ASCII_EPOCH_LOG, limit),
                        "policy", "always keep new ASCII epochs waiting; external judges weigh only; local proof promotes"
                )), "application/json");
                return;
            }
            if (!"POST".equals(exchange.getRequestMethod())) {
                send(exchange, 405, jsonError("method_not_allowed"), "application/json");
                return;
            }
            String body = readBody(exchange);
            Map<String, Object> epoch = new LinkedHashMap<>();
            epoch.put("kind", "ascii_epoch_proposal");
            epoch.put("version", SDK_VERSION);
            epoch.put("timestamp", Instant.now().toString());
            epoch.put("body", body);
            epoch.put("subsystems", List.of("chooser", "db_retrieval", "karoo", "abliterated", "loihi", "lava", "soap", "ledger", "network", "java_sdk"));
            epoch.put("judgeSlots", List.of("local_benchmark", "karoo_compare", "tiny_critic", "optional_copilot", "optional_gemini", "optional_cloud_agent"));
            epoch.put("quickEditVars", List.of(
                    "route",
                    "token_budget",
                    "retrieval_weight",
                    "web_research_gate",
                    "karoo_rounds",
                    "loihi_cube",
                    "lava_mode",
                    "soap_endpoint",
                    "promotion_gate"
            ));
            epoch.put("ascii", asciiEpochCube());
            epoch.put("proposedDiagram", proposedEpochDiagram(body));
            epoch.put("promotion", "proposal queue only until benchmark gate proves success >= 99.99 and speed/resource improvement");
            epoch.put("sha256", sha256(jsonObject(epoch)));
            appendJsonLine(ASCII_EPOCH_LOG, epoch);
            appendJsonLine(PERSISTENCE_LOG, mapOf("event", "ascii_epoch_append", "sha256", epoch.get("sha256")));
            send(exchange, 200, jsonObject(epoch), "application/json");
        }
    }

    private static class EpochUpgradeProofHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                send(exchange, 405, jsonError("method_not_allowed"), "application/json");
                return;
            }
            String body = readBody(exchange);
            Map<String, Object> proof = buildEpochUpgradeProof(body);
            proof.put("sha256", sha256(jsonObject(proof)));
            appendJsonLine(EPOCH_UPGRADE_LOG, proof);
            appendJsonLine(PERSISTENCE_LOG, mapOf("event", "epoch_upgrade_proof_append", "sha256", proof.get("sha256")));
            send(exchange, 200, jsonObject(proof), "application/json");
        }
    }

    private static class EpochImplementHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
                int limit = Math.max(1, Math.min(parseInt(query.getOrDefault("limit", "20"), 20), 100));
                send(exchange, 200, jsonObject(mapOf(
                        "status", "ok",
                        "version", SDK_VERSION,
                        "queue", readJsonLines(EPOCH_IMPLEMENT_LOG, limit)
                )), "application/json");
                return;
            }
            if (!"POST".equals(exchange.getRequestMethod())) {
                send(exchange, 405, jsonError("method_not_allowed"), "application/json");
                return;
            }
            String body = readBody(exchange);
            String proposalId = extractJsonString(body, "proposalId", "UNKNOWN_PROPOSAL");
            String subsystem = extractJsonString(body, "subsystem", "unknown_subsystem");
            String proposedChange = extractJsonString(body, "proposedChange", "No proposed change supplied.");
            String acceptanceTest = extractJsonString(body, "acceptanceTest", "No acceptance test supplied.");
            String evidence = extractJsonString(body, "evidence", "No evidence supplied.");
            String goal = extractJsonString(body, "goal", "accepted_epoch_implementation");
            String implementationMode = extractJsonString(body, "implementationMode", "bridge_build_request");

            String bridgePrompt = acceptedEpochBridgePrompt(proposalId, subsystem, proposedChange, acceptanceTest, evidence, goal);
            Map<String, Object> bridgeResult = bridgeImplementationRequest(bridgePrompt);
            Map<String, Object> queueEntry = new LinkedHashMap<>();
            queueEntry.put("kind", "epoch_implementation_request");
            queueEntry.put("version", SDK_VERSION);
            queueEntry.put("timestamp", Instant.now().toString());
            queueEntry.put("proposalId", proposalId);
            queueEntry.put("goal", goal);
            queueEntry.put("subsystem", subsystem);
            queueEntry.put("proposedChange", proposedChange);
            queueEntry.put("acceptanceTest", acceptanceTest);
            queueEntry.put("evidence", evidence);
            queueEntry.put("implementationMode", implementationMode);
            queueEntry.put("approvalStatus", "accepted_by_user");
            queueEntry.put("implementationStatus", bridgeResult.get("status"));
            queueEntry.put("bridgePrompt", bridgePrompt);
            queueEntry.put("bridgeResult", bridgeResult);
            queueEntry.put("guard", "Accepted proposal is queued and handed to the bridge as a bounded build request. GUI remains preserved; proof gate still applies.");
            queueEntry.put("sha256", sha256(jsonObject(queueEntry)));
            appendJsonLine(EPOCH_IMPLEMENT_LOG, queueEntry);
            appendJsonLine(PERSISTENCE_LOG, mapOf(
                    "event", "epoch_implementation_append",
                    "proposalId", proposalId,
                    "sha256", queueEntry.get("sha256"),
                    "implementationStatus", bridgeResult.get("status")
            ));
            send(exchange, 200, jsonObject(queueEntry), "application/json");
        }
    }

    private static class LogTailHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
            String file = query.getOrDefault("file", "system");
            int lines = parseInt(query.getOrDefault("lines", "80"), 80);
            Path path = switch (file) {
                case "shipper" -> ROOT.resolve("logic_blockchain_shipper.log");
                case "topology" -> ROOT.resolve("topology_sidecar_loop.log");
                case "house_stdout" -> ROOT.resolve("house_inference_stdout.log");
                case "house_stderr" -> ROOT.resolve("house_inference_stderr.log");
                case "tests" -> TEST_LOG;
                case "ab" -> AB_LOG;
                case "training" -> TRAINING_LOG;
                case "recursive_training" -> TRAINING_EPOCH_LOG;
                case "loihi" -> LOIHI_LOG;
                case "benchmarks" -> BENCHMARK_LOG;
                case "ascii_epochs" -> ASCII_EPOCH_LOG;
                case "epoch_upgrades" -> EPOCH_UPGRADE_LOG;
                case "epoch_implementations" -> EPOCH_IMPLEMENT_LOG;
                case "algebraic_flows" -> ALGEBRAIC_FLOW_LOG;
                case "persistence" -> PERSISTENCE_LOG;
                case "darwin_programs" -> DARWIN_TEST_PROGRAM_LOG;
                case "darwin_algorithms" -> DARWIN_ALGORITHM_REGISTRY_LOG;
                case "darwin_generations" -> DARWIN_GENERATION_LOG;
                case "darwin_winners" -> DARWIN_WINNER_LOG;
                default -> ROOT.resolve("system_log.txt");
            };
            send(exchange, 200, jsonObject(mapOf(
                    "file", file,
                    "path", path.toString(),
                    "tail", tail(path, Math.max(1, Math.min(lines, 400)))
            )), "application/json");
        }
    }

    private static class DesignHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            send(exchange, 200, jsonObject(mapOf(
                    "sdk", "VIPER Java SDK",
                    "persistence", List.of(
                            "settings persisted in sdk_settings.json",
                            "system tests appended to system_tests.jsonl",
                            "AB tests appended to ab_tests.jsonl",
                            "training runs appended to training_runs.jsonl",
                            "recursive training epochs appended to recursive_training_epochs.jsonl",
                            "Loihi experiments appended to loihi_experiments.jsonl",
                            "benchmark snapshots appended to benchmark_snapshots.jsonl",
                            "ASCII epoch proposals appended to ascii_epoch_queue.jsonl",
                            "epoch upgrade proofs appended to epoch_upgrade_proofs.jsonl",
                            "accepted epoch implementations appended to epoch_implementation_queue.jsonl",
                            "algebraic pattern flow runs appended to algebraic_pattern_flows.jsonl",
                            "persistence events appended to persistence_events.jsonl"
                    ),
                    "training", "Active eval training exists now: Java lab probes services, bridge prefetch, recent benchmarks, scores a candidate, writes training/epoch/benchmark proof logs, and keeps weights untouched.",
                    "recursiveTraining", "Recursive training records are now backed by actual eval probes. Real model-weight mutation is not submitted by this Java SDK.",
                    "loihi", "Future Lava/Loihi sidecar receives topological codes, not raw hidden thoughts. It maps codes to spike topology and returns measurable logic/code deltas.",
                    "karoo", "Proposal-only optimizer until 99.99% success plus speed/resource gate is proven.",
                    "fabric", "Tiny chooser writes 15-word cards for ask, DB, recent prompts, and repair state; larger model gets selected lens plus real retrieval.",
                    "externalJudges", "Copilot/Gemini/cloud agents are optional judge slots. They can weigh an epoch, but cannot auto-promote without local proof.",
                    "epochProof", "The upgrade proof endpoint analyzes current logs/benchmarks and emits concrete proposed changes with evidence and tests.",
                    "epochImplement", "Accepted epoch proposals can now be queued and handed to the bridge as bounded implementation requests with the acceptance test attached.",
                    "algebraicCube", "The algebraic flow lab permutes entry data, axiomatic sets, processing paths, and exit targets, then probes the live model route with bounded examples.",
                    "darwinLab", "The Darwin lab evolves bounded algorithm weight sets against local test programs, records generations and winners, and keeps brute force plus the matrix as the primary baseline.",
                    "ui", "Separate VS Code-like SDK surface; locked main GUI remains unchanged."
            )), "application/json");
        }
    }

    private static class AlgebraicFlowHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
                int limit = Math.max(1, Math.min(parseInt(query.getOrDefault("limit", "12"), 12), 50));
                send(exchange, 200, jsonObject(mapOf(
                        "status", "ok",
                        "version", SDK_VERSION,
                        "supportedEntries", supportedAlgebraicEntries(),
                        "supportedExits", supportedAlgebraicExits(),
                        "recentRuns", readJsonFragments(ALGEBRAIC_FLOW_LOG, limit),
                        "policy", "bounded permutation lab; first failures are recorded as evidence rather than hidden"
                )), "application/json");
                return;
            }
            if (!"POST".equals(exchange.getRequestMethod())) {
                send(exchange, 405, jsonError("method_not_allowed"), "application/json");
                return;
            }
            String body = readBody(exchange);
            Map<String, Object> run = buildAlgebraicFlowRun(body);
            run.put("sha256", sha256(jsonObject(run)));
            appendJsonLine(ALGEBRAIC_FLOW_LOG, run);
            appendJsonLine(PERSISTENCE_LOG, mapOf(
                    "event", "algebraic_flow_append",
                    "sha256", run.get("sha256"),
                    "summary", ((Map<?, ?>) run.get("summary")).get("status")
            ));
            send(exchange, 200, jsonObject(run), "application/json");
        }
    }

    private static Map<String, Object> serviceHealth() {
        Map<String, Object> services = new LinkedHashMap<>();
        services.put("bridge8080", probe("http://127.0.0.1:8080/api/benchmarks?limit=1"));
        services.put("house11435", probe("http://127.0.0.1:11435/health"));
        services.put("shipper18081", probe("http://127.0.0.1:18081/health"));
        return services;
    }

    private static Map<String, Object> currentBenchmark(String mode) {
        long start = System.currentTimeMillis();
        Map<String, Object> benchmark = new LinkedHashMap<>();
        benchmark.put("kind", "benchmark_snapshot");
        benchmark.put("mode", mode);
        benchmark.put("timestamp", Instant.now().toString());
        benchmark.put("services", serviceHealth());
        benchmark.put("counts", mapOf(
                "systemTests", countLines(TEST_LOG),
                "abTests", countLines(AB_LOG),
                "trainingRuns", countLines(TRAINING_LOG),
                "recursiveTrainingEpochs", countLines(TRAINING_EPOCH_LOG),
                "loihiExperiments", countLines(LOIHI_LOG),
                "benchmarkSnapshots", countLines(BENCHMARK_LOG),
                "algebraicFlows", countLines(ALGEBRAIC_FLOW_LOG),
                "persistenceEvents", countLines(PERSISTENCE_LOG)
        ));
        benchmark.put("logs", mapOf(
                "systemBytes", fileSize(ROOT.resolve("system_log.txt")),
                "shipperBytes", fileSize(ROOT.resolve("logic_blockchain_shipper.log")),
                "topologyBytes", fileSize(ROOT.resolve("topology_sidecar_loop.log")),
                "houseStdoutBytes", fileSize(ROOT.resolve("house_inference_stdout.log")),
                "houseStderrBytes", fileSize(ROOT.resolve("house_inference_stderr.log"))
        ));
        benchmark.put("policy", "benchmarks prove service latency, log growth, and proposal epochs before recursive automation is trusted");
        benchmark.put("durationMs", System.currentTimeMillis() - start);
        return benchmark;
    }

    private static String asciiEpochCube() {
        return """
                         z: subsystem / top-code family
                              ^
                              |
                 +------------+------------+
                /| chooser   /| karoo     /|
               / | db       / | lava     / |
              +------------+------------+  |
              |  | soap    |  | loihi   |  |
              |  +---------|--+---------|--+--> x: logic / code coordinate
              | / ledger   | / network  | /
              |/ java_sdk  |/ agents    |/
              +------------+------------+
             /
            v
          y: weight / confidence / resource cost

          propose -> weigh -> benchmark -> compare -> wait/promote
          """;
    }

    private static List<String> supportedAlgebraicEntries() {
        return List.of(
                "topology_ascii",
                "benchmark_json",
                "epoch_proposal",
                "successful_code_card",
                "behavior_card",
                "nominal_fact_card",
                "source_tree_card",
                "performative_acl_card",
                "bytecode_signature_card"
        );
    }

    private static List<String> supportedAlgebraicExits() {
        return List.of(
                "route_plan",
                "proof_card",
                "code_pattern_card",
                "logic_pattern_card",
                "epoch_proposal",
                "performative_route_card",
                "bytecode_plan_card"
        );
    }

    private static Map<String, Object> buildAlgebraicFlowRun(String body) {
        String requestedEntry = extractJsonString(body, "startData", "topology_ascii");
        String requestedExit = extractJsonString(body, "endData", "code_pattern_card");
        String objective = extractJsonString(body, "objective", "map algebraic and logic patterns for reusable code generation");
        String customAsciiFlow = extractJsonString(body, "customAsciiFlow", algebraicCubeFlowDiagram());
        String customMathNotes = extractJsonString(body, "customMathNotes", algebraicMathModel());
        int maxPermutations = Math.max(1, Math.min(parseInt(extractJsonString(body, "maxPermutations", "4"), 4), 8));
        int physicsComparisons = Math.max(0, Math.min(parseInt(extractJsonString(body, "physicsComparisons", "0"), 0), 50));
        int physicsEvolutionRounds = Math.max(0, Math.min(parseInt(extractJsonString(body, "physicsEvolutionRounds", "0"), 0), 50));
        String comparisonFamily = extractJsonString(body, "comparisonFamily", "established_physics_grids");
        String chooserExperiment = extractJsonString(body, "chooserExperiment", "none");
        boolean includeModelProbes = !"false".equalsIgnoreCase(extractJsonString(body, "includeModelProbes", "true"));

        List<String> entryCandidates = uniqueOrdered(List.of(
                requestedEntry,
                "topology_ascii",
                "benchmark_json",
                "epoch_proposal",
                "successful_code_card",
                "behavior_card",
                "nominal_fact_card",
                "source_tree_card",
                "performative_acl_card",
                "bytecode_signature_card"
        ));
        List<String> exitCandidates = uniqueOrdered(List.of(
                requestedExit,
                "code_pattern_card",
                "logic_pattern_card",
                "proof_card",
                "route_plan",
                "epoch_proposal",
                "performative_route_card",
                "bytecode_plan_card"
        ));

        List<Object> permutations = new ArrayList<>();
        int pass = 0;
        int fail = 0;
        int skipped = 0;
        outer:
        for (String entry : entryCandidates) {
            for (String exit : exitCandidates) {
                if (permutations.size() >= maxPermutations) {
                    break outer;
                }
                Map<String, Object> axioms = algebraicAxiomFit(entry, exit);
                String sampleData = algebraicSampleData(entry);
                Map<String, Object> probe = includeModelProbes
                        ? algebraicModelProbe(entry, exit, objective, axioms, sampleData, customAsciiFlow, customMathNotes)
                        : mapOf("status", "skipped", "reason", "includeModelProbes=false");
                String status = String.valueOf(probe.getOrDefault("status", "skipped"));
                if ("pass".equals(status)) {
                    pass++;
                } else if ("fail".equals(status)) {
                    fail++;
                } else {
                    skipped++;
                }
                Map<String, Object> blockScale = algebraicBlockScale(entry, exit);
                Map<String, Object> scorecard = algebraicChooserScorecard(entry, exit, axioms, blockScale);
                permutations.add(mapOf(
                        "entryPoint", entry,
                        "exitPoint", exit,
                        "objective", objective,
                        "axiomaticSet", axioms.get("axiomaticSet"),
                        "mathFit", axioms.get("mathFit"),
                        "logicFit", axioms.get("logicFit"),
                        "recommendedRoute", axioms.get("recommendedRoute"),
                        "recommendedModelHook", axioms.get("recommendedModelHook"),
                        "entryTransform", axioms.get("entryTransform"),
                        "exitTransform", axioms.get("exitTransform"),
                        "entryExitSearch", algebraicEntryExitSearch(entry, exit),
                        "softwareSignals", algebraicSoftwareSignals(entry),
                        "performativeRules", algebraicPerformativeRules(entry, exit),
                        "bytecodePlan", algebraicBytecodePlan(entry, exit),
                        "blockScale", blockScale,
                        "chooserScorecard", scorecard,
                        "sampleData", sampleData,
                        "probe", probe
                ));
            }
        }

        Map<String, Object> physicsComparisonSuite = physicsComparisonSuite(permutations, objective, comparisonFamily, physicsComparisons);
        Map<String, Object> physicsEvolutionRefinement = physicsEvolutionRefinement(physicsComparisonSuite, permutations, objective, physicsEvolutionRounds);
        Map<String, Object> topologicalTemplatePromotion = topologicalTemplatePromotion(permutations, physicsComparisonSuite, physicsEvolutionRefinement);

        return mapOf(
                "kind", "algebraic_pattern_flow_run",
                "version", SDK_VERSION,
                "timestamp", Instant.now().toString(),
                "request", body,
                "objective", objective,
                "asciiFlow", customAsciiFlow,
                "mathModel", customMathNotes,
                "softwareSearchPolicy", softwareSearchPolicy(),
                "ruleRegistry", algebraicRuleRegistry(),
                "processableData", processableDataCards(),
                "supportedEntries", supportedAlgebraicEntries(),
                "supportedExits", supportedAlgebraicExits(),
                "summary", mapOf(
                        "status", fail == 0 ? "pass_or_partial" : "mixed_failures_recorded",
                        "testedPermutations", permutations.size(),
                        "pass", pass,
                        "fail", fail,
                        "skipped", skipped
                ),
                "layeredDbModel", layeredDbModel(),
                "hookupGuide", algebraicHookupGuide(),
                "chooserComparison", chooserComparison(permutations, objective, requestedEntry, requestedExit),
                "chooserABExperiment", chooserABExperiment(permutations, objective, requestedEntry, requestedExit, chooserExperiment, includeModelProbes),
                "physicsComparisonSuite", physicsComparisonSuite,
                "physicsEvolutionRefinement", physicsEvolutionRefinement,
                "topologicalTemplatePromotion", topologicalTemplatePromotion,
                "permutations", permutations,
                "guard", "Bounded permutation lab only. Records fit, failures, and model hookups without mutating code paths."
        );
    }

    private static Map<String, Object> darwinSeedAlgorithm(
            String algorithmId,
            String family,
            double bruteForceWeight,
            double topologyWeight,
            double proofWeight,
            double routeWeight,
            double performativeWeight,
            double repairWeight,
            double latencyWeight,
            double recursionWeight,
            double dependencyWeight
    ) {
        return mapOf(
                "algorithmId", algorithmId,
                "family", family,
                "status", "seed",
                "generation", 0,
                "parentAlgorithmId", "ROOT",
                "bruteForceWeight", bruteForceWeight,
                "topologyWeight", topologyWeight,
                "proofWeight", proofWeight,
                "routeWeight", routeWeight,
                "performativeWeight", performativeWeight,
                "repairWeight", repairWeight,
                "latencyWeight", latencyWeight,
                "recursionWeight", recursionWeight,
                "dependencyWeight", dependencyWeight
        );
    }

    private static List<Map<String, Object>> darwinDefaultAlgorithmSeeds() {
        return List.of(
                darwinSeedAlgorithm("ALG_BRUTE_MATRIX_SEED", "bruteforce_matrix_primary", 0.84, 0.78, 0.72, 0.74, 0.69, 0.62, 0.58, 0.48, 0.81),
                darwinSeedAlgorithm("ALG_TOPOLOGY_ANCHOR_SEED", "topology_anchor_primary", 0.58, 0.91, 0.76, 0.72, 0.64, 0.57, 0.54, 0.61, 0.56),
                darwinSeedAlgorithm("ALG_PROOF_ROUTE_SEED", "proof_route_primary", 0.61, 0.76, 0.88, 0.86, 0.79, 0.51, 0.63, 0.44, 0.55),
                darwinSeedAlgorithm("ALG_PREDICTIVE_HYBRID_SEED", "predictive_hybrid", 0.52, 0.82, 0.79, 0.83, 0.77, 0.58, 0.59, 0.67, 0.52),
                darwinSeedAlgorithm("ALG_REPAIR_BALANCED_SEED", "repair_balanced", 0.66, 0.71, 0.74, 0.68, 0.63, 0.85, 0.61, 0.52, 0.59),
                darwinSeedAlgorithm("ALG_RECURSIVE_BIND_SEED", "recursive_bind_primary", 0.57, 0.83, 0.68, 0.71, 0.74, 0.63, 0.49, 0.88, 0.66),
                darwinSeedAlgorithm("ALG_LATENCY_GUARD_SEED", "latency_guard_primary", 0.73, 0.67, 0.69, 0.58, 0.54, 0.48, 0.91, 0.36, 0.62)
        );
    }

    private static List<Map<String, Object>> darwinPrograms(int limit) {
        List<Map<String, Object>> programs = new ArrayList<>();
        for (String line : readJsonLines(DARWIN_TEST_PROGRAM_LOG, Math.max(limit, 20))) {
            programs.add(mapOf(
                    "programId", extractJsonString(line, "programId", "UNKNOWN_PROGRAM"),
                    "language", extractJsonString(line, "language", "unknown"),
                    "topologyKind", extractJsonString(line, "topologyKind", "unknown"),
                    "pageCount", extractJsonNumber(line, "pageCount", 1.0),
                    "syntaxPressure", extractJsonNumber(line, "syntaxPressure", 0.5),
                    "dependencyPressure", extractJsonNumber(line, "dependencyPressure", 0.5),
                    "topologyPressure", extractJsonNumber(line, "topologyPressure", 0.5),
                    "performativePressure", extractJsonNumber(line, "performativePressure", 0.5),
                    "routePressure", extractJsonNumber(line, "routePressure", 0.5),
                    "recursionPressure", extractJsonNumber(line, "recursionPressure", 0.3),
                    "repairRisk", extractJsonNumber(line, "repairRisk", 0.3),
                    "latencyPressure", extractJsonNumber(line, "latencyPressure", 0.4),
                    "bruteForceFriendliness", extractJsonNumber(line, "bruteForceFriendliness", 0.6),
                    "acceptanceTarget", extractJsonString(line, "acceptanceTarget", "matrix_pass")
            ));
            if (programs.size() >= limit) {
                break;
            }
        }
        return programs;
    }

    private static List<Map<String, Object>> darwinAlgorithms(int limit) {
        List<Map<String, Object>> algorithms = new ArrayList<>();
        for (String line : readJsonLines(DARWIN_ALGORITHM_REGISTRY_LOG, Math.max(limit, 20))) {
            algorithms.add(mapOf(
                    "algorithmId", extractJsonString(line, "algorithmId", "UNKNOWN_ALGORITHM"),
                    "family", extractJsonString(line, "family", "unknown"),
                    "status", extractJsonString(line, "status", "seed"),
                    "generation", extractJsonNumber(line, "generation", 0.0),
                    "parentAlgorithmId", extractJsonString(line, "parentAlgorithmId", "ROOT"),
                    "bruteForceWeight", extractJsonNumber(line, "bruteForceWeight", 0.5),
                    "topologyWeight", extractJsonNumber(line, "topologyWeight", 0.5),
                    "proofWeight", extractJsonNumber(line, "proofWeight", 0.5),
                    "routeWeight", extractJsonNumber(line, "routeWeight", 0.5),
                    "performativeWeight", extractJsonNumber(line, "performativeWeight", 0.5),
                    "repairWeight", extractJsonNumber(line, "repairWeight", 0.5),
                    "latencyWeight", extractJsonNumber(line, "latencyWeight", 0.5),
                    "recursionWeight", extractJsonNumber(line, "recursionWeight", 0.5),
                    "dependencyWeight", extractJsonNumber(line, "dependencyWeight", 0.5)
            ));
            if (algorithms.size() >= limit) {
                break;
            }
        }
        return algorithms;
    }

    private static List<Map<String, Object>> darwinSeedAlgorithms(int limit) {
        List<Map<String, Object>> seeds = new ArrayList<>();
        for (Map<String, Object> algorithm : darwinAlgorithms(Math.max(limit, 50))) {
            if ("seed".equals(String.valueOf(algorithm.getOrDefault("status", "")))) {
                seeds.add(algorithm);
            }
            if (seeds.size() >= limit) {
                break;
            }
        }
        return seeds;
    }

    private static Map<String, Object> buildDarwinLabRun(String body) throws IOException {
        int generations = Math.max(1, Math.min(parseInt(extractJsonString(body, "generations", "6"), 6), 12));
        int programLimit = Math.max(1, Math.min(parseInt(extractJsonString(body, "programLimit", "8"), 8), 8));
        double mutationRate = clamp01(numericValue(extractJsonString(body, "mutationRate", "0.08")));
        String objective = extractJsonString(body, "objective", "darwinistically evolve bounded algorithms against local test programs");
        String strategy = extractJsonString(body, "strategy", "bruteforce_matrix_primary");
        String runId = "DARWIN_" + Instant.now().toString().replace(":", "").replace("-", "").replace(".", "");
        List<Map<String, Object>> programs = darwinPrograms(programLimit);
        List<Map<String, Object>> population = new ArrayList<>(darwinDefaultAlgorithmSeeds());
        List<Object> generationSummaries = new ArrayList<>();
        Map<String, Object> globalBest = Map.of();
        Map<String, Object> finalBaseline = Map.of();

        for (int generation = 1; generation <= generations; generation++) {
            List<Map<String, Object>> evaluated = evaluateDarwinPopulation(population, programs, objective, strategy, generation);
            evaluated.sort(Comparator.comparingDouble((Map<String, Object> candidate) -> numericValue(candidate.get("fitness"))).reversed());
            Map<String, Object> best = evaluated.get(0);
            Map<String, Object> baseline = darwinBaselineCandidate(evaluated);
            finalBaseline = baseline;
            if (globalBest.isEmpty() || numericValue(best.get("fitness")) > numericValue(globalBest.get("fitness"))) {
                globalBest = best;
            }
            Map<String, Object> generationRecord = mapOf(
                    "kind", "darwin_generation",
                    "timestamp", Instant.now().toString(),
                    "runId", runId,
                    "generation", generation,
                    "objective", objective,
                    "strategy", strategy,
                    "mutationRate", mutationRate,
                    "programIds", darwinProgramIds(programs),
                    "baseline", darwinCompactAlgorithm(baseline),
                    "best", darwinCompactAlgorithm(best),
                    "population", darwinCompactPopulation(evaluated)
            );
            generationRecord.put("sha256", sha256(jsonObject(generationRecord)));
            appendJsonLine(DARWIN_GENERATION_LOG, generationRecord);
            generationSummaries.add(mapOf(
                    "generation", generation,
                    "baseline", darwinCompactAlgorithm(baseline),
                    "best", darwinCompactAlgorithm(best),
                    "medianFitness", round3(darwinMedianFitness(evaluated)),
                    "populationSize", evaluated.size(),
                    "winnerBeatsBaseline", Boolean.TRUE.equals(nestedMap(best, "baselineComparison").get("overtakeReady"))
            ));
            if (generation < generations) {
                population = evolveDarwinPopulation(evaluated, generation + 1, mutationRate, objective);
            }
        }
        Map<String, Object> finalBaselineComparison = nestedMap(globalBest, "baselineComparison");

        return mapOf(
                "kind", "darwin_lab_run",
                "version", SDK_VERSION,
                "timestamp", Instant.now().toString(),
                "runId", runId,
                "objective", objective,
                "strategy", strategy,
                "generations", generations,
                "programs", programs,
                "initialPopulationSize", darwinDefaultAlgorithmSeeds().size(),
                "summary", mapOf(
                        "status", "completed",
                        "programCount", programs.size(),
                        "generationCount", generations,
                        "baselineAlgorithmId", finalBaselineComparison.getOrDefault("baselineAlgorithmId", ""),
                        "winnerAlgorithmId", globalBest.get("algorithmId"),
                        "winnerFitness", round3(numericValue(globalBest.get("fitness"))),
                        "winnerBeatsBaseline", Boolean.TRUE.equals(finalBaselineComparison.get("overtakeReady")),
                        "winnerReason", finalBaselineComparison.getOrDefault("overtakeReason", "baseline_guard")
                ),
                "generationSummaries", generationSummaries,
                "finalWinner", globalBest,
                "finalBaseline", finalBaseline,
                "guard", "Bounded Darwin lab only. Brute force plus matrix stay primary; evolved winners must still pass proof gates."
        );
    }

    private static List<Map<String, Object>> evaluateDarwinPopulation(
            List<Map<String, Object>> population,
            List<Map<String, Object>> programs,
            String objective,
            String strategy,
            int generation
    ) {
        List<Map<String, Object>> rawEvaluated = new ArrayList<>();
        for (Map<String, Object> algorithm : population) {
            List<Object> results = new ArrayList<>();
            double matrixSum = 0.0;
            double promotionSum = 0.0;
            double repairSum = 0.0;
            double latencySum = 0.0;
            double topologySum = 0.0;
            for (Map<String, Object> program : programs) {
                Map<String, Object> result = darwinProgramResult(algorithm, program, objective, strategy, generation);
                results.add(result);
                matrixSum += numericValue(result.get("matrixPassRatio"));
                promotionSum += Boolean.TRUE.equals(result.get("promotionReady")) ? 1.0 : 0.0;
                repairSum += numericValue(result.get("repairBurden"));
                latencySum += numericValue(result.get("latencyScore"));
                topologySum += numericValue(result.get("topologyFit"));
            }
            double count = Math.max(1, programs.size());
            double avgMatrix = matrixSum / count;
            double promotionSuccess = promotionSum / count;
            double avgRepair = repairSum / count;
            double avgLatency = latencySum / count;
            double avgTopology = topologySum / count;
            double fitness = clamp01(
                    (0.40 * avgMatrix)
                            + (0.24 * promotionSuccess)
                            + (0.18 * (1.0 - avgRepair))
                            + (0.10 * avgLatency)
                            + (0.08 * avgTopology)
            );
            if (objective.toLowerCase().contains("brute")) {
                fitness = clamp01(fitness + (0.03 * numericValue(algorithm.get("bruteForceWeight"))));
            }
            rawEvaluated.add(mapOf(
                    "algorithmId", algorithm.get("algorithmId"),
                    "family", algorithm.get("family"),
                    "generation", generation,
                    "parentAlgorithmId", algorithm.get("parentAlgorithmId"),
                    "weights", darwinWeightMap(algorithm),
                    "metrics", mapOf(
                            "avgMatrixPassRatio", round3(avgMatrix),
                            "promotionSuccessRatio", round3(promotionSuccess),
                            "avgRepairBurden", round3(avgRepair),
                            "avgLatencyScore", round3(avgLatency),
                            "avgTopologyFit", round3(avgTopology)
                    ),
                    "geneticPerformance", mapOf(
                            "matrixPassRatio", round3(avgMatrix),
                            "repairBurden", round3(avgRepair),
                            "latencyScore", round3(avgLatency),
                            "promotionSuccess", round3(promotionSuccess)
                    ),
                    "programResults", results,
                    "rawFitness", round3(fitness),
                    "fitness", round3(fitness)
            ));
        }
        Map<String, Object> baseline = darwinBaselineCandidate(rawEvaluated);
        Map<String, Object> baselineMetrics = nestedMap(baseline, "metrics");
        double baselineMatrix = numericValue(baselineMetrics.get("avgMatrixPassRatio"));
        double baselinePromotion = numericValue(baselineMetrics.get("promotionSuccessRatio"));
        double baselineRepair = numericValue(baselineMetrics.get("avgRepairBurden"));
        double baselineLatency = numericValue(baselineMetrics.get("avgLatencyScore"));
        double baselineTopology = numericValue(baselineMetrics.get("avgTopologyFit"));
        String baselineAlgorithmId = String.valueOf(baseline.getOrDefault("algorithmId", ""));
        List<Map<String, Object>> evaluated = new ArrayList<>();
        for (Map<String, Object> candidate : rawEvaluated) {
            Map<String, Object> metrics = nestedMap(candidate, "metrics");
            double avgMatrix = numericValue(metrics.get("avgMatrixPassRatio"));
            double promotionSuccess = numericValue(metrics.get("promotionSuccessRatio"));
            double avgRepair = numericValue(metrics.get("avgRepairBurden"));
            double avgLatency = numericValue(metrics.get("avgLatencyScore"));
            double avgTopology = numericValue(metrics.get("avgTopologyFit"));
            double matrixDelta = round3(avgMatrix - baselineMatrix);
            double promotionDelta = round3(promotionSuccess - baselinePromotion);
            double repairDelta = round3(baselineRepair - avgRepair);
            double latencyDelta = round3(avgLatency - baselineLatency);
            double topologyDelta = round3(avgTopology - baselineTopology);
            boolean isBaseline = String.valueOf(candidate.get("algorithmId")).equals(baselineAlgorithmId);
            boolean overtakeReady = isBaseline
                    || (matrixDelta >= 0.0
                    && promotionDelta >= 0.0
                    && repairDelta >= -0.02
                    && latencyDelta >= -0.03
                    && topologyDelta >= -0.02);
            double adjustedFitness = numericValue(candidate.get("rawFitness"));
            if (isBaseline) {
                adjustedFitness = clamp01(adjustedFitness + 0.012);
            } else if (overtakeReady) {
                adjustedFitness = clamp01(
                        adjustedFitness
                                + 0.018
                                + (0.03 * Math.max(0.0, matrixDelta))
                                + (0.02 * Math.max(0.0, promotionDelta))
                                + (0.01 * Math.max(0.0, repairDelta))
                                + (0.01 * Math.max(0.0, latencyDelta))
                                + (0.01 * Math.max(0.0, topologyDelta))
                );
            } else {
                adjustedFitness = clamp01(
                        adjustedFitness
                                - 0.035
                                - (0.02 * Math.max(0.0, -matrixDelta))
                                - (0.01 * Math.max(0.0, -promotionDelta))
                );
            }
            Map<String, Object> promoted = new LinkedHashMap<>(candidate);
            promoted.put("fitness", round3(adjustedFitness));
            promoted.put("promotionStatus", overtakeReady ? "baseline_clear" : "baseline_hold");
            promoted.put("baselineComparison", mapOf(
                    "baselineAlgorithmId", baselineAlgorithmId,
                    "isBaseline", isBaseline,
                    "overtakeReady", overtakeReady,
                    "matrixDelta", matrixDelta,
                    "promotionDelta", promotionDelta,
                    "repairDelta", repairDelta,
                    "latencyDelta", latencyDelta,
                    "topologyDelta", topologyDelta,
                    "overtakeReason", isBaseline ? "baseline_guard" : (overtakeReady ? "beats_or_matches_bruteforce_baseline" : "baseline_still_stronger")
            ));
            evaluated.add(promoted);
        }
        return evaluated;
    }

    private static Map<String, Object> darwinProgramResult(
            Map<String, Object> algorithm,
            Map<String, Object> program,
            String objective,
            String strategy,
            int generation
    ) {
        double syntaxFit = darwinFeatureFit(numericValue(algorithm.get("proofWeight")), numericValue(program.get("syntaxPressure")));
        double dependencyFit = darwinFeatureFit(numericValue(algorithm.get("dependencyWeight")), numericValue(program.get("dependencyPressure")));
        double topologyFit = darwinFeatureFit(numericValue(algorithm.get("topologyWeight")), numericValue(program.get("topologyPressure")));
        double performativeFit = darwinFeatureFit(numericValue(algorithm.get("performativeWeight")), numericValue(program.get("performativePressure")));
        double routeFit = darwinFeatureFit(numericValue(algorithm.get("routeWeight")), numericValue(program.get("routePressure")));
        double recursionFit = darwinFeatureFit(numericValue(algorithm.get("recursionWeight")), numericValue(program.get("recursionPressure")));
        double bruteForceFit = darwinFeatureFit(numericValue(algorithm.get("bruteForceWeight")), numericValue(program.get("bruteForceFriendliness")));
        double repairCoverage = darwinFeatureFit(numericValue(algorithm.get("repairWeight")), numericValue(program.get("repairRisk")));
        double latencyScore = clamp01(
                numericValue(algorithm.get("latencyWeight"))
                        - (0.40 * numericValue(program.get("latencyPressure")))
                        + (0.30 * bruteForceFit)
        );
        double determinismFit = round3((topologyFit + routeFit + syntaxFit) / 3.0);
        if (objective.toLowerCase().contains("matrix")) {
            determinismFit = clamp01(determinismFit + 0.05);
        }
        if (strategy.toLowerCase().contains("bruteforce")) {
            bruteForceFit = clamp01(bruteForceFit + 0.06);
        }
        double matrixPassRatio = round3((
                syntaxFit
                        + dependencyFit
                        + topologyFit
                        + performativeFit
                        + routeFit
                        + recursionFit
                        + bruteForceFit
                        + repairCoverage
                        + latencyScore
                        + determinismFit
        ) / 10.0);
        double repairBurden = round3(clamp01(numericValue(program.get("repairRisk")) - (0.72 * numericValue(algorithm.get("repairWeight"))) - (0.10 * numericValue(algorithm.get("proofWeight")))));
        boolean promotionReady = matrixPassRatio >= 0.78 && routeFit >= 0.72 && topologyFit >= 0.72;
        return mapOf(
                "programId", program.get("programId"),
                "language", program.get("language"),
                "acceptanceTarget", program.get("acceptanceTarget"),
                "matrixPassRatio", matrixPassRatio,
                "matrixPassCount", Math.round(matrixPassRatio * 100.0),
                "topologyFit", round3(topologyFit),
                "routeFit", round3(routeFit),
                "performativeFit", round3(performativeFit),
                "repairBurden", repairBurden,
                "latencyScore", round3(latencyScore),
                "promotionReady", promotionReady
        );
    }

    private static List<Map<String, Object>> evolveDarwinPopulation(
            List<Map<String, Object>> evaluated,
            int nextGeneration,
            double mutationRate,
            String objective
    ) {
        List<Map<String, Object>> next = new ArrayList<>();
        int targetSize = evaluated.size();
        int survivors = Math.min(2, evaluated.size());
        Map<String, Object> baseline = darwinBaselineCandidate(evaluated);
        if (!baseline.isEmpty()) {
            next.add(mapOf(
                    "algorithmId", baseline.get("algorithmId"),
                    "family", baseline.get("family"),
                    "status", "survivor",
                    "generation", nextGeneration,
                    "parentAlgorithmId", baseline.get("algorithmId"),
                    "bruteForceWeight", geneValue(nestedMap(baseline, "weights"), "bruteForceWeight"),
                    "topologyWeight", geneValue(nestedMap(baseline, "weights"), "topologyWeight"),
                    "proofWeight", geneValue(nestedMap(baseline, "weights"), "proofWeight"),
                    "routeWeight", geneValue(nestedMap(baseline, "weights"), "routeWeight"),
                    "performativeWeight", geneValue(nestedMap(baseline, "weights"), "performativeWeight"),
                    "repairWeight", geneValue(nestedMap(baseline, "weights"), "repairWeight"),
                    "latencyWeight", geneValue(nestedMap(baseline, "weights"), "latencyWeight"),
                    "recursionWeight", geneValue(nestedMap(baseline, "weights"), "recursionWeight"),
                    "dependencyWeight", geneValue(nestedMap(baseline, "weights"), "dependencyWeight")
            ));
        }
        for (int i = 0; i < evaluated.size() && next.size() < survivors; i++) {
            Map<String, Object> survivor = evaluated.get(i);
            if (String.valueOf(survivor.get("algorithmId")).equals(String.valueOf(baseline.get("algorithmId")))) {
                continue;
            }
            next.add(mapOf(
                    "algorithmId", survivor.get("algorithmId"),
                    "family", survivor.get("family"),
                    "status", "survivor",
                    "generation", nextGeneration,
                    "parentAlgorithmId", survivor.get("algorithmId"),
                    "bruteForceWeight", geneValue(nestedMap(survivor, "weights"), "bruteForceWeight"),
                    "topologyWeight", geneValue(nestedMap(survivor, "weights"), "topologyWeight"),
                    "proofWeight", geneValue(nestedMap(survivor, "weights"), "proofWeight"),
                    "routeWeight", geneValue(nestedMap(survivor, "weights"), "routeWeight"),
                    "performativeWeight", geneValue(nestedMap(survivor, "weights"), "performativeWeight"),
                    "repairWeight", geneValue(nestedMap(survivor, "weights"), "repairWeight"),
                    "latencyWeight", geneValue(nestedMap(survivor, "weights"), "latencyWeight"),
                    "recursionWeight", geneValue(nestedMap(survivor, "weights"), "recursionWeight"),
                    "dependencyWeight", geneValue(nestedMap(survivor, "weights"), "dependencyWeight")
            ));
        }
        while (next.size() < targetSize) {
            Map<String, Object> parent = evaluated.get((next.size() - survivors) % survivors);
            next.add(mutateDarwinAlgorithm(parent, nextGeneration, next.size(), mutationRate, objective));
        }
        return next;
    }

    private static Map<String, Object> mutateDarwinAlgorithm(
            Map<String, Object> parent,
            int nextGeneration,
            int childIndex,
            double mutationRate,
            String objective
    ) {
        Map<String, Object> weights = nestedMap(parent, "weights");
        String parentId = String.valueOf(parent.get("algorithmId"));
        return mapOf(
                "algorithmId", parentId + "_G" + nextGeneration + "_" + childIndex,
                "family", String.valueOf(parent.get("family")) + "_mutant",
                "status", "mutant",
                "generation", nextGeneration,
                "parentAlgorithmId", parentId,
                "bruteForceWeight", darwinMutateGene(weights, "bruteForceWeight", mutationRate, objective, nextGeneration, childIndex),
                "topologyWeight", darwinMutateGene(weights, "topologyWeight", mutationRate, objective, nextGeneration, childIndex),
                "proofWeight", darwinMutateGene(weights, "proofWeight", mutationRate, objective, nextGeneration, childIndex),
                "routeWeight", darwinMutateGene(weights, "routeWeight", mutationRate, objective, nextGeneration, childIndex),
                "performativeWeight", darwinMutateGene(weights, "performativeWeight", mutationRate, objective, nextGeneration, childIndex),
                "repairWeight", darwinMutateGene(weights, "repairWeight", mutationRate, objective, nextGeneration, childIndex),
                "latencyWeight", darwinMutateGene(weights, "latencyWeight", mutationRate, objective, nextGeneration, childIndex),
                "recursionWeight", darwinMutateGene(weights, "recursionWeight", mutationRate, objective, nextGeneration, childIndex),
                "dependencyWeight", darwinMutateGene(weights, "dependencyWeight", mutationRate, objective, nextGeneration, childIndex)
        );
    }

    private static void promoteDarwinWinnerToRegistry(Map<String, Object> winner) throws IOException {
        Map<String, Object> weights = nestedMap(winner, "weights");
        appendMissingSeedRows(DARWIN_ALGORITHM_REGISTRY_LOG, List.of(mapOf(
                "algorithmId", winner.get("algorithmId"),
                "family", winner.get("family"),
                "status", "promoted_winner",
                "generation", winner.getOrDefault("generation", 0),
                "parentAlgorithmId", winner.getOrDefault("parentAlgorithmId", "ROOT"),
                "bruteForceWeight", geneValue(weights, "bruteForceWeight"),
                "topologyWeight", geneValue(weights, "topologyWeight"),
                "proofWeight", geneValue(weights, "proofWeight"),
                "routeWeight", geneValue(weights, "routeWeight"),
                "performativeWeight", geneValue(weights, "performativeWeight"),
                "repairWeight", geneValue(weights, "repairWeight"),
                "latencyWeight", geneValue(weights, "latencyWeight"),
                "recursionWeight", geneValue(weights, "recursionWeight"),
                "dependencyWeight", geneValue(weights, "dependencyWeight"),
                "promotionStatus", winner.getOrDefault("promotionStatus", "baseline_hold"),
                "promotedAt", Instant.now().toString()
        )), "algorithmId");
    }

    private static Map<String, Object> darwinBaselineCandidate(List<Map<String, Object>> candidates) {
        if (candidates.isEmpty()) {
            return Map.of();
        }
        for (Map<String, Object> candidate : candidates) {
            if ("ALG_BRUTE_MATRIX_SEED".equals(String.valueOf(candidate.getOrDefault("algorithmId", "")))) {
                return candidate;
            }
        }
        List<Map<String, Object>> bruteCandidates = new ArrayList<>();
        for (Map<String, Object> candidate : candidates) {
            String family = String.valueOf(candidate.getOrDefault("family", "")).toLowerCase(Locale.ROOT);
            String algorithmId = String.valueOf(candidate.getOrDefault("algorithmId", "")).toLowerCase(Locale.ROOT);
            if (family.contains("bruteforce_matrix_primary") || algorithmId.contains("alg_brute_matrix_seed")) {
                bruteCandidates.add(candidate);
            }
        }
        List<Map<String, Object>> pool = bruteCandidates.isEmpty() ? candidates : bruteCandidates;
        pool.sort(Comparator.comparingDouble((Map<String, Object> candidate) -> numericValue(candidate.get("fitness"))).reversed());
        return pool.get(0);
    }

    private static double darwinMutateGene(
            Map<String, Object> weights,
            String key,
            double mutationRate,
            String objective,
            int generation,
            int childIndex
    ) {
        double base = geneValue(weights, key);
        double centered = (stableRandom01(objective + "|" + key + "|" + generation + "|" + childIndex) - 0.5) * (mutationRate * 2.0);
        return round3(clamp01(base + centered));
    }

    private static double darwinFeatureFit(double weight, double target) {
        return round3(clamp01(1.0 - Math.abs(weight - target)));
    }

    private static Map<String, Object> darwinWeightMap(Map<String, Object> algorithm) {
        return mapOf(
                "bruteForceWeight", round3(numericValue(algorithm.get("bruteForceWeight"))),
                "topologyWeight", round3(numericValue(algorithm.get("topologyWeight"))),
                "proofWeight", round3(numericValue(algorithm.get("proofWeight"))),
                "routeWeight", round3(numericValue(algorithm.get("routeWeight"))),
                "performativeWeight", round3(numericValue(algorithm.get("performativeWeight"))),
                "repairWeight", round3(numericValue(algorithm.get("repairWeight"))),
                "latencyWeight", round3(numericValue(algorithm.get("latencyWeight"))),
                "recursionWeight", round3(numericValue(algorithm.get("recursionWeight"))),
                "dependencyWeight", round3(numericValue(algorithm.get("dependencyWeight")))
        );
    }

    private static List<Object> darwinCompactPopulation(List<Map<String, Object>> population) {
        List<Object> compact = new ArrayList<>();
        for (int i = 0; i < Math.min(5, population.size()); i++) {
            compact.add(darwinCompactAlgorithm(population.get(i)));
        }
        return compact;
    }

    private static Map<String, Object> darwinCompactAlgorithm(Map<String, Object> algorithm) {
        return mapOf(
                "algorithmId", algorithm.get("algorithmId"),
                "family", algorithm.get("family"),
                "fitness", round3(numericValue(algorithm.get("fitness"))),
                "promotionStatus", algorithm.get("promotionStatus"),
                "baselineComparison", algorithm.get("baselineComparison"),
                "metrics", algorithm.get("metrics"),
                "weights", algorithm.get("weights")
        );
    }

    private static List<Object> darwinProgramIds(List<Map<String, Object>> programs) {
        List<Object> ids = new ArrayList<>();
        for (Map<String, Object> program : programs) {
            ids.add(program.get("programId"));
        }
        return ids;
    }

    private static double darwinMedianFitness(List<Map<String, Object>> evaluated) {
        if (evaluated.isEmpty()) {
            return 0.0;
        }
        List<Double> scores = new ArrayList<>();
        for (Map<String, Object> algorithm : evaluated) {
            scores.add(numericValue(algorithm.get("fitness")));
        }
        scores.sort(Double::compareTo);
        return scores.get(scores.size() / 2);
    }

    private static String algebraicCubeFlowDiagram() {
        return """
                START_DATA
                +- compress.card
                +- axiomatic_set_select
                |  +- math_fit
                |  +- logic_fit
                +- transform.chain
                |  +- entry_normalize
                |  +- route_choose
                |  +- model_hook
                +- END_DATA

                flow:
                entry -> compact evidence -> axiomatic fit -> route/model -> proof/code/logic artifact
                """;
    }

    private static String algebraicMathModel() {
        return """
                score = fit(entry, exit, constraints) + proof_weight + reuse_weight - risk_weight
                choose argmax(score)
                if tie then keep bounded top-k and compare with proof

                sets:
                - structure graph
                - evidence proof
                - code reuse
                - behavior markov
                - context anchor
                """;
    }

    private static List<Object> processableDataCards() {
        return List.of(
                mapOf("type", "topology_ascii", "tested", true, "meaning", "ASCII topology trees and epoch cube layouts", "bestFor", "graph routing, structural decomposition"),
                mapOf("type", "benchmark_json", "tested", true, "meaning", "route latency, service timings, benchmark snapshots", "bestFor", "proof scoring, optimization targeting"),
                mapOf("type", "epoch_proposal", "tested", true, "meaning", "problem, evidence, proposed change, acceptance test", "bestFor", "one-variable upgrade planning"),
                mapOf("type", "successful_code_card", "tested", true, "meaning", "distilled success summaries and reusable code intent", "bestFor", "code pattern reuse"),
                mapOf("type", "behavior_card", "tested", true, "meaning", "liked/disliked intent and markov hints", "bestFor", "behavior routing and repair"),
                mapOf("type", "nominal_fact_card", "tested", true, "meaning", "small user profile facts kept separate from behavior cards", "bestFor", "context anchoring without crowding"),
                mapOf("type", "source_tree_card", "tested", true, "meaning", "software file tree, module boundaries, and source locations", "bestFor", "entry and exit point discovery"),
                mapOf("type", "performative_acl_card", "tested", true, "meaning", "performative sender/receiver/action cards", "bestFor", "agent route contracts and performative processing"),
                mapOf("type", "bytecode_signature_card", "tested", true, "meaning", "low-level signature and decompile/compile hook cards", "bestFor", "bytecode-oriented processing plans")
        );
    }

    private static Map<String, Object> algebraicHookupGuide() {
        return mapOf(
                "tinyChooser", "Use for entry classification, purpose extraction, and compact lens drafting.",
                "bridgePlanning", "Use for route plans, proof cards, and multi-step system reasoning.",
                "bridgeBuild", "Use for code pattern cards, reusable block hypotheses, and bounded implementation prompts.",
                "karooComparator", "Use after a candidate pattern exists and needs one-variable ranking or approval gating.",
                "performativeRouter", "Use for ACL/KQML-style action routing, sender/receiver mapping, and software performatives.",
                "bytecodeProcessing", "Use for bounded bytecode, decompile, compile, and signature verification plans.",
                "physicsGridCompare", "Use a bounded 50-grid reference suite to compare software logic against established physics-style processing grids.",
                "physicsEvolutionRefine", "Use a bounded 50-generation refinement pass on the strongest physics family to see which gene weights actually improve alignment.",
                "topologyTemplateGate", "Advance to code-template action items when topological alignment clears the 0.70 gate and attach a race-condition elimination checklist.",
                "testingLabReports", "Use as the proof source for benchmark_json and route evidence inputs.",
                "entryExitRule", "Start data stays compressed, axiomatic set selects math/logic fit, exit target decides route and expected artifact."
        );
    }

    private static Map<String, Object> softwareSearchPolicy() {
        return mapOf(
                "goal", "record software-oriented entry and exit points aggressively but keep execution bounded",
                "entrySearch", List.of("source tree", "routes", "handlers", "performative channels", "bytecode hooks", "benchmarks", "success cards"),
                "exitSearch", List.of("code pattern cards", "logic rules", "performative routes", "bytecode plans", "epoch proposals", "proof cards"),
                "ruleStorage", "store rules, transforms, performatives, and bytecode plans in algebraic flow logs before promotion",
                "processingBias", "software first over generic prose"
        );
    }

    private static List<Object> algebraicRuleRegistry() {
        return List.of(
                mapOf("rule_id", "RULE_ENTRY_EXIT_DISCOVERY", "meaning", "search source and runtime surfaces for bounded software entry/exit points"),
                mapOf("rule_id", "RULE_PERFORMATIVE_CAPTURE", "meaning", "capture performative sender, receiver, action, and route contract before processing"),
                mapOf("rule_id", "RULE_BYTECODE_PLAN", "meaning", "record bytecode or decompile plan as a bounded verification path"),
                mapOf("rule_id", "RULE_AXIOMATIC_FIT", "meaning", "choose the smallest math and logic set that fits the software artifact"),
                mapOf("rule_id", "RULE_PROOF_FIRST", "meaning", "log rules and proof artifacts before future promotion"),
                mapOf("rule_id", "RULE_LAYERED_BLOCK_SCALE", "meaning", "tag candidates as small, medium, or large blocks before chooser scoring"),
                mapOf("rule_id", "RULE_STOCHASTIC_TOPK", "meaning", "sample inside a trusted top-k band instead of pretending one deterministic pick is always enough"),
                mapOf("rule_id", "RULE_PHYSICS_GRID_ALIGNMENT", "meaning", "compare candidate software logic against established physics-style grids and log where lineups occur"),
                mapOf("rule_id", "RULE_PHYSICS_GENETIC_REFINEMENT", "meaning", "mutate gene weights for 50 bounded generations and keep an accepted lineage for the best-aligned physics family"),
                mapOf("rule_id", "RULE_TOPOLOGY_TEMPLATE_GATE", "meaning", "if topological alignment reaches at least 0.70, emit code-template action items plus race-condition elimination tasks")
        );
    }

    private static List<Object> layeredDbModel() {
        return List.of(
                mapOf("layer", "small_block", "examples", List.of("bytecode signatures", "handlers", "functions", "performative actions"), "chooserBias", "prefer when entry and exit are both narrow and low-risk"),
                mapOf("layer", "medium_block", "examples", List.of("modules", "code pattern cards", "logic pattern cards", "route plans"), "chooserBias", "prefer for most reusable software workflows"),
                mapOf("layer", "large_block", "examples", List.of("topology grids", "subsystem orchestrations", "epoch proposals"), "chooserBias", "prefer only when the ask spans many modules or proof surfaces")
        );
    }

    private static Map<String, Object> algebraicBlockScale(String entry, String exit) {
        String entryScale = switch (entry) {
            case "bytecode_signature_card", "behavior_card", "nominal_fact_card" -> "small_block";
            case "source_tree_card", "performative_acl_card", "successful_code_card", "benchmark_json" -> "medium_block";
            case "topology_ascii", "epoch_proposal" -> "large_block";
            default -> "medium_block";
        };
        String exitScale = switch (exit) {
            case "proof_card", "bytecode_plan_card" -> "small_block";
            case "code_pattern_card", "logic_pattern_card", "route_plan", "performative_route_card" -> "medium_block";
            case "epoch_proposal" -> "large_block";
            default -> "medium_block";
        };
        String transition = entryScale + "_to_" + exitScale;
        String layerHint = entryScale.equals(exitScale) ? "same_layer" : "cross_layer";
        return mapOf(
                "entryScale", entryScale,
                "exitScale", exitScale,
                "transition", transition,
                "layerHint", layerHint
        );
    }

    private static Map<String, Object> algebraicChooserScorecard(
            String entry,
            String exit,
            Map<String, Object> axioms,
            Map<String, Object> blockScale
    ) {
        double blockFit = blockFitScore(String.valueOf(blockScale.get("entryScale")), String.valueOf(blockScale.get("exitScale")));
        double entryExitAffinity = entryExitAffinity(entry, exit);
        double performativeConfidence = performativeConfidence(entry, exit);
        double bytecodeConfidence = bytecodeConfidence(entry, exit);
        Map<String, Object> history = chooserHistory(entry, exit);
        double proofHistory = (double) history.get("proofHistory");
        double timeoutRisk = (double) history.get("timeoutRisk");
        double total = clamp01(
                (0.28 * blockFit)
                        + (0.22 * entryExitAffinity)
                        + (0.15 * performativeConfidence)
                        + (0.10 * bytecodeConfidence)
                        + (0.20 * proofHistory)
                        - (0.15 * timeoutRisk)
        );
        return mapOf(
                "blockFit", round3(blockFit),
                "entryExitAffinity", round3(entryExitAffinity),
                "performativeConfidence", round3(performativeConfidence),
                "bytecodeConfidence", round3(bytecodeConfidence),
                "proofHistory", round3(proofHistory),
                "timeoutRisk", round3(timeoutRisk),
                "recommendedHook", axioms.get("recommendedModelHook"),
                "totalScore", round3(total)
        );
    }

    private static Map<String, Object> chooserHistory(String entry, String exit) {
        int pass = 0;
        int fail = 0;
        int timeout = 0;
        int observed = 0;
        String pattern = Pattern.quote("\"entryPoint\":\"" + entry + "\"") + ".*?" + Pattern.quote("\"exitPoint\":\"" + exit + "\"") + ".*?\"probe\":\\{\"status\":\"(pass|fail|skipped)\".*?(?:\"error\":\"([^\"]*)\")?";
        Pattern regex = Pattern.compile(pattern);
        for (String line : readJsonLines(ALGEBRAIC_FLOW_LOG, 40)) {
            Matcher matcher = regex.matcher(line);
            while (matcher.find()) {
                observed++;
                String status = matcher.group(1);
                String error = matcher.groupCount() >= 2 ? matcher.group(2) : null;
                if ("pass".equals(status)) {
                    pass++;
                } else if ("fail".equals(status)) {
                    fail++;
                }
                if (error != null && error.contains("Timeout")) {
                    timeout++;
                }
            }
        }
        double proofHistory = observed == 0 ? 0.55 : ((pass + 0.5) / (observed + 1.0));
        double timeoutRisk = observed == 0 ? 0.15 : (timeout / (double) observed);
        return mapOf(
                "observed", observed,
                "passCount", pass,
                "failCount", fail,
                "timeoutCount", timeout,
                "proofHistory", clamp01(proofHistory),
                "timeoutRisk", clamp01(timeoutRisk)
        );
    }

    private static double blockFitScore(String entryScale, String exitScale) {
        if (entryScale.equals(exitScale)) {
            return 1.0;
        }
        if (("small_block".equals(entryScale) && "medium_block".equals(exitScale))
                || ("medium_block".equals(entryScale) && "small_block".equals(exitScale))
                || ("medium_block".equals(entryScale) && "large_block".equals(exitScale))
                || ("large_block".equals(entryScale) && "medium_block".equals(exitScale))) {
            return 0.8;
        }
        return 0.45;
    }

    private static double entryExitAffinity(String entry, String exit) {
        if ("source_tree_card".equals(entry) && "performative_route_card".equals(exit)) return 0.9;
        if ("source_tree_card".equals(entry) && "code_pattern_card".equals(exit)) return 0.88;
        if ("performative_acl_card".equals(entry) && "performative_route_card".equals(exit)) return 0.95;
        if ("performative_acl_card".equals(entry) && "bytecode_plan_card".equals(exit)) return 0.68;
        if ("bytecode_signature_card".equals(entry) && "bytecode_plan_card".equals(exit)) return 0.94;
        if ("benchmark_json".equals(entry) && "epoch_proposal".equals(exit)) return 0.86;
        if ("behavior_card".equals(entry) && "route_plan".equals(exit)) return 0.9;
        if ("topology_ascii".equals(entry) && "code_pattern_card".equals(exit)) return 0.82;
        return 0.56;
    }

    private static double performativeConfidence(String entry, String exit) {
        if ("performative_acl_card".equals(entry) || "performative_route_card".equals(exit)) return 0.95;
        if ("source_tree_card".equals(entry) && "performative_route_card".equals(exit)) return 0.78;
        return 0.42;
    }

    private static double bytecodeConfidence(String entry, String exit) {
        if ("bytecode_signature_card".equals(entry) || "bytecode_plan_card".equals(exit)) return 0.94;
        if ("source_tree_card".equals(entry)) return 0.58;
        return 0.3;
    }

    private static Map<String, Object> chooserComparison(List<Object> permutations, String objective, String requestedEntry, String requestedExit) {
        List<Map<String, Object>> candidates = new ArrayList<>();
        for (Object item : permutations) {
            if (item instanceof Map<?, ?> raw) {
                @SuppressWarnings("unchecked")
                Map<String, Object> candidate = (Map<String, Object>) raw;
                candidates.add(candidate);
            }
        }
        candidates.sort(Comparator.comparingDouble(ViperLabSuiteServer::candidateScore).reversed());
        if (candidates.isEmpty()) {
            return mapOf("status", "no_candidates");
        }
        Map<String, Object> deterministic = chooserPickView(candidates.get(0), "deterministic_top1");
        double best = candidateScore(candidates.get(0));
        List<Map<String, Object>> topBand = new ArrayList<>();
        for (Map<String, Object> candidate : candidates) {
            if (topBand.size() >= 3) break;
            if (best - candidateScore(candidate) <= 0.12) {
                topBand.add(candidate);
            }
        }
        if (topBand.isEmpty()) {
            topBand.add(candidates.get(0));
        }
        double random01 = stableRandom01(objective + "|" + requestedEntry + "|" + requestedExit + "|" + best);
        Map<String, Object> stochastic = stochasticPick(topBand, random01);
        List<Object> topK = new ArrayList<>();
        for (Map<String, Object> candidate : topBand) {
            topK.add(chooserPickView(candidate, "top_k_candidate"));
        }
        return mapOf(
                "strategy", "deterministic_vs_bounded_topk_stochastic",
                "deterministicPick", deterministic,
                "topK", topK,
                "stochasticPick", stochastic,
                "bandThreshold", 0.12,
                "random01", round3(random01)
        );
    }

    private static Map<String, Object> chooserABExperiment(
            List<Object> permutations,
            String objective,
            String requestedEntry,
            String requestedExit,
            String chooserExperiment,
            boolean includeModelProbes
    ) {
        if (!"predictive_vs_bruteforce".equalsIgnoreCase(chooserExperiment)) {
            return mapOf("status", "not_requested");
        }
        List<Map<String, Object>> candidates = new ArrayList<>();
        for (Object item : permutations) {
            if (item instanceof Map<?, ?> raw) {
                @SuppressWarnings("unchecked")
                Map<String, Object> candidate = (Map<String, Object>) raw;
                candidates.add(candidate);
            }
        }
        candidates.sort(Comparator.comparingDouble(ViperLabSuiteServer::candidateScore).reversed());
        if (candidates.isEmpty()) {
            return mapOf("status", "no_candidates");
        }

        List<Map<String, Object>> bruteWindow = new ArrayList<>(candidates.subList(0, Math.min(10, candidates.size())));
        List<Object> bruteTrace = new ArrayList<>();
        Map<String, Object> bruteWinner = null;
        int bruteInspected = 0;
        for (Map<String, Object> candidate : bruteWindow) {
            bruteInspected++;
            String probeStatus = probeStatus(candidate);
            bruteTrace.add(mapOf(
                    "rank", bruteInspected,
                    "entryPoint", candidate.get("entryPoint"),
                    "exitPoint", candidate.get("exitPoint"),
                    "totalScore", round3(candidateScore(candidate)),
                    "probeStatus", probeStatus
            ));
            if (bruteWinner == null && !"fail".equals(probeStatus)) {
                bruteWinner = candidate;
                if ("pass".equals(probeStatus)) {
                    break;
                }
            }
        }
        if (bruteWinner == null) {
            bruteWinner = bruteWindow.get(0);
        }

        List<Map<String, Object>> predicted = new ArrayList<>(candidates);
        predicted.sort(Comparator.comparingDouble((Map<String, Object> candidate) ->
                predictiveHybridScore(candidate, requestedEntry, requestedExit, objective)).reversed());
        List<Map<String, Object>> predictedWindow = new ArrayList<>(predicted.subList(0, Math.min(3, predicted.size())));
        List<Object> hybridTrace = new ArrayList<>();
        Map<String, Object> hybridWinner = null;
        int hybridInspected = 0;
        for (Map<String, Object> candidate : predictedWindow) {
            hybridInspected++;
            String probeStatus = probeStatus(candidate);
            double predictiveScore = predictiveHybridScore(candidate, requestedEntry, requestedExit, objective);
            hybridTrace.add(mapOf(
                        "phase", "predictive_shortlist",
                        "rank", hybridInspected,
                        "entryPoint", candidate.get("entryPoint"),
                        "exitPoint", candidate.get("exitPoint"),
                        "predictiveScore", round3(predictiveScore),
                        "probeStatus", probeStatus
            ));
            if (hybridWinner == null && ("pass".equals(probeStatus) || (!includeModelProbes && !"fail".equals(probeStatus)))) {
                hybridWinner = candidate;
                break;
            }
        }
        if (hybridWinner == null) {
            for (Map<String, Object> candidate : bruteWindow) {
                if (predictedWindow.contains(candidate)) {
                    continue;
                }
                hybridInspected++;
                String probeStatus = probeStatus(candidate);
                hybridTrace.add(mapOf(
                        "phase", "bounded_bruteforce_fallback",
                        "rank", hybridInspected,
                        "entryPoint", candidate.get("entryPoint"),
                        "exitPoint", candidate.get("exitPoint"),
                        "totalScore", round3(candidateScore(candidate)),
                        "probeStatus", probeStatus
                ));
                if (!"fail".equals(probeStatus)) {
                    hybridWinner = candidate;
                    if ("pass".equals(probeStatus)) {
                        break;
                    }
                }
                if (hybridInspected >= 5) {
                    break;
                }
            }
        }
        if (hybridWinner == null) {
            hybridWinner = predictedWindow.get(0);
        }

        double bruteScore = candidateScore(bruteWinner);
        double hybridScore = candidateScore(hybridWinner);
        boolean sameWinner = String.valueOf(bruteWinner.get("entryPoint")).equals(String.valueOf(hybridWinner.get("entryPoint")))
                && String.valueOf(bruteWinner.get("exitPoint")).equals(String.valueOf(hybridWinner.get("exitPoint")));
        int estimatedSaved = Math.max(0, bruteInspected - hybridInspected);
        String recommendation;
        if (sameWinner && hybridInspected < bruteInspected) {
            recommendation = "prefer_predictive_hybrid";
        } else if (hybridScore >= bruteScore - 0.03 && hybridInspected < bruteInspected) {
            recommendation = "prefer_predictive_hybrid";
        } else if (bruteScore > hybridScore + 0.05) {
            recommendation = "prefer_bounded_bruteforce";
        } else {
            recommendation = "keep_both_and_measure_more";
        }

        return mapOf(
                "status", "generated",
                "strategy", "top10_small_variable_bruteforce_vs_predictive_then_bounded_bruteforce",
                "entryAnchor", requestedEntry,
                "exitAnchor", requestedExit,
                "objective", objective,
                "bruteforceLane", mapOf(
                        "candidateWindow", bruteWindow.size(),
                        "inspected", bruteInspected,
                        "winner", chooserPickView(bruteWinner, "top10_small_variable_bruteforce"),
                        "trace", bruteTrace
                ),
                "predictiveHybridLane", mapOf(
                "candidateWindow", predictedWindow.size(),
                "inspected", hybridInspected,
                "winner", chooserPickView(hybridWinner, "predictive_then_bounded_bruteforce"),
                "trace", hybridTrace
                ),
                "comparison", mapOf(
                        "estimatedCandidateSaves", estimatedSaved,
                        "sameWinner", sameWinner,
                        "bruteforceScore", round3(bruteScore),
                        "predictiveHybridScore", round3(hybridScore),
                        "recommendation", recommendation
                ),
                "guard", "Bounded experiment only. Uses current proof surfaces and falls back before promotion."
        );
    }

    private static Map<String, Object> stochasticPick(List<Map<String, Object>> candidates, double random01) {
        double total = 0.0;
        for (Map<String, Object> candidate : candidates) {
            total += Math.max(0.01, candidateScore(candidate));
        }
        double cursor = random01 * total;
        for (Map<String, Object> candidate : candidates) {
            cursor -= Math.max(0.01, candidateScore(candidate));
            if (cursor <= 0) {
                return chooserPickView(candidate, "bounded_topk_stochastic");
            }
        }
        return chooserPickView(candidates.get(candidates.size() - 1), "bounded_topk_stochastic");
    }

    private static Map<String, Object> chooserPickView(Map<String, Object> candidate, String chooserMode) {
        Map<String, Object> scorecard = nestedMap(candidate, "chooserScorecard");
        Map<String, Object> blockScale = nestedMap(candidate, "blockScale");
        return mapOf(
                "chooserMode", chooserMode,
                "entryPoint", candidate.get("entryPoint"),
                "exitPoint", candidate.get("exitPoint"),
                "entryScale", blockScale.get("entryScale"),
                "exitScale", blockScale.get("exitScale"),
                "recommendedRoute", candidate.get("recommendedRoute"),
                "recommendedModelHook", candidate.get("recommendedModelHook"),
                "totalScore", scorecard.get("totalScore")
        );
    }

    private static Map<String, Object> nestedMap(Map<String, Object> parent, String key) {
        Object value = parent.get(key);
        if (value instanceof Map<?, ?> raw) {
            @SuppressWarnings("unchecked")
            Map<String, Object> nested = (Map<String, Object>) raw;
            return nested;
        }
        return Map.of();
    }

    private static double candidateScore(Map<String, Object> candidate) {
        Map<String, Object> scorecard = nestedMap(candidate, "chooserScorecard");
        Object value = scorecard.get("totalScore");
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return 0.0;
    }

    private static String probeStatus(Map<String, Object> candidate) {
        Map<String, Object> probe = nestedMap(candidate, "probe");
        return String.valueOf(probe.getOrDefault("status", "skipped"));
    }

    private static double predictiveHybridScore(
            Map<String, Object> candidate,
            String requestedEntry,
            String requestedExit,
            String objective
    ) {
        Map<String, Object> scorecard = nestedMap(candidate, "chooserScorecard");
        String entryPoint = String.valueOf(candidate.getOrDefault("entryPoint", ""));
        String exitPoint = String.valueOf(candidate.getOrDefault("exitPoint", ""));
        String recommendedRoute = String.valueOf(candidate.getOrDefault("recommendedRoute", ""));
        String recommendedHook = String.valueOf(candidate.getOrDefault("recommendedModelHook", ""));
        String loweredObjective = objective == null ? "" : objective.toLowerCase();
        double total = numericValue(scorecard.get("totalScore"));
        double proof = numericValue(scorecard.get("proofHistory"));
        double timeout = numericValue(scorecard.get("timeoutRisk"));
        double performative = numericValue(scorecard.get("performativeConfidence"));
        double entryAnchor = entryPoint.equals(requestedEntry) ? 1.0 : 0.0;
        double exitAnchor = exitPoint.equals(requestedExit) ? 1.0 : 0.0;
        double topologyPrior = 0.0;
        if (loweredObjective.contains("topology") || loweredObjective.contains("route")) {
            if ("source_tree_card".equals(entryPoint)) {
                topologyPrior += 0.16;
            } else if ("topology_ascii".equals(entryPoint)) {
                topologyPrior += 0.08;
            }
            if ("performative_route_card".equals(exitPoint) || "route_plan".equals(exitPoint)) {
                topologyPrior += 0.08;
            }
        }
        double routePrior = 0.0;
        if (loweredObjective.contains("routing") || loweredObjective.contains("route")) {
            if ("planning".equals(recommendedRoute)) {
                routePrior += 0.08;
            }
            if ("performativeRouter".equals(recommendedHook)) {
                routePrior += 0.1;
            }
        }
        return round3(clamp01(
                (0.34 * total)
                        + (0.10 * proof)
                        + (0.10 * performative)
                        + (0.16 * entryAnchor)
                        + (0.14 * exitAnchor)
                        + topologyPrior
                        + routePrior
                        - (0.16 * timeout)
        ));
    }

    private static double stableRandom01(String seed) {
        String hash = sha256(seed);
        long bits = Long.parseUnsignedLong(hash.substring(0, 12), 16);
        double max = 0xFFFFFFFFFFFFL;
        return bits / max;
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static double round3(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private static Map<String, Object> physicsComparisonSuite(
            List<Object> permutations,
            String objective,
            String comparisonFamily,
            int requestedComparisons
    ) {
        boolean shouldRun = requestedComparisons > 0 || objective.toLowerCase().contains("physics");
        if (!shouldRun) {
            return mapOf("status", "not_requested");
        }
        List<Map<String, Object>> candidates = castPermutationCandidates(permutations);
        if (candidates.isEmpty()) {
            return mapOf("status", "no_candidates");
        }
        List<Map<String, Object>> comparisons = new ArrayList<>();
        int strong = 0;
        int partial = 0;
        int weak = 0;
        for (Map<String, Object> physicsGrid : physicsGridCatalog(Math.max(1, requestedComparisons == 0 ? 50 : requestedComparisons))) {
            Map<String, Object> match = physicsGridComparison(physicsGrid, candidates);
            comparisons.add(match);
            String alignmentLevel = String.valueOf(match.get("alignmentLevel"));
            if ("strong".equals(alignmentLevel)) {
                strong++;
            } else if ("partial".equals(alignmentLevel)) {
                partial++;
            } else {
                weak++;
            }
        }
        List<Map<String, Object>> sorted = new ArrayList<>(comparisons);
        sorted.sort(Comparator.comparingDouble(ViperLabSuiteServer::physicsMatchScore).reversed());
        List<Object> topMatches = new ArrayList<>();
        for (int i = 0; i < Math.min(10, sorted.size()); i++) {
            Map<String, Object> match = sorted.get(i);
            topMatches.add(mapOf(
                    "gridId", match.get("gridId"),
                    "family", match.get("family"),
                    "matchScore", match.get("matchScore"),
                    "alignmentLevel", match.get("alignmentLevel"),
                    "logicLineup", match.get("logicLineup"),
                    "bestPermutation", match.get("bestPermutation")
            ));
        }
        return mapOf(
                "status", "generated",
                "comparisonFamily", comparisonFamily,
                "comparisonCount", comparisons.size(),
                "lineupAtAnyLevel", strong > 0 || partial > 0,
                "alignmentBands", mapOf(
                        "strong", strong,
                        "partial", partial,
                        "weak", weak
                ),
                "topMatches", topMatches,
                "comparisons", comparisons
        );
    }

    private static Map<String, Object> physicsEvolutionRefinement(
            Map<String, Object> physicsComparisonSuite,
            List<Object> permutations,
            String objective,
            int rounds
    ) {
        if (rounds <= 0) {
            return mapOf("status", "not_requested");
        }
        List<Map<String, Object>> candidates = castPermutationCandidates(permutations);
        List<Map<String, Object>> comparisons = nestedObjectList(physicsComparisonSuite, "comparisons");
        if (candidates.isEmpty()) {
            return mapOf("status", "no_candidates");
        }
        if (comparisons.isEmpty()) {
            return mapOf("status", "no_seed_comparison");
        }
        comparisons.sort(Comparator.comparingDouble(ViperLabSuiteServer::physicsMatchScore).reversed());
        Map<String, Object> seed = comparisons.get(0);
        String focusFamily = String.valueOf(seed.get("family"));
        List<Map<String, Object>> familyGrids = new ArrayList<>();
        for (Map<String, Object> grid : physicsGridCatalog(50)) {
            if (focusFamily.equals(String.valueOf(grid.get("family")))) {
                familyGrids.add(grid);
            }
        }
        if (familyGrids.isEmpty()) {
            return mapOf("status", "no_family_grids", "focusFamily", focusFamily);
        }
        Map<String, Double> currentWeights = baseGeneWeights();
        Map<String, Object> currentBest = evaluateEvolutionRound(candidates, familyGrids, currentWeights);
        currentBest.put("weights", currentWeights);
        Map<String, Object> globalBest = currentBest;
        List<Object> lineage = new ArrayList<>();
        int accepted = 0;
        int improved = 0;
        for (int generation = 1; generation <= rounds; generation++) {
            Map<String, Object> currentSoftwareGenes = nestedMap(currentBest, "softwareGenes");
            Map<String, Object> currentPhysicsGenes = nestedMap(currentBest, "physicsGenes");
            List<String> focusGenes = topMismatchGenes(currentSoftwareGenes, currentPhysicsGenes, 2);
            Map<String, Double> mutatedWeights = mutateGeneWeights(currentWeights, objective, generation, focusGenes);
            Map<String, Object> trial = evaluateEvolutionRound(candidates, familyGrids, mutatedWeights);
            trial.put("weights", mutatedWeights);
            double currentScore = numericValue(currentBest.get("matchScore"));
            double trialScore = numericValue(trial.get("matchScore"));
            double tolerance = Math.max(0.006, (0.055 - (generation * 0.0007)));
            boolean acceptedRound = trialScore > currentScore || (currentScore - trialScore <= tolerance && stableRandom01(objective + "|physics_accept|" + generation) > 0.86);
            if (acceptedRound) {
                currentBest = trial;
                currentWeights = mutatedWeights;
                accepted++;
                if (trialScore > numericValue(globalBest.get("matchScore"))) {
                    globalBest = trial;
                    improved++;
                }
            }
            lineage.add(mapOf(
                    "generation", generation,
                    "accepted", acceptedRound,
                    "tolerance", round3(tolerance),
                    "mutationFocus", focusGenes,
                    "trialGrid", trial.get("gridId"),
                    "trialFamily", trial.get("family"),
                    "trialScore", trial.get("matchScore"),
                    "currentChampionGrid", currentBest.get("gridId"),
                    "currentChampionScore", currentBest.get("matchScore")
            ));
        }
        double startScore = numericValue(seed.get("matchScore"));
        double finalScore = numericValue(globalBest.get("matchScore"));
        return mapOf(
                "status", "generated",
                "rounds", rounds,
                "seedFamily", focusFamily,
                "seedGrid", seed.get("gridId"),
                "seedScore", seed.get("matchScore"),
                "seedBestPermutation", seed.get("bestPermutation"),
                "finalGrid", globalBest.get("gridId"),
                "finalFamily", globalBest.get("family"),
                "finalScore", globalBest.get("matchScore"),
                "improvement", round3(finalScore - startScore),
                "acceptedRounds", accepted,
                "improvedRounds", improved,
                "dominantGenes", topWeightGenes((Map<String, Double>) globalBest.get("weights"), 4),
                "bestPermutation", chooserPickView(nestedMap(globalBest, "candidate"), "physics_genetic_refined"),
                "finalSoftwareGenes", nestedMap(globalBest, "softwareGenes"),
                "finalPhysicsGenes", nestedMap(globalBest, "physicsGenes"),
                "lineage", lineage
        );
    }

    private static Map<String, Object> topologicalTemplatePromotion(
            List<Object> permutations,
            Map<String, Object> physicsComparisonSuite,
            Map<String, Object> physicsEvolutionRefinement
    ) {
        double advanceFloor = 0.70;
        double optimizeTarget = 0.95;
        Map<String, Object> bestPermutation = Map.of();
        double alignmentScore = 0.0;
        String evidenceSource = "none";
        if ("generated".equals(String.valueOf(physicsEvolutionRefinement.get("status")))) {
            alignmentScore = numericValue(physicsEvolutionRefinement.get("finalScore"));
            bestPermutation = nestedMap(physicsEvolutionRefinement, "bestPermutation");
            evidenceSource = "physicsEvolutionRefinement";
        } else if ("generated".equals(String.valueOf(physicsComparisonSuite.get("status")))) {
            List<Map<String, Object>> topMatches = nestedObjectList(physicsComparisonSuite, "topMatches");
            if (!topMatches.isEmpty()) {
                Map<String, Object> top = topMatches.get(0);
                alignmentScore = numericValue(top.get("matchScore"));
                bestPermutation = nestedMap(top, "bestPermutation");
                evidenceSource = "physicsComparisonSuite";
            }
        }
        String entryPoint = String.valueOf(bestPermutation.getOrDefault("entryPoint", ""));
        boolean topologyAnchored = "source_tree_card".equals(entryPoint) || "topology_ascii".equals(entryPoint);
        boolean advance = topologyAnchored && alignmentScore >= advanceFloor;
        boolean targetReady = topologyAnchored && alignmentScore >= optimizeTarget;
        List<Object> actionItems = new ArrayList<>();
        if (targetReady) {
            actionItems.add("Freeze the current topological winner as a target-grade code-template seed card.");
            actionItems.add("Emit the first bounded file-template wave page by page from the winner.");
            actionItems.add("Keep helper-bit repair lanes optional and post-proof only.");
            actionItems.add("Run Karoo only after the base template wave is emitted and checked.");
            actionItems.add("Distill the promoted topology/template lineage into DB cards for low-CPU reuse.");
        } else if (advance) {
            actionItems.add("Freeze the current topological winner as a code-template seed card.");
            actionItems.add("Emit a bounded code-template draft from the winning permutation before any code mutation.");
            actionItems.add("Run deterministic vs bounded stochastic chooser replay against the same topology seed.");
            actionItems.add("Require a compile-or-parse proof on the template before promotion.");
            actionItems.add("Attach Karoo compare and epoch proof artifacts to the template lane.");
        } else {
            actionItems.add("Keep the topology in analysis mode until alignment clears 0.70.");
            actionItems.add("Prefer more source-tree and topology_ascii entries before template promotion.");
            actionItems.add("Do not call the lane optimized until it approaches 0.95.");
        }
        List<Object> raceConditionItems = List.of(
                mapOf("risk", "sqlite_lock_contention", "lane", "bridge and lens writes", "action", "serialize write-heavy template promotion steps and keep read-first proofs"),
                mapOf("risk", "listener_overlap", "lane", "8080/18081/18181 concurrent control paths", "action", "treat template promotion as one bounded lane with explicit ownership"),
                mapOf("risk", "jsonl_append_interleave", "lane", "proof and persistence logs", "action", "append promotion artifacts in one ordered batch per generation"),
                mapOf("risk", "stale_pid_or_tunnel_state", "lane", "sidecar and relay surfaces", "action", "exclude stale PID/tunnel artifacts from template promotion triggers"),
                mapOf("risk", "chooser_race", "lane", "deterministic and stochastic pickers", "action", "record both picks first, then promote only one winner after proof")
        );
        return mapOf(
                "status", targetReady ? "target_grade_template_ready" : advance ? "advance_to_code_template" : "hold_for_more_alignment",
                "advanceFloor", advanceFloor,
                "optimizeTarget", optimizeTarget,
                "alignmentScore", round3(alignmentScore),
                "topologyAnchored", topologyAnchored,
                "targetReady", targetReady,
                "evidenceSource", evidenceSource,
                "bestPermutation", bestPermutation,
                "actionItems", actionItems,
                "raceConditionItems", raceConditionItems
        );
    }

    private static List<Map<String, Object>> castPermutationCandidates(List<Object> permutations) {
        List<Map<String, Object>> candidates = new ArrayList<>();
        for (Object item : permutations) {
            if (item instanceof Map<?, ?> raw) {
                @SuppressWarnings("unchecked")
                Map<String, Object> candidate = (Map<String, Object>) raw;
                candidates.add(candidate);
            }
        }
        candidates.sort(Comparator.comparingDouble(ViperLabSuiteServer::candidateScore).reversed());
        return candidates;
    }

    private static List<Map<String, Object>> physicsGridCatalog(int limit) {
        String[][] families = {
                {"statistical_mechanics", "ising_square_lattice", "ising_triangular_lattice", "spin_glass_energy_grid", "percolation_square_grid", "monte_carlo_lattice"},
                {"pde_stencil", "heat_equation_5_point", "wave_equation_9_point", "advection_diffusion_grid", "poisson_relaxation_grid", "phase_field_grid"},
                {"finite_element", "triangular_mesh_fem", "tetrahedral_mesh_fem", "boundary_element_strip", "spring_mass_lattice", "peridynamics_bond_graph"},
                {"lattice_boltzmann", "d2q9_flow_lattice", "d3q19_flow_lattice", "collision_stream_grid", "channel_boundary_lattice", "vortex_shedding_grid"},
                {"tensor_multiscale", "tensor_network_mps", "tensor_network_peps", "renormalization_block_spin", "multigrid_v_cycle", "hierarchical_coarse_fine_grid"},
                {"graph_field", "graph_laplacian_field", "belief_propagation_factor_graph", "causal_spacetime_graph", "constraint_satisfaction_lattice", "contact_graph_discrete_elements"},
                {"swarm_network", "agent_swarm_grid", "kuramoto_sync_network", "reaction_network_stoichiometric_matrix", "compartment_ode_network", "reservoir_wave_grid"},
                {"quantum_lattice", "quantum_walk_lattice", "path_integral_lattice", "topological_edge_lattice", "interference_phase_grid", "spin_chain_transfer_grid"},
                {"continuum_field", "reaction_diffusion_grid", "level_set_front_grid", "smoothed_particle_hydrodynamics", "molecular_dynamics_neighbor_grid", "neural_field_continuum"},
                {"tree_transport", "barnes_hut_tree_grid", "transport_upwind_mesh", "finite_volume_conservation_grid", "boltzmann_collision_stream", "hidden_markov_chain"}
        };
        List<Map<String, Object>> grids = new ArrayList<>();
        for (String[] familyRow : families) {
            String family = familyRow[0];
            for (int i = 1; i < familyRow.length; i++) {
                String design = familyRow[i];
                grids.add(mapOf(
                        "gridId", design,
                        "family", family,
                        "establishedMath", physicsEstablishedMath(family, design),
                        "establishedLogic", physicsEstablishedLogic(family, design),
                        "processingGrid", physicsProcessingGrid(family, design),
                        "genes", physicsGeneModel(family, design)
                ));
                if (grids.size() >= limit) {
                    return grids;
                }
            }
        }
        return grids;
    }

    private static Map<String, Double> baseGeneWeights() {
        Map<String, Double> weights = new LinkedHashMap<>();
        weights.put("structureDensity", 0.18);
        weights.put("stochasticity", 0.12);
        weights.put("locality", 0.18);
        weights.put("multiscale", 0.14);
        weights.put("conservation", 0.14);
        weights.put("messagePassing", 0.12);
        weights.put("lowLevelSignature", 0.06);
        weights.put("energyOptimization", 0.06);
        return weights;
    }

    private static String physicsEstablishedMath(String family, String design) {
        return switch (family) {
            case "statistical_mechanics" -> "local energy weighting, neighborhood state transitions, and partition-style scoring";
            case "pde_stencil" -> "finite-difference stencil updates over bounded neighborhoods";
            case "finite_element" -> "mesh basis assembly with boundary and continuity constraints";
            case "lattice_boltzmann" -> "collision-stream lattice updates with conservation-focused local flows";
            case "tensor_multiscale" -> "coarse-to-fine factorization, contraction, and renormalized scale reduction";
            case "graph_field" -> "graph adjacency, factor propagation, and discrete field constraints";
            case "swarm_network" -> "stochastic coordination with local state exchange and collective convergence";
            case "quantum_lattice" -> "state amplitude evolution over discrete paths and interference constraints";
            case "continuum_field" -> "field evolution with front propagation, diffusion, or particle-neighborhood coupling";
            case "tree_transport" -> "hierarchical transport, conservation, and path-routing aggregation";
            default -> "bounded weighted comparison";
        };
    }

    private static String physicsEstablishedLogic(String family, String design) {
        return switch (family) {
            case "statistical_mechanics" -> "logic lines up through local consistency, reversible scoring pressure, and bounded stochastic repair";
            case "pde_stencil" -> "logic lines up through neighbor updates, boundary conditions, and stable propagation";
            case "finite_element" -> "logic lines up through mesh-local responsibility blocks and constrained assembly";
            case "lattice_boltzmann" -> "logic lines up through local routing plus conservation-aware transitions";
            case "tensor_multiscale" -> "logic lines up through compressed blocks, multiscale reuse, and coarse-to-fine proof";
            case "graph_field" -> "logic lines up through adjacency reasoning, message passing, and graph-bound proofs";
            case "swarm_network" -> "logic lines up through stochastic choosers, performative exchange, and bounded convergence";
            case "quantum_lattice" -> "logic lines up through path competition, signature sensitivity, and state selection";
            case "continuum_field" -> "logic lines up through evolving field states, diffusion-like retrieval, and front tracking";
            case "tree_transport" -> "logic lines up through hierarchical routing, transport edges, and aggregated proof flow";
            default -> "bounded lineage comparison";
        };
    }

    private static String physicsProcessingGrid(String family, String design) {
        return switch (family) {
            case "statistical_mechanics" -> "discrete lattice with neighborhood state rules";
            case "pde_stencil" -> "fixed stencil grid with directional updates";
            case "finite_element" -> "mesh cells and boundary surfaces";
            case "lattice_boltzmann" -> "velocity-channel lattice";
            case "tensor_multiscale" -> "hierarchical contraction grid";
            case "graph_field" -> "factor or adjacency graph";
            case "swarm_network" -> "agent network with local exchanges";
            case "quantum_lattice" -> "path lattice with state amplitudes";
            case "continuum_field" -> "field cells with front or particle coupling";
            case "tree_transport" -> "tree or transport grid";
            default -> design;
        };
    }

    private static Map<String, Object> physicsGeneModel(String family, String design) {
        double structure = 0.7;
        double stochasticity = 0.2;
        double locality = 0.7;
        double multiscale = 0.45;
        double conservation = 0.55;
        double message = 0.25;
        double lowLevel = 0.2;
        double energy = 0.55;
        switch (family) {
            case "statistical_mechanics" -> {
                structure = 0.78; stochasticity = 0.72; locality = 0.9; multiscale = 0.48; conservation = 0.62; message = 0.2; lowLevel = 0.22; energy = 0.9;
            }
            case "pde_stencil" -> {
                structure = 0.76; stochasticity = 0.18; locality = 0.95; multiscale = 0.56; conservation = 0.86; message = 0.15; lowLevel = 0.2; energy = 0.72;
            }
            case "finite_element" -> {
                structure = 0.9; stochasticity = 0.1; locality = 0.76; multiscale = 0.82; conservation = 0.9; message = 0.18; lowLevel = 0.28; energy = 0.75;
            }
            case "lattice_boltzmann" -> {
                structure = 0.82; stochasticity = 0.38; locality = 0.88; multiscale = 0.58; conservation = 0.95; message = 0.42; lowLevel = 0.24; energy = 0.8;
            }
            case "tensor_multiscale" -> {
                structure = 0.84; stochasticity = 0.16; locality = 0.66; multiscale = 0.98; conservation = 0.52; message = 0.34; lowLevel = 0.35; energy = 0.58;
            }
            case "graph_field" -> {
                structure = 0.96; stochasticity = 0.24; locality = 0.7; multiscale = 0.68; conservation = 0.66; message = 0.76; lowLevel = 0.28; energy = 0.56;
            }
            case "swarm_network" -> {
                structure = 0.68; stochasticity = 0.76; locality = 0.52; multiscale = 0.52; conservation = 0.38; message = 0.9; lowLevel = 0.18; energy = 0.48;
            }
            case "quantum_lattice" -> {
                structure = 0.82; stochasticity = 0.34; locality = 0.72; multiscale = 0.72; conservation = 0.72; message = 0.38; lowLevel = 0.62; energy = 0.84;
            }
            case "continuum_field" -> {
                structure = 0.72; stochasticity = 0.42; locality = 0.62; multiscale = 0.64; conservation = 0.78; message = 0.46; lowLevel = 0.2; energy = 0.7;
            }
            case "tree_transport" -> {
                structure = 0.88; stochasticity = 0.3; locality = 0.62; multiscale = 0.82; conservation = 0.88; message = 0.58; lowLevel = 0.24; energy = 0.74;
            }
            default -> {
            }
        }
        String lowered = design.toLowerCase();
        if (lowered.contains("markov") || lowered.contains("monte_carlo") || lowered.contains("percolation")) {
            stochasticity += 0.16;
        }
        if (lowered.contains("graph") || lowered.contains("network") || lowered.contains("tree")) {
            structure += 0.08;
            message += 0.12;
        }
        if (lowered.contains("tensor") || lowered.contains("multigrid") || lowered.contains("renormalization") || lowered.contains("coarse")) {
            multiscale += 0.16;
        }
        if (lowered.contains("boundary") || lowered.contains("conservation") || lowered.contains("collision")) {
            conservation += 0.08;
            locality += 0.05;
        }
        if (lowered.contains("quantum") || lowered.contains("interference") || lowered.contains("spin_chain") || lowered.contains("path_integral")) {
            lowLevel += 0.1;
            energy += 0.08;
        }
        if (lowered.contains("swarm") || lowered.contains("kuramoto")) {
            stochasticity += 0.08;
            message += 0.14;
        }
        return mapOf(
                "structureDensity", round3(clamp01(structure)),
                "stochasticity", round3(clamp01(stochasticity)),
                "locality", round3(clamp01(locality)),
                "multiscale", round3(clamp01(multiscale)),
                "conservation", round3(clamp01(conservation)),
                "messagePassing", round3(clamp01(message)),
                "lowLevelSignature", round3(clamp01(lowLevel)),
                "energyOptimization", round3(clamp01(energy))
        );
    }

    private static Map<String, Object> physicsGridComparison(Map<String, Object> physicsGrid, List<Map<String, Object>> candidates) {
        Map<String, Object> physicsGenes = nestedMap(physicsGrid, "genes");
        Map<String, Object> bestCandidate = candidates.get(0);
        Map<String, Object> bestSoftwareGenes = softwareGeneModel(bestCandidate);
        double bestScore = -1.0;
        for (Map<String, Object> candidate : candidates) {
            Map<String, Object> softwareGenes = softwareGeneModel(candidate);
            double score = clamp01(geneSimilarity(softwareGenes, physicsGenes) - physicsMismatchPenalty(softwareGenes, physicsGenes));
            if (score > bestScore) {
                bestScore = score;
                bestCandidate = candidate;
                bestSoftwareGenes = softwareGenes;
            }
        }
        List<String> lineupLevels = logicLineupLevels(bestSoftwareGenes, physicsGenes);
        String alignmentLevel = bestScore >= 0.88 ? "strong" : bestScore >= 0.76 ? "partial" : "weak";
        return mapOf(
                "gridId", physicsGrid.get("gridId"),
                "family", physicsGrid.get("family"),
                "processingGrid", physicsGrid.get("processingGrid"),
                "establishedMath", physicsGrid.get("establishedMath"),
                "establishedLogic", physicsGrid.get("establishedLogic"),
                "matchScore", round3(bestScore),
                "alignmentLevel", alignmentLevel,
                "logicLineup", String.join(", ", lineupLevels),
                "bestPermutation", chooserPickView(bestCandidate, "physics_genetic_best"),
                "physicsGenes", physicsGenes,
                "softwareGenes", bestSoftwareGenes
        );
    }

    private static Map<String, Object> evaluateEvolutionRound(
            List<Map<String, Object>> candidates,
            List<Map<String, Object>> familyGrids,
            Map<String, Double> weights
    ) {
        double bestScore = -1.0;
        Map<String, Object> bestCandidate = candidates.get(0);
        Map<String, Object> bestGrid = familyGrids.get(0);
        Map<String, Object> bestSoftwareGenes = softwareGeneModel(bestCandidate);
        Map<String, Object> bestPhysicsGenes = nestedMap(bestGrid, "genes");
        for (Map<String, Object> physicsGrid : familyGrids) {
            Map<String, Object> physicsGenes = nestedMap(physicsGrid, "genes");
            for (Map<String, Object> candidate : candidates) {
                Map<String, Object> softwareGenes = softwareGeneModel(candidate);
                double weightedSimilarity = weightedGeneSimilarity(weights, softwareGenes, physicsGenes);
                double weightedPenalty = weightedPhysicsPenalty(weights, softwareGenes, physicsGenes);
                double score = clamp01(weightedSimilarity - weightedPenalty);
                if (score > bestScore) {
                    bestScore = score;
                    bestCandidate = candidate;
                    bestGrid = physicsGrid;
                    bestSoftwareGenes = softwareGenes;
                    bestPhysicsGenes = physicsGenes;
                }
            }
        }
        return mapOf(
                "gridId", bestGrid.get("gridId"),
                "family", bestGrid.get("family"),
                "matchScore", round3(bestScore),
                "candidate", bestCandidate,
                "softwareGenes", bestSoftwareGenes,
                "physicsGenes", bestPhysicsGenes
        );
    }

    private static Map<String, Object> softwareGeneModel(Map<String, Object> candidate) {
        String entry = String.valueOf(candidate.getOrDefault("entryPoint", ""));
        String exit = String.valueOf(candidate.getOrDefault("exitPoint", ""));
        String axiomaticSet = String.valueOf(candidate.getOrDefault("axiomaticSet", ""));
        String objective = String.valueOf(candidate.getOrDefault("objective", ""));
        Map<String, Object> blockScale = nestedMap(candidate, "blockScale");
        Map<String, Object> scorecard = nestedMap(candidate, "chooserScorecard");
        Map<String, Object> probe = nestedMap(candidate, "probe");

        double structure = axiomaticSet.contains("STRUCTURE_GRAPH") || "source_tree_card".equals(entry) || "topology_ascii".equals(entry) ? 0.92 : 0.68;
        double stochasticity = "behavior_card".equals(entry) ? 0.78 : 0.32;
        double locality = "small_block".equals(blockScale.get("entryScale")) ? 0.88 : "medium_block".equals(blockScale.get("entryScale")) ? 0.74 : 0.6;
        double multiscale = "cross_layer".equals(blockScale.get("layerHint")) ? 0.82 : ("large_block".equals(blockScale.get("entryScale")) || "large_block".equals(blockScale.get("exitScale")) ? 0.86 : 0.46);
        double conservation = "benchmark_json".equals(entry) || "proof_card".equals(exit) || "epoch_proposal".equals(exit) ? 0.86 : 0.62;
        double message = "performative_acl_card".equals(entry) || "performative_route_card".equals(exit) ? 0.92 : "route_plan".equals(exit) ? 0.76 : 0.34;
        double lowLevel = "bytecode_signature_card".equals(entry) || "bytecode_plan_card".equals(exit) ? 0.95 : "source_tree_card".equals(entry) ? 0.58 : 0.18;
        double energy = "code_pattern_card".equals(exit) || "epoch_proposal".equals(exit) || "benchmark_json".equals(entry) ? 0.78 : 0.48;

        if (objective.toLowerCase().contains("genetic") || objective.toLowerCase().contains("stochastic")) {
            stochasticity += 0.1;
        }
        if ("pass".equals(String.valueOf(probe.get("status")))) {
            conservation += 0.05;
            energy += 0.05;
        }
        if (scorecard.get("proofHistory") instanceof Number history) {
            conservation += history.doubleValue() * 0.08;
        }
        return mapOf(
                "structureDensity", round3(clamp01(structure)),
                "stochasticity", round3(clamp01(stochasticity)),
                "locality", round3(clamp01(locality)),
                "multiscale", round3(clamp01(multiscale)),
                "conservation", round3(clamp01(conservation)),
                "messagePassing", round3(clamp01(message)),
                "lowLevelSignature", round3(clamp01(lowLevel)),
                "energyOptimization", round3(clamp01(energy))
        );
    }

    private static double geneSimilarity(Map<String, Object> softwareGenes, Map<String, Object> physicsGenes) {
        return round3(
                (0.18 * geneCloseness(softwareGenes, physicsGenes, "structureDensity"))
                        + (0.12 * geneCloseness(softwareGenes, physicsGenes, "stochasticity"))
                        + (0.18 * geneCloseness(softwareGenes, physicsGenes, "locality"))
                        + (0.14 * geneCloseness(softwareGenes, physicsGenes, "multiscale"))
                        + (0.14 * geneCloseness(softwareGenes, physicsGenes, "conservation"))
                        + (0.12 * geneCloseness(softwareGenes, physicsGenes, "messagePassing"))
                        + (0.06 * geneCloseness(softwareGenes, physicsGenes, "lowLevelSignature"))
                        + (0.06 * geneCloseness(softwareGenes, physicsGenes, "energyOptimization"))
        );
    }

    private static double weightedGeneSimilarity(Map<String, Double> weights, Map<String, Object> softwareGenes, Map<String, Object> physicsGenes) {
        double weighted = 0.0;
        for (Map.Entry<String, Double> entry : weights.entrySet()) {
            weighted += entry.getValue() * geneCloseness(softwareGenes, physicsGenes, entry.getKey());
        }
        return round3(weighted);
    }

    private static double physicsMismatchPenalty(Map<String, Object> softwareGenes, Map<String, Object> physicsGenes) {
        double structurePenalty = Math.max(0.0, Math.abs(geneValue(softwareGenes, "structureDensity") - geneValue(physicsGenes, "structureDensity")) - 0.14) * 0.22;
        double stochasticPenalty = Math.max(0.0, Math.abs(geneValue(softwareGenes, "stochasticity") - geneValue(physicsGenes, "stochasticity")) - 0.18) * 0.12;
        double multiscalePenalty = Math.max(0.0, Math.abs(geneValue(softwareGenes, "multiscale") - geneValue(physicsGenes, "multiscale")) - 0.14) * 0.18;
        double messagePenalty = Math.max(0.0, Math.abs(geneValue(softwareGenes, "messagePassing") - geneValue(physicsGenes, "messagePassing")) - 0.14) * 0.18;
        double lowLevelPenalty = Math.max(0.0, Math.abs(geneValue(softwareGenes, "lowLevelSignature") - geneValue(physicsGenes, "lowLevelSignature")) - 0.18) * 0.14;
        double conservationPenalty = Math.max(0.0, Math.abs(geneValue(softwareGenes, "conservation") - geneValue(physicsGenes, "conservation")) - 0.16) * 0.16;
        return round3(structurePenalty + stochasticPenalty + multiscalePenalty + messagePenalty + lowLevelPenalty + conservationPenalty);
    }

    private static double weightedPhysicsPenalty(Map<String, Double> weights, Map<String, Object> softwareGenes, Map<String, Object> physicsGenes) {
        double penalty = 0.0;
        for (Map.Entry<String, Double> entry : weights.entrySet()) {
            String gene = entry.getKey();
            double slack = switch (gene) {
                case "structureDensity", "locality", "multiscale", "messagePassing" -> 0.14;
                case "conservation" -> 0.16;
                default -> 0.18;
            };
            penalty += Math.max(0.0, Math.abs(geneValue(softwareGenes, gene) - geneValue(physicsGenes, gene)) - slack) * entry.getValue() * 0.9;
        }
        return round3(penalty);
    }

    private static Map<String, Double> mutateGeneWeights(
            Map<String, Double> currentWeights,
            String objective,
            int generation,
            List<String> focusGenes
    ) {
        Map<String, Double> mutated = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : currentWeights.entrySet()) {
            String gene = entry.getKey();
            double base = entry.getValue();
            double centered = (stableRandom01(objective + "|physics_mutation|" + generation + "|" + gene) - 0.5) * 0.12;
            if (focusGenes.contains(gene)) {
                centered *= 1.8;
            }
            mutated.put(gene, Math.max(0.02, base + centered));
        }
        double total = mutated.values().stream().mapToDouble(Double::doubleValue).sum();
        Map<String, Double> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : mutated.entrySet()) {
            normalized.put(entry.getKey(), round3(entry.getValue() / total));
        }
        return normalized;
    }

    private static List<String> topMismatchGenes(Map<String, Object> softwareGenes, Map<String, Object> physicsGenes, int limit) {
        List<Map<String, Object>> diffs = new ArrayList<>();
        for (String gene : baseGeneWeights().keySet()) {
            diffs.add(mapOf(
                    "gene", gene,
                    "diff", round3(Math.abs(geneValue(softwareGenes, gene) - geneValue(physicsGenes, gene)))
            ));
        }
        diffs.sort((a, b) -> Double.compare(numericValue(b.get("diff")), numericValue(a.get("diff"))));
        List<String> top = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, diffs.size()); i++) {
            top.add(String.valueOf(diffs.get(i).get("gene")));
        }
        return top;
    }

    private static List<Object> topWeightGenes(Map<String, Double> weights, int limit) {
        List<Map.Entry<String, Double>> entries = new ArrayList<>(weights.entrySet());
        entries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        List<Object> top = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, entries.size()); i++) {
            Map.Entry<String, Double> entry = entries.get(i);
            top.add(mapOf("gene", entry.getKey(), "weight", round3(entry.getValue())));
        }
        return top;
    }

    private static List<Map<String, Object>> nestedObjectList(Map<String, Object> parent, String key) {
        Object value = parent.get(key);
        List<Map<String, Object>> list = new ArrayList<>();
        if (value instanceof List<?> rawList) {
            for (Object item : rawList) {
                if (item instanceof Map<?, ?> raw) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> cast = (Map<String, Object>) raw;
                    list.add(cast);
                }
            }
        }
        return list;
    }

    private static double geneCloseness(Map<String, Object> softwareGenes, Map<String, Object> physicsGenes, String gene) {
        return 1.0 - Math.abs(geneValue(softwareGenes, gene) - geneValue(physicsGenes, gene));
    }

    private static double geneValue(Map<String, Object> genes, String key) {
        Object value = genes.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return 0.0;
    }

    private static double numericValue(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value != null) {
            try {
                return Double.parseDouble(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                return 0.0;
            }
        }
        return 0.0;
    }

    private static List<String> logicLineupLevels(Map<String, Object> softwareGenes, Map<String, Object> physicsGenes) {
        List<String> levels = new ArrayList<>();
        if (geneValue(softwareGenes, "structureDensity") > 0.72 && geneValue(physicsGenes, "structureDensity") > 0.72 && geneValue(softwareGenes, "locality") > 0.7 && geneValue(physicsGenes, "locality") > 0.7) {
            levels.add("topology/locality");
        }
        if (geneValue(softwareGenes, "stochasticity") > 0.68 && geneValue(physicsGenes, "stochasticity") > 0.68) {
            levels.add("stochastic chooser");
        }
        if (geneValue(softwareGenes, "conservation") > 0.72 && geneValue(physicsGenes, "conservation") > 0.72) {
            levels.add("proof/constraint");
        }
        if (geneValue(softwareGenes, "messagePassing") > 0.72 && geneValue(physicsGenes, "messagePassing") > 0.72) {
            levels.add("performative routing");
        }
        if (geneValue(softwareGenes, "multiscale") > 0.72 && geneValue(physicsGenes, "multiscale") > 0.72) {
            levels.add("epoch/orchestrator");
        }
        if (geneValue(softwareGenes, "lowLevelSignature") > 0.72 && geneValue(physicsGenes, "lowLevelSignature") > 0.55) {
            levels.add("bytecode/signature");
        }
        if (geneValue(softwareGenes, "energyOptimization") > 0.72 && geneValue(physicsGenes, "energyOptimization") > 0.72) {
            levels.add("optimization/fitness");
        }
        if (levels.isEmpty()) {
            levels.add("weak local overlap only");
        }
        return levels;
    }

    private static double physicsMatchScore(Map<String, Object> match) {
        Object value = match.get("matchScore");
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return 0.0;
    }

    private static Map<String, Object> algebraicEntryExitSearch(String entry, String exit) {
        return mapOf(
                "entrySearchFocus", switch (entry) {
                    case "source_tree_card" -> "search directories, filenames, modules, route owners, and file boundaries";
                    case "performative_acl_card" -> "search sender/receiver, performative verbs, route contracts, and action payloads";
                    case "bytecode_signature_card" -> "search decompile hooks, compile hooks, executable signatures, and adapter points";
                    default -> "search bounded cards, logs, and proof surfaces related to the entry data";
                },
                "exitSearchFocus", switch (exit) {
                    case "performative_route_card" -> "emit performative route contract with target and proof envelope";
                    case "bytecode_plan_card" -> "emit bytecode/decompile/compile processing plan with signatures and verification steps";
                    default -> "emit bounded artifact for the selected exit target";
                }
        );
    }

    private static List<Object> algebraicSoftwareSignals(String entry) {
        return switch (entry) {
            case "source_tree_card" -> List.of(
                    mapOf("signal", "module_boundary", "meaning", "file or directory edge defining software responsibility"),
                    mapOf("signal", "route_owner", "meaning", "source file owning an API or runtime path"),
                    mapOf("signal", "call_surface", "meaning", "entry function, handler, or CLI point")
            );
            case "performative_acl_card" -> List.of(
                    mapOf("signal", "performative", "meaning", "tell/request/propose/achieve/query-if action tag"),
                    mapOf("signal", "sender_receiver", "meaning", "agent contract endpoints"),
                    mapOf("signal", "approval_lane", "meaning", "approval-gated versus direct action")
            );
            case "bytecode_signature_card" -> List.of(
                    mapOf("signal", "bytecode_hook", "meaning", "decompile or compile adapter point"),
                    mapOf("signal", "signature", "meaning", "hash or structural signature for low-level artifact"),
                    mapOf("signal", "verifier", "meaning", "compile, parse, or runtime proof hook")
            );
            default -> List.of(
                    mapOf("signal", "bounded_entry", "meaning", "compressed software-relevant entry point"),
                    mapOf("signal", "proof_target", "meaning", "expected software artifact or route target")
            );
        };
    }

    private static List<Object> algebraicPerformativeRules(String entry, String exit) {
        return List.of(
                mapOf("performative", "tell", "use", "record a fact or observed software state"),
                mapOf("performative", "request", "use", "ask a subsystem to expose an entry or exit point"),
                mapOf("performative", "propose", "use", "suggest a bounded software transformation or pattern"),
                mapOf("performative", "achieve", "use", "queue an implementation-oriented goal after proof"),
                mapOf("route_binding", entry + " -> " + exit, "policy", "keep performative contract visible before processing")
        );
    }

    private static Map<String, Object> algebraicBytecodePlan(String entry, String exit) {
        return mapOf(
                "enabled", "source_tree_card".equals(entry) || "bytecode_signature_card".equals(entry) || "bytecode_plan_card".equals(exit),
                "capture", List.of("signature hash", "adapter point", "decompile hook", "compile hook", "verification command"),
                "boundedProcessing", "record plan and signatures first; do not mutate binaries from this lab",
                "futureAdapters", List.of("Binary Ninja", "Ghidra", "javap/jar", "python py_compile", "D8/JDK adapters"),
                "exitArtifact", "bytecode_plan_card".equals(exit) ? "bounded bytecode processing card" : "supporting low-level evidence only"
        );
    }

    private static Map<String, Object> algebraicAxiomFit(String entry, String exit) {
        String axiomaticSet = "SET_EVIDENCE_PROOF";
        String mathFit = "weighted truth table";
        String logicFit = "proof scoring and constraint checks";
        String route = "planning";
        String hook = "bridgePlanning";
        String entryTransform = "compress input to one local card";
        String exitTransform = "emit concise proof artifact";
        if ("topology_ascii".equals(entry)) {
            axiomaticSet = "SET_STRUCTURE_GRAPH";
            mathFit = "graph traversal plus coordinate mapping";
            logicFit = "module adjacency and dependency localization";
            entryTransform = "convert topology tree into bounded node-edge card";
        } else if ("successful_code_card".equals(entry)) {
            axiomaticSet = "SET_CODE_REUSE";
            mathFit = "set intersection plus reusable block weighting";
            logicFit = "reusable implementation pattern selection";
            route = "build";
            hook = "bridgeBuild";
            entryTransform = "reduce code success record to hash, scope, and verdict";
        } else if ("behavior_card".equals(entry)) {
            axiomaticSet = "SET_BEHAVIOR_MARKOV";
            mathFit = "transition weighting and top-k path selection";
            logicFit = "behavior routing and repair guidance";
            entryTransform = "merge liked/disliked and markov hints into capped state card";
        } else if ("nominal_fact_card".equals(entry)) {
            axiomaticSet = "SET_CONTEXT_ANCHOR";
            mathFit = "small stable set membership";
            logicFit = "context anchoring without behavioral drift";
            entryTransform = "keep only compact nominal variables";
        } else if ("source_tree_card".equals(entry)) {
            axiomaticSet = "SET_SOFTWARE_ENTRY_EXIT";
            mathFit = "graph reachability plus file adjacency";
            logicFit = "entry and exit point localization";
            route = "build";
            hook = "bridgeBuild";
            entryTransform = "reduce file tree to modules, handlers, routes, and call surfaces";
        } else if ("performative_acl_card".equals(entry)) {
            axiomaticSet = "SET_PERFORMATIVE_ROUTING";
            mathFit = "relation mapping plus action partition";
            logicFit = "agent performative routing";
            route = "planning";
            hook = "performativeRouter";
            entryTransform = "reduce performative card to sender, receiver, action, route, and proof target";
        } else if ("bytecode_signature_card".equals(entry)) {
            axiomaticSet = "SET_BYTECODE_VERIFICATION";
            mathFit = "signature matching plus verifier gating";
            logicFit = "bytecode and decompile plan selection";
            route = "build";
            hook = "bytecodeProcessing";
            entryTransform = "reduce low-level artifact to hash, adapter, command, and verifier";
        }

        if ("code_pattern_card".equals(exit)) {
            mathFit = "permutation matrix plus weighted truth table";
            logicFit = "code generation pattern selection";
            route = "build";
            hook = "bridgeBuild";
            exitTransform = "emit reusable block card with constraints and counterexample";
        } else if ("logic_pattern_card".equals(exit)) {
            mathFit = "constraint satisfaction plus route weighting";
            logicFit = "logic policy card";
            route = "planning";
            hook = "bridgePlanning";
            exitTransform = "emit logic schema, proof rule, and failure trigger";
        } else if ("epoch_proposal".equals(exit)) {
            mathFit = "delta comparison plus promotion gate";
            logicFit = "one-variable epoch synthesis";
            route = "planning";
            hook = "karooComparator";
            exitTransform = "emit bounded epoch proposal with acceptance test";
        } else if ("route_plan".equals(exit)) {
            mathFit = "categorical route partition";
            logicFit = "entry-exit routing";
            route = "planning";
            hook = "tinyChooser";
            exitTransform = "emit route plan with required transforms";
        } else if ("performative_route_card".equals(exit)) {
            mathFit = "relation mapping plus action partition";
            logicFit = "performative route contract";
            route = "planning";
            hook = "performativeRouter";
            exitTransform = "emit sender/receiver/performative/approval route card";
        } else if ("bytecode_plan_card".equals(exit)) {
            mathFit = "signature matching plus verifier gating";
            logicFit = "bytecode processing plan";
            route = "build";
            hook = "bytecodeProcessing";
            exitTransform = "emit decompile/compile/signature verification card";
        }

        return mapOf(
                "axiomaticSet", axiomaticSet,
                "mathFit", mathFit,
                "logicFit", logicFit,
                "recommendedRoute", route,
                "recommendedModelHook", hook,
                "entryTransform", entryTransform,
                "exitTransform", exitTransform
        );
    }

    private static String algebraicSampleData(String entry) {
        return switch (entry) {
            case "topology_ascii" -> compactStatus(asciiEpochCube());
            case "benchmark_json" -> compactStatus(fetchText("http://127.0.0.1:8080/api/benchmarks?limit=1", 6));
            case "epoch_proposal" -> compactStatus(tail(EPOCH_UPGRADE_LOG, 1));
            case "successful_code_card" -> "distilled_success route=build hash=reusable_block verdict=pass scope=small_safe_change";
            case "behavior_card" -> "top5 behavior pack: liked intents, disliked intents, markov hint, related logic, capped context";
            case "nominal_fact_card" -> "compact nominal facts only: identity/work/context anchor separated from behavior memory";
            case "source_tree_card" -> "source_tree modules=java_notes_suite,tools,system_mirrors handlers=api/page/log-tail routes=bridge/shipper/java_sdk";
            case "performative_acl_card" -> "performative route=both sender=user receiver=karoo action=propose content=software entry_exit discovery approval=required";
            case "bytecode_signature_card" -> "bytecode_plan adapter=py_compile+javac signature=sha256 verifier=parse_compile_runtime scope=bounded";
            default -> "unknown_entry";
        };
    }

    private static Map<String, Object> algebraicModelProbe(
            String entry,
            String exit,
            String objective,
            Map<String, Object> axioms,
            String sampleData,
            String asciiFlow,
            String mathNotes
    ) {
        String prompt = "VIPER algebraic flow lab. "
                + "Objective: " + objective + ". "
                + "Entry: " + entry + ". "
                + "Exit: " + exit + ". "
                + "Axiomatic set: " + axioms.get("axiomaticSet") + ". "
                + "Math fit: " + axioms.get("mathFit") + ". "
                + "Logic fit: " + axioms.get("logicFit") + ". "
                + "ASCII flow: " + compactStatus(asciiFlow) + ". "
                + "Math notes: " + compactStatus(mathNotes) + ". "
                + "Required output: name the best transform chain, state one failure risk, and produce a concise "
                + exit + " using this sample data: " + sampleData;
        Map<String, Object> bridge = bridgePredictProbe(prompt);
        Object httpStatus = bridge.get("httpStatus");
        boolean ok = httpStatus instanceof Number number && number.intValue() >= 200 && number.intValue() < 300;
        String preview = String.valueOf(bridge.getOrDefault("responsePreview", ""));
        String status = ok && preview.length() > 30 ? "pass" : "fail";
        return mapOf(
                "status", status,
                "httpStatus", bridge.get("httpStatus"),
                "durationMs", bridge.get("durationMs"),
                "responsePreview", preview,
                "routeHint", axioms.get("recommendedRoute"),
                "modelHook", axioms.get("recommendedModelHook"),
                "failureRecorded", "fail".equals(status),
                "error", bridge.get("error")
        );
    }

    private static String proposedEpochDiagram(String body) {
        String lower = body == null ? "" : body.toLowerCase();
        String subsystem = firstMatch(lower, List.of(
                "chooser", "db_retrieval", "karoo", "abliterated", "loihi",
                "lava", "soap", "ledger", "network", "java_sdk"
        ), "proposed_node");
        String quickVar = firstMatch(lower, List.of(
                "retrieval_weight", "token_budget", "lava_mode", "loihi_cube",
                "soap_endpoint", "promotion_gate", "karoo_rounds", "web_research_gate"
        ), "quick_var");
        String judge = firstMatch(lower, List.of(
                "optional_copilot", "optional_gemini", "optional_cloud_agent",
                "local_benchmark", "karoo_compare", "tiny_critic"
        ), "judge_slot");
        return """
                VIPER_SDK_EPOCH_PROPOSAL  version: %s

                explorer
                +- system
                |  +- chooser
                |  +- db_retrieval
                |  +- karoo
                |  +- abliterated
                |  +- loihi
                |  +- lava
                |  +- soap
                |  +- ledger
                |  +- network
                |  +- java_sdk
                |
                +- proposed_change
                |  +- subsystem: >>> %s <<<
                |  +- variable:  >>> %s <<<
                |  +- judge:     >>> %s <<<
                |
                +- flow
                   +- ask.card
                   +- db.retrieve
                   +- lens.compose
                   +- route.execute
                   +- judge.weigh      [highlight]
                   +- benchmark.prove   [highlight]
                   +- sha256.log
                   +- promote.or.wait

                +------------------+     +------------------+     +------------------+
                | current baseline | --> | >>> proposal <<< | --> | benchmark gate   |
                +------------------+     +------------------+     +------------------+
                          |                        |                        |
                          v                        v                        v
                +------------------+     +------------------+     +------------------+
                | keep history     |     | one variable     |     | 99.99%% + 10%%    |
                +------------------+     +------------------+     +------------------+
                """.formatted(SDK_VERSION, subsystem, quickVar, judge);
    }

    private static Map<String, Object> buildEpochUpgradeProof(String requestBody) {
        String bridgeBenchmarks = fetchText("http://127.0.0.1:8080/api/benchmarks?limit=8", 6);
        String houseHealth = fetchText("http://127.0.0.1:11435/health", 4);
        String shipperHealth = fetchText("http://127.0.0.1:18081/health", 4);
        String shipperTail = tail(ROOT.resolve("logic_blockchain_shipper.log"), 40);
        String topologyTail = tail(ROOT.resolve("topology_sidecar_loop.log"), 40);

        List<Map<String, Object>> proposals = new ArrayList<>();
        proposals.add(epochProposal(
                "EPOCH_BRIDGE_HEADROOM_REPAIR",
                "bridge8080",
                "Thin replies and repaired planning turns show the orchestration works but needs a stronger response contract.",
                evidenceLine(bridgeBenchmarks, "response_chars\": 5", "bridge benchmark includes a very short response"),
                "Add a route-level completion proof card: answer_min_chars, required_sections, and retry reason before returning to the user.",
                "Replay the last thin prompt; pass only if response_chars >= 300 or route explicitly marks terse chat as intended."
        ));
        proposals.add(epochProposal(
                "EPOCH_SHIPPER_UPLINK_COMPAT",
                "logic_shipper18081",
                "Heartbeat succeeds, but local /api/uplink posts are repeatedly 404, so ledger shipping needs endpoint compatibility proof.",
                evidenceLine(shipperTail, "/api/uplink HTTP/1.1\" 404", "shipper log shows repeated /api/uplink 404 responses"),
                "Add an endpoint negotiation table: local_uplink_path, cloud_uplink_path, ledger_path, and preflight route check.",
                "Run health plus one dry-run uplink preflight; pass only if route returns 2xx/accepted or clear disabled status."
        ));
        proposals.add(epochProposal(
                "EPOCH_KAROO_COMPARATOR_ATTACH",
                "karoo_topology_loop",
                "Karoo is safely capturing baselines, but comparison_count is zero, so it cannot yet rank mutations like a real epoch judge.",
                evidenceLine(topologyTail, "comparison_count\": 0", "topology tail shows comparison_count is zero"),
                "Attach a comparator source set: project-local snippet, successful ledger block, and optional external judge score.",
                "Next Karoo candidate must include comparison_count >= 3 and one-variable score deltas before patch proposal."
        ));
        proposals.add(epochProposal(
                "EPOCH_SOVEREIGN_AGENT_CONTRACT",
                "agent_network",
                "The system is ready for orchestration if every agent declares capability, limits, endpoints, and proof outputs first.",
                "house=" + compactStatus(houseHealth) + "; shipper=" + compactStatus(shipperHealth),
                "Add an ACL/KQML capability card per agent: can_do, cannot_do, endpoint, token budget, storage role, heartbeat.",
                "Pass when each registered agent can answer capability ping and produce a SHA-256 proof envelope."
        ));
        proposals.add(epochProposal(
                "EPOCH_ROLLING_TRIPLET_RESTORE",
                "chooser_triplet_tail",
                "User-approved: restore the lighter rolling recursive triplet response because it felt better than a single heavy bridge pass.",
                "Active request approved rolling triplet plus tiny chooser/decider first.",
                "Tiny chooser selects route -> light model draft -> Karoo/action pass -> verifier/editor pass -> tail continuation stitches long responses.",
                "TBD: replay long planning prompt; pass when response is complete, sectioned, and no 'PASS.' or cutoff appears."
        ));
        proposals.add(epochProposal(
                "EPOCH_MISSION_DIRECTIVE_ALWAYS_ON",
                "mission_directive",
                "User-approved: display and inject the mission directive first so every model keeps the same purpose frame.",
                "Active request supplied always-on directive text.",
                "Prefix every route with the mission directive before variable context so prefix caching can reuse it.",
                "TBD: inspect prompt pack; pass when directive appears before retrieval/lens and output remains on mission."
        ));
        proposals.add(epochProposal(
                "EPOCH_LONG_RESPONSE_TAIL_STITCH",
                "tail_continuation",
                "Long answers should be allowed to take time and continue cleanly instead of collapsing under token or timeout pressure.",
                "Bridge defaults were raised; tail engineering is approved for long local responses.",
                "When answer may exceed token budget, ask model to end with TAIL_CONTINUE token and resume from the last outline point.",
                "TBD: request a long answer; pass when continuation joins without repeated intro or missing final section."
        ));
        proposals.add(epochProposal(
                "EPOCH_INFERENCE_OPTIMIZATION_STACK",
                "inference_runtime",
                "User supplied optimization stack: quantization, prefix caching, disaggregated prefill/decode, Flash Attention, continuous batching, KV cache management, speculative decoding.",
                "Imported optimization notes from active request.",
                "Create environment capability table and enable only supported optimizations: GGUF quant choice now; vLLM/Flash/PagedAttention/speculative later where hardware supports it.",
                "TBD: benchmark TTFT, tokens/sec, memory use, and accuracy before/after each single optimization."
        ));
        proposals.add(epochProposal(
                "EPOCH_DISTRIBUTED_RESOURCE_APP",
                "distributed_exe_network",
                "User-approved final phase: real distributable app that can lend/take CPU, memory, and storage across PCs and phones.",
                "Standalone Java EXE exists; APK skeleton exists; resource lending still needs a real protocol.",
                "Add node capability daemon: announce resources, accept signed tasks, return SHA-256 proof, enforce local opt-in quotas.",
                "TBD: two-node LAN smoke test; pass when node A sees node B resources and runs a harmless benchmark task."
        ));
        proposals.add(epochProposal(
                "EPOCH_AXIOMATIC_WEIGHTED_TRUTH_TABLES",
                "coding_truth_tables",
                "User-approved: all agents should use axiomatic truth tables with weighted truths for coding decisions.",
                "Active request: 'EVERYONE is using AXIOMATIC TRUTH TABLES WITH WEIGHTED TRUTHES FOR CODING'.",
                "Add coding decision table: axiom, evidence, counterexample, weight, confidence, test, verdict. Require it in Karoo proposal packets.",
                "TBD: next code proposal must include weighted truth table and pass/fail evidence before patch approval."
        ));
        proposals.add(epochProposal(
                "EPOCH_REAL_TINY_CHOOSER",
                "qwen2_5_chooser",
                "The previous chooser was too deterministic; Qwen2.5-0.5B now writes the active lens instead of merely validating a template.",
                "Tiny model runtime downloads and probes Qwen2.5-0.5B GGUF, then logs qwen_chooser benchmarks.",
                "Use Qwen to emit the max-100-word active lens after DB/user/web evidence is gathered; deterministic text is fallback only.",
                "TBD: pass when qwen_chooser_lens_100_words status is chosen_by_qwen2_5 and response route/layer are correct."
        ));
        proposals.add(epochProposal(
                "EPOCH_AXIOMATIC_RETRIEVAL_MATCHER",
                "smollm2_retrieval",
                "Keyword retrieval alone was noisy; SmolLM2-360M now selects the closest 50-word axiomatic DB match.",
                "Tiny model runtime downloads and probes SmolLM2-360M GGUF, with H2O-Danube3 as fallback.",
                "Run purpose-first DB retrieval, compress candidates, ask SmolLM2 for one closest match, then inject only that reduced card.",
                "TBD: pass when axiomatic_retrieval_match_50_words status is matched_by_smollm2 and does not exceed 50 words."
        ));
        proposals.add(epochProposal(
                "EPOCH_NAS_AGENT_SPINUP_SYNC",
                "nas_agent_bootstrap",
                "The resource network needs a repeatable way to copy project files and tiny models to new machines without guessing paths.",
                "VIPER_NAS_ROOT is optional and no hard-coded NAS path is assumed.",
                "Add CREATE_VIPER_NAS_LINK.ps1 and SPIN_UP_AGENT_NODE.ps1 for staging, env config, tiny model paths, and node bootstrap.",
                "TBD: pass when a second machine can run the generated env file and report model-path/status without manual file hunting."
        ));

        Map<String, Object> proof = new LinkedHashMap<>();
        proof.put("kind", "epoch_upgrade_proof");
        proof.put("version", SDK_VERSION);
        proof.put("timestamp", Instant.now().toString());
        proof.put("request", requestBody);
        proof.put("checkpoint", "C:\\Users\\viper\\VIPER_JAVA_RISC_CHECKPOINTS");
        proof.put("mode", "proof_of_concept_proposals_only");
        proof.put("approvalStatus", "approved_by_user_pending_tbd_tests");
        proof.put("systemRead", mapOf(
                "bridgeBenchmarks", bridgeBenchmarks.substring(0, Math.min(1200, bridgeBenchmarks.length())),
                "houseHealth", houseHealth,
                "shipperHealth", shipperHealth,
                "shipperTail", shipperTail,
                "topologyTail", topologyTail
        ));
        proof.put("proposals", proposals);
        proof.put("diagram", upgradeProofDiagram());
        proof.put("promotionGate", "No auto-apply. Promote only after one-variable test, e2e proof, success >= 99.99, and +10% speed or -10% resources.");
        return proof;
    }

    private static Map<String, Object> epochProposal(String id, String subsystem, String problem, String evidence, String proposedChange, String acceptanceTest) {
        return mapOf(
                "id", id,
                "subsystem", subsystem,
                "problem", problem,
                "evidence", evidence,
                "proposedChange", proposedChange,
                "acceptanceTest", acceptanceTest,
                "testResult", "TBD",
                "approvalStatus", "approved_by_user_pending_test",
                "visualHighlight", "HIGH_CONTRAST_YELLOW",
                "highlight", ">>> " + subsystem + " :: " + proposedChange + " <<<",
                "diagram", proposalFlowDiagram(id, subsystem)
        );
    }

    private static String evidenceLine(String text, String needle, String hit) {
        if (text != null && text.contains(needle)) {
            return hit;
        }
        return "No exact marker found in current tail; proposal remains queued for more evidence.";
    }

    private static String compactStatus(String text) {
        if (text == null || text.isBlank()) {
            return "empty";
        }
        String clean = text.replace("\r", " ").replace("\n", " ");
        return clean.substring(0, Math.min(180, clean.length()));
    }

    private static String proposalFlowDiagram(String id, String subsystem) {
        return """
                %s
                +- baseline.read
                |  +- logs
                |  +- benchmarks
                |  +- health
                +- proposed_change
                |  +- subsystem: >>> %s <<<
                |  +- scope: one variable
                |  +- mode: proposal only
                +- proof
                   +- run test
                   +- compare delta
                   +- sha256 record
                   +- wait for approval
                """.formatted(id, subsystem);
    }

    private static String upgradeProofDiagram() {
        return """
                VIPER_EPOCH_UPGRADE_PROOF

                +-----------------+      +------------------+      +------------------+
                | subsystem scan  | ---> | proposed epoch   | ---> | acceptance test  |
                +-----------------+      +------------------+      +------------------+
                         |                         |                         |
                         v                         v                         v
                +-----------------+      +------------------+      +------------------+
                | evidence tail   |      | >>> highlight <<<|      | benchmark + SHA |
                +-----------------+      +------------------+      +------------------+

                surgical agents:
                  bridge.headroom       -> fix thin replies with completion proof
                  shipper.uplink        -> fix endpoint compatibility before ledger shipping
                  karoo.comparator      -> require comparison_count before mutation scoring
                  sovereign.contract    -> every agent declares capability and proof envelope
                """;
    }

    private static String firstMatch(String text, List<String> choices, String fallback) {
        for (String choice : choices) {
            if (text.contains(choice)) {
                return choice;
            }
        }
        return fallback;
    }

    private static Map<String, Object> probe(String url) {
        long start = System.currentTimeMillis();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("url", url);
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            result.put("status", response.statusCode());
            result.put("ok", response.statusCode() >= 200 && response.statusCode() < 300);
            result.put("durationMs", System.currentTimeMillis() - start);
            result.put("bodyPreview", response.body().substring(0, Math.min(240, response.body().length())));
        } catch (Exception e) {
            result.put("ok", false);
            result.put("durationMs", System.currentTimeMillis() - start);
            result.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return result;
    }

    private static Map<String, Object> bridgeImplementationRequest(String prompt) {
        long start = System.currentTimeMillis();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("url", "http://127.0.0.1:8080/api/loibi/predict");
        result.put("promptPreview", prompt.substring(0, Math.min(320, prompt.length())));
        try {
            String payload = jsonObject(mapOf("message", prompt));
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:8080/api/loibi/predict"))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body() == null ? "" : response.body();
            result.put("httpStatus", response.statusCode());
            result.put("status", response.statusCode() >= 200 && response.statusCode() < 300 ? "bridge_request_recorded" : "bridge_request_failed");
            result.put("durationMs", System.currentTimeMillis() - start);
            result.put("responsePreview", body.substring(0, Math.min(900, body.length())));
        } catch (Exception e) {
            result.put("status", "bridge_request_error");
            result.put("durationMs", System.currentTimeMillis() - start);
            result.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return result;
    }

    private static Map<String, Object> bridgePredictProbe(String prompt) {
        long start = System.currentTimeMillis();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("url", "http://127.0.0.1:8080/api/loibi/predict");
        result.put("promptPreview", prompt.substring(0, Math.min(320, prompt.length())));
        try {
            String payload = jsonObject(mapOf("message", prompt));
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:8080/api/loibi/predict"))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body() == null ? "" : response.body();
            result.put("httpStatus", response.statusCode());
            result.put("durationMs", System.currentTimeMillis() - start);
            result.put("responsePreview", body.substring(0, Math.min(900, body.length())));
        } catch (Exception e) {
            result.put("durationMs", System.currentTimeMillis() - start);
            result.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return result;
    }

    private static String acceptedEpochBridgePrompt(
            String proposalId,
            String subsystem,
            String proposedChange,
            String acceptanceTest,
            String evidence,
            String goal
    ) {
        return "Accepted epoch proposal " + proposalId + ". "
                + "Goal: " + goal + ". "
                + "Subsystem: " + subsystem + ". "
                + "Implement the smallest safe change implied by this accepted epoch proposal. "
                + "Proposed change: " + proposedChange + ". "
                + "Acceptance test: " + acceptanceTest + ". "
                + "Evidence: " + evidence + ". "
                + "Preserve the GUI, keep one variable per change, log proof, and stay within bounded local implementation.";
    }

    private static String fetchText(String url, int timeoutSeconds) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .GET()
                    .build();
            return HTTP.send(request, HttpResponse.BodyHandlers.ofString()).body();
        } catch (Exception e) {
            return "FETCH_ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    private static List<String> uniqueOrdered(List<String> values) {
        List<String> unique = new ArrayList<>();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            if (!unique.contains(value)) {
                unique.add(value);
            }
        }
        return unique;
    }

    private static String trainingPrompt(String dataset, String route, String changedVariable, String objective) {
        return "VIPER Java lab training eval. dataset=" + dataset
                + "; route=" + route
                + "; changed_variable=" + changedVariable
                + "; objective=" + objective
                + "; use real retrieval, Qwen chooser lens, SmolLM match, rolling triplet, and proof logs.";
    }

    private static Map<String, Object> evaluateTrainingRun(
            Map<String, Object> before,
            Map<String, Object> after,
            String prefetch,
            String bridgeBenchmarks
    ) {
        boolean bridgeOk = serviceOk(before, "bridge8080") && serviceOk(after, "bridge8080");
        boolean houseOk = serviceOk(before, "house11435") && serviceOk(after, "house11435");
        boolean shipperOk = serviceOk(before, "shipper18081") && serviceOk(after, "shipper18081");
        boolean prefetchOk = prefetch != null && prefetch.contains("\"prediction\"");
        boolean benchmarkOk = bridgeBenchmarks != null && bridgeBenchmarks.contains("\"benchmarks\"");
        long beforeTotal = serviceMs(before, "bridge8080") + serviceMs(before, "house11435") + serviceMs(before, "shipper18081");
        long afterTotal = serviceMs(after, "bridge8080") + serviceMs(after, "house11435") + serviceMs(after, "shipper18081");
        long latencyDeltaMs = afterTotal - beforeTotal;
        int passSignals = 0;
        passSignals += bridgeOk ? 1 : 0;
        passSignals += houseOk ? 1 : 0;
        passSignals += shipperOk ? 1 : 0;
        passSignals += prefetchOk ? 1 : 0;
        passSignals += benchmarkOk ? 1 : 0;
        double score = passSignals / 5.0;
        boolean speedImproved = beforeTotal > 0 && afterTotal <= Math.round(beforeTotal * 0.90);
        return mapOf(
                "score", score,
                "bridgeOk", bridgeOk,
                "houseOk", houseOk,
                "shipperOk", shipperOk,
                "prefetchOk", prefetchOk,
                "benchmarkReadOk", benchmarkOk,
                "beforeServiceTotalMs", beforeTotal,
                "afterServiceTotalMs", afterTotal,
                "latencyDeltaMs", latencyDeltaMs,
                "speedImproved10Percent", speedImproved,
                "promotionEligible", score >= 0.9999 && speedImproved,
                "verdict", score >= 0.8 ? "training_eval_passed_recorded" : "training_eval_needs_attention",
                "nextAction", "keep logging; only promote after repeatable one-variable e2e proof"
        );
    }

    private static boolean serviceOk(Map<String, Object> benchmark, String name) {
        Object servicesObj = benchmark.get("services");
        if (!(servicesObj instanceof Map<?, ?> services)) {
            return false;
        }
        Object serviceObj = services.get(name);
        if (!(serviceObj instanceof Map<?, ?> service)) {
            return false;
        }
        return Boolean.TRUE.equals(service.get("ok"));
    }

    private static long serviceMs(Map<String, Object> benchmark, String name) {
        Object servicesObj = benchmark.get("services");
        if (!(servicesObj instanceof Map<?, ?> services)) {
            return 0L;
        }
        Object serviceObj = services.get(name);
        if (!(serviceObj instanceof Map<?, ?> service)) {
            return 0L;
        }
        Object value = service.get("durationMs");
        if (value instanceof Number number) {
            return number.longValue();
        }
        return 0L;
    }

    private static long fileSize(Path path) {
        try {
            return Files.exists(path) ? Files.size(path) : 0;
        } catch (IOException e) {
            return -1;
        }
    }

    private static Map<String, Object> fileInfo(Path path) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("path", path.toString());
        info.put("exists", Files.exists(path));
        try {
            info.put("bytes", Files.exists(path) ? Files.size(path) : 0);
            info.put("lastModified", Files.exists(path) ? Files.getLastModifiedTime(path).toInstant().toString() : "");
            info.put("sha256", Files.exists(path) ? sha256(Files.readString(path, StandardCharsets.UTF_8)) : "");
        } catch (IOException e) {
            info.put("error", e.getMessage());
        }
        return info;
    }

    private static void appendJsonLine(Path path, Map<String, Object> event) throws IOException {
        Files.createDirectories(path.getParent());
        String line = jsonObject(event) + "\n";
        Files.writeString(path, line, StandardCharsets.UTF_8, Files.exists(path)
                ? java.nio.file.StandardOpenOption.APPEND
                : java.nio.file.StandardOpenOption.CREATE);
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static void send(HttpExchange exchange, int status, String body, String contentType) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String readTextSafe(Path path, String fallback) {
        try {
            return Files.exists(path) ? Files.readString(path, StandardCharsets.UTF_8) : fallback;
        } catch (IOException e) {
            return fallback;
        }
    }

    private static long countLines(Path path) {
        if (!Files.exists(path)) {
            return 0;
        }
        try (Stream<String> stream = Files.lines(path, StandardCharsets.UTF_8)) {
            return stream.count();
        } catch (IOException e) {
            return -1;
        }
    }

    private static long fileBytes(Path path) {
        if (!Files.exists(path)) {
            return 0;
        }
        try {
            return Files.size(path);
        } catch (IOException e) {
            return -1;
        }
    }

    private static Map<String, Object> libraryGrowthSummary() {
        List<Map<String, Object>> sources = List.of(
                sourceSummary("system_tests", TEST_LOG),
                sourceSummary("ab_tests", AB_LOG),
                sourceSummary("training_runs", TRAINING_LOG),
                sourceSummary("recursive_training_epochs", TRAINING_EPOCH_LOG),
                sourceSummary("loihi_experiments", LOIHI_LOG),
                sourceSummary("benchmark_snapshots", BENCHMARK_LOG),
                sourceSummary("ascii_epoch_queue", ASCII_EPOCH_LOG),
                sourceSummary("epoch_upgrade_proofs", EPOCH_UPGRADE_LOG),
                sourceSummary("epoch_implementation_queue", EPOCH_IMPLEMENT_LOG),
                sourceSummary("algebraic_pattern_flows", ALGEBRAIC_FLOW_LOG),
                sourceSummary("persistence_events", PERSISTENCE_LOG),
                sourceSummary("web_source_manifest", WEB_SOURCE_LOG),
                sourceSummary("darwin_test_programs", DARWIN_TEST_PROGRAM_LOG),
                sourceSummary("darwin_algorithm_registry", DARWIN_ALGORITHM_REGISTRY_LOG),
                sourceSummary("darwin_algorithm_generations", DARWIN_GENERATION_LOG),
                sourceSummary("darwin_algorithm_winners", DARWIN_WINNER_LOG)
        );
        long totalBytes = 0;
        long totalRecords = 0;
        for (Map<String, Object> source : sources) {
            totalBytes += numericLong(source.get("bytes"));
            totalRecords += numericLong(source.get("records"));
        }
        Map<String, Object> rate = growthRateSummary(totalRecords, totalBytes, List.of(
                TEST_LOG,
                AB_LOG,
                TRAINING_LOG,
                TRAINING_EPOCH_LOG,
                LOIHI_LOG,
                BENCHMARK_LOG,
                ASCII_EPOCH_LOG,
                EPOCH_UPGRADE_LOG,
                EPOCH_IMPLEMENT_LOG,
                ALGEBRAIC_FLOW_LOG,
                PERSISTENCE_LOG,
                DARWIN_TEST_PROGRAM_LOG,
                DARWIN_ALGORITHM_REGISTRY_LOG,
                DARWIN_GENERATION_LOG,
                DARWIN_WINNER_LOG
        ));
        return mapOf(
                "status", "ok",
                "timestamp", Instant.now().toString(),
                "library", mapOf(
                        "totalRecords", totalRecords,
                        "totalBytes", totalBytes,
                        "totalBytesHuman", humanBytes(totalBytes),
                        "sourceCount", sources.size()
                ),
                "progress", mapOf(
                        "algebraicRuns", countLines(ALGEBRAIC_FLOW_LOG),
                        "permutationsCaptured", sumRegexInt(ALGEBRAIC_FLOW_LOG, Pattern.compile("\"testedPermutations\":(\\d+)")),
                        "physicsComparisons", sumRegexInt(ALGEBRAIC_FLOW_LOG, Pattern.compile("\"comparisonCount\":(\\d+)")),
                        "physicsEvolutionRounds", sumRegexInt(ALGEBRAIC_FLOW_LOG, Pattern.compile("\"physicsEvolutionRefinement\":\\{\"status\":\"generated\".*?\"rounds\":(\\d+)")),
                        "targetGradeTemplates", countRegexHits(ALGEBRAIC_FLOW_LOG, Pattern.compile("\"topologicalTemplatePromotion\":\\{\"status\":\"target_grade_template_ready\"")),
                        "advanceTemplates", countRegexHits(ALGEBRAIC_FLOW_LOG, Pattern.compile("\"topologicalTemplatePromotion\":\\{\"status\":\"advance_to_code_template\"")),
                        "darwinGenerations", countLines(DARWIN_GENERATION_LOG),
                        "darwinWinners", countLines(DARWIN_WINNER_LOG)
                ),
                "rate", rate,
                "observedEntries", topRegexCounts(ALGEBRAIC_FLOW_LOG, Pattern.compile("\"entryPoint\":\"([^\"]+)\""), 8),
                "observedExits", topRegexCounts(ALGEBRAIC_FLOW_LOG, Pattern.compile("\"exitPoint\":\"([^\"]+)\""), 8),
                "templateFamilies", topRegexCounts(ALGEBRAIC_FLOW_LOG, Pattern.compile("\"finalFamily\":\"([^\"]+)\""), 6),
                "webSources", webSourceCandidates(),
                "sources", sources
        );
    }

    private static Map<String, Object> sourceSummary(String sourceId, Path path) {
        long records = countLines(path);
        long bytes = fileBytes(path);
        return mapOf(
                "sourceId", sourceId,
                "path", path.toString(),
                "records", records,
                "bytes", bytes,
                "bytesHuman", humanBytes(bytes),
                "lastModified", fileInfo(path).get("lastModified")
        );
    }

    private static long sumRegexInt(Path path, Pattern pattern) {
        if (!Files.exists(path)) {
            return 0;
        }
        long sum = 0;
        try (Stream<String> stream = Files.lines(path, StandardCharsets.UTF_8)) {
            for (String line : (Iterable<String>) stream::iterator) {
                Matcher matcher = pattern.matcher(line);
                while (matcher.find()) {
                    sum += parseInt(matcher.group(1), 0);
                }
            }
        } catch (IOException ignored) {
            return 0;
        }
        return sum;
    }

    private static Map<String, Object> growthRateSummary(long totalRecords, long totalBytes, List<Path> paths) {
        Instant earliest = null;
        Instant latest = null;
        Pattern timestampPattern = Pattern.compile("\"timestamp\":\"([^\"]+)\"");
        for (Path path : paths) {
            if (!Files.exists(path)) {
                continue;
            }
            try (Stream<String> stream = Files.lines(path, StandardCharsets.UTF_8)) {
                for (String line : (Iterable<String>) stream::iterator) {
                    Matcher matcher = timestampPattern.matcher(line);
                    while (matcher.find()) {
                        try {
                            Instant ts = Instant.parse(matcher.group(1));
                            if (earliest == null || ts.isBefore(earliest)) {
                                earliest = ts;
                            }
                            if (latest == null || ts.isAfter(latest)) {
                                latest = ts;
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            } catch (IOException ignored) {
                return mapOf("status", "scan_error");
            }
        }
        if (earliest == null || latest == null || !latest.isAfter(earliest)) {
            return mapOf("status", "insufficient_window");
        }
        double hours = Math.max(0.001, Duration.between(earliest, latest).toMinutes() / 60.0);
        double recordsPerHour = totalRecords / hours;
        double bytesPerHour = totalBytes / hours;
        double recordsTo500MHours = recordsPerHour > 0 ? 500_000_000.0 / recordsPerHour : Double.POSITIVE_INFINITY;
        return mapOf(
                "status", "estimated",
                "earliest", earliest.toString(),
                "latest", latest.toString(),
                "windowHours", round3(hours),
                "recordsPerHour", round3(recordsPerHour),
                "bytesPerHour", round3(bytesPerHour),
                "bytesPerHourHuman", humanBytes((long) bytesPerHour) + "/h",
                "recordsTo500MHours", round3(recordsTo500MHours),
                "recordsTo500MYears", round3(recordsTo500MHours / (24.0 * 365.0))
        );
    }

    private static long countRegexHits(Path path, Pattern pattern) {
        if (!Files.exists(path)) {
            return 0;
        }
        long count = 0;
        try (Stream<String> stream = Files.lines(path, StandardCharsets.UTF_8)) {
            for (String line : (Iterable<String>) stream::iterator) {
                Matcher matcher = pattern.matcher(line);
                while (matcher.find()) {
                    count++;
                }
            }
        } catch (IOException ignored) {
            return 0;
        }
        return count;
    }

    private static List<Object> topRegexCounts(Path path, Pattern pattern, int limit) {
        Map<String, Long> counts = new LinkedHashMap<>();
        if (Files.exists(path)) {
            try (Stream<String> stream = Files.lines(path, StandardCharsets.UTF_8)) {
                for (String line : (Iterable<String>) stream::iterator) {
                    Matcher matcher = pattern.matcher(line);
                    while (matcher.find()) {
                        String key = matcher.group(1);
                        counts.put(key, counts.getOrDefault(key, 0L) + 1L);
                    }
                }
            } catch (IOException ignored) {
                return List.of();
            }
        }
        List<Map.Entry<String, Long>> ordered = new ArrayList<>(counts.entrySet());
        ordered.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        List<Object> top = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, ordered.size()); i++) {
            Map.Entry<String, Long> entry = ordered.get(i);
            top.add(mapOf("name", entry.getKey(), "count", entry.getValue()));
        }
        return top;
    }

    private static List<Object> webSourceCandidates() {
        List<Object> sources = new ArrayList<>();
        for (String line : readJsonLines(WEB_SOURCE_LOG, 50)) {
            sources.add(mapOf(
                    "sourceId", extractJsonString(line, "sourceId", "unknown"),
                    "kind", extractJsonString(line, "kind", "unknown"),
                    "url", extractJsonString(line, "url", ""),
                    "status", extractJsonString(line, "status", "candidate"),
                    "priority", extractJsonString(line, "priority", "secondary"),
                    "crawlMode", extractJsonString(line, "crawlMode", "unspecified"),
                    "scope", extractJsonString(line, "scope", ""),
                    "why", extractJsonString(line, "why", "")
            ));
        }
        return sources;
    }

    private static long numericLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value != null) {
            try {
                return Long.parseLong(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private static String humanBytes(long bytes) {
        if (bytes < 0) {
            return "unknown";
        }
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format("%.1f KB", kb);
        }
        double mb = kb / 1024.0;
        if (mb < 1024) {
            return String.format("%.1f MB", mb);
        }
        double gb = mb / 1024.0;
        return String.format("%.2f GB", gb);
    }

    private static String tail(Path path, int lines) {
        if (!Files.exists(path)) {
            return "";
        }
        try {
            List<String> all = Files.readAllLines(path, StandardCharsets.UTF_8);
            int start = Math.max(0, all.size() - lines);
            return String.join("\n", all.subList(start, all.size()));
        } catch (IOException e) {
            return "TAIL_ERROR: " + e.getMessage();
        }
    }

    private static List<String> readJsonLines(Path path, int limit) {
        if (!Files.exists(path)) {
            return List.of();
        }
        try {
            List<String> all = Files.readAllLines(path, StandardCharsets.UTF_8);
            List<String> nonBlank = new ArrayList<>();
            for (String line : all) {
                if (!line.isBlank()) {
                    nonBlank.add(line);
                }
            }
            int start = Math.max(0, nonBlank.size() - limit);
            return nonBlank.subList(start, nonBlank.size());
        } catch (IOException e) {
            return List.of(jsonObject(mapOf("error", e.getMessage())));
        }
    }

    private static List<Object> readJsonFragments(Path path, int limit) {
        List<Object> fragments = new ArrayList<>();
        for (String line : readJsonLines(path, limit)) {
            String trimmed = line.trim();
            if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
                    (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
                fragments.add(new JsonFragment(trimmed));
            } else {
                fragments.add(line);
            }
        }
        return fragments;
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> map = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return map;
        }
        for (String part : rawQuery.split("&")) {
            String[] pair = part.split("=", 2);
            String key = decode(pair[0]);
            String value = pair.length > 1 ? decode(pair[1]) : "";
            map.put(key, value);
        }
        return map;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static double extractJsonNumber(String body, String key, double fallback) {
        if (body == null || body.isBlank()) {
            return fallback;
        }
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");
        Matcher matcher = pattern.matcher(body);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static String extractJsonString(String body, String key, String fallback) {
        if (body == null || body.isBlank()) {
            return fallback;
        }
        String needle = "\"" + key + "\"";
        int keyAt = body.indexOf(needle);
        if (keyAt < 0) {
            return fallback;
        }
        int colonAt = body.indexOf(':', keyAt + needle.length());
        if (colonAt < 0) {
            return fallback;
        }
        int valueAt = colonAt + 1;
        while (valueAt < body.length() && Character.isWhitespace(body.charAt(valueAt))) {
            valueAt++;
        }
        if (valueAt >= body.length()) {
            return fallback;
        }
        if (body.charAt(valueAt) == '"') {
            StringBuilder out = new StringBuilder();
            boolean escaping = false;
            for (int i = valueAt + 1; i < body.length(); i++) {
                char ch = body.charAt(i);
                if (escaping) {
                    out.append(switch (ch) {
                        case 'n' -> '\n';
                        case 'r' -> '\r';
                        case 't' -> '\t';
                        default -> ch;
                    });
                    escaping = false;
                } else if (ch == '\\') {
                    escaping = true;
                } else if (ch == '"') {
                    String value = out.toString().trim();
                    return value.isBlank() ? fallback : value;
                } else {
                    out.append(ch);
                }
            }
            return fallback;
        }
        int endAt = valueAt;
        while (endAt < body.length() && body.charAt(endAt) != ',' && body.charAt(endAt) != '}') {
            endAt++;
        }
        String value = body.substring(valueAt, endAt).trim();
        return value.isBlank() ? fallback : value;
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "sha256_error";
        }
    }

    private static String jsonError(String error) {
        return jsonObject(mapOf("status", "error", "error", error));
    }

    private static Map<String, Object> mapOf(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            map.put(String.valueOf(values[i]), values[i + 1]);
        }
        return map;
    }

    private static String jsonObject(Map<String, ?> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append("\"").append(escape(entry.getKey())).append("\":").append(jsonValue(entry.getValue()));
        }
        sb.append("}");
        return sb.toString();
    }

    private static String jsonValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof JsonFragment fragment) {
            return fragment.json();
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof Map<?, ?> nested) {
            Map<String, Object> clean = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : nested.entrySet()) {
                clean.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return jsonObject(clean);
        }
        if (value instanceof Iterable<?> iterable) {
            List<String> parts = new ArrayList<>();
            for (Object item : iterable) {
                parts.add(jsonValue(item));
            }
            return "[" + String.join(",", parts) + "]";
        }
        return "\"" + escape(String.valueOf(value)) + "\"";
    }

    private static String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    private record JsonFragment(String json) {}

    private static String html() {
        return """
                <!doctype html>
                <html>
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>VIPER Java SDK</title>
                  <style>
                    :root { color-scheme: dark; font-family: Consolas, "Cascadia Mono", monospace; }
                    * { box-sizing: border-box; }
                    body { margin:0; min-height:100vh; background:#0d1117; color:#c9d1d9; }
                    header { height:44px; display:flex; align-items:center; justify-content:space-between; padding:0 14px; background:#161b22; border-bottom:1px solid #30363d; }
                    header strong { color:#f0f6fc; font-size:14px; }
                    header span { color:#8b949e; font-size:12px; }
                    .shell { display:grid; grid-template-columns:48px 250px 1fr 360px; height:calc(100vh - 44px); }
                    .rail { background:#0d1117; border-right:1px solid #30363d; padding:8px 6px; display:flex; flex-direction:column; gap:8px; }
                    .rail button { height:36px; border:0; border-radius:6px; background:#161b22; color:#c9d1d9; cursor:pointer; }
                    .rail button:hover { background:#1f6feb; color:white; }
                    .sidebar { background:#161b22; border-right:1px solid #30363d; padding:12px; overflow:auto; }
                    .sidebar h2, .panel h2 { margin:0 0 10px; font-size:12px; letter-spacing:0; text-transform:uppercase; color:#8b949e; }
                    .tree { display:flex; flex-direction:column; gap:6px; }
                    .tree button, .cmd { width:100%; text-align:left; background:#0d1117; color:#c9d1d9; border:1px solid #30363d; border-radius:6px; padding:8px; cursor:pointer; }
                    .tree button:hover, .cmd:hover { border-color:#58a6ff; }
                    main { overflow:auto; background:#0d1117; }
                    .tabs { display:flex; height:36px; background:#161b22; border-bottom:1px solid #30363d; }
                    .tab { padding:10px 14px; border-right:1px solid #30363d; font-size:12px; color:#8b949e; }
                    .tab.active { background:#0d1117; color:#f0f6fc; }
                    .editor { padding:16px; display:grid; grid-template-columns:repeat(2,minmax(260px,1fr)); gap:12px; }
                    .panel { border:1px solid #30363d; border-radius:6px; background:#0f1520; padding:12px; min-height:130px; }
                    .panel.wide { grid-column:1 / -1; }
                    label { display:block; font-size:12px; color:#8b949e; margin:8px 0 4px; }
                    input, textarea, select { width:100%; background:#010409; color:#c9d1d9; border:1px solid #30363d; border-radius:6px; padding:8px; font-family:inherit; }
                    textarea { min-height:92px; resize:vertical; }
                    canvas { width:100%; height:230px; display:block; background:#010409; border:1px solid #30363d; border-radius:6px; }
                    pre { margin:0; white-space:pre-wrap; word-break:break-word; color:#d2a8ff; font-size:12px; line-height:1.45; }
                    aside { background:#0f1520; border-left:1px solid #30363d; padding:12px; overflow:auto; }
                    .status { display:grid; grid-template-columns:1fr auto; gap:6px; font-size:12px; margin-bottom:8px; }
                    .ok { color:#3fb950; }
                    .bad { color:#f85149; }
                    .muted { color:#8b949e; }
    .proposal-grid { display:grid; grid-template-columns:1fr; gap:10px; margin-top:10px; }
    .epoch-card { border:2px solid #f2cc60; border-left:10px solid #f2cc60; border-radius:6px; background:#161b22; padding:10px; box-shadow:0 0 0 1px #3d2f00 inset; }
    .epoch-card h3 { margin:0 0 8px; color:#f2cc60; font-size:14px; }
    .epoch-card b { color:#f0f6fc; }
    .hot { background:#f2cc60; color:#010409; padding:1px 4px; border-radius:3px; font-weight:bold; }
    .tbd { color:#ffa657; font-weight:bold; }
    .diagram { color:#a5d6ff; background:#010409; border:1px solid #30363d; border-radius:6px; padding:8px; margin-top:8px; }
    .action-row { display:flex; gap:8px; flex-wrap:wrap; margin-top:10px; }
    .cmd.primary { background:#f2cc60; color:#010409; border-color:#f2cc60; font-weight:bold; }
    .cmd.primary:hover { background:#ffdf7a; border-color:#ffdf7a; }
    .approval-banner { border:2px solid #f2cc60; border-radius:6px; background:#201800; color:#f8e7a8; padding:10px; margin-bottom:10px; }
    .approval-banner strong { color:#fff3bf; }
  </style>
                </head>
                <body>
                  <header><strong>VIPER Java SDK <span class="muted">v0.4.1-training-lab</span></strong><span>persistent lab | tests | AB | training | Loihi topology | algebraic cube</span></header>
                  <div class="shell">
                    <div class="rail">
                      <button title="State" onclick="loadState()">S</button>
                      <button title="Tests" onclick="runTest()">T</button>
                      <button title="A/B" onclick="logAb()">A/B</button>
                      <button title="Training" onclick="logTraining()">TR</button>
                      <button title="Benchmarks" onclick="captureBenchmark()">BM</button>
                      <button title="ASCII Epochs" onclick="queueAsciiEpoch()">AE</button>
                      <button title="Upgrade Proof" onclick="runUpgradeProof()">UP</button>
                      <button title="Loihi" onclick="logLoihi()">L</button>
                      <button title="Design" onclick="loadDesign()">D</button>
                    </div>
                    <div class="sidebar">
                      <h2>Explorer</h2>
                      <div class="tree">
                        <button onclick="tail('system')">system_log.txt</button>
                        <button onclick="tail('shipper')">logic_shipper.log</button>
                        <button onclick="tail('topology')">topology_sidecar.log</button>
                        <button onclick="tail('tests')">system_tests.jsonl</button>
                        <button onclick="tail('ab')">ab_tests.jsonl</button>
                        <button onclick="tail('training')">training_runs.jsonl</button>
                        <button onclick="tail('recursive_training')">recursive_training_epochs.jsonl</button>
        <button onclick="tail('benchmarks')">benchmark_snapshots.jsonl</button>
        <button onclick="tail('ascii_epochs')">ascii_epoch_queue.jsonl</button>
        <button onclick="tail('epoch_upgrades')">epoch_upgrade_proofs.jsonl</button>
        <button onclick="tail('epoch_implementations')">epoch_implementation_queue.jsonl</button>
        <button onclick="tail('loihi')">loihi_experiments.jsonl</button>
        <button onclick="tail('algebraic_flows')">algebraic_pattern_flows.jsonl</button>
        <button onclick="tail('darwin_programs')">darwin_test_programs.jsonl</button>
        <button onclick="tail('darwin_algorithms')">darwin_algorithm_registry.jsonl</button>
        <button onclick="tail('darwin_generations')">darwin_algorithm_generations.jsonl</button>
        <button onclick="tail('darwin_winners')">darwin_algorithm_winners.jsonl</button>
        <button onclick="tail('persistence')">persistence_events.jsonl</button>
      </div>
                    </div>
                    <main>
                      <div class="tabs"><div class="tab active">control.sdk</div><div class="tab">settings.json</div><div class="tab">loihi.plan</div><div class="tab">algebra.cube</div></div>
                      <div class="editor">
                        <div class="panel">
                          <h2>Quick Test</h2>
                          <label>Test name</label><input id="testName" value="end_to_end_health">
                          <label>One variable</label><input id="variable" value="reply_headroom">
                          <button class="cmd" onclick="runTest()">Run Java SDK Test</button>
                        </div>
                        <div class="panel">
                          <h2>Settings</h2>
                          <label>Mode</label><select id="mode"><option>chat</option><option selected>planning</option><option>build</option><option>training</option></select>
                          <label>Planning reply tokens</label><input id="planningTokens" value="1024">
                          <button class="cmd" onclick="saveSettings()">Persist Settings</button>
                        </div>
                        <div class="panel">
                          <h2>A/B Test</h2>
                          <label>Variant A</label><input id="variantA" value="current_lens">
                          <label>Variant B</label><input id="variantB" value="candidate_lens">
                          <button class="cmd" onclick="logAb()">Log A/B Plan</button>
                        </div>
                        <div class="panel">
                          <h2>Loihi Experiment</h2>
                          <label>Topology cube</label><input id="cube" value="100x100x100">
                          <label>Spike contract</label><input id="spike" value="x/y/z top-code weights, SHA-256 edge ids">
                          <button class="cmd" onclick="logLoihi()">Log Loihi Sidecar Experiment</button>
                        </div>
                        <div class="panel">
                          <h2>Training Run</h2>
                          <label>Dataset</label><input id="dataset" value="successful_code_and_liked_logic">
                          <label>Route</label><input id="trainRoute" value="proposal_only_lens_improvement">
                          <button class="cmd" onclick="logTraining()">Log Training Plan</button>
                        </div>
                        <div class="panel">
                          <h2>Recursive Epoch</h2>
                          <label>Changed variable</label><input id="epochVariable" value="retrieval_lens_rerank_weight">
                          <label>Dataset slice</label><input id="epochDataset" value="liked_logic_successful_code_recent_failures">
                          <button class="cmd" onclick="logRecursiveEpoch()">Log Proposal Epoch</button>
                        </div>
                        <div class="panel">
                          <h2>ASCII Epoch Queue</h2>
                          <label>Subsystem</label><select id="epochSubsystem"><option>chooser</option><option>db_retrieval</option><option>karoo</option><option>abliterated</option><option>loihi</option><option>lava</option><option>soap</option><option>ledger</option><option>network</option><option>java_sdk</option></select>
                          <label>Quick var</label><input id="quickVar" value="retrieval_weight">
                          <label>External judge</label><select id="judgeSlot"><option>local_benchmark</option><option>karoo_compare</option><option>tiny_critic</option><option>optional_copilot</option><option>optional_gemini</option><option>optional_cloud_agent</option></select>
                          <button class="cmd" onclick="queueAsciiEpoch()">Queue ASCII Epoch</button>
                        </div>
        <div class="panel">
          <h2>Upgrade Proof</h2>
          <label>Goal</label><input id="proofGoal" value="sovereign_orchestration_epoch">
          <label>Scope</label><input id="proofScope" value="bridge shipper karoo agent_contract">
          <button class="cmd" onclick="runUpgradeProof()">Analyze And Propose Epoch</button>
        </div>
        <div class="panel wide">
          <h2>Epoch Actions</h2>
          <div id="epochActions" class="muted">Run "Analyze And Propose Epoch" to load approval controls.</div>
        </div>
        <div class="panel wide">
          <h2>Algebraic Pattern Lab</h2>
          <label>Start data</label><select id="algStart"><option>topology_ascii</option><option>benchmark_json</option><option>epoch_proposal</option><option>successful_code_card</option><option>behavior_card</option><option>nominal_fact_card</option><option>source_tree_card</option><option>performative_acl_card</option><option>bytecode_signature_card</option></select>
          <label>End data</label><select id="algEnd"><option>code_pattern_card</option><option>logic_pattern_card</option><option>proof_card</option><option>route_plan</option><option>epoch_proposal</option><option>performative_route_card</option><option>bytecode_plan_card</option></select>
          <label>Max permutations</label><input id="algPermutations" value="4">
          <label>Physics genetic comparisons</label><input id="algPhysicsCount" value="50">
          <label>Evolution rounds</label><input id="algEvolutionRounds" value="50">
          <label>Comparison family</label><select id="algCompareFamily"><option selected>established_physics_grids</option><option>software_logic_alignment</option></select>
          <label>Objective</label><input id="algObjective" value="map algebraic patterns for code generation and logic patterns for routing">
          <label>ASCII flow override</label><textarea id="algAsciiFlow">START_DATA
+- compress.card
+- axiomatic_set_select
|  +- math_fit
|  +- logic_fit
+- transform.chain
|  +- entry_normalize
|  +- route_choose
|  +- model_hook
+- END_DATA

flow:
entry -> compact evidence -> axiomatic fit -> route/model -> proof/code/logic artifact</textarea>
          <label>Math and logic override</label><textarea id="algMathNotes">score = fit(entry, exit, constraints) + proof_weight + reuse_weight - risk_weight
choose argmax(score)
if tie then keep bounded top-k and compare with proof

sets:
- structure graph
- evidence proof
- code reuse
- behavior markov
- context anchor</textarea>
          <div class="action-row">
            <button class="cmd primary" onclick="runAlgebraicFlowLab()">Run Algebraic Flow Lab</button>
            <button class="cmd" onclick="runPhysicsGeneticCompare()">Run 50 Physics Genetic Comparisons</button>
            <button class="cmd" onclick="runChooserAB()">Run Predictive vs Top10 Brute</button>
            <button class="cmd" onclick="loadAlgebraicFlowLab()">Load Recent Algebraic Runs</button>
            <button class="cmd" onclick="tail('algebraic_flows')">View Algebraic Flow Log</button>
          </div>
          <div id="algebraicPanel" class="muted" style="margin-top:10px;">Run the algebraic flow lab to test bounded permutations of entry and exit data with live model probes.</div>
        </div>
        <div class="panel wide">
          <h2>Darwin Program Lab</h2>
          <label>Generations</label><input id="darwinGenerations" value="6">
          <label>Program limit</label><input id="darwinProgramLimit" value="8">
          <label>Mutation rate</label><input id="darwinMutationRate" value="0.08">
          <label>Objective</label><input id="darwinObjective" value="darwinistically evolve bounded algorithms against local test programs">
          <div class="action-row">
            <button class="cmd primary" onclick="runDarwinLab()">Run Darwin Lab</button>
            <button class="cmd" onclick="loadDarwinLab(true)">Load Darwin State</button>
            <button class="cmd" onclick="tail('darwin_generations')">View Darwin Generations</button>
            <button class="cmd" onclick="tail('darwin_winners')">View Darwin Winners</button>
          </div>
          <div id="darwinPanel" class="muted" style="margin-top:10px;">Run the Darwin lab to evolve bounded algorithm weights against local test programs.</div>
        </div>
        <div class="panel wide">
          <h2>Library Growth</h2>
          <div class="action-row">
            <button class="cmd primary" onclick="loadLibraryGrowth(true)">Refresh Library Growth</button>
            <button class="cmd" onclick="tail('algebraic_flows')">View Flow Source</button>
            <button class="cmd" onclick="tail('training')">View Training Source</button>
          </div>
          <div id="libraryGrowthPanel" class="muted" style="margin-top:10px;">Refresh to watch records, sources, and total library size grow.</div>
        </div>
        <div class="panel wide">
          <h2>Benchmarks</h2>
          <canvas id="benchChart" width="980" height="230"></canvas>
                          <button class="cmd" onclick="captureBenchmark()">Capture Benchmark Snapshot</button>
                        </div>
                        <div class="panel wide">
                          <h2>Output</h2>
                          <div id="out"><pre>Ready.</pre></div>
                        </div>
                      </div>
                    </main>
                    <aside>
                      <h2>Service Watch</h2>
                      <div id="watch" class="muted">Not loaded.</div>
                      <button class="cmd" onclick="loadState()">Refresh State</button>
                    </aside>
                  </div>
                  <script>
    const out = document.getElementById('out');
    const watch = document.getElementById('watch');
    const benchChart = document.getElementById('benchChart');
    const epochActions = document.getElementById('epochActions');
    const algebraicPanel = document.getElementById('algebraicPanel');
    const darwinPanel = document.getElementById('darwinPanel');
    const libraryGrowthPanel = document.getElementById('libraryGrowthPanel');
                    async function api(url, opts){ const r = await fetch(url, opts); const t = await r.text(); try { return JSON.parse(t); } catch { return {raw:t}; } }
                    function print(x){ out.innerHTML = `<pre>${esc(JSON.stringify(x, null, 2))}</pre>`; }
                    function esc(v){ return String(v ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c])); }
                    function printHtml(html){ out.innerHTML = html; }
                    async function loadState(){ const x = await api('/api/state'); print(x); renderWatch(x.services || {}); loadBenchmarks(false); loadLibraryGrowth(false); loadDarwinLab(false); }
                    function renderWatch(s){ watch.innerHTML = Object.entries(s).map(([k,v]) => `<div class="status"><span>${k}</span><span class="${v.ok?'ok':'bad'}">${v.ok?'ok':'down'}</span></div>`).join(''); }
                    async function loadLibraryGrowth(show){
                      const x = await api('/api/library-growth');
                      renderLibraryGrowth(x);
                      if (show) print(x);
                    }
                    function renderLibraryGrowth(x){
                      if (!libraryGrowthPanel) return;
                      const sources = (x.sources || []).map(source =>
                        `<div><b>${esc(source.sourceId)}:</b> ${esc(source.records)} records | ${esc(source.bytesHuman)} | ${esc(source.lastModified || '')}</div>`
                      ).join('');
                      const webSources = (x.webSources || []).map(source =>
                        `<div><b>${esc(source.sourceId)}:</b> ${esc(source.kind)} | ${esc(source.status || '')} | ${esc(source.priority || '')}<br><span class="muted">${esc(source.crawlMode || '')} | ${esc(source.scope || '')}</span><br><a href="${esc(source.url)}" target="_blank" rel="noreferrer">${esc(source.url)}</a><br><span class="muted">${esc(source.why)}</span></div>`
                      ).join('');
                      const entries = (x.observedEntries || []).map(item =>
                        `<div><b>${esc(item.name)}:</b> ${esc(item.count)}</div>`
                      ).join('');
                      const exits = (x.observedExits || []).map(item =>
                        `<div><b>${esc(item.name)}:</b> ${esc(item.count)}</div>`
                      ).join('');
                      const families = (x.templateFamilies || []).map(item =>
                        `<div><b>${esc(item.name)}:</b> ${esc(item.count)}</div>`
                      ).join('');
                      libraryGrowthPanel.innerHTML = `
                        <div class="approval-banner"><strong>Library:</strong> ${esc(x.library && x.library.totalRecords)} records | ${esc(x.library && x.library.totalBytesHuman)} | ${esc(x.library && x.library.sourceCount)} sources | <strong>Growth:</strong> home-grown local proof logs first</div>
                        <div class="epoch-card">
                          <h3>Progress <span class="hot">live</span></h3>
                          <div><b>Algebraic runs:</b> ${esc(x.progress && x.progress.algebraicRuns)}</div>
                          <div><b>Permutations captured:</b> ${esc(x.progress && x.progress.permutationsCaptured)}</div>
                          <div><b>Physics comparisons:</b> ${esc(x.progress && x.progress.physicsComparisons)}</div>
                          <div><b>Evolution rounds:</b> ${esc(x.progress && x.progress.physicsEvolutionRounds)}</div>
                          <div><b>Target-grade templates:</b> ${esc(x.progress && x.progress.targetGradeTemplates)}</div>
                          <div><b>Advance-only templates:</b> ${esc(x.progress && x.progress.advanceTemplates)}</div>
                          <div><b>Darwin generations:</b> ${esc(x.progress && x.progress.darwinGenerations)}</div>
                          <div><b>Darwin winners:</b> ${esc(x.progress && x.progress.darwinWinners)}</div>
                          <div style="margin-top:8px;"><b>Rate:</b> ${esc(x.rate && x.rate.recordsPerHour)} records/h | ${esc(x.rate && x.rate.bytesPerHourHuman)}</div>
                          <div><b>Window:</b> ${esc(x.rate && x.rate.windowHours)} h | <b>500M at current rate:</b> ${esc(x.rate && x.rate.recordsTo500MYears)} years</div>
                        </div>
                        <div class="epoch-card">
                          <h3>Sources <span class="hot">bytes</span></h3>
                          ${sources || '<div class="muted">No sources yet.</div>'}
                        </div>
                        <div class="epoch-card">
                          <h3>Web Sources <span class="hot">planned</span></h3>
                          ${webSources || '<div class="muted">No web sources yet.</div>'}
                        </div>
                        <div class="epoch-card">
                          <h3>Observed Entries <span class="hot">top</span></h3>
                          ${entries || '<div class="muted">No entries yet.</div>'}
                          <div style="margin-top:8px;"><b>Observed exits:</b></div>
                          ${exits || '<div class="muted">No exits yet.</div>'}
                          <div style="margin-top:8px;"><b>Template families:</b></div>
                          ${families || '<div class="muted">No families yet.</div>'}
                        </div>`;
                    }
                    async function saveSettings(){
                      const body = {mode:mode.value, planningReplyTokens:Number(planningTokens.value), karooProposalOnly:true, heartbeatSeconds:300};
                      print(await api('/api/settings', {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify(body,null,2)}));
                    }
                    async function runTest(){
                      const body = {testName:testName.value, variable:variable.value, rule:'one variable per test', timestamp:new Date().toISOString()};
                      print(await api('/api/run-test', {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify(body)}));
                    }
                    async function logAb(){
                      const body = {variantA:variantA.value, variantB:variantB.value, metric:'success + speed + resources', promotionGate:'99.99% and 10%'};
                      print(await api('/api/ab-test', {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify(body)}));
                    }
                    async function logLoihi(){
                      const body = {cube:cube.value, spike:spike.value, mode:'proposal_simulation', bridge:'NLP -> top codes -> spikes -> logic deltas'};
                      print(await api('/api/loihi-experiment', {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify(body)}));
                    }
                    async function logTraining(){
                      const body = {dataset:dataset.value, route:trainRoute.value, rule:'log first; proposal-only until promotion gate'};
                      print(await api('/api/training', {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify(body)}));
                    }
                    async function logRecursiveEpoch(){
                      const body = {
                        changedVariable:epochVariable.value,
                        datasetSlice:epochDataset.value,
                        rule:'proposal/eval only; one variable; no model-weight mutation',
                        promotionGate:'99.99% success and +10% speed or -10% resources'
                      };
                      const x = await api('/api/recursive-training', {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify(body)});
                      print(x);
                      await loadBenchmarks(false);
                    }
                    async function queueAsciiEpoch(){
                      const body = {
                        subsystem:epochSubsystem.value,
                        quickVar:quickVar.value,
                        judgeSlot:judgeSlot.value,
                        rule:'always keep new ASCII epochs waiting; optional outside judge weighs only',
                        theme:'keep current VS Code dark SDK theme'
                      };
                      const x = await api('/api/ascii-epochs', {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify(body)});
                      print({status:x.status || 'queued', version:x.version, sha256:x.sha256, proposedDiagram:x.proposedDiagram, full:x});
                    }
                    async function runUpgradeProof(){
                      const body = {
                        goal:proofGoal.value,
                        scope:proofScope.value,
                        rule:'analyze evidence and produce concrete proposed epoch changes; no auto-apply'
                      };
                      const x = await api('/api/epoch-upgrade-proof', {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify(body)});
                      renderUpgradeProof(x);
                    }
                    async function runAlgebraicFlowLab(){
                      const body = {
                        startData: algStart.value,
                        endData: algEnd.value,
                        maxPermutations: algPermutations.value,
                        includeModelProbes: true,
                        objective: algObjective.value,
                        customAsciiFlow: algAsciiFlow.value,
                        customMathNotes: algMathNotes.value
                      };
                      const x = await api('/api/algebraic-flow', {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify(body)});
                      renderAlgebraicFlow(x);
                      print(x);
                    }
                    async function runPhysicsGeneticCompare(){
                      const body = {
                        startData: algStart.value,
                        endData: algEnd.value,
                        maxPermutations: Math.max(Number(algPermutations.value || 4), 8),
                        includeModelProbes: false,
                        objective: algObjective.value + ' | compare genetically against established physics-style processing grids',
                        customAsciiFlow: algAsciiFlow.value,
                        customMathNotes: algMathNotes.value,
                        physicsComparisons: algPhysicsCount.value,
                        physicsEvolutionRounds: algEvolutionRounds.value,
                        comparisonFamily: algCompareFamily.value
                      };
                      const x = await api('/api/algebraic-flow', {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify(body)});
                      renderAlgebraicFlow(x);
                      print(x);
                    }
                    async function runChooserAB(){
                      const body = {
                        startData: algStart.value,
                        endData: algEnd.value,
                        maxPermutations: Math.max(Number(algPermutations.value || 4), 8),
                        includeModelProbes: false,
                        objective: algObjective.value + ' | compare predictive hybrid against bounded top10 brute force',
                        customAsciiFlow: algAsciiFlow.value,
                        customMathNotes: algMathNotes.value,
                        chooserExperiment: 'predictive_vs_bruteforce'
                      };
                      const x = await api('/api/algebraic-flow', {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify(body)});
                      renderAlgebraicFlow(x);
                      print(x);
                    }
                    async function runDarwinLab(){
                      const body = {
                        generations: darwinGenerations.value,
                        programLimit: darwinProgramLimit.value,
                        mutationRate: darwinMutationRate.value,
                        objective: darwinObjective.value,
                        strategy: 'bruteforce_matrix_primary'
                      };
                      const x = await api('/api/darwin-lab', {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify(body)});
                      renderDarwinLab(x);
                      print(x);
                    }
                    async function loadDarwinLab(show){
                      const x = await api('/api/darwin-lab?limit=8');
                      renderDarwinLab(x);
                      if (show) print(x);
                    }
                    async function loadAlgebraicFlowLab(){
                      const x = await api('/api/algebraic-flow?limit=8');
                      renderAlgebraicFlow(x);
                      print(x);
                    }
                    function renderDarwinLab(x){
                      if (!darwinPanel) return;
                      const programs = (x.programs || []).map(program =>
                        `<div><b>${esc(program.programId)}:</b> ${esc(program.language)} | topology ${esc(program.topologyKind)} | brute ${esc(program.bruteForceFriendliness)} | route ${esc(program.routePressure)} | target ${esc(program.acceptanceTarget || '')}</div>`
                      ).join('');
                      const algorithms = (x.algorithms || []).map(algorithm =>
                        `<div><b>${esc(algorithm.algorithmId)}:</b> ${esc(algorithm.family)} | brute ${esc(algorithm.bruteForceWeight)} | topology ${esc(algorithm.topologyWeight)} | proof ${esc(algorithm.proofWeight)}</div>`
                      ).join('');
                      const generations = (x.generationSummaries || x.recentGenerations || []).map(gen =>
                        `<div><b>Gen ${esc(gen.generation || '')}:</b> baseline ${esc((gen.baseline && gen.baseline.algorithmId) || '')} -> winner ${esc((gen.best && gen.best.algorithmId) || '')} | fitness ${esc((gen.best && gen.best.fitness) || '')} | beat baseline ${esc(gen.winnerBeatsBaseline)}</div>`
                      ).join('');
                      const winner = x.finalWinner || ((x.recentWinners && x.recentWinners[0]) || null);
                      const baseline = x.finalBaseline || null;
                      const winnerHtml = winner
                        ? `<pre class="diagram">${esc(JSON.stringify(winner, null, 2))}</pre>`
                        : '<div class="muted">No winner yet.</div>';
                      const baselineHtml = baseline
                        ? `<pre class="diagram">${esc(JSON.stringify(baseline, null, 2))}</pre>`
                        : '<div class="muted">No baseline loaded.</div>';
                      const summary = x.summary
                        ? `<div class="approval-banner"><strong>Darwin:</strong> ${esc(x.summary.status)} | <strong>Programs:</strong> ${esc(x.summary.programCount)} | <strong>Generations:</strong> ${esc(x.summary.generationCount)} | <strong>Baseline:</strong> ${esc(x.summary.baselineAlgorithmId || '')} | <strong>Winner:</strong> ${esc(x.summary.winnerAlgorithmId)} | <strong>Fitness:</strong> ${esc(x.summary.winnerFitness)} | <strong>Beat baseline:</strong> ${esc(x.summary.winnerBeatsBaseline)} | <strong>Reason:</strong> ${esc(x.summary.winnerReason || '')}</div>`
                        : `<div class="approval-banner"><strong>Darwin state loaded.</strong></div>`;
                      darwinPanel.innerHTML = `${summary}
                        <div class="epoch-card">
                          <h3>Test Programs <span class="hot">${esc((x.programs || []).length)}</span></h3>
                          ${programs || '<div class="muted">No test programs loaded.</div>'}
                        </div>
                        <div class="epoch-card">
                          <h3>Algorithm Registry <span class="hot">${esc((x.algorithms || []).length)}</span></h3>
                          ${algorithms || '<div class="muted">No algorithms loaded.</div>'}
                        </div>
                        <div class="epoch-card">
                          <h3>Generation Lineage <span class="hot">recent</span></h3>
                          ${generations || '<div class="muted">No generations logged yet.</div>'}
                        </div>
                        <div class="epoch-card">
                          <h3>Current Baseline <span class="hot">guard</span></h3>
                          ${baselineHtml}
                        </div>
                        <div class="epoch-card">
                          <h3>Current Winner <span class="hot">db</span></h3>
                          ${winnerHtml}
                        </div>`;
                    }
                    async function acceptProposal(index){
                      const proposal = (window.lastUpgradeProof && window.lastUpgradeProof.proposals && window.lastUpgradeProof.proposals[index]) || null;
                      if (!proposal) {
                        print({status:'error', error:'proposal_not_found', index});
                        return;
                      }
                      const body = {
                        proposalId: proposal.id,
                        goal: proofGoal.value,
                        subsystem: proposal.subsystem,
                        proposedChange: proposal.proposedChange,
                        acceptanceTest: proposal.acceptanceTest,
                        evidence: proposal.evidence,
                        implementationMode: 'bridge_build_request'
                      };
                      const x = await api('/api/epoch-implement', {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify(body)});
                      renderEpochActionResult(proposal, x);
                      print(x);
                    }
                    function renderEpochActionResult(proposal, x){
                      if (!epochActions) return;
                      epochActions.innerHTML = `<div class="approval-banner"><strong>Accepted:</strong> ${esc(proposal.id)} <span class="hot">${esc(proposal.subsystem)}</span><br><strong>Status:</strong> ${esc(x.implementationStatus || x.status || 'queued')}<br><strong>Queue SHA-256:</strong> ${esc(x.sha256 || '')}</div>`;
                    }
                    function renderEpochActions(x){
                      if (!epochActions) return;
                      const proposals = x.proposals || [];
                      if (!proposals.length) {
                        epochActions.innerHTML = '<div class="muted">No epoch proposals available.</div>';
                        return;
                      }
                      const controls = proposals.map((p, index) => `
                        <div class="epoch-card">
                          <h3>${esc(p.id)} <span class="hot">${esc(p.subsystem)}</span></h3>
                          <div><b>Proposed Change:</b> ${esc(p.proposedChange)}</div>
                          <div><b>Approval:</b> <span class="tbd">${esc(p.approvalStatus || 'waiting_for_user')}</span></div>
                          <div class="action-row">
                            <button class="cmd primary" onclick="acceptProposal(${index})">Implement This Epoch</button>
                            <button class="cmd" onclick="tail('epoch_implementations')">View Implementation Queue</button>
                          </div>
                        </div>`).join('');
                      epochActions.innerHTML = `<div class="approval-banner"><strong>Epoch proposals are ready.</strong> Choose one below to accept and queue implementation.</div>${controls}`;
                    }
                    function renderUpgradeProof(x){
                      window.lastUpgradeProof = x;
                      const cards = (x.proposals || []).map((p, index) => `
                        <div class="epoch-card">
                          <h3>${esc(p.id)} <span class="hot">${esc(p.subsystem)}</span></h3>
                          <div><b>Problem:</b> ${esc(p.problem)}</div>
                          <div><b>Evidence:</b> ${esc(p.evidence)}</div>
                          <div><b>PROPOSED CHANGE:</b> <span class="hot">${esc(p.proposedChange)}</span></div>
                          <div><b>Acceptance Test:</b> ${esc(p.acceptanceTest)}</div>
                          <div><b>Test Result:</b> <span class="tbd">${esc(p.testResult || 'TBD')}</span></div>
                          <div class="action-row"><button class="cmd primary" onclick="acceptProposal(${index})">Accept + Implement</button></div>
                          <pre class="diagram">${esc(p.diagram)}</pre>
                        </div>`).join('');
                      renderEpochActions(x);
                      printHtml(`<div><b>Version:</b> ${esc(x.version)} | <b>SHA-256:</b> ${esc(x.sha256)} | <span class="tbd">${esc(x.approvalStatus)}</span></div>
                        <pre class="diagram">${esc(x.diagram)}</pre>
                        <div class="proposal-grid">${cards}</div>`);
                    }
                    function renderAlgebraicFlow(x){
                      if (!algebraicPanel) return;
                      const items = x.permutations || x.recentRuns || [];
                      if (!items.length){
                        algebraicPanel.innerHTML = '<div class="muted">No algebraic flow runs yet.</div>';
                        return;
                      }
                      const processable = (x.processableData || []).map(card =>
                        `<div><b>${esc(card.type)}:</b> ${esc(card.bestFor)}</div>`
                      ).join('');
                      const layeredDb = (x.layeredDbModel || []).length
                        ? `<div style="margin-top:8px;"><b>Layered DB model:</b></div><pre class="diagram">${esc(JSON.stringify(x.layeredDbModel, null, 2))}</pre>`
                        : '';
                      const hooks = x.hookupGuide ? Object.entries(x.hookupGuide).map(([k,v]) =>
                        `<div><b>${esc(k)}:</b> ${esc(v)}</div>`
                      ).join('') : '';
                      const chooserCompare = x.chooserComparison && x.chooserComparison.strategy
                        ? `<div style="margin-top:8px;"><b>Chooser compare:</b></div><pre class="diagram">${esc(JSON.stringify(x.chooserComparison, null, 2))}</pre>`
                        : '';
                      const chooserAB = x.chooserABExperiment && x.chooserABExperiment.status === 'generated' ? x.chooserABExperiment : null;
                      const physicsSuite = x.physicsComparisonSuite && x.physicsComparisonSuite.status === 'generated' ? x.physicsComparisonSuite : null;
                      const evolution = x.physicsEvolutionRefinement && x.physicsEvolutionRefinement.status === 'generated' ? x.physicsEvolutionRefinement : null;
                      const topologyGate = x.topologicalTemplatePromotion && x.topologicalTemplatePromotion.status ? x.topologicalTemplatePromotion : null;
                      const physicsTopMatches = physicsSuite
                        ? (physicsSuite.topMatches || []).map(match =>
                            `<div><b>${esc(match.gridId)}:</b> ${esc(match.matchScore)} ${esc(match.alignmentLevel)} :: ${esc(match.logicLineup)}</div>`
                          ).join('')
                        : '';
                      const physicsSummary = physicsSuite
                        ? `<div class="epoch-card"><h3>Physics Genetic Compare <span class="hot">${esc(physicsSuite.comparisonCount)}</span></h3><div><b>Family:</b> ${esc(physicsSuite.comparisonFamily)}</div><div><b>Lineup at any level:</b> ${esc(physicsSuite.lineupAtAnyLevel)}</div><div style="margin-top:8px;"><b>Alignment bands:</b></div><pre class="diagram">${esc(JSON.stringify(physicsSuite.alignmentBands || {}, null, 2))}</pre><div style="margin-top:8px;"><b>Top matches:</b></div>${physicsTopMatches}<div class="muted" style="margin-top:8px;">Full 50-comparison suite is in the JSON output panel.</div></div>`
                        : '';
                      const evolutionSummary = evolution
                        ? `<div class="epoch-card"><h3>Physics Evolution Refinement <span class="hot">${esc(evolution.rounds)}</span></h3><div><b>Seed:</b> ${esc(evolution.seedGrid)} ${esc(evolution.seedScore)}</div><div><b>Final:</b> ${esc(evolution.finalGrid)} ${esc(evolution.finalScore)}</div><div><b>Improvement:</b> ${esc(evolution.improvement)}</div><div><b>Accepted rounds:</b> ${esc(evolution.acceptedRounds)} | <b>Improved rounds:</b> ${esc(evolution.improvedRounds)}</div><div style="margin-top:8px;"><b>Dominant genes:</b></div><pre class="diagram">${esc(JSON.stringify(evolution.dominantGenes || [], null, 2))}</pre><div style="margin-top:8px;"><b>Best permutation:</b></div><pre class="diagram">${esc(JSON.stringify(evolution.bestPermutation || {}, null, 2))}</pre><div class="muted" style="margin-top:8px;">Full evolution lineage is in the JSON output panel.</div></div>`
                        : '';
                      const topologyGateSummary = topologyGate
                        ? `<div class="epoch-card"><h3>Topology Template Gate <span class="hot">${esc(topologyGate.status)}</span></h3><div><b>Alignment:</b> ${esc(topologyGate.alignmentScore)} | floor ${esc(topologyGate.advanceFloor)} | target ${esc(topologyGate.optimizeTarget)}</div><div><b>Target ready:</b> ${esc(topologyGate.targetReady)}</div><div><b>Topology anchored:</b> ${esc(topologyGate.topologyAnchored)}</div><div><b>Evidence:</b> ${esc(topologyGate.evidenceSource)}</div><div style="margin-top:8px;"><b>Best permutation:</b></div><pre class="diagram">${esc(JSON.stringify(topologyGate.bestPermutation || {}, null, 2))}</pre><div style="margin-top:8px;"><b>Action items:</b></div><pre class="diagram">${esc(JSON.stringify(topologyGate.actionItems || [], null, 2))}</pre><div style="margin-top:8px;"><b>Race-condition items:</b></div><pre class="diagram">${esc(JSON.stringify(topologyGate.raceConditionItems || [], null, 2))}</pre></div>`
                        : '';
                      const chooserABSummary = chooserAB
                        ? `<div class="epoch-card"><h3>Chooser A/B <span class="hot">${esc(chooserAB.comparison && chooserAB.comparison.recommendation)}</span></h3><div><b>Strategy:</b> ${esc(chooserAB.strategy)}</div><div><b>Brute inspected:</b> ${esc(chooserAB.bruteforceLane && chooserAB.bruteforceLane.inspected)} | <b>Hybrid inspected:</b> ${esc(chooserAB.predictiveHybridLane && chooserAB.predictiveHybridLane.inspected)}</div><div><b>Candidate saves:</b> ${esc(chooserAB.comparison && chooserAB.comparison.estimatedCandidateSaves)}</div><div><b>Brute winner:</b></div><pre class="diagram">${esc(JSON.stringify(chooserAB.bruteforceLane && chooserAB.bruteforceLane.winner || {}, null, 2))}</pre><div><b>Hybrid winner:</b></div><pre class="diagram">${esc(JSON.stringify(chooserAB.predictiveHybridLane && chooserAB.predictiveHybridLane.winner || {}, null, 2))}</pre><div><b>Hybrid trace:</b></div><pre class="diagram">${esc(JSON.stringify(chooserAB.predictiveHybridLane && chooserAB.predictiveHybridLane.trace || [], null, 2))}</pre></div>`
                        : '';
                      if (x.asciiFlow && document.getElementById('algAsciiFlow')) document.getElementById('algAsciiFlow').value = x.asciiFlow;
                      if (x.mathModel && document.getElementById('algMathNotes')) document.getElementById('algMathNotes').value = x.mathModel;
                      const cards = items.map(item => {
                        const p = item.entryPoint ? item : null;
                        if (!p) {
                          return `<pre class="diagram">${esc(JSON.stringify(item, null, 2))}</pre>`;
                        }
                        const probe = p.probe || {};
                        const probeClass = probe.status === 'pass' ? 'ok' : (probe.status === 'fail' ? 'bad' : 'tbd');
                        const scorecard = p.chooserScorecard || {};
                        const blockScale = p.blockScale || {};
                        return `
                          <div class="epoch-card">
                            <h3>${esc(p.entryPoint)} <span class="hot">${esc(p.exitPoint)}</span></h3>
                            <div><b>Axiomatic Set:</b> ${esc(p.axiomaticSet)}</div>
                            <div><b>Math Fit:</b> ${esc(p.mathFit)}</div>
                            <div><b>Logic Fit:</b> ${esc(p.logicFit)}</div>
                            <div><b>Hook:</b> ${esc(p.recommendedModelHook)} via ${esc(p.recommendedRoute)}</div>
                            <div><b>Probe:</b> <span class="${probeClass}">${esc(probe.status || 'unknown')}</span> ${esc(probe.durationMs || '')}ms</div>
                            <div><b>Block scale:</b> ${esc(blockScale.entryScale || '')} -> ${esc(blockScale.exitScale || '')} (${esc(blockScale.layerHint || '')})</div>
                            <div><b>Chooser score:</b> ${esc(scorecard.totalScore || '')} | proof ${esc(scorecard.proofHistory || '')} | timeout ${esc(scorecard.timeoutRisk || '')}</div>
                            <div><b>Entry Search:</b> ${esc((p.entryExitSearch && p.entryExitSearch.entrySearchFocus) || '')}</div>
                            <div><b>Exit Search:</b> ${esc((p.entryExitSearch && p.entryExitSearch.exitSearchFocus) || '')}</div>
                            <div><b>Sample:</b> ${esc(p.sampleData)}</div>
                            <pre class="diagram">${esc(JSON.stringify(p.performativeRules || [], null, 2))}</pre>
                            <pre class="diagram">${esc(JSON.stringify(p.bytecodePlan || {}, null, 2))}</pre>
                            <pre class="diagram">${esc(probe.responsePreview || '')}</pre>
                          </div>`;
                      }).join('');
                      const summary = x.summary
                        ? `<div class="approval-banner"><strong>Permutations:</strong> ${esc(x.summary.testedPermutations)} | <strong>Pass:</strong> ${esc(x.summary.pass)} | <strong>Fail:</strong> ${esc(x.summary.fail)} | <strong>Status:</strong> ${esc(x.summary.status)}</div>`
                        : `<div class="approval-banner"><strong>Recent algebraic runs loaded.</strong></div>`;
                      const registry = (x.ruleRegistry || []).length ? `<div style="margin-top:8px;"><b>Rule registry:</b></div><pre class="diagram">${esc(JSON.stringify(x.ruleRegistry, null, 2))}</pre>` : '';
                      const searchPolicy = x.softwareSearchPolicy ? `<div style="margin-top:8px;"><b>Software search policy:</b></div><pre class="diagram">${esc(JSON.stringify(x.softwareSearchPolicy, null, 2))}</pre>` : '';
                      const meta = `<div class="epoch-card"><h3>Hookup Guide <span class="hot">models</span></h3>${hooks}<div style="margin-top:8px;"><b>Processable data tested:</b></div>${processable}<div style="margin-top:8px;"><b>ASCII flow:</b></div><pre class="diagram">${esc(x.asciiFlow || '')}</pre><div style="margin-top:8px;"><b>Math and logic:</b></div><pre class="diagram">${esc(x.mathModel || '')}</pre>${layeredDb}${chooserCompare}${searchPolicy}${registry}</div>`;
                      algebraicPanel.innerHTML = `${summary}${chooserABSummary}${physicsSummary}${evolutionSummary}${topologyGateSummary}${meta}${cards}`;
                    }
                    async function captureBenchmark(){
                      const body = {reason:'manual_sdk_capture', timestamp:new Date().toISOString()};
                      const x = await api('/api/benchmark-snapshot', {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify(body)});
                      print(x);
                      await loadBenchmarks(false);
                    }
                    async function loadBenchmarks(show){
                      const x = await api('/api/benchmarks?limit=40');
                      renderBenchChart(x.history || []);
                      if (show) print(x);
                    }
                    function serviceMs(s, name){
                      return s && s[name] && typeof s[name].durationMs === 'number' ? s[name].durationMs : 0;
                    }
                    function parseHistory(lines){
                      return lines.map(line => { try { return JSON.parse(line); } catch { return null; } }).filter(Boolean);
                    }
                    function renderBenchChart(lines){
                      if (!benchChart) return;
                      const ctx = benchChart.getContext('2d');
                      const w = benchChart.width, h = benchChart.height;
                      ctx.clearRect(0,0,w,h);
                      ctx.fillStyle = '#010409'; ctx.fillRect(0,0,w,h);
                      ctx.strokeStyle = '#30363d'; ctx.lineWidth = 1;
                      for (let i=1;i<5;i++){ const y = i*h/5; ctx.beginPath(); ctx.moveTo(0,y); ctx.lineTo(w,y); ctx.stroke(); }
                      const data = parseHistory(lines).slice(-40);
                      ctx.fillStyle = '#8b949e'; ctx.font = '12px Consolas';
                      if (!data.length){ ctx.fillText('Capture a benchmark snapshot to start graphing.', 16, 28); return; }
                      const series = [
                        {name:'bridge', color:'#58a6ff', values:data.map(d => serviceMs(d.services, 'bridge8080'))},
                        {name:'house', color:'#3fb950', values:data.map(d => serviceMs(d.services, 'house11435'))},
                        {name:'shipper', color:'#d29922', values:data.map(d => serviceMs(d.services, 'shipper18081'))}
                      ];
                      const max = Math.max(50, ...series.flatMap(s => s.values));
                      series.forEach((s, si) => {
                        ctx.strokeStyle = s.color; ctx.lineWidth = 2; ctx.beginPath();
                        s.values.forEach((v, i) => {
                          const x = 28 + (data.length === 1 ? 0 : i * (w - 56) / (data.length - 1));
                          const y = h - 30 - ((v / max) * (h - 60));
                          if (i === 0) ctx.moveTo(x,y); else ctx.lineTo(x,y);
                        });
                        ctx.stroke();
                        ctx.fillStyle = s.color; ctx.fillText(`${s.name} ${s.values.at(-1)}ms`, 16 + si*150, 18);
                      });
                      ctx.fillStyle = '#8b949e'; ctx.fillText(`snapshots ${data.length} | max ${Math.round(max)}ms`, 16, h-10);
                    }
                    async function tail(file){ print(await api('/api/log-tail?file='+encodeURIComponent(file)+'&lines=80')); }
                    async function loadDesign(){ print(await api('/api/design')); }
                    loadState();
                  </script>
                </body>
                </html>
                """;
    }
}
