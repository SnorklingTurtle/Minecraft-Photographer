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
