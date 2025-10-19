### Apktool
**This is the repository for Apktool. If you are looking for the Apktool website. Click [here](https://github.com/iBotPeaches/Apktool/tree/docs).**

[![CI](https://github.com/iBotPeaches/Apktool/actions/workflows/build.yml/badge.svg)](https://github.com/iBotPeaches/Apktool/actions/workflows/test.yml)
[![Software License](https://img.shields.io/badge/license-Apache%202.0-brightgreen.svg)](https://github.com/iBotPeaches/Apktool/blob/master/LICENSE)

Apktool is a tool for reverse engineering third-party, closed, binary, Android apps. It can decode resources to nearly original form and rebuild them after making some modifications; it makes it possible to debug smali code step-by-step. It also makes working with apps easier thanks to project-like file structure and automation of some repetitive tasks such as building apk, etc.

Apktool is **NOT** intended for piracy and other non-legal uses. It could be used for localizing and adding features, adding support for custom platforms, and other GOOD purposes. Just try to be fair with the authors of an app, that you use and probably like.

### Automated Framework Dependency Resolution (vX.Y.Z)

System applications and OEM-customized apps frequently require additional framework packages before they can be decoded. Apktool now provides an automated resolver that inspects the target APK and installs the required frameworks on the fly.

* Use `apktool d target.apk --framework-path /path/to/frameworks` to point the resolver at a directory that contains collected frameworks such as `framework-res.apk` or `sec-framework.apk`.
* When enabled (the default), Apktool scans the manifest and `resources.arsc` for known signatures. If matching frameworks are found in the supplied directory (or the default framework directory), they are installed before decoding begins.
* If a framework is suspected but cannot be located, Apktool logs a warning with the name of the missing package so you can add it to your framework collection.

### Smali Workflow Augmentation (vX.Y.Z)

Apktool now validates and indexes decoded smali sources before they are rebuilt.

* **Pre-build validation** – `apktool b` automatically runs the Smali syntax validator, catching register overflows, missing `.end method` statements, malformed branch labels, and common opcode typos prior to DEX compilation.
* **Interactive analysis** – `apktool query <project_dir>` launches a REPL that indexes smali files for key security domains such as networking, cryptography, and native loading, returning color highlighted snippets with surrounding context.

### License Manifest Automation

Run `./gradlew generateLicenseManifest` to produce a curated `LICENSE_MANIFEST.md` summarizing third-party dependencies and new internal components alongside their licenses and attributions.

#### Support
- [Project Page](https://ibotpeaches.github.io/Apktool/)
- [#apktool on libera.chat](https://web.libera.chat/)

#### Security Vulnerabilities

If you discover a security vulnerability within Apktool, please send an e-mail to Connor Tumbleson at connor.tumbleson(at)gmail.com. All security vulnerabilities will be promptly addressed.

#### Links
- [Downloads](https://bitbucket.org/iBotPeaches/apktool/downloads)
- [Downloads Mirror](https://connortumbleson.com/apktool/)
- [How to Build](https://ibotpeaches.github.io/Apktool/build/)
- [Documentation](https://ibotpeaches.github.io/Apktool/documentation/)
- [Bug Reports](https://github.com/iBotPeaches/Apktool/issues)
- [Changelog/Information](https://ibotpeaches.github.io/Apktool/changes/)
- [XDA Post](https://forum.xda-developers.com/t/util-dec-2-2020-apktool-tool-for-reverse-engineering-apk-files.1755243/)
- [Source (Github)](https://github.com/iBotPeaches/Apktool)
- [Source (Bitbucket)](https://bitbucket.org/iBotPeaches/apktool/)


## Sponsors

Special thanks goes to the following sponsors:

### Sourcetoad
[Sourcetoad](https://sourcetoad.com/) is an award-winning software and app development firm committed to the co-creation of technology solutions that solve complex business problems, delight users, and help our clients achieve their goals.

<a href="https://www.sourcetoad.com" alt="Sourcetoad">
    <picture>
        <img src="https://github.com/ibotpeaches/apktool/raw/master/.github/assets/sponsors/sourcetoad-horizontal.svg">
    </picture>
</a>

### Emerge Tools

[Emerge Tools](https://www.emergetools.com) is a suite of revolutionary products designed to supercharge mobile apps and the teams that build them.

<a href="https://www.emergetools.com" alt="Emerge Tools">
    <picture>
        <source media="(prefers-color-scheme: dark)" srcset="https://github.com/ibotpeaches/apktool/raw/master/.github/assets/sponsors/emerge-tools-vertical-white.svg">
        <source media="(prefers-color-scheme: light)" srcset="https://github.com/ibotpeaches/apktool/raw/master/.github/assets/sponsors/emerge-tools-vertical-black.svg">
        <img src="https://github.com/ibotpeaches/apktool/raw/master/.github/assets/sponsors/emerge-tools-vertical-black.svg">
    </picture>
</a>
