# APK Tour Template

This template organizes an APK's extracted contents into repeatable "tour" views. Each view highlights files and folders involved in a specific functional domain so that auditors, testers, or maintainers can quickly locate related assets.

## How to Use This Template
1. Extract the APK into a working directory (e.g., using `apktool d <apk>`).
2. Populate the tables below by mapping the APK's folders and files into the relevant view(s).
3. Capture additional notes such as test cases, outstanding issues, or ownership details for each domain.
4. Store the completed tour alongside your analysis documentation so future reviews can reuse it.

---

## 1. Billing & Monetization View
| Area | Path(s) | Description | Notes |
| ---- | ------- | ----------- | ----- |
| In-app billing client code | `smali_classes*/**/billing/` | Core billing flows, purchase handling logic | |
| Subscription management | `res/xml/`, `res/raw/` | SKU configuration, subscription plans | |
| Billing configuration files | `assets/billing/`, `assets/config/` | Remote config, feature flags impacting billing | |
| Payment provider integrations | `smali_classes*/**/payments/` | Third-party SDK hooks, payment processors | |
| Receipts & verification | `smali_classes*/**/receipt/` | Signature checks, server validation helpers | |

---

## 2. Configuration & Feature Flags View
| Area | Path(s) | Description | Notes |
| ---- | ------- | ----------- | ----- |
| Global configuration manifests | `res/xml/`, `res/raw/`, `assets/config/` | Default configuration bundles | |
| Environment/variant toggles | `smali_classes*/**/config/` | Runtime configuration managers, flag readers | |
| Remote configuration endpoints | `smali_classes*/**/network/`, `res/values/strings.xml` | URLs and identifiers for fetching configs | |
| Device capability handling | `AndroidManifest.xml`, `smali_classes*/**/compat/` | Feature availability per device/version | |

---

## 3. Permissions & Security View
| Area | Path(s) | Description | Notes |
| ---- | ------- | ----------- | ----- |
| Declared permissions | `AndroidManifest.xml` | Required/optional permissions, protection levels | |
| Runtime permission prompts | `smali_classes*/**/permission/`, `res/values/strings.xml` | Permission request flows, rationale copy | |
| Sensitive data storage | `smali_classes*/**/storage/`, `res/xml/backup/` | Credential caches, backups, encrypted storage | |
| Security-sensitive binaries | `lib/**`, `oat/**` | Native libraries handling crypto, DRM | |
| Integrity checks & tamper detection | `smali_classes*/**/integrity/`, `assets/**` | Signature verification, anti-tamper logic | |

---

## 4. Administration & Management View
| Area | Path(s) | Description | Notes |
| ---- | ------- | ----------- | ----- |
| Administrative UI components | `res/layout/**admin**`, `smali_classes*/**/admin/` | Activities/fragments for admin features | |
| Access control & role management | `smali_classes*/**/auth/`, `res/xml/roles/` | Role assignments, entitlement checks | |
| Audit logging | `smali_classes*/**/logging/`, `assets/logging/` | Log sinks, event schemas | |
| Scheduled jobs & services | `AndroidManifest.xml`, `smali_classes*/**/scheduler/` | Background sync, maintenance tasks | |

---

## 5. Networking & Connectivity View
| Area | Path(s) | Description | Notes |
| ---- | ------- | ----------- | ----- |
| API clients & endpoints | `smali_classes*/**/network/`, `res/values/strings.xml` | REST/gRPC clients, base URLs | |
| WebSocket or streaming components | `smali_classes*/**/stream/` | Persistent connection handlers | |
| Certificates & trust stores | `res/raw/*.crt`, `assets/certs/` | Embedded certificates, pinning configurations | |
| Analytics & telemetry endpoints | `smali_classes*/**/analytics/`, `assets/analytics/` | Data collection services, identifiers | |

---

## 6. Updates & Split Delivery View
| Area | Path(s) | Description | Notes |
| ---- | ------- | ----------- | ----- |
| Dynamic feature modules | `split_config.*`, `smali_classes*/split*/` | Split APK modules and related resources | |
| Update workflows | `smali_classes*/**/update/` | In-app update logic (Play Core, custom flows) | |
| Patch/binary diff assets | `assets/patches/`, `res/raw/patch/*` | OTA patch data, delta updates | |
| Version metadata | `build-data.properties`, `res/values/version.xml` | Release identifiers, build channels | |

---

## 7. Advertising & Attribution View
| Area | Path(s) | Description | Notes |
| ---- | ------- | ----------- | ----- |
| Ad SDK integrations | `smali_classes*/**/ads/`, `lib/**/ads*.so` | Embedded ad networks and adapters | |
| Mediation & waterfalls | `smali_classes*/**/mediation/` | Mediation configurations, fallback logic | |
| Attribution & tracking | `smali_classes*/**/tracking/`, `res/xml/analytics/` | Install tracking, conversion measurement | |
| Consent management | `res/layout/**consent**`, `smali_classes*/**/consent/` | GDPR/CCPA consent flows | |

---

## 8. Binary & Resource Integrity View
| Area | Path(s) | Description | Notes |
| ---- | ------- | ----------- | ----- |
| Native libraries | `lib/**` | ABI-specific `.so` files | |
| Optimized bytecode | `oat/**`, `dex/**` | Pre-optimized or runtime dex files | |
| Resource packages | `res/**`, `resources.arsc` | Localized resources, drawables, strings | |
| Asset bundles | `assets/**`, `raw/**` | Additional packaged data, ML models | |

---

## 9. Custom Domain-Specific Views
Add bespoke views when the application contains unique domains (e.g., IoT device control, media playback, social features).

| View Name | Area | Path(s) | Description | Notes |
| --------- | ---- | ------- | ----------- | ----- |
| | | | | |
| | | | | |

---

## Appendix: Collection Checklist
- [ ] APK decompiled and folder structure documented
- [ ] Paths verified against the latest build
- [ ] Sensitive information redacted before sharing
- [ ] Ownership and escalation contacts recorded for each view

