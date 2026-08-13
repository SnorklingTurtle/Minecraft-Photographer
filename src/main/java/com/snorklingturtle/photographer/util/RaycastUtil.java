package com.snorklingturtle.photographer.util;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

import java.util.Map;

import static java.util.Map.entry;

public class RaycastUtil {

    private static final double STEP = 0.05;  // metres per step — smaller = more accurate, slower

    public static final Map<Material, Double> TRANSPARENT_MATERIALS = Map.ofEntries(
            entry(Material.WATER, 0.85d),
            entry(Material.TINTED_GLASS, 0.85),
            entry(Material.GLASS, 0.35),
            entry(Material.GLASS_PANE, 0.35),
            entry(Material.GRAY_STAINED_GLASS, 0.5),
            entry(Material.GRAY_STAINED_GLASS_PANE, 0.5),
            entry(Material.GREEN_STAINED_GLASS, 0.5),
            entry(Material.GREEN_STAINED_GLASS_PANE, 0.5),
            entry(Material.BLACK_STAINED_GLASS, 0.5),
            entry(Material.BLACK_STAINED_GLASS_PANE, 0.5),
            entry(Material.BLUE_STAINED_GLASS, 0.5),
            entry(Material.BLUE_STAINED_GLASS_PANE, 0.5),
            entry(Material.BROWN_STAINED_GLASS, 0.5),
            entry(Material.BROWN_STAINED_GLASS_PANE, 0.5),
            entry(Material.CYAN_STAINED_GLASS, 0.5),
            entry(Material.CYAN_STAINED_GLASS_PANE, 0.5),
            entry(Material.LIGHT_BLUE_STAINED_GLASS, 0.5),
            entry(Material.LIGHT_BLUE_STAINED_GLASS_PANE, 0.5),
            entry(Material.LIGHT_GRAY_STAINED_GLASS, 0.5),
            entry(Material.LIGHT_GRAY_STAINED_GLASS_PANE, 0.5),
            entry(Material.LIME_STAINED_GLASS, 0.5),
            entry(Material.LIME_STAINED_GLASS_PANE, 0.5),
            entry(Material.MAGENTA_STAINED_GLASS, 0.5),
            entry(Material.MAGENTA_STAINED_GLASS_PANE, 0.5),
            entry(Material.ORANGE_STAINED_GLASS, 0.5),
            entry(Material.ORANGE_STAINED_GLASS_PANE, 0.5),
            entry(Material.PINK_STAINED_GLASS, 0.5),
            entry(Material.PINK_STAINED_GLASS_PANE, 0.5),
            entry(Material.PURPLE_STAINED_GLASS, 0.5),
            entry(Material.PURPLE_STAINED_GLASS_PANE, 0.5),
            entry(Material.RED_STAINED_GLASS, 0.5),
            entry(Material.RED_STAINED_GLASS_PANE, 0.5),
            entry(Material.WHITE_STAINED_GLASS, 0.5),
            entry(Material.WHITE_STAINED_GLASS_PANE, 0.5),
            entry(Material.YELLOW_STAINED_GLASS, 0.5),
            entry(Material.YELLOW_STAINED_GLASS_PANE, 0.5)
    );

    private RaycastUtil() {}

    public static RayHit cast(Location eye, Vector direction, double length) {
        Vector norm  = direction.clone().normalize();
        Vector delta = norm.clone().multiply(STEP);
        Location cur = eye.clone();

        Block prev = null;
        Material passedThroguhMaterial = null;

        for (double d = 0; d < length; d += STEP) {
            cur.add(delta);
            Block block = cur.getBlock();

            if (TRANSPARENT_MATERIALS.containsKey(block.getType()))
            {
                passedThroguhMaterial = block.getType();
            }

            if (isSolid(block.getType())) {
                BlockFace face = determineFace(prev, block);
                int lightLevel = block.getRelative(face).getLightLevel();

                // Hack to avoid black pixels at block edges
                if (lightLevel == 0)
                {
                    lightLevel = block.getRelative(face).getLightFromBlocks();
                }

                return new RayHit(block.getType(), face, d, lightLevel, passedThroguhMaterial);
            }

            prev = block;
        }

        // Ray escaped into the sky / void
        return new RayHit(null, BlockFace.UP, length, 15, null);
    }

    // Figure out on which side the ray hit the block
    private static BlockFace determineFace(Block prev, Block hit) {
        if (prev == null) return BlockFace.NORTH;

        int dx = hit.getX() - prev.getX();
        int dy = hit.getY() - prev.getY();
        int dz = hit.getZ() - prev.getZ();

        if (dy > 0) return BlockFace.DOWN; // ray came from below → hit bottom face
        if (dy < 0) return BlockFace.UP;    // ray came from above → hit top face
        if (dx > 0) return BlockFace.WEST;
        if (dx < 0) return BlockFace.EAST;
        if (dz > 0) return BlockFace.NORTH;
        if (dz < 0) return BlockFace.SOUTH;

        return BlockFace.UP;
    }

    private static boolean isSolid(Material mat) {
        if (mat == null || mat == Material.AIR || mat == Material.CAVE_AIR
                || mat == Material.VOID_AIR) return false;

        // Treat water and lava as solid so they show up
        if (mat == Material.LAVA) return true;
        // if (mat == Material.WATER || mat == Material.LAVA) return true;

        if (TRANSPARENT_MATERIALS.containsKey(mat)) return false;

        return mat.isOccluding() || mat.isSolid();
    }

    // ---------------------------------------------------------------------------

    public static class RayHit {
        public final Material material;
        public final BlockFace face;
        public final double distance;
        public final double lightLevel;
        public final Material passedThroguhMaterial;

        public RayHit(Material material, BlockFace face, double distance, double lightLevel, Material passedThroguhMaterial) {
            this.material = material;
            this.face     = face;
            this.distance = distance;
            this.lightLevel = lightLevel;
            this.passedThroguhMaterial = passedThroguhMaterial;
        }

        public boolean isSky() {
            return material == null;
        }
    }
}
