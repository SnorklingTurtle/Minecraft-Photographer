package com.snorklingturtle.photographer.util;

import com.snorklingturtle.photographer.ColorPalette;
import com.snorklingturtle.photographer.Photographer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.awt.*;
import java.util.Map;
import java.util.zip.DataFormatException;

import static java.util.Map.entry;

public class RenderUtil {

    // TODO: Take color for mapping instead
    public static final Map<Material, Color> DYES = Map.ofEntries(
            entry(Material.WHITE_DYE, new Color(249, 255, 254)),
            entry(Material.LIGHT_GRAY_DYE, new Color(157, 157, 151)),
            entry(Material.GRAY_DYE, new Color(71, 79, 82)),
            entry(Material.BLACK_DYE, new Color(29, 29, 33)),
            entry(Material.BROWN_DYE, new Color(131, 84, 50)),
            entry(Material.RED_DYE, new Color(176, 46, 38)),
            entry(Material.ORANGE_DYE, new Color(249, 128, 29)),
            entry(Material.YELLOW_DYE, new Color(254, 216, 61)),
            entry(Material.LIME_DYE, new Color(128, 199, 31)),
            entry(Material.GREEN_DYE, new Color(94, 124, 22)),
            entry(Material.CYAN_DYE, new Color(22, 156, 156)),
            entry(Material.LIGHT_BLUE_DYE, new Color(58, 179, 218)),
            entry(Material.BLUE_DYE, new Color(60, 68, 170)),
            entry(Material.PURPLE_DYE, new Color(137, 50, 184)),
            entry(Material.MAGENTA_DYE, new Color(199, 78, 189)),
            entry(Material.PINK_DYE, new Color(243, 139, 170))
    );

    public static MapRenderer photoRender(byte[] mapDataSerialized) {
        return photoRender(mapDataSerialized, null, true);
    }

    public static MapRenderer photoRender(byte[] mapDataSerialized, Color itemFrameColor, boolean isInitial) {
        return new MapRenderer() {
            @Override
            public void render(@NonNull MapView mapViewNew, @NonNull MapCanvas mapCanvas, @NonNull Player player) {
                if (isInitial && !Photographer.cachedMapIDs.contains(mapViewNew.getId())) {
                    Photographer.cachedMapIDs.add(mapViewNew.getId());
                }

                try {
                    byte[] pixels;
                    try {
                        pixels = ByteArrayCompression.decompress(mapDataSerialized);
                    } catch (DataFormatException ex) {
                        throw new RuntimeException(ex);
                    }

                    mapViewNew.setLocked(true);
                    mapViewNew.setTrackingPosition(false);
                    mapViewNew.setUnlimitedTracking(false);

                    for (int py = 0; py < Photographer.MAP_SIZE; py++) {
                        for (int px = 0; px < Photographer.MAP_SIZE; px++) {
                            byte colorByte = pixels[py * Photographer.MAP_SIZE + px];
                            mapCanvas.setPixelColor(px, py, ColorPalette.getColor(colorByte));
                        }
                    }

                    if (itemFrameColor != null) {
                        drawFrame(mapCanvas,
                                itemFrameColor.darker(),
                                itemFrameColor
                        );
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                //}
            }
        };
    }

    private static void drawFrame(MapCanvas canvas, Color colorOuter, Color colorInner) {
        byte frameColor = MapPalette.matchColor(colorOuter);
        byte innerColor = MapPalette.matchColor(colorInner);
        int thickness = 6;

        for (int i = 0; i < Photographer.MAP_SIZE; i++) {
            for (int t = 0; t < thickness; t++) {
                // Outer edge
                canvas.setPixel(i, t, frameColor);                  // top
                canvas.setPixel(i, Photographer.MAP_SIZE - 1 - t, frameColor);  // bottom
                canvas.setPixel(t, i, frameColor);                  // left
                canvas.setPixel(Photographer.MAP_SIZE - 1 - t, i, frameColor);  // right

                // Inner edge highlight
                if (i < Photographer.MAP_SIZE - thickness)
                {
                    canvas.setPixel(i, thickness + t, innerColor);                 // top inner
                    canvas.setPixel(i, Photographer.MAP_SIZE - 1 - thickness - t, innerColor); // bottom inner
                    canvas.setPixel(thickness + t, i, innerColor);                 // left inner
                    canvas.setPixel(Photographer.MAP_SIZE - 1 - thickness - t, i, innerColor); // right inner
                }
            }
        }
    }

    public static int toBytes(Color color) {
        return color.getRGB() & 0xFFFFFF;
    }

    public static Color toColor(int color) {
        return new Color(color);
    }
}
