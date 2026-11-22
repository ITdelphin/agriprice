package com.agriprice.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public final class SmtpEmailUtil {

    private SmtpEmailUtil() {
    }

    public static boolean isConfigured() {
        return setting("AGRIPRICE_SMTP_USER") != null
                && setting("AGRIPRICE_SMTP_PASSWORD") != null
                && fromAddress() != null;
    }

    public static void sendPasswordResetEmail(String to, String resetUrl, int expiryMinutes) throws IOException {
        String host = setting("AGRIPRICE_SMTP_HOST") == null ? "smtp.gmail.com" : setting("AGRIPRICE_SMTP_HOST");
        int port = 465;
        String portValue = setting("AGRIPRICE_SMTP_PORT");
        if (portValue != null) {
            port = Integer.parseInt(portValue);
        }
        String user = setting("AGRIPRICE_SMTP_USER");
        String password = setting("AGRIPRICE_SMTP_PASSWORD");
        String from = fromAddress();

        if (user == null || password == null || from == null) {
            throw new IOException("SMTP is not configured.");
        }

        String subject = "Reset your AgriPrice Rwanda password";
        String body = "Hello,\n\n"
                + "A password reset was requested for your AgriPrice Rwanda agent account.\n\n"
                + "Use this verification link within " + expiryMinutes + " minutes:\n"
                + resetUrl + "\n\n"
                + "If you did not request this, ignore this email and your password will stay unchanged.\n\n"
                + "AgriPrice Rwanda";

        try (SSLSocket socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(host, port)) {
            socket.setSoTimeout(15000);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            Writer writer = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);

            require(readResponse(reader), 2, "SMTP greeting failed");
            send(writer, reader, "EHLO " + InetAddress.getLocalHost().getHostName(), 2, "EHLO failed");
            send(writer, reader, "AUTH LOGIN", 3, "AUTH LOGIN failed");
            send(writer, reader, encode(user), 3, "SMTP username rejected");
            send(writer, reader, encode(password), 2, "SMTP authentication failed");
            send(writer, reader, "MAIL FROM:<" + from + ">", 2, "MAIL FROM rejected");
            send(writer, reader, "RCPT TO:<" + to + ">", 2, "RCPT TO rejected");
            send(writer, reader, "DATA", 3, "DATA rejected");

            line(writer, "From: AgriPrice Rwanda <" + from + ">");
            line(writer, "To: " + to);
            line(writer, "Subject: " + subject);
            line(writer, "MIME-Version: 1.0");
            line(writer, "Content-Type: text/plain; charset=UTF-8");
            line(writer, "Content-Transfer-Encoding: 8bit");
            line(writer, "");
            String[] bodyLines = body.split("\\R", -1);
            for (String bodyLine : bodyLines) {
                line(writer, bodyLine.startsWith(".") ? "." + bodyLine : bodyLine);
            }
            line(writer, ".");
            writer.flush();

            require(readResponse(reader), 2, "Message rejected");
            send(writer, reader, "QUIT", -1, "QUIT failed");
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

    private static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static void send(Writer writer, BufferedReader reader, String command, int expectedCode, String error) throws IOException {
        line(writer, command);
        writer.flush();
        if (expectedCode >= 0) {
            require(readResponse(reader), expectedCode, error);
        } else {
            readResponse(reader);
        }
    }

    private static void line(Writer writer, String value) throws IOException {
        writer.write(value);
        writer.write("\r\n");
    }

    private static int readResponse(BufferedReader reader) throws IOException {
        String line = reader.readLine();
        if (line == null) {
            throw new IOException("SMTP connection closed.");
        }
        int code = code(line);
        while (line.length() > 3 && line.charAt(3) == '-') {
            line = reader.readLine();
            if (line == null) {
                throw new IOException("SMTP connection closed during response.");
            }
        }
        return code;
    }

    private static int code(String line) throws IOException {
        if (line.length() < 3) {
            throw new IOException("Malformed SMTP response: " + line);
        }
        try {
            return Integer.parseInt(line.substring(0, 3));
        } catch (NumberFormatException e) {
            throw new IOException("Malformed SMTP response: " + line, e);
        }
    }

    private static void require(int code, int expectedFamily, String error) throws IOException {
        if (code / 100 != expectedFamily) {
            throw new IOException(error + " SMTP code " + code);
        }
    }
}
