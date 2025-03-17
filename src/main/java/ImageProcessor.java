import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.util.*;

public class ImageProcessor {

    private final BlockRepository blockRepository;

    // Result data for schematic generation
    private BlockRepository.BlockData[][] resultBlockGrid;

    public ImageProcessor(BlockRepository blockRepository) {
        this.blockRepository = blockRepository;
    }

    public Image processImage(
            Image sourceImage,
            int resolution,
            double sensitivity,
            int blockVariety,
            String algorithm,
            boolean showGrid) {

        if (sourceImage == null) {
            throw new IllegalArgumentException("Source image cannot be null");
        }

        // Calculate dimensions
        double aspectRatio = sourceImage.getWidth() / sourceImage.getHeight();
        int blocksWidth, blocksHeight;

        if (aspectRatio >= 1.0) {
            blocksWidth = resolution;
            blocksHeight = (int) (resolution / aspectRatio);
        } else {
            blocksHeight = resolution;
            blocksWidth = (int) (resolution * aspectRatio);
        }

        // Ensure at least 1 block in each dimension
        blocksWidth = Math.max(1, blocksWidth);
        blocksHeight = Math.max(1, blocksHeight);

        // Initialize result grid
        resultBlockGrid = new BlockRepository.BlockData[blocksHeight][blocksWidth];

        // Get available blocks with variety limit
        List<BlockRepository.BlockData> availableBlocks =
                blockRepository.getBlocksWithVariety(blockVariety);

        // Create a canvas for the result
        int outputWidth = blocksWidth * 16;  // 16 pixels per block texture
        int outputHeight = blocksHeight * 16;
        Canvas canvas = new Canvas(outputWidth, outputHeight);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Process by selected algorithm
        switch (algorithm) {
            case "Average Color":
                processAverageColor(sourceImage, gc, blocksWidth, blocksHeight, availableBlocks, sensitivity);
                break;
            case "Color Matching":
                processColorMatching(sourceImage, gc, blocksWidth, blocksHeight, availableBlocks, sensitivity);
                break;
            case "Dithering":
                processDithering(sourceImage, gc, blocksWidth, blocksHeight, availableBlocks, sensitivity);
                break;
            case "K-Means Clustering":
                processKMeansClustering(sourceImage, gc, blocksWidth, blocksHeight, availableBlocks, sensitivity);
                break;
            case "Edge Preservation":
                processEdgePreservation(sourceImage, gc, blocksWidth, blocksHeight, availableBlocks, sensitivity);
                break;
            case "Full Hybrid":  // Changed from "Hybrid" to "Full Hybrid"
                processHybrid(sourceImage, gc, blocksWidth, blocksHeight, availableBlocks, sensitivity);
                break;
            case "Average with Dithering":  // New algorithm
                processAverageWithDithering(sourceImage, gc, blocksWidth, blocksHeight, availableBlocks, sensitivity);
                break;
            default:
                processAverageColor(sourceImage, gc, blocksWidth, blocksHeight, availableBlocks, sensitivity);
        }

        // Draw grid if requested
        if (showGrid) {
            drawGrid(gc, blocksWidth, blocksHeight);
        }

        // Convert canvas to image
        WritableImage result = new WritableImage(outputWidth, outputHeight);
        canvas.snapshot(null, result);

        return result;
    }

    private void processAverageColor(
            Image sourceImage,
            GraphicsContext gc,
            int blocksWidth,
            int blocksHeight,
            List<BlockRepository.BlockData> availableBlocks,
            double sensitivity) {

        // Calculate scaling factors
        double sourceWidth = sourceImage.getWidth();
        double sourceHeight = sourceImage.getHeight();
        double blockWidth = sourceWidth / blocksWidth;
        double blockHeight = sourceHeight / blocksHeight;

        PixelReader pixelReader = sourceImage.getPixelReader();

        for (int y = 0; y < blocksHeight; y++) {
            for (int x = 0; x < blocksWidth; x++) {
                // Calculate source region for this block
                int sourceX = (int) (x * blockWidth);
                int sourceY = (int) (y * blockHeight);
                int sourceEndX = (int) Math.min(sourceWidth, (x + 1) * blockWidth);
                int sourceEndY = (int) Math.min(sourceHeight, (y + 1) * blockHeight);

                // Calculate average color for this region
                Color avgColor = calculateRegionAverage(
                        pixelReader, sourceX, sourceY, sourceEndX, sourceEndY);

                // Find closest block
                BlockRepository.BlockData block =
                        blockRepository.findClosestBlock(avgColor, availableBlocks, sensitivity);

                // Store result
                resultBlockGrid[y][x] = block;

                // Draw block texture
                if (block != null) {
                    gc.drawImage(block.getTexture(), x * 16, y * 16, 16, 16);
                }
            }
        }
    }

    private void processColorMatching(
            Image sourceImage,
            GraphicsContext gc,
            int blocksWidth,
            int blocksHeight,
            List<BlockRepository.BlockData> availableBlocks,
            double sensitivity) {

        // Calculate scaling factors
        double sourceWidth = sourceImage.getWidth();
        double sourceHeight = sourceImage.getHeight();
        double blockWidth = sourceWidth / blocksWidth;
        double blockHeight = sourceHeight / blocksHeight;

        PixelReader pixelReader = sourceImage.getPixelReader();

        // Enhanced direct color matching that preserves more visual detail
        for (int y = 0; y < blocksHeight; y++) {
            for (int x = 0; x < blocksWidth; x++) {
                // Sample multiple points within the region for better matching
                int sourceX = (int) (x * blockWidth);
                int sourceY = (int) (y * blockHeight);
                int sourceEndX = (int) Math.min(sourceWidth, (x + 1) * blockWidth);
                int sourceEndY = (int) Math.min(sourceHeight, (y + 1) * blockHeight);

                // Create color histogram for the region
                int samples = Math.min(16, (sourceEndX - sourceX) * (sourceEndY - sourceY));
                Color[] sampleColors = new Color[samples];

                // Sample colors strategically across the region
                int sampleIndex = 0;
                for (int sy = 0; sy < Math.sqrt(samples); sy++) {
                    for (int sx = 0; sx < Math.sqrt(samples); sx++) {
                        if (sampleIndex < samples) {
                            int px = sourceX + (int)(sx * (sourceEndX - sourceX) / Math.sqrt(samples));
                            int py = sourceY + (int)(sy * (sourceEndY - sourceY) / Math.sqrt(samples));
                            sampleColors[sampleIndex++] = pixelReader.getColor(px, py);
                        }
                    }
                }

                // Find dominant color using median-cut algorithm (simplified)
                Color dominantColor = findDominantColor(sampleColors);

                // Adjust sensitivity based on the region's color variance
                double adjustedSensitivity = sensitivity * calculateColorVariance(sampleColors);

                // Find closest block with adjusted sensitivity
                BlockRepository.BlockData block =
                        blockRepository.findClosestBlock(dominantColor, availableBlocks, adjustedSensitivity);

                // Store result
                resultBlockGrid[y][x] = block;

                // Draw block texture
                if (block != null) {
                    gc.drawImage(block.getTexture(), x * 16, y * 16, 16, 16);
                }
            }
        }
    }

    // Helper method to find dominant color in a sample set
    private Color findDominantColor(Color[] colors) {
        if (colors.length == 0) return Color.BLACK;

        // Simple approach: sort by perceived brightness and take the median
        Arrays.sort(colors, (c1, c2) -> {
            double brightness1 = 0.299 * c1.getRed() + 0.587 * c1.getGreen() + 0.114 * c1.getBlue();
            double brightness2 = 0.299 * c2.getRed() + 0.587 * c2.getGreen() + 0.114 * c2.getBlue();
            return Double.compare(brightness1, brightness2);
        });

        return colors[colors.length / 2]; // Return the median color
    }

    // Calculate color variance to adjust sensitivity
    private double calculateColorVariance(Color[] colors) {
        if (colors.length <= 1) return 1.0;

        double avgR = 0, avgG = 0, avgB = 0;
        for (Color c : colors) {
            avgR += c.getRed();
            avgG += c.getGreen();
            avgB += c.getBlue();
        }
        avgR /= colors.length;
        avgG /= colors.length;
        avgB /= colors.length;

        double variance = 0;
        for (Color c : colors) {
            variance += Math.pow(c.getRed() - avgR, 2);
            variance += Math.pow(c.getGreen() - avgG, 2);
            variance += Math.pow(c.getBlue() - avgB, 2);
        }

        // Normalize and scale the variance factor between 0.5 and 2.0
        double normalizedVariance = 1.0 + (Math.min(1.0, variance / (3 * colors.length)) - 0.5);
        return normalizedVariance;
    }

    private void processDithering(
            Image sourceImage,
            GraphicsContext gc,
            int blocksWidth,
            int blocksHeight,
            List<BlockRepository.BlockData> availableBlocks,
            double sensitivity) {

        // Calculate scaling factors
        double sourceWidth = sourceImage.getWidth();
        double sourceHeight = sourceImage.getHeight();
        double blockWidth = sourceWidth / blocksWidth;
        double blockHeight = sourceHeight / blocksHeight;

        PixelReader pixelReader = sourceImage.getPixelReader();

        // Create a downscaled image representation for dithering
        double[][][] imageData = new double[blocksHeight][blocksWidth][3]; // RGB channels

        // First pass: populate the image data array with average colors
        for (int y = 0; y < blocksHeight; y++) {
            for (int x = 0; x < blocksWidth; x++) {
                int sourceX = (int) (x * blockWidth);
                int sourceY = (int) (y * blockHeight);
                int sourceEndX = (int) Math.min(sourceWidth, (x + 1) * blockWidth);
                int sourceEndY = (int) Math.min(sourceHeight, (y + 1) * blockHeight);

                // Calculate average color for this region
                Color avgColor = calculateRegionAverage(
                        pixelReader, sourceX, sourceY, sourceEndX, sourceEndY);

                // Store RGB values
                imageData[y][x][0] = avgColor.getRed();
                imageData[y][x][1] = avgColor.getGreen();
                imageData[y][x][2] = avgColor.getBlue();
            }
        }

        // Create a color distance matrix with all available blocks
        // This helps us choose more perceptually accurate colors
        Map<Color, BlockRepository.BlockData> colorMatchCache = new HashMap<>();

        // Second pass: Apply Floyd-Steinberg dithering with color correction
        for (int y = 0; y < blocksHeight; y++) {
            for (int x = 0; x < blocksWidth; x++) {
                // Get current pixel color
                double oldR = imageData[y][x][0];
                double oldG = imageData[y][x][1];
                double oldB = imageData[y][x][2];
                Color oldColor = Color.color(oldR, oldG, oldB);

                // Special handling for off-whites and dark colors to prevent color shift
                boolean isOffWhite = isOffWhite(oldColor);
                boolean isDarkColor = isDarkColor(oldColor);

                // Apply color correction for problematic colors
                double correctedSensitivity = sensitivity;
                List<BlockRepository.BlockData> filteredBlocks = availableBlocks;

                if (isOffWhite || isDarkColor) {
                    // Increase sensitivity for better matching of these problematic colors
                    correctedSensitivity = sensitivity * 0.7;

                    // Filter blocks to consider only appropriate ones
                    filteredBlocks = availableBlocks.stream()
                            .filter(block -> {
                                Color blockColor = block.getAverageColor();
                                if (isOffWhite) {
                                    // For off-whites, consider only white/light gray/cream blocks
                                    // Exclude blues and pinks explicitly
                                    boolean isLightColor = isLightColor(blockColor);
                                    boolean hasBlueOrPink = hasBlueOrPinkTint(blockColor);
                                    return isLightColor && !hasBlueOrPink;
                                } else if (isDarkColor) {
                                    // For dark colors, stick to truly dark blocks
                                    return isDarkColor(blockColor);
                                }
                                return true;
                            })
                            .collect(java.util.stream.Collectors.toList());

                    // If no blocks match our criteria, revert to all blocks
                    if (filteredBlocks.isEmpty()) {
                        filteredBlocks = availableBlocks;
                    }
                }

                // Find the closest block color using cache for performance
                BlockRepository.BlockData block;
                if (colorMatchCache.containsKey(oldColor)) {
                    block = colorMatchCache.get(oldColor);
                } else {
                    block = blockRepository.findClosestBlock(oldColor, filteredBlocks, correctedSensitivity);
                    colorMatchCache.put(oldColor, block);
                }

                // Store result block
                resultBlockGrid[y][x] = block;

                // Draw the block
                if (block != null) {
                    gc.drawImage(block.getTexture(), x * 16, y * 16, 16, 16);

                    // Calculate quantization error with gamma correction
                    Color blockColor = block.getAverageColor();
                    double quantErrorR = oldR - blockColor.getRed();
                    double quantErrorG = oldG - blockColor.getGreen();
                    double quantErrorB = oldB - blockColor.getBlue();

                    // Limit error propagation for problematic colors to prevent artifacts
                    if (isOffWhite || isDarkColor) {
                        // Reduce error diffusion for problematic colors
                        quantErrorR *= 0.5;
                        quantErrorG *= 0.5;
                        quantErrorB *= 0.5;
                    }

                    // Distribute error to neighboring pixels using Floyd-Steinberg pattern
                    distributeError(imageData, x + 1, y, quantErrorR, quantErrorG, quantErrorB, 7.0/16.0, blocksWidth, blocksHeight);
                    distributeError(imageData, x - 1, y + 1, quantErrorR, quantErrorG, quantErrorB, 3.0/16.0, blocksWidth, blocksHeight);
                    distributeError(imageData, x, y + 1, quantErrorR, quantErrorG, quantErrorB, 5.0/16.0, blocksWidth, blocksHeight);
                    distributeError(imageData, x + 1, y + 1, quantErrorR, quantErrorG, quantErrorB, 1.0/16.0, blocksWidth, blocksHeight);
                }
            }
        }
    }

    // Helper methods for color classification
    private boolean isOffWhite(Color color) {
        double brightness = color.getRed() * 0.299 + color.getGreen() * 0.587 + color.getBlue() * 0.114;
        double saturation = Math.max(Math.max(color.getRed(), color.getGreen()), color.getBlue())
                - Math.min(Math.min(color.getRed(), color.getGreen()), color.getBlue());

        // Off-whites are high brightness with low saturation
        return brightness > 0.85 && saturation < 0.1;
    }

    private boolean isDarkColor(Color color) {
        double brightness = color.getRed() * 0.299 + color.getGreen() * 0.587 + color.getBlue() * 0.114;
        // Dark colors have low brightness
        return brightness < 0.25;
    }

    private boolean isLightColor(Color color) {
        double brightness = color.getRed() * 0.299 + color.getGreen() * 0.587 + color.getBlue() * 0.114;
        return brightness > 0.7;
    }

    private boolean hasBlueOrPinkTint(Color color) {
        // Check if blue is significantly higher than red and green (for blue tint)
        boolean hasBlue = color.getBlue() > color.getRed() * 1.2 && color.getBlue() > color.getGreen() * 1.2;

        // Check if red and blue are significantly higher than green (for pink/purple tint)
        boolean hasPink = color.getRed() > color.getGreen() * 1.2 && color.getBlue() > color.getGreen() * 1.2;

        return hasBlue || hasPink;
    }

    private void processKMeansClustering(
            Image sourceImage,
            GraphicsContext gc,
            int blocksWidth,
            int blocksHeight,
            List<BlockRepository.BlockData> availableBlocks,
            double sensitivity) {

        // Calculate scaling factors
        double sourceWidth = sourceImage.getWidth();
        double sourceHeight = sourceImage.getHeight();
        double blockWidth = sourceWidth / blocksWidth;
        double blockHeight = sourceHeight / blocksHeight;

        PixelReader pixelReader = sourceImage.getPixelReader();

        for (int y = 0; y < blocksHeight; y++) {
            for (int x = 0; x < blocksWidth; x++) {
                // Calculate source region for this block
                int sourceX = (int) (x * blockWidth);
                int sourceY = (int) (y * blockHeight);
                int sourceEndX = (int) Math.min(sourceWidth, (x + 1) * blockWidth);
                int sourceEndY = (int) Math.min(sourceHeight, (y + 1) * blockHeight);

                // Sample colors from the region
                List<Color> samples = sampleColors(pixelReader, sourceX, sourceY, sourceEndX, sourceEndY);

                // Find dominant color using K-means
                Color dominantColor = findDominantColorKMeans(samples, 3); // 3 clusters

                // Find closest block with adjusted sensitivity
                double adjustedSensitivity = sensitivity * calculateColorVariance(samples.toArray(new Color[0]));
                BlockRepository.BlockData block =
                        blockRepository.findClosestBlock(dominantColor, availableBlocks, adjustedSensitivity);

                // Store result
                resultBlockGrid[y][x] = block;

                // Draw block texture
                if (block != null) {
                    gc.drawImage(block.getTexture(), x * 16, y * 16, 16, 16);
                }
            }
        }
    }

    // Helper method to sample colors from a region
    private List<Color> sampleColors(PixelReader pixelReader, int startX, int startY, int endX, int endY) {
        List<Color> samples = new ArrayList<>();

        // Calculate how many samples to take (max 25)
        int width = endX - startX;
        int height = endY - startY;
        int sampleCount = Math.min(25, width * height);

        // If region is small, sample every pixel
        if (width * height <= sampleCount) {
            for (int y = startY; y < endY; y++) {
                for (int x = startX; x < endX; x++) {
                    samples.add(pixelReader.getColor(x, y));
                }
            }
        } else {
            // Otherwise, sample in a grid pattern
            int gridSize = (int) Math.sqrt(sampleCount);
            double xStep = width / (double) gridSize;
            double yStep = height / (double) gridSize;

            for (int i = 0; i < gridSize; i++) {
                for (int j = 0; j < gridSize; j++) {
                    int x = startX + (int) (j * xStep + xStep / 2);
                    int y = startY + (int) (i * yStep + yStep / 2);
                    samples.add(pixelReader.getColor(x, y));
                }
            }
        }

        return samples;
    }

    // K-means clustering to find dominant color
    private Color findDominantColorKMeans(List<Color> colors, int k) {
        if (colors.isEmpty()) return Color.BLACK;
        if (colors.size() <= k) return colors.get(0);

        // Check if the colors are predominantly white
        boolean mostlyWhite = colors.stream()
                .filter(c -> c.getRed() > 0.85 && c.getGreen() > 0.85 && c.getBlue() > 0.85)
                .count() > colors.size() * 0.7;

        // For white areas, simplify to just use average color with balanced channels
        if (mostlyWhite) {
            double avgR = 0, avgG = 0, avgB = 0;
            int count = 0;

            for (Color color : colors) {
                avgR += color.getRed();
                avgG += color.getGreen();
                avgB += color.getBlue();
                count++;
            }

            if (count > 0) {
                avgR /= count;
                avgG /= count;
                avgB /= count;

                // Balance the channels to avoid color bias
                double avg = (avgR + avgG + avgB) / 3;
                // Pull 70% toward balanced white to reduce color artifacts
                double balanceFactor = 0.7;
                avgR = avgR * (1 - balanceFactor) + avg * balanceFactor;
                avgG = avgG * (1 - balanceFactor) + avg * balanceFactor;
                avgB = avgB * (1 - balanceFactor) + avg * balanceFactor;

                return Color.color(avgR, avgG, avgB);
            }
        }

        if (colors.isEmpty()) return Color.BLACK;
        if (colors.size() <= k) return colors.get(0);

        // Initialize k centroids randomly
        List<Color> centroids = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < k; i++) {
            centroids.add(colors.get(random.nextInt(colors.size())));
        }

        // Maximum iterations
        int maxIterations = 10;

        for (int iteration = 0; iteration < maxIterations; iteration++) {
            // Assign each color to nearest centroid
            List<List<Color>> clusters = new ArrayList<>();
            for (int i = 0; i < k; i++) {
                clusters.add(new ArrayList<>());
            }

            for (Color color : colors) {
                int nearestCentroid = 0;
                double minDistance = Double.MAX_VALUE;

                for (int i = 0; i < k; i++) {
                    double distance = calculateColorDistanceSquared(color, centroids.get(i));
                    if (distance < minDistance) {
                        minDistance = distance;
                        nearestCentroid = i;
                    }
                }

                clusters.get(nearestCentroid).add(color);
            }

            // Update centroids to cluster average
            boolean centroidsChanged = false;
            for (int i = 0; i < k; i++) {
                List<Color> cluster = clusters.get(i);
                if (cluster.isEmpty()) continue;

                double sumR = 0, sumG = 0, sumB = 0;
                for (Color color : cluster) {
                    sumR += color.getRed();
                    sumG += color.getGreen();
                    sumB += color.getBlue();
                }

                Color newCentroid = Color.color(
                        sumR / cluster.size(),
                        sumG / cluster.size(),
                        sumB / cluster.size()
                );

                if (calculateColorDistanceSquared(newCentroid, centroids.get(i)) > 0.0001) {
                    centroids.set(i, newCentroid);
                    centroidsChanged = true;
                }
            }

            // If centroids didn't change significantly, we've converged
            if (!centroidsChanged) break;
        }

        // Find the largest cluster
        int largestClusterIndex = 0;
        int largestClusterSize = 0;
        List<List<Color>> clusters = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            clusters.add(new ArrayList<>());
        }

        for (Color color : colors) {
            int nearestCentroid = 0;
            double minDistance = Double.MAX_VALUE;

            for (int i = 0; i < k; i++) {
                double distance = calculateColorDistanceSquared(color, centroids.get(i));
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestCentroid = i;
                }
            }

            clusters.get(nearestCentroid).add(color);
        }

        for (int i = 0; i < k; i++) {
            if (clusters.get(i).size() > largestClusterSize) {
                largestClusterSize = clusters.get(i).size();
                largestClusterIndex = i;
            }
        }

        if (mostlyWhite) {
            // Add a check for near-white results to reduce color artifacts
            Color finalColor = centroids.get(largestClusterIndex);
            if (finalColor.getRed() > 0.8 && finalColor.getGreen() > 0.8 && finalColor.getBlue() > 0.8) {
                // Balance channels to reduce color bias
                double avg = (finalColor.getRed() + finalColor.getGreen() + finalColor.getBlue()) / 3;
                double balanceFactor = 0.6;
                return Color.color(
                        finalColor.getRed() * (1 - balanceFactor) + avg * balanceFactor,
                        finalColor.getGreen() * (1 - balanceFactor) + avg * balanceFactor,
                        finalColor.getBlue() * (1 - balanceFactor) + avg * balanceFactor
                );
            }
        }

        return centroids.get(largestClusterIndex);
    }

    // Calculate squared color distance (for faster computation)
    private double calculateColorDistanceSquared(Color c1, Color c2) {
        double dr = c1.getRed() - c2.getRed();
        double dg = c1.getGreen() - c2.getGreen();
        double db = c1.getBlue() - c2.getBlue();

        // Weight green more heavily as human eyes are more sensitive to it
        return dr*dr*0.3 + dg*dg*0.59 + db*db*0.11;
    }

    private void processEdgePreservation(
            Image sourceImage,
            GraphicsContext gc,
            int blocksWidth,
            int blocksHeight,
            List<BlockRepository.BlockData> availableBlocks,
            double sensitivity) {

        // Calculate scaling factors
        double sourceWidth = sourceImage.getWidth();
        double sourceHeight = sourceImage.getHeight();
        double blockWidth = sourceWidth / blocksWidth;
        double blockHeight = sourceHeight / blocksHeight;

        PixelReader pixelReader = sourceImage.getPixelReader();

        // Detect edges in the source image
        double[][] edgeMap = detectEdges(pixelReader, sourceWidth, sourceHeight);

        for (int y = 0; y < blocksHeight; y++) {
            for (int x = 0; x < blocksWidth; x++) {
                // Calculate source region for this block
                int sourceX = (int) (x * blockWidth);
                int sourceY = (int) (y * blockHeight);
                int sourceEndX = (int) Math.min(sourceWidth, (x + 1) * blockWidth);
                int sourceEndY = (int) Math.min(sourceHeight, (y + 1) * blockHeight);

                // Get average edge strength in this region
                double edgeStrength = calculateRegionEdgeStrength(
                        edgeMap, sourceX, sourceY, sourceEndX, sourceEndY);

                // Choose color sampling strategy based on edge strength
                Color chosenColor;
                if (edgeStrength > 0.3) { // High edge content - use dominant color
                    List<Color> samples = sampleColors(pixelReader, sourceX, sourceY, sourceEndX, sourceEndY);
                    chosenColor = findDominantColorKMeans(samples, 2); // Use fewer clusters for edge regions
                } else { // Low edge content - use average color
                    chosenColor = calculateRegionAverage(
                            pixelReader, sourceX, sourceY, sourceEndX, sourceEndY);
                }

                // Adjust sensitivity based on edge strength - be more accurate on edges
                double adjustedSensitivity = sensitivity * (1.0 - edgeStrength * 0.5);

                // Find closest block
                BlockRepository.BlockData block =
                        blockRepository.findClosestBlock(chosenColor, availableBlocks, adjustedSensitivity);

                // Store result
                resultBlockGrid[y][x] = block;

                // Draw block texture
                if (block != null) {
                    gc.drawImage(block.getTexture(), x * 16, y * 16, 16, 16);
                }
            }
        }
    }

    // Detect edges using a simple Sobel operator
    private double[][] detectEdges(PixelReader pixelReader, double sourceWidth, double sourceHeight) {
        int width = (int) sourceWidth;
        int height = (int) sourceHeight;
        double[][] edgeMap = new double[height][width];

        // Skip the border pixels
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                // Compute grayscale values for 3x3 neighborhood
                double[][] gray = new double[3][3];

                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        Color color = pixelReader.getColor(x + kx, y + ky);
                        // Convert to grayscale using perceptual weights
                        gray[ky+1][kx+1] = 0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue();
                    }
                }

                // Apply Sobel operators
                double gx = (gray[0][0] + 2 * gray[0][1] + gray[0][2]) - (gray[2][0] + 2 * gray[2][1] + gray[2][2]);
                double gy = (gray[0][0] + 2 * gray[1][0] + gray[2][0]) - (gray[0][2] + 2 * gray[1][2] + gray[2][2]);

                // Calculate gradient magnitude
                double magnitude = Math.sqrt(gx * gx + gy * gy);

                // Normalize to [0, 1]
                edgeMap[y][x] = Math.min(1.0, magnitude / 2.0);
            }
        }

        return edgeMap;
    }

    // Calculate average edge strength in a region
    private double calculateRegionEdgeStrength(
            double[][] edgeMap, int startX, int startY, int endX, int endY) {

        double totalStrength = 0;
        int count = 0;

        // Ensure we stay within bounds
        int startXSafe = Math.max(0, startX);
        int startYSafe = Math.max(0, startY);
        int endXSafe = Math.min(edgeMap[0].length, endX);
        int endYSafe = Math.min(edgeMap.length, endY);

        for (int y = startYSafe; y < endYSafe; y++) {
            for (int x = startXSafe; x < endXSafe; x++) {
                totalStrength += edgeMap[y][x];
                count++;
            }
        }

        return count > 0 ? totalStrength / count : 0;
    }

    // Enhanced perceptual color distance
    private double calculateColorDistancePerceptual(Color c1, Color c2) {
        // Convert to Lab color space for better perceptual distance
        // This is a simplified conversion to CIE Lab space

        // Step 1: Convert RGB to XYZ
        double r = c1.getRed() > 0.04045 ? Math.pow((c1.getRed() + 0.055) / 1.055, 2.4) : c1.getRed() / 12.92;
        double g = c1.getGreen() > 0.04045 ? Math.pow((c1.getGreen() + 0.055) / 1.055, 2.4) : c1.getGreen() / 12.92;
        double b = c1.getBlue() > 0.04045 ? Math.pow((c1.getBlue() + 0.055) / 1.055, 2.4) : c1.getBlue() / 12.92;

        double r2 = c2.getRed() > 0.04045 ? Math.pow((c2.getRed() + 0.055) / 1.055, 2.4) : c2.getRed() / 12.92;
        double g2 = c2.getGreen() > 0.04045 ? Math.pow((c2.getGreen() + 0.055) / 1.055, 2.4) : c2.getGreen() / 12.92;
        double b2 = c2.getBlue() > 0.04045 ? Math.pow((c2.getBlue() + 0.055) / 1.055, 2.4) : c2.getBlue() / 12.92;

        r *= 100; g *= 100; b *= 100;
        r2 *= 100; g2 *= 100; b2 *= 100;

        // Special handling for whites and darks
        if (isOffWhite(c1) && !isOffWhite(c2)) {
            return 2.0; // Penalize non-whites for off-white colors
        }
        if (isDarkColor(c1) && !isDarkColor(c2)) {
            return 2.0; // Penalize non-darks for dark colors
        }

        // For colors that have been identified as problematic, use a simpler but
        // more reliable RGB distance with higher weights for red/green components
        if (isOffWhite(c1) || isDarkColor(c1)) {
            double dr = c1.getRed() - c2.getRed();
            double dg = c1.getGreen() - c2.getGreen();
            double db = c1.getBlue() - c2.getBlue();

            // Higher weight for red and green to avoid blue tint in whites
            return Math.sqrt(dr*dr*0.4 + dg*dg*0.5 + db*db*0.1);
        }

        // For regular colors, use a standard distance formula
        return calculateColorDistance(c1, c2);
    }

    private void processHybrid(
            Image sourceImage,
            GraphicsContext gc,
            int blocksWidth,
            int blocksHeight,
            List<BlockRepository.BlockData> availableBlocks,
            double sensitivity) {

        // Calculate scaling factors
        double sourceWidth = sourceImage.getWidth();
        double sourceHeight = sourceImage.getHeight();
        double blockWidth = sourceWidth / blocksWidth;
        double blockHeight = sourceHeight / blocksHeight;

        PixelReader pixelReader = sourceImage.getPixelReader();

        // Detect edges for intelligent algorithm selection
        double[][] edgeMap = detectEdges(pixelReader, sourceWidth, sourceHeight);

        // Create a downscaled image representation for dithering
        double[][][] imageData = new double[blocksHeight][blocksWidth][3]; // RGB channels

        // First pass: populate the image data array with appropriate colors
        for (int y = 0; y < blocksHeight; y++) {
            for (int x = 0; x < blocksWidth; x++) {
                // Calculate source region for this block
                int sourceX = (int) (x * blockWidth);
                int sourceY = (int) (y * blockHeight);
                int sourceEndX = (int) Math.min(sourceWidth, (x + 1) * blockWidth);
                int sourceEndY = (int) Math.min(sourceHeight, (y + 1) * blockHeight);

                // Get region data
                double edgeStrength = calculateRegionEdgeStrength(
                        edgeMap, sourceX, sourceY, sourceEndX, sourceEndY);

                // Choose color based on region characteristics
                Color chosenColor;

                if (edgeStrength > 0.3) {
                    // For edge areas - use dominant color
                    List<Color> samples = sampleColors(pixelReader, sourceX, sourceY, sourceEndX, sourceEndY);
                    chosenColor = findDominantColorKMeans(samples, 2);
                } else {
                    // For non-edge areas - use average color
                    chosenColor = calculateRegionAverage(
                            pixelReader, sourceX, sourceY, sourceEndX, sourceEndY);
                }

                // Store RGB values in image data array
                imageData[y][x][0] = chosenColor.getRed();
                imageData[y][x][1] = chosenColor.getGreen();
                imageData[y][x][2] = chosenColor.getBlue();
            }
        }

        // Create a color match cache for performance
        Map<Color, BlockRepository.BlockData> colorMatchCache = new HashMap<>();

        // Second pass: Apply full Floyd-Steinberg dithering
        for (int y = 0; y < blocksHeight; y++) {
            for (int x = 0; x < blocksWidth; x++) {
                // Get current pixel color
                double oldR = imageData[y][x][0];
                double oldG = imageData[y][x][1];
                double oldB = imageData[y][x][2];
                Color oldColor = Color.color(oldR, oldG, oldB);

                // Choose appropriate block
                BlockRepository.BlockData block;

                if (colorMatchCache.containsKey(oldColor)) {
                    // Use cache for performance
                    block = colorMatchCache.get(oldColor);
                } else {
                    // Find closest block
                    block = blockRepository.findClosestBlock(oldColor, availableBlocks, sensitivity);
                    colorMatchCache.put(oldColor, block);
                }

                // Store result
                resultBlockGrid[y][x] = block;

                // Draw block texture
                if (block != null) {
                    gc.drawImage(block.getTexture(), x * 16, y * 16, 16, 16);

                    // Calculate quantization error
                    Color blockColor = block.getAverageColor();
                    double quantErrorR = oldR - blockColor.getRed();
                    double quantErrorG = oldG - blockColor.getGreen();
                    double quantErrorB = oldB - blockColor.getBlue();

                    double ditherStrength = 0.5;
                    quantErrorR *= ditherStrength;
                    quantErrorG *= ditherStrength;
                    quantErrorB *= ditherStrength;

                    // Distribute error using Floyd-Steinberg pattern
                    distributeError(imageData, x + 1, y, quantErrorR, quantErrorG, quantErrorB, 7.0/16.0, blocksWidth, blocksHeight);
                    distributeError(imageData, x - 1, y + 1, quantErrorR, quantErrorG, quantErrorB, 3.0/16.0, blocksWidth, blocksHeight);
                    distributeError(imageData, x, y + 1, quantErrorR, quantErrorG, quantErrorB, 5.0/16.0, blocksWidth, blocksHeight);
                    distributeError(imageData, x + 1, y + 1, quantErrorR, quantErrorG, quantErrorB, 1.0/16.0, blocksWidth, blocksHeight);
                }
            }
        }
    }

    // Modify the distributeError method in the Dithering algorithm
    private void distributeError(double[][][] imageData, int x, int y,
                                 double errorR, double errorG, double errorB,
                                 double factor, int width, int height) {
        // Check if coordinates are valid
        if (x >= 0 && x < width && y >= 0 && y < height) {
            // Add color bias correction for near-white colors
            boolean isNearWhite = imageData[y][x][0] > 0.85 &&
                    imageData[y][x][1] > 0.85 &&
                    imageData[y][x][2] > 0.85;

            // For near-white colors, reduce color error diffusion to avoid color artifacts
            double whiteCorrectionFactor = isNearWhite ? 0.3 : 1.0;

            // Reduce blue bias specifically in light areas
            double blueBias = 0.0;
            if (isNearWhite && errorB > 0) {
                blueBias = Math.min(0.5, errorB) * -0.5;
            }

            // Add weighted error to the pixel with color correction
            imageData[y][x][0] = Math.min(1.0, Math.max(0.0,
                    imageData[y][x][0] + errorR * factor * whiteCorrectionFactor));
            imageData[y][x][1] = Math.min(1.0, Math.max(0.0,
                    imageData[y][x][1] + errorG * factor * whiteCorrectionFactor));
            imageData[y][x][2] = Math.min(1.0, Math.max(0.0,
                    imageData[y][x][2] + errorB * factor * whiteCorrectionFactor + blueBias));

            // Color neutralization for near-white areas to prevent color drift
            if (isNearWhite) {
                // Calculate grayscale value
                double gray = 0.299 * imageData[y][x][0] +
                        0.587 * imageData[y][x][1] +
                        0.114 * imageData[y][x][2];

                // Pull colors back toward grayscale to reduce artifacts
                double neutralizeFactor = 0.7; // Strength of correction
                imageData[y][x][0] = imageData[y][x][0] * (1 - neutralizeFactor) + gray * neutralizeFactor;
                imageData[y][x][1] = imageData[y][x][1] * (1 - neutralizeFactor) + gray * neutralizeFactor;
                imageData[y][x][2] = imageData[y][x][2] * (1 - neutralizeFactor) + gray * neutralizeFactor;
            }
        }
    }

    private Color calculateRegionAverage(
            PixelReader pixelReader,
            int startX, int startY,
            int endX, int endY) {

        double totalR = 0, totalG = 0, totalB = 0;
        int pixelCount = 0;

        for (int y = startY; y < endY; y++) {
            for (int x = startX; x < endX; x++) {
                Color color = pixelReader.getColor(x, y);
                totalR += color.getRed();
                totalG += color.getGreen();
                totalB += color.getBlue();
                pixelCount++;
            }
        }

        if (pixelCount > 0) {
            return Color.color(
                    totalR / pixelCount,
                    totalG / pixelCount,
                    totalB / pixelCount
            );
        } else {
            return Color.BLACK;
        }
    }

    private void processAverageWithDithering(
            Image sourceImage,
            GraphicsContext gc,
            int blocksWidth,
            int blocksHeight,
            List<BlockRepository.BlockData> availableBlocks,
            double sensitivity) {

        // Calculate scaling factors
        double sourceWidth = sourceImage.getWidth();
        double sourceHeight = sourceImage.getHeight();
        double blockWidth = sourceWidth / blocksWidth;
        double blockHeight = sourceHeight / blocksHeight;

        PixelReader pixelReader = sourceImage.getPixelReader();

        // Create a downscaled image representation for dithering
        double[][][] imageData = new double[blocksHeight][blocksWidth][3]; // RGB channels

        // First pass: populate the image data array with average colors
        for (int y = 0; y < blocksHeight; y++) {
            for (int x = 0; x < blocksWidth; x++) {
                int sourceX = (int) (x * blockWidth);
                int sourceY = (int) (y * blockHeight);
                int sourceEndX = (int) Math.min(sourceWidth, (x + 1) * blockWidth);
                int sourceEndY = (int) Math.min(sourceHeight, (y + 1) * blockHeight);

                // Calculate average color for this region - basic approach
                Color avgColor = calculateRegionAverage(
                        pixelReader, sourceX, sourceY, sourceEndX, sourceEndY);

                // Store RGB values
                imageData[y][x][0] = avgColor.getRed();
                imageData[y][x][1] = avgColor.getGreen();
                imageData[y][x][2] = avgColor.getBlue();
            }
        }

        // Create a color match cache for performance
        Map<Color, BlockRepository.BlockData> colorMatchCache = new HashMap<>();

        // Second pass: Apply dithering with 0.5 strength
        for (int y = 0; y < blocksHeight; y++) {
            for (int x = 0; x < blocksWidth; x++) {
                // Get current pixel color
                double oldR = imageData[y][x][0];
                double oldG = imageData[y][x][1];
                double oldB = imageData[y][x][2];
                Color oldColor = Color.color(oldR, oldG, oldB);

                // Find closest block
                BlockRepository.BlockData block;
                if (colorMatchCache.containsKey(oldColor)) {
                    block = colorMatchCache.get(oldColor);
                } else {
                    block = blockRepository.findClosestBlock(oldColor, availableBlocks, sensitivity);
                    colorMatchCache.put(oldColor, block);
                }

                // Store result
                resultBlockGrid[y][x] = block;

                // Draw block texture
                if (block != null) {
                    gc.drawImage(block.getTexture(), x * 16, y * 16, 16, 16);

                    // Calculate quantization error
                    Color blockColor = block.getAverageColor();
                    double quantErrorR = oldR - blockColor.getRed();
                    double quantErrorG = oldG - blockColor.getGreen();
                    double quantErrorB = oldB - blockColor.getBlue();

                    // Apply fixed 0.5 dithering strength
                    double ditherStrength = 0.5;
                    quantErrorR *= ditherStrength;
                    quantErrorG *= ditherStrength;
                    quantErrorB *= ditherStrength;

                    // Distribute error using Floyd-Steinberg pattern
                    distributeError(imageData, x + 1, y, quantErrorR, quantErrorG, quantErrorB, 7.0/16.0, blocksWidth, blocksHeight);
                    distributeError(imageData, x - 1, y + 1, quantErrorR, quantErrorG, quantErrorB, 3.0/16.0, blocksWidth, blocksHeight);
                    distributeError(imageData, x, y + 1, quantErrorR, quantErrorG, quantErrorB, 5.0/16.0, blocksWidth, blocksHeight);
                    distributeError(imageData, x + 1, y + 1, quantErrorR, quantErrorG, quantErrorB, 1.0/16.0, blocksWidth, blocksHeight);
                }
            }
        }
    }

    // Add this method to ImageProcessor.java
    private double calculateColorDistance(Color c1, Color c2) {
        // Use LAB color space for better perceptual distance
        // This is a simplified approximation - a full LAB conversion would be better
        double dr = c1.getRed() - c2.getRed();
        double dg = c1.getGreen() - c2.getGreen();
        double db = c1.getBlue() - c2.getBlue();

        // Weight green more heavily as human eyes are more sensitive to it
        return Math.sqrt(dr*dr*0.3 + dg*dg*0.59 + db*db*0.11);
    }

    private void drawGrid(GraphicsContext gc, int blocksWidth, int blocksHeight) {
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(0.5);

        // Draw vertical lines
        for (int x = 0; x <= blocksWidth; x++) {
            gc.strokeLine(x * 16, 0, x * 16, blocksHeight * 16);
        }

        // Draw horizontal lines
        for (int y = 0; y <= blocksHeight; y++) {
            gc.strokeLine(0, y * 16, blocksWidth * 16, y * 16);
        }
    }

    public BlockRepository.BlockData[][] getResultBlockGrid() {
        return resultBlockGrid;
    }
}