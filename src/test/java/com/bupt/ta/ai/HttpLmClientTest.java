package com.bupt.ta.ai;

import com.bupt.ta.testsupport.LmTestSupport;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies Doubao configuration failures and its OpenAI-compatible HTTP wire format. */
class HttpLmClientTest {
    private static final String MODEL_NOT_OPEN_RESPONSE = "{\"error\":{\"code\":\"ModelNotOpen\","
            + "\"message\":\"Your account has not activated the model. Request id: test-request-123456\","
            + "\"type\":\"Not Found\"}}";

    @Test
    void generate_withoutApiKey_failsBeforeNetwork() {
        LmConfig cfg = LmConfig.load(LmTestSupport.servletContextWithLmProperties(
                "LM_PROVIDER=doubao\nLM_API_KEY=\nLM_MODEL=ep-test-endpoint\n"));

        LmException ex = assertThrows(LmException.class,
                () -> new HttpLmClient(cfg).generate(request()));

        assertTrue(ex.getMessage().contains("LM_API_KEY") || ex.getMessage().contains("ARK_API_KEY"));
    }

    @Test
    void generate_withoutModel_failsBeforeNetwork() {
        LmConfig cfg = LmConfig.load(LmTestSupport.servletContextWithLmProperties(
                "LM_PROVIDER=doubao\nLM_API_KEY=ark-test-key\nLM_MODEL=\n"));

        LmException ex = assertThrows(LmException.class,
                () -> new HttpLmClient(cfg).generate(request()));

        assertTrue(ex.getMessage().contains("LM_MODEL") || ex.getMessage().contains("ARK_MODEL"));
    }

    @Test
    void generate_postsDoubaoChatCompletionAndParsesReply() throws Exception {
        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<String> path = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = startServer(exchange -> {
            method.set(exchange.getRequestMethod());
            path.set(exchange.getRequestURI().getPath());
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, "application/json",
                    "{\"model\":\"ep-test-endpoint\",\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"\\u8c46\\u5305\\u56de\\u590d\"}}]}");
        });
        try {
            LmResponse reply = new HttpLmClient(localConfig(server.getAddress().getPort())).generate(request());

            assertTrue(reply.isSuccess());
            assertEquals("豆包回复", reply.getText());
            assertFalse(reply.getProvider().equals("mock"));
            assertEquals("POST", method.get());
            assertEquals("/api/v3/chat/completions", path.get());
            assertEquals("Bearer ark-test-key", authorization.get());

            JsonObject body = JsonParser.parseString(requestBody.get()).getAsJsonObject();
            assertEquals("ep-test-endpoint", body.get("model").getAsString());
            assertEquals("system", body.getAsJsonArray("messages").get(0).getAsJsonObject().get("role").getAsString());
            assertEquals("sys", body.getAsJsonArray("messages").get(0).getAsJsonObject().get("content").getAsString());
            assertEquals("user", body.getAsJsonArray("messages").get(1).getAsJsonObject().get("role").getAsString());
            assertEquals("user", body.getAsJsonArray("messages").get(1).getAsJsonObject().get("content").getAsString());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void stream_postsStreamingRequestAndForwardsSseDeltas() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = startServer(exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, "text/event-stream",
                    "data: {\"model\":\"ep-test-endpoint\",\"choices\":[{\"delta\":{\"content\":\"你\"}}]}\n\n"
                            + "data: {\"model\":\"ep-test-endpoint\",\"choices\":[{\"delta\":{\"content\":\"好\"}}]}\n\n"
                            + "data: [DONE]\n\n");
        });
        try {
            StringBuilder deltas = new StringBuilder();
            AtomicReference<String> completedModel = new AtomicReference<>();
            AtomicReference<String> error = new AtomicReference<>();

            new HttpLmClient(localConfig(server.getAddress().getPort())).stream(request(), new LmStreamListener() {
                @Override public void onDelta(String text) { deltas.append(text); }
                @Override public void onComplete(String model) { completedModel.set(model); }
                @Override public void onError(String message) { error.set(message); }
            });

            assertNull(error.get());
            assertEquals("你好", deltas.toString());
            assertEquals("ep-test-endpoint", completedModel.get());
            assertTrue(JsonParser.parseString(requestBody.get()).getAsJsonObject().get("stream").getAsBoolean());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void generate_modelNotOpenExplainsHowToFixConfiguration() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        HttpServer server = startServer(exchange -> {
            attempts.incrementAndGet();
            respond(exchange, 404, "application/json", MODEL_NOT_OPEN_RESPONSE);
        });
        try {
            LmException error = assertThrows(LmException.class,
                    () -> new HttpLmClient(localConfig(server.getAddress().getPort())).generate(request()));

            assertTrue(error.getMessage().contains("ep-test-endpoint"));
            assertTrue(error.getMessage().contains("Volcengine Ark Console"));
            assertTrue(error.getMessage().contains("ARK_MODEL"));
            assertTrue(error.getMessage().contains("Ark request ID: test-request-123456"));
            assertFalse(error.getMessage().contains("{\"error\""));
            assertEquals(2, attempts.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void stream_modelNotOpenExplainsHowToFixConfiguration() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        HttpServer server = startServer(exchange -> {
            attempts.incrementAndGet();
            respond(exchange, 404, "application/json", MODEL_NOT_OPEN_RESPONSE);
        });
        try {
            LmException error = assertThrows(LmException.class,
                    () -> new HttpLmClient(localConfig(server.getAddress().getPort()))
                            .stream(request(), new LmStreamListener() {
                                @Override public void onDelta(String text) {}
                                @Override public void onComplete(String model) {}
                                @Override public void onError(String message) {}
                            }));

            assertTrue(error.getMessage().contains("ep-test-endpoint"));
            assertTrue(error.getMessage().contains("Volcengine Ark Console"));
            assertTrue(error.getMessage().contains("ARK_MODEL"));
            assertTrue(error.getMessage().contains("Ark request ID: test-request-123456"));
            assertFalse(error.getMessage().contains("{\"error\""));
            assertEquals(2, attempts.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void generate_transientModelNotOpenRetriesAndSucceeds() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        HttpServer server = startServer(exchange -> {
            if (attempts.incrementAndGet() == 1) {
                respond(exchange, 404, "application/json", MODEL_NOT_OPEN_RESPONSE);
            } else {
                respond(exchange, "application/json",
                        "{\"model\":\"ep-test-endpoint\",\"choices\":[{\"message\":{\"content\":\"OK\"}}]}");
            }
        });
        try {
            LmResponse response = new HttpLmClient(localConfig(server.getAddress().getPort())).generate(request());

            assertTrue(response.isSuccess());
            assertEquals("OK", response.getText());
            assertEquals(2, attempts.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void stream_transientModelNotOpenRetriesAndCompletes() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        HttpServer server = startServer(exchange -> {
            if (attempts.incrementAndGet() == 1) {
                respond(exchange, 404, "application/json", MODEL_NOT_OPEN_RESPONSE);
            } else {
                respond(exchange, "text/event-stream",
                        "data: {\"choices\":[{\"delta\":{\"content\":\"OK\"}}]}\n\n"
                                + "data: [DONE]\n\n");
            }
        });
        try {
            StringBuilder text = new StringBuilder();
            AtomicReference<String> error = new AtomicReference<>();
            AtomicReference<String> completedModel = new AtomicReference<>();
            new HttpLmClient(localConfig(server.getAddress().getPort())).stream(request(), new LmStreamListener() {
                @Override public void onDelta(String delta) { text.append(delta); }
                @Override public void onComplete(String model) { completedModel.set(model); }
                @Override public void onError(String message) { error.set(message); }
            });

            assertEquals("OK", text.toString());
            assertNull(error.get());
            assertEquals("ep-test-endpoint", completedModel.get());
            assertEquals(2, attempts.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void generate_otherHttpErrorsKeepTheirOriginalMessage() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        HttpServer server = startServer(exchange -> {
            attempts.incrementAndGet();
            respond(exchange, 404, "application/json",
                    "{\"error\":{\"code\":\"OtherError\",\"message\":\"Other failure\"}}");
        });
        try {
            LmException error = assertThrows(LmException.class,
                    () -> new HttpLmClient(localConfig(server.getAddress().getPort())).generate(request()));

            assertTrue(error.getMessage().contains("LM HTTP request failed: HTTP 404:"));
            assertTrue(error.getMessage().contains("OtherError"));
            assertEquals(1, attempts.get());
        } finally {
            server.stop(0);
        }
    }

    private static LmRequest request() {
        return LmRequest.builder()
                .featureName(AiFeatureNames.SKILL_MATCH)
                .systemPrompt("sys")
                .userPrompt("user")
                .build();
    }

    private static LmConfig localConfig(int port) {
        String props = "LM_PROVIDER=doubao\n"
                + "LM_API_KEY=ark-test-key\n"
                + "LM_MODEL=ep-test-endpoint\n"
                + "LM_BASE_URL=http://127.0.0.1:" + port + "/api/v3\n";
        return LmConfig.load(LmTestSupport.servletContextWithLmProperties(props));
    }

    private static HttpServer startServer(HttpHandler handler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v3/chat/completions", handler);
        server.start();
        return server;
    }

    private static void respond(HttpExchange exchange, String contentType, String body) throws IOException {
        respond(exchange, 200, contentType, body);
    }

    private static void respond(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
