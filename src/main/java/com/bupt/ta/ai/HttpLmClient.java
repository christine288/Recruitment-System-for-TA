package com.bupt.ta.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.bupt.ta.util.HttpJsonClient;
import com.bupt.ta.util.Strings;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
<<<<<<< Updated upstream
 * Generic HTTPS JSON provider skeleton (OpenAI-compatible Chat Completions shape).
 * Vendor-specific differences should stay inside this class (headers, URL, body, response parsing).
=======
 * HTTPS JSON implementation of {@link LmClient} for OpenAI-compatible Chat Completions APIs.
 *
 * <p>{@link HttpLmClient} is the production-oriented backend of the LM integration layer.
 * It is constructed by {@link LmClientFactory} for Doubao and other HTTP providers.
 * All vendor-specific concerns—URL
 * assembly, {@code Authorization} header, JSON body shape, response parsing, and SSE
 * streaming—are encapsulated here so servlets and AI services only see {@link LmRequest}
 * and {@link LmResponse}.
 *
 * <p><strong>Configuration used:</strong> {@link LmConfig#getBaseUrl()},
 * {@link LmConfig#getHttpChatPath()}, {@link LmConfig#getApiKey()},
 * {@link LmConfig#getTimeoutMs()}, and {@link LmConfig#getModel()} (with per-request
 * override via {@link LmRequest#getModel()}).
 *
 * <p>If credentials or the model are missing, both
 * {@link #generate(LmRequest)} and {@link #stream(LmRequest, LmStreamListener)} throw
 * {@link LmException}. Provider error JSON is converted to unsuccessful {@link LmResponse}
 * or {@link LmStreamListener#onError(String)} rather than returning mock content.
 *
 * <p>Vendor-specific differences (Azure api-version, Gemini/Claude adapters, custom headers)
 * should stay inside this class; see inline TODO markers.
 *
 * @see LmClientFactory
 * @see LmConfig
>>>>>>> Stashed changes
 */
public final class HttpLmClient implements LmClient {
    private static final Logger LOG = Logger.getLogger(HttpLmClient.class.getName());
    private final LmConfig config;

    public HttpLmClient(LmConfig config) {
        this.config = config;
    }

    @Override
    public LmResponse generate(LmRequest request) throws LmException {
        requireCredentials();
        String url = joinBaseAndPath(config.getBaseUrl(), config.getHttpChatPath());
        String model = resolveModel(request);
        String body;
        try {
            body = buildOpenAiCompatibleChatBody(request, model);
        } catch (Exception e) {
            throw new LmException("Failed to build request JSON: " + e.getMessage(), e);
        }

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + config.getApiKey());
        try {
            String raw = HttpJsonClient.postJson(url, headers, body, config.getTimeoutMs());
            return parseCompletion(raw, model);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "LM HTTP request failed: " + url, e);
            throw new LmException("LM HTTP request failed: " + e.getMessage(), e);
        }
    }

<<<<<<< Updated upstream
=======
    /**
     * Streams a Chat Completions response via Server-Sent Events (SSE).
     *
     * <p>Sends {@code "stream": true} in the request body, reads {@code data:} lines from the
     * response, forwards {@code delta.content} fragments to {@link LmStreamListener#onDelta(String)},
     * and completes on {@code [DONE]}. A premature end-of-stream or provider error invokes
     * {@link LmStreamListener#onError(String)} without throwing.
     *
     * @param request  same shape as {@link #generate(LmRequest)}
     * @param listener streaming callbacks; must not be {@code null}
     * @throws LmException if credentials are missing, the request body cannot be built, or
     *                     streaming I/O fails before a terminal listener callback
     */
>>>>>>> Stashed changes
    @Override
    public void stream(LmRequest request, LmStreamListener listener) throws LmException {
        requireCredentials();
        String url = joinBaseAndPath(config.getBaseUrl(), config.getHttpChatPath());
        String model = resolveModel(request);
        String body;
        try {
            body = buildOpenAiCompatibleChatBodyStream(request, model);
        } catch (Exception e) {
            throw new LmException("Failed to build streaming request JSON: " + e.getMessage(), e);
        }
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + config.getApiKey());
        try (InputStream rawIn = HttpJsonClient.postJsonStream(url, headers, body, config.getTimeoutMs());
             BufferedReader br = new BufferedReader(new InputStreamReader(rawIn, StandardCharsets.UTF_8))) {
            String line;
            String lastModel = model;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (!trimmed.startsWith("data:")) {
                    continue;
                }
                String payload = trimmed.substring(trimmed.indexOf(':') + 1).trim();
                if ("[DONE]".equals(payload)) {
                    listener.onComplete(lastModel);
                    return;
                }
                try {
                    JsonObject event = JsonParser.parseString(payload).getAsJsonObject();
                    String apiError = errorMessage(event);
                    if (apiError != null) {
                        listener.onError(apiError);
                        return;
                    }
                    String eventModel = stringValue(event, "model");
                    if (eventModel != null && !eventModel.isEmpty()) {
                        lastModel = eventModel;
                    }
                    JsonObject delta = firstChoiceObject(event, "delta");
                    String content = stringValue(delta, "content");
                    if (content != null && !content.isEmpty()) {
                        listener.onDelta(content);
                    }
                } catch (RuntimeException parseError) {
                    listener.onError("Invalid JSON from AI provider.");
                    return;
                }
            }
            listener.onError("AI provider stream ended before completion.");
        } catch (Exception e) {
            LOG.log(Level.WARNING, "LM HTTP stream failed: " + url, e);
            throw new LmException(hasTimeoutCause(e)
                    ? "AI provider stream timed out. Please try again."
                    : "LM HTTP stream failed: " + e.getMessage(), e);
        }
    }

    private static boolean hasTimeoutCause(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof HttpTimeoutException) {
                return true;
            }
        }
        return false;
    }

    private void requireCredentials() throws LmException {
        if (config.getApiKey().isEmpty()) {
            throw new LmException("AI API key is missing. Set ARK_API_KEY or LM_API_KEY.");
        }
        if (config.getBaseUrl().isEmpty()) {
            throw new LmException("AI API base URL is missing. Set LM_BASE_URL.");
        }
    }

    private String resolveModel(LmRequest request) throws LmException {
        String model = Strings.firstNonBlank(request.getModel(), config.getModel());
        if (model.isEmpty()) {
            throw new LmException("AI model is missing. Set ARK_MODEL or LM_MODEL to an enabled model or endpoint ID.");
        }
        return model;
    }

    private String providerName() {
        return config.getProviderType().name().toLowerCase(Locale.ROOT);
    }

    private static String joinBaseAndPath(String baseUrl, String path) {
        String b = baseUrl == null ? "" : baseUrl.trim();
        String p = path == null ? "" : path.trim();
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        while (b.endsWith("/")) {
            b = b.substring(0, b.length() - 1);
        }
        return b + p;
    }

    /**
     * OpenAI Chat Completions compatible payload.
     * TODO: split into strategy classes if you support multiple vendors side-by-side.
     */
    private static String buildOpenAiCompatibleChatBody(LmRequest request, String model) {
        StringBuilder messagesJson = new StringBuilder();
        if (request.getMessages() != null && !request.getMessages().isEmpty()) {
            boolean first = true;
            for (LmMessage m : request.getMessages()) {
                if (!first) {
                    messagesJson.append(',');
                }
                first = false;
                messagesJson.append("{\"role\":\"").append(jsonEscape(m.role))
                        .append("\",\"content\":\"").append(jsonEscape(m.content)).append("\"}");
            }
        } else {
            String sys = request.getSystemPrompt() != null ? request.getSystemPrompt() : "";
            String usr = request.getUserPrompt() != null ? request.getUserPrompt() : "";
            messagesJson.append("{\"role\":\"system\",\"content\":\"").append(jsonEscape(sys)).append("\"},");
            messagesJson.append("{\"role\":\"user\",\"content\":\"").append(jsonEscape(usr)).append("\"}");
        }

        StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append("\"model\":\"").append(jsonEscape(model)).append("\",");
        sb.append("\"temperature\":").append(request.getTemperature()).append(',');
        sb.append("\"max_tokens\":").append(request.getMaxTokens()).append(',');
        sb.append("\"messages\":[").append(messagesJson).append(']');
        sb.append('}');
        return sb.toString();
    }

    private static String buildOpenAiCompatibleChatBodyStream(LmRequest request, String model) {
        StringBuilder messagesJson = new StringBuilder();
        if (request.getMessages() != null && !request.getMessages().isEmpty()) {
            boolean first = true;
            for (LmMessage m : request.getMessages()) {
                if (!first) {
                    messagesJson.append(',');
                }
                first = false;
                messagesJson.append("{\"role\":\"").append(jsonEscape(m.role))
                        .append("\",\"content\":\"").append(jsonEscape(m.content)).append("\"}");
            }
        } else {
            String sys = request.getSystemPrompt() != null ? request.getSystemPrompt() : "";
            String usr = request.getUserPrompt() != null ? request.getUserPrompt() : "";
            messagesJson.append("{\"role\":\"system\",\"content\":\"").append(jsonEscape(sys)).append("\"},");
            messagesJson.append("{\"role\":\"user\",\"content\":\"").append(jsonEscape(usr)).append("\"}");
        }

        StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append("\"model\":\"").append(jsonEscape(model)).append("\",");
        sb.append("\"temperature\":").append(request.getTemperature()).append(',');
        sb.append("\"max_tokens\":").append(request.getMaxTokens()).append(',');
        sb.append("\"stream\":true,");
        sb.append("\"messages\":[").append(messagesJson).append(']');
        sb.append('}');
        return sb.toString();
    }

    private static String jsonEscape(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\':
                    sb.append("\\\\");
                    break;
                case '"':
                    sb.append("\\\"");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    private LmResponse parseCompletion(String raw, String requestedModel) {
        try {
            JsonObject response = JsonParser.parseString(raw).getAsJsonObject();
            String error = errorMessage(response);
            String model = Strings.firstNonBlank(stringValue(response, "model"), requestedModel);
            if (error != null) {
                return new LmResponse("", providerName(), model, false, raw, error);
            }
            JsonObject message = firstChoiceObject(response, "message");
            String content = stringValue(message, "content");
            if (content == null || content.isEmpty()) {
                return new LmResponse("", providerName(), model, false, raw,
                        "AI provider returned no assistant text.");
            }
            return new LmResponse(content, providerName(), model, true, raw, null);
        } catch (RuntimeException e) {
            return new LmResponse("", providerName(), requestedModel, false, raw,
                    "Invalid JSON from AI provider.");
        }
    }

    private static JsonObject firstChoiceObject(JsonObject response, String field) {
        if (response == null) {
            return null;
        }
        JsonElement choicesElement = response.get("choices");
        if (choicesElement == null || !choicesElement.isJsonArray()) {
            return null;
        }
        JsonArray choices = choicesElement.getAsJsonArray();
        if (choices.isEmpty() || !choices.get(0).isJsonObject()) {
            return null;
        }
        JsonElement value = choices.get(0).getAsJsonObject().get(field);
        return value != null && value.isJsonObject() ? value.getAsJsonObject() : null;
    }

    private static String errorMessage(JsonObject response) {
        if (response == null || !response.has("error")) {
            return null;
        }
        JsonElement error = response.get("error");
        if (error != null && error.isJsonObject()) {
            return Strings.firstNonBlank(stringValue(error.getAsJsonObject(), "message"),
                    "AI provider returned an error.");
        }
        return error != null && error.isJsonPrimitive() ? error.getAsString()
                : "AI provider returned an error.";
    }

    private static String stringValue(JsonObject object, String field) {
        if (object == null) {
            return null;
        }
        JsonElement value = object.get(field);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : null;
    }

}
