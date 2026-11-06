package com.bupt.ta.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable request payload for language-model completions in the TA system.
 *
 * <p>{@link LmRequest} unifies chat-style multi-turn input ({@link LmMessage} list) and
 * simple prompt-style input ({@link #getSystemPrompt()} + {@link #getUserPrompt()}) behind
 * one type consumed by {@link LmClient}. Build instances with {@link #builder()}.
 *
 * <p>If {@link #getMessages()} is non-empty, {@link HttpLmClient} serialises the message list
 * directly. If empty, it synthesises system and user messages from the prompt fields.
 *
 * <p>The optional {@link #getFeatureName()} ({@link AiFeatureNames}) routes
 * {@link MockLmClient} to feature-specific deterministic handlers and supports logging
 * in AI services.
 *
 * @see LmClient#generate(LmRequest)
 * @see LmRequest.Builder
 * @see LmMessage
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

    /**
     * Returns the system instruction text for prompt-style requests.
     *
     * @return system prompt; empty string when not set
     */
    public String getSystemPrompt() {
        return systemPrompt;
    }

    /**
     * Returns the user instruction or question for prompt-style requests.
     *
     * @return user prompt; empty string when not set
     */
    public String getUserPrompt() {
        return userPrompt;
    }

    /**
     * Returns an unmodifiable list of chat messages for multi-turn requests.
     *
     * <p>When non-empty, takes precedence over {@link #getSystemPrompt()} and
     * {@link #getUserPrompt()} in {@link HttpLmClient}.
     *
     * @return message list; never {@code null}, may be empty
     */
    public List<LmMessage> getMessages() {
        return messages;
    }

    /**
     * Returns the sampling temperature passed to the provider.
     *
     * @return temperature in {@code [0.0, 2.0]} typical range; builder default {@code 0.7}
     */
    public double getTemperature() {
        return temperature;
    }

    /**
     * Returns the maximum number of tokens to generate.
     *
     * @return max tokens; builder default {@code 1024}
     */
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

    /**
     * Returns the AI feature key for mock routing and logging.
     *
     * @return feature name (see {@link AiFeatureNames}), or empty string when not set
     */
    public String getFeatureName() {
        return featureName;
    }

    /**
     * Creates a new {@link Builder} with default sampling and empty prompts.
     *
     * @return a mutable builder for constructing {@link LmRequest} instances
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link LmRequest}.
     *
     * <p>Defaults: temperature {@code 0.7}, max tokens {@code 1024}, empty strings for
     * prompts, model, and feature name.
     */
    public static final class Builder {
        private String systemPrompt = "";
        private String userPrompt = "";
        private final List<LmMessage> messages = new ArrayList<>();
        private double temperature = 0.7d;
        private int maxTokens = 1024;
        private String model = "";
        private String featureName = "";

        /**
         * Sets the system prompt for prompt-style requests.
         *
         * @param systemPrompt system instruction text; {@code null} is not expected by callers
         * @return this builder
         */
        public Builder systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        /**
         * Sets the user prompt for prompt-style requests.
         *
         * @param userPrompt user message or task description; {@code null} is not expected by callers
         * @return this builder
         */
        public Builder userPrompt(String userPrompt) {
            this.userPrompt = userPrompt;
            return this;
        }

        /**
         * Appends a chat message to the multi-turn message list.
         *
         * @param m message to add; {@code null} entries are ignored
         * @return this builder
         */
        public Builder addMessage(LmMessage m) {
            if (m != null) {
                messages.add(m);
            }
            return this;
        }

        /**
         * Sets the sampling temperature for the completion.
         *
         * @param temperature provider temperature parameter
         * @return this builder
         */
        public Builder temperature(double temperature) {
            this.temperature = temperature;
            return this;
        }

        /**
         * Sets the maximum tokens to generate.
         *
         * @param maxTokens upper bound on generated tokens
         * @return this builder
         */
        public Builder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        /**
         * Sets a per-request model override.
         *
         * @param model model identifier; empty string defers to {@link LmConfig} / fallback
         * @return this builder
         */
        public Builder model(String model) {
            this.model = model;
            return this;
        }

        /**
         * Sets the AI feature key ({@link AiFeatureNames}) for mock routing and logging.
         *
         * @param featureName stable feature identifier
         * @return this builder
         */
        public Builder featureName(String featureName) {
            this.featureName = featureName;
            return this;
        }

        /**
         * Builds an immutable {@link LmRequest} from the current builder state.
         *
         * @return a new {@link LmRequest} ready for {@link LmClient#generate(LmRequest)}
         */
        public LmRequest build() {
            return new LmRequest(this);
        }
    }
}
