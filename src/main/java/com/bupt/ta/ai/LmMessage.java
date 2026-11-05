package com.bupt.ta.ai;

import java.util.Objects;

/**
 * Immutable chat message for multi-turn conversations sent to language models.
 *
 * <p>Each instance represents one turn with a {@link #role} (typically {@code system},
 * {@code user}, or {@code assistant}) and {@link #content} text. Messages are assembled
 * into {@link LmRequest} via {@link LmRequest.Builder#addMessage(LmMessage)} and serialised
 * by {@link HttpLmClient} into OpenAI-compatible Chat Completions JSON.
 *
 * <p>If {@link LmRequest#getMessages()} is empty, {@link HttpLmClient} falls back to
 * {@link LmRequest#getSystemPrompt()} and {@link LmRequest#getUserPrompt()} as synthetic
 * system and user messages.
 *
 * @see LmRequest
 * @see HttpLmClient
 */
public final class LmMessage {

    /**
     * Message role sent to the provider (for example {@code system}, {@code user}, {@code assistant}).
     * Defaults to {@code user} when {@code null} is passed to the constructor.
     */
    public final String role;

    /**
     * Message body text. Defaults to an empty string when {@code null} is passed to the constructor.
     */
    public final String content;

    /**
     * Constructs a chat message with the given role and content.
     *
     * @param role    provider role string; {@code null} is treated as {@code "user"}
     * @param content message text; {@code null} is treated as an empty string
     */
    public LmMessage(String role, String content) {
        this.role = role != null ? role : "user";
        this.content = content != null ? content : "";
    }

    /**
     * Compares this message to another object for value equality on role and content.
     *
     * @param o the object to compare
     * @return {@code true} if {@code o} is an {@link LmMessage} with the same role and content
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LmMessage)) return false;
        LmMessage lmMessage = (LmMessage) o;
        return Objects.equals(role, lmMessage.role) && Objects.equals(content, lmMessage.content);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return hash code derived from role and content
     */
    @Override
    public int hashCode() {
        return Objects.hash(role, content);
    }
}
