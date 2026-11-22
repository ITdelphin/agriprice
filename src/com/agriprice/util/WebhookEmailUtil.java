package com.agriprice.util;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStream;
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

        byte[] body = payload.toString().getBytes(StandardCharsets.UTF_8);
        HttpURLConnection connection = (HttpURLConnection) URI.create(setting("AGRIPRICE_EMAIL_WEBHOOK_URL"))
                .toURL().openConnection();
        connection.setInstanceFollowRedirects(true);
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setDoOutput(true);
        connection.setRequestProperty("accept", "text/plain");
        connection.setRequestProperty("content-type", "application/json; charset=UTF-8");

        try (OutputStream out = connection.getOutputStream()) {
            out.write(body);
        }

        int status = connection.getResponseCode();
        String response = readBody(connection);
        connection.disconnect();

        if (status < 200 || status >= 300 || !"ok".equals(response.trim())) {
            throw new IOException("Email webhook failed. HTTP " + status);
        }
    }

    private static String readBody(HttpURLConnection connection) throws IOException {
        InputStream stream = connection.getErrorStream();
        if (stream == null) {
            stream = connection.getInputStream();
        }
        if (stream == null) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        try (InputStream in = stream) {
            byte[] buffer = new byte[1024];
            int count;
            while ((count = in.read(buffer)) != -1) {
                text.append(new String(buffer, 0, count, StandardCharsets.UTF_8));
            }
        }
        return text.toString();
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
