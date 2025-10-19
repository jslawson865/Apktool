package brut.apktool.license;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class LicenseScanner {
    private LicenseScanner() {
    }

    public static void main(String[] args) throws IOException {
        Path output = args.length > 0 ? Paths.get(args[0]) : Paths.get("build/reports/licenses/licenses.json");
        Files.createDirectories(output.getParent());
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        LicenseInventory inventory = new LicenseInventory();
        inventory.dependencies = curatedDependencies();
        inventory.internalComponents = internalComponents();
        Files.writeString(output, gson.toJson(inventory), StandardCharsets.UTF_8);
    }

    private static List<DependencyLicense> curatedDependencies() {
        List<DependencyLicense> deps = new ArrayList<>();
        deps.add(new DependencyLicense("com.android.tools.smali:smali", "3.0.3", "Apache-2.0", "Google", "https://github.com/JesusFreke/smali"));
        deps.add(new DependencyLicense("com.android.tools.smali:smali-baksmali", "3.0.3", "Apache-2.0", "Google", "https://github.com/JesusFreke/smali"));
        deps.add(new DependencyLicense("commons-cli:commons-cli", "1.6.0", "Apache-2.0", "Apache Software Foundation", "https://commons.apache.org/proper/commons-cli/"));
        deps.add(new DependencyLicense("com.google.guava:guava", "32.0.1-jre", "Apache-2.0", "Google", "https://github.com/google/guava"));
        deps.add(new DependencyLicense("com.google.code.gson:gson", "2.10.1", "Apache-2.0", "Google", "https://github.com/google/gson"));
        deps.add(new DependencyLicense("commons-io:commons-io", "2.15.1", "Apache-2.0", "Apache Software Foundation", "https://commons.apache.org/proper/commons-io/"));
        return deps;
    }

    private static List<InternalComponent> internalComponents() {
        List<InternalComponent> components = new ArrayList<>();
        components.add(new InternalComponent("ManifestSanitizer", "Apache 2.0 (New)", "J S Lawson Ventures"));
        components.add(new InternalComponent("ContextualFrameworkResolver", "Apache 2.0 (New)", "J S Lawson Ventures"));
        components.add(new InternalComponent("SmaliSyntaxValidator", "Apache 2.0 (New)", "J S Lawson Ventures"));
        return components;
    }

    public static final class LicenseInventory {
        public List<DependencyLicense> dependencies;
        public List<InternalComponent> internalComponents;
    }

    public static final class DependencyLicense {
        public String artifact;
        public String version;
        public String license;
        public String author;
        public String url;

        public DependencyLicense(String artifact, String version, String license, String author, String url) {
            this.artifact = artifact;
            this.version = version;
            this.license = license;
            this.author = author;
            this.url = url;
        }
    }

    public static final class InternalComponent {
        public String component;
        public String license;
        public String author;

        public InternalComponent(String component, String license, String author) {
            this.component = component;
            this.license = license;
            this.author = author;
        }
    }
}
