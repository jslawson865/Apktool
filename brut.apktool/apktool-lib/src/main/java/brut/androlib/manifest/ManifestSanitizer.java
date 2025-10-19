/*
 *  Copyright (C) 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package brut.androlib.manifest;

import brut.androlib.ApktoolProperties;
import brut.androlib.apk.ApkInfo;
import brut.androlib.exceptions.AndrolibException;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sanitizes AndroidManifest.xml prior to the resource compilation step.
 */
public final class ManifestSanitizer {
    private static final Logger LOGGER = Logger.getLogger(ManifestSanitizer.class.getName());
    private static final String BLACKLIST_RESOURCE = "manifest_blacklist.json";
    private static final String CURRENT_VERSION = ApktoolProperties.getVersion();
    private static final ManifestBlacklist BLACKLIST;

    static {
        ManifestBlacklist loaded;
        try {
            loaded = loadBlacklist();
        } catch (AndrolibException ex) {
            LOGGER.log(Level.WARNING, "Failed to load manifest sanitizer blacklist: {0}", ex.getMessage());
            loaded = new ManifestBlacklist();
        }
        BLACKLIST = loaded;
    }

    private ManifestSanitizer() { }

    public static boolean sanitize(File manifestFile, ApkInfo apkInfo) throws AndrolibException {
        if (manifestFile == null || !manifestFile.exists() || BLACKLIST.isEmpty()) {
            return false;
        }

        Document document = parseManifest(manifestFile);
        Element root = document.getDocumentElement();
        if (root == null) {
            return false;
        }

        List<String> historyEntries = new ArrayList<>();
        AtomicBoolean modified = new AtomicBoolean(false);

        sanitizeElement(root, "", historyEntries, modified);

        if (modified.get()) {
            writeManifest(document, manifestFile);
            if (apkInfo != null) {
                if (apkInfo.manifestSanitizerHistory == null) {
                    apkInfo.manifestSanitizerHistory = new ArrayList<>();
                }
                apkInfo.manifestSanitizerHistory.addAll(historyEntries);
            }
        }

        return modified.get();
    }

    private static void sanitizeElement(Element element, String parentPath,
                                        List<String> historyEntries, AtomicBoolean modified) {
        String elementName = localName(element);
        String currentPath = parentPath.isEmpty() ? elementName : parentPath + "/" + elementName;

        if (removeIfBlacklisted(element, parentPath, historyEntries, modified)) {
            return;
        }

        removeBlacklistedAttributes(element, currentPath, historyEntries, modified);

        Node child = element.getFirstChild();
        while (child != null) {
            Node next = child.getNextSibling();
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                sanitizeElement((Element) child, currentPath, historyEntries, modified);
            }
            child = next;
        }
    }

    private static boolean removeIfBlacklisted(Element element, String parentPath,
                                               List<String> historyEntries, AtomicBoolean modified) {
        if (BLACKLIST.tagToRemove == null) {
            return false;
        }
        for (TagRule rule : BLACKLIST.tagToRemove) {
            if (rule.applies(element, parentPath)) {
                String message = String.format("Removed tag <%s> under scope '%s'.", localName(element),
                        parentPath.isEmpty() ? "root" : parentPath);
                LOGGER.info("I: " + message);
                historyEntries.add(message);
                Node parent = element.getParentNode();
                if (parent != null) {
                    parent.removeChild(element);
                    modified.set(true);
                }
                return true;
            }
        }
        return false;
    }

    private static void removeBlacklistedAttributes(Element element, String currentPath,
                                                    List<String> historyEntries, AtomicBoolean modified) {
        NamedNodeMap attributes = element.getAttributes();
        if (attributes == null || attributes.getLength() == 0) {
            return;
        }

        if (BLACKLIST.attributesToRemove == null) {
            return;
        }

        List<Attr> toRemove = new ArrayList<>();
        for (int i = 0; i < attributes.getLength(); i++) {
            Attr attr = (Attr) attributes.item(i);
            for (AttributeRule rule : BLACKLIST.attributesToRemove) {
                if (rule.applies(attr, currentPath)) {
                    toRemove.add(attr);
                    String reason = (rule.reason == null || rule.reason.isEmpty())
                            ? ""
                            : " (reason: " + rule.reason + ")";
                    String message = String.format("Removed attribute '%s' from <%s>%s.", attr.getName(),
                            currentPath, reason);
                    LOGGER.info("I: " + message);
                    historyEntries.add(message);
                    modified.set(true);
                    break;
                }
            }
        }

        for (Attr attr : toRemove) {
            element.removeAttributeNode(attr);
        }
    }

    private static Document parseManifest(File manifestFile) throws AndrolibException {
        try (InputStream is = new FileInputStream(manifestFile)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setValidating(false);
            disableExternalEntities(factory);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(is);
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            throw new AndrolibException("Failed to parse AndroidManifest.xml", ex);
        }
    }

    private static void writeManifest(Document document, File manifestFile) throws AndrolibException {
        try (FileOutputStream out = new FileOutputStream(manifestFile)) {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.transform(new DOMSource(document), new StreamResult(out));
        } catch (TransformerException | IOException ex) {
            throw new AndrolibException("Failed to write sanitized AndroidManifest.xml", ex);
        }
    }

    private static ManifestBlacklist loadBlacklist() throws AndrolibException {
        try (InputStream stream = ManifestSanitizer.class.getClassLoader().getResourceAsStream(BLACKLIST_RESOURCE)) {
            if (stream == null) {
                throw new AndrolibException("Missing manifest blacklist resource: " + BLACKLIST_RESOURCE);
            }
            String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            ManifestBlacklist blacklist = new ManifestBlacklist();
            blacklist.attributesToRemove = parseAttributeRules(json);
            blacklist.tagToRemove = parseTagRules(json);
            return blacklist;
        } catch (IOException ex) {
            throw new AndrolibException("Failed to load manifest blacklist", ex);
        }
    }

    private static List<AttributeRule> parseAttributeRules(String json) {
        List<AttributeRule> rules = new ArrayList<>();
        String section = extractSection(json, "attributes_to_remove");
        if (section == null) {
            return rules;
        }
        Matcher matcher = Pattern.compile("\\{(.*?)\\}", Pattern.DOTALL).matcher(section);
        while (matcher.find()) {
            String body = matcher.group(1);
            AttributeRule rule = new AttributeRule();
            rule.name = extractString(body, "name");
            rule.scope = extractString(body, "scope");
            rule.minApktoolVersion = extractString(body, "min_apktool_version");
            rule.reason = extractString(body, "reason");
            if (rule.name != null && rule.scope != null) {
                rules.add(rule);
            }
        }
        return rules;
    }

    private static List<TagRule> parseTagRules(String json) {
        List<TagRule> rules = new ArrayList<>();
        String section = extractSection(json, "tag_to_remove");
        if (section == null) {
            return rules;
        }
        Matcher matcher = Pattern.compile("\\{(.*?)\\}", Pattern.DOTALL).matcher(section);
        while (matcher.find()) {
            String body = matcher.group(1);
            TagRule rule = new TagRule();
            rule.name = extractString(body, "name");
            rule.scope = extractString(body, "scope");
            if (rule.name != null) {
                rules.add(rule);
            }
        }
        return rules;
    }

    private static String extractSection(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String extractString(String body, String key) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(body);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static void disableExternalEntities(DocumentBuilderFactory factory) {
        try {
            String feature = "http://apache.org/xml/features/disallow-doctype-decl";
            factory.setFeature(feature, true);
        } catch (ParserConfigurationException ignored) {
        }
        try {
            String feature = "http://xml.org/sax/features/external-general-entities";
            factory.setFeature(feature, false);
        } catch (ParserConfigurationException ignored) {
        }
        try {
            String feature = "http://xml.org/sax/features/external-parameter-entities";
            factory.setFeature(feature, false);
        } catch (ParserConfigurationException ignored) {
        }
    }

    private static String localName(Element element) {
        String localName = element.getLocalName();
        return localName != null ? localName : element.getTagName();
    }

    private static boolean matchesScope(String scope, String path) {
        if (scope == null || scope.isEmpty()) {
            return true;
        }
        if (Objects.equals(scope, path)) {
            return true;
        }
        if (path.endsWith("/" + scope)) {
            return true;
        }
        int lastSeparator = path.lastIndexOf('/') + 1;
        String leaf = lastSeparator > 0 ? path.substring(lastSeparator) : path;
        return Objects.equals(scope, leaf);
    }

    private static class ManifestBlacklist {
        List<AttributeRule> attributesToRemove = new ArrayList<>();
        List<TagRule> tagToRemove = new ArrayList<>();

        boolean isEmpty() {
            return (attributesToRemove == null || attributesToRemove.isEmpty())
                    && (tagToRemove == null || tagToRemove.isEmpty());
        }
    }

    private static class AttributeRule {
        String name;
        String scope;
        String minApktoolVersion;
        String reason;

        boolean applies(Attr attribute, String elementPath) {
            if (attribute == null || name == null || !name.equals(attribute.getName())) {
                return false;
            }
            if (!matchesScope(scope, elementPath)) {
                return false;
            }
            return isVersionSatisfied();
        }

        private boolean isVersionSatisfied() {
            if (minApktoolVersion == null || minApktoolVersion.isEmpty()) {
                return true;
            }
            return isVersionAtLeast(CURRENT_VERSION, minApktoolVersion);
        }
    }

    private static class TagRule {
        String name;
        String scope;

        boolean applies(Element element, String parentPath) {
            if (element == null || name == null) {
                return false;
            }
            String elementName = localName(element);
            if (!name.equals(elementName)) {
                return false;
            }
            return matchesScope(scope, parentPath);
        }
    }

    private static boolean isVersionAtLeast(String current, String required) {
        List<Integer> currentParts = extractVersionParts(current);
        List<Integer> requiredParts = extractVersionParts(required);
        int max = Math.max(currentParts.size(), requiredParts.size());
        for (int i = 0; i < max; i++) {
            int cur = i < currentParts.size() ? currentParts.get(i) : 0;
            int req = i < requiredParts.size() ? requiredParts.get(i) : 0;
            if (cur < req) {
                return false;
            }
            if (cur > req) {
                return true;
            }
        }
        return true;
    }

    private static List<Integer> extractVersionParts(String version) {
        if (version == null) {
            return Collections.singletonList(0);
        }
        List<Integer> parts = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\d+").matcher(version);
        while (matcher.find()) {
            parts.add(Integer.parseInt(matcher.group()));
        }
        if (parts.isEmpty()) {
            parts.add(0);
        }
        return parts;
    }
}
