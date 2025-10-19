package brut.androlib.smali;

import brut.androlib.exceptions.AndrolibException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Performs lightweight structural validation of decoded smali sources prior to invoking the
 * expensive dex build pipeline. The validator focuses on the highest impact classes of
 * mistakes reported by reverse engineers: method termination, register accounting and branch
 * label correctness.
 */
public final class SmaliSyntaxValidator {
    private static final Pattern REGISTER_PATTERN = Pattern.compile("\\bv(\\d+)\\b");
    private static final Pattern BRANCH_PATTERN = Pattern.compile("\\b(?:goto|if-[a-z0-9_]+)\\s+(?:\\w+,\\s+)?(:[\\w$]+)");
    private static final Pattern LABEL_PATTERN = Pattern.compile("^\\s*(:[\\w$]+)");
    private static final Pattern LOCALS_PATTERN = Pattern.compile("^\\s*\\.(?:locals|registers)\\s+(\\d+)");

    private static final Set<String> KNOWN_INVALID_OPCODES = Collections.singleton("move-oject");

    private SmaliSyntaxValidator() {
    }

    public static void validate(File projectDir, Logger logger) throws AndrolibException {
        if (projectDir == null || !projectDir.exists()) {
            return;
        }
        List<ValidationError> errors = new ArrayList<>();
        traverse(projectDir.toPath(), errors);
        if (errors.isEmpty()) {
            return;
        }
        for (ValidationError error : errors) {
            if (logger != null) {
                logger.severe("E: " + error.getMessage());
            }
        }
        throw new AndrolibException(errors.get(0).getMessage());
    }

    private static void traverse(Path dir, List<ValidationError> errors) {
        File[] children = dir.toFile().listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                String name = child.getName();
                if (name.equals("smali") || name.startsWith("smali")) {
                    validateDirectory(child.toPath(), errors);
                } else {
                    traverse(child.toPath(), errors);
                }
            }
        }
    }

    private static void validateDirectory(Path dir, List<ValidationError> errors) {
        try (java.util.stream.Stream<Path> paths = Files.walk(dir)) {
            paths.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".smali"))
                .forEach(path -> {
                    try {
                        validateFile(path, errors);
                    } catch (IOException e) {
                        errors.add(new ValidationError(path, 0, "Failed to read file: " + e.getMessage()));
                    }
                });
        } catch (IOException e) {
            errors.add(new ValidationError(dir, 0, "Failed to scan directory: " + e.getMessage()));
        }
    }

    private static void validateFile(Path file, List<ValidationError> errors) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            boolean inMethod = false;
            int locals = -1;
            int highestRegister = -1;
            Set<String> labels = new HashSet<>();
            Set<String> referencedLabels = new LinkedHashSet<>();
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmed = line.trim();
                if (trimmed.startsWith(".method")) {
                    inMethod = true;
                    locals = -1;
                    highestRegister = -1;
                    labels.clear();
                    referencedLabels.clear();
                    continue;
                }
                if (!inMethod) {
                    continue;
                }
                if (trimmed.startsWith(".end method")) {
                    checkRegisterUsage(file, locals, highestRegister, errors, lineNumber);
                    checkLabelIntegrity(file, labels, referencedLabels, errors, lineNumber);
                    inMethod = false;
                    continue;
                }
                if (trimmed.startsWith(".locals") || trimmed.startsWith(".registers")) {
                    Matcher localsMatcher = LOCALS_PATTERN.matcher(trimmed);
                    if (localsMatcher.find()) {
                        locals = Integer.parseInt(localsMatcher.group(1));
                    }
                    continue;
                }
                Matcher labelMatcher = LABEL_PATTERN.matcher(trimmed);
                if (labelMatcher.find()) {
                    labels.add(labelMatcher.group(1));
                }
                Matcher branchMatcher = BRANCH_PATTERN.matcher(trimmed);
                if (branchMatcher.find()) {
                    referencedLabels.add(branchMatcher.group(1));
                }
                Matcher registerMatcher = REGISTER_PATTERN.matcher(trimmed);
                while (registerMatcher.find()) {
                    int register = Integer.parseInt(registerMatcher.group(1));
                    if (register > highestRegister) {
                        highestRegister = register;
                    }
                }
                for (String opcode : KNOWN_INVALID_OPCODES) {
                    if (trimmed.startsWith(opcode + " ") || trimmed.equals(opcode)) {
                        errors.add(new ValidationError(file, lineNumber,
                            "Unknown instruction '" + opcode + "'"));
                    }
                }
            }
            if (inMethod) {
                errors.add(new ValidationError(file, lineNumber,
                    "Method not terminated before EOF (missing .end method)"));
            }
        }
    }

    private static void checkRegisterUsage(Path file, int locals, int highestRegister,
                                           List<ValidationError> errors, int lineNumber) {
        if (locals >= 0 && highestRegister >= locals) {
            errors.add(new ValidationError(file, lineNumber,
                String.format(Locale.ROOT,
                    "Register v%d exceeds declared .locals %d", highestRegister, locals)));
        }
    }

    private static void checkLabelIntegrity(Path file, Set<String> labels,
                                            Set<String> referenced, List<ValidationError> errors,
                                            int lineNumber) {
        for (String reference : referenced) {
            if (!labels.contains(reference)) {
                errors.add(new ValidationError(file, lineNumber,
                    "Branch references undefined label " + reference));
            }
        }
    }

    public static final class ValidationError {
        private final Path file;
        private final int line;
        private final String message;

        public ValidationError(Path file, int line, String message) {
            this.file = file;
            this.line = line;
            this.message = message;
        }

        public Path getFile() {
            return file;
        }

        public int getLine() {
            return line;
        }

        public String getMessage() {
            if (line > 0) {
                return file + ":" + line + ": " + message;
            }
            return file + ": " + message;
        }
    }
}
