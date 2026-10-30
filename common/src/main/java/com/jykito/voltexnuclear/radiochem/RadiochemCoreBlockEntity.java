package com.jykito.voltexnuclear.radiochem;

import com.jykito.industrialcore.block.custom.MultiblockPortBlock;
import com.jykito.industrialcore.block.entity.EnergyPortBlockEntity;
import com.jykito.industrialcore.block.entity.ItemFluidPortBlockEntity;
import com.jykito.voltexnuclear.VoltexNuclear;
import com.jykito.voltexnuclear.cascade.CasingRole;
import com.jykito.voltexnuclear.cascade.NuclearCasingBlock;
import com.jykito.voltexnuclear.content.NuclearBlockEntities;
import com.jykito.voltexnuclear.content.NuclearBlocks;
import com.jykito.voltexnuclear.content.NuclearItems;
import com.jykito.voltexnuclear.core.radiochem.Radiochem;
import com.jykito.voltexnuclear.core.radiochem.Radiochem.Product;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The working radiochemical plant (DESIGN step 13). Once a second it looks at its blocks and claims them; every tick it
 * charges its buffer from the energy ports, tops up its water from the input ports and runs the cycle: takes one spent
 * rod or assembly from the input ports when it has the water for it and the output ports have room for all it will give,
 * spends energy while the work adds up, then gives the empty rods back and whatever the shares make whole. The room is
 * checked on a copy of the shares, so it is exactly what comes out. The numbers and the shares are in {@link Radiochem}.
 * <p>
 * Saving has a different method on each version: {@code saveAdditional(tag)} and {@code load(tag)} on Forge 1.20.1,
 * {@code saveAdditional(tag, registries)} and {@code loadAdditional(tag, registries)} on NeoForge 1.21.1. The class declares
 * both; each version overrides its own.
 */
public class RadiochemCoreBlockEntity extends BlockEntity implements MenuProvider, WindowNumbers {

    /** Why the plant does not run, for the window. */
    public enum Problem { NONE, NOT_FORMED, NO_RODS, NO_WATER, OUTPUT_FULL, NO_ENERGY }

    /** Ticks between looks at the structure. */
    private static final int SCAN_EVERY = 20;
    /** The lights stay on this long after the last working tick, so a plant waiting for the next rod does not blink. */
    private static final int LIGHT_HOLD = 40;
    /** The most water one tick takes from the input ports, mB. */
    private static final int WATER_INTAKE = 1_000;
    /** Rods in a spent single rod, dual and quad assembly, in the order the plant looks for them. */
    public static final int[] RODS = {1, 2, 4};

    private final Radiochem.Shares shares = new Radiochem.Shares();
    /** Rods in the part under way, 0 between parts. */
    private int cycle;
    private int progress, energy, water, made;

    // the client's animation, kept between frames by the renderer
    public float clientAngle, clientSwing;
    public double clientTime = -1;

    // runtime, not saved: found again after loading
    private RadiochemStructure.Scan scan = RadiochemStructure.Scan.NONE;
    private final Set<BlockPos> claimed = new HashSet<>();
    private int scanTimer, idle = LIGHT_HOLD;
    private Problem problem = Problem.NOT_FORMED;
    private boolean working;
    /** What the ports hold, for the window: the spent parts by size; the empty rods, then the products in their order. */
    private final int[] spentIn = new int[RODS.length], givenOut = new int[Product.values().length + 1];

    public RadiochemCoreBlockEntity(BlockPos pos, BlockState state) {
        super(NuclearBlockEntities.RADIOCHEM_CORE.get(), pos, state);
    }

    /** The spent part of this size: single, dual, quad. */
    public static Item spent(int size) {
        return (size == 0 ? NuclearItems.SPENT_FUEL_ROD : size == 1 ? NuclearItems.SPENT_FUEL_ROD_DUAL : NuclearItems.SPENT_FUEL_ROD_QUAD).get();
    }

    public static Item product(Product p) {
        return (switch (p) {
            case LOW_ENRICHED -> NuclearItems.LOW_ENRICHED_URANIUM;
            case PLUTONIUM -> NuclearItems.PLUTONIUM;
            case DEPLETED -> NuclearItems.DEPLETED_URANIUM;
            case PALLADIUM -> NuclearItems.PALLADIUM;
        }).get();
    }

    public static Item emptyRod() {
        return VoltexNuclear.voltexItem("empty_fuel_rod");
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
        if (!scan.formed()) {
            problem = Problem.NOT_FORMED;
        } else {
            List<ItemFluidPortBlockEntity> inputs = new ArrayList<>(), outputs = new ArrayList<>();
            List<EnergyPortBlockEntity> power = new ArrayList<>();
            for (BlockPos p : scan.sockets()) {
                BlockEntity e = level.getBlockEntity(p);
                if (e instanceof ItemFluidPortBlockEntity port) {
                    port.linkCore(worldPosition);   // the port is named after this machine in the AE2 terminal
                    (port.isOutput() ? outputs : inputs).add(port);
                } else if (e instanceof EnergyPortBlockEntity port) {
                    power.add(port);
                }
            }
            charge(power);
            refill(inputs);
            run(inputs, outputs);
            for (int i = 0; i < RODS.length; i++) spentIn[i] = VoltexPorts.count(inputs, spent(i));
            givenOut[0] = VoltexPorts.count(outputs, emptyRod());
            for (Product p : Product.values()) givenOut[p.ordinal() + 1] = VoltexPorts.count(outputs, product(p));
        }
        idle = working ? 0 : Math.min(idle + 1, LIGHT_HOLD);
        showLights(idle < LIGHT_HOLD);
        setCoreState(getBlockState().setValue(RadiochemCoreBlock.LEVEL, Math.round(8f * water / Radiochem.WATER_TANK)));
    }

    private void charge(List<EnergyPortBlockEntity> power) {
        int want = Math.min(Radiochem.INTAKE, Radiochem.BUFFER - energy);
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

    /** Water from the tanks of the input ports into the plant's own, as much as it lacks and one tick takes. */
    private void refill(List<ItemFluidPortBlockEntity> inputs) {
        int want = Math.min(WATER_INTAKE, Radiochem.WATER_TANK - water);
        for (ItemFluidPortBlockEntity port : inputs) {
            if (want <= 0) break;
            int got = VoltexNuclear.platform().drainPort(port, Fluids.WATER, want);
            water += got;
            want -= got;
        }
    }

    private void run(List<ItemFluidPortBlockEntity> inputs, List<ItemFluidPortBlockEntity> outputs) {
        if (cycle == 0) {
            int size = -1;
            for (int i = 0; i < RODS.length && size < 0; i++) if (VoltexPorts.count(inputs, spent(i)) > 0) size = i;
            if (size < 0) { problem = Problem.NO_RODS; return; }
            int rods = RODS[size];
            if (water < Radiochem.water(rods)) { problem = Problem.NO_WATER; return; }
            if (!VoltexPorts.fits(outputs, gives(rods, false))) { problem = Problem.OUTPUT_FULL; return; }
            VoltexPorts.take(inputs, spent(size), 1);
            water -= Radiochem.water(rods);
            cycle = rods;
            progress = 0;
            setChanged();
        }
        if (energy < Radiochem.FE_PER_TICK) { problem = Problem.NO_ENERGY; return; }
        energy -= Radiochem.FE_PER_TICK;
        progress++;
        problem = Problem.NONE;
        working = true;
        if (progress >= Radiochem.ticks(cycle)) {
            // the room was checked when the part was taken, on these very shares
            for (ItemStack s : gives(cycle, true)) VoltexPorts.put(outputs, s);
            made += cycle;
            cycle = 0;
            progress = 0;
        }
        setChanged();
    }

    /**
     * What a part of this many rods gives: its empty rods and what the shares make whole. {@code commit} adds the rods to
     * the shares; without it the look is taken on a copy and changes nothing.
     */
    private List<ItemStack> gives(int rods, boolean commit) {
        Radiochem.Shares s = shares;
        if (!commit) {
            s = new Radiochem.Shares();
            s.load(shares.save());
        }
        int[] whole = s.add(rods);
        List<ItemStack> list = new ArrayList<>();
        list.add(new ItemStack(emptyRod(), rods));
        for (Product p : Product.values()) list.add(new ItemStack(product(p), whole[p.ordinal()]));
        return list;
    }

    // ------------------------------------------------------------------------------------------------------------------
    // The structure
    // ------------------------------------------------------------------------------------------------------------------

    private Direction facing() {
        return getBlockState().getValue(RadiochemCoreBlock.FACING);
    }

    private void rescan() {
        scan = RadiochemStructure.scan(level, worldPosition, facing());
        Set<BlockPos> now = new HashSet<>();
        for (RadiochemStructure.Cell c : scan.cells()) now.add(c.pos());
        for (BlockPos p : claimed) if (!now.contains(p)) release(p);
        claimed.clear();
        claimed.addAll(now);
        boolean lit = getBlockState().getValue(RadiochemCoreBlock.LIT);
        for (RadiochemStructure.Cell c : scan.cells()) claim(c, lit);
        setCoreState(getBlockState().setValue(RadiochemCoreBlock.FORMED, scan.formed()));
    }

    /** The lights of the core and of its casing, changed only when they change (a remesh of the chunk). */
    private void showLights(boolean lit) {
        if (getBlockState().getValue(RadiochemCoreBlock.LIT) == lit) return;
        setCoreState(getBlockState().setValue(RadiochemCoreBlock.LIT, lit));
        for (RadiochemStructure.Cell c : scan.cells()) claim(c, lit);
    }

    private void setCoreState(BlockState want) {
        if (want != getBlockState()) level.setBlock(worldPosition, want, Block.UPDATE_CLIENTS);
    }

    /**
     * Every block becomes its piece of the plant (DESIGN step 18): the casing by the number of its cell, the glass by its
     * pane; a port hides, the renderer draws the casing's piece and the port's mark in its place.
     */
    private void claim(RadiochemStructure.Cell c, boolean lit) {
        BlockState s = level.getBlockState(c.pos()), want = s;
        if (s.is(NuclearBlocks.NUCLEAR_CASING.get()))
            want = s.setValue(NuclearCasingBlock.ROLE, CasingRole.PLANT).setValue(NuclearCasingBlock.PIECE, c.index())
                    .setValue(NuclearCasingBlock.FACING, facing()).setValue(NuclearCasingBlock.LIT, lit);
        else if (s.is(NuclearBlocks.HOT_CELL_GLASS.get()))
            want = s.setValue(HotCellGlassBlock.FORMED, true).setValue(HotCellGlassBlock.PANE, c.column() - 1).setValue(HotCellGlassBlock.FACING, facing());
        else if (s.getBlock() instanceof MultiblockPortBlock)
            want = s.setValue(MultiblockPortBlock.CONCEALED, true);
        if (want != s) level.setBlock(c.pos(), want, Block.UPDATE_CLIENTS);
    }

    private void release(BlockPos p) {
        if (!level.isLoaded(p)) return;
        BlockState s = level.getBlockState(p), want = s;
        if (s.is(NuclearBlocks.NUCLEAR_CASING.get()) && s.getValue(NuclearCasingBlock.ROLE) == CasingRole.PLANT)
            want = s.setValue(NuclearCasingBlock.ROLE, CasingRole.NONE).setValue(NuclearCasingBlock.LIT, false).setValue(NuclearCasingBlock.PIECE, 0);
        else if (s.is(NuclearBlocks.HOT_CELL_GLASS.get())) want = s.setValue(HotCellGlassBlock.FORMED, false);
        else if (s.getBlock() instanceof MultiblockPortBlock) want = s.setValue(MultiblockPortBlock.CONCEALED, false);
        if (want != s) level.setBlock(p, want, Block.UPDATE_CLIENTS);
    }

    /** The core is gone: every block it claimed, and every one a fresh look finds, shows again as it is. */
    void dismantle() {
        if (level == null || level.isClientSide) return;
        Set<BlockPos> all = new HashSet<>(claimed);
        for (RadiochemStructure.Cell c : RadiochemStructure.scan(level, worldPosition, facing()).cells()) all.add(c.pos());
        for (BlockPos p : all) release(p);
        claimed.clear();
    }

    /** Whether this block belongs to the plant (on the server). */
    boolean claims(BlockPos p) {
        return claimed.contains(p);
    }

    /** What the renderer may draw: the whole plant. */
    public AABB renderBox() {
        Direction f = facing();
        return new AABB(RadiochemStructure.cell(worldPosition, f, 0, 0, 0))
                .minmax(new AABB(RadiochemStructure.cell(worldPosition, f, RadiochemStructure.COLUMNS - 1, RadiochemStructure.DEPTH - 1, RadiochemStructure.LEVELS - 1)));
    }

    /** Forge 1.20.1 culls block entity renderers by this box; on NeoForge 1.21.1 the renderer answers instead. */
    public AABB getRenderBoundingBox() {
        return renderBox();
    }

    // ------------------------------------------------------------------------------------------------------------------
    // The window
    // ------------------------------------------------------------------------------------------------------------------

    /**
     * Indices of the numbers the window gets. Big numbers go in two cells: the client gets 16 bits a cell. The shares by
     * {@link Product}, the spent parts by size, the outputs: the empty rods, then the products.
     */
    public static final int D_FORMED = 0, D_CYCLE = 1, D_PROGRESS = 2, D_WORK = 3, D_ENERGY = 4, D_WATER = 6, D_PROBLEM = 7,
            D_MADE = 8, D_SHARES = 10, D_SPENT = 14, D_OUT = 17, D_FE = 22, D_WORKING = 23, D_COUNT = 24;

    @Override
    public ContainerData windowNumbers() {
        return data;
    }

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            if (i >= D_SHARES && i < D_SPENT) return shares.get(Product.values()[i - D_SHARES]);
            if (i >= D_SPENT && i < D_OUT) return Math.min(spentIn[i - D_SPENT], Short.MAX_VALUE);
            if (i >= D_OUT && i < D_FE) return Math.min(givenOut[i - D_OUT], Short.MAX_VALUE);
            return switch (i) {
                case D_FORMED -> scan.formed() ? 1 : 0;
                case D_CYCLE -> cycle;
                case D_PROGRESS -> progress;
                case D_WORK -> Radiochem.ticks(cycle);
                case D_ENERGY -> energy & 0xFFFF;
                case D_ENERGY + 1 -> energy >>> 16;
                case D_WATER -> water;
                case D_PROBLEM -> problem.ordinal();
                case D_MADE -> made & 0xFFFF;
                case D_MADE + 1 -> made >>> 16;
                case D_FE -> working ? Radiochem.FE_PER_TICK : 0;
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
        return new RadiochemMenu(id, inv, data, ContainerLevelAccess.create(level, worldPosition));
    }

    // ------------------------------------------------------------------------------------------------------------------
    // Saving, in both spellings
    // ------------------------------------------------------------------------------------------------------------------

    private void write(CompoundTag tag) {
        tag.putInt("Cycle", cycle);
        tag.putInt("Progress", progress);
        tag.putInt("Energy", energy);
        tag.putInt("Water", water);
        tag.putInt("Made", made);
        tag.putIntArray("Shares", shares.save());
    }

    private void read(CompoundTag tag) {
        int c = tag.getInt("Cycle");
        cycle = c == 1 || c == 2 || c == 4 ? c : 0;
        progress = cycle == 0 ? 0 : Math.max(0, Math.min(Radiochem.ticks(cycle), tag.getInt("Progress")));
        energy = Math.max(0, Math.min(Radiochem.BUFFER, tag.getInt("Energy")));
        water = Math.max(0, Math.min(Radiochem.WATER_TANK, tag.getInt("Water")));
        made = tag.getInt("Made");
        shares.load(tag.getIntArray("Shares"));
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
