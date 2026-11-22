package com.agriprice.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public final class BrevoEmailUtil {

    private BrevoEmailUtil() {
    }

    public static boolean isConfigured() {
        return setting("AGRIPRICE_BREVO_API_KEY") != null && fromAddress() != null;
    }

    public static void sendPasswordResetEmail(String to, String resetUrl, int expiryMinutes) throws IOException {
        String apiKey = setting("AGRIPRICE_BREVO_API_KEY");
        String from = fromAddress();
        if (apiKey == null || from == null) {
            throw new IOException("Brevo email is not configured.");
        }

        String subject = "Reset your AgriPrice Rwanda password";
        String text = "Hello,\n\n"
                + "A password reset was requested for your AgriPrice Rwanda agent account.\n\n"
                + "Use this verification link within " + expiryMinutes + " minutes:\n"
                + resetUrl + "\n\n"
                + "If you did not request this, ignore this email and your password will stay unchanged.\n\n"
                + "AgriPrice Rwanda";
        String html = "<p>Hello,</p>"
                + "<p>A password reset was requested for your AgriPrice Rwanda agent account.</p>"
                + "<p>Use this verification link within " + expiryMinutes + " minutes:</p>"
                + "<p><a href=\"" + escapeHtml(resetUrl) + "\">Reset my password</a></p>"
                + "<p>If you did not request this, ignore this email and your password will stay unchanged.</p>"
                + "<p>AgriPrice Rwanda</p>";

        JsonObject sender = new JsonObject();
        sender.addProperty("name", "AgriPrice Rwanda");
        sender.addProperty("email", from);

        JsonObject recipient = new JsonObject();
        recipient.addProperty("email", to);
        JsonArray recipients = new JsonArray();
        recipients.add(recipient);

        JsonObject payload = new JsonObject();
        payload.add("sender", sender);
        payload.add("to", recipients);
        payload.addProperty("subject", subject);
        payload.addProperty("textContent", text);
        payload.addProperty("htmlContent", html);

        HttpURLConnection connection = (HttpURLConnection) URI.create("https://api.brevo.com/v3/smtp/email").toURL()
.openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setDoOutput(true);
        connection.setRequestProperty("accept", "application/json");
        connection.setRequestProperty("api-key", apiKey);
        connection.setRequestProperty("content-type", "application/json");

        try (OutputStream out = connection.getOutputStream()) {
            out.write(payload.toString().getBytes(StandardCharsets.UTF_8));
        }

        int status = connection.getResponseCode();
        if (status < 200 || status >= 300) {
            throw new IOException("Brevo API rejected the email. HTTP " + status);
        }
    }

    private static String fromAddress() {
        String from = setting("AGRIPRICE_SMTP_FROM");
        return from == null ? setting("AGRIPRICE_SMTP_USER") : from;
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
