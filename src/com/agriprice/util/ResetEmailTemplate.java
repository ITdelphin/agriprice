package com.agriprice.util;

public final class ResetEmailTemplate {

    public static final String SUBJECT = "Reset your AgriPrice Rwanda password";

    private ResetEmailTemplate() {
    }

    public static String text(String resetUrl, int expiryMinutes) {
        return "Hello,\n\n"
                + "A password reset was requested for your AgriPrice Rwanda agent account.\n\n"
                + "Open this secure verification link within " + expiryMinutes + " minutes:\n"
                + resetUrl + "\n\n"
                + "The link works once. If you did not request this, ignore this email and your password will stay unchanged.\n\n"
                + "AgriPrice Rwanda\n";
    }

    public static String html(String resetUrl, int expiryMinutes) {
        String safeUrl = escapeHtml(resetUrl);
        return "<!doctype html>"
                + "<html><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"></head>"
                + "<body style=\"margin:0;padding:0;background:#f2f5f3;font-family:Arial,Helvetica,sans-serif;color:#183b2a;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#f2f5f3;padding:32px 16px;\">"
                + "<tr><td align=\"center\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:560px;background:#ffffff;border-radius:18px;overflow:hidden;box-shadow:0 14px 40px rgba(23,76,49,.12);\">"
                + "<tr><td style=\"background:#174c31;padding:28px 32px;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"><tr>"
                + "<td style=\"width:42px;height:42px;border-radius:12px;background:#ffffff;color:#174c31;font-size:20px;font-weight:700;text-align:center;vertical-align:middle;\">A</td>"
                + "<td style=\"padding-left:12px;color:#ffffff;font-size:18px;font-weight:700;letter-spacing:.3px;\">AgriPrice <span style=\"font-weight:400;opacity:.8;\">Rwanda</span></td>"
                + "</tr></table>"
                + "</td></tr>"
                + "<tr><td style=\"padding:36px 32px 12px;\">"
                + "<div style=\"display:inline-block;padding:6px 12px;border-radius:999px;background:#e8f3ec;color:#174c31;font-size:12px;font-weight:700;letter-spacing:.6px;\">ACCOUNT RECOVERY</div>"
                + "<h1 style=\"margin:18px 0 10px;font-size:26px;line-height:1.25;color:#123c28;\">Reset your password</h1>"
                + "<p style=\"margin:0;color:#52685d;font-size:15px;line-height:1.6;\">We received a request to reset the password for your AgriPrice Rwanda agent account. Use the secure link below to choose a new one.</p>"
                + "</td></tr>"
                + "<tr><td style=\"padding:20px 32px 8px;text-align:center;\">"
                + "<a href=\"" + safeUrl + "\" style=\"display:inline-block;padding:14px 28px;border-radius:12px;background:#174c31;color:#ffffff;text-decoration:none;font-size:16px;font-weight:700;\">Choose new password</a>"
                + "</td></tr>"
                + "<tr><td style=\"padding:12px 32px 28px;\">"
                + "<div style=\"padding:14px 16px;border-radius:12px;background:#f5f8f6;border:1px solid #dce8e1;color:#52685d;font-size:13px;line-height:1.6;\">"
                + "<strong style=\"color:#174c31;\">Security notice:</strong> this link expires in " + expiryMinutes + " minutes and can be used once."
                + "</div>"
                + "<p style=\"margin:18px 0 8px;color:#6b7d74;font-size:12px;\">If the button does not work, copy and paste this link into your browser:</p>"
                + "<p style=\"margin:0;word-break:break-all;color:#174c31;font-size:12px;\"><a href=\"" + safeUrl + "\" style=\"color:#174c31;\">" + safeUrl + "</a></p>"
                + "</td></tr>"
                + "<tr><td style=\"padding:20px 32px;background:#f8faf9;border-top:1px solid #e5ece8;color:#7a8a82;font-size:12px;line-height:1.6;\">"
                + "If you did not request this reset, you can safely ignore this email. Your current password will remain unchanged."
                + "</td></tr>"
                + "</table>"
                + "</td></tr></table>"
                + "</body></html>";
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
