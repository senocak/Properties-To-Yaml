# Properties-To-Yaml

![Build](https://github.com/senocak/Properties-To-Yaml/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

## Implementation Status
- [x] Create a new [IntelliJ Platform Plugin Template][template] project.
- [x] Get familiar with the [template documentation][template].
- [x] Adjust the [pluginGroup](./gradle.properties) and [pluginName](./gradle.properties), as well as the [id](./src/main/resources/META-INF/plugin.xml) and [sources package](./src/main/kotlin).
- [x] Implement properties to YAML conversion functionality
  - [x] Add SnakeYAML dependency
  - [x] Create conversion service
  - [x] Implement UI in tool window
- [x] Implement YAML to properties conversion functionality
  - [x] Create conversion service
  - [x] Add right-click menu option
- [x] Adjust the plugin description in `README` (see [Tips][docs:plugin-description])

## Next Steps Before Publishing
- [ ] Review the [Legal Agreements](https://plugins.jetbrains.com/docs/marketplace/legal-agreements.html?from=IJPluginTemplate).
- [ ] [Publish a plugin manually](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html?from=IJPluginTemplate) for the first time.
- [ ] Set the `MARKETPLACE_ID` in the above README badges. You can obtain it once the plugin is published to JetBrains Marketplace.
- [ ] Set the [Plugin Signing](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html?from=IJPluginTemplate) related [secrets](https://github.com/JetBrains/intellij-platform-plugin-template#environment-variables).
- [ ] Set the [Deployment Token](https://plugins.jetbrains.com/docs/marketplace/plugin-upload.html?from=IJPluginTemplate).

<!-- Plugin description -->
Properties-To-Yaml is a JetBrains IDE plugin that simplifies the conversion between Java properties files and YAML format.

Features:
- Convert `.properties` files to `.yml` format with a simple UI
- Convert `.yml` files back to `.properties` format
- Preserves hierarchical structure using dot notation in property keys
- Provides a live preview of the YAML output before saving
- Maintains proper indentation and formatting in the generated YAML

Usage:
1. Open the "PropertiesToYaml" tool window from the IDE sidebar
   - Click "Select Properties File" to choose a .properties file
   - Preview the YAML conversion in the tool window
   - Click "Convert to YAML" to save the result as a .yml file

2. Right-click menu options:
   - Right-click on a `.properties` file and select "Convert to YAML"
   - Right-click on a `.yml` file and select "Convert to Properties"
<!-- Plugin description end -->

## Installation

- Using the IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "Properties-To-Yaml"</kbd> >
  <kbd>Install</kbd>

- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID) and install it by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download the [latest release](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID/versions) from JetBrains Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

- Manually:

  Download the [latest release](https://github.com/senocak/Properties-To-Yaml/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
