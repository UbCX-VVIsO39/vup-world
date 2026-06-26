package com.example.vupworld;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteStyleV2ImageQualityTest {
    private static final Path ROUTE_STYLE_DIR = Path.of("图库", "v4");
    private static final int EXPECTED_COUNT = 11;
    private static final int MIN_WIDTH = 1536;
    private static final int MIN_HEIGHT = 900;
    private static final double MIN_ASPECT_RATIO = 1.45;
    private static final double MAX_ASPECT_RATIO = 1.90;
    private static final long MIN_BYTES = 1_000_000L;
    private static final long MAX_BYTES = 6_000_000L;
    private static final double MIN_LUMA_STD = 18.0;
    private static final int MIN_COLOR_BUCKETS = 96;
    private static final int MIN_AVERAGE_HASH_DISTANCE = 64;

    @Test
    void routeStyleV2ImagesAreDecodedDistinctAndProductionSized() throws Exception {
        List<RouteImage> images = routeImagesFromManifest();
        assertEquals(EXPECTED_COUNT, images.size(), "v4 manifest must describe the full route cover set");

        Set<String> hashes = new HashSet<>();
        for (RouteImage image : images) {
            assertFalse(image.file().contains("probe"), image.route() + " should not use probe assets");

            Path imagePath = ROUTE_STYLE_DIR.resolve(image.file());
            assertTrue(Files.isRegularFile(imagePath), image.file() + " must exist");
            long bytes = Files.size(imagePath);
            assertTrue(bytes >= MIN_BYTES, image.file() + " is too small for a generated route cover");
            assertTrue(bytes <= MAX_BYTES, image.file() + " is unexpectedly large");

            BufferedImage decoded = ImageIO.read(imagePath.toFile());
            assertNotNull(decoded, image.file() + " must be a decodable PNG");
            assertTrue(decoded.getWidth() >= MIN_WIDTH, image.file() + " width");
            assertTrue(decoded.getHeight() >= MIN_HEIGHT, image.file() + " height");
            double aspectRatio = (double) decoded.getWidth() / decoded.getHeight();
            assertTrue(aspectRatio >= MIN_ASPECT_RATIO && aspectRatio <= MAX_ASPECT_RATIO,
                    image.file() + " should be a usable landscape cover, aspectRatio=" + aspectRatio);

            ImageStats stats = imageStats(decoded);
            assertTrue(stats.lumaStd() >= MIN_LUMA_STD,
                    image.file() + " is too flat: lumaStd=" + stats.lumaStd());
            assertTrue(stats.colorBuckets() >= MIN_COLOR_BUCKETS,
                    image.file() + " has too few color buckets: " + stats.colorBuckets());

            String sha256 = sha256(imagePath);
            assertTrue(hashes.add(sha256), image.file() + " duplicates another route cover exactly");
        }

        for (int left = 0; left < images.size(); left++) {
            for (int right = left + 1; right < images.size(); right++) {
                int distance = hamming(images.get(left).averageHash(), images.get(right).averageHash());
                assertTrue(distance >= MIN_AVERAGE_HASH_DISTANCE,
                        images.get(left).file() + " and " + images.get(right).file()
                                + " are too visually similar: distance=" + distance);
            }
        }
    }

    private List<RouteImage> routeImagesFromManifest() throws Exception {
        JsonNode root = new ObjectMapper().readTree(ROUTE_STYLE_DIR.resolve("manifest.json").toFile());

        List<RouteImage> images = new ArrayList<>();
        for (JsonNode item : root.path("assets")) {
            String file = item.path("file").asText();
            if (!file.startsWith("routes/") || !file.endsWith("/cover-landscape.png")) {
                continue;
            }
            String route = file.split("/")[1];
            assertFalse(route.isBlank(), "route must be present");

            BufferedImage decoded = ImageIO.read(ROUTE_STYLE_DIR.resolve(file).toFile());
            assertNotNull(decoded, file + " must be decodable before hashing");
            images.add(new RouteImage(route, file, averageHash(decoded)));
        }
        return images;
    }

    private ImageStats imageStats(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
        Set<Integer> buckets = new HashSet<>();
        double sum = 0;
        double sumSquared = 0;

        for (int pixel : pixels) {
            int red = (pixel >> 16) & 0xff;
            int green = (pixel >> 8) & 0xff;
            int blue = pixel & 0xff;
            double luma = luma(red, green, blue);
            sum += luma;
            sumSquared += luma * luma;
            buckets.add(((red / 16) << 8) | ((green / 16) << 4) | (blue / 16));
        }

        double mean = sum / pixels.length;
        double variance = Math.max(0, (sumSquared / pixels.length) - (mean * mean));
        return new ImageStats(Math.sqrt(variance), buckets.size());
    }

    private long[] averageHash(BufferedImage image) {
        int cells = 16;
        int cellWidth = image.getWidth() / cells;
        int cellHeight = image.getHeight() / cells;
        double[] lumaByCell = new double[cells * cells];
        double total = 0;

        for (int cy = 0; cy < cells; cy++) {
            for (int cx = 0; cx < cells; cx++) {
                double sum = 0;
                int count = 0;
                for (int y = cy * cellHeight; y < (cy + 1) * cellHeight; y++) {
                    for (int x = cx * cellWidth; x < (cx + 1) * cellWidth; x++) {
                        int pixel = image.getRGB(x, y);
                        sum += luma((pixel >> 16) & 0xff, (pixel >> 8) & 0xff, pixel & 0xff);
                        count++;
                    }
                }
                double average = sum / count;
                int index = cy * cells + cx;
                lumaByCell[index] = average;
                total += average;
            }
        }

        double globalAverage = total / lumaByCell.length;
        long[] hash = new long[4];
        for (int index = 0; index < lumaByCell.length; index++) {
            if (lumaByCell[index] >= globalAverage) {
                hash[index / Long.SIZE] |= 1L << (index % Long.SIZE);
            }
        }
        return hash;
    }

    private int hamming(long[] left, long[] right) {
        int distance = 0;
        for (int index = 0; index < left.length; index++) {
            distance += Long.bitCount(left[index] ^ right[index]);
        }
        return distance;
    }

    private String sha256(Path path) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path));
        return HexFormat.of().formatHex(digest);
    }

    private double luma(int red, int green, int blue) {
        return 0.2126 * red + 0.7152 * green + 0.0722 * blue;
    }

    private record RouteImage(String route, String file, long[] averageHash) {
    }

    private record ImageStats(double lumaStd, int colorBuckets) {
    }
}
