package com.jykito.voltexnuclear.client;

import com.jykito.voltexnuclear.content.NuclearBlocks;
import com.jykito.voltexnuclear.content.NuclearItems;
import com.jykito.voltexnuclear.content.NuclearSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.SectionPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.function.Predicate;

/**
 * The Geiger counter in hand (DESIGN step 1): it finds the nearest uranium ore within {@link #RADIUS} blocks and clicks
 * the faster the closer it is. All on the client, from the chunks the client already has, so the server does nothing.
 * Each loader calls {@link #tick} once per client tick.
 */
public final class GeigerClient {

    private GeigerClient() {}

    /** How far the counter hears, blocks. */
    public static final int RADIUS = 32;
    /** The search runs this often, ticks (half a second). */
    private static final int SCAN_EVERY = 10;

    private static int cooldown;
    /** Distance to the nearest uranium ore, or -1 when there is none within the radius. */
    private static double nearest = -1;

    public static boolean holding(Player player) {
        return player.getMainHandItem().is(NuclearItems.GEIGER_COUNTER.get()) || player.getOffhandItem().is(NuclearItems.GEIGER_COUNTER.get());
    }

    public static void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.isPaused() || !holding(player)) {
            nearest = -1;
            cooldown = 0;
            return;
        }
        if (--cooldown <= 0) {
            cooldown = SCAN_EVERY;
            nearest = scan(mc.level, player.getX(), player.getY(), player.getZ());
        }
        // clicks come at random, as with a real counter: the chance per tick follows the rate
        if (player.getRandom().nextDouble() < clicksPerSecond() / 20.0) {
            mc.level.playLocalSound(player.getX(), player.getY(), player.getZ(), NuclearSounds.GEIGER_CLICK.get(), SoundSource.PLAYERS,
                    0.4F, 0.9F + player.getRandom().nextFloat() * 0.2F, false);
        }
    }

    /** Signal from 0 to 1: 1 right on the ore, 0 at the edge of the radius or with nothing around. */
    public static double strength() {
        return nearest < 0 ? 0 : Math.max(0, 1 - nearest / RADIUS);
    }

    /** A rare background click with nothing around, up to about 18 a second right on the ore; steep near the ore. */
    static double clicksPerSecond() {
        double s = strength();
        return 0.2 + 18 * s * s * s;
    }

    /** Distance to the nearest uranium ore; sections without it are skipped by their palette, without looking at blocks. */
    private static double scan(Level level, double px, double py, double pz) {
        Block ore = NuclearBlocks.URANIUM_ORE.get(), deep = NuclearBlocks.DEEPSLATE_URANIUM_ORE.get();
        Predicate<BlockState> uranium = state -> state.is(ore) || state.is(deep);
        int bx = (int) Math.floor(px), by = (int) Math.floor(py), bz = (int) Math.floor(pz);
        double best = (double) RADIUS * RADIUS;
        boolean found = false;
        for (int cx = (bx - RADIUS) >> 4; cx <= (bx + RADIUS) >> 4; cx++) {
            for (int cz = (bz - RADIUS) >> 4; cz <= (bz + RADIUS) >> 4; cz++) {
                LevelChunk chunk = level.getChunk(cx, cz);
                LevelChunkSection[] sections = chunk.getSections();
                for (int i = 0; i < sections.length; i++) {
                    LevelChunkSection section = sections[i];
                    int baseY = SectionPos.sectionToBlockCoord(level.getSectionYFromSectionIndex(i));
                    if (baseY + 15 < by - RADIUS || baseY > by + RADIUS || section.hasOnlyAir() || !section.maybeHas(uranium)) continue;
                    for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                        if (!uranium.test(section.getBlockState(x, y, z))) continue;
                        double dx = (cx << 4) + x + 0.5 - px, dy = baseY + y + 0.5 - py, dz = (cz << 4) + z + 0.5 - pz;
                        double d2 = dx * dx + dy * dy + dz * dz;
                        if (d2 <= best) { best = d2; found = true; }
                    }
                }
            }
        }
        return found ? Math.sqrt(best) : -1;
    }
}
