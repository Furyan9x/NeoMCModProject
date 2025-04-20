# DEVOXXGENIE.md

## Project Guidelines

### Build Commands

- **Build:** `./gradlew build`
- **Test:** `./gradlew test`
- **Single Test:** `./gradlew test --tests ClassName.methodName`
- **Clean:** `./gradlew clean`
- **Run:** `./gradlew run`

### Code Style

- **Formatting:** Use IDE or checkstyle for formatting
- **Naming:**
  - Use camelCase for variables, methods, and fields
  - Use PascalCase for classes and interfaces
  - Use SCREAMING_SNAKE_CASE for constants
- **Documentation:** Use JavaDoc for documentation
- **Imports:** Organize imports and avoid wildcard imports
- **Exception Handling:** Prefer specific exceptions and document throws

### General Rules 
- **Keep it simple:** Keep your code simple and easy to understand
- **Keep it modular:** Keep your code modular and easy to extend
- **Stay on Track:** Adhere to Neoforge Mod Loader rules and best practices for coding Java mods in Minecraft verison 1.21.1
- 


### Project Tree

```
ProjectRiyapos-1.21.1/
  run/
    logs/
      telemetry/
    mods/
    saves/
      New World/
        poi/
        data/
          riyaposmod/
            vanilla_weights.json
            all_vanilla_weights.json
        DIM1/
          data/
        DIM-1/
          data/
        stats/
          380df991-f603-344c-a090-369bad2a924a.json
        region/
        entities/
        datapacks/
          weights/
            data/
              riyaposmod/
                weights/
                  vanilla_weights.json
                  all_vanilla_weights.json
        playerdata/
        advancements/
          380df991-f603-344c-a090-369bad2a924a.json
        serverconfig/
          readme.txt
    config/
      jei/
        world/
          local/
            New_World/
        blacklist.json
    downloads/
      log.json
    options.txt
    crash-reports/
      crash-2025-03-31_14.45.17-fml.txt
      crash-2025-03-31_18.16.31-fml.txt
      crash-2025-03-31_18.21.54-fml.txt
      crash-2025-03-31_18.30.27-fml.txt
      crash-2025-04-04_14.34.33-fml.txt
      crash-2025-04-06_00.27.30-fml.txt
      crash-2025-04-06_00.28.33-fml.txt
      crash-2025-04-06_14.47.32-fml.txt
      crash-2025-04-06_15.06.02-fml.txt
      crash-2025-04-06_15.08.54-fml.txt
      crash-2025-04-06_15.09.33-fml.txt
      crash-2025-04-06_15.09.57-fml.txt
      crash-2025-04-06_15.11.14-fml.txt
      crash-2025-04-07_14.18.53-fml.txt
      crash-2025-04-07_14.22.00-fml.txt
      crash-2025-04-07_14.23.56-fml.txt
      crash-2025-04-07_14.28.21-fml.txt
      crash-2025-04-07_14.29.29-fml.txt
      crash-2025-04-07_14.31.17-fml.txt
      crash-2025-04-07_14.32.48-fml.txt
      crash-2025-04-07_14.43.45-fml.txt
      crash-2025-04-07_14.47.38-fml.txt
      crash-2025-04-07_14.58.01-fml.txt
      crash-2025-04-07_15.01.55-fml.txt
      crash-2025-04-07_15.03.26-fml.txt
      crash-2025-04-07_15.05.48-fml.txt
      crash-2025-04-07_15.56.50-fml.txt
      crash-2025-04-07_16.06.37-fml.txt
      crash-2025-04-07_16.31.46-fml.txt
      crash-2025-04-07_16.34.33-fml.txt
      crash-2025-04-07_16.45.40-fml.txt
      crash-2025-04-07_17.03.14-fml.txt
      crash-2025-04-17_16.42.53-fml.txt
      crash-2025-04-06_02.31.11-client.txt
      crash-2025-04-06_02.36.01-client.txt
      crash-2025-04-06_14.59.22-client.txt
      crash-2025-04-06_15.01.33-client.txt
      crash-2025-04-06_15.13.00-client.txt
      crash-2025-04-06_15.19.25-client.txt
      crash-2025-04-06_15.29.16-client.txt
      crash-2025-04-06_15.38.03-client.txt
      crash-2025-04-06_15.53.03-client.txt
      crash-2025-04-06_16.00.41-client.txt
      crash-2025-04-06_16.17.04-client.txt
      crash-2025-04-08_16.29.10-client.txt
      crash-2025-04-08_16.32.24-client.txt
      crash-2025-04-08_17.39.25-client.txt
      crash-2025-04-08_18.11.31-client.txt
      crash-2025-04-08_18.21.59-client.txt
      crash-2025-04-14_12.47.01-client.txt
      crash-2025-04-14_12.54.40-client.txt
      crash-2025-04-14_18.00.54-client.txt
    resourcepacks/
    defaultconfigs/
    usercache.json
    patchouli_books/
    usernamecache.json
    command_history.txt
    patchouli_data.json
  src/
    main/
      data/
      java/
        net/
          furyan/
            riyaposmod/
              data/
              item/
                weapons/
                  Element.java
                  RiyaStaffItem.java
                  RiyaStaffTier.java
              util/
                helpers/
                  AttributeHelper.java
                ModConstants.java
                ConfigHandler.java
              client/
                gui/
                events/
                  ClientEvents.java
                  ItemTooltipHandler.java
                keybindings/
                  Keybindings.java
                ClientProxy.java
                ClientEventHandler.java
              common/
              events/
              mixins/
              server/
                ServerProxy.java
              weight/
                events/
                  WeightEventHandler.java
                capability/
                  IPlayerWeight.java
                  PlayerWeightImpl.java
                  PlayerWeightProvider.java
                WeightRegistry.java
                EncumbranceLevel.java
                WeightDataGenerator.java
              faction/
                data/
                  PlayerFactionData.java
                commands/
                  FactionCommands.java
                capability/
                  IPlayerFaction.java
                  PlayerFactionImpl.java
                  PlayerFactionProvider.java
                Faction.java
                FactionRegistry.java
              network/
                packet/
                  JoinFactionPacket.java
                  SyncWeightDataPacket.java
                  SyncFactionDataPacket.java
                ModNetworking.java
              registries/
                ItemRegistry.java
                CreativeTabRegistry.java
                WeightAttachmentRegistry.java
                FactionAttachmentRegistry.java
              attachments/
              Config.java
              RiyaposMod.java
      resources/
        data/
          riyaposmod/
            recipes/
            loot_tables/
            advancements/
        assets/
          riyaposmod/
            lang/
              en_us.json
            models/
              item/
                starter_staff.json
                icestaff_adept.json
                firestaff_adept.json
                holystaff_adept.json
                icestaff_master.json
                icestaff_novice.json
                bloodstaff_adept.json
                firestaff_master.json
                firestaff_novice.json
                holystaff_master.json
                holystaff_novice.json
                bloodstaff_master.json
                bloodstaff_novice.json
                naturestaff_adept.json
                icestaff_legendary.json
                naturestaff_master.json
                naturestaff_novice.json
                eldritchstaff_adept.json
                firestaff_legendary.json
                holystaff_legendary.json
                bloodstaff_legendary.json
                eldritchstaff_master.json
                eldritchstaff_novice.json
                evocationstaff_adept.json
                lightningstaff_adept.json
                evocationstaff_master.json
                evocationstaff_novice.json
                lightningstaff_master.json
                lightningstaff_novice.json
                naturestaff_legendary.json
                eldritchstaff_legendary.json
                evocationstaff_legendary.json
                lightningstaff_legendary.json
            textures/
              gui/
              item/
        templates/
          META-INF/
  .junie/
    guidelines.md
  gradle/
    wrapper/
      gradle-wrapper.properties
  .github/
    workflows/
      build.yml
  .gradle/
    vcs-1/
      gc.properties
    8.12.1/
      expanded/
      checksums/
      fileHashes/
      fileChanges/
      vcsMetadata/
      gc.properties
      executionHistory/
    buildOutputCleanup/
      cache.properties
    configuration-cache/
      gc.properties
      1aqv3yxf3gjfmv209q6w6qw1c/
      2n4dpd31a8nq24b2ctrrxef0r/
      1072501e-f8e7-44bf-82b4-91e1341c57fd/
      25291b8f-4195-4ca9-961d-09ef7889fbb6/
      4950d397-480c-4720-ba90-d912f1c08b2d/
      71a9c09a-d643-4c0a-a918-4490ad06f8cf/
      9bb875e0-7c84-406c-bb8a-7333026ec37f/
      b79efddf-b4ea-4c25-8ee1-e9a03255201d/
  README.md
  DEVOXXGENIE.md
  gradle.properties
  TEMPLATE_LICENSE.txt

```
