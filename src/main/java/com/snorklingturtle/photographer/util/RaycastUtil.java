package com.snorklingturtle.photographer.util;

import com.snorklingturtle.photographer.Photographer;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.Set;

import static java.util.Map.entry;
import static org.bukkit.Bukkit.getServer;

public class RaycastUtil {

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

    private static final Set<Material> PASSABLE_SOLIDS = Set.of(
            Material.SNOW,
            Material.LAVA
    );

    private static final Set<Material> LIGHT_SOURCES = Set.of(
            Material.LAVA
    );

    public static RayHit cast(Location eye, Vector direction, double length) {
        World world = eye.getWorld();

        Material passedThroughMaterial = null;

        RayTraceResult transparentCheck = world.rayTraceBlocks(
                eye, direction, length,
                FluidCollisionMode.ALWAYS,
                true
        );

        // Determine the start point for the solid check
        Location solidCheckOrigin = eye;
        if (transparentCheck != null && transparentCheck.getHitBlock() != null) {
            Material hitMat = transparentCheck.getHitBlock().getType();
            if (TRANSPARENT_MATERIALS.containsKey(hitMat)) {
                passedThroughMaterial = hitMat;
                if (!transparentCheck.getHitBlock().isLiquid())
                {
                    // Start the solid check just past the transparent block
                    solidCheckOrigin = transparentCheck.getHitBlock().getLocation().add(
                            direction.clone().normalize().multiply(1.1) // step 1.1 blocks into/past it
                    );
                }
            }
        }

        double remainingLength = length - solidCheckOrigin.distance(eye);
        RayTraceResult solidCheck = remainingLength > 0 ? world.rayTraceBlocks(
                solidCheckOrigin, direction, remainingLength,
                FluidCollisionMode.NEVER,
                true
        ) : null;

        // Check for passable blocks we explicitly want to render
        RayTraceResult passableCheck = length > 0 ? world.rayTraceBlocks(
                solidCheckOrigin, direction, length,
                FluidCollisionMode.ALWAYS,
                false // include passable blocks
        ) : null;

        // Only use it if it hit something we actually want
        if (passableCheck != null && passableCheck.getHitBlock() != null
                && PASSABLE_SOLIDS.contains(passableCheck.getHitBlock().getType())) {
            double passableDist = passableCheck.getHitPosition().distance(eye.toVector());
            double solidDist = solidCheck != null && solidCheck.getHitBlock() != null
                    ? solidCheck.getHitPosition().distance(eye.toVector())
                    : Double.MAX_VALUE;
            if (passableDist < solidDist) {
                solidCheck = passableCheck;
            }
        }

        if (solidCheck == null || solidCheck.getHitBlock() == null) {
            return new RayHit(null, BlockFace.UP, length, 15, passedThroughMaterial);
        }

        Block hit = solidCheck.getHitBlock();
        BlockFace face = solidCheck.getHitBlockFace() != null ? solidCheck.getHitBlockFace() : getExactFaceFromRay(direction);
        double dist = solidCheck.getHitPosition().distance(eye.toVector());

        int lightLevel = hit.getRelative(face).getLightLevel();
        if (lightLevel == 0) // Hack to avoid black pixels at block edges
        {
            if (LIGHT_SOURCES.contains(hit.getType()))
            {
                lightLevel = 15;
            }
            else {
                lightLevel = hit.getLightFromSky();
            }
        }

        return new RayHit(hit.getType(), face, dist, lightLevel, passedThroughMaterial);
    }

    private static BlockFace getExactFaceFromRay(Vector rayDirection) {
        double dx = Math.abs(rayDirection.getX());
        double dy = Math.abs(rayDirection.getY());
        double dz = Math.abs(rayDirection.getZ());

        if (dy > dx && dy > dz) {
            return rayDirection.getY() > 0 ? BlockFace.DOWN : BlockFace.UP;
        }
        if (dx > dz) {
            return rayDirection.getX() > 0 ? BlockFace.WEST : BlockFace.EAST;
        }
        return rayDirection.getZ() > 0 ? BlockFace.NORTH : BlockFace.SOUTH;
    }

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
