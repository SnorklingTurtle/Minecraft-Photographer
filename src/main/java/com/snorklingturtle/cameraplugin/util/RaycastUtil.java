package com.snorklingturtle.cameraplugin.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

public class RaycastUtil {

    private static final double STEP   = 0.05;  // metres per step — smaller = more accurate, slower

    private RaycastUtil() {}

    public static RayHit cast(Location eye, Vector direction, double length) {
        Vector norm  = direction.clone().normalize();
        Vector delta = norm.clone().multiply(STEP);
        Location cur = eye.clone();

        Block prev = null;

        for (double d = 0; d < length; d += STEP) {
            cur.add(delta);
            Block block = cur.getBlock();

            if (isSolid(block.getType())) {
                BlockFace face = determineFace(prev, block);
                int lightLevel = block.getRelative(face).getLightLevel();

                // Hack to avoid black pixels at block edges
                if (lightLevel == 0)
                {
                    lightLevel = block.getRelative(face).getLightFromBlocks();
                }

                return new RayHit(block.getType(), face, d, lightLevel);
            }

            prev = block;
        }

        // Ray escaped into the sky / void
        return new RayHit(null, BlockFace.UP, length, 15);
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
        if (mat == Material.WATER || mat == Material.LAVA) return true;

        return mat.isOccluding() || mat.isSolid();
    }

    // ---------------------------------------------------------------------------

    public static class RayHit {
        public final Material material;
        public final BlockFace face;
        public final double distance;

        public final double lightLevel;

        public RayHit(Material material, BlockFace face, double distance, double lightLevel) {
            this.material = material;
            this.face     = face;
            this.distance = distance;
            this.lightLevel = lightLevel;
        }

        public boolean isSky() {
            return material == null;
        }
    }
}
