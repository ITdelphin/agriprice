package com.agriprice.util;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public final class WebhookEmailUtil {

    private WebhookEmailUtil() {
    }

    public static boolean isConfigured() {
        String url = setting("AGRIPRICE_EMAIL_WEBHOOK_URL");
        return url != null && url.startsWith("https://") && setting("AGRIPRICE_EMAIL_WEBHOOK_SECRET") != null;
    }

    public static void sendPasswordResetEmail(String to, String resetUrl, int expiryMinutes) throws IOException {
        if (!isConfigured()) {
            throw new IOException("Webhook email is not configured.");
        }

        String subject = "Reset your AgriPrice Rwanda password";
        String text = "Use this verification link within " + expiryMinutes + " minutes:\n" + resetUrl;
        String html = "<p>Hello,</p>"
                + "<p>A password reset was requested for your AgriPrice Rwanda agent account.</p>"
                + "<p>Use this verification link within " + expiryMinutes + " minutes:</p>"
                + "<p><a href=\"" + escapeHtml(resetUrl) + "\">Reset my password</a></p>"
                + "<p>If you did not request this, ignore this email and your password will stay unchanged.</p>"
                + "<p>AgriPrice Rwanda</p>";

        JsonObject payload = new JsonObject();
        payload.addProperty("secret", setting("AGRIPRICE_EMAIL_WEBHOOK_SECRET"));
        payload.addProperty("to", to);
        payload.addProperty("subject", subject);
        payload.addProperty("text", text);
        payload.addProperty("html", html);

        HttpURLConnection connection = (HttpURLConnection) URI.create(setting("AGRIPRICE_EMAIL_WEBHOOK_URL"))
                .toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setDoOutput(true);
        connection.setRequestProperty("accept", "text/plain");
        connection.setRequestProperty("content-type", "application/json; charset=UTF-8");

        try (OutputStream out = connection.getOutputStream()) {
            out.write(payload.toString().getBytes(StandardCharsets.UTF_8));
        }

        int status = connection.getResponseCode();
        connection.disconnect();

        if (status < 200 || status >= 300) {
            throw new IOException("Email webhook rejected the message. HTTP " + status);
        }
    }

    private static String setting(String name) {
        String value = System.getProperty(name);
        if (value == null || value.trim().isEmpty()) {
            value = System.getenv(name);
        }
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
