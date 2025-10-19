package brut.apktool.query;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SmaliIndexer {
    private static final int CONTEXT_WINDOW = 5;

    private static final Map<String, Pattern> KEYWORD_PATTERNS;

    static {
        Map<String, Pattern> patterns = new LinkedHashMap<>();
        patterns.put("NETWORKING", Pattern.compile("L(?:android/net/|java/net/|okhttp3/)", Pattern.CASE_INSENSITIVE));
        patterns.put("CRYPTO", Pattern.compile("Ljavax/crypto/", Pattern.CASE_INSENSITIVE));
        patterns.put("NATIVE", Pattern.compile("Ljava/lang/System;->loadLibrary", Pattern.CASE_INSENSITIVE));
        KEYWORD_PATTERNS = Collections.unmodifiableMap(patterns);
    }

    public Map<String, List<FileContext>> index(Path projectDir) throws IOException {
        if (projectDir == null) {
            return Collections.emptyMap();
        }
        Path normalized = projectDir.toAbsolutePath().normalize();
        Map<String, List<FileContext>> result = new LinkedHashMap<>();
        for (String key : KEYWORD_PATTERNS.keySet()) {
            result.put(key, new ArrayList<>());
        }

        Files.walk(normalized)
            .filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".smali"))
            .forEach(path -> {
                try {
                    List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
                    for (int i = 0; i < lines.size(); i++) {
                        String line = lines.get(i);
                        for (Map.Entry<String, Pattern> entry : KEYWORD_PATTERNS.entrySet()) {
                            if (entry.getValue().matcher(line).find()) {
                                List<String> context = extractContext(lines, i);
                                Path relative = normalized.relativize(path.toAbsolutePath().normalize());
                                result.get(entry.getKey()).add(new FileContext(relative, i + 1, line.trim(), context));
                            }
                        }
                    }
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to index " + path + ": " + e.getMessage(), e);
                }
            });

        return result.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> {
                entry.getValue().sort(Comparator.comparing(FileContext::file).thenComparingInt(FileContext::line));
                return entry.getValue();
            }, (a, b) -> a, LinkedHashMap::new));
    }

    private List<String> extractContext(List<String> lines, int index) {
        int start = Math.max(0, index - CONTEXT_WINDOW);
        int end = Math.min(lines.size(), index + CONTEXT_WINDOW + 1);
        List<String> context = new ArrayList<>();
        for (int i = start; i < end; i++) {
            String prefix = (i == index ? "-> " : "   ");
            context.add(String.format(Locale.ROOT, "%s%4d | %s", prefix, i + 1, lines.get(i)));
        }
        return context;
    }

    public Set<String> supportedKeywords() {
        return KEYWORD_PATTERNS.keySet();
    }

    public static final class FileContext {
        private final Path file;
        private final int line;
        private final String hit;
        private final List<String> context;

        public FileContext(Path file, int line, String hit, List<String> context) {
            this.file = file;
            this.line = line;
            this.hit = hit;
            this.context = context;
        }

        public Path file() {
            return file;
        }

        public int line() {
            return line;
        }

        public String hit() {
            return hit;
        }

        public List<String> context() {
            return context;
        }
    }
}
