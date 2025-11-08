rootProject.name = "apktool-cli"
include(
    "brut.j.common",
    "brut.j.util",
    "brut.j.dir",
    "brut.apktool:apktool-lib",
    "brut.apktool:apktool-cli",
    "brut.apktool:apktool-gui"
)

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {}
    }
}
