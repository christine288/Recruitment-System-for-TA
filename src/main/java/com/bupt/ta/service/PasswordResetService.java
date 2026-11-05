package com.bupt.ta.service;

import com.bupt.ta.model.User;
import com.bupt.ta.security.PasswordHasher;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Creates, validates, and consumes password reset tokens stored in {@code password-reset-tokens.json}.
 *
 * <p>Tokens are single-use, time-limited (24 hours), and emailed via {@link NotificationService}
 * when SMTP is configured. {@link com.bupt.ta.servlet.ForgotPasswordServlet} and
 * {@link com.bupt.ta.servlet.ResetPasswordServlet} orchestrate the user-facing flow.
 *
 * <p>Successful reset updates the user's password hash through {@link AuthService}.
 */
public class PasswordResetService {

    private static final Logger LOG = Logger.getLogger(PasswordResetService.class.getName());
    private static final int TOKEN_HOURS = 24;
    private static final String TOKENS_JSON = "password-reset-tokens.json";

    private final FileStore store;
    private final AuthService users;
    private final NotificationService notifications;

    /**
     * @param dataDir       directory for {@code password-reset-tokens.json}
     * @param users         user lookup and password updates
     * @param notifications email delivery (optional)
     */
    public PasswordResetService(Path dataDir, AuthService users, NotificationService notifications) {
        this.store = new FileStore(dataDir);
        this.users = users;
        this.notifications = notifications;
    }

    /**
     * Creates a single-use token for the account matching {@code email}.
     *
     * @return token when the email exists; empty when unknown
     */
    public Optional<String> createTokenForEmail(String email) throws IOException {
        return users.findByEmail(email).map(user -> {
            try {
                deleteByUserId(user.id);
                String token = UUID.randomUUID().toString();
                String now = Instant.now().toString();
                String expires = Instant.now().plus(TOKEN_HOURS, ChronoUnit.HOURS).toString();
                insert(token, user.id, expires, now);
                if (notifications.isConfigured()) {
                    String resetPath = "/reset-password?token=" + token;
                    notifications.sendPlainText(
                            user.email,
                            "TA Recruitment password reset",
                            "Use this link to reset your password:\n\n" + resetPath
                                    + "\n\nThis link expires in " + TOKEN_HOURS + " hours.");
                } else {
                    LOG.info("Password reset token for " + user.email + ": " + token);
                }
                return token;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /** Returns {@code true} when the token exists and has not expired. */
    public boolean isValidToken(String token) throws IOException {
        return findValidRow(token).isPresent();
    }

    /** Updates the user password and deletes the token when valid. */
    public boolean resetPassword(String token, String newPassword) throws IOException {
        Optional<TokenRow> row = findValidRow(token);
        if (!row.isPresent()) {
            return false;
        }
        Optional<User> userOpt = users.findById(row.get().userId);
        if (!userOpt.isPresent()) {
            return false;
        }
        users.updatePasswordHash(userOpt.get().id, PasswordHasher.hash(newPassword));
        deleteByToken(token);
        return true;
    }

    /** Deletes a token without changing the password (invalid link handling). */
    public void consumeToken(String token) throws IOException {
        deleteByToken(token);
    }

    private Optional<TokenRow> findValidRow(String token) throws IOException {
        Optional<TokenRow> row = findByToken(token);
        if (!row.isPresent()) {
            return Optional.empty();
        }
        try {
            Instant expires = Instant.parse(row.get().expiresAt);
            if (Instant.now().isAfter(expires)) {
                deleteByToken(token);
                return Optional.empty();
            }
        } catch (Exception e) {
            deleteByToken(token);
            return Optional.empty();
        }
        return row;
    }

    private void insert(String token, String userId, String expiresAt, String createdAt) throws IOException {
        List<Map<String, String>> rows = store.readMaps(TOKENS_JSON);
        Map<String, String> row = new LinkedHashMap<>();
        row.put("token", token);
        row.put("userId", userId);
        row.put("expiresAt", expiresAt);
        row.put("createdAt", createdAt);
        rows.add(row);
        store.writeMaps(TOKENS_JSON, rows);
    }

    private Optional<TokenRow> findByToken(String token) throws IOException {
        if (token == null || token.isEmpty()) {
            return Optional.empty();
        }
        return store.readMaps(TOKENS_JSON).stream()
                .filter(m -> token.equals(m.get("token")))
                .map(m -> new TokenRow(m.get("token"), m.get("userId"), m.get("expiresAt"), m.get("createdAt")))
                .findFirst();
    }

    private void deleteByToken(String token) throws IOException {
        if (token == null || token.isEmpty()) {
            return;
        }
        List<Map<String, String>> rows = store.readMaps(TOKENS_JSON);
        rows.removeIf(m -> token.equals(m.get("token")));
        store.writeMaps(TOKENS_JSON, rows);
    }

    private void deleteByUserId(String userId) throws IOException {
        if (userId == null || userId.isEmpty()) {
            return;
        }
        List<Map<String, String>> rows = store.readMaps(TOKENS_JSON);
        rows.removeIf(m -> userId.equals(m.get("userId")));
        store.writeMaps(TOKENS_JSON, rows);
    }

    static final class TokenRow {
        final String token;
        final String userId;
        final String expiresAt;
        final String createdAt;

        TokenRow(String token, String userId, String expiresAt, String createdAt) {
            this.token = token;
            this.userId = userId;
            this.expiresAt = expiresAt;
            this.createdAt = createdAt;
        }
    }
}
