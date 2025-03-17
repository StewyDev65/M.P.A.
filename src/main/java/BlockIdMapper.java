import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class BlockIdMapper {
    // Map to store texture filename to Minecraft block ID mappings
    private static final Map<String, String> blockIdMap = new HashMap<>();

    // Initialize with known mappings
    static {
        // Common block mappings
        // Stone blocks
        blockIdMap.put("stone", "minecraft:stone");
        blockIdMap.put("granite", "minecraft:granite");
        blockIdMap.put("polished_granite", "minecraft:polished_granite");
        blockIdMap.put("diorite", "minecraft:diorite");
        blockIdMap.put("polished_diorite", "minecraft:polished_diorite");
        blockIdMap.put("andesite", "minecraft:andesite");
        blockIdMap.put("polished_andesite", "minecraft:polished_andesite");

        // Dirt blocks
        blockIdMap.put("grass_block", "minecraft:grass_block");
        blockIdMap.put("dirt", "minecraft:dirt");
        blockIdMap.put("coarse_dirt", "minecraft:coarse_dirt");
        blockIdMap.put("podzol", "minecraft:podzol");

        // Wood blocks
        blockIdMap.put("oak_planks", "minecraft:oak_planks");
        blockIdMap.put("spruce_planks", "minecraft:spruce_planks");
        blockIdMap.put("birch_planks", "minecraft:birch_planks");
        blockIdMap.put("jungle_planks", "minecraft:jungle_planks");
        blockIdMap.put("acacia_planks", "minecraft:acacia_planks");
        blockIdMap.put("dark_oak_planks", "minecraft:dark_oak_planks");
        blockIdMap.put("mangrove_planks", "minecraft:mangrove_planks");
        blockIdMap.put("cherry_planks", "minecraft:cherry_planks");
        blockIdMap.put("bamboo_planks", "minecraft:bamboo_planks");

        // Concrete blocks
        blockIdMap.put("white_concrete", "minecraft:white_concrete");
        blockIdMap.put("orange_concrete", "minecraft:orange_concrete");
        blockIdMap.put("magenta_concrete", "minecraft:magenta_concrete");
        blockIdMap.put("light_blue_concrete", "minecraft:light_blue_concrete");
        blockIdMap.put("yellow_concrete", "minecraft:yellow_concrete");
        blockIdMap.put("lime_concrete", "minecraft:lime_concrete");
        blockIdMap.put("pink_concrete", "minecraft:pink_concrete");
        blockIdMap.put("gray_concrete", "minecraft:gray_concrete");
        blockIdMap.put("light_gray_concrete", "minecraft:light_gray_concrete");
        blockIdMap.put("cyan_concrete", "minecraft:cyan_concrete");
        blockIdMap.put("purple_concrete", "minecraft:purple_concrete");
        blockIdMap.put("blue_concrete", "minecraft:blue_concrete");
        blockIdMap.put("brown_concrete", "minecraft:brown_concrete");
        blockIdMap.put("green_concrete", "minecraft:green_concrete");
        blockIdMap.put("red_concrete", "minecraft:red_concrete");
        blockIdMap.put("black_concrete", "minecraft:black_concrete");

        // Wool blocks
        blockIdMap.put("white_wool", "minecraft:white_wool");
        blockIdMap.put("orange_wool", "minecraft:orange_wool");
        blockIdMap.put("magenta_wool", "minecraft:magenta_wool");
        blockIdMap.put("light_blue_wool", "minecraft:light_blue_wool");
        blockIdMap.put("yellow_wool", "minecraft:yellow_wool");
        blockIdMap.put("lime_wool", "minecraft:lime_wool");
        blockIdMap.put("pink_wool", "minecraft:pink_wool");
        blockIdMap.put("gray_wool", "minecraft:gray_wool");
        blockIdMap.put("light_gray_wool", "minecraft:light_gray_wool");
        blockIdMap.put("cyan_wool", "minecraft:cyan_wool");
        blockIdMap.put("purple_wool", "minecraft:purple_wool");
        blockIdMap.put("blue_wool", "minecraft:blue_wool");
        blockIdMap.put("brown_wool", "minecraft:brown_wool");
        blockIdMap.put("green_wool", "minecraft:green_wool");
        blockIdMap.put("red_wool", "minecraft:red_wool");
        blockIdMap.put("black_wool", "minecraft:black_wool");

        // Terracotta blocks
        blockIdMap.put("terracotta", "minecraft:terracotta");
        blockIdMap.put("white_terracotta", "minecraft:white_terracotta");
        blockIdMap.put("orange_terracotta", "minecraft:orange_terracotta");
        blockIdMap.put("magenta_terracotta", "minecraft:magenta_terracotta");
        blockIdMap.put("light_blue_terracotta", "minecraft:light_blue_terracotta");
        blockIdMap.put("yellow_terracotta", "minecraft:yellow_terracotta");
        blockIdMap.put("lime_terracotta", "minecraft:lime_terracotta");
        blockIdMap.put("pink_terracotta", "minecraft:pink_terracotta");
        blockIdMap.put("gray_terracotta", "minecraft:gray_terracotta");
        blockIdMap.put("light_gray_terracotta", "minecraft:light_gray_terracotta");
        blockIdMap.put("cyan_terracotta", "minecraft:cyan_terracotta");
        blockIdMap.put("purple_terracotta", "minecraft:purple_terracotta");
        blockIdMap.put("blue_terracotta", "minecraft:blue_terracotta");
        blockIdMap.put("brown_terracotta", "minecraft:brown_terracotta");
        blockIdMap.put("green_terracotta", "minecraft:green_terracotta");
        blockIdMap.put("red_terracotta", "minecraft:red_terracotta");
        blockIdMap.put("black_terracotta", "minecraft:black_terracotta");

        // Add more mappings as needed...
    }

    /**
     * Get the Minecraft block ID for the given texture filename
     * @param textureFilename The filename of the texture
     * @return The Minecraft block ID
     */
    public static String getBlockId(String textureFilename) {
        // Remove .png extension if present
        String baseName = textureFilename;
        if (baseName.toLowerCase().endsWith(".png")) {
            baseName = baseName.substring(0, baseName.length() - 4);
        }

        // Check if we have a direct mapping
        String blockId = blockIdMap.get(baseName.toLowerCase());
        if (blockId != null) {
            return blockId;
        }

        // If no direct mapping, apply some heuristics

        // 1. Try adding "minecraft:" prefix
        if (!baseName.contains(":")) {
            return "minecraft:" + baseName.toLowerCase();
        }

        // 2. If all else fails, return the base name as is
        return baseName.toLowerCase();
    }

    /**
     * Import block ID mappings from a file
     * @param file The file containing mappings
     * @throws IOException If an I/O error occurs
     */
    public static void importMappings(File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue; // Skip empty lines and comments
                }

                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    String textureFilename = parts[0].trim().toLowerCase();
                    String blockId = parts[1].trim();
                    blockIdMap.put(textureFilename, blockId);
                }
            }
        }
    }

    /**
     * Export current block ID mappings to a file
     * @param file The file to export to
     * @throws IOException If an I/O error occurs
     */
    public static void exportMappings(File file) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("# Texture Filename to Minecraft Block ID Mappings");
            writer.println("# Format: texture_filename=minecraft:block_id");
            writer.println();

            blockIdMap.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> writer.println(entry.getKey() + "=" + entry.getValue()));
        }
    }

    /**
     * Update the BlockRepository with the correct Minecraft block IDs
     * @param repository The BlockRepository to update
     */
    public static void updateBlockRepository(BlockRepository repository) {
        for (BlockRepository.BlockData blockData : repository.getAllBlocks()) {
            String origId = blockData.getId();
            String minecraftId = getBlockId(origId);

            // Store the Minecraft ID as a property
            blockData.setProperty("minecraftId", minecraftId);
        }
    }
}