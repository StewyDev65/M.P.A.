import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class BlockLibraryEditor {

    private final BlockRepository blockRepository;
    private final Stage parentStage;
    private Stage editorStage;
    private TableView<BlockTableItem> blockTable;
    private FilteredList<BlockTableItem> filteredBlocks;
    private ObservableList<BlockTableItem> allBlocks;
    private ImageView selectedBlockPreview;
    private VBox blockDetailsPane;

    // Block properties fields
    private TextField nameField;
    private ColorPicker averageColorPicker;
    private Slider weightSlider;
    private CheckBox enabledCheckbox;

    // Selected block data
    private BlockTableItem selectedBlock;

    private static final String BLOCK_SETTINGS_FILE = "block_settings.dat";

    // Block table wrapper class
    public static class BlockTableItem {
        private final SimpleStringProperty id;
        private final SimpleStringProperty name;
        private final SimpleStringProperty category;
        private final BlockRepository.BlockData blockData;
        private final boolean enabled;

        public BlockTableItem(BlockRepository.BlockData blockData, boolean enabled) {
            this.blockData = blockData;
            this.id = new SimpleStringProperty(blockData.getId());
            this.name = new SimpleStringProperty(blockData.getName());

            // Extract category from id (e.g. "stone_bricks" -> "stone")
            String cat = "other";
            if (blockData.getId().contains("_")) {
                cat = blockData.getId().substring(0, blockData.getId().indexOf('_'));
            }
            this.category = new SimpleStringProperty(cat);

            this.enabled = enabled;
        }

        public String getId() { return id.get(); }
        public String getName() { return name.get(); }
        public String getCategory() { return category.get(); }
        public BlockRepository.BlockData getBlockData() { return blockData; }
        public boolean isEnabled() { return enabled; }

        public SimpleStringProperty idProperty() { return id; }
        public SimpleStringProperty nameProperty() { return name; }
        public SimpleStringProperty categoryProperty() { return category; }
    }

    public BlockLibraryEditor(BlockRepository blockRepository, Stage parentStage) {
        this.blockRepository = blockRepository;
        this.parentStage = parentStage;
        createEditorStage();
    }

    private void createEditorStage() {
        editorStage = new Stage();
        editorStage.setTitle("Block Library Editor");
        editorStage.initModality(Modality.WINDOW_MODAL);
        editorStage.initOwner(parentStage);
        editorStage.setMinWidth(900);
        editorStage.setMinHeight(600);

        BorderPane root = new BorderPane();
        Scene scene = new Scene(root, 900, 600);

        // Apply dark theme - try to load from file first, fallback to inline
        try {
            String css = getClass().getResource("/pixelart/dark-theme.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception e) {
            // Fallback to inline CSS
            scene.getRoot().setStyle(AppStyles.DARK_THEME_CSS);
        }

        // Top section - toolbar
        HBox toolbar = createToolbar();
        root.setTop(toolbar);

        // Left section - block list with search and filter
        VBox leftPane = createBlockListPane();
        root.setLeft(leftPane);

        // Center section - block details and editor
        blockDetailsPane = createBlockDetailsPane();
        root.setCenter(blockDetailsPane);

        editorStage.setScene(scene);
    }

    private HBox createToolbar() {
        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(10));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("preview-container");

        Button importButton = new Button("Import Textures");
        importButton.setOnAction(e -> importTextures());

        Button exportButton = new Button("Export Block List");
        exportButton.setOnAction(e -> exportBlockList());

        Button refreshButton = new Button("Refresh List");
        refreshButton.setOnAction(e -> refreshBlockList());

        ComboBox<String> categoryFilter = new ComboBox<>();
        categoryFilter.setPromptText("Filter by Category");
        categoryFilter.setPrefWidth(150);

        categoryFilter.valueProperty().addListener((obs, oldVal, newVal) -> {
            applyFilters(categoryFilter.getValue(), null);
        });

        TextField searchField = new TextField();
        searchField.setPromptText("Search blocks...");
        searchField.setPrefWidth(200);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            applyFilters(categoryFilter.getValue(), newVal);
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> editorStage.close());

        toolbar.getChildren().addAll(
                importButton,
                exportButton,
                refreshButton,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                categoryFilter,
                searchField,
                spacer,
                closeButton
        );

        return toolbar;
    }

    private VBox createBlockListPane() {
        VBox pane = new VBox(10);
        pane.setPadding(new Insets(10));
        pane.setPrefWidth(400);
        pane.getStyleClass().add("preview-container");

        Label titleLabel = new Label("Block Library");
        titleLabel.getStyleClass().add("title-label");

        // Create block table
        blockTable = new TableView<>();
        blockTable.setEditable(false);
        blockTable.setPlaceholder(new Label("No blocks available"));
        VBox.setVgrow(blockTable, Priority.ALWAYS);

        // Define columns
        TableColumn<BlockTableItem, String> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        idColumn.setPrefWidth(100);

        TableColumn<BlockTableItem, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameColumn.setPrefWidth(150);

        TableColumn<BlockTableItem, String> categoryColumn = new TableColumn<>("Category");
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        categoryColumn.setPrefWidth(100);

        TableColumn<BlockTableItem, Boolean> enabledColumn = new TableColumn<>("Enabled");
        enabledColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleBooleanProperty(cellData.getValue().isEnabled()));
        enabledColumn.setCellFactory(col -> new TableCell<BlockTableItem, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    CheckBox checkBox = new CheckBox();
                    checkBox.setSelected(item);
                    checkBox.setDisable(true); // Read-only in the table
                    setGraphic(checkBox);
                }
            }
        });
        enabledColumn.setPrefWidth(70);

        blockTable.getColumns().addAll(idColumn, nameColumn, categoryColumn, enabledColumn);

        // Add color preview column
        TableColumn<BlockTableItem, Color> colorColumn = new TableColumn<>("Color");
        colorColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleObjectProperty<>(
                        cellData.getValue().getBlockData().getAverageColor()));
        colorColumn.setCellFactory(col -> new TableCell<BlockTableItem, Color>() {
            @Override
            protected void updateItem(Color item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    CustomRect colorRect = new CustomRect(20, 20);
                    colorRect.setFill(item);
                    colorRect.setStroke(Color.GRAY);
                    setGraphic(colorRect);
                }
            }
        });
        colorColumn.setPrefWidth(50);

        blockTable.getColumns().add(0, colorColumn); // Add as first column

        // Initialize block list
        allBlocks = FXCollections.observableArrayList();
        filteredBlocks = new FilteredList<>(allBlocks, p -> true);
        blockTable.setItems(filteredBlocks);

        // Add selection listener
        blockTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        selectedBlock = newSelection;
                        updateBlockDetails(selectedBlock);
                    }
                });

        pane.getChildren().addAll(titleLabel, blockTable);

        return pane;
    }

    private VBox createBlockDetailsPane() {
        VBox pane = new VBox(15);
        pane.setPadding(new Insets(15));
        pane.getStyleClass().add("preview-container");

        Label titleLabel = new Label("Block Details");
        titleLabel.getStyleClass().add("title-label");

        // Preview section
        HBox previewBox = new HBox(20);
        previewBox.setAlignment(Pos.CENTER);
        previewBox.setPadding(new Insets(10));

        // Block texture preview
        selectedBlockPreview = new ImageView();
        selectedBlockPreview.setFitWidth(128);
        selectedBlockPreview.setFitHeight(128);
        selectedBlockPreview.setPreserveRatio(true);

        // Color swatch
        VBox colorBox = new VBox(5);
        colorBox.setAlignment(Pos.CENTER);

        CustomRect colorSwatch = new CustomRect(80, 80);
        colorSwatch.setStroke(Color.GRAY);

        Label colorLabel = new Label("Average Color");

        colorBox.getChildren().addAll(colorSwatch, colorLabel);

        previewBox.getChildren().addAll(selectedBlockPreview, colorBox);

        // Properties section
        GridPane propertiesGrid = new GridPane();
        propertiesGrid.setHgap(10);
        propertiesGrid.setVgap(10);
        propertiesGrid.setPadding(new Insets(10));

        // ID (read-only)
        Label idLabel = new Label("Block ID:");
        TextField idField = new TextField();
        idField.setEditable(false);
        propertiesGrid.add(idLabel, 0, 0);
        propertiesGrid.add(idField, 1, 0);

        // Name
        Label nameLabel = new Label("Display Name:");
        nameField = new TextField();
        propertiesGrid.add(nameLabel, 0, 1);
        propertiesGrid.add(nameField, 1, 1);

        // Category
        Label categoryLabel = new Label("Category:");
        TextField categoryField = new TextField();
        categoryField.setEditable(false);
        propertiesGrid.add(categoryLabel, 0, 2);
        propertiesGrid.add(categoryField, 1, 2);

        // Color picker
        Label colorPickerLabel = new Label("Color Override:");
        averageColorPicker = new ColorPicker();
        propertiesGrid.add(colorPickerLabel, 0, 3);
        propertiesGrid.add(averageColorPicker, 1, 3);

        // Weight slider
        Label weightLabel = new Label("Priority Weight:");
        weightSlider = new Slider(0, 10, 5);
        weightSlider.setShowTickLabels(true);
        weightSlider.setShowTickMarks(true);
        weightSlider.setMajorTickUnit(1);
        weightSlider.setMinorTickCount(0);
        weightSlider.setSnapToTicks(true);
        propertiesGrid.add(weightLabel, 0, 4);
        propertiesGrid.add(weightSlider, 1, 4);

        // Enabled checkbox
        enabledCheckbox = new CheckBox("Enable this block for pixel art");
        propertiesGrid.add(enabledCheckbox, 0, 5, 2, 1);

        // Action buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button saveButton = new Button("Save Changes");
        saveButton.setOnAction(e -> saveBlockChanges());

        Button resetButton = new Button("Reset");
        resetButton.setOnAction(e -> updateBlockDetails(selectedBlock));

        buttonBox.getChildren().addAll(resetButton, saveButton);

        // Default values
        idField.setText("No block selected");
        nameField.setText("");
        categoryField.setText("");
        enabledCheckbox.setSelected(false);

        // Make connections for live preview
        averageColorPicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            colorSwatch.setFill(newVal);
        });

        // Disable editing when no block is selected
        blockDetailsPane = new VBox(10,
                titleLabel,
                previewBox,
                new Separator(),
                propertiesGrid,
                buttonBox);
        blockDetailsPane.setDisable(true);

        return blockDetailsPane;
    }

    private void refreshBlockList() {
        if (!blockRepository.isInitialized()) {
            showAlert("Block repository not initialized. Please load textures first.",
                    Alert.AlertType.WARNING);
            return;
        }

        // Clear existing items
        allBlocks.clear();

        // Load blocks from repository
        for (BlockRepository.BlockData block : blockRepository.getAllBlocks()) {
            // For now, all blocks are enabled by default
            allBlocks.add(new BlockTableItem(block, true));
        }

        // Update category filter with available categories
        updateCategoryFilter();
    }

    private void updateCategoryFilter() {
        // Extract unique categories
        List<String> categories = allBlocks.stream()
                .map(BlockTableItem::getCategory)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        // Update the category filter ComboBox
        ComboBox<String> categoryFilter = (ComboBox<String>)
                ((HBox) editorStage.getScene().getRoot().getChildrenUnmodifiable().get(0))
                        .getChildren().get(4);

        categoryFilter.getItems().clear();
        categoryFilter.getItems().add("All"); // Add "All" option
        categoryFilter.getItems().addAll(categories);
    }

    private void applyFilters(String category, String searchText) {
        filteredBlocks.setPredicate(block -> {
            boolean matchesCategory = category == null || category.equals("All") ||
                    block.getCategory().equals(category);

            boolean matchesSearch = searchText == null || searchText.isEmpty() ||
                    block.getName().toLowerCase().contains(searchText.toLowerCase()) ||
                    block.getId().toLowerCase().contains(searchText.toLowerCase());

            return matchesCategory && matchesSearch;
        });
    }

    private void updateBlockDetails(BlockTableItem item) {
        if (item == null) {
            blockDetailsPane.setDisable(true);
            return;
        }

        blockDetailsPane.setDisable(false);

        // Update UI elements with block data
        BlockRepository.BlockData blockData = item.getBlockData();

        // Update ID field (index 2 is the GridPane, first row first column is ID label)
        GridPane grid = (GridPane) blockDetailsPane.getChildren().get(3);
        TextField idField = (TextField) grid.getChildren().get(1);
        idField.setText(blockData.getId());

        // Update name field
        nameField.setText(blockData.getName());

        // Update category field
        TextField categoryField = (TextField) grid.getChildren().get(3);
        categoryField.setText(item.getCategory());

        // Update color picker
        averageColorPicker.setValue(blockData.getAverageColor());

        // Update weight slider based on property if available
        Object weightProp = blockData.getProperty("weight");
        double weight = weightProp != null ? ((Number) weightProp).doubleValue() : 5.0;
        weightSlider.setValue(weight);

        // Update enabled checkbox based on item
        enabledCheckbox.setSelected(item.isEnabled());

        // Update preview image
        selectedBlockPreview.setImage(blockData.getTexture());

        // Update color swatch
        HBox previewBox = (HBox) blockDetailsPane.getChildren().get(1);
        VBox colorBox = (VBox) previewBox.getChildren().get(1);
        CustomRect colorSwatch = (CustomRect) colorBox.getChildren().get(0);
        colorSwatch.setFill(blockData.getAverageColor());
    }

    private void saveBlockChanges() {
        if (selectedBlock == null) return;

        BlockRepository.BlockData blockData = selectedBlock.getBlockData();

        // Update block name
        blockData.setName(nameField.getText());

        // Update block weight property
        blockData.setProperty("weight", weightSlider.getValue());

        // Update color if overridden
        blockData.setAverageColor(averageColorPicker.getValue());

        // Update enabled status
        blockRepository.setBlockEnabled(blockData.getId(), enabledCheckbox.isSelected());

        // Save all block settings to file
        blockRepository.saveBlockSettings(BLOCK_SETTINGS_FILE);

        // Update the UI
        refreshBlockList();

        showAlert("Changes saved for block: " + blockData.getId(), Alert.AlertType.INFORMATION);
    }

    private void importTextures() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Block Textures Directory");
        File directory = directoryChooser.showDialog(editorStage);

        if (directory != null) {
            blockRepository.initialize(directory.toPath());
            refreshBlockList();
            showAlert("Imported " + blockRepository.getBlockCount() + " block textures.",
                    Alert.AlertType.INFORMATION);
        }
    }

    private void exportBlockList() {
        // Export block list to file
        showAlert("Export functionality not yet implemented", Alert.AlertType.INFORMATION);
    }

    private void applyDarkThemeToDialog(DialogPane dialogPane) {
        try {
            String css = getClass().getResource("/pixelart/dark-theme.css").toExternalForm();
            dialogPane.getStylesheets().add(css);
        } catch (Exception e) {
            // Fallback to inline CSS
            dialogPane.setStyle(AppStyles.DARK_THEME_CSS);
        }
    }

    private void showAlert(String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(alertType == Alert.AlertType.ERROR ? "Error" : "Information");
        alert.setHeaderText(null);
        alert.setContentText(message);

        // Apply dark theme to dialog
        applyDarkThemeToDialog(alert.getDialogPane());

        alert.showAndWait();
    }

    public void show() {
        refreshBlockList();
        editorStage.show();
    }
}