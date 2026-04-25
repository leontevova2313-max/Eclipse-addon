# Eclipse v16 build notes

Target:

```text
Minecraft 1.21.11
Meteor 1.21.11-SNAPSHOT
Fabric Loader 0.18.2
Java 21
Gradle wrapper 9.2.0
```

New source files:

```text
src/main/java/eclipse/modules/movement/AutoFireworks.java
src/main/java/eclipse/modules/utility/InventoryPresets.java
src/main/java/eclipse/modules/utility/Profiles.java
src/main/java/eclipse/client/inventory/InventoryPresetStore.java
src/main/java/eclipse/client/profiles/ProfileStore.java
```

Registration changed in:

```text
src/main/java/eclipse/Eclipse.java
```

Expected local build command:

```bash
./gradlew clean build
```

In this execution environment the Gradle wrapper cannot download `gradle-9.2.0-bin.zip` because DNS/network access to `services.gradle.org` is unavailable. The project is prepared as source and should be built in a normal environment with internet access or a preinstalled Gradle 9.2.0 wrapper distribution.
