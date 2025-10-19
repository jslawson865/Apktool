package brut.apktool.query;

import java.util.List;
import java.util.Locale;

public class SmaliQueryEngine {
    private static final String HEADER_COLOR = "\u001B[36m";
    private static final String HIGHLIGHT_COLOR = "\u001B[93m";
    private static final String RESET = "\u001B[0m";

    public String render(String keyword, List<SmaliIndexer.FileContext> contexts) {
        StringBuilder builder = new StringBuilder();
        builder.append(HEADER_COLOR)
            .append(String.format(Locale.ROOT, "=== Results for %s (%d hit%s) ===%n",
                keyword, contexts.size(), contexts.size() == 1 ? "" : "s"))
            .append(RESET);
        if (contexts.isEmpty()) {
            builder.append("No matches found.\n");
            return builder.toString();
        }
        for (SmaliIndexer.FileContext context : contexts) {
            builder.append(HEADER_COLOR)
                .append(context.file())
                .append(':')
                .append(context.line())
                .append(RESET)
                .append('\n');
            for (String line : context.context()) {
                if (line.contains(context.hit().trim())) {
                    builder.append(HIGHLIGHT_COLOR)
                        .append(line)
                        .append(RESET)
                        .append('\n');
                } else {
                    builder.append(line).append('\n');
                }
            }
            builder.append('\n');
        }
        return builder.toString();
    }
}
