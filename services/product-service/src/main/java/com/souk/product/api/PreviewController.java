package com.souk.product.api;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/preview")
public class PreviewController {

    private static final long MAX_BYTES = 20L * 1024 * 1024; // 20MB safety cap

    @PostMapping(path = "/fetch", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> fetch(@RequestBody Map<String, String> body) {
        String url = body.get("url");
        if (url == null || !(url.startsWith("http://") || url.startsWith("https://"))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only http/https URLs supported"));
        }
        try {
            String hash = sha256Hex(url);
            String baseDir = System.getProperty("user.home") + "/souk-uploads/previews";
            Files.createDirectories(Path.of(baseDir));

            // Try to infer extension from URL; fallback later from content-type
            String ext = inferredExtFromUrl(url);
            Path target = Paths.get(baseDir, hash + (ext != null ? ("." + ext) : ""));

            String mime = null;
            long size = 0L;

            if (!Files.exists(target)) {
                HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(10)).build();
                HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30)).GET().build();
                HttpResponse<InputStream> resp = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
                if (resp.statusCode() >= 400) {
                    return ResponseEntity.status(resp.statusCode()).body(Map.of("error", "Upstream returned " + resp.statusCode()));
                }
                mime = resp.headers().firstValue("content-type").orElse(null);
                if (ext == null && mime != null) {
                    ext = extFromMime(mime);
                }
                target = Paths.get(baseDir, hash + (ext != null ? ("." + ext) : ""));
                try (InputStream in = resp.body(); OutputStream out = Files.newOutputStream(target)) {
                    byte[] buf = new byte[8192];
                    long written = 0;
                    int r;
                    while ((r = in.read(buf)) != -1) {
                        written += r;
                        if (written > MAX_BYTES) {
                            try { out.close(); } catch (Exception ignored) {}
                            Files.deleteIfExists(target);
                            return ResponseEntity.status(413).body(Map.of("error", "File too large (>20MB)"));
                        }
                        out.write(buf, 0, r);
                    }
                    size = written;
                }
            } else {
                size = Files.size(target);
            }

            Map<String, Object> resp = new HashMap<>();
            resp.put("localUrl", "/uploads/previews/" + target.getFileName());
            if (mime == null) mime = probeMime(target);
            if (mime != null) resp.put("mimeType", mime);
            resp.put("size", size);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    private static String probeMime(Path p) {
        try { return Files.probeContentType(p); } catch (Exception ignored) { return null; }
    }

    private static String inferredExtFromUrl(String url) {
        int q = url.indexOf('?');
        String path = q >= 0 ? url.substring(0, q) : url;
        int dot = path.lastIndexOf('.');
        if (dot > 0 && dot > path.lastIndexOf('/')) {
            String e = path.substring(dot + 1).toLowerCase();
            if (e.length() <= 5) return e;
        }
        return null;
    }

    private static String extFromMime(String mime) {
        if (mime == null) return null;
        String m = mime.toLowerCase();
        if (m.startsWith("image/")) return m.substring(6);
        if (m.startsWith("video/")) return m.substring(6);
        return null;
    }

    private static String sha256Hex(String s) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] dig = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : dig) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}

