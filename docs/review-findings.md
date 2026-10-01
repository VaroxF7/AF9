# Prüfbericht: Recipes, Quests, Multiblocks (und was man dagegen tun kann)

Stand: nach der kompletten Durchsicht auf dem Branch `claude/usage-limit-issue-r4fsr7`.
Nichts davon wurde in Minecraft gestartet (siehe „Grenzen"); geprüft wurde mit eigenen Werkzeugen und von Hand.

## 1. Kurzfassung

* Ich habe **sechs Prüfprogramme** gebaut (`tools/lint/`, Aufruf `bash tools/lint/run.sh --selftest`). Sie laden die KubeJS-Skripte
  gegen Attrappen von GT/Minecraft (nichts davon läuft wirklich), lesen die Quest-Dateien (SNBT), die Texturen, Modelle,
  Lang-Dateien, den Java-Quelltext und die Dokumente und melden **was im Spiel kaputt wäre**: fehlende Namen, doppelte Ids,
  Recipes die nie laufen können, Multiblocks die sich nicht bauen lassen, Quests die sich nie freischalten, Texte die lügen.
* Die Prüfung läuft jetzt bei **jedem Build in GitHub Actions** (Job `lint` vor `build`); ein Fehler stoppt den Build.
  Ein **Selbsttest** (`tools/lint/selftest.sh`) baut rund 40 Fehler absichtlich in eine Kopie des Repos und prüft, dass jede
  Regel ihren Fehler noch findet. Er hat unterwegs auch einen Fehler in meiner eigenen Arbeit gefunden (Fund 9).
* Auf dem Stand von `origin/main` (vor dieser Durchsicht) fanden die Werkzeuge **1 Fehler und 14 Warnungen**: 1× Rezept sprengt
  die Slots seiner Maschine (Void Miner), 8× Cleanroom-Regel verletzt, 2× mehrdeutige Rezepte, 2× Maschine ohne Namen,
  1× Quest-Absätze falsch nummeriert, 1× fremde ATM9-Datei (die 14 Warnungen). Alles bis auf die ATM9-Datei ist behoben. Auf dem
  Branch ist das Ergebnis: **0 Fehler, 0 echte Warnungen** (die eine Warnung ist die ATM9-Datei, Fund 7). Dazu kamen Fehler aus meiner eigenen
  Arbeit dieser Session (die Sub-atomic-Karten aus Fund 12, die neuen Kartenstufen und der IV-Hatch aus Fund 14), die die
  Werkzeuge vor dem Push gefunden haben.
* Zusätzlich sind beim Bauen der neuen Features **sechs Entwurfsfehler** aufgefallen, bevor sie eingebaut waren (Abschnitt 4).

## 2. Womit geprüft wurde

| Programm | prüft | Regeln |
|---|---|---|
| `scripts.js` | KubeJS: Recipes, Multiblock-Muster, Maschinen, Lang-Schlüssel | S1 S2 R1–R13 M1 M3 M4 L1 |
| `quests.py` | alle Quest-Kapitel (SNBT) + Texte | Q1–Q8 |
| `assets.py` | Texturen, `.mcmeta`, Modelle, Lang-Dateien, Namen | A1–A8 |
| `facts.py` | Zahlen in Quest-Texten gegen die Recipes, Java-`LithoMode` gegen die Print-Recipes | X1 X2 |
| `docs.py` | Pfade, Links, Abschnittsverweise in README und `docs/` | D1–D3 |
| `links.py` | die `Java.loadClass`-Aufrufe der Skripte und die `kubejs:`-Ids, die das Java erwartet, gegen den Java-Quelltext | J1–J3 |
| `selftest.sh` | prüft die Prüfer | – |

Jede Regel mit Bedeutung und Gegenmaßnahme steht in `tools/lint/README.md`. Nebenprodukte: `tools/textures/` (die Skripte, die
die Texturen der neuen Items zeichnen) und `tools/lint/update-gt-lists.py` (frische Namenslisten aus einem GT-Checkout).

## 3. Funde

„Wie vermeiden" ist immer die Regel, die der Fund künftig verhindert, und der Check, der sie erzwingt.

| # | Bereich | Fund | Folge im Spiel | Behoben | Wie man es vermeidet (Check) |
|---|---|---|---|---|---|
| 1 | Multiblock | `n1_computation_array` und `n1_supercomputer_array` hatten **kein `.langValue()`** und keinen Lang-Eintrag | der Block heißt im Spiel `block.gtceu.n1_computation_array` | ja: `langValue('N1 Computation Array')` / `'N1 Supercomputer Array'` | Jede Maschine bekommt `.langValue(...)` in der Definition, nie „kommt schon irgendwo her" (**A5**). Gleiches gilt für Materialien (`material.gtceu.<id>`) und Rezepttypen (`gtceu.<id>`), auch das prüft A5 jetzt |
| 2 | Recipes | Die 7 **Melt-Charge-Rezepte** (HV bis UHV) und `tin_tetrachloride` (EV) hatten **keine Cleanroom-Bedingung**, obwohl die Doku sagt „nicht-thermische Fab-Rezepte ab HV brauchen eine Cleanroom" | Einzelblock-Spieler konnten sie ohne Cleanroom laufen lassen, alle anderen Rezepte der Regel nicht | ja: `.cleanroom(CleanroomType.CLEANROOM)` ab HV (Multiblöcke bringen die Filterdecke mit), Doku §6.5 ergänzt | Eine Regel, die nur in der Doku steht, wird vergessen: **R9** prüft sie jetzt in jedem Build (jedes `fab_*`-Rezept ab 512 EU/t braucht `cleanroom()` oder `blastFurnaceTemp()`) |
| 3 | Quest | Bus-Connector-Quest: Absatz-Schlüssel in der Reihenfolge `.1 .2 .4 .3` (Text 4 stand an der falschen Stelle der Lang-Datei) | kein sichtbarer Fehler, aber ein Wartungsfallen: der nächste, der „Absatz 3" ändert, ändert den falschen | ja: umnummeriert | Absätze in Anzeigereihenfolge nummerieren (**Q5**) |
| 4 | Quest/Doku | Die Quest „The Coater Track" beschrieb **fünf Fluide pro Print**, die „Fluids per print"-Zeilen aller sieben Node-Quests (200 bis 7 nm) und die Doku-Tabelle in §6.6 stimmten nach dem Umbau nicht mehr | Spieler sehen falsche Mengen | ja: Texte neu aus den Recipes erzeugt | Zahlen, die ein Recipe entscheidet, dürfen nicht von Hand in Texte kopiert werden: **X1** vergleicht die Quest-Zahlen mit den Recipes und nennt den Schlüssel, der neu geschrieben werden muss |
| 5 | Java ↔ KubeJS | Die neun Lithographie-Modi stehen an drei Stellen (Java `LithoMode`, `AF9_WAFERS` im Server-Skript, Rezepttypen im Startup-Skript), nichts hat sie verglichen | Ein geändertes Break-Chance oder eine andere Spannung in nur einer Datei fällt erst im Spiel auf | Prüfung ergänzt, es war **keine** Abweichung vorhanden | **X2** vergleicht jetzt Substrat, Spannung, Ampere, Break-Chance und gebrochenen Wafer der 158 + 27 Print-Recipes mit `LithoMode.java` |
| 6 | Quest | Quest-Positionen: Eine neue Quest lag auf dem Link einer anderen (zweimal) | verdeckter Link | ja: andere Position | **Q4** (nur AF9-Quests; die Überlappungen der ATM9-Kapitel sind nicht unsere) |
| 7 | Repo | `kubejs/startup_scripts/gtceu/micro_universe_orb.js` ist eine **Datei von AllTheMods** (Kopfzeile „All Rights Reserved"), ihre Recipes-Datei existiert nicht mehr, ihre Multiblock-Definition verstößt gegen unsere Regeln (Mindestanzahl, `autoAbilities`) | Block-/Maschinen-Ids ohne Recipe; rechtlich fremder Inhalt im Repo | **nicht angefasst** (deine Entscheidung) | **F1** meldet jede Datei mit ATM-Kopfzeile; fremde Dateien nicht in das Overlay kopieren, sondern nur unsere ändern. Vorschlag: löschen oder bewusst behalten |
| 8 | Prüfer | `scripts.js --json` schnitt die Ausgabe bei 64 KB ab (Node beendet sich vor dem Leeren der Pipe); `assets.py` fiel dann **stillschweigend** auf „kein Register" zurück und prüfte KubeJS-Namen nicht mehr | die Prüfung wäre in CI grün gewesen, ohne etwas zu prüfen | ja: kein `process.exit` mehr; fehlendes Register ist jetzt ein **Fehler** | Ein Prüfer muss laut scheitern. Der Selbsttest prüft das indirekt (er findet A5/A1 nur mit Register) |
| 9 | Prüfer | `quests.py` stürzte ab, nachdem ich das Format des Registers geändert hatte | der Selbsttest schlug an | ja | genau dafür gibt es `selftest.sh` in CI |
| 10 | Prüfer | Falschmeldungen: GT-Blöcke aus Schleifen (Spulen, Linsen, Rohre, Lampen, GCYM-Gehäuse), `%`-Zeichen in Texten, `world_data_scanner` aus dem ATM-Pack | Lärm, der echte Funde versteckt | ja: `data/gt-patterns.txt`, `data/pack.txt`, Minecraft-genaue `%`-Regel | Listen pflegen statt Meldungen zu ignorieren |
| 11 | Prüfer | Die erste Fassung von R3 sah `shaped`/`shapeless`-Rezepte nicht (alle Plascrete-Blöcke galten als „ohne Rezept") | Falschmeldung | ja: Crafting-Rezepte werden aufgenommen und auf Ids geprüft | – |

| 12 | Recipes | Die drei **Sub-atomic-Karten** (CPU, GPU, RAM) brauchten **zwei Fluide** (Lötzinn + Quantenpunkt-Kolloid) im `assembler`. GT 7.2.0 hat dort **nur einen Fluid-Slot** (ich hatte irrtümlich angenommen, der Assembler habe zwei) | die Rezepte hätten sich in keinem Einzelblock-Assembler ausführen lassen: die Sub-atomic-Karten wären nicht herstellbar | ja: im Circuit Assembler, das Kolloid ersetzt das Lötzinn (Quantenpunkte werden gedruckt, nicht gelötet) | **R2** kennt jetzt auch die Slots von GTs eigenen Rezepttypen (`data/gt-recipe-slots.txt`, aus dem GT-Quelltext); vorher prüfte es nur unsere Typen |
| 13 | Recipes | Das **Void-Miner-Rezept** (ATM9) hatte durch unsere ASIC-Ergänzung (`4x kubejs:asic_chip`, Commit „ASIC uses") **10 Zutaten in einem Assembler mit 9 Slots** | der Void Miner war nicht mehr herstellbar | ja: die lange Titanstange entfällt (die Platten bleiben) | R2 (siehe 12). Wer eine Zutat zu einem fremden Rezept hinzufügt, zählt die Slots |
| 14 | Recipes | **Mehrdeutige Rezepte**: Ein Rezept, dessen Zutaten alle in einem anderen stecken (mit mindestens gleicher Menge), kann von der Maschine statt dessen gewählt werden. Betroffen: Bus Connector ⊂ Bus Controller, RAM-Karte (Silizium) ⊂ GTs Processor Assembly, die CPU-/GPU-/RAM-Karten der drei neuen Kartenstufen (gleiche Zutaten, andere Mengen), IV-Air-Conditioning-Hatch ⊂ Scanner | GT nimmt irgendein passendes Rezept: der Spieler hätte statt der GPU-Karte die CPU-Karte bekommen (und die Reste behalten), statt des Bus Controllers den Connector | ja: programmierte Schaltkreise (`.circuit(n)`): Connector 1 / Controller 2, Karten CPU 1 / GPU 2 / RAM 3, Hatch 1, Silizium-RAM-Karte 3 | **R7** vergleicht jetzt auch Mengen und gilt für alle Rezepttypen (unsere `af9:`-Rezepte untereinander), nicht nur für unsere Typen |
| 15 | Prüfer | R7 (die Mehrdeutigkeitsregel) galt nur für unsere eigenen Rezepttypen und hätte die Reticle-Kollision (Abschnitt 4b) im `laser_engraver` nicht gesehen | – | ja, siehe 14 | – |
Nicht als Fehler gewertet, aber im Bericht der Werkzeuge sichtbar: 17 Abhängigkeiten der ATM9-Kapitel führen auf Quests, die in
diesem Repo nicht liegen (Basis-Pack), die Überlappungen und Absatz-Nummern der ATM9-Kapitel (Basis-Pack), gemischte Tabs/Leerzeichen in zwei
Lang-Dateien (harmlos).

Stichproben ohne eigenen Befund: die **GT-Formen** der Rezepte (Platte, Folie, Stange, Schraube, Draht ...) habe ich für alle
~60 verwendeten Material/Form-Paare gegen den GT-7.2.0-Quelltext geprüft (Flags wie `EXT2_METAL`, `GENERATE_FINE_WIRE`): alle
existieren. Alle Methoden, die die Rezepte am GT-Builder aufrufen (`CWUt`, `stationResearch`, `blastFurnaceTemp` ...), gibt es in
7.2.0 mit diesen Signaturen. Die 54 Java-Klassen, die die Skripte laden, und jede ihrer verwendeten statischen Member gibt es.
Dafür gibt es keine Dauerprüfung außer J1–J3; die Formen-Prüfung war einmalig und von Hand (sie braucht GTs Flag-Logik).

## 4. Beim Bauen der neuen Features abgefangen

Das sind keine Fehler im Bestand, sondern Fehler, die die neuen Features gehabt hätten, wären sie ohne Gegenprobe gebaut worden.

| # | Feature | Fehler im Entwurf | Gegenmaßnahme | Wie man es vermeidet |
|---|---|---|---|---|
| a | Coater Track | Mit dem Coater auf HV und dem Ätzplasma (CF4, HV-Chemie) im **ersten** Print wäre der 350-nm-Node (MV) hinter HV gerutscht: der Einstieg in die ganze Lithographie wäre weg gewesen | Coater wird mit MV-Teilen gebaut, das Plasma erst ab 200 nm (der 350-nm-Print ätzt nass) | Bei jeder neuen Zutat die **Spannung der ersten Stufe** prüfen: ein neues Pflicht-Ingredient darf die Eingangsstufe nicht höher setzen (die EUt-Spalten der Tabellen in `docs/semiconductor-factory.md` §6.6) |
| b | Mask-Klassen | 27 Chips brauchen je bis zu 3 Reticles, GT hat nur **16 Linsenfarben**: eine Linse pro Rezept hätte Rezepte mit **identischen Eingaben** ergeben (die Maschine nimmt eines von beiden) | Die feineren Klassen werden vom eigenen Reticle des Chips als Master geschrieben (nicht verbraucht), nur das eigene Reticle nimmt die Linse | **R7** meldet Rezepte, deren Eingaben in einem anderen enthalten sind (ab Fund 14 auch in GT-Typen wie dem Laser Engraver; zum Zeitpunkt des Entwurfs galt es noch nur für unsere Typen, die Kollision fiel beim Nachdenken auf) |
| c | Mask-Klassen | ASoC (Linse lila) und VPU (Linse lila) wären auf demselben PSM-Rohling gelandet | ASoC bekommt Orange | die Linsenliste in Doku §6.4 (R7 kann das, seit es für alle Typen gilt: gleiche Rohlinge + gleiche Linse = gleiche Eingaben) |
| d | Mo/Si-Spiegel | Das Rezept des Spiegels hatte dieselben Feststoffe wie der EUV-Maskenrohling (Quarz, Molybdän, Silizium): Kollision | Der Spiegel nimmt ein ULE-Glassubstrat statt der Quarzplatte | R7 (Spiegel ⊂ Maskenrohling, jetzt abgedeckt) |
| e | Quests | `n50` sollte vom EUV-Rohling abhängen, der Rohling hing über Zinn-Chlorid von `n50` ab: **Quest-Zyklus** | Zinn-Chlorid hängt jetzt von der Orbital-Station ab | **Q3** findet Zyklen |
| f | Multi-Patterning | Ein Toggle-Knopf hätte drei Konsolen-Bildschirme (Line, Scanner, Orbital) umgebaut; nicht ohne Spieltest sicher | Schraubendreher auf dem Controller; die Konsolen zeigen nur „MP x2" | – |

## 5. Regeln, die daraus folgen (Checkliste vor jedem Commit)

1. `bash tools/lint/run.sh --selftest` (dasselbe läuft in CI; ohne die GT-Textur-Prüfung, Neuaufbau der Listen mit `update-gt-lists.py`; `GT_SRC=<GT-Checkout>` prüft zusätzlich GTs eigene Texturen und
   Lang-Schlüssel).
2. Neue Maschine / Material / Rezepttyp / Item → Name in der Definition (`langValue`, `displayName`) oder im Lang-File.
3. Neues `fab_*`-Rezept ab HV → `.cleanroom(CleanroomType.CLEANROOM)`, außer es hat `.blastFurnaceTemp()`.
4. Neues Recipe → `.duration()` und `.EUt()`, ganze Zahlen, Chancen 1–10000; **Slots der Maschine zählen** (Assembler: 9 Items,
   **1 Fluid**; Circuit Assembler: 6 Items, 1 Fluid), Ofenrezepte brauchen `.blastFurnaceTemp()`; teilt es seine Zutaten mit einem
   anderen Rezept derselben Maschine, bekommt eines ein `.circuit(n)`.
5. Neue Quest → Id mit `secrets.token_hex(8).upper()` und **gegen alle Kapitel geprüft**, erste Hex-Ziffer 0–7; Absätze in
   Anzeigereihenfolge nummerieren; Position mit Abstand ≥ 1 zu Quests und Links.
6. Zahl in einem Text, die ein Recipe bestimmt → nicht abtippen, sondern aus dem Recipe ableiten (siehe `facts.py`), sonst ein
   X-Check dafür ergänzen.
7. Multiblock → nur Maximalzahlen (`setMaxGlobalLimited(max, preview)`), kein `autoAbilities`, jedes Zeichen im Muster hat ein
   `where()`.
8. Regeln, die nur in der Doku stehen, in einen Check überführen (so wurde aus der Cleanroom-Regel R9).
9. Wird ein Prüfer geändert: `selftest.sh` laufen lassen und den neuen Fehler dort einbauen.

## 6. Bewusst offen / nicht gebaut

* **Wartung / Verschleiß** (Linsen, Spiegel, Kollektor, Katalysator-Deaktivierung, vorausschauende Wartung): auf deinen
  Wunsch nicht umgesetzt.
* **Temperatur-Update** (Wärme um Maschinen, Hotbar-Meldung, Bildschirmeffekt): nur der Haken `IHeatEmitter` am Air Conditioning
  Hatch; den Rest baust du am PC.
* **Quantum Computer** (`wip/quantum-computer`): wartet auf deinen Push.
* **Abgasstrom der Prints** (verbrauchtes Ätzplasma, HF/CaF₂-Kreislauf): die Print-Rezepttypen haben keine Fluid-Ausgänge; Line,
  Scanner und Orbital-Station bräuchten dafür Ausgangs-Hatches und die Konsolen eine Zeile. Der Coater hat seinen
  Abfallkreislauf (Lösungsmittel zu 60 % zurück).
* **Offene Entscheidungen, unverändert gelassen:** Cleanroom für die Prints (die Line bringt ihre eigene Filterdecke mit),
  Größe 3×4×N, welche Stufe EUV bekommt. Standen schon in `docs/semiconductor-factory.md` §9.
* **ATM9-Datei** `micro_universe_orb.js` (Fund 7): bitte entscheiden, ob sie raus soll.

## 7. Grenzen der Prüfung

* **Kein Spieltest.** Die Prüfer sehen Strukturen, Namen, Zahlen und Zusammenhänge; sie sehen nicht, wie sich eine Maschine
  anfühlt, ob ein Pattern im Spiel wirklich einrastet, wie eine Oberfläche aussieht oder ob ein Recipe zu schwer ist.
* **Java wird nur von GitHub Actions kompiliert**, nicht geprüft; die neue Multi-Patterning-Logik und die Konsolen sind
  kompiliert (Run 56), aber nie gelaufen.
* **Die GT-Listen kommen aus GT 7.2.0** (Tag `v.7.2.0-1.20.1`, die Version des Packs; `tools/lint/update-gt-lists.py`). Sie sind
  absichtlich großzügig (jede Zeichenkette der Registrierung zählt), ein gemeldeter `gtceu:`-Name existiert also sicher nirgends,
  ein nicht gemeldeter muss deshalb nicht existieren (Tippfehler, die zufällig ein anderer Name sind, gehen durch).
* **Das Basis-Pack (ATM9) wird nur oberflächlich geprüft** (Ids, unsere Texte); seine eigenen Quests und Skripte sind nicht
  unsere Arbeit.
* **Balance** (Preise, Zeiten, Mengen) prüft nichts; nur Zusammenhänge (Spannungsstufe, Ausgangs-/Eingangs-Bilanz, Zyklen).
