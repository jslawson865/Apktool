package brut.apktool.query;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

public class SmaliQueryConsole {
    private final SmaliIndexer indexer;
    private final SmaliQueryEngine engine;

    public SmaliQueryConsole() {
        this(new SmaliIndexer(), new SmaliQueryEngine());
    }

    public SmaliQueryConsole(SmaliIndexer indexer, SmaliQueryEngine engine) {
        this.indexer = indexer;
        this.engine = engine;
    }

    public void run(Path projectDir) throws IOException {
        Map<String, List<SmaliIndexer.FileContext>> index = indexer.index(projectDir);
        Map<String, String> hotkeys = buildHotkeys(index.keySet());
        System.out.println("Interactive Smali Query Console");
        System.out.println("Press the highlighted letter to query, or type 'list' to view keywords, 'q' to quit.");
        printKeywordLegend(hotkeys);
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("query> ");
                String input = scanner.nextLine().trim();
                if (input.isEmpty()) {
                    continue;
                }
                String upper = input.toUpperCase(Locale.ROOT);
                if (upper.equals("Q") || upper.equals("QUIT")) {
                    break;
                }
                if (upper.equals("LIST")) {
                    printKeywordLegend(hotkeys);
                    continue;
                }
                String keyword = resolveKeyword(upper, hotkeys);
                if (keyword == null) {
                    System.out.println("Unknown keyword. Type 'list' to see available shortcuts.");
                    continue;
                }
                String output = engine.render(keyword, index.getOrDefault(keyword, Collections.emptyList()));
                System.out.println(output);
            }
        }
    }

    private Map<String, String> buildHotkeys(Iterable<String> keywords) {
        Map<String, String> hotkeys = new LinkedHashMap<>();
        for (String keyword : keywords) {
            String key = String.valueOf(keyword.charAt(0));
            String uniqueKey = key;
            int idx = 1;
            while (hotkeys.containsKey(uniqueKey)) {
                uniqueKey = key + idx;
                idx++;
            }
            hotkeys.put(uniqueKey, keyword);
        }
        return hotkeys;
    }

    private void printKeywordLegend(Map<String, String> hotkeys) {
        System.out.println("Available Keywords:");
        for (Map.Entry<String, String> entry : hotkeys.entrySet()) {
            System.out.printf("  [%s] %s%n", entry.getKey(), entry.getValue());
        }
    }

    private String resolveKeyword(String input, Map<String, String> hotkeys) {
        if (hotkeys.containsKey(input)) {
            return hotkeys.get(input);
        }
        for (String keyword : hotkeys.values()) {
            if (keyword.equalsIgnoreCase(input)) {
                return keyword;
            }
        }
        return null;
    }
}
