# nexo-plus

Addons für [Nexo](https://nexomc.com) auf Paper 26.3. Jedes Addon ist ein eigenes Gradle-Projekt mit eigenem Wrapper (Java 25).

| Ordner | Was es macht |
|---|---|
| `NexoCoreProtect` | Protokolliert Setzen und Abbauen von Nexo-Möbeln in CoreProtect, sichtbar in `/co lookup` und `/co inspect`. Ein Rollback lässt Möbel unverändert. |
| `NexoWaterCrops` | Pflanzen im Wasser als reine Blöcke auf Seetang-Zuständen, ohne Entities (Reis). |

Bauen: im Ordner des Addons `./gradlew build`, die Jar liegt dann unter `build/libs/`.

`NexoCoreProtect` braucht zum Kompilieren `libs/CoreProtect-25.1.jar`, die nicht im Repo liegt. Das Addon nutzt interne Teile von CoreProtect und ist nur mit Version 25.1 geprüft.
