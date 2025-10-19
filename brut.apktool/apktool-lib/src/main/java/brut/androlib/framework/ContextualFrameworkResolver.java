package brut.androlib.framework;

import brut.androlib.Config;
import brut.androlib.apk.ApkInfo;
import brut.androlib.exceptions.AndrolibException;
import brut.androlib.res.Framework;
import brut.androlib.res.data.ResPackage;
import brut.androlib.res.data.ResResSpec;
import brut.androlib.res.decoder.ARSCDecoder;
import brut.androlib.res.decoder.AXmlResourceParser;
import brut.directory.DirectoryException;
import brut.directory.ExtFile;
import org.apache.commons.text.StringEscapeUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects framework dependencies for system and OEM applications and attempts
 * to install the matching framework packages prior to decoding resources.
 */
public final class ContextualFrameworkResolver {
    private static final Logger LOGGER = Logger.getLogger(ContextualFrameworkResolver.class.getName());

    private static final FrameworkHints HINTS = FrameworkHints.load();

    private ContextualFrameworkResolver() {
        // Utility class
    }

    public static void resolveAndInstall(Config config, ApkInfo apkInfo) {
        if (config == null || apkInfo == null || !config.autoResolveFrameworks) {
            return;
        }

        ExtFile apkFile = apkInfo.getApkFile();
        if (apkFile == null) {
            return;
        }

        if (HINTS.isEmpty()) {
            return;
        }

        Map<String, String> requests = new LinkedHashMap<>();

        Optional<String> manifestPackage = readManifestPackage(apkFile);
        manifestPackage.ifPresent(packageName -> {
            String mapped = HINTS.getSystemPackageMatch(packageName);
            if (mapped != null) {
                mergeRequest(requests, new FrameworkRequest(mapped,
                        String.format("manifest package '%s'", packageName)));
            }
        });

        for (FrameworkRequest request : scanResources(apkFile)) {
            mergeRequest(requests, request);
        }

        if (requests.isEmpty()) {
            return;
        }

        Framework framework = new Framework(config);
        File frameworkDirectory;
        try {
            frameworkDirectory = framework.getFrameworkDirectory();
        } catch (AndrolibException ex) {
            LOGGER.log(Level.FINE, "Unable to initialise framework directory for contextual resolver", ex);
            return;
        }

        File searchDirectory = null;
        if (config.frameworkSearchPath != null) {
            File candidate = new File(config.frameworkSearchPath);
            if (candidate.isDirectory()) {
                searchDirectory = candidate;
            } else {
                LOGGER.warning(String.format("Contextual framework search path '%s' is not a directory.",
                        config.frameworkSearchPath));
            }
        }

        for (Map.Entry<String, String> entry : requests.entrySet()) {
            String fileName = entry.getKey();
            String reason = entry.getValue();

            File frameworkSource = locateFrameworkSource(fileName, searchDirectory, frameworkDirectory);
            if (frameworkSource == null) {
                logMissingFramework(fileName, reason, searchDirectory, frameworkDirectory);
                continue;
            }

            try {
                LOGGER.info(String.format("I: Installing contextual framework '%s' (%s).",
                        fileName, reason));
                framework.installFramework(frameworkSource);
            } catch (AndrolibException ex) {
                LOGGER.log(Level.WARNING,
                        String.format("Failed to install contextual framework from '%s'.", frameworkSource.getAbsolutePath()),
                        ex);
            }
        }
    }

    private static void mergeRequest(Map<String, String> requests, FrameworkRequest request) {
        requests.merge(request.fileName, request.reason, (existing, incoming) -> {
            if (existing.equals(incoming)) {
                return existing;
            }
            return existing + "; " + incoming;
        });
    }

    private static Optional<String> readManifestPackage(ExtFile apkFile) {
        try (InputStream stream = apkFile.getDirectory().getFileInput("AndroidManifest.xml")) {
            if (stream == null) {
                return Optional.empty();
            }

            AXmlResourceParser parser = new AXmlResourceParser(null);
            parser.open(stream);
            try {
                int event;
                while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.START_TAG && "manifest".equals(parser.getName())) {
                        String packageName = parser.getAttributeValue(null, "package");
                        if (packageName != null && !packageName.isEmpty()) {
                            return Optional.of(packageName);
                        }
                    }
                }
            } finally {
                parser.close();
            }
        } catch (DirectoryException | IOException | XmlPullParserException ex) {
            LOGGER.log(Level.FINE, "Unable to parse AndroidManifest.xml for contextual framework resolution", ex);
        }
        return Optional.empty();
    }

    private static Collection<FrameworkRequest> scanResources(ExtFile apkFile) {
        if (HINTS.resourceSignatures.isEmpty()) {
            return Collections.emptyList();
        }

        Map<ResourceSignatureHint, String> matches = new LinkedHashMap<>();

        try (InputStream stream = apkFile.getDirectory().getFileInput("resources.arsc")) {
            if (stream == null) {
                return Collections.emptyList();
            }

            ResPackage[] packages = ARSCDecoder.decode(stream, false, true).getPackages();
            for (ResPackage resPackage : packages) {
                for (ResResSpec spec : resPackage.listResSpecs()) {
                    String resId = spec.getId().toString();
                    for (ResourceSignatureHint hint : HINTS.resourceSignatures) {
                        if (!matches.containsKey(hint) && hint.matches(resId)) {
                            matches.put(hint, resId);
                        }
                    }
                }

                if (matches.size() == HINTS.resourceSignatures.size()) {
                    break;
                }
            }
        } catch (AndrolibException | DirectoryException | IOException ex) {
            LOGGER.log(Level.FINE, "Unable to scan resources.arsc for contextual framework hints", ex);
        }

        if (matches.isEmpty()) {
            return Collections.emptyList();
        }

        List<FrameworkRequest> requests = new ArrayList<>();
        for (Map.Entry<ResourceSignatureHint, String> entry : matches.entrySet()) {
            ResourceSignatureHint hint = entry.getKey();
            String reason;
            if (hint.hint != null && !hint.hint.isEmpty()) {
                reason = String.format("%s via resource %s", hint.hint, entry.getValue());
            } else {
                reason = String.format("resource %s", entry.getValue());
            }
            requests.add(new FrameworkRequest(hint.fileName, reason));
        }
        return requests;
    }

    private static File locateFrameworkSource(String fileName, File searchDirectory, File frameworkDirectory) {
        if (searchDirectory != null) {
            File candidate = new File(searchDirectory, fileName);
            if (candidate.isFile()) {
                return candidate;
            }
        }

        File fallback = new File(frameworkDirectory, fileName);
        if (fallback.isFile()) {
            return fallback;
        }

        return null;
    }

    private static void logMissingFramework(String fileName, String reason, File searchDirectory, File frameworkDirectory) {
        if (searchDirectory != null) {
            LOGGER.warning(String.format(
                    "Contextual framework '%s' (%s) was not found in '%s' or '%s'.",
                    fileName,
                    reason,
                    searchDirectory.getAbsolutePath(),
                    frameworkDirectory.getAbsolutePath()));
        } else {
            LOGGER.warning(String.format(
                    "Contextual framework '%s' (%s) was not found in '%s'.",
                    fileName,
                    reason,
                    frameworkDirectory.getAbsolutePath()));
        }
    }

    private static final class FrameworkRequest {
        final String fileName;
        final String reason;

        FrameworkRequest(String fileName, String reason) {
            this.fileName = fileName;
            this.reason = reason;
        }
    }

    private static final class FrameworkHints {
        private final Map<String, String> systemPackageNames;
        private final List<ResourceSignatureHint> resourceSignatures;

        FrameworkHints(Map<String, String> systemPackageNames, List<ResourceSignatureHint> resourceSignatures) {
            this.systemPackageNames = systemPackageNames;
            this.resourceSignatures = resourceSignatures;
        }

        static FrameworkHints load() {
            try (InputStream stream = ContextualFrameworkResolver.class
                    .getClassLoader()
                    .getResourceAsStream("framework_hints.json")) {
                if (stream == null) {
                    return new FrameworkHints(Collections.emptyMap(), Collections.emptyList());
                }

                String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> packages = parsePackages(json);
                List<ResourceSignatureHint> signatures = parseSignatures(json);
                return new FrameworkHints(packages, signatures);
            } catch (IOException ex) {
                LOGGER.log(Level.FINE, "Unable to read contextual framework hints", ex);
                return new FrameworkHints(Collections.emptyMap(), Collections.emptyList());
            }
        }

        boolean isEmpty() {
            return systemPackageNames.isEmpty() && resourceSignatures.isEmpty();
        }

        String getSystemPackageMatch(String packageName) {
            return systemPackageNames.get(packageName);
        }

        static Map<String, String> parsePackages(String json) {
            Matcher section = Pattern.compile("\\\"system_package_names\\\"\\s*:\\s*\\{([^}]*)\\}", Pattern.DOTALL).matcher(json);
            if (!section.find()) {
                return Collections.emptyMap();
            }

            Map<String, String> result = new LinkedHashMap<>();
            Matcher entry = Pattern.compile("\\\"([^\\\"]+)\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"").matcher(section.group(1));
            while (entry.find()) {
                String key = StringEscapeUtils.unescapeJson(entry.group(1));
                String value = StringEscapeUtils.unescapeJson(entry.group(2));
                result.put(key, value);
            }
            return result;
        }

        static List<ResourceSignatureHint> parseSignatures(String json) {
            Matcher section = Pattern.compile("\\\"resource_signatures\\\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL).matcher(json);
            if (!section.find()) {
                return Collections.emptyList();
            }

            List<ResourceSignatureHint> result = new ArrayList<>();
            Matcher objectMatcher = Pattern.compile("\\{(.*?)\\}", Pattern.DOTALL).matcher(section.group(1));
            while (objectMatcher.find()) {
                String object = objectMatcher.group(1);
                String pattern = extractString(object, "pattern");
                String fileName = extractString(object, "file_name");
                String hint = extractString(object, "hint");

                if (pattern != null && fileName != null) {
                    result.add(new ResourceSignatureHint(pattern, fileName, hint));
                }
            }
            return result;
        }

        private static String extractString(String json, String key) {
            String regex = "\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"";
            Matcher matcher = Pattern.compile(regex).matcher(json);
            if (matcher.find()) {
                return StringEscapeUtils.unescapeJson(matcher.group(1));
            }
            return null;
        }
    }

    private static final class ResourceSignatureHint {
        private final Pattern pattern;
        private final String fileName;
        private final String hint;

        ResourceSignatureHint(String regex, String fileName, String hint) {
            this.pattern = Pattern.compile(regex);
            this.fileName = fileName;
            this.hint = hint;
        }

        boolean matches(String resourceId) {
            return pattern.matcher(resourceId).matches();
        }
    }
}
