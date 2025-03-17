import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class BlockRepository {

    private Path currentTexturesPath;
    private final Map<String, BlockData> blocks = new ConcurrentHashMap<>();
    private List<BlockData> sortedBlocks = new ArrayList<>();
    private boolean initialized = false;

    public Path getCurrentTexturesPath() {
        return currentTexturesPath;
    }

    // Track enabled/disabled blocks
    private final Set<String> disabledBlocks = new HashSet<>();

    // Class to hold block information
    public static class BlockData implements Comparable<BlockData> {
        private final String id;
        private String name;
        private final Image texture;
        private Color averageColor;
        private final Map<String, Object> properties = new HashMap<>();

        public BlockData(String id, String name, Image texture, Color averageColor) {
            this.id = id;
            this.name = name;
            this.texture = texture;
            this.averageColor = averageColor;

            // Default properties
            this.properties.put("weight", 5.0);
            this.properties.put("category", extractCategory(id));
        }

        private String extractCategory(String id) {
            if (id.contains("_")) {
                return id.substring(0, id.indexOf('_'));
            }
            return "other";
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Image getTexture() { return texture; }
        public Color getAverageColor() { return averageColor; }
        public void setAverageColor(Color color) { this.averageColor = color; }

        public void setProperty(String key, Object value) {
            properties.put(key, value);
        }

        public Object getProperty(String key) {
            return properties.get(key);
        }

        @Override
        public int compareTo(BlockData other) {
            // Sort by category first, then name
            String thisCategory = (String) this.getProperty("category");
            String otherCategory = (String) other.getProperty("category");

            int categoryCompare = thisCategory.compareTo(otherCategory);
            if (categoryCompare != 0) {
                return categoryCompare;
            }

            return this.name.compareTo(other.name);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BlockData blockData = (BlockData) o;
            return id.equals(blockData.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return name + " (" + id + ")";
        }
    }

    public BlockRepository() {
        // Will load blocks when initialized
    }

    public void initialize(Path texturesDirectory) {
        if (texturesDirectory == null) {
            throw new IllegalArgumentException("Textures directory cannot be null");
        }

        currentTexturesPath = texturesDirectory;

        // Clear existing blocks if reloading
        blocks.clear();
        disabledBlocks.clear();

        // Load block textures from the provided directory
        File dir = texturesDirectory.toFile();
        if (!dir.exists() || !dir.isDirectory()) {
            throw new IllegalArgumentException("Invalid textures directory: " + texturesDirectory);
        }

        // Process each texture file
        File[] textureFiles = dir.listFiles((d, name) ->
                name.toLowerCase().endsWith(".png"));

        if (textureFiles != null) {
            for (File textureFile : textureFiles) {
                try {
                    // Load texture
                    Image texture = new Image(new FileInputStream(textureFile));

                    // Extract block ID and name from filename
                    String filename = textureFile.getName();
                    String id = filename.substring(0, filename.lastIndexOf('.'));

                    // Format name - replace underscores with spaces and capitalize words
                    String name = Arrays.stream(id.split("_"))
                            .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                            .reduce((a, b) -> a + " " + b)
                            .orElse(id);

                    // Calculate average color
                    Color avgColor = calculateAverageColor(texture);

                    // Create and store block data
                    BlockData blockData = new BlockData(id, name, texture, avgColor);
                    blocks.put(id, blockData);

                } catch (Exception e) {
                    System.err.println("Error loading texture: " + textureFile + ": " + e.getMessage());
                }
            }
        }

        // Create sorted list
        updateSortedList();

        initialized = true;
    }

    private void updateSortedList() {
        sortedBlocks = new ArrayList<>(blocks.values());
        Collections.sort(sortedBlocks);
    }

    private Color calculateAverageColor(Image texture) {
        int width = (int) texture.getWidth();
        int height = (int) texture.getHeight();

        // Use JavaFX PixelReader to access image pixels
        PixelReader pixelReader = texture.getPixelReader();

        double totalR = 0, totalG = 0, totalB = 0;
        int pixelCount = 0;

        // Sample pixels for average color
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = pixelReader.getColor(x, y);

                // Skip transparent pixels
                if (color.getOpacity() > 0.1) {
                    totalR += color.getRed();
                    totalG += color.getGreen();
                    totalB += color.getBlue();
                    pixelCount++;
                }
            }
        }

        // Calculate average, avoiding division by zero
        if (pixelCount > 0) {
            return Color.color(
                    totalR / pixelCount,
                    totalG / pixelCount,
                    totalB / pixelCount
            );
        } else {
            return Color.TRANSPARENT;
        }
    }

    public BlockData getBlock(String id) {
        return blocks.get(id);
    }

    public List<BlockData> getAllBlocks() {
        return new ArrayList<>(sortedBlocks);
    }

    public List<BlockData> getEnabledBlocks() {
        return sortedBlocks.stream()
                .filter(block -> !disabledBlocks.contains(block.getId()))
                .collect(Collectors.toList());
    }

    public void setBlockEnabled(String blockId, boolean enabled) {
        if (enabled) {
            disabledBlocks.remove(blockId);
        } else {
            disabledBlocks.add(blockId);
        }
    }

    public boolean isBlockEnabled(String blockId) {
        return !disabledBlocks.contains(blockId);
    }

    public List<BlockData> getBlocksWithVariety(int maxBlockTypes) {
        List<BlockData> enabledBlocks = getEnabledBlocks();

        // If we want to limit the variety of blocks
        if (maxBlockTypes >= enabledBlocks.size()) {
            return enabledBlocks;
        }

        // Sort blocks by weight (higher weight = higher priority)
        List<BlockData> sortedByWeight = new ArrayList<>(enabledBlocks);
        sortedByWeight.sort((a, b) -> {
            Double weightA = (Double) a.getProperty("weight");
            Double weightB = (Double) b.getProperty("weight");
            if (weightA == null) weightA = 5.0;
            if (weightB == null) weightB = 5.0;
            return weightB.compareTo(weightA); // Higher weights first
        });

        // Take the top blocks according to max variety
        return sortedByWeight.subList(0, maxBlockTypes);
    }

    public BlockData findClosestBlock(Color targetColor, List<BlockData> blocksToConsider, double sensitivity) {
        BlockData closest = null;
        double minDistance = Double.MAX_VALUE;

        for (BlockData block : blocksToConsider) {
            Color blockColor = block.getAverageColor();

            // Calculate color distance
            double distance = calculateColorDistance(targetColor, blockColor);

            // Apply sensitivity factor - higher sensitivity means stricter matching
            distance *= sensitivity;

            if (distance < minDistance) {
                minDistance = distance;
                closest = block;
            }
        }

        return closest;
    }

    private double calculateColorDistance(Color c1, Color c2) {
        // Use LAB color space for better perceptual distance
        // This is a simplified approximation - a full LAB conversion would be better
        double dr = c1.getRed() - c2.getRed();
        double dg = c1.getGreen() - c2.getGreen();
        double db = c1.getBlue() - c2.getBlue();

        // Weight green more heavily as human eyes are more sensitive to it
        return Math.sqrt(dr*dr*0.3 + dg*dg*0.59 + db*db*0.11);
    }

    public int getBlockCount() {
        return blocks.size();
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void updateBlockData(String blockId, String name, Color color, double weight) {
        BlockData block = blocks.get(blockId);
        if (block != null) {
            block.setName(name);
            if (color != null) {
                block.setAverageColor(color);
            }
            block.setProperty("weight", weight);
        }
    }

    public Set<String> getCategories() {
        Set<String> categories = new HashSet<>();
        for (BlockData block : blocks.values()) {
            String category = (String) block.getProperty("category");
            if (category != null) {
                categories.add(category);
            }
        }
        return categories;
    }

    // Add this to BlockRepository.java
    public void saveBlockSettings(String filePath) {
        try (FileOutputStream fos = new FileOutputStream(filePath);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {

            // Create a map to store all block settings
            Map<String, Map<String, Object>> blockSettings = new HashMap<>();

            // For each block, store its settings
            for (BlockData block : blocks.values()) {
                Map<String, Object> settings = new HashMap<>();
                settings.put("properties", new HashMap<>(block.properties));
                settings.put("enabled", !disabledBlocks.contains(block.getId()));

                blockSettings.put(block.getId(), settings);
            }

            // Write the map to file
            oos.writeObject(blockSettings);

        } catch (IOException e) {
            System.err.println("Error saving block settings: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public void loadBlockSettings(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) return;

        try (FileInputStream fis = new FileInputStream(file);
             ObjectInputStream ois = new ObjectInputStream(fis)) {

            // Read the map from file
            Map<String, Map<String, Object>> blockSettings =
                    (Map<String, Map<String, Object>>) ois.readObject();

            // Apply settings to each block
            for (String blockId : blockSettings.keySet()) {
                BlockData block = blocks.get(blockId);
                if (block != null) {
                    Map<String, Object> settings = blockSettings.get(blockId);

                    // Apply properties
                    Map<String, Object> properties = (Map<String, Object>) settings.get("properties");
                    if (properties != null) {
                        block.properties.putAll(properties);
                    }

                    // Apply enabled/disabled state
                    Boolean enabled = (Boolean) settings.get("enabled");
                    if (enabled != null) {
                        setBlockEnabled(blockId, enabled);
                    }
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading block settings: " + e.getMessage());
        }
    }
}