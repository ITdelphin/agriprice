package com.agriprice.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class GmailApiEmailUtil {

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String SEND_URL = "https://gmail.googleapis.com/gmail/v1/users/me/messages/send";

    private GmailApiEmailUtil() {
    }

    public static boolean isConfigured() {
        return setting("AGRIPRICE_GMAIL_CLIENT_ID") != null
                && setting("AGRIPRICE_GMAIL_CLIENT_SECRET") != null
                && setting("AGRIPRICE_GMAIL_REFRESH_TOKEN") != null
                && fromAddress() != null;
    }

    public static void sendPasswordResetEmail(String to, String resetUrl, int expiryMinutes) throws IOException {
        if (!isConfigured()) {
            throw new IOException("Gmail API email is not configured.");
        }

        String accessToken = requestAccessToken();
        String html = "<p>Hello,</p>"
                + "<p>A password reset was requested for your AgriPrice Rwanda agent account.</p>"
                + "<p>Use this verification link within " + expiryMinutes + " minutes:</p>"
                + "<p><a href=\"" + escapeHtml(resetUrl) + "\">Reset my password</a></p>"
                + "<p>If you did not request this, ignore this email and your password will stay unchanged.</p>"
                + "<p>AgriPrice Rwanda</p>";
        String raw = buildRawMessage(to, html);

        JsonObject payload = new JsonObject();
        payload.addProperty("raw", raw);

        StringBuilder response = new StringBuilder();
        int status = post(SEND_URL, "application/json", "Bearer " + accessToken, payload.toString(), response);
        if (status < 200 || status >= 300) {
            throw new IOException("Gmail API rejected the email. HTTP " + status);
        }
    }

    private static String requestAccessToken() throws IOException {
        String form = "client_id=" + encode(setting("AGRIPRICE_GMAIL_CLIENT_ID"))
                + "&client_secret=" + encode(setting("AGRIPRICE_GMAIL_CLIENT_SECRET"))
                + "&refresh_token=" + encode(setting("AGRIPRICE_GMAIL_REFRESH_TOKEN"))
                + "&grant_type=refresh_token";

        StringBuilder response = new StringBuilder();
        int status = post(TOKEN_URL, "application/x-www-form-urlencoded", null, form, response);
        if (status < 200 || status >= 300) {
            throw new IOException("Gmail OAuth token request failed. HTTP " + status);
        }

        JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
        if (!json.has("access_token")) {
            throw new IOException("Gmail OAuth token response did not include an access token.");
        }
        return json.get("access_token").getAsString();
    }

    private static String buildRawMessage(String to, String html) {
        String from = headerSafe(fromAddress());
        String recipient = headerSafe(to);
        String subject = "Reset your AgriPrice Rwanda password";
        String message = "From: AgriPrice Rwanda <" + from + ">\r\n"
                + "To: " + recipient + "\r\n"
                + "Subject: " + subject + "\r\n"
                + "MIME-Version: 1.0\r\n"
                + "Content-Type: text/html; charset=UTF-8\r\n"
                + "\r\n"
                + html;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(message.getBytes(StandardCharsets.UTF_8));
    }

    private static int post(String url, String contentType, String authorization, String body, StringBuilder response)
            throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setDoOutput(true);
        connection.setRequestProperty("accept", "application/json");
        connection.setRequestProperty("content-type", contentType);
        if (authorization != null) {
            connection.setRequestProperty("Authorization", authorization);
        }

        try (OutputStream out = connection.getOutputStream()) {
            out.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int status = connection.getResponseCode();
        InputStream stream = status < 400 ? connection.getInputStream() : connection.getErrorStream();
        if (stream != null) {
            try (InputStream in = stream) {
                byte[] buffer = new byte[4096];
                int count;
                while ((count = in.read(buffer)) != -1) {
                    response.append(new String(buffer, 0, count, StandardCharsets.UTF_8));
                }
            }
        }
        connection.disconnect();
        return status;
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

    private static String encode(String value) throws IOException {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
    }

    private static String headerSafe(String value) {
        return value.replace("\r", "").replace("\n", "");
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
