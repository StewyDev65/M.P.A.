# 🎮 Minecraft Pixel Art Creator

> Transform your images into beautiful Minecraft block art with customizable algorithms and export options

[![Java Version](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/technologies/downloads/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21.0.2-blue.svg)](https://openjfx.io/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

## ✨ Features

- 🖼️ **Load and process any image format** - Convert JPG, PNG, BMP into Minecraft block representations
- 🧩 **Multiple conversion algorithms** - Choose from Average Color, Color Matching, Dithering, K-Means Clustering, and more
- 🎨 **Customizable settings** - Adjust block variety, color sensitivity, and resolution
- 👁️ **Real-time preview** - See your pixel art instantly as you modify settings
- 📦 **Export options** - Save as Minecraft commands, function files, or data packs
- 🔄 **Horizontal and vertical placement** - Create floor art or wall displays in-game
- 🧰 **Block library editor** - Manage which blocks are used in conversions
- 🗺️ **Block ID mapping** - Includes a customizable block texture to Minecraft ID mapper

## 📋 Prerequisites

- [Java 21+](https://www.oracle.com/java/technologies/downloads/)
- [Maven](https://maven.apache.org/download.cgi) (for building)
- **Minecraft Textures** (not included, must be extracted separately for legal reasons)

## 🛠️ Installation

1. Clone or download this repository
   ```bash
   git clone https://github.com/StewyDev65/M.P.A..git
   cd M.P.A.
   ```

2. Extract Minecraft block textures from your legal copy of Minecraft
    - Navigate to your Minecraft installation
    - Find the JAR file for your Minecraft version (e.g., `.minecraft/versions/1.21.4/1.21.4.jar`)
    - Extract the `assets/minecraft/textures/block` directory

3. Build the application with Maven
   ```bash
   # Clean and package the application with dependencies
   mvn clean package
   ```

4. Run the application (two options)

   **Option 1:** Using the Maven JavaFX plugin
   ```bash
   mvn javafx:run
   ```

   **Option 2:** Using the generated JAR file
   ```bash
   java -jar target/M.P.A.M.-1.0-SNAPSHOT.jar
   ```

   Note: If you encounter JavaFX module errors when running the JAR directly, use this command instead:
   ```bash
   java --module-path /path/to/javafx-sdk-21.0.2/lib --add-modules javafx.controls,javafx.fxml,javafx.graphics -jar target/M.P.A.M.-1.0-SNAPSHOT.jar
   ```

## 🚀 Using the App

### Initial Setup

1. **First Launch**: Open the application
2. **Load Block Textures**: Go to `File → Load Block Textures` and select your directory of extracted Minecraft block textures
3. **Open an Image**: Go to `File → Open Image` to select an image to convert

### Converting Images

<table>
  <tr>
    <td width="60%">
      <h4>Step 1: Adjust Settings</h4>
      <ul>
        <li>Select a conversion algorithm from the dropdown menu</li>
        <li>Adjust block variety (1-250) to control the diversity of blocks</li>
        <li>Set color sensitivity (1-100%) to fine-tune color matching</li>
        <li>Configure resolution to determine creation size</li>
      </ul>
      <h4>Step 2: Processing</h4>
      <ul>
        <li>Enable "Live Preview" for real-time updates as you adjust settings</li>
        <li>Click "Process Image" to generate the final preview</li>
      </ul>
      <h4>Step 3: Exporting</h4>
      <ul>
        <li>Click "Export Schematic" to save as Minecraft commands</li>
        <li>Choose from multiple export formats</li>
        <li>Select coordinate style (relative/absolute)</li>
      </ul>
    </td>
  </tr>
</table>

### Export Options

The app offers multiple ways to export your creations:

| Export Format | Description | Use Case |
|---------------|-------------|----------|
| Individual Commands | A text file with separate setblock/fill commands | For manual pasting into chat/command blocks |
| One Command Block | A single command that spawns command block minecarts | For "one-click" installation in creative mode |
| Function File | `.mcfunction` file compatible with Minecraft's function system | For use in datapacks or function folders |
| Data Pack | Complete datapack with horizontal and vertical placement functions | Most user-friendly option for survival worlds |

Example of generated commands:
```minecraft
# Minecraft Pixel Art Commands
# Generated by Minecraft Pixel Art Creator
# Dimensions: 64x64

fill ~0 ~0 ~0 ~3 ~0 ~0 minecraft:grass_block
setblock ~4 ~0 ~0 minecraft:oak_log
fill ~5 ~0 ~0 ~7 ~0 ~0 minecraft:oak_planks
# ... more commands ...
```

### Block Management

- **Block Library Editor**: Access via `Options → Block Library Editor`
    - Enable/disable specific blocks
    - Adjust color properties
    - Set block priorities

- **Block ID Mappings**: Access via `Options → Block ID Mappings`
    - Import custom mappings
    - Export current mappings
    - Update block repository

## 🧠 Conversion Algorithms

| Algorithm | Description | Best For |
|-----------|-------------|----------|
| Average Color | Simple color matching | Quick conversions |
| Color Matching | Precise color matching | Detailed art |
| Dithering | Adds texture for gradients | Smooth transitions |
| K-Means Clustering | Groups similar colors | Complex images |
| Edge Preservation | Maintains sharp details | Images with clear edges |
| Full Hybrid | Combines multiple techniques | Balanced results |
| Average with Dithering | Balanced approach | Most images |

## 🔍 Technical Details

<details>
<summary><b>UI Components</b></summary>

- Dark-themed UI optimized for creative work
- Dual panel layout with original and preview images
- Zoomable image viewers with pan functionality
- Control panel with real-time settings adjustment
- Progress tracking for background processing

</details>

<details>
<summary><b>Block Processing</b></summary>

- Intelligent color matching algorithms
- Texture-aware block selection
- Optimization for minimal block count
- Supports transparent blocks and multi-directional blocks

</details>

<details>
<summary><b>Performance Considerations</b></summary>

- Multithreaded image processing
- Efficient block data storage
- Optimized preview generation
- Responsive UI during processing

</details>

## 🔮 Future Improvements

- [ ] 3D model export (.schematic, .nbt formats)
- [ ] Structure block integration
- [ ] More advanced dithering algorithms
- [ ] Cloud sharing of completed pixel art
- [ ] Automatic texture extraction from Minecraft installations
- [ ] Improved block pattern recognition

## ⚠️ Important Notes

- **Minecraft Textures**: You must supply your own Minecraft textures due to licensing restrictions
- **Legal Usage**: This tool is for personal use and artistic creation; follow Minecraft's EULA
- **Performance**: Large images or high resolution settings will require more processing power

## 💬 FAQ

<details>
<summary><b>Why do I need to provide my own textures?</b></summary>
Due to Mojang's licensing terms, we cannot distribute Minecraft textures with this application. You must extract them from your legitimate copy of Minecraft.
</details>

<details>
<summary><b>What's the recommended image size?</b></summary>
For best results, start with images between 512x512 and 1024x1024 pixels. The application will resize based on your resolution setting.
</details>

<details>
<summary><b>Can I use this in survival mode?</b></summary>
Not yet. The Data Pack currently uses commands that require creative mode or operator permissions. A survival-friendly version is planned for a future update.
</details>

<details>
<summary><b>Why do I get "JavaFX runtime components are missing" error?</b></summary>
If you're running the JAR directly and see this error, you need to specify the path to JavaFX modules. Use the alternate command that includes `--module-path` and `--add-modules` parameters.
</details>

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE.md) file for details.

## 🙏 Acknowledgments

- Inspired by various Minecraft pixel art generators
- Block ID mapping system adapted from community reference
- Color matching algorithms inspired by image processing research

---

<div align="center">
  <sub>Built with ❤️ by Samuel Stewart (stewy.s.dev@gmail.com)</sub>
</div>