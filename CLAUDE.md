
# Kontext für Claude

<!-- Kurze Projektbeschreibung: Name und Zweck in 1-2 Sätzen -->
InficicalPlugin — IntelliJ-Platform-Plugin zur Integration von Infisical (Secrets-Management).

## Beim Start

**Pflichtausgabe beim ersten Prompt jeder Session:** Lies diese CLAUDE.md und gib sofort folgende Übersicht aus — ohne Aufforderung, keine Ausnahmen:

- **Projekt:** Name und Zweck (1 Satz)
- **Schwerpunkte:** Aktuelle Themen als Stichpunkte
- **Offene TODOs:** Alle nicht abgehakten Punkte aus `docs/todos.md`
- **Skills:** Alle projektlokalen Skills aus der Skills-Tabelle (Name + Kurzbeschreibung)
- **Quickstart:** Die wichtigsten Skills für den Einstieg
- **Setup-Status:** Falls `## Projektinitialisierung` noch vorhanden ist → frage ob das Onboarding jetzt interaktiv durchgeführt werden soll (Fallback für den Fall, dass es direkt nach `init`/`re-init` übersprungen oder vertagt wurde)
- **README-Merge:** Falls `README.template.md` im Projektverzeichnis vorhanden ist → weise explizit darauf hin: *"Es gibt eine bestehende README.md und eine README.template.md (Template-Struktur). Soll ich beide zusammenführen?"*

> Tipp: Falls die Session mid-Session gestartet wurde (z.B. nach `git clone`), kann die Übersicht manuell mit `/abc-teamwork` abgerufen werden.

## Deine Rolle

- Entwickeln, Reviewen, Debuggen des IntelliJ-Plugins (Java/Gradle) · Refactorings vorschlagen · Tests schreiben
- Du schlägst Verbesserungen vor wenn sinnvoll
- Antworte auf Deutsch, außer es wird explizit anders gewünscht

## Konventionen

Allgemeine Konventionen (Markdown, Dateinamen) → `.claude/rules/`

- Commits mit `/commit`-Skill erstellen
- **Skills/Agents/Rules nicht ungewollt projekt-lokal editieren:** `.claude/commands|agents|rules` in diesem Projekt werden bei jedem `/syncfromtemplate`-Lauf mit dem globalen Stand abgeglichen — unveränderte, veraltete Kopien werden dabei entfernt. Für allgemeine Anpassungen immer direkt in `~/.claude/...` bearbeiten (gilt dann für alle Projekte), danach mit `/synctotemplate` ins Template-Repo zurückspielen. Soll ein Skill/Agent/Rule bewusst **nur in diesem Projekt** anders verhalten, projekt-lokal eine gleichnamige Datei anlegen und die Zeile `override: true` ergänzen — `/syncfromtemplate` lässt diese Datei dann unangetastet, und Claude Code bevorzugt sie automatisch gegenüber der globalen Version.
-ich bin ein Praktikant und möchte so viel wie möglich lernen bei diesem Project und ich möchte so weit möglich viel eigenständig Programmieren.
Keine besonderen projektspezifischen Konventionen (Coding-Style, Branch-Naming, Commit-Format, Test-Strategie) über die allgemeinen Regeln hinaus.

## Aktuelle Schwerpunkte

<!-- Was wird gerade bearbeitet? Regelmäßig aktualisieren -->
- Frühe Phase: Gradle-/IntelliJ-Plugin-Grundgerüst vorhanden, noch keine konkrete Feature-Arbeit

## Verfügbare Skills

| Skill | Beschreibung |
|---|---|
| `/claudemd` | CLAUDE.md aktuell halten (Skills-Tabelle, Schwerpunkte) |
| `/abc-teamwork [info\|help\|update\|synctotemplate\|init\|templateversion\|docs-export\|howto]` | Projektübersicht, Template-Sync, Projekt-Init, Template-Version, Docs-Export, Howto |
| `/todo [show\|add\|done\|rm\|move\|list]` | TODO-Liste verwalten (`docs/todos.md`) |
| `/document` | Projektdokumentation prüfen und aktualisieren (README.md, todos.md) |
| `/erkenntnis [save\|list\|summary\|search]` | Erkenntnisse verwalten und dokumentieren |
| `/version [show\|setup\|snapshot\|merge\|bump\|tag\|regenerate]` | App-Version verwalten (Tag-getrieben, SemVer) |
| `/commit` | Änderungen analysieren und smarte, atomare Commits erstellen |
| `/spec [#N\|suchtext]` | Implementierungs-Spec für ein TODO erstellen |
| `/implement` | Spec implementieren (Feature-Branch, Code, Tests, Merge) |
| `/gitlab-ci [new\|validate\|...]` | `.gitlab-ci.yml` erstellen oder validieren |
| `/timelog` | Tagesend-Zeiterfassung (Arbeiten gruppieren, Zeiten erfassen) |
| `/githooks [install\|uninstall\|help]` | Globale Git-Hooks installieren oder deinstallieren |
| `/synctotemplate [skillname]` | Skills/Agents/Rules aus diesem Projekt ins Template-Repo zurückübertragen |
| `/syncfromtemplate` | Template-Infrastruktur (`.claude/`, globale Skills inkl. Howto) aus dem Template-Repo aktualisieren |
| `/docs-export [setup\|export]` | Projektdokumentation als ZIM-Wiki und/oder Obsidian-Vault exportieren |

## Template-Repo

Sync-Konfiguration (Git-URL, Marker ob dieses Projekt template-verwaltet ist) liegt in
`.claude/template-repo.md` — nicht hier editieren.

## TODOs

Siehe `docs/todos.md` — verwaltbar mit `/todo`.
