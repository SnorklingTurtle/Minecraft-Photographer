package com.snorklingturtle.photographer;

import com.snorklingturtle.photographer.util.CameraUtil;
import com.snorklingturtle.photographer.util.RaycastUtil;
import com.snorklingturtle.photographer.util.RaycastUtil.RayHit;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.GeneratedStructure;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.NonNull;
import java.awt.Color;
import java.sql.Connection;
import java.util.*;


@SuppressWarnings("deprecation")
public class CameraRenderer {

    // Supersampling grid size — 2 = 2×2 (4 rays/pixel), 3 = 3×3 (9 rays/pixel)
    // Higher = smoother edges but linearly more expensive
    private static final int SUPER_SAMPLING = 2;

    private CameraRenderer() {}

    // Overworld sky colors
    private static final Color[] skyColors = {
            new Color(113, 194, 237),  // Morning
            new Color(52, 113, 227),   // Noon
            new Color(101, 97, 207),   // Night
            new Color(45, 56, 74),     // Midnight
    };

    private static class TraceResult {
        public float[][] RawR;
        public float[][] RawG;
        public float[][] RawB;
    }

    public interface PostEffectCallback {
        Color getColor(Player player, RaycastUtil.RayHit hit, org.bukkit.util.Vector ray, String worldName, long worldTime, int positionY);
    }

    public static void capture(Player player, Photographer plugin) {
        double renderDistance = plugin.getConfig().getInt(Photographer.CONFIG_KEY_RENDER_DISTANCE);

        World world = player.getWorld();
        ItemStack heldItem = player.getInventory().getItemInMainHand();

        TraceResult traceResult = trace(
                world,
                player,
                renderDistance,
                Math.toRadians(CameraUtil.getFieldOfView(heldItem)),
                CameraRenderer::PostProcessing
        );

        // Optional dithering
        byte[] pixels;
        if (CameraUtil.hasDithering(heldItem)) {
            // Floyd–Steinberg dithering → final byte[] pixel buffer
            pixels = ditherToMapPalette(traceResult.RawR, traceResult.RawG, traceResult.RawB);
        } else {
            pixels = new byte[Photographer.MAP_SIZE * Photographer.MAP_SIZE];
            for (int py = 0; py < Photographer.MAP_SIZE; py++) {
                for (int px = 0; px < Photographer.MAP_SIZE; px++) {
                    pixels[py * Photographer.MAP_SIZE + px] = MapPalette.matchColor(
                            clamp(Math.round(traceResult.RawR[px][py])),
                            clamp(Math.round(traceResult.RawG[px][py])),
                            clamp(Math.round(traceResult.RawB[px][py]))
                    );
                }
            }
        }

        // Register renderer and give map to player (main thread) ---
        Bukkit.getScheduler().runTask(plugin, () -> {
            MapView mapView = Bukkit.createMap(world);
            mapView.getRenderers().clear();
            mapView.setScale(MapView.Scale.CLOSE);
            mapView.setLocked(true);
            mapView.setTrackingPosition(false);
            mapView.setUnlimitedTracking(false);

            mapView.addRenderer(new MapRenderer() {
                private boolean done = false;

                @Override
                public void render(MapView view, MapCanvas canvas, Player viewer) {
                    if (done) return;
                    done = true;
                    for (int py = 0; py < Photographer.MAP_SIZE; py++) {
                        for (int px = 0; px < Photographer.MAP_SIZE; px++) {
                            canvas.setPixel(px, py, pixels[py * Photographer.MAP_SIZE + px]);
                        }
                    }
                }
            });

            // Create item
            ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
            MapMeta meta = (MapMeta) mapItem.getItemMeta();
            meta.setMapView(mapView);

            // Prepare text for item
            String biome = player.getLocation().getBlock().getBiome().name();
            String structure = getNearestStructure(player);

            meta.setItemName("§6Photo at ".concat(formatName(structure != null ? structure : biome)));
            ArrayList<String> loreList = new ArrayList<>();
            loreList.add("by ".concat(player.getDisplayName()));
            meta.setLore(loreList);

            // Hide attributes item
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            mapItem.setItemMeta(meta);

            // Add item to inventory
            player.getInventory().addItem(mapItem);

            // Cache ID
            Photographer.cachedMapIDs.add(mapView.getId());

            // Save to database
            Connection dbConnection = Storage.connect(plugin);
            Storage.store(plugin, dbConnection, mapView.getId(), world.getSeed(), pixels, player.getUniqueId(), 1);
            Storage.disconnect(plugin, dbConnection);
        });
    }

    private static Color PostProcessing(Player player, RaycastUtil.RayHit hit, org.bukkit.util.Vector ray, String worldName, long worldTime, int positionY) {
        ItemStack heldItem = player.getInventory().getItemInMainHand();

        Color c;
        if (hit.isSky()) {
            c = getSkyColor(worldName, positionY, worldTime);

            Color sunColor  = renderSun(ray, worldTime);
            Color moonColor = renderMoon(ray, worldTime);
            if (sunColor  != null) c = sunColor;
            if (moonColor != null) c = moonColor;
        } else {
            c = ColorMapping.getBaseColor(hit.material);

            if (hit.passedThroguhMaterial != null)
            {
                Color tintColor = ColorMapping.getBaseColor(hit.passedThroguhMaterial);
                double tintAlpha = RaycastUtil.TRANSPARENT_MATERIALS.get(hit.passedThroguhMaterial);
                c = getTintedColor(c, tintColor, tintAlpha);
            }
            if (CameraUtil.hasShading(heldItem))
            {
                c = shadedColor(c, hit.face);
            }
            if (CameraUtil.hasShading(heldItem))
            {
                c = shadowColor(c, hit.lightLevel);
            }
        }
        return c;
    }

    private static Color getTintedColor(Color base, Color tint, double strength) {
        int r = clamp((int)(base.getRed()   + (tint.getRed()   - base.getRed())   * strength));
        int g = clamp((int)(base.getGreen() + (tint.getGreen() - base.getGreen()) * strength));
        int b = clamp((int)(base.getBlue()  + (tint.getBlue()  - base.getBlue())  * strength));
        return new Color(r, g, b);
    }

    private static TraceResult trace(World world, Player player, double distance, double fieldOfView, PostEffectCallback postEffectCallback) {

        Location eye = player.getEyeLocation();
        org.bukkit.util.Vector forward = eye.getDirection().normalize();

        String worldName = world.getName();
        long worldTime = world.getTime();

        // Build an orthonormal camera basis
        org.bukkit.util.Vector worldUp = new org.bukkit.util.Vector(0, 1, 0);
        org.bukkit.util.Vector right = forward.clone().crossProduct(worldUp).normalize();
        org.bukkit.util.Vector up = right.clone().crossProduct(forward).normalize();

        // Raytrace all pixels and collect raw (shaded) RGB floats ---
        float[][] rawR = new float[Photographer.MAP_SIZE][Photographer.MAP_SIZE];
        float[][] rawG = new float[Photographer.MAP_SIZE][Photographer.MAP_SIZE];
        float[][] rawB = new float[Photographer.MAP_SIZE][Photographer.MAP_SIZE];

        ItemStack heldItem = player.getInventory().getItemInMainHand();
        boolean hasAntiAliasing = CameraUtil.hasAntialiasing(heldItem);

        if (hasAntiAliasing) {
            final double SS_STEP = 1.0 / SUPER_SAMPLING;

            for (int py = 0; py < Photographer.MAP_SIZE; py++) {
                for (int px = 0; px < Photographer.MAP_SIZE; px++) {

                    float accR = 0, accG = 0, accB = 0;

                    for (int sy = 0; sy < SUPER_SAMPLING; sy++) {
                        for (int sx = 0; sx < SUPER_SAMPLING; sx++) {

                            // Offset each sub-ray within the pixel (0.5 centres the grid)
                            double subX = (sx + 0.5) * SS_STEP - 0.5;
                            double subY = (sy + 0.5) * SS_STEP - 0.5;

                            double ndcX = ((px + subX) / (Photographer.MAP_SIZE - 1.0)) * 2.0 - 1.0;
                            double ndcY = -(((py + subY) / (Photographer.MAP_SIZE - 1.0)) * 2.0 - 1.0);

                            double halfFov = fieldOfView / 2.0;
                            org.bukkit.util.Vector ray = forward.clone()
                                    .add(right.clone().multiply(Math.tan(halfFov) * ndcX))
                                    .add(up.clone().multiply(Math.tan(halfFov) * ndcY))
                                    .normalize();

                            RayHit hit = RaycastUtil.cast(eye, ray, distance);

                            // Do sun, moon, shading and shadows
                            Color c = postEffectCallback.getColor(player, hit, ray, worldName, worldTime, py);

                            accR += c.getRed();
                            accG += c.getGreen();
                            accB += c.getBlue();
                        }
                    }

                    // Average across all sub-rays
                    int samples = SUPER_SAMPLING * SUPER_SAMPLING;
                    rawR[px][py] = accR / samples;
                    rawG[px][py] = accG / samples;
                    rawB[px][py] = accB / samples;
                }
            }
        } else {
            for (int py = 0; py < Photographer.MAP_SIZE; py++) {
                for (int px = 0; px < Photographer.MAP_SIZE; px++) {
                    // NDC from −1 to +1
                    double ndcX = (px / (Photographer.MAP_SIZE - 1.0)) * 2.0 - 1.0;
                    double ndcY = -((py / (Photographer.MAP_SIZE - 1.0)) * 2.0 - 1.0); // flip Y (screen coords)

                    double halfFov = fieldOfView / 2.0;
                    org.bukkit.util.Vector ray = forward.clone()
                            .add(right.clone().multiply(Math.tan(halfFov) * ndcX))
                            .add(up.clone().multiply(Math.tan(halfFov) * ndcY))
                            .normalize();

                    RayHit hit = RaycastUtil.cast(eye, ray, distance);

                    // Do sun, moon, shading and shadows
                    Color c = postEffectCallback.getColor(player, hit, ray, worldName, worldTime, py);

                    rawR[px][py] = c.getRed();
                    rawG[px][py] = c.getGreen();
                    rawB[px][py] = c.getBlue();
                }
            }
        }

        TraceResult traceResult = new TraceResult();
        traceResult.RawR = rawR;
        traceResult.RawG = rawG;
        traceResult.RawB = rawB;

        return traceResult;
    }


    private static String getNearestStructure(Player player) {
        String[] structuresOfInterest = createStructuresOfInterest();

        Chunk chunk = player.getWorld().getChunkAt(player.getLocation());
        Collection<GeneratedStructure> structures = chunk.getStructures();

        // Pick structure
        for (GeneratedStructure structure : structures)
        {
            String structureKey = structure.getStructure().getKey().toString();
            String matchingStructureName = String.valueOf(
                    Arrays.stream(structuresOfInterest).filter(structureKey::contains).findFirst().orElse(null)
            );
            if (matchingStructureName != null)
            {
                return matchingStructureName;
            }
        }

        return null;
    }

    private static @NonNull String[] createStructuresOfInterest() {
         return new String[]{
                // "buried_treasure"; // Leaving this out - to avoid spoiling :P
                "village",
                "igloo",
                "nether_fossil",
                "ocean_ruin",
                "ruined_portal",
                "desert_pyramid",
                "jungle_pyramid",
                "pillager_outpost",
                "monument",
                "shipwreck",
                "mansion",
                "mineshaft",
                "trial_chambers",
                "ancient_city",
                "bastion",
                "end_city",
                "fortress",
                "stronghold",
            };
    }

    private static String formatName(String name) {
        return capitalize(name.replace("_", " "));
    }

    private static String capitalize(String words) {
        char[] array = words.toLowerCase().toCharArray();
        boolean capitalizeNext = true;

        for (int i = 0; i < array.length; i++) {
            if (Character.isWhitespace(array[i])) {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                array[i] = Character.toUpperCase(array[i]);
                capitalizeNext = false;
            }
        }
        return new String(array);
    }

    // Directional shading
    private static Color shadedColor(Color base, BlockFace face) {
        double factor = shadeFactor(face);
        return new Color(
                clamp((int)(base.getRed()   * factor)),
                clamp((int)(base.getGreen() * factor)),
                clamp((int)(base.getBlue()  * factor))
        );
    }

    private static double shadeFactor(BlockFace face) {
        return switch (face) {
            case UP           -> 1.00;
            case SOUTH, NORTH  -> 0.80;
            case EAST,  WEST   -> 0.65;
            case DOWN        -> 0.50;
            default            -> 0.85;
        };
    }

    private static Color shadowColor(Color color, double lightLevel) {
        // Minimum ambient factor so even unlit surfaces retain some color.
        // Maybe adjust MIN_AMBIENT (0.08 ≈ a barely visible surface).
        final double MIN_AMBIENT = 0.2;
        double factor = MIN_AMBIENT + (1.0 - MIN_AMBIENT) * (Math.max(0, lightLevel) / 15.0);
        return new Color(
                clamp((int)(color.getRed()   * factor)),
                clamp((int)(color.getGreen() * factor)),
                clamp((int)(color.getBlue()  * factor))
        );
    }

    // Floyd–Steinberg dithering
    /**
     * Applies Floyd–Steinberg error diffusion to the raw float RGB buffers,
     * quantising each pixel to the nearest Minecraft map palette color.
     *
     * Error is spread to four neighbours:
     *
     *          [current]  →  7/16
     *   3/16    5/16       1/16
     *
     * The pixel buffer is stored in row-major order: index = py * MAP_SIZE + px.
     */
    private static byte[] ditherToMapPalette(float[][] rBuf, float[][] gBuf, float[][] bBuf) {
        // Work on mutable copies so we can accumulate error in-place
        float[][] r = deepCopy(rBuf);
        float[][] g = deepCopy(gBuf);
        float[][] b = deepCopy(bBuf);

        byte[] out = new byte[Photographer.MAP_SIZE * Photographer.MAP_SIZE];

        for (int py = 0; py < Photographer.MAP_SIZE; py++) {
            for (int px = 0; px < Photographer.MAP_SIZE; px++) {
                // Clamp accumulated value to valid byte range
                int ri = clamp(Math.round(r[px][py]));
                int gi = clamp(Math.round(g[px][py]));
                int bi = clamp(Math.round(b[px][py]));

                // Quantise to nearest map palette color
                byte mapByte = MapPalette.matchColor(ri, gi, bi);
                out[py * Photographer.MAP_SIZE + px] = mapByte;

                // Recover the actual RGB the palette chose
                Color actual = MapPalette.getColor(mapByte);
                float errR = ri - actual.getRed();
                float errG = gi - actual.getGreen();
                float errB = bi - actual.getBlue();

                // Distribute error to right neighbour (7/16)
                if (px + 1 < Photographer.MAP_SIZE) {
                    r[px+1][py]   += errR * (7f/16f);
                    g[px+1][py]   += errG * (7f/16f);
                    b[px+1][py]   += errB * (7f/16f);
                }
                // Distribute error to bottom-left neighbour (3/16)
                if (px - 1 >= 0 && py + 1 < Photographer.MAP_SIZE) {
                    r[px-1][py+1] += errR * (3f/16f);
                    g[px-1][py+1] += errG * (3f/16f);
                    b[px-1][py+1] += errB * (3f/16f);
                }
                // Distribute error to bottom neighbour (5/16)
                if (py + 1 < Photographer.MAP_SIZE) {
                    r[px][py+1]   += errR * (5f/16f);
                    g[px][py+1]   += errG * (5f/16f);
                    b[px][py+1]   += errB * (5f/16f);
                }
                // Distribute error to bottom-right neighbour (1/16)
                if (px + 1 < Photographer.MAP_SIZE && py + 1 < Photographer.MAP_SIZE) {
                    r[px+1][py+1] += errR * (1f/16f);
                    g[px+1][py+1] += errG * (1f/16f);
                    b[px+1][py+1] += errB * (1f/16f);
                }
            }
        }

        return out;
    }

    // Helpers
    private static float[][] deepCopy(float[][] src) {
        float[][] copy = new float[src.length][];
        for (int i = 0; i < src.length; i++) {
            copy[i] = src[i].clone();
        }
        return copy;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private static Color getSkyColor(String worldName, int y, long time) {
        Color dayColor = skyColors[1];
        if (worldName == null) return dayColor;
        if (worldName.contains("end")) return new Color(36, 20, 61);
        if (worldName.contains("nether")) return new Color(44, 7, 7);

        Color interpolated = interpolateColor((int)time);
        return getSkyGradientColor(interpolated, y, time > 12000 ? 128 : 190);
    }

    private static Color getSkyGradientColor(Color color, int y, float maxBrightness)
    {
        float brightnessFactor = y / Math.min(maxBrightness, 255);
        int red = (int)(color.getRed() + (255 - color.getRed()) * brightnessFactor);
        int green = (int)(color.getGreen() + (255 - color.getGreen()) * brightnessFactor);
        int blue = (int)(color.getBlue() + (255 - color.getBlue()) * brightnessFactor);
        return new Color(
                Math.min(red, 255),
                Math.min(green, 255),
                Math.min(blue, 255)
        );
    }

    public static Color interpolateColor(int value) {
        value = Math.min(Math.max(value, 0), 24000);

        // Determine the index of the first color
        int index = (int) ((double) value / 6000);

        // Calculate the fractional part of the value within the range of each color segment
        double fraction = (double) (value % 6000) / 6000;

        // Determine the two colors to interpolate between
        Color color1 = skyColors[index];
        Color color2 = skyColors[(index + 1) % skyColors.length];

        // Perform linear interpolation between the two colors
        int red = (int) (color1.getRed() + fraction * (color2.getRed() - color1.getRed()));
        int green = (int) (color1.getGreen() + fraction * (color2.getGreen() - color1.getGreen()));
        int blue = (int) (color1.getBlue() + fraction * (color2.getBlue() - color1.getBlue()));

        return new Color(red, green, blue);
    }

    private static Color renderSun(Vector ray, long worldTime) {
        double angleOffset = Math.toRadians(5); // adjust to fix timing
        double angle = (worldTime / 24000.0) * 2 * Math.PI + angleOffset;

        Vector sunDir = new Vector(Math.cos(angle), Math.sin(angle), 0).normalize();

        double dot = ray.dot(sunDir);
        if (dot <= 0) return null; // sun is behind the camera

        // Build two axes perpendicular to the sun direction
        Vector worldUp  = new Vector(0, 1, 0);
        Vector sunRight = sunDir.clone().crossProduct(worldUp).normalize();
        Vector sunUp    = sunRight.clone().crossProduct(sunDir).normalize();

        // Angular offset of the ray along each axis
        double hAngle = Math.asin(Math.abs(ray.clone().subtract(sunDir.clone().multiply(dot)).dot(sunRight)));
        double vAngle = Math.asin(Math.abs(ray.clone().subtract(sunDir.clone().multiply(dot)).dot(sunUp)));

        double halfSize = Math.toRadians(6.0);
        if (hAngle > halfSize || vAngle > halfSize) return null;

        return new Color(255, 249, 230);
    }

    private static Color renderMoon(Vector ray, long worldTime) {
        double angleOffset = Math.toRadians(5); // keep in sync with sun
        double angle = (worldTime / 24000.0) * 2 * Math.PI + angleOffset;

        Vector moonDir = new Vector(-Math.cos(angle), -Math.sin(angle), 0).normalize();

        double dot = ray.dot(moonDir);
        if (dot <= 0) return null;

        Vector worldUp   = new Vector(0, 1, 0);
        Vector moonRight = moonDir.clone().crossProduct(worldUp).normalize();
        Vector moonUp    = moonRight.clone().crossProduct(moonDir).normalize();

        double hAngle = Math.asin(Math.abs(ray.clone().subtract(moonDir.clone().multiply(dot)).dot(moonRight)));
        double vAngle = Math.asin(Math.abs(ray.clone().subtract(moonDir.clone().multiply(dot)).dot(moonUp)));

        double halfSize = Math.toRadians(3.5);
        if (hAngle > halfSize || vAngle > halfSize) return null;

        return new Color(220, 220, 255);
    }
}
