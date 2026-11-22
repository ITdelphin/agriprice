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
        String url = setting("AGRIPRICE_EMAIL_WEBHOOK_URL");
        for (int attempt = 0; attempt < 6; attempt++) {
            WebhookResponse response = postOnce(url, body);
            if (isRedirect(response.status) && response.location != null && attempt < 5) {
                url = URI.create(url).resolve(response.location).toString();
                continue;
            }
            if (response.status >= 200 && response.status < 300 && "ok".equals(response.text.trim())) {
                return;
            }
            throw new IOException("Email webhook failed. HTTP " + response.status);
        }
        throw new IOException("Email webhook redirect loop.");
    }

    private static WebhookResponse postOnce(String url, byte[] body) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setDoOutput(true);
        connection.setFixedLengthStreamingMode(body.length);
        connection.setRequestProperty("accept", "text/plain");
        connection.setRequestProperty("content-type", "application/json; charset=UTF-8");

        try (OutputStream out = connection.getOutputStream()) {
            out.write(body);
        }

        int status = connection.getResponseCode();
        String text = readBody(connection);
        String location = connection.getHeaderField("Location");
        connection.disconnect();
        return new WebhookResponse(status, text, location);
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

    private static boolean isRedirect(int status) {
        return status == 301 || status == 302 || status == 303 || status == 307 || status == 308;
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

    private static final class WebhookResponse {
        private final int status;
        private final String text;
        private final String location;

        private WebhookResponse(int status, String text, String location) {
            this.status = status;
            this.text = text;
            this.location = location;
        }
    }
}
