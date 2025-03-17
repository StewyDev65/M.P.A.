import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * MinecraftPixelArtApp.java
 *
 * A JavaFX application for converting images into Minecraft block art.
 * This tool allows users to load images, select block textures, and convert images into
 * a representation made of Minecraft blocks that can be exported as schematics or images
 * for use in Minecraft worlds.
 *
 * Key Features:
 * - Load and process any image format
 * - Select from multiple conversion algorithms
 * - Customize block variety and color sensitivity
 * - Preview pixel art in real-time
 * - Export as Minecraft commands, function files, or data packs
 * - Save pixel art as images
 * - Manage block mappings and settings
 * - Zoom and pan image preview
 *
 * The UI uses a dark theme optimized for creative work with side-by-side
 * comparison of original and converted images.
 *
 * @author Samuel Stewart, stewy.s.dev@gmail.com
 * @version 1.0
 */
public class MinecraftPixelArtApp extends Application {

    private Stage primaryStage;
    private static final String BLOCK_SETTINGS_FILE = "block_settings.dat";
    private Label texturesPathLabel;
    private Properties appSettings;
    private static final String CONFIG_FILE = "pixelart_config.properties";
    private ImageView originalImageView;
    private ImageView previewImageView;
    private File selectedImageFile;
    private Path blockTexturesPath;
    private Slider blockVarietySlider;
    private Slider sensitivitySlider;
    private Slider resolutionSlider;
    private CheckBox showGridCheckbox;
    private ComboBox<String> algorithmComboBox;
    private ProgressBar progressBar;
    private Label statusLabel;
    private ExecutorService executorService;

    // Block database reference
    private BlockRepository blockRepository;

    // Image processor
    private ImageProcessor imageProcessor;

    // Block library editor
    private BlockLibraryEditor blockLibraryEditor;

    /**
     * Entry point for the JavaFX application. Sets up the primary stage, UI components,
     * applies styles, and initializes application services.
     *
     * @param primaryStage The primary stage for the application window
     */
    @Override
    public void start(Stage primaryStage) {
        // Initialize components
        this.primaryStage = primaryStage;
        appSettings = new Properties();
        blockRepository = new BlockRepository();
        imageProcessor = new ImageProcessor(blockRepository);
        blockLibraryEditor = new BlockLibraryEditor(blockRepository, primaryStage);
        executorService = Executors.newSingleThreadExecutor(); // Single thread for background processing

        // Main layout
        BorderPane root = new BorderPane();

        // Apply dark theme
        Scene scene = new Scene(root, 1250, 750);
        try {
            String css = getClass().getResource("/pixelart/dark-theme.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception e) {
            // Fallback to inline CSS if resource not found
            scene.getRoot().setStyle(AppStyles.DARK_THEME_CSS);
        }

        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);

        // Setup window resize listeners to refresh layout
        primaryStage.widthProperty().addListener((obs, oldVal, newVal) -> {
            refreshLayout();
        });

        primaryStage.heightProperty().addListener((obs, oldVal, newVal) -> {
            refreshLayout();
        });

        primaryStage.setTitle("Minecraft Pixel Art Creator");

        // Top menu
        MenuBar menuBar = createMenuBar();
        root.setTop(menuBar);

        // Center content - images in styled containers
        HBox centerBox = new HBox(20);
        centerBox.setPadding(new Insets(20));
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setSpacing(20);

        // Make centerBox fill available space
        HBox.setHgrow(centerBox, Priority.ALWAYS);

        // Create a wrapper for each container with equal sizing constraints
        VBox originalWrapper = new VBox();
        VBox previewWrapper = new VBox();

        // Set both wrappers to grow equally
        HBox.setHgrow(originalWrapper, Priority.ALWAYS);
        HBox.setHgrow(previewWrapper, Priority.ALWAYS);

        // Set each wrapper to take up exactly 50% of the available width
        originalWrapper.prefWidthProperty().bind(centerBox.widthProperty().subtract(centerBox.getPadding().getLeft()
                + centerBox.getPadding().getRight() + centerBox.getSpacing()).divide(2));
        previewWrapper.prefWidthProperty().bind(centerBox.widthProperty().subtract(centerBox.getPadding().getLeft()
                + centerBox.getPadding().getRight() + centerBox.getSpacing()).divide(2));

        // Original image container
        originalImageView = new ImageView();
        originalImageView.setPreserveRatio(true);
        originalImageView.setSmooth(true);

        Button selectImageButton = new Button("Select Image");
        selectImageButton.setOnAction(e -> openImage());

        VBox originalContainer = createZoomableImageContainer(
                originalImageView, "Original Image", selectImageButton);
        VBox.setVgrow(originalContainer, Priority.ALWAYS); // Make it grow vertically

        // Preview image container
        previewImageView = new ImageView();
        previewImageView.setPreserveRatio(true);
        previewImageView.setSmooth(true);

        Button exportButton = new Button("Export Schematic");
        exportButton.setOnAction(e -> saveSchematic());
        exportButton.setDisable(true); // Disabled until implemented

        VBox previewContainer = createZoomableImageContainer(
                previewImageView, "Minecraft Preview", exportButton);
        VBox.setVgrow(previewContainer, Priority.ALWAYS); // Make it grow vertically

        // Add containers to their wrappers
        originalWrapper.getChildren().add(originalContainer);
        previewWrapper.getChildren().add(previewContainer);

        // Add wrappers to the centerBox
        centerBox.getChildren().addAll(originalWrapper, previewWrapper);
        root.setCenter(centerBox);

        // Right controls in styled container
        VBox controlsContainer = createControlsPanel();
        controlsContainer.getStyleClass().add("controls-container");
        root.setRight(controlsContainer);

        // Set size constraints for controls panel
        controlsContainer.setPrefWidth(280);
        controlsContainer.setMinWidth(220);
        controlsContainer.setMaxWidth(320);

        // Bottom status bar
        HBox statusBar = new HBox(10);
        statusBar.setPadding(new Insets(10));
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.getStyleClass().add("preview-container");

        statusLabel = new Label("Ready");
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(200);

        statusBar.getChildren().addAll(new Label("Status:"), statusLabel, progressBar);

        texturesPathLabel = new Label("No textures loaded");
        texturesPathLabel.setPrefWidth(200);
        statusBar.getChildren().add(texturesPathLabel);

        // Set the scene
        primaryStage.setTitle("Minecraft Pixel Art Creator");
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/pixelart/icon.png")));
        primaryStage.setScene(scene);
        primaryStage.show();

        // Load saved settings
        loadSettings();
    }

    /**
     * Refreshes the layout when window size changes.
     * Ensures proper resizing and positioning of UI elements.
     */
    private void refreshLayout() {
        // This method can be used to perform any layout refresh operations that
        // don't conflict with existing bindings

        // If you need to update anything when window size changes, do it here

        // For example, you could trigger a re-render of the preview if needed
        if (originalImageView.getImage() != null && previewImageView.getImage() != null) {
            // Only resize images if they both exist
            // This avoids unnecessary processing

            // If we need to re-render the preview after resize, uncomment:
            // updatePreview();
        }
    }

    /**
     * Creates the application menu bar with file operations, options, and help.
     *
     * @return The configured MenuBar
     */
    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();

        // File menu
        Menu fileMenu = new Menu("File");
        MenuItem openItem = new MenuItem("Open Image");
        openItem.setOnAction(e -> openImage());

        MenuItem openBlocksItem = new MenuItem("Load Block Textures");
        openBlocksItem.setOnAction(e -> loadBlockTextures());

        MenuItem saveItem = new MenuItem("Save Schematic");
        saveItem.setOnAction(e -> saveSchematic());

        MenuItem exportImageItem = new MenuItem("Export as Image");
        exportImageItem.setOnAction(e -> exportAsImage());

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> System.exit(0));

        fileMenu.getItems().addAll(
                openItem,
                openBlocksItem,
                new SeparatorMenuItem(),
                saveItem,
                exportImageItem,
                new SeparatorMenuItem(),
                exitItem
        );

        // Options menu
        Menu optionsMenu = new Menu("Options");
        MenuItem blockLibraryItem = new MenuItem("Block Library Editor");
        blockLibraryItem.setOnAction(e -> openBlockLibrary());

        MenuItem settingsItem = new MenuItem("Settings");
        settingsItem.setOnAction(e -> openSettings());

        MenuItem blockMappingsItem = new MenuItem("Block ID Mappings");
        blockMappingsItem.setOnAction(e -> manageBlockMappings());

        optionsMenu.getItems().addAll(blockLibraryItem, settingsItem, new SeparatorMenuItem(), blockMappingsItem);

        // Help menu
        Menu helpMenu = new Menu("Help");
        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(e -> showAbout());

        MenuItem helpItem = new MenuItem("Help");
        helpItem.setOnAction(e -> showHelp());

        helpMenu.getItems().addAll(helpItem, aboutItem);

        menuBar.getMenus().addAll(fileMenu, optionsMenu, helpMenu);
        return menuBar;
    }

    /**
     * Creates the controls panel with settings for image conversion.
     * Includes sliders, comboboxes, and checkboxes for customizing the conversion process.
     *
     * @return The configured controls panel as a VBox
     */
    private VBox createControlsPanel() {
        VBox controlsBox = new VBox(15);
        controlsBox.setPadding(new Insets(20));
        // controlsBox.setPrefWidth(280);

        // Title
        Label controlsTitle = new Label("Conversion Settings");
        controlsTitle.getStyleClass().add("title-label");

        // Algorithm selection
        Label algorithmLabel = new Label("Conversion Algorithm:");
        algorithmLabel.getStyleClass().add("section-header");

        algorithmComboBox = new ComboBox<>();
        algorithmComboBox.getItems().addAll("Average Color", "Color Matching", "Dithering",
                "K-Means Clustering", "Edge Preservation", "Full Hybrid", "Average with Dithering");
        algorithmComboBox.setValue("Average Color");
        algorithmComboBox.setMaxWidth(Double.MAX_VALUE);

        Label algorithmDescription = new Label("Choose how pixels are matched to blocks");
        algorithmDescription.getStyleClass().add("info-label");

        // Block variety slider
        Label varietyLabel = new Label("Block Variety:");
        varietyLabel.getStyleClass().add("section-header");

        blockVarietySlider = new Slider(1, 250, 50);
        blockVarietySlider.setShowTickLabels(true);
        blockVarietySlider.setShowTickMarks(true);
        blockVarietySlider.setMajorTickUnit(25);
        blockVarietySlider.setMaxWidth(Double.MAX_VALUE);

        Label varietyValue = new Label("50 block types");
        blockVarietySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            varietyValue.setText(String.format("%.0f block types", newVal));
        });

        // Sensitivity slider
        Label sensitivityLabel = new Label("Color Sensitivity:");
        sensitivityLabel.getStyleClass().add("section-header");

        sensitivitySlider = new Slider(1, 100, 50);
        sensitivitySlider.setShowTickLabels(true);
        sensitivitySlider.setShowTickMarks(true);
        sensitivitySlider.setMajorTickUnit(25);
        sensitivitySlider.setMaxWidth(Double.MAX_VALUE);

        Label sensitivityValue = new Label("50%");
        sensitivitySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            sensitivityValue.setText(String.format("%.0f%%", newVal));
        });

        // Resolution slider
        Label resolutionLabel = new Label("Resolution (blocks):");
        resolutionLabel.getStyleClass().add("section-header");

        resolutionSlider = new Slider(10, 500, 100);
        resolutionSlider.setShowTickLabels(true);
        resolutionSlider.setShowTickMarks(true);
        resolutionSlider.setMajorTickUnit(100);
        resolutionSlider.setMaxWidth(Double.MAX_VALUE);

        Label resolutionValue = new Label("100 blocks");
        resolutionSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            resolutionValue.setText(String.format("%.0f blocks", newVal));
        });

        // Bind slider widths to control panel width for proper scaling
        blockVarietySlider.prefWidthProperty().bind(controlsBox.widthProperty().subtract(40));
        sensitivitySlider.prefWidthProperty().bind(controlsBox.widthProperty().subtract(40));
        resolutionSlider.prefWidthProperty().bind(controlsBox.widthProperty().subtract(40));

        // Show grid option
        showGridCheckbox = new CheckBox("Show Grid");

        // Preview options
        Label previewLabel = new Label("Preview Options:");
        previewLabel.getStyleClass().add("section-header");

        // Enable live preview
        CheckBox livePreviewCheckbox = new CheckBox("Live Preview");
        livePreviewCheckbox.setSelected(true);

        Label whiteCorrectLabel = new Label("White Correction:");
        whiteCorrectLabel.getStyleClass().add("section-header");

        Slider whiteCorrectSlider = new Slider(0, 100, 50);
        whiteCorrectSlider.setShowTickLabels(true);
        whiteCorrectSlider.setShowTickMarks(true);
        whiteCorrectSlider.setMajorTickUnit(25);
        whiteCorrectSlider.setMaxWidth(Double.MAX_VALUE);

        Label whiteCorrectValue = new Label("50%");
        whiteCorrectSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            whiteCorrectValue.setText(String.format("%.0f%%", newVal));
            if (livePreviewCheckbox.isSelected()) updatePreview();
        });

        // Add change listeners for sliders to enable live preview
        blockVarietySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (livePreviewCheckbox.isSelected()) updatePreview();
        });

        sensitivitySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (livePreviewCheckbox.isSelected()) updatePreview();
        });

        resolutionSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (livePreviewCheckbox.isSelected()) updatePreview();
        });

        algorithmComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (livePreviewCheckbox.isSelected()) updatePreview();
        });

        showGridCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (livePreviewCheckbox.isSelected()) updatePreview();
        });

        // Process button
        Button processButton = new Button("Process Image");
        processButton.setMaxWidth(Double.MAX_VALUE);
        processButton.getStyleClass().add("process-button");
        processButton.setOnAction(e -> processImage());

        // Organize controls with some spacing
        VBox algorithmBox = new VBox(5, algorithmLabel, algorithmComboBox, algorithmDescription);
        VBox varietyBox = new VBox(5, varietyLabel, blockVarietySlider, varietyValue);
        VBox sensitivityBox = new VBox(5, sensitivityLabel, sensitivitySlider, sensitivityValue);
        VBox resolutionBox = new VBox(5, resolutionLabel, resolutionSlider, resolutionValue);
        VBox previewBox = new VBox(5, previewLabel, livePreviewCheckbox, showGridCheckbox);
        VBox whiteCorrectBox = new VBox(5, whiteCorrectLabel, whiteCorrectSlider, whiteCorrectValue);

        // Add a spacer to push the process button to the bottom
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        controlsBox.getChildren().addAll(
                controlsTitle,
                algorithmBox,
                varietyBox,
                sensitivityBox,
                whiteCorrectBox,
                resolutionBox,
                previewBox,
                spacer,
                processButton
        );

        return controlsBox;
    }

    /**
     * Opens a file chooser dialog to select an image for conversion.
     * Loads the selected image and updates the UI.
     */
    private void openImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.bmp")
        );
        selectedImageFile = fileChooser.showOpenDialog(null);

        if (selectedImageFile != null) {
            try {
                Image image = new Image(selectedImageFile.toURI().toString());
                originalImageView.setImage(image);

                // Update status
                statusLabel.setText("Image loaded: " + selectedImageFile.getName());

                // Generate initial preview if blocks are loaded
                if (blockRepository.isInitialized()) {
                    updatePreview();
                } else {
                    showAlert("Please load block textures first", Alert.AlertType.INFORMATION);
                }
            } catch (Exception e) {
                showAlert("Error opening image: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    /**
     * Opens a directory chooser dialog to select a folder containing Minecraft block textures.
     * Passes the selected directory to the loadBlockTextures method.
     */
    private void loadBlockTextures() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Block Textures Directory");
        File directory = directoryChooser.showDialog(null);

        if (directory != null) {
            loadBlockTextures(directory.toPath());
        }
    }

    /**
     * Loads Minecraft block textures from the specified directory.
     * Initializes the BlockRepository with the textures and updates UI accordingly.
     *
     * @param path The path to the directory containing block textures
     */
    private void loadBlockTextures(Path path) {
        try {
            blockTexturesPath = path;

            // Show loading indicator
            statusLabel.setText("Loading textures...");
            progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);

            // Load blocks in a background thread to prevent UI freezing
            executorService.submit(() -> {
                try {
                    blockRepository.initialize(blockTexturesPath);
                    BlockIdMapper.updateBlockRepository(blockRepository);
                    blockRepository.loadBlockSettings(BLOCK_SETTINGS_FILE);

                    javafx.application.Platform.runLater(() -> {
                        statusLabel.setText("Loaded " + blockRepository.getBlockCount() + " block textures");
                        progressBar.setProgress(1.0);

                        // Save settings after successful load
                        saveSettings();

                        // Update preview if image is already loaded
                        if (originalImageView.getImage() != null) {
                            updatePreview();
                        }

                        // Update the UI to show current textures path
                        updateTexturesPathDisplay();
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    javafx.application.Platform.runLater(() -> {
                        showAlert("Error loading textures: " + e.getMessage(), Alert.AlertType.ERROR);
                        statusLabel.setText("Error loading textures");
                        progressBar.setProgress(0);
                    });
                }
            });
        } catch (Exception e) {
            showAlert("Error accessing directory: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * Updates the preview image based on current conversion settings.
     * Processes the original image in a background thread and updates the UI when complete.
     */
    private void updatePreview() {
        if (selectedImageFile == null || !blockRepository.isInitialized()) return;

        // Get processing parameters from UI controls
        int blockVariety = (int) blockVarietySlider.getValue();
        double sensitivity = sensitivitySlider.getValue() / 100.0; // Convert to 0-1 range
        int resolution = (int) resolutionSlider.getValue();
        String algorithm = algorithmComboBox.getValue();
        boolean showGrid = showGridCheckbox.isSelected();

        // Update status
        statusLabel.setText("Processing image...");
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);

        // Process image in background thread to prevent UI freezing
        executorService.submit(() -> {
            try {
                // Capture these values on the background thread
                Image original = originalImageView.getImage();

                // Use Platform.runLater for any JavaFX operations
                javafx.application.Platform.runLater(() -> {
                    try {
                        // Run the image processing on the FX thread
                        Image preview = imageProcessor.processImage(
                                original,
                                resolution,
                                sensitivity,
                                blockVariety,
                                algorithm,
                                showGrid
                        );

                        // Update UI on JavaFX thread
                        previewImageView.setImage(preview);
                        statusLabel.setText("Preview updated");
                        progressBar.setProgress(1.0);
                    } catch (Exception e) {
                        e.printStackTrace();
                        showAlert("Error processing image: " + e.getMessage(), Alert.AlertType.ERROR);
                        statusLabel.setText("Error processing image");
                        progressBar.setProgress(0);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    showAlert("Error processing image: " + e.getMessage(), Alert.AlertType.ERROR);
                    statusLabel.setText("Error processing image");
                    progressBar.setProgress(0);
                });
            }
        });
    }

    /**
     * Triggers the image processing operation to update the preview.
     * Called when the "Process Image" button is clicked.
     */
    private void processImage() {
        updatePreview();
    }

    /**
     * Exports the processed pixel art as a Minecraft schematic or command set.
     * Provides options for different export formats including individual commands,
     * one command block, function file, or data pack.
     */
    private void saveSchematic() {
        BlockRepository.BlockData[][] resultBlockGrid = imageProcessor.getResultBlockGrid();
        if (resultBlockGrid == null || resultBlockGrid.length == 0) {
            showAlert("No pixel art has been processed. Please process an image first.", Alert.AlertType.WARNING);
            return;
        }

        // Create dialog for export options
        Dialog<ExportSettings> dialog = new Dialog<>();
        dialog.setTitle("Export Minecraft Commands");
        dialog.setHeaderText("Export Settings");

        // Apply dark theme to the dialog
        DialogPane dialogPane = dialog.getDialogPane();
        try {
            String css = getClass().getResource("/pixelart/dark-theme.css").toExternalForm();
            dialogPane.getStylesheets().add(css);
        } catch (Exception e) {
            dialogPane.setStyle(AppStyles.DARK_THEME_CSS);
        }

        // Setup buttons
        ButtonType exportButtonType = new ButtonType("Export", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(exportButtonType, ButtonType.CANCEL);

        // Create form controls
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Export format selection
        ComboBox<String> formatComboBox = new ComboBox<>();
        formatComboBox.getItems().addAll("Individual Commands", "One Command Block", "Function File", "Data Pack");
        formatComboBox.setValue("Individual Commands");

        // Coordinate system option
        CheckBox relativePositionCheckbox = new CheckBox("Use relative coordinates (~)");
        relativePositionCheckbox.setSelected(true);

        // Data pack specific fields
        TextField namespaceField = new TextField("pixelart");
        TextField packNameField = new TextField("PixelArt_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));

        // Add these fields for data pack format only
        VBox dataPackFields = new VBox(10);
        dataPackFields.getChildren().addAll(
                new Label("Data Pack Settings:"),
                new HBox(10, new Label("Namespace:"), namespaceField),
                new HBox(10, new Label("Pack Name:"), packNameField)
        );
        dataPackFields.setVisible(false);
        dataPackFields.setManaged(false);

        // Show data pack fields only when Data Pack is selected
        formatComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean isDataPack = "Data Pack".equals(newVal);
            dataPackFields.setVisible(isDataPack);
            dataPackFields.setManaged(isDataPack);
            dialog.getDialogPane().getScene().getWindow().sizeToScene();
        });

        // Add controls to grid
        grid.add(new Label("Export Format:"), 0, 0);
        grid.add(formatComboBox, 1, 0);
        grid.add(relativePositionCheckbox, 0, 1, 2, 1);
        grid.add(dataPackFields, 0, 2, 2, 1);

        dialog.getDialogPane().setContent(grid);

        // Convert result to ExportSettings when dialog is confirmed
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == exportButtonType) {
                return new ExportSettings(
                        formatComboBox.getValue(),
                        relativePositionCheckbox.isSelected(),
                        namespaceField.getText(),
                        packNameField.getText()
                );
            }
            return null;
        });

        // Show dialog and handle result
        Optional<ExportSettings> result = dialog.showAndWait();
        result.ifPresent(settings -> {
            try {
                if ("Data Pack".equals(settings.format)) {
                    // Create and export a data pack
                    exportAsDataPack(settings);
                } else {
                    // Get the export text for other formats
                    String commandText = generateMinecraftCommands(
                            imageProcessor.getResultBlockGrid(),
                            settings.useRelativeCoordinates,
                            settings.format);

                    // Save to file
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle("Save Minecraft Commands");

                    // Set extension based on format
                    if (settings.format.equals("Function File")) {
                        fileChooser.getExtensionFilters().add(
                                new FileChooser.ExtensionFilter("Minecraft Function", "*.mcfunction"));
                        fileChooser.setInitialFileName("pixel_art.mcfunction");
                    } else {
                        fileChooser.getExtensionFilters().add(
                                new FileChooser.ExtensionFilter("Text File", "*.txt"));
                        fileChooser.setInitialFileName("minecraft_commands.txt");
                    }

                    File file = fileChooser.showSaveDialog(null);
                    if (file != null) {
                        try (FileWriter writer = new FileWriter(file)) {
                            writer.write(commandText);
                        }
                        showAlert("Commands exported successfully to " + file.getName(), Alert.AlertType.INFORMATION);
                    }
                }
            } catch (Exception e) {
                showAlert("Error exporting: " + e.getMessage(), Alert.AlertType.ERROR);
                e.printStackTrace();
            }
        });
    }

    /**
     * Settings class for storing export configuration options.
     * Used to pass export settings between the dialog and export methods.
     */
    private static class ExportSettings {
        final String format;
        final boolean useRelativeCoordinates;
        final String namespace;
        final String packName;

        /**
         * Constructor for simple export settings without data pack options.
         *
         * @param format                 Export format (Individual Commands, One Command Block, etc.)
         * @param useRelativeCoordinates Whether to use relative coordinates (~)
         */
        ExportSettings(String format, boolean useRelativeCoordinates) {
            this(format, useRelativeCoordinates, "pixelart", "PixelArt");
        }

        /**
         * Constructor for complete export settings including data pack options.
         *
         * @param format                 Export format
         * @param useRelativeCoordinates Whether to use relative coordinates
         * @param namespace              Namespace for data pack
         * @param packName               Name for data pack
         */
        ExportSettings(String format, boolean useRelativeCoordinates, String namespace, String packName) {
            this.format = format;
            this.useRelativeCoordinates = useRelativeCoordinates;
            this.namespace = namespace;
            this.packName = packName;
        }
    }

    /**
     * Exports the pixel art as a Minecraft Data Pack.
     * Creates a complete folder structure with functions for both horizontal and vertical placement.
     *
     * @param settings Export settings containing namespace and pack name
     * @throws IOException If there is an error creating the data pack files
     */
    private void exportAsDataPack(ExportSettings settings) throws IOException {
        // Create a directory chooser
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Directory to Save Data Pack");
        File saveDir = directoryChooser.showDialog(null);

        if (saveDir == null) return;

        // Create data pack directory
        String sanitizedPackName = settings.packName.replaceAll("[^a-zA-Z0-9_-]", "_");
        File dataPackDir = new File(saveDir, sanitizedPackName);
        if (!dataPackDir.mkdir()) {
            throw new IOException("Failed to create data pack directory: " + dataPackDir.getAbsolutePath());
        }

        // Create pack.mcmeta file with pack metadata
        File packMcmeta = new File(dataPackDir, "pack.mcmeta");
        try (FileWriter writer = new FileWriter(packMcmeta)) {
            writer.write("{\n");
            writer.write("  \"pack\": {\n");
            writer.write("    \"pack_format\": 61,\n"); // Updated to 61 per request
            writer.write("    \"description\": \"Pixel Art Creator: " + settings.packName + "\"\n");
            writer.write("  }\n");
            writer.write("}\n");
        }

        // Create data directory structure
        File dataDir = new File(dataPackDir, "data");
        dataDir.mkdir();

        // Create namespace directory using sanitized namespace
        String sanitizedNamespace = settings.namespace.toLowerCase().replaceAll("[^a-z0-9_-]", "_");
        File namespaceDir = new File(dataDir, sanitizedNamespace);
        namespaceDir.mkdir();

        // Create function directory (singular, not plural for newer versions)
        File functionDir = new File(namespaceDir, "function");
        functionDir.mkdir();

        // Generate the horizontal function commands
        String horizontalCommands = generateMinecraftCommands(
                imageProcessor.getResultBlockGrid(),
                settings.useRelativeCoordinates,
                "Individual Commands");

        // Generate the vertical function commands
        String verticalCommands = generateVerticalMinecraftCommands(
                imageProcessor.getResultBlockGrid(),
                settings.useRelativeCoordinates,
                "Individual Commands");

        // Create the pixel_art.mcfunction file for horizontal placement
        File functionFile = new File(functionDir, "pixel_art.mcfunction");
        try (FileWriter writer = new FileWriter(functionFile)) {
            writer.write("# Pixel Art Function (Horizontal Placement)\n");
            writer.write("# Generated by Minecraft Pixel Art Creator\n\n");

            // Add a title message when the function runs
            writer.write("tellraw @a {\"text\":\"Creating horizontal pixel art...\",\"color\":\"green\"}\n\n");

            writer.write(horizontalCommands);

            // Add a completion message
            writer.write("\n# Done message\n");
            writer.write("tellraw @a {\"text\":\"Horizontal pixel art creation complete!\",\"color\":\"green\"}\n");
        }

        // Create the pixel_art_vertical.mcfunction file for vertical placement
        File verticalFunctionFile = new File(functionDir, "pixel_art_vertical.mcfunction");
        try (FileWriter writer = new FileWriter(verticalFunctionFile)) {
            writer.write("# Pixel Art Function (Vertical Placement)\n");
            writer.write("# Generated by Minecraft Pixel Art Creator\n\n");

            // Add a title message when the function runs
            writer.write("tellraw @a {\"text\":\"Creating vertical pixel art...\",\"color\":\"green\"}\n\n");

            writer.write(verticalCommands);

            // Add a completion message
            writer.write("\n# Done message\n");
            writer.write("tellraw @a {\"text\":\"Vertical pixel art creation complete!\",\"color\":\"green\"}\n");
        }

        // Also create a load.mcfunction that prints a message when the datapack is loaded
        File loadFile = new File(functionDir, "load.mcfunction");
        try (FileWriter writer = new FileWriter(loadFile)) {
            writer.write("# Load function\n");
            writer.write("tellraw @a {\"text\":\"Pixel Art Creator data pack loaded!\",\"color\":\"green\"}\n");
            writer.write("tellraw @a {\"text\":\"Use /function " + sanitizedNamespace + ":pixel_art for horizontal placement\",\"color\":\"yellow\"}\n");
            writer.write("tellraw @a {\"text\":\"Use /function " + sanitizedNamespace + ":pixel_art_vertical for vertical placement\",\"color\":\"yellow\"}\n");
        }

        // Create minecraft/tags/function directory for the load tag
        File minecraftDir = new File(dataDir, "minecraft");
        minecraftDir.mkdir();
        File tagsDir = new File(minecraftDir, "tags");
        tagsDir.mkdir();
        File functionTagsDir = new File(tagsDir, "function");
        functionTagsDir.mkdir();

        // Create load.json tag file to register the load function
        File loadTagFile = new File(functionTagsDir, "load.json");
        try (FileWriter writer = new FileWriter(loadTagFile)) {
            writer.write("{\n");
            writer.write("  \"values\": [\n");
            writer.write("    \"" + sanitizedNamespace + ":load\"\n");
            writer.write("  ]\n");
            writer.write("}\n");
        }

        // Create a README.txt file with installation and usage instructions
        File readmeFile = new File(dataPackDir, "README.txt");
        try (FileWriter writer = new FileWriter(readmeFile)) {
            writer.write("Minecraft Pixel Art Data Pack\n");
            writer.write("Created by Minecraft Pixel Art Creator\n\n");
            writer.write("Installation Instructions:\n");
            writer.write("1. Place this folder in the 'datapacks' folder of your Minecraft world\n");
            writer.write("2. In the game, run '/reload' to load the data pack\n");
            writer.write("3. Run one of the following functions:\n");
            writer.write("   - For horizontal placement (on the ground): /function " + sanitizedNamespace + ":pixel_art\n");
            writer.write("   - For vertical placement (standing up): /function " + sanitizedNamespace + ":pixel_art_vertical\n\n");
            writer.write("The pixel art will be created relative to the position where you run the command.\n");
            writer.write("\nFor Minecraft versions below 1.21, you may need to rename the 'function' folder to 'functions' and the 'tags/function' folder to 'tags/functions'.\n");
        }

        showAlert("Data pack created successfully at: " + dataPackDir.getAbsolutePath(), Alert.AlertType.INFORMATION);
    }

    /**
     * Generates Minecraft commands for vertical pixel art placement.
     * Creates commands to place blocks in a vertical orientation (like a wall).
     *
     * @param blockGrid   The 2D array of block data
     * @param useRelative Whether to use relative coordinates (~)
     * @param format      The export format (Individual Commands, One Command Block, etc.)
     * @return A string containing the generated commands
     */
    private String generateVerticalMinecraftCommands(BlockRepository.BlockData[][] blockGrid, boolean useRelative, String format) {
        if (blockGrid == null || blockGrid.length == 0) {
            return "# No blocks to export";
        }

        int height = blockGrid.length;
        int width = blockGrid[0].length;
        StringBuilder commands = new StringBuilder();

        // Add header comment
        commands.append("# Minecraft Pixel Art Commands - Vertical Placement\n");
        commands.append("# Generated by Minecraft Pixel Art Creator\n");
        commands.append("# Dimensions: ").append(width).append("x").append(height).append("\n\n");

        // Map to store block counts for summary
        Map<String, Integer> blockCounts = new HashMap<>();

        // Function to convert block ID to Minecraft block name
        Function<String, String> getMinecraftBlockId = (blockId) -> {
            if (blockId.contains("frosted_ice")) {
                return "minecraft:packed_ice";
            }

            if (blockId.contains("mangrove_leaves")) {
                return "minecraft:light_gray_concrete";
            }

            if (blockId.contains("leaves") && !blockId.contains("mangrove")) {
                String leavesType = blockId.replace("_leaves", "");
                return "minecraft:" + leavesType + "_leaves[persistent=true]";
            }

            if (blockId.contains("trapdoor")) {
                String trapdoorType = blockId.replace("_trapdoor", "");
                return "minecraft:" + trapdoorType + "_trapdoor[facing=east,open=true]";
            }

            if (blockId.contains("shulker_box")) {
                if (blockId.equals("shulker_box")) {
                    // Normal shulker box becomes purple concrete
                    return "minecraft:purple_concrete";
                } else {
                    // Any colored shulker box gets the matching color concrete
                    String color = blockId.replace("_shulker_box", "");
                    return "minecraft:" + color + "_concrete";
                }
            }

            // Replace lightning rod on with white concrete
            if (blockId.equals("lightning_rod_on")) {
                return "minecraft:white_concrete";
            }

            // First try to get the minecraft ID from the block properties
            BlockRepository.BlockData block = blockRepository.getBlock(blockId);
            if (block != null) {
                Object minecraftIdObj = block.getProperty("minecraftId");
                if (minecraftIdObj != null) {
                    return (String) minecraftIdObj;
                }
            }

            // If no mapped ID is found, create one using the BlockIdMapper
            String mappedId = BlockIdMapper.getBlockId(blockId);
            return mappedId;
        };

        // For vertical placement, we transpose the grid
        // x-axis becomes y-axis (vertical), and y-axis becomes z-axis (horizontal)

        if (format.equals("Individual Commands") || format.equals("Function File")) {
            // Process column by column for optimization
            for (int x = 0; x < width; x++) {
                int startY = 0;
                BlockRepository.BlockData currentBlock = null;

                for (int y = 0; y <= height; y++) {
                    // Invert the y-axis to avoid upside-down placement
                    int invertedY = (y < height) ? (height - 1 - y) : height;
                    BlockRepository.BlockData block = (y < height) ? blockGrid[invertedY][x] : null;

                    // If we hit a different block or the end of the column
                    if (currentBlock != null && (y == height || !block.getId().equals(currentBlock.getId()))) {
                        // Create a fill command for the run of identical blocks
                        String blockId = getMinecraftBlockId.apply(currentBlock.getId());

                        // Update block count for summary
                        blockCounts.put(blockId, blockCounts.getOrDefault(blockId, 0) + (y - startY));

                        // Create coordinate strings
                        String pos1, pos2;
                        if (useRelative) {
                            pos1 = "~0 ~" + startY + " ~" + x;
                            pos2 = "~0 ~" + (y - 1) + " ~" + x;
                        } else {
                            pos1 = "0 " + startY + " " + x;
                            pos2 = "0 " + (y - 1) + " " + x;
                        }

                        // For a single block, use setblock instead of fill
                        if (startY == y - 1) {
                            commands.append("setblock ")
                                    .append(pos1).append(" ")
                                    .append(blockId)
                                    .append("\n");
                        } else {
                            // Generate the fill command for multiple blocks
                            commands.append("fill ")
                                    .append(pos1).append(" ")
                                    .append(pos2).append(" ")
                                    .append(blockId)
                                    .append("\n");
                        }

                        // Reset for next run
                        startY = y;
                    }

                    currentBlock = block;
                }
            }
        }

        // Add block usage summary
        commands.append("\n# Block Usage Summary:\n");
        blockCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> commands.append("# ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n"));

        return commands.toString();
    }

    /**
     * Generates Minecraft commands for horizontal pixel art placement.
     * Creates commands to place blocks on the ground in a horizontal pattern.
     *
     * @param blockGrid   The 2D array of block data
     * @param useRelative Whether to use relative coordinates (~)
     * @param format      The export format (Individual Commands, One Command Block, etc.)
     * @return A string containing the generated commands
     */
    private String generateMinecraftCommands(BlockRepository.BlockData[][] blockGrid, boolean useRelative, String format) {
        if (blockGrid == null || blockGrid.length == 0) {
            return "# No blocks to export";
        }

        int height = blockGrid.length;
        int width = blockGrid[0].length;
        StringBuilder commands = new StringBuilder();

        // Add header comment
        commands.append("# Minecraft Pixel Art Commands\n");
        commands.append("# Generated by Minecraft Pixel Art Creator\n");
        commands.append("# Dimensions: ").append(width).append("x").append(height).append("\n\n");

        // Map to store block counts for summary
        Map<String, Integer> blockCounts = new HashMap<>();

        // Function to convert block ID to Minecraft block name
        Function<String, String> getMinecraftBlockId = (blockId) -> {
            // Special handling for certain block types
            if (blockId.contains("frosted_ice")) {
                return "minecraft:packed_ice";
            }

            if (blockId.contains("mangrove_leaves")) {
                return "minecraft:light_gray_concrete";
            }

            if (blockId.contains("leaves") && !blockId.contains("mangrove")) {
                String leavesType = blockId.replace("_leaves", "");
                return "minecraft:" + leavesType + "_leaves[persistent=true]";
            }

            if (blockId.contains("trapdoor")) {
                String trapdoorType = blockId.replace("_trapdoor", "");
                return "minecraft:" + trapdoorType + "_trapdoor[half=top]";
            }

            if (blockId.contains("shulker_box")) {
                if (blockId.equals("shulker_box")) {
                    // Normal shulker box becomes purple concrete
                    return "minecraft:purple_concrete";
                } else {
                    // Any colored shulker box gets the matching color concrete
                    String color = blockId.replace("_shulker_box", "");
                    return "minecraft:" + color + "_concrete";
                }
            }

            // Replace lightning rod on with white concrete
            if (blockId.equals("lightning_rod_on")) {
                return "minecraft:white_concrete";
            }

            // Continue with normal processing for other blocks
            BlockRepository.BlockData block = blockRepository.getBlock(blockId);
            if (block != null) {
                Object minecraftIdObj = block.getProperty("minecraftId");
                if (minecraftIdObj != null) {
                    return (String) minecraftIdObj;
                }
            }

            // If no mapped ID is found, create one using the BlockIdMapper
            String mappedId = BlockIdMapper.getBlockId(blockId);
            return mappedId;
        };

        if (format.equals("Individual Commands") || format.equals("Function File")) {
            // Process row by row for optimization - fill commands for consecutive identical blocks
            for (int y = 0; y < height; y++) {
                int startX = 0;
                BlockRepository.BlockData currentBlock = null;

                for (int x = 0; x <= width; x++) {
                    BlockRepository.BlockData block = (x < width) ? blockGrid[y][x] : null;

                    // If we hit a different block or the end of the row
                    if (currentBlock != null && (x == width || !block.getId().equals(currentBlock.getId()))) {
                        // Create a fill command for the run of identical blocks
                        String blockId = getMinecraftBlockId.apply(currentBlock.getId());

                        // Update block count for summary
                        blockCounts.put(blockId, blockCounts.getOrDefault(blockId, 0) + (x - startX));

                        // Create coordinate strings
                        String pos1, pos2;
                        if (useRelative) {
                            pos1 = "~" + startX + " ~0 ~" + y;
                            pos2 = "~" + (x - 1) + " ~0 ~" + y;
                        } else {
                            pos1 = startX + " 0 " + y;
                            pos2 = (x - 1) + " 0 " + y;
                        }

                        // For a single block, use setblock instead of fill
                        if (startX == x - 1) {
                            commands.append("setblock ")
                                    .append(pos1).append(" ")
                                    .append(blockId)
                                    .append("\n");
                        } else {
                            // Generate the fill command for multiple blocks
                            commands.append("fill ")
                                    .append(pos1).append(" ")
                                    .append(pos2).append(" ")
                                    .append(blockId)
                                    .append("\n");
                        }

                        // Reset for next run
                        startX = x;
                    }

                    currentBlock = block;
                }
            }
        } else if (format.equals("One Command Block")) {
            // Create the one command block header
            commands.append("# Paste this into a command block:\n");
            commands.append("summon falling_block ~ ~1 ~ {Time:1,BlockState:{Name:redstone_block},Passengers:[");
            commands.append("{id:falling_block,Time:1,BlockState:{Name:activator_rail},Passengers:[");
            commands.append("{id:command_block_minecart,Command:'gamerule commandBlockOutput false'},\n");

            // Process row by row, but create more compact commands to fit in one command block
            List<String> commandList = new ArrayList<>();

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    BlockRepository.BlockData block = blockGrid[y][x];
                    if (block != null) {
                        String blockId = getMinecraftBlockId.apply(block.getId());

                        // Update block count
                        blockCounts.put(blockId, blockCounts.getOrDefault(blockId, 0) + 1);

                        // Create coordinate string
                        String pos;
                        if (useRelative) {
                            pos = "~" + x + " ~0 ~" + y;
                        } else {
                            pos = x + " 0 " + y;
                        }

                        // Generate the setblock command
                        commandList.add("setblock " + pos + " " + blockId);
                    }
                }
            }

            // Add commands to one command structure
            for (int i = 0; i < commandList.size(); i++) {
                commands.append("{id:command_block_minecart,Command:'").append(commandList.get(i)).append("'},\n");
            }

            // Add cleanup commands
            commands.append("{id:command_block_minecart,Command:'setblock ~ ~1 ~ command_block{auto:1,Command:\"fill ~ ~ ~ ~ ~-3 ~ air\"}'},\n");
            commands.append("{id:command_block_minecart,Command:'kill @e[type=command_block_minecart,distance=..1]'}\n");
            commands.append("]}]}");
        }

        // Add block usage summary
        commands.append("\n# Block Usage Summary:\n");
        blockCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> commands.append("# ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n"));

        return commands.toString();
    }

    /**
     * Exports the current pixel art preview as a PNG image.
     * Provides a file chooser dialog to select save location.
     */
    private void exportAsImage() {
        if (previewImageView.getImage() == null) {
            showAlert("No preview image available. Please process an image first.", Alert.AlertType.WARNING);
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Pixel Art Preview");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PNG Image", "*.png"));
        fileChooser.setInitialFileName("pixel_art_preview.png");

        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            try {
                // Use the original image from the preview, which has the full 16x16 resolution per block
                Image fullResImage = previewImageView.getImage();

                // Write to file using ImageIO
                BufferedImage bufferedImage = SwingFXUtils.fromFXImage(fullResImage, null);
                ImageIO.write(bufferedImage, "png", file);

                showAlert("Image exported successfully to " + file.getName(), Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("Error exporting image: " + e.getMessage(), Alert.AlertType.ERROR);
                e.printStackTrace();
            }
        }
    }

    /**
     * Opens the block library editor dialog.
     * Allows users to manage block settings like enabled/disabled state and color properties.
     */
    private void openBlockLibrary() {
        if (!blockRepository.isInitialized()) {
            showAlert("Please load block textures first", Alert.AlertType.INFORMATION);
            return;
        }

        blockLibraryEditor.show();
    }

    /**
     * Opens the settings dialog for the application.
     * Currently displays a placeholder message.
     */
    private void openSettings() {
        // Will show settings dialog
        showAlert("Settings dialog not yet implemented", Alert.AlertType.INFORMATION);
    }

    /**
     * Loads application settings from the config file.
     * Restores the previously used block textures path if available.
     */
    private void loadSettings() {
        appSettings = new Properties();
        try {
            File configFile = new File(CONFIG_FILE);
            if (configFile.exists()) {
                try (FileInputStream in = new FileInputStream(configFile)) {
                    appSettings.load(in);

                    // Load textures path if it exists
                    String savedTexturePath = appSettings.getProperty("textures_path");
                    if (savedTexturePath != null && !savedTexturePath.isEmpty()) {
                        Path path = Paths.get(savedTexturePath);
                        File dir = path.toFile();
                        if (dir.exists() && dir.isDirectory()) {
                            loadBlockTextures(path);
                            blockRepository.loadBlockSettings("block_settings.dat");
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading settings: " + e.getMessage());
        }
    }

    /**
     * Updates the UI to display the current textures path.
     * Shows the filename with tooltip for full path.
     */
    private void updateTexturesPathDisplay() {
        if (blockTexturesPath != null) {
            texturesPathLabel.setText("Textures: " + blockTexturesPath.getFileName());
            texturesPathLabel.setTooltip(new Tooltip(blockTexturesPath.toString()));
        } else {
            texturesPathLabel.setText("No textures loaded");
            texturesPathLabel.setTooltip(null);
        }
    }

    /**
     * Saves application settings to the config file.
     * Stores the current block textures path and block settings.
     */
    private void saveSettings() {
        try {
            if (blockTexturesPath != null) {
                appSettings.setProperty("textures_path", blockTexturesPath.toString());
            }

            try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
                appSettings.store(out, "Minecraft Pixel Art Creator Settings");
            }

            // Save block settings when the app closes
            if (blockRepository.isInitialized()) {
                blockRepository.saveBlockSettings(BLOCK_SETTINGS_FILE);
            }
        } catch (Exception e) {
            System.err.println("Error saving settings: " + e.getMessage());
        }
    }

    /**
     * Displays the about dialog with application information.
     * Includes version and attribution details.
     */
    private void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About Minecraft Pixel Art Creator");
        alert.setHeaderText("Minecraft Pixel Art Creator");
        alert.setContentText("A tool for converting images into Minecraft block art.\n\n" +
                "Version: 1.0\n" +
                "Created with JavaFX");

        // Apply dark theme to dialog
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/pixelart/dark-theme.css").toExternalForm());
        // Or use inline style
        dialogPane.setStyle(AppStyles.DARK_THEME_CSS);

        alert.showAndWait();
    }

    /**
     * Displays the help dialog with basic usage instructions.
     * Provides step-by-step guidance for new users.
     */
    private void showHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Help");
        alert.setHeaderText("How to Use Minecraft Pixel Art Creator");
        alert.setContentText(
                "1. Load Block Textures: File -> Load Block Textures\n" +
                        "2. Open an Image: File -> Open Image\n" +
                        "3. Adjust settings as needed\n" +
                        "4. Click Process Image to generate the preview\n" +
                        "5. Export as schematic or image when ready"
        );

        // Apply dark theme to dialog
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/pixelart/dark-theme.css").toExternalForm());
        // Or use inline style
        dialogPane.setStyle(AppStyles.DARK_THEME_CSS);

        alert.showAndWait();
    }

    /**
     * Displays an alert dialog with the specified message and type.
     * Applies the dark theme to the dialog.
     *
     * @param message   The message to display
     * @param alertType The type of alert (ERROR, WARNING, INFORMATION)
     */
    private void showAlert(String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(alertType == Alert.AlertType.ERROR ? "Error" : "Information");
        alert.setHeaderText(null);
        alert.setContentText(message);

        // Apply dark theme to dialog
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/pixelart/dark-theme.css").toExternalForm());
        // Or use inline style
        dialogPane.setStyle(AppStyles.DARK_THEME_CSS);

        alert.showAndWait();
    }

    /**
     * Creates a container for displaying images with zoom and pan functionality.
     * Includes title, zoom controls, and an optional action button.
     *
     * @param imageView    The ImageView to display
     * @param title        The title for the container
     * @param actionButton An optional button to add below the image
     * @return A VBox containing the configured image viewer
     */
    private VBox createZoomableImageContainer(ImageView imageView, String title, Button actionButton) {
        VBox container = new VBox(10);
        container.getStyleClass().add("preview-container");

        container.setAlignment(Pos.CENTER);

        // Make container fill available space
        VBox.setVgrow(container, Priority.ALWAYS);
        container.setMinWidth(300); // Set minimum width instead of fixed width

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("title-label");

        // Create the zoom controls
        HBox zoomControls = new HBox(10);
        zoomControls.setAlignment(Pos.CENTER);

        Button zoomInButton = new Button("+");
        Button zoomOutButton = new Button("-");
        Button resetZoomButton = new Button("Reset");
        Label zoomLabel = new Label("100%");

        zoomControls.getChildren().addAll(zoomOutButton, zoomLabel, zoomInButton, resetZoomButton);

        // Create a Pane to hold the image and allow for panning
        Pane imagePane = new Pane();
        imagePane.getChildren().add(imageView);

        VBox.setVgrow(imagePane, Priority.ALWAYS);

        // Center the image in the pane initially
        imageView.layoutXProperty().bind(
                imagePane.widthProperty().subtract(imageView.fitWidthProperty().multiply(imageView.scaleXProperty())).divide(2));
        imageView.layoutYProperty().bind(
                imagePane.heightProperty().subtract(imageView.fitHeightProperty().multiply(imageView.scaleYProperty())).divide(2));

        // Create a ScrollPane to contain the image for scrolling when zoomed
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(imagePane);
        scrollPane.setPannable(true);
        scrollPane.setFitToWidth(true);  // Make contents fit to width
        scrollPane.setFitToHeight(true); // Make contents fit to height

        // Make scrollPane grow with container
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // Bind image view size to its container to enable resizing
        imageView.fitWidthProperty().bind(scrollPane.widthProperty().multiply(0.95));
        imageView.fitHeightProperty().bind(scrollPane.heightProperty().multiply(0.95));

        // Set the viewport size for the scroll pane
        imagePane.prefWidthProperty().bind(scrollPane.widthProperty());
        imagePane.prefHeightProperty().bind(scrollPane.heightProperty());
        imagePane.minWidthProperty().bind(scrollPane.widthProperty());
        imagePane.minHeightProperty().bind(scrollPane.heightProperty());

        // Initialize zoom level and drag state
        final double[] zoomLevel = {1.0};
        final boolean[] isDragging = {false};
        final double[] dragX = {0};
        final double[] dragY = {0};

        // Mouse handling for panning
        imageView.setOnMousePressed(e -> {
            dragX[0] = e.getSceneX() - imageView.getLayoutX();
            dragY[0] = e.getSceneY() - imageView.getLayoutY();
            isDragging[0] = true;
            imageView.setCursor(javafx.scene.Cursor.CLOSED_HAND);
        });

        imageView.setOnMouseDragged(e -> {
            if (isDragging[0] && zoomLevel[0] > 1.0) {
                // Calculate new position
                double newLayoutX = e.getSceneX() - dragX[0];
                double newLayoutY = e.getSceneY() - dragY[0];

                // Update image position directly
                imageView.setLayoutX(newLayoutX);
                imageView.setLayoutY(newLayoutY);

                // Unbind temporarily while dragging
                imageView.layoutXProperty().unbind();
                imageView.layoutYProperty().unbind();
            }
        });

        imageView.setOnMouseReleased(e -> {
            isDragging[0] = false;
            imageView.setCursor(javafx.scene.Cursor.DEFAULT);
        });

        // Zoom in button action
        zoomInButton.setOnAction(e -> {
            zoomLevel[0] *= 1.2; // Increase zoom by 20%
            updateZoom(imageView, zoomLevel[0], zoomLabel);

            // Unbind when zoomed to allow manual positioning
            if (zoomLevel[0] > 1.0) {
                imageView.layoutXProperty().unbind();
                imageView.layoutYProperty().unbind();
            } else {
                // Recenter when at normal zoom
                recenterImage(imageView, imagePane);
            }
        });

        // Zoom out button action
        zoomOutButton.setOnAction(e -> {
            zoomLevel[0] /= 1.2; // Decrease zoom by 20%
            updateZoom(imageView, zoomLevel[0], zoomLabel);

            // Rebind to center if we're back at normal zoom
            if (zoomLevel[0] <= 1.0) {
                recenterImage(imageView, imagePane);
            }
        });

        // Reset zoom button action
        resetZoomButton.setOnAction(e -> {
            zoomLevel[0] = 1.0; // Reset to 100%
            updateZoom(imageView, zoomLevel[0], zoomLabel);
            recenterImage(imageView, imagePane);
        });

        // Mouse wheel zoom
        scrollPane.setOnScroll(e -> {
            if (e.isControlDown()) { // Only zoom when Ctrl is pressed
                // Store the mouse position relative to the image
                double mouseX = e.getX() - imageView.getLayoutX();
                double mouseY = e.getY() - imageView.getLayoutY();
                double oldScale = imageView.getScaleX();

                if (e.getDeltaY() > 0) {
                    zoomLevel[0] *= 1.05; // Zoom in
                } else {
                    zoomLevel[0] /= 1.05; // Zoom out
                }

                double newScale = Math.min(10.0, Math.max(0.1, zoomLevel[0]));
                updateZoom(imageView, zoomLevel[0], zoomLabel);

                // Unbind when zoomed
                if (newScale > 1.0) {
                    imageView.layoutXProperty().unbind();
                    imageView.layoutYProperty().unbind();

                    // Adjust position to zoom toward mouse pointer
                    double scaleFactor = newScale / oldScale - 1.0;
                    imageView.setLayoutX(imageView.getLayoutX() - mouseX * scaleFactor);
                    imageView.setLayoutY(imageView.getLayoutY() - mouseY * scaleFactor);
                } else {
                    recenterImage(imageView, imagePane);
                }

                e.consume();
            }
        });

        container.getChildren().addAll(titleLabel, scrollPane, zoomControls);

        // Add action buttons for the preview container
        if (actionButton != null) {
            // If this is the preview container and actionButton is the export schematic button
            if (title.equals("Minecraft Preview")) {
                // Enable the export schematic button and connect it to saveSchematic
                actionButton.setDisable(false);
                actionButton.setOnAction(e -> saveSchematic());

                // Create "Save as Image" button
                Button saveImageButton = new Button("Save as Image");
                saveImageButton.setOnAction(e -> exportAsImage());

                // Create a VBox to hold both buttons
                HBox buttonBox = new HBox(10);
                buttonBox.setAlignment(Pos.CENTER);
                buttonBox.getChildren().addAll(actionButton, saveImageButton);

                container.getChildren().add(buttonBox);
            } else {
                // For the original image container
                container.getChildren().add(actionButton);
            }
        }

        return container;
    }

    /**
     * Updates the zoom level of an image view and corresponding label.
     * Clamps the zoom level to reasonable values.
     *
     * @param imageView The ImageView to zoom
     * @param zoomLevel The desired zoom level
     * @param zoomLabel The label to update with zoom percentage
     */
    private void updateZoom(ImageView imageView, double zoomLevel, Label zoomLabel) {
        // Limit zoom level to reasonable values
        double clampedZoom = Math.min(10.0, Math.max(0.1, zoomLevel));

        // Update the image scale
        imageView.setScaleX(clampedZoom);
        imageView.setScaleY(clampedZoom);

        // Update zoom label
        zoomLabel.setText(String.format("%.0f%%", clampedZoom * 100));
    }

    /**
     * Recenters an image view within its parent pane.
     * Binds the layout properties to keep the image centered.
     *
     * @param imageView The ImageView to center
     * @param pane      The parent pane
     */
    private void recenterImage(ImageView imageView, Pane pane) {
        // Rebind the layout properties to center the image
        imageView.layoutXProperty().bind(
                pane.widthProperty().subtract(imageView.fitWidthProperty().multiply(imageView.scaleXProperty())).divide(2));
        imageView.layoutYProperty().bind(
                pane.heightProperty().subtract(imageView.fitHeightProperty().multiply(imageView.scaleYProperty())).divide(2));
    }

    /**
     * Opens the block ID mappings manager dialog.
     * Allows users to import, export, and update block ID mappings.
     */
    private void manageBlockMappings() {
        Stage stage = new Stage();
        stage.setTitle("Block ID Mappings");
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(primaryStage);

        VBox root = new VBox(10);
        root.setPadding(new Insets(20));

        // Apply dark theme
        Scene scene = new Scene(root, 600, 400);
        try {
            String css = getClass().getResource("/pixelart/dark-theme.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception e) {
            scene.getRoot().setStyle(AppStyles.DARK_THEME_CSS);
        }

        Label titleLabel = new Label("Manage Block ID Mappings");
        titleLabel.getStyleClass().add("title-label");

        Button importButton = new Button("Import Mappings from File");
        importButton.setMaxWidth(Double.MAX_VALUE);
        importButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Import Block ID Mappings");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Text Files", "*.txt"));
            File file = fileChooser.showOpenDialog(stage);
            if (file != null) {
                try {
                    BlockIdMapper.importMappings(file);
                    showAlert("Block ID mappings imported successfully.", Alert.AlertType.INFORMATION);

                    // Update block repository with new mappings
                    BlockIdMapper.updateBlockRepository(blockRepository);
                    blockRepository.saveBlockSettings(BLOCK_SETTINGS_FILE);
                } catch (Exception ex) {
                    showAlert("Error importing mappings: " + ex.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });

        Button exportButton = new Button("Export Current Mappings to File");
        exportButton.setMaxWidth(Double.MAX_VALUE);
        exportButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Export Block ID Mappings");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Text Files", "*.txt"));
            fileChooser.setInitialFileName("block_id_mappings.txt");
            File file = fileChooser.showSaveDialog(stage);
            if (file != null) {
                try {
                    BlockIdMapper.exportMappings(file);
                    showAlert("Block ID mappings exported successfully.", Alert.AlertType.INFORMATION);
                } catch (Exception ex) {
                    showAlert("Error exporting mappings: " + ex.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });

        Button updateButton = new Button("Update Block Repository with Current Mappings");
        updateButton.setMaxWidth(Double.MAX_VALUE);
        updateButton.setOnAction(e -> {
            BlockIdMapper.updateBlockRepository(blockRepository);
            blockRepository.saveBlockSettings(BLOCK_SETTINGS_FILE);
            showAlert("Block repository updated with current mappings.", Alert.AlertType.INFORMATION);
        });

        Button closeButton = new Button("Close");
        closeButton.setMaxWidth(Double.MAX_VALUE);
        closeButton.setOnAction(e -> stage.close());

        TextArea infoArea = new TextArea(
                "Block ID mappings are used to convert texture filenames to valid Minecraft block IDs.\n\n" +
                        "The mapping file should have one mapping per line, in the format:\n" +
                        "texture_filename=minecraft:block_id\n\n" +
                        "Example:\n" +
                        "oak_planks=minecraft:oak_planks\n" +
                        "grass_block_top=minecraft:grass_block\n\n" +
                        "Comments start with # and are ignored."
        );
        infoArea.setEditable(false);
        infoArea.setPrefHeight(150);

        root.getChildren().addAll(
                titleLabel,
                infoArea,
                new Separator(),
                importButton,
                exportButton,
                updateButton,
                closeButton
        );

        stage.setScene(scene);
        stage.show();
    }

    /**
     * Called when the application is closing.
     * Saves settings and shuts down the executor service.
     */
    @Override
    public void stop() {
        // Save settings
        saveSettings();

        // Clean up resources
        executorService.shutdown();
    }

    /**
     * Main entry point for the JavaFX application.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}