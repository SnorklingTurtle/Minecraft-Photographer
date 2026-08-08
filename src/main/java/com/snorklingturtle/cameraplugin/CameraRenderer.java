package com.snorklingturtle.cameraplugin;

import com.snorklingturtle.cameraplugin.util.RaycastUtil;
import com.snorklingturtle.cameraplugin.util.RaycastUtil.RayHit;
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
import org.checkerframework.checker.nullness.qual.NonNull;

import java.awt.Color;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;


@SuppressWarnings("deprecation")
public class CameraRenderer {

    // Supersampling grid size — 2 = 2×2 (4 rays/pixel), 3 = 3×3 (9 rays/pixel)
    // Higher = smoother edges but linearly more expensive
    private static final int SUPER_SAMPLING = 2;

    private CameraRenderer() {}

    public static void capture(Player player, CameraPlugin plugin) {
        double renderDistance = plugin.getConfig().getInt(CameraPlugin.CONFIG_KEY_RENDER_DISTANCE);

        Location eye = player.getEyeLocation();
        org.bukkit.util.Vector forward = eye.getDirection().normalize();

        World world = player.getWorld();

        double fieldOfView = Math.toRadians(plugin.getConfig().getInt(CameraPlugin.CONFIG_KEY_FIELD_OF_VIEW));

        // Build an orthonormal camera basis
        org.bukkit.util.Vector worldUp = new org.bukkit.util.Vector(0, 1, 0);
        org.bukkit.util.Vector right = forward.clone().crossProduct(worldUp).normalize();
        org.bukkit.util.Vector up = right.clone().crossProduct(forward).normalize();

        // --- Step 1: Raytrace all pixels and collect raw (shaded) RGB floats ---
        float[][] rawR = new float[CameraPlugin.MAP_SIZE][CameraPlugin.MAP_SIZE];
        float[][] rawG = new float[CameraPlugin.MAP_SIZE][CameraPlugin.MAP_SIZE];
        float[][] rawB = new float[CameraPlugin.MAP_SIZE][CameraPlugin.MAP_SIZE];

        // TODO: Clean up this if...else
        if (plugin.getConfig().getBoolean(CameraPlugin.CONFIG_KEY_ANTIALIASING)) {
            final double SS_STEP = 1.0 / SUPER_SAMPLING;

            for (int py = 0; py < CameraPlugin.MAP_SIZE; py++) {
                for (int px = 0; px < CameraPlugin.MAP_SIZE; px++) {

                    float accR = 0, accG = 0, accB = 0;

                    for (int sy = 0; sy < SUPER_SAMPLING; sy++) {
                        for (int sx = 0; sx < SUPER_SAMPLING; sx++) {

                            // Offset each sub-ray within the pixel (0.5 centres the grid)
                            double subX = (sx + 0.5) * SS_STEP - 0.5;
                            double subY = (sy + 0.5) * SS_STEP - 0.5;

                            double ndcX = ((px + subX) / (CameraPlugin.MAP_SIZE - 1.0)) * 2.0 - 1.0;
                            double ndcY = -(((py + subY) / (CameraPlugin.MAP_SIZE - 1.0)) * 2.0 - 1.0);

                            double halfFov = fieldOfView / 2.0;
                            org.bukkit.util.Vector ray = forward.clone()
                                    .add(right.clone().multiply(Math.tan(halfFov) * ndcX))
                                    .add(up.clone().multiply(Math.tan(halfFov) * ndcY))
                                    .normalize();

                            RayHit hit = RaycastUtil.cast(eye, ray, renderDistance);

                            Color c;
                            if (hit.isSky()) {
                                c = ColorPalette.getBaseColor(null);
                            } else {
                                c = shadedColor(ColorPalette.getBaseColor(hit.material), hit.face);

                                // if (hit.lightLevel > 0)
                                {
                                    c = shadowColor(c, hit.lightLevel > 0 ? hit.lightLevel : 8);
                                }
                            }

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
            for (int py = 0; py < CameraPlugin.MAP_SIZE; py++) {
                for (int px = 0; px < CameraPlugin.MAP_SIZE; px++) {
                    // NDC from −1 to +1
                    double ndcX = (px / (CameraPlugin.MAP_SIZE - 1.0)) * 2.0 - 1.0;
                    double ndcY = -((py / (CameraPlugin.MAP_SIZE - 1.0)) * 2.0 - 1.0); // flip Y (screen coords)

                    double halfFov = fieldOfView / 2.0;
                    org.bukkit.util.Vector ray = forward.clone()
                            .add(right.clone().multiply(Math.tan(halfFov) * ndcX))
                            .add(up.clone().multiply(Math.tan(halfFov) * ndcY))
                            .normalize();

                    RayHit hit = RaycastUtil.cast(eye, ray, renderDistance);

                    Color c;
                    if (hit.isSky()) {
                        c = ColorPalette.getBaseColor(null);
                    } else {
                        c = shadedColor(ColorPalette.getBaseColor(hit.material), hit.face);

                        // if (hit.lightLevel > 0)
                        {
                            c = shadowColor(c, hit.lightLevel > 0 ? hit.lightLevel : 8);
                        }
                    }

                    rawR[px][py] = c.getRed();
                    rawG[px][py] = c.getGreen();
                    rawB[px][py] = c.getBlue();
                }
            }
        }


        // --- Step 2: Optional dithering
        byte[] pixels;
        if (plugin.getConfig().getBoolean(CameraPlugin.CONFIG_KEY_DITHERING)) {
            // Floyd–Steinberg dithering → final byte[] pixel buffer
            pixels = ditherToMapPalette(rawR, rawG, rawB);
        } else {
            pixels = new byte[CameraPlugin.MAP_SIZE * CameraPlugin.MAP_SIZE];
            for (int py = 0; py < CameraPlugin.MAP_SIZE; py++) {
                for (int px = 0; px < CameraPlugin.MAP_SIZE; px++) {
                    pixels[py * CameraPlugin.MAP_SIZE + px] = MapPalette.matchColor(
                            clamp(Math.round(rawR[px][py])),
                            clamp(Math.round(rawG[px][py])),
                            clamp(Math.round(rawB[px][py]))
                    );
                }
            }
        }

        // --- Step 3: Register renderer and give map to player (main thread) ---
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
                    for (int py = 0; py < CameraPlugin.MAP_SIZE; py++) {
                        for (int px = 0; px < CameraPlugin.MAP_SIZE; px++) {
                            canvas.setPixel(px, py, pixels[py * CameraPlugin.MAP_SIZE + px]);
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

            meta.setItemName("§6Photo at ".concat(formatName(structure != null ? structure : biome) ));
            ArrayList<String> loreList = new ArrayList<>();
            loreList.add("by ".concat(player.getDisplayName()));
            meta.setLore(loreList);

            // Hide attributes item
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            mapItem.setItemMeta(meta);

            // Add item to inventory
            player.getInventory().addItem(mapItem);

            // Save to database
            Connection dbConnection = Storage.connect(plugin);
            Storage.store(plugin, dbConnection, mapView.getId(), world.getSeed(), pixels, player.getUniqueId(), 1);
            Storage.disconnect(plugin, dbConnection);
        });
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
        final double MIN_AMBIENT = 0.08;
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

        byte[] out = new byte[CameraPlugin.MAP_SIZE * CameraPlugin.MAP_SIZE];

        for (int py = 0; py < CameraPlugin.MAP_SIZE; py++) {
            for (int px = 0; px < CameraPlugin.MAP_SIZE; px++) {
                // Clamp accumulated value to valid byte range
                int ri = clamp(Math.round(r[px][py]));
                int gi = clamp(Math.round(g[px][py]));
                int bi = clamp(Math.round(b[px][py]));

                // Quantise to nearest map palette color
                byte mapByte = MapPalette.matchColor(ri, gi, bi);
                out[py * CameraPlugin.MAP_SIZE + px] = mapByte;

                // Recover the actual RGB the palette chose
                Color actual = MapPalette.getColor(mapByte);
                float errR = ri - actual.getRed();
                float errG = gi - actual.getGreen();
                float errB = bi - actual.getBlue();

                // Distribute error to right neighbour (7/16)
                if (px + 1 < CameraPlugin.MAP_SIZE) {
                    r[px+1][py]   += errR * (7f/16f);
                    g[px+1][py]   += errG * (7f/16f);
                    b[px+1][py]   += errB * (7f/16f);
                }
                // Distribute error to bottom-left neighbour (3/16)
                if (px - 1 >= 0 && py + 1 < CameraPlugin.MAP_SIZE) {
                    r[px-1][py+1] += errR * (3f/16f);
                    g[px-1][py+1] += errG * (3f/16f);
                    b[px-1][py+1] += errB * (3f/16f);
                }
                // Distribute error to bottom neighbour (5/16)
                if (py + 1 < CameraPlugin.MAP_SIZE) {
                    r[px][py+1]   += errR * (5f/16f);
                    g[px][py+1]   += errG * (5f/16f);
                    b[px][py+1]   += errB * (5f/16f);
                }
                // Distribute error to bottom-right neighbour (1/16)
                if (px + 1 < CameraPlugin.MAP_SIZE && py + 1 < CameraPlugin.MAP_SIZE) {
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
}
