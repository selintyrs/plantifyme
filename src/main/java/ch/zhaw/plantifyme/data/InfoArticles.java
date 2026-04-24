package ch.zhaw.plantifyme.data;

import java.util.List;

public final class InfoArticles {

    public record Article(String id, String category, String title, String content, List<String> sources) {}

    public static final List<Article> ALL_ARTICLES = List.of(
            new Article("fleischindustrie", "Tiere & Ethik",
                    "Die Fleischindustrie: Was hinter unserem Stück Fleisch steckt",
                    """
                    Die Fleischproduktion ist ein auf Effizienz ausgerichteter Prozess, in dem Tiere primär als Wirtschaftsgüter betrachtet werden. Der gesamte Ablauf – von der Aufzucht bis zur Schlachtung – ist auf Durchsatzgeschwindigkeit und ökonomische Kennzahlen ausgelegt.

                    Aufzucht und Wachstum

                    In der industriellen Mast werden Tiere nach ihrer täglichen Gewichtszunahme bewertet. Mastpoulets erreichen heute durch intensive Zucht innerhalb von etwa 30 Tagen ein Zielgewicht von 2 kg. Dieses beschleunigte Wachstum kann zu erheblichen physischen Belastungen führen [1].

                    Lebensdauer und Verwertung

                    Die Nutzungsdauer orientiert sich nicht an der biologischen Lebensspanne, sondern am wirtschaftlichen Produktionszyklus. Schweine werden mit etwa 6 Monaten, Rinder mit rund 2 Jahren und Masthühner nach wenigen Wochen geschlachtet – deutlich vor Erreichen ihrer natürlichen Lebenserwartung [2].

                    Betäubung

                    Bei Schweinen ist die Betäubung mit Kohlendioxid (CO₂) ein verbreiteter industrieller Standard. Das Gas führt auf den Schleimhäuten zu Kohlensäurebildung und akuter Atemnot, bevor der narkotische Zustand eintritt [3].

                    Fehleranfälligkeit

                    Der hohe Zeitdruck in der Schlachtlinie macht den Prozess fehleranfällig. Untersuchungen zeigen, dass Fehlbetäubungen – in manchen Betrieben bis zu 44 % – im laufenden Akkordbetrieb nicht immer erkannt werden [4].""",
                    List.of(
                            "STS – Schweizer Tierschutz. (o.J.). Mastpoulets. https://www.tierschutz.com",
                            "Vier Pfoten. (o.J.). Lebensdauer von Nutztieren. https://www.vier-pfoten.ch",
                            "EFSA. (2020). Welfare of pigs at slaughter. EFSA Journal, 18(6), 6148.",
                            "PROVIEH. (2026). Stellungnahmen zur Prozessüberwachung in Schlachtbetrieben."
                    )),
            new Article("milchindustrie", "Tiere & Ethik",
                    "Die Milchindustrie: Wie Milchproduktion funktioniert",
                    """
                    Wie bei allen Säugetieren produziert eine Kuh Milch nur, um ihren Nachwuchs zu ernähren. Damit dieser Laktationszyklus aufrechterhalten wird, muss sie regelmässig kalben. In der modernen Milchwirtschaft werden Kühe dafür systematisch besamt, um die Milchleistung konstant auf einem wirtschaftlich verwertbaren Niveau zu halten [1].

                    Damit die Milch für den Markt verfügbar bleibt, erfolgt in konventionellen Betrieben die Trennung von Kalb und Muttertier meist innerhalb kurzer Zeit nach der Geburt [1]. Männliche Kälber haben für die Milchproduktion keinen direkten Nutzen, da sie selbst keine Milch geben. Sie werden daher in die Kälbermast überführt und nach wenigen Monaten geschlachtet [2].

                    Durch gezielte Zucht liegt die durchschnittliche Milchleistung in der Schweiz heute bei rund 7'100 kg pro Kuh und Jahr [3]. Diese hohe Leistung stellt eine erhebliche körperliche Belastung dar. Wenn die Milchmenge nach einigen Jahren nachlässt, werden die Kühe in der Regel aus der Produktion genommen.""",
                    List.of(
                            "FiBL – Forschungsinstitut für biologischen Landbau. (2023). Milchviehhaltung: Kuh-Kalb-Trennung.",
                            "Proviande. (2024). Fleischmarkt Schweiz.",
                            "Agristat et al. (2024). Milchstatistik der Schweiz."
                    )),
            new Article("eierindustrie", "Tiere & Ethik",
                    "Die Eierindustrie: Wie die Eierproduktion funktioniert",
                    """
                    Das ursprüngliche Bankivahuhn legte zur Fortpflanzung etwa 40 Eier im Jahr. Durch gezielte Zucht wurden daraus spezialisierte Legehybriden entwickelt, die rund 300 Eier pro Jahr produzieren. Nur zwei globale Konzerne erzeugen 90 % dieser Zuchtlinien weltweit [1].

                    Die fast tägliche Eiproduktion verlangt dem Körper der Hennen viel Kalzium für die Schalenbildung ab. Nach etwa einem Jahr treten vermehrt schmerzhafte Knochenbrüche auf – unabhängig von der Haltungsform [2].

                    Da Legehybriden auf Eiproduktion spezialisiert sind, setzen sie kaum verwertbares Fleisch an. Männliche Küken gelten daher als wirtschaftlich nicht nutzbar und werden in vielen Ländern nach dem Schlüpfen getötet oder vorab als Embryonen aussortiert [3].

                    Nach etwa 12 bis 15 Monaten sinkt die Legeleistung leicht. Da eine weitere Fütterung ab diesem Punkt wirtschaftlich nicht mehr rentabel ist, wird die gesamte Herde ausgestallt und durch neue Junghennen ersetzt [1].""",
                    List.of(
                            "Albert Schweitzer Stiftung. (2022). Legehennenhaltung. https://albert-schweitzer-stiftung.de",
                            "STS – Schweizer Tierschutz. (2023). Tierwohl bei Legehennen.",
                            "Heinrich-Böll-Stiftung. (2021). Fleischatlas 2021."
                    )),
            new Article("honigindustrie", "Tiere & Ethik",
                    "Die Honigindustrie: Bienen als Nutztiere",
                    """
                    In der Imkerei werden Bienenvölker als landwirtschaftliche Nutztiere gehalten und bewirtschaftet. Um das natürliche Ausschwärmen zu verhindern, werden den Bienenköniginnen häufig die Flügel gestutzt. Krankheiten werden chemisch behandelt, und in bestimmten Betriebsweisen wird der Verlust ganzer Völker einkalkuliert.

                    Die Honigbiene ist keine gefährdete Wildart, sondern ein in hoher Dichte gehaltenes Nutztier. Die grossen Kolonien entziehen der Umgebung Nektar und Pollen, was zu einer Nahrungskonkurrenz mit Wildbienen führt. Über die Hälfte der Wildbienenarten stehen bereits auf der Roten Liste.

                    Die intensive Bewirtschaftung und globale Transporte schwächen das Immunsystem der Honigbienen. Dadurch können Krankheitserreger beim Anfliegen derselben Blüten auf die ohnehin bedrohten Wildbienenpopulationen übertragen werden.""",
                    List.of()),
            new Article("fische-aquakulturen", "Tiere & Ethik",
                    "Fische & Aquakulturen: Wie industrielle Fischerei funktioniert",
                    """
                    In der industriellen Fischerei und Aquakultur werden Fische primär als Biomasse in Tonnen erfasst und gehandelt. Zur Schlachtung werden Fische in vielen Ländern auf Eis gelegt, wo sie in einem minuten- bis stundenlangen Erstickungsprozess verenden – eine verbreitete Standardpraxis [1].

                    Die Zucht wirtschaftlich lukrativer, fleischfressender Arten wie Lachs erfordert grosse Mengen an Fischmehl und Fischöl als Futter. Dafür müssen weiterhin wildlebende Fische gefangen werden, was die Überfischung der Ozeane weiter antreibt [2].

                    Aquakulturen arbeiten mit hohen Bestandsdichten, was systematischen Parasitenbefall (wie Lachsläuse) nach sich zieht. Die Bekämpfung macht bis zu 10 % der Produktionskosten aus und führt regelmässig zu hohen Ausfallraten [3].""",
                    List.of(
                            "Fair-Fish. (2022). Tierschutz bei der Schlachtung von Fischen.",
                            "FAO. (2024). The State of World Fisheries and Aquaculture 2024.",
                            "Albert Schweitzer Stiftung. (2023). Aquakulturen. https://albert-schweitzer-stiftung.de"
                    )),
            new Article("treibhausgase", "Umwelt & Klima",
                    "Treibhausgase & Emissionen",
                    """
                    Die globale Tierindustrie emittiert jährlich rund 6,2 Gigatonnen CO₂-Äquivalente – das entspricht etwa 12 % der weltweiten, menschengemachten Treibhausgasemissionen. Bezogen auf das gesamte Ernährungssystem entfallen 57 % der Emissionen auf tierische Lebensmittel, während pflanzliche bei 29 % liegen [1].

                    Ein besonders klimarelevanter Faktor ist der Methanausstoss von Wiederkäuern: Methan hat ein rund 30-fach höheres Treibhauspotential als CO₂, weshalb die Rinderhaltung den kurzfristigen Erwärmungspfad besonders stark beeinflusst. Innerhalb der Tierindustrie sind Rinder für über 60 % der Emissionen verantwortlich [2][3].""",
                    List.of(
                            "Xu, X. et al. (2021). Global greenhouse gas emissions from animal-based foods are twice those of plant-based foods. Nature Food, 2, 724–732.",
                            "FAO. (2023). Pathways towards lower emissions. Food and Agriculture Organization of the United Nations.",
                            "IPCC. (2021). Climate Change 2021: The Physical Science Basis. Cambridge University Press."
                    )),
            new Article("flaechenverbrauch", "Umwelt & Klima",
                    "Flächenverbrauch",
                    """
                    Die globale Tierindustrie beansprucht durch Weideflächen und Futtermittelanbau rund 80 % der weltweiten Agrarfläche, liefert im Gegenzug aber nur 18 % der globalen Kalorien und 37 % der Proteine [1]. Dieser grosse Flächenbedarf macht tierische Produkte deutlich flächenintensiver als pflanzliche Proteinquellen [2].

                    Zudem ist die Landnutzungsänderung für den industriellen Futtermittelanbau (insbesondere Soja) ein zentraler Treiber der globalen Entwaldung [3].""",
                    List.of(
                            "Poore, J., & Nemecek, T. (2018). Reducing food's environmental impacts through producers and consumers. Science, 360(6392), 987–992.",
                            "Flachowsky, G., Meyer, U., & Südekum, K.-H. (2018). Resource inputs and land, water and carbon footprints from the production of edible protein of animal origin. Archives Animal Breeding, 61, 17–36.",
                            "Pieper, M., Gaugler, T., & Stoll-Kleemann, S. (2023). Dietary change and land use change. Sustainability Science."
                    )),
            new Article("wasser-fussabdruck", "Umwelt & Klima",
                    "Wasser-Fussabdruck",
                    """
                    Die Tierindustrie beansprucht knapp ein Drittel des gesamten Wasser-Fussabdrucks der globalen Landwirtschaft. Mit durchschnittlich rund 15'400 Litern Wasser pro Kilogramm ist Rindfleisch deutlich wasserintensiver als pflanzliche Proteinquellen wie Hülsenfrüchte (ca. 4'000 l/kg) oder Getreide (ca. 1'600 l/kg) [1].

                    Der grösste Teil dieses Wasserbedarfs entsteht durch den Anbau der Futtermittel, nicht durch das direkte Tränkewasser der Tiere [2].""",
                    List.of(
                            "Mekonnen, M. M., & Hoekstra, A. Y. (2012). A global assessment of the water footprint of farm animal products. Ecosystems, 15(3), 401–415.",
                            "Govoni, C., Chiarelli, D. D., & Rulli, M. C. (2024). A global dataset of the national green and blue water footprint of livestock feeds. Scientific Data, 11, 1419."
                    )),
            new Article("gewaesserbelastung", "Umwelt & Klima",
                    "Gewässerbelastung (Eutrophierung)",
                    """
                    Der Einsatz von Stickstoff- und Phosphordüngern für den Futtermittelanbau sowie die grossflächige Gülleausbringung führen zu Nährstoffüberschüssen in Gewässern [1]. Diese lösen verstärktes Algenwachstum aus. Wenn die Algen absterben und abgebaut werden, wird der gelöste Sauerstoff im Wasser aufgezehrt [2].

                    Die Folge sind sauerstoffarme Zonen in den Ozeanen, in denen viele Meereslebewesen nicht mehr überleben können [3].""",
                    List.of(
                            "Poore, J., & Nemecek, T. (2018). Reducing food's environmental impacts through producers and consumers. Science, 360(6392), 987–992.",
                            "Diaz, R. J., & Rosenberg, R. (2008). Spreading dead zones and consequences for marine ecosystems. Science, 321(5891), 926–929.",
                            "IPCC. (2019). Climate Change and Land: An IPCC Special Report."
                    )),
            new Article("pflanzliche-proteine", "Nährstoffe & Bausteine",
                    "Pflanzliche Proteine und Aminosäurenprofile",
                    """
                    1. Das Gerücht vom unvollständigen Protein

                    Es hält sich hartnäckig das Gerücht, pflanzliches Eiweiss sei "unvollständig". Pflanzliche Lebensmittel enthalten jedoch grundsätzlich alle essenziellen Aminosäuren. Der Unterschied zu tierischen Produkten liegt lediglich darin, dass die Menge einzelner Aminosäuren je nach Pflanze variiert – Getreide hat beispielsweise etwas weniger von der einen Aminosäure, Hülsenfrüchte etwas weniger von einer anderen [1][2].

                    2. Das 24-Stunden-Prinzip

                    Früher dachte man, man müsse verschiedene pflanzliche Proteine in einer einzelnen Mahlzeit kombinieren. Heute weiss die Wissenschaft: Der menschliche Körper besitzt einen Aminosäurenpool, der als Zwischenspeicher dient. Es reicht völlig aus, wenn du über den Tag verteilt verschiedene pflanzliche Proteinquellen isst – wie Brot am Morgen und Linsen am Abend. Der Körper fügt die Bausteine selbst zu vollständigem Protein zusammen [3][4].""",
                    List.of(
                            "Melina, V., Craig, W., & Levin, S. (2016). Position of the Academy of Nutrition and Dietetics: Vegetarian Diets. Journal of the Academy of Nutrition and Dietetics, 116(12), 1970–1980.",
                            "FAO. (2013). Dietary protein quality evaluation in human nutrition. FAO Expert Consultation.",
                            "WHO/FAO/UNU. (2007). Protein and amino acid requirements in human nutrition. WHO Technical Report Series.",
                            "Academy of Nutrition and Dietetics. (2016). Position: Vegetarian Diets. Journal of the Academy of Nutrition and Dietetics, 116(12), 1970–1980."
                    )),
            new Article("vitamin-b12", "Nährstoffe & Bausteine",
                    "Die Wahrheit über Vitamin B12 (Cobalamin)",
                    """
                    1. Der Ursprung: Bakterien, nicht Tiere

                    Ein weit verbreiteter Irrtum ist, dass Vitamin B12 von Tieren produziert wird. Tatsächlich können weder Tiere noch Pflanzen dieses Vitamin selbst herstellen. B12 wird in der Natur ausschliesslich von speziellen Bakterien im Boden produziert, die dafür das Spurenelement Cobalt benötigen [1]. Auch das B12 für den weltweiten Markt wird heute industriell mithilfe dieser Bakterienkulturen hergestellt [2].

                    2. Warum steckt B12 dann in Fleisch und Milch?

                    In der modernen Tierhaltung haben die Tiere kaum noch Kontakt zu naturbelassenen Böden. Deshalb wird dem Tierfutter routinemässig Vitamin B12 oder das Spurenelement Cobalt als Supplement zugesetzt [3]. Das bedeutet: Auch wer B12 über tierische Produkte aufnimmt, konsumiert letztlich ein industriell hergestelltes Supplement – nur eben über den Umweg des Tieres. Bei einer pflanzlichen Ernährung nimmst du dasselbe B12 einfach direkt ein [1].""",
                    List.of(
                            "Watanabe, F., Yabuta, Y., Tanioka, Y., & Bito, T. (2013). Vitamin B12-containing plant food sources for vegetarians. Nutrients, 5(5), 1861–1873.",
                            "Calvillo, Á., Pellicer, T., Carnicer, M., & Planas, A. (2022). Bioprocess strategies for vitamin B12 production by microbial fermentation. Bioengineering, 9(8), 365.",
                            "EFSA FEEDAP Panel. (2015). Scientific opinion on the safety and efficacy of vitamin B12 as a feed additive. EFSA Journal, 13(7), 4170."
                    )),
            new Article("pflanzliches-eisen", "Nährstoffe & Bausteine",
                    "Pflanzliches Eisen und Bioverfügbarkeit",
                    """
                    1. Hämeisen vs. Nicht-Hämeisen

                    Tierische Produkte liefern sogenanntes Hämeisen, das der Körper zu etwa 14–18 % aufnimmt. Pflanzliche Lebensmittel liefern Nicht-Hämeisen mit einer etwas niedrigeren Aufnahmerate von ca. 5–12 %. Der Grund: Natürliche Pflanzenstoffe wie Phytinsäure binden das Eisen im Dünndarm teilweise und reduzieren so die Aufnahme [1][2].

                    2. Der Vitamin-C-Trick

                    Die gleichzeitige Aufnahme von Vitamin C neutralisiert die hemmende Wirkung der Phytinsäure und kann die Eisenaufnahme mehr als verdoppeln. In der Praxis heisst das: ein Glas Orangensaft zu den Haferflocken oder etwas Paprika zu den Linsen [3][4].""",
                    List.of(
                            "Hurrell, R., & Egli, I. (2010). Iron bioavailability and dietary reference values. American Journal of Clinical Nutrition, 91(5), 1461S–1467S.",
                            "Winichagoon, P. (2017). Iron bioavailability. Journal of Nutrition Association of Thailand, 52(1), 23–32.",
                            "Cook, J. D., & Reddy, M. B. (2001). Effect of ascorbic acid intake on nonheme-iron absorption. American Journal of Clinical Nutrition, 73(1), 93–98.",
                            "Tetens, I. et al. (2005). Vitamin C and iron bioavailability."
                    )),
            new Article("omega-3", "Nährstoffe & Bausteine",
                    "Die Wahrheit über Omega-3 (EPA/DHA) und Fische",
                    """
                    1. Die wahren Produzenten: Algen

                    Fische können die langkettigen Omega-3-Fettsäuren EPA und DHA nicht selbst herstellen. Die eigentlichen Produzenten sind marine Mikroalgen (wie Schizochytrium). Fische nehmen das Omega-3 über ihre Nahrung auf und speichern es im Gewebe [1].

                    2. Algenöl als direkte Quelle

                    Mikroalgenöl liefert dem menschlichen Körper die gleiche Bioverfügbarkeit an EPA und DHA wie Fischöl [2]. Wer Omega-3 direkt aus der Alge bezieht, umgeht die gesamte Fisch-Lieferkette – und damit die ökologischen Auswirkungen der industriellen Fischerei [3].""",
                    List.of(
                            "Ebm, N. et al. (2021). Polyunsaturated fatty acids in fish tissues more closely resemble algal than terrestrial diet sources. Hydrobiologia, 848, 371–383.",
                            "Stiefvatter, L. et al. (2021). Oral bioavailability of omega-3 fatty acids and carotenoids from the microalgae Phaeodactylum tricornutum. Marine Drugs, 19(12), 700.",
                            "Schade, S., Stangl, G. I., & Meier, T. (2020). Distinct microalgae species for food—Part 2: Life cycle assessment. Journal of Applied Phycology, 32, 2997–3013."
                    )),
            new Article("b12-protokoll", "Supplemente",
                    "Das B12-Protokoll: Dosierung und Umsetzung",
                    """
                    1. Der Flaschenhals im Körper

                    Der Körper benötigt etwa 4 µg Vitamin B12 pro Tag. Das Problem: Der Darm lässt pro Mahlzeit maximal 1,5 bis 2,0 µg durch [1][2]. Bei einer pflanzlichen Ernährung, bei der nicht über den ganzen Tag verteilt kleine Mengen B12 aufgenommen werden, muss dieser Engpass berücksichtigt werden.

                    2. Die 1-Prozent-Regel

                    Bei einer hohen Einzeldosis drückt sich etwa 1 % automatisch durch die Darmwand – unabhängig vom normalen Aufnahmemechanismus [3]. Das erklärt die hohen Angaben auf Supplementen: Bei einer täglichen Tablette mit 250 µg nimmt der Körper etwa 2,5 µg auf – genug, um den Tagesbedarf zu decken. Als bewährteste Form gilt Cyanocobalamin [4].""",
                    List.of(
                            "EFSA. (2015). Scientific opinion on vitamin B12 as a feed additive. EFSA Journal, 13(7), 4170.",
                            "WHO. (2004). Vitamin and mineral requirements in human nutrition. WHO/FAO.",
                            "Watanabe, F. (2007). Vitamin B12 sources and bioavailability. Experimental Biology and Medicine, 232(10), 1266–1274.",
                            "Melina, V., Craig, W., & Levin, S. (2016). Position of the Academy of Nutrition and Dietetics: Vegetarian Diets. Journal of the Academy of Nutrition and Dietetics, 116(12), 1970–1980."
                    )),
            new Article("omega-3-direktversorgung", "Supplemente",
                    "Die Omega-3-Direktversorgung: Warum ALA oft nicht reicht",
                    """
                    1. Die Verlustrate

                    Pflanzliche Quellen wie Leinsamen oder Walnüsse liefern Omega-3 nur als Vorstufe (ALA). Der Körper muss diese erst in die benötigten Formen EPA und DHA umwandeln – ein Prozess mit hohen Verlusten: Von ALA werden oft weniger als 10 % in EPA und unter 1 % in DHA umgewandelt [1][2].

                    2. Algenöl als Lösung

                    Algenöl liefert EPA und DHA bereits in fertiger Form, sodass der ineffiziente Umwandlungsprozess umgangen wird. Mit einer täglichen Dosis von 250 bis 500 mg Algenöl ist die Versorgung direkt und verlustfrei sichergestellt [2].""",
                    List.of(
                            "Burdge, G. C., & Calder, P. C. (2005). Conversion of α-linolenic acid to longer-chain polyunsaturated fatty acids. Reproduction, Nutrition, Development, 45(5), 581–597.",
                            "Brenna, J. T. et al. (2009). Alpha-Linolenic acid supplementation and conversion to n-3 long-chain polyunsaturated fatty acids. Prostaglandins, Leukotrienes and Essential Fatty Acids, 80(2-3), 85–91."
                    )),
            new Article("eisen-zink-management", "Supplemente",
                    "Eisen- & Zink-Management",
                    """
                    1. Lebensmittel vor Tabletten

                    Im Gegensatz zu Vitamin B12 solltest du Eisen und Zink nicht vorsorglich als Nahrungsergänzungsmittel einnehmen. Eine Überdosierung kann gesundheitsschädlich sein. Bei einer ausgewogenen pflanzlichen Ernährung mit dem Vitamin-C-Trick (z.B. Zitronensaft zu Linsen) reicht die natürliche Versorgung in der Regel aus [1].

                    2. Die Blutbild-Regel

                    Hochdosierte Eisen- oder Zinkpräparate sind nur bei einem ärztlich festgestellten Mangel sinnvoll. Dafür müssen spezifische Speicherwerte (wie der Serum-Ferritin-Spiegel) gemessen werden. Erst bei nachweislich leeren Speichern sollte in Absprache mit einem Arzt gezielt supplementiert werden [2].""",
                    List.of(
                            "Schümann, K. et al. (2014). Risiken und Nutzen der Eisensupplementation. Perspectives in Medicine, 2(1–4), 19–39.",
                            "Maret, W., & Sandstead, H. H. (2014). Zinkbedarf und Risiko und Nutzen einer Zinksupplementierung. Perspectives in Medicine, 2(1–4), 3–18."
                    )),
            new Article("geografische-mangelstoffe", "Supplemente",
                    "Geografische Mangelstoffe: Kein reines Vegan-Problem",
                    """
                    1. Jod & Selen

                    Europäische Böden sind von Natur aus arm an Jod und Selen. Um Mangelerscheinungen in der Bevölkerung vorzubeugen, werden diese Spurenelemente dem Tierfutter standardmässig beigemischt [1]. Wer tierische Produkte konsumiert, nimmt diese Nährstoffe also nicht "natürlich" auf, sondern indirekt über das angereicherte Futter. Ein direktes pflanzliches Supplement ist eine ebenso wirksame und oft präzisere Option [2].

                    2. Vitamin D3

                    In europäischen Breitengraden reicht die Sonneneinstrahlung von Oktober bis März nicht aus, um die körpereigene Vitamin-D-Synthese zu aktivieren [2]. Dieser Mangel betrifft den Grossteil der Bevölkerung – unabhängig von der Ernährungsform. Da Vitamin D kaum in Lebensmitteln vorkommt, wird eine Supplementierung in den Wintermonaten für alle Menschen empfohlen [1].""",
                    List.of(
                            "BfR – Bundesinstitut für Risikobewertung. (2025). Bewertung von Vitaminen und Mineralstoffen in Lebensmitteln.",
                            "DGE – Deutsche Gesellschaft für Ernährung. (2025). Referenzwerte für die Nährstoffzufuhr."
                    )),
            new Article("praevention-cholesterin", "Gesundheit & Lebensphasen",
                    "Prävention: Cholesterin und Ballaststoffe",
                    """
                    1. Plaque-Bildung (Atherosklerose)

                    Nahrungscholesterin kommt ausschliesslich in tierischen Produkten vor. Gesättigte tierische Fette fördern die körpereigene Produktion von LDL-Cholesterin, was langfristig zur Plaque-Bildung in den Blutgefässen beitragen kann. Dieser Prozess verengt die Gefässe und erhöht das Risiko für Herz-Kreislauf-Erkrankungen [1].

                    2. Insulinresistenz (Typ-2-Diabetes)

                    Tierische Fette können dazu beitragen, dass sich Fettpartikel in den Muskelzellen ablagern und dort die Insulinrezeptoren blockieren. Dies ist ein biochemischer Hauptfaktor für Typ-2-Diabetes. Eine fettärmere, pflanzenbasierte Ernährung kann diese zellulären Fettspeicher reduzieren und die Insulinsensitivität verbessern [2].

                    3. Die Rolle der Ballaststoffe

                    Pflanzliche Ballaststoffe – die in Fleisch, Milch und Eiern nicht vorkommen – binden im Darm Gallensäure, was die Leber dazu anregt, überschüssiges LDL-Cholesterin aus dem Blut abzubauen. Zusätzlich verlangsamen Ballaststoffe die Glukoseaufnahme und können systemische Entzündungswerte senken [3].""",
                    List.of(
                            "American Heart Association. (2021). Saturated fat. https://www.heart.org",
                            "American Diabetes Association. (2022). Standards of Medical Care in Diabetes—2022.",
                            "Harvard T.H. Chan School of Public Health. (2021). Fiber. https://www.hsph.harvard.edu"
                    )),
            new Article("sport-regeneration", "Gesundheit & Lebensphasen",
                    "Sport & Regeneration: Muskelaufbau",
                    """
                    1. Protein ist Protein

                    Für den Muskelaufbau ist nicht die Quelle des Proteins entscheidend, sondern die Gesamtmenge und das Vorhandensein der Aminosäure Leucin – sie startet den Aufbauprozess im Muskel. Hochwertige pflanzliche Quellen wie Soja- oder Erbsenprotein lösen diesen Reiz vergleichbar aus. Entscheidend ist die tägliche Gesamtproteinmenge, nicht die Quelle [1][2].

                    2. Regeneration durch Antioxidantien

                    Intensives Training verursacht Mikrorisse in den Muskelfasern und oxidativen Stress. Eine pflanzliche Ernährung liefert eine hohe Dichte an Antioxidantien, die freie Radikale neutralisieren und die Entzündungsreaktion nach dem Training dämpfen können. Das kann die Regenerationszeit positiv beeinflussen [1][3].""",
                    List.of(
                            "JISSN. (2017). The short-term effect of high versus moderate protein intake on recovery. Journal of the International Society of Sports Nutrition, 14, Artikel 44.",
                            "McGlory, C., Devries, M. C., & Phillips, S. M. (2017). Skeletal muscle and resistance exercise training. Journal of Applied Physiology, 122(3), 541–548.",
                            "Gehlert, S. et al. (2020). Acute and chronic resistance exercise differentially modulates the skeletal muscle metabolome. Medicine & Science in Sports & Exercise, 52(7S), 508."
                    )),
            new Article("schwangerschaft", "Gesundheit & Lebensphasen",
                    "Schwangerschaft & Stillzeit",
                    """
                    1. Grundsätzlich machbar

                    Eine gut geplante pflanzliche Ernährung wird von grossen Fachgesellschaften wie der Academy of Nutrition and Dietetics auch für die Schwangerschaft und Stillzeit als geeignet eingestuft [1]. Entscheidend ist dabei das Wort "geplant": Es reicht nicht aus, tierische Produkte einfach wegzulassen – die gezielte Versorgung mit kritischen Nährstoffen muss sichergestellt werden.

                    2. Kritische Nährstoffe

                    Während der Schwangerschaft steigt der Bedarf an Vitamin B12, DHA (für die Gehirnentwicklung des Kindes) und Eisen deutlich an [2]. Eine konsequente Supplementierung (insbesondere B12 und DHA) sowie regelmässige ärztliche Kontrolle der Blutwerte sind in dieser Phase besonders wichtig [3].""",
                    List.of(
                            "Academy of Nutrition and Dietetics. (2016). Position: Vegetarian Diets. Journal of the Academy of Nutrition and Dietetics, 116(12), 1970–1980.",
                            "EFSA. (2015). Scientific opinion on dietary reference values for iron. EFSA Journal, 13(10), 4254.",
                            "WHO. (2012). Guideline: Daily iron and folic acid supplementation in pregnant women."
                    )),
            new Article("kindheit-wachstum", "Gesundheit & Lebensphasen",
                    "Kindheit & Wachstum: Nährstoffdichte",
                    """
                    1. Das Volumen-Thema

                    Kleinkinder haben ein begrenztes Magenvolumen, brauchen aber viel Energie für ihr Wachstum. Pflanzliche Mahlzeiten bestehen oft aus viel Wasser und Ballaststoffen – das füllt den Magen, bevor genug Kalorien aufgenommen wurden [1].

                    2. Kaloriendichte erhöhen

                    Um ein gesundes Wachstum sicherzustellen, können Mahlzeiten gezielt mit kalorienreichen Zutaten wie Nussmus, Avocados oder hochwertigen Pflanzenölen angereichert werden. Gleichzeitig sollten sehr ballaststoffreiche Lebensmittel in grossen Mengen (wie rohes Gemüse) etwas reduziert werden, damit das Kind genügend Energie aufnehmen kann [2].""",
                    List.of(
                            "Kim, K. et al. (2021). Nutritional adequacy and diet quality are associated with standardized height-for-age among U.S. children. Nutrients, 13(5), 1689.",
                            "Burgard, L. et al. (2024). Unfavorable nutrient intakes in children up to school entry age. Frontiers in Nutrition, 10, 1302323."
                    )),
            new Article("kosmetik-tierversuche", "Konsumgüter & Textilien",
                    "Kosmetik & Tierversuche: Inhaltsstoffe und Gesetzeslücken",
                    """
                    1. Tierversuchsfrei ≠ Vegan

                    Viele kosmetische Grundstoffe (wie Glycerin, Squalen oder Keratin) werden aus wirtschaftlichen Gründen aus tierischen Schlachtabfällen gewonnen. Das Label "tierversuchsfrei" bedeutet lediglich, dass das fertige Produkt nicht an Tieren getestet wurde – über die Inhaltsstoffe sagt es nichts aus. Ein tierversuchsfreies Shampoo kann also trotzdem tierische Fette enthalten [1].

                    2. Die Gesetzeslücke

                    Tierversuche für fertige Kosmetika sind in Europa verboten. Jedoch schreibt die EU-Chemikalienverordnung (REACH) für neue Rohstoffe teils Tierversuche vor. Um diese Lücke zu umgehen, bieten unabhängige Zertifikate wie die "Vegan-Blume" eine verlässliche Orientierung – sie garantieren einen komplett pflanzlichen Prozess [2].""",
                    List.of(
                            "Verordnung (EG) Nr. 1223/2009 des Europäischen Parlaments über kosmetische Mittel.",
                            "ECHA. (2024). Understanding REACH. https://echa.europa.eu"
                    )),
            new Article("haushalt-seifen", "Konsumgüter & Textilien",
                    "Haushalt & Seifen",
                    """
                    1. Rindertalg in Seifen

                    In vielen konventionellen Seifen und Waschmitteln wird Rindertalg als Basis verwendet – auf der Zutatenliste als "Sodium Tallowate" deklariert. Der Grund ist wirtschaftlich: Rindertalg ist eines der günstigsten Nebenprodukte der Fleischindustrie [1].

                    2. Pflanzliche Alternativen

                    Pflanzliche Tenside auf Basis von Kokosöl (Sodium Cocoate) oder Olivenöl (Sodium Olivate) bieten die gleiche Reinigungsleistung. Mit einem Blick auf die Inhaltsstoffe lässt sich der Haushalt vollständig von tierischen Nebenprodukten trennen, ohne Kompromisse bei der Hygiene [2].""",
                    List.of(
                            "RCT-online. (2024). Synthetische Tenside. https://www.rct-online.de",
                            "Chemie-zeitschrift.at. (2025). Tenside. https://www.chemie-zeitschrift.at/lexikon/tenside/"
                    )),
            new Article("leder", "Konsumgüter & Textilien",
                    "Leder: Nebenprodukt oder Profitfaktor?",
                    """
                    1. Die Ökonomie

                    Leder ist kein reines Abfallprodukt der Fleischindustrie. Die tierische Haut macht bis zu 70 % der Produktionskosten einer Gerberei aus und ist damit einer der wertvollsten Rohstoffe der gesamten Fleischkette [1]. Der Kauf von Leder finanziert die Fleischproduktion also mit [2].

                    2. Die Chemie

                    Um die natürliche Verwesung der Haut zu stoppen, verwenden 80 bis 90 % der Gerbereien weltweit giftige Chrom-Salze. Dieser Prozess führt zu erheblichen Schwermetallbelastungen in den Abwässern. Das Endprodukt Leder ist ein chemisch stark bearbeitetes Industriegut [1][2].""",
                    List.of(
                            "Umweltbundesamt. (o.J.). Lederindustrie – Stand der Technik und Umweltauswirkungen.",
                            "Ferraris, S. et al. (2025). Comparative characterization of leather from different tanning processes. Scientific Reports, 15(1)."
                    )),
            new Article("wolle-daunen-pelz", "Konsumgüter & Textilien",
                    "Wolle, Daunen & Pelz",
                    """
                    1. Industrielle Standardverfahren

                    In der globalen Textilindustrie sind physische Eingriffe am Tier etablierte Praxis. Bei Merinowolle ist das "Mulesing" – das Herausschneiden von Hautfalten ohne Betäubung – in grossen Produktionsländern wie Australien ein Standard [1]. Auch bei Daunen sind zyklische Rupfprozesse in die Produktionsabläufe integriert, während Pelze überwiegend aus Käfigsystemen stammen [2].

                    2. Pflanzliche und synthetische Alternativen

                    Die Kernfunktionen dieser tierischen Textilien – Wärmeisolation und Atmungsaktivität – werden heute von pflanzlichen Cellulosefasern (wie Lyocell oder Kapok) und recycelten Synthetikisolierungen (wie PrimaLoft) gleichwertig erfüllt [2].""",
                    List.of(
                            "Sneddon, J., & Rollin, B. (2010). Mulesing and Animal Ethics. Journal of Agricultural and Environmental Ethics, 23, 371–386.",
                            "FOUR PAWS. (2023). Taming fashion: A review of the negative effects of fur, wool, down and leather."
                    )),
            new Article("soziale-dynamik", "Lifestyle & Alltag",
                    "Soziale Dynamik in der Schweiz",
                    """
                    1. Die Schweizer Infrastruktur

                    Die Schweiz verfügt über eine gute pflanzliche Infrastruktur. Eine vegane Option gehört in der Schweizer Gastronomie mittlerweile weitgehend zum Standard – auch in traditionellen Gasthöfen oder Restaurantketten [1]. Die Herausforderung bei Einladungen liegt heute weniger an fehlenden Optionen, sondern eher an der Kommunikation innerhalb der Gruppe [2].

                    2. Die proaktive Strategie

                    Um Diskussionen am Tisch zu vermeiden, hilft es, frühzeitig Restaurants vorzuschlagen, die sowohl klassische als auch pflanzliche Optionen anbieten. So müssen keine Sonderwünsche verhandelt werden und die eigene Ernährungsweise wird für das Umfeld unauffällig [3].""",
                    List.of(
                            "GastroSuisse. (2025). Gastrospiegel 2025.",
                            "Swissveg. (2024). Swissveg-Report 2024.",
                            "Swissveg. (2025). Swissveg-Analyse 2025."
                    )),
            new Article("kostenstruktur", "Lifestyle & Alltag",
                    "Die Kostenstruktur: Was vegane Ernährung wirklich kostet",
                    """
                    1. Die günstigen Basics

                    Pflanzliche Grundnahrungsmittel wie Linsen, Bohnen, Reis, Hafer und Kartoffeln gehören weltweit zu den günstigsten Kalorien- und Proteinquellen. In der Schweiz kostet ein Kilogramm Reis oder Pasta durchschnittlich etwa 3 Franken – Rindfleisch dagegen 40 bis 60 Franken pro Kilogramm [1]. Studien zeigen, dass eine Ernährung auf Basis dieser Grundnahrungsmittel die Lebensmittelkosten spürbar senken kann [2].

                    2. Die Premiumprodukte

                    Vegane Ersatzprodukte wie Burger-Patties oder Käse-Alternativen sind oft teurer – das liegt an den höheren Entwicklungskosten und kleineren Produktionsmengen. Das bedeutet aber nicht, dass vegane Ernährung generell teuer ist. Wie bei jeder Ernährungsform bestimmst du selbst, wie viel du ausgibst: Wer hauptsächlich auf die günstigen Basics setzt und Ersatzprodukte als gelegentliches Extra behandelt, kann sehr günstig vegan leben.""",
                    List.of(
                            "Stadt Bern. (2025). Konsumentenpreise für Nahrungsmittel 2024.",
                            "Springmann, M. et al. (2021). The global and regional costs of healthy and sustainable dietary patterns. The Lancet Planetary Health, 5(11), e797–e807."
                    )),
            new Article("supermarkt-navigation", "Lifestyle & Alltag",
                    "Supermarkt-Navigation: Der Scanner-Blick",
                    """
                    1. Die Allergen-Kennzeichnung

                    Die Schweizer Lebensmittelverordnung (LIV) verpflichtet Hersteller, Hauptallergene – darunter Milch, Eier, Fisch – in der Zutatenliste hervorzuheben, meist durch Fettdruck oder Grossbuchstaben [1]. Ein schneller Blick auf diese hervorgehobenen Begriffe genügt, um die meisten nicht-veganen Produkte in Sekunden zu erkennen, ohne die gesamte Liste lesen zu müssen [2].

                    2. Das V-Label

                    Technische Hilfsstoffe (wie Klärmittel) müssen auf der Verpackung nicht immer deklariert werden. Das V-Label schliesst diese Lücke: Es zertifiziert, dass in keinem Produktionsschritt tierische Substanzen eingesetzt wurden – auch keine, die nicht im Zutatenverzeichnis erscheinen [3].""",
                    List.of(
                            "Eidgenössisches Departement des Innern (EDI). (2016). Verordnung betreffend die Information über Lebensmittel (LIV).",
                            "Dienststelle für Verbraucherschutz Wallis. (2023). Allergenkennzeichnung bei Lebensmitteln.",
                            "V-Label GmbH. (2019). V-Label Zertifizierungsrichtlinien. https://www.v-label.com"
                    )),
            new Article("reisen-unterwegs", "Lifestyle & Alltag",
                    "Reisen & Unterwegs: Vegane Versorgung weltweit",
                    """
                    1. Globale Infrastruktur

                    Der vegane Reisesektor wird für 2025 auf ein Marktvolumen von rund 78 Milliarden US-Dollar geschätzt [1]. Regionen mit traditionell pflanzenbasierter Küche (wie Teile Indiens) sowie urbane Zentren in Europa bieten heute eine dichte pflanzliche Infrastruktur.

                    2. Das Puffer-System

                    In Transit-Zonen wie Flughäfen oder auf Langstreckenflügen kann die Auswahl begrenzt sein [2]. Es hilft, energiedichte, haltbare Snacks (wie Nüsse oder Riegel) im Handgepäck mitzuführen, um unabhängig von der lokalen Versorgung zu bleiben.""",
                    List.of(
                            "TravelPulse. (2025). The rise of vegan travel. TravelPulse.",
                            "Magraw, P. (2023). Why vegan meal options are the next big thing for the onboard industry. Airport-Suppliers."
                    )),
            new Article("protein-dichte", "Mythen-Check",
                    "Die Protein-Dichte: Sojagranulat vs. Rindfleisch",
                    """
                    1. Proteindichte im Vergleich

                    Nährwertdatenbanken zeigen, dass einige pflanzliche Proteinquellen tierische Produkte bei der reinen Proteindichte übertreffen: Während Rindfleisch etwa 25 g und Pouletbrust rund 23 g Protein pro 100 g liefern, erreicht texturiertes Sojagranulat 50 g und Seitan bis zu 80 g pro 100 g. Auch Kürbiskerne (ca. 30 g) liegen über Hühnereiern (ca. 13 g) [1][2].

                    2. Proteinqualität

                    Moderne Analysen zeigen, dass verarbeitetes Soja (wie Tofu oder Granulat) eine Proteinqualität aufweist, die mit der von Pouletbrust vergleichbar ist [3]. Durch den Konsum verschiedener pflanzlicher Quellen über den Tag verteilt wird ein vollständiges Aminosäurenprofil erreicht.""",
                    List.of(
                            "BLV. (2024). Schweizer Nährwertdatenbank. https://www.naehrwertdaten.ch",
                            "USDA. (2023). FoodData Central. https://fdc.nal.usda.gov",
                            "Hammer, L. et al. (2025). Influence of processing on protein quality of soy-based meat analogues. Food Research International, 222(1), 117636."
                    )),
            new Article("soja-hormon-mythos", "Mythen-Check",
                    "Der Soja-Hormon-Mythos",
                    """
                    1. Phytoöstrogene und Testosteron

                    Phytoöstrogene aus Soja haben zwar eine strukturelle Ähnlichkeit zu menschlichem Östrogen, binden aber nur schwach an menschliche Rezeptoren. Grosse Meta-Analysen belegen: Auch bei hohem Sojakonsum gibt es bei Männern keine messbare Veränderung des Testosteronspiegels [1][2].

                    2. Hormone in Kuhmilch

                    Kuhmilch enthält echte, biologisch aktive Säugetier-Hormone (Östrogene und Progesteron), da die Milch von trächtigen oder frisch kalbenden Kühen stammt. Diese Hormone sind mit menschlichen Hormonen nahezu identisch und wirken direkt auf das menschliche Hormonsystem. Aus biochemischer Sicht sind die echten Hormone in Milch also deutlich potenter als die pflanzlichen Phytoöstrogene in Soja [3].""",
                    List.of(
                            "Viscardi, S. et al. (2025). Soy isoflavone intake and testosterone levels in men: A systematic review and meta-analysis. Frontiers in Nutrition, 12, 1435927.",
                            "Ahlin, S. et al. (2023). Phytoestrogen exposure and reproductive health outcomes. Reproductive Toxicology, 115, 108–117.",
                            "Domínguez-López, I. et al. (2020). Effects of dietary phytoestrogens on hormones throughout a human lifespan. Nutrients, 12(8), 2456."
                    )),
            new Article("kalzium-thema", "Mythen-Check",
                    "Das Kalzium-Thema: Milch vs. Pflanzen",
                    """
                    1. Bioverfügbarkeit

                    Entscheidend für die Kalziumversorgung ist nicht nur die aufgenommene Menge, sondern wie viel der Körper davon tatsächlich aufnimmt. Aus Kuhmilch werden typischerweise rund 30 % des Kalziums resorbiert. Bei grünem Gemüse wie Brokkoli oder Grünkohl liegt die Rate bei 50 bis 60 %. Auch kalziumreiches Mineralwasser ist eine effiziente und kalorienfreie Quelle [1][2].

                    2. Das Gesamtsystem

                    Kalzium allein reicht für starke Knochen nicht aus. Damit es in die Knochenmatrix eingebaut wird, braucht der Körper Vitamin D3 für die Aufnahme im Darm und Vitamin K2 für die Einlagerung. Ohne diese Kofaktoren und ohne mechanische Belastung (wie Krafttraining) kann selbst eine hohe Kalziumzufuhr wirkungslos bleiben [3].""",
                    List.of(
                            "Burckhardt, P. (2008). The effect of the alkali load of mineral water on bone metabolism. Journal of Nutrition, 138(2), 435S–437S.",
                            "Burckhardt, P. (2015). Calcium revisited, part III. BoneKEy Reports, 4, Artikel 708.",
                            "Vegane Ernährung aus osteologischer und endokrinologischer Sicht. (2024). Osteologie."
                    )),
            new Article("steinzeit-mythos", "Mythen-Check",
                    "Der Steinzeit-Mythos",
                    """
                    1. Jagd vs. moderner Supermarkt

                    In der Frühgeschichte war Fleisch eine unter grossem Aufwand erjagte Überlebensressource. Heute stammt tierisches Protein aus einem hochindustrialisierten System, das keinen Bezug mehr zu natürlichen Ökosystemen hat [1]. In modernen Supermärkten mit unbegrenztem Zugang zu pflanzlichen Lebensmitteln ist das historische Überlebensargument für Fleisch nicht mehr relevant.

                    2. Das Glukose-Fundament

                    Evolutionsbiologische Forschung zeigt, dass die Entwicklung des menschlichen Gehirns nicht primär auf Fleisch, sondern auf der Verfügbarkeit von Glukose basierte – ermöglicht durch das Kochen stärkehaltiger Knollen [2]. Die hohe Kopienzahl des Speichel-Amylase-Gens (AMY1) beim Menschen zeigt zudem, dass unser Organismus besonders gut auf die Verdauung von pflanzlicher Stärke ausgelegt ist [3].""",
                    List.of(
                            "Langthaler, E. (2016). Tiere mästen und essen: Die Fabrikation des globalen Fleisch-Komplexes. Jahrbuch für Geschichte des ländlichen Raumes, 13, 11–36.",
                            "Hardy, K. et al. (2015). The importance of dietary carbohydrate in human evolution. The Quarterly Review of Biology, 90(3), 251–268.",
                            "Perry, G. H. et al. (2007). Diet and the evolution of human amylase gene copy number variation. Nature Genetics, 39(10), 1256–1260."
                    )),
            new Article("chemie-mythos", "Mythen-Check",
                    "Der Chemie-Mythos: Pflanzliche Produkte vs. Fleischverarbeitung",
                    """
                    1. Wie pflanzliche Alternativen hergestellt werden

                    Traditionelle pflanzliche Proteine wie Tofu oder Tempeh basieren auf jahrtausendealten Verfahren wie Fermentation und Gerinnung. Auch moderne Ersatzprodukte bestehen oft aus wenigen natürlichen Zutaten wie Erbsenprotein, Wasser und Öl. Ihre fleischähnliche Textur entsteht durch Extrusion – einen physikalisch-mechanischen Prozess mit Druck und Hitze, ohne chemische Modifikation der Aminosäuren [1][2].

                    2. Chemie in der Fleischverarbeitung

                    Die industrielle Verarbeitung von Fleischprodukten wie Schinken oder Wurst verwendet standardmässig Nitritpökelsalze. Diese können bei Erhitzung toxische Verbindungen (N-Nitrosamine) bilden. Die Weltgesundheitsorganisation (WHO/IARC) hat verarbeitetes Fleisch deshalb als Karzinogen der Gruppe 1 eingestuft – in derselben Kategorie wie Asbest oder Tabakrauch [3].""",
                    List.of(
                            "Li, R., Raak, N., & Roman, L. (2025). Recent advances in processing-induced changes in food proteins. Foods, 14(5), 764.",
                            "Hammer, L. et al. (2024). Influence of processing on protein quality of soy-based meat analogues. Food Research International.",
                            "Bouvard, V. et al. (2015). Carcinogenicity of consumption of red and processed meat. The Lancet Oncology, 16(16), 1599–1600."
                    ))
    );

    private InfoArticles() {}
}
