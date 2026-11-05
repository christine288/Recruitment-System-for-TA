package com.bupt.ta.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Unified request for both chat-style and prompt-style LMs.
 * If {@code messages} is empty, callers may rely on {@code systemPrompt} + {@code userPrompt} only.
 */
public final class LmRequest {
    private final String systemPrompt;
    private final String userPrompt;
    private final List<LmMessage> messages;
    private final double temperature;
    private final int maxTokens;
    private final String model;
    private final String featureName;

    private LmRequest(Builder b) {
        this.systemPrompt = b.systemPrompt;
        this.userPrompt = b.userPrompt;
        this.messages = Collections.unmodifiableList(new ArrayList<>(b.messages));
        this.temperature = b.temperature;
        this.maxTokens = b.maxTokens;
        this.model = b.model;
        this.featureName = b.featureName;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public String getUserPrompt() {
        return userPrompt;
    }

    public List<LmMessage> getMessages() {
        return messages;
    }

    public double getTemperature() {
        return temperature;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    /**
     * Returns the per-request model override, if any.
     *
     * <p>When blank, {@link HttpLmClient} uses {@link LmConfig#getModel()} and reports an
     * error if that is also blank. {@link MockLmClient} uses {@code "mock-model"} when blank.
     *
     * @return model identifier, or empty string to defer to config/fallback
     */
    public String getModel() {
        return model;
    }

    public String getFeatureName() {
        return featureName;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String systemPrompt = "";
        private String userPrompt = "";
        private final List<LmMessage> messages = new ArrayList<>();
        private double temperature = 0.7d;
        private int maxTokens = 1024;
        private String model = "";
        private String featureName = "";

        public Builder systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        public Builder userPrompt(String userPrompt) {
            this.userPrompt = userPrompt;
            return this;
        }

        public Builder addMessage(LmMessage m) {
            if (m != null) {
                messages.add(m);
            }
            return this;
        }

        public Builder temperature(double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder featureName(String featureName) {
            this.featureName = featureName;
            return this;
        }

        public LmRequest build() {
            return new LmRequest(this);
        }
    }
}
