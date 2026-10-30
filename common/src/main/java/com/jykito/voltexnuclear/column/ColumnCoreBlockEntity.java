package com.jykito.voltexnuclear.column;

import com.jykito.industrialcore.block.custom.MultiblockPortBlock;
import com.jykito.industrialcore.block.entity.EnergyPortBlockEntity;
import com.jykito.industrialcore.block.entity.HeatPortBlockEntity;
import com.jykito.industrialcore.block.entity.ItemFluidPortBlockEntity;
import com.jykito.voltexnuclear.VoltexNuclear;
import com.jykito.voltexnuclear.cascade.CasingRole;
import com.jykito.voltexnuclear.cascade.NuclearCasingBlock;
import com.jykito.voltexnuclear.content.NuclearBlockEntities;
import com.jykito.voltexnuclear.content.NuclearBlocks;
import com.jykito.voltexnuclear.content.NuclearFluids;
import com.jykito.voltexnuclear.core.column.HeavyWaterColumn;
import com.jykito.voltexnuclear.machine.VoltexPorts;
import com.jykito.voltexnuclear.machine.WindowNumbers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The working heavy water column (DESIGN step 13, the Girdler process, variant A). Once a second it looks at its blocks
 * and claims them; every tick it charges its buffer from the energy ports, tops up its water from the input ports, reads
 * the heat of its heat ports as the Voltex late cores do (the sum, not spent) and runs: below the heat threshold it
 * stands; above it the speed is its own (tiers / 8) slowed by the share of the coolant it gets. The coolant comes from the
 * input ports as far as the output ports take it back hot; the heavy water goes straight into the output ports. Fluids
 * move in fractions of a mB a tick, the whole mB when there are some. The numbers are in {@link HeavyWaterColumn}.
 * <p>
 * Saving has a different method on each version, as on the plant: the class declares both spellings.
 */
public class ColumnCoreBlockEntity extends BlockEntity implements MenuProvider, WindowNumbers {

    /** Why the column does not run, for the window. */
    public enum Problem { NONE, NOT_FORMED, NO_HEAT, NO_COOLANT, NO_WATER, OUTPUT_FULL, NO_ENERGY }

    /** Ticks between looks at the structure. */
    private static final int SCAN_EVERY = 20;
    /** The lights stay on this long after the last working tick. */
    private static final int LIGHT_HOLD = 40;
    /** The most water one tick takes from the input ports, mB. */
    private static final int WATER_INTAKE = 1_000;

    private int energy, water;
    /** Heavy water made, mB. */
    private long made;
    /** Coolant drained and not yet used, mB. */
    private double coolantCredit;
    private final HeavyWaterColumn.Flow waterUse = new HeavyWaterColumn.Flow(), heavyOut = new HeavyWaterColumn.Flow();

    // runtime, not saved: found again after loading
    private ColumnStructure.Scan scan = ColumnStructure.Scan.NONE;
    private final Set<BlockPos> claimed = new HashSet<>();
    private int scanTimer, idle = LIGHT_HOLD;
    private Problem problem = Problem.NOT_FORMED;
    private boolean working;
    /** This tick, for the window: the speed against a full column, the heat and the heat ports, the coolant used (mB). */
    private double speed, coolantUsed;
    private int heat, heatPorts, heavyIn, heavyRoom;

    public ColumnCoreBlockEntity(BlockPos pos, BlockState state) {
        super(NuclearBlockEntities.COLUMN_CORE.get(), pos, state);
    }

    // ------------------------------------------------------------------------------------------------------------------
    // The work
    // ------------------------------------------------------------------------------------------------------------------

    void serverTick() {
        if (level == null) return;
        if (--scanTimer <= 0) {
            scanTimer = SCAN_EVERY;
            rescan();
        }
        working = false;
        speed = 0;
        coolantUsed = 0;
        if (!scan.formed()) {
            problem = Problem.NOT_FORMED;
            heat = heatPorts = heavyIn = heavyRoom = 0;
        } else {
            List<ItemFluidPortBlockEntity> inputs = new ArrayList<>(), outputs = new ArrayList<>();
            List<EnergyPortBlockEntity> power = new ArrayList<>();
            long heatSum = 0;
            heatPorts = 0;
            for (BlockPos p : scan.sockets()) {
                BlockEntity e = level.getBlockEntity(p);
                if (e instanceof ItemFluidPortBlockEntity port) {
                    port.linkCore(worldPosition);   // the port is named after this machine in the AE2 terminal
                    // as his late cores: fluid goes out only where a pipe can take it, a port set to fluid or to both
                    if (!port.isOutput()) inputs.add(port);
                    else if (port.acceptsFluids()) outputs.add(port);
                } else if (e instanceof EnergyPortBlockEntity port) {
                    power.add(port);
                } else if (e instanceof HeatPortBlockEntity port) {
                    heatSum += port.getHeat();
                    heatPorts++;
                }
            }
            heat = (int) Math.min(Integer.MAX_VALUE, heatSum);
            charge(power);
            refill(inputs);
            run(inputs, outputs);
            Fluid heavy = NuclearFluids.HEAVY_WATER.get();
            heavyIn = VoltexPorts.fluid(outputs, heavy);
            heavyRoom = VoltexPorts.capacity(outputs, heavy);
        }
        idle = working ? 0 : Math.min(idle + 1, LIGHT_HOLD);
        showLights(idle < LIGHT_HOLD);
    }

    private void charge(List<EnergyPortBlockEntity> power) {
        int want = Math.min(HeavyWaterColumn.INTAKE, HeavyWaterColumn.BUFFER - energy);
        for (EnergyPortBlockEntity port : power) {
            if (want <= 0) break;
            int take = Math.min(want, port.getEnergy().getEnergyStored());
            if (take <= 0) continue;
            port.getEnergy().consumeEnergy(take);
            port.setChanged();
            energy += take;
            want -= take;
        }
    }

    /** Water from the tanks of the input ports into the column's own, as much as it lacks and one tick takes. */
    private void refill(List<ItemFluidPortBlockEntity> inputs) {
        int want = Math.min(WATER_INTAKE, HeavyWaterColumn.WATER_TANK - water);
        for (ItemFluidPortBlockEntity port : inputs) {
            if (want <= 0) break;
            int got = VoltexNuclear.platform().drainPort(port, Fluids.WATER, want);
            water += got;
            want -= got;
        }
    }

    private void run(List<ItemFluidPortBlockEntity> inputs, List<ItemFluidPortBlockEntity> outputs) {
        int tiers = scan.tiers();
        if (heat < HeavyWaterColumn.HEAT_THRESHOLD) { problem = Problem.NO_HEAT; return; }
        Fluid coolant = VoltexNuclear.voltexFluid("coolant"), hot = VoltexNuclear.voltexFluid("hot_coolant");
        Fluid heavy = NuclearFluids.HEAVY_WATER.get();
        // the coolant it can have this tick: what was drained before and what the inputs hold, as far as the outputs take it back hot
        int ask = (int) Math.ceil(HeavyWaterColumn.coolantNeed(tiers)) + 1;
        int coolantIn = VoltexPorts.fluid(inputs, coolant);
        double v = HeavyWaterColumn.speed(tiers, heat, coolantCredit + Math.min(coolantIn, VoltexPorts.room(outputs, hot, ask)));
        if (v <= 0) { problem = coolantIn > 0 ? Problem.OUTPUT_FULL : Problem.NO_COOLANT; return; }
        if (water < Math.ceil(waterUse.owed() + HeavyWaterColumn.water(v))) { problem = Problem.NO_WATER; return; }
        int heavyWant = (int) Math.ceil(heavyOut.owed() + HeavyWaterColumn.heavyWater(v));
        if (VoltexPorts.room(outputs, heavy, heavyWant) < heavyWant) { problem = Problem.OUTPUT_FULL; return; }
        int fe = HeavyWaterColumn.energyPerTick(tiers);
        if (energy < fe) { problem = Problem.NO_ENERGY; return; }

        energy -= fe;
        water -= waterUse.add(HeavyWaterColumn.water(v));
        int out = heavyOut.add(HeavyWaterColumn.heavyWater(v));
        if (out > 0) {
            int left = VoltexPorts.put(outputs, heavy, out);
            heavyOut.giveBack(left);
            made += out - left;
        }
        coolantUsed = HeavyWaterColumn.coolant(tiers, v);
        coolantCredit = VoltexPorts.draw(inputs, coolant, coolantUsed, coolantCredit, hot, outputs);
        speed = v;
        working = true;
        problem = Problem.NONE;
        setChanged();
    }

    // ------------------------------------------------------------------------------------------------------------------
    // The structure
    // ------------------------------------------------------------------------------------------------------------------

    private Direction facing() {
        return getBlockState().getValue(ColumnCoreBlock.FACING);
    }

    private void rescan() {
        scan = ColumnStructure.scan(level, worldPosition, facing());
        Set<BlockPos> now = new HashSet<>();
        for (ColumnStructure.Cell c : scan.cells()) now.add(c.pos());
        for (BlockPos p : claimed) if (!now.contains(p)) release(p);
        claimed.clear();
        claimed.addAll(now);
        boolean lit = getBlockState().getValue(ColumnCoreBlock.LIT);
        for (ColumnStructure.Cell c : scan.cells()) claim(c, lit);
        setCoreState(getBlockState().setValue(ColumnCoreBlock.FORMED, scan.formed()));
    }

    /** The lights of the core and of its casing, changed only when they change (a remesh of the chunk). */
    private void showLights(boolean lit) {
        if (getBlockState().getValue(ColumnCoreBlock.LIT) == lit) return;
        setCoreState(getBlockState().setValue(ColumnCoreBlock.LIT, lit));
        for (ColumnStructure.Cell c : scan.cells()) claim(c, lit);
    }

    private void setCoreState(BlockState want) {
        if (want != getBlockState()) level.setBlock(worldPosition, want, Block.UPDATE_CLIENTS);
    }

    /**
     * Every block becomes its piece of the column (DESIGN step 18): the casing by the kind of its cell and the tiers there
     * are (the top tier carries the cap, the coolant pipe follows the height); a port of the pillar hides, the renderer
     * draws the pillar's piece and the port's square in its place.
     */
    private void claim(ColumnStructure.Cell c, boolean lit) {
        BlockState s = level.getBlockState(c.pos()), want = s;
        if (s.is(NuclearBlocks.NUCLEAR_CASING.get()))
            want = s.setValue(NuclearCasingBlock.ROLE, CasingRole.HEAVY_COLUMN).setValue(NuclearCasingBlock.PIECE, ColumnStructure.piece(c, scan.tiers()))
                    .setValue(NuclearCasingBlock.FACING, facing()).setValue(NuclearCasingBlock.LIT, lit);
        else if (s.getBlock() instanceof MultiblockPortBlock)
            want = s.setValue(MultiblockPortBlock.CONCEALED, true);
        if (want != s) level.setBlock(c.pos(), want, Block.UPDATE_CLIENTS);
    }

    private void release(BlockPos p) {
        if (!level.isLoaded(p)) return;
        BlockState s = level.getBlockState(p), want = s;
        if (s.is(NuclearBlocks.NUCLEAR_CASING.get()) && s.getValue(NuclearCasingBlock.ROLE) == CasingRole.HEAVY_COLUMN)
            want = s.setValue(NuclearCasingBlock.ROLE, CasingRole.NONE).setValue(NuclearCasingBlock.LIT, false).setValue(NuclearCasingBlock.PIECE, 0);
        else if (s.getBlock() instanceof MultiblockPortBlock) want = s.setValue(MultiblockPortBlock.CONCEALED, false);
        if (want != s) level.setBlock(p, want, Block.UPDATE_CLIENTS);
    }

    /** The core is gone: every block it claimed, and every one a fresh look finds, shows again as it is. */
    void dismantle() {
        if (level == null || level.isClientSide) return;
        Set<BlockPos> all = new HashSet<>(claimed);
        for (ColumnStructure.Cell c : ColumnStructure.scan(level, worldPosition, facing()).cells()) all.add(c.pos());
        for (BlockPos p : all) release(p);
        claimed.clear();
    }

    /** Whether this block belongs to the column (on the server). */
    boolean claims(BlockPos p) {
        return claimed.contains(p);
    }

    /** What the renderer may draw: the ports of the pillar. */
    public AABB renderBox() {
        Direction f = facing();
        return new AABB(worldPosition).minmax(new AABB(ColumnStructure.pillar(worldPosition, f, ColumnStructure.PILLAR - 1)));
    }

    /** Forge 1.20.1 culls block entity renderers by this box; on NeoForge 1.21.1 the renderer answers instead. */
    public AABB getRenderBoundingBox() {
        return renderBox();
    }

    // ------------------------------------------------------------------------------------------------------------------
    // The window
    // ------------------------------------------------------------------------------------------------------------------

    /**
     * Indices of the numbers the window gets. Big numbers go in two cells: the client gets 16 bits a cell. The coolant in
     * hundredths of a mB a tick, the speed in thousandths of a full column.
     */
    public static final int D_FORMED = 0, D_TIERS = 1, D_ENERGY = 2, D_WATER = 4, D_HEAT = 5, D_HEAT_PORTS = 7, D_COOL_NEED = 8,
            D_COOL_USED = 9, D_HEAVY = 10, D_HEAVY_ROOM = 12, D_PROBLEM = 14, D_SPEED = 15, D_MADE = 16, D_WORKING = 18, D_COUNT = 19;

    @Override
    public ContainerData windowNumbers() {
        return data;
    }

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            int made32 = (int) Math.min(Integer.MAX_VALUE, made);
            return switch (i) {
                case D_FORMED -> scan.formed() ? 1 : 0;
                case D_TIERS -> scan.tiers();
                case D_ENERGY -> energy & 0xFFFF;
                case D_ENERGY + 1 -> energy >>> 16;
                case D_WATER -> water;
                case D_HEAT -> heat & 0xFFFF;
                case D_HEAT + 1 -> heat >>> 16;
                case D_HEAT_PORTS -> heatPorts;
                case D_COOL_NEED -> (int) Math.round(HeavyWaterColumn.coolantNeed(scan.tiers()) * 100);
                case D_COOL_USED -> (int) Math.round(coolantUsed * 100);
                case D_HEAVY -> heavyIn & 0xFFFF;
                case D_HEAVY + 1 -> heavyIn >>> 16;
                case D_HEAVY_ROOM -> heavyRoom & 0xFFFF;
                case D_HEAVY_ROOM + 1 -> heavyRoom >>> 16;
                case D_PROBLEM -> problem.ordinal();
                case D_SPEED -> (int) Math.round(speed * 1000);
                case D_MADE -> made32 & 0xFFFF;
                case D_MADE + 1 -> made32 >>> 16;
                case D_WORKING -> working ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int i, int v) {}

        @Override
        public int getCount() {
            return D_COUNT;
        }
    };

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ColumnMenu(id, inv, data, ContainerLevelAccess.create(level, worldPosition));
    }

    // ------------------------------------------------------------------------------------------------------------------
    // Saving, in both spellings
    // ------------------------------------------------------------------------------------------------------------------

    private void write(CompoundTag tag) {
        tag.putInt("Energy", energy);
        tag.putInt("Water", water);
        tag.putLong("Made", made);
        tag.putDouble("Coolant", coolantCredit);
        tag.putDouble("WaterOwed", waterUse.owed());
        tag.putDouble("HeavyOwed", heavyOut.owed());
    }

    private void read(CompoundTag tag) {
        energy = Math.max(0, Math.min(HeavyWaterColumn.BUFFER, tag.getInt("Energy")));
        water = Math.max(0, Math.min(HeavyWaterColumn.WATER_TANK, tag.getInt("Water")));
        made = Math.max(0, tag.getLong("Made"));
        double c = tag.getDouble("Coolant");
        coolantCredit = Double.isFinite(c) && c >= 0 ? c : 0;
        waterUse.load(tag.getDouble("WaterOwed"));
        heavyOut.load(tag.getDouble("HeavyOwed"));
    }

    protected void saveAdditional(CompoundTag tag) {
        write(tag);
    }

    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        write(tag);
    }

    public void load(CompoundTag tag) {
        read(tag);
    }

    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        read(tag);
    }
}
