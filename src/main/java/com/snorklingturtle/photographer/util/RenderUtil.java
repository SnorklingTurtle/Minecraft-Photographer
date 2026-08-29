package com.snorklingturtle.photographer.util;

import com.snorklingturtle.photographer.ColorPalette;
import com.snorklingturtle.photographer.Photographer;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.awt.*;
import java.util.zip.DataFormatException;


public class RenderUtil {

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
                                itemFrameColor.darker().darker()
                        );
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }

    private static void drawFrame(MapCanvas canvas, Color colorOuter, Color colorInner) {
        int thickness = 8;

        double[] pattern = {
                0.90, 0.90, 0.90, 0.90, 0.90, 0.90, 0.90, 0.90,
                0.95, 0.95, 0.95, 0.95, 0.95, 0.95, 0.95, 0.95,
                1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00,
                1.10, 1.10, 1.10, 1.10, 1.10, 1.10, 1.10, 1.10,
                1.15, 1.15, 1.15, 1.15, 1.15, 1.15, 1.15, 1.15,
                1.15, 1.15, 1.15, 1.15, 1.15, 1.15, 1.15, 1.15,
                1.10, 1.10, 1.10, 1.10, 1.10, 1.10, 1.10, 1.10,
                1.05, 1.05, 1.05, 1.05, 1.05, 1.05, 1.05, 1.05,
                1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00,
                0.95, 0.95, 0.95, 0.95, 0.95, 0.95, 0.95, 0.95,
                0.90, 0.90, 0.90, 0.90, 0.90, 0.90, 0.90, 0.90,
                0.85, 0.85, 0.85, 0.85, 0.85, 0.85, 0.85, 0.85,
                0.90, 0.90, 0.90, 0.90, 0.90, 0.90, 0.90, 0.90,
                0.95, 0.95, 0.95, 0.95, 0.95, 0.95, 0.95, 0.95,
                1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00,
                1.05, 1.05, 1.05, 1.05, 1.05, 1.05, 1.05, 1.05,

                1.10, 1.10, 1.10, 1.10, 1.10, 1.10, 1.10, 1.10,
                1.15, 1.15, 1.15, 1.15, 1.15, 1.15, 1.15, 1.15,
                1.15, 1.15, 1.15, 1.15, 1.15, 1.15, 1.15, 1.15,
                1.10, 1.10, 1.10, 1.10, 1.10, 1.10, 1.10, 1.10,
                1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00,
                0.95, 0.95, 0.95, 0.95, 0.95, 0.95, 0.95, 0.95,
                0.90, 0.90, 0.90, 0.90, 0.90, 0.90, 0.90, 0.90,
                0.85, 0.85, 0.85, 0.85, 0.85, 0.85, 0.85, 0.85,
                0.85, 0.85, 0.85, 0.85, 0.85, 0.85, 0.85, 0.85,
                0.90, 0.90, 0.90, 0.90, 0.90, 0.90, 0.90, 0.90,
                0.95, 0.95, 0.95, 0.95, 0.95, 0.95, 0.95, 0.95,
                1.05, 1.05, 1.05, 1.05, 1.05, 1.05, 1.05, 1.05,
                1.10, 1.10, 1.10, 1.10, 1.10, 1.10, 1.10, 1.10,
                1.15, 1.15, 1.15, 1.15, 1.15, 1.15, 1.15, 1.15,
                1.10, 1.10, 1.10, 1.10, 1.10, 1.10, 1.10, 1.10,
                1.05, 1.05, 1.05, 1.05, 1.05, 1.05, 1.05, 1.05
        };

        for (int i = 0; i < Photographer.MAP_SIZE; i++) {
            byte topColor    = tintedFrameColor(colorOuter, pattern[i % pattern.length]);
            byte bottomColor = tintedFrameColor(colorOuter, pattern[(i + 16) % pattern.length]);

            for (int t = 0; t < thickness; t++) {
                canvas.setPixel(i, t, topColor);
                canvas.setPixel(i, Photographer.MAP_SIZE - 1 - t, bottomColor);

                if (i >= thickness && i < Photographer.MAP_SIZE - thickness) {
                    byte leftColor   = tintedFrameColor(colorOuter, pattern[(i + 32) % pattern.length]);
                    byte rightColor  = tintedFrameColor(colorOuter, pattern[(i + 64) % pattern.length]);
                    canvas.setPixel(t, i, leftColor);
                    canvas.setPixel(Photographer.MAP_SIZE - 1 - t, i, rightColor);

                    byte topInner    = tintedFrameColor(colorInner, pattern[i % pattern.length]);
                    byte bottomInner = tintedFrameColor(colorInner, pattern[(i + 16) % pattern.length]);
                    canvas.setPixel(i, thickness + t, topInner);
                    canvas.setPixel(i, Photographer.MAP_SIZE - 1 - thickness - t, bottomInner);

                    if (i >= (thickness * 2) && i < Photographer.MAP_SIZE - (thickness * 2)) {
                        byte leftInner   = tintedFrameColor(colorInner, pattern[(i + 32) % pattern.length]);
                        byte rightInner  = tintedFrameColor(colorInner, pattern[(i + 64) % pattern.length]);
                        canvas.setPixel(thickness + t, i, leftInner);
                        canvas.setPixel(Photographer.MAP_SIZE - 1 - thickness - t, i, rightInner);
                    }
                }
            }
        }
    }

    private static byte tintedFrameColor(Color base, double brightness) {
        return MapPalette.matchColor(new Color(
                clamp((int)(base.getRed()   * brightness)),
                clamp((int)(base.getGreen() * brightness)),
                clamp((int)(base.getBlue()  * brightness))
        ));
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    public static int toBytes(Color color) {
        return color.getRGB() & 0xFFFFFF;
    }

    public static Color toColor(int color) {
        return new Color(color);
    }
}
