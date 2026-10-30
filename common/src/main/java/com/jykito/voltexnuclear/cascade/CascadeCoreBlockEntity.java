package com.jykito.voltexnuclear.cascade;

import com.jykito.industrialcore.block.custom.MultiblockPortBlock;
import com.jykito.industrialcore.block.entity.EnergyPortBlockEntity;
import com.jykito.industrialcore.block.entity.ItemFluidPortBlockEntity;
import com.jykito.voltexnuclear.content.NuclearBlockEntities;
import com.jykito.voltexnuclear.content.NuclearBlocks;
import com.jykito.voltexnuclear.content.NuclearItems;
import com.jykito.voltexnuclear.core.cascade.Cascade;
import com.jykito.voltexnuclear.machine.VoltexPorts;
import com.jykito.voltexnuclear.core.cascade.Cascade.Run;
import com.jykito.voltexnuclear.core.cascade.Cascade.Stage;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The working cascade (DESIGN step 3). Once a second it looks at the row and fits the blocks of the structure to it; every
 * tick it charges its buffer from the energy ports and runs the cycle: takes the yellowcake for one uranium from the input
 * ports when the output ports have room, spends the energy of the row while the work adds up, then puts out one enriched
 * and the rest depleted. With no yellowcake in the input the low stage takes depleted uranium instead and re-enriches the
 * tails ({@link Run#TAILS}). The numbers are in {@link Cascade}.
 * <p>
 * Saving has a different method on each version: {@code saveAdditional(tag)} and {@code load(tag)} on Forge 1.20.1,
 * {@code saveAdditional(tag, registries)} and {@code loadAdditional(tag, registries)} on NeoForge 1.21.1. The class declares
 * both; each version overrides its own.
 */
public class CascadeCoreBlockEntity extends BlockEntity implements MenuProvider, WindowNumbers {

    /** Why the cascade does not run, for the window. */
    public enum Problem { NONE, NOT_FORMED, STAGE_SHUT, NO_YELLOWCAKE, OUTPUT_FULL, NO_ENERGY }

    /** Ticks between looks at the row. */
    private static final int SCAN_EVERY = 20;
    /** The lights stay on this long after the last working tick, so a cascade waiting for the next yellowcake does not blink. */
    private static final int LIGHT_HOLD = 40;

    private Stage stage = Stage.LOW;
    /** The run of the cycle under way, or null between cycles. */
    @Nullable private Run cycle;
    private int progress, energy, made;

    // runtime, not saved: found again after loading
    private CascadeStructure.Scan scan = new CascadeStructure.Scan(0, List.of(), List.of());
    private final Set<BlockPos> claimed = new HashSet<>();
    private int scanTimer, idle = LIGHT_HOLD;
    private Problem problem = Problem.NOT_FORMED;
    private boolean working;
    private int feedIn, tailsIn, enrichedOut, depletedOut;

    // the client's animation, kept between frames by the renderer
    public float clientAngle, clientSpin;
    public double clientTime = -1;

    public CascadeCoreBlockEntity(BlockPos pos, BlockState state) {
        super(NuclearBlockEntities.CASCADE_CORE.get(), pos, state);
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
            run(inputs, outputs);
            feedIn = VoltexPorts.count(inputs, NuclearItems.YELLOWCAKE.get());
            tailsIn = VoltexPorts.count(inputs, NuclearItems.DEPLETED_URANIUM.get());
            enrichedOut = VoltexPorts.count(outputs, product(shownStage()));
            depletedOut = VoltexPorts.count(outputs, NuclearItems.DEPLETED_URANIUM.get());
        }
        idle = working ? 0 : Math.min(idle + 1, LIGHT_HOLD);
        showLights(idle < LIGHT_HOLD);
    }

    private void charge(List<EnergyPortBlockEntity> power) {
        int want = Math.min(Cascade.INTAKE, Cascade.BUFFER - energy);
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

    private void run(List<ItemFluidPortBlockEntity> inputs, List<ItemFluidPortBlockEntity> outputs) {
        int columns = scan.columns();
        if (cycle == null) {
            if (!stage.open(columns)) { problem = Problem.STAGE_SHUT; return; }
            Run next = next(inputs);
            if (next == null) { problem = Problem.NO_YELLOWCAKE; return; }
            if (!VoltexPorts.fits(outputs, product(stage), 1, NuclearItems.DEPLETED_URANIUM.get(), next.depleted())) { problem = Problem.OUTPUT_FULL; return; }
            VoltexPorts.take(inputs, feed(next), next.feed());
            cycle = next;
            progress = 0;
            setChanged();
        }
        if (!cycle.stage().open(columns)) { problem = Problem.STAGE_SHUT; return; }   // columns broken off during the cycle: it waits
        int need = Cascade.energyPerTick(columns);
        if (energy < need) { problem = Problem.NO_ENERGY; return; }
        energy -= need;
        progress += columns;
        problem = Problem.NONE;
        working = true;
        if (progress >= cycle.work()) {
            // the room was checked when the cycle began; whatever no longer fits waits in the next check, nothing is lost
            VoltexPorts.put(outputs, product(cycle.stage()), 1);
            VoltexPorts.put(outputs, NuclearItems.DEPLETED_URANIUM.get(), cycle.depleted());
            made++;
            cycle = null;
            progress = 0;
        }
        setChanged();
    }

    /** The run the chosen stage starts next: on yellowcake, else the low stage on tails; null when the input has neither. */
    @Nullable
    private Run next(List<ItemFluidPortBlockEntity> inputs) {
        Run run = Run.of(stage);
        if (VoltexPorts.count(inputs, NuclearItems.YELLOWCAKE.get()) >= run.feed()) return run;
        if (stage == Stage.LOW && VoltexPorts.count(inputs, NuclearItems.DEPLETED_URANIUM.get()) >= Run.TAILS.feed()) return Run.TAILS;
        return null;
    }

    private static Item feed(Run run) {
        return (run.tails() ? NuclearItems.DEPLETED_URANIUM : NuclearItems.YELLOWCAKE).get();
    }

    private static Item product(Stage stage) {
        return (stage == Stage.LOW ? NuclearItems.LOW_ENRICHED_URANIUM : NuclearItems.HIGH_ENRICHED_URANIUM).get();
    }

    /** The stage whose product the window shows: the cycle under way, else the chosen stage. */
    private Stage shownStage() {
        return cycle != null ? cycle.stage() : stage;
    }

    /** The window's stage buttons: a stage the row cannot make is refused; the choice counts from the next cycle. */
    public void chooseStage(Stage choice) {
        if (!choice.open(scan.columns()) || choice == stage) return;
        stage = choice;
        setChanged();
    }

    // ------------------------------------------------------------------------------------------------------------------
    // The structure
    // ------------------------------------------------------------------------------------------------------------------

    private Direction facing() {
        return getBlockState().getValue(CascadeCoreBlock.FACING);
    }

    private void rescan() {
        scan = CascadeStructure.scan(level, worldPosition, facing());
        Set<BlockPos> now = new HashSet<>();
        for (CascadeStructure.Cell c : scan.cells()) now.add(c.pos());
        for (BlockPos p : claimed) if (!now.contains(p)) release(p);
        claimed.clear();
        claimed.addAll(now);
        boolean lit = getBlockState().getValue(CascadeCoreBlock.LIT);
        for (CascadeStructure.Cell c : scan.cells()) claim(c, lit);
        setCoreState(getBlockState().setValue(CascadeCoreBlock.FORMED, scan.formed()).setValue(CascadeCoreBlock.COLUMNS, scan.columns()));
    }

    /** The lights of the core and of every ring and the lamp, changed only when they change (a remesh of the chunk). */
    private void showLights(boolean lit) {
        if (getBlockState().getValue(CascadeCoreBlock.LIT) == lit) return;
        setCoreState(getBlockState().setValue(CascadeCoreBlock.LIT, lit));
        for (CascadeStructure.Cell c : scan.cells()) claim(c, lit);
    }

    private void setCoreState(BlockState want) {
        if (want != getBlockState()) level.setBlock(worldPosition, want, Block.UPDATE_CLIENTS);
    }

    private void claim(CascadeStructure.Cell c, boolean lit) {
        BlockState s = level.getBlockState(c.pos()), want = s;
        if (s.is(NuclearBlocks.NUCLEAR_CASING.get()))
            want = s.setValue(NuclearCasingBlock.ROLE, c.role()).setValue(NuclearCasingBlock.FACING, facing()).setValue(NuclearCasingBlock.LIT, lit);
        else if (s.is(NuclearBlocks.CENTRIFUGE.get()))
            want = s.setValue(CentrifugeBlock.FORMED, true).setValue(CentrifugeBlock.FACING, facing()).setValue(CentrifugeBlock.LIT, lit);
        else if (s.getBlock() instanceof MultiblockPortBlock)
            want = s.setValue(MultiblockPortBlock.CONCEALED, true);
        if (want != s) level.setBlock(c.pos(), want, Block.UPDATE_CLIENTS);
    }

    private void release(BlockPos p) {
        if (!level.isLoaded(p)) return;
        BlockState s = level.getBlockState(p), want = s;
        if (s.is(NuclearBlocks.NUCLEAR_CASING.get())) want = s.setValue(NuclearCasingBlock.ROLE, CasingRole.NONE).setValue(NuclearCasingBlock.LIT, false);
        else if (s.is(NuclearBlocks.CENTRIFUGE.get())) want = s.setValue(CentrifugeBlock.FORMED, false).setValue(CentrifugeBlock.LIT, false);
        else if (s.getBlock() instanceof MultiblockPortBlock) want = s.setValue(MultiblockPortBlock.CONCEALED, false);
        if (want != s) level.setBlock(p, want, Block.UPDATE_CLIENTS);
    }

    /** The core is gone: every block it claimed, and every one a fresh look finds, shows again as it is. */
    void dismantle() {
        if (level == null || level.isClientSide) return;
        Set<BlockPos> all = new HashSet<>(claimed);
        for (CascadeStructure.Cell c : CascadeStructure.scan(level, worldPosition, facing()).cells()) all.add(c.pos());
        for (BlockPos p : all) release(p);
        claimed.clear();
    }

    /** Whether this block belongs to the cascade (on the server). */
    boolean claims(BlockPos p) {
        return claimed.contains(p);
    }

    /** What the renderer may draw: from the plinth under the cabinet to the top of the last column. */
    public AABB renderBox() {
        Direction f = facing();
        int n = Math.max(1, getBlockState().getValue(CascadeCoreBlock.COLUMNS));
        return new AABB(CascadeStructure.cell(worldPosition, f, 0, 0)).minmax(new AABB(CascadeStructure.cell(worldPosition, f, n, CascadeStructure.ROWS - 1)));
    }

    /** Forge 1.20.1 culls block entity renderers by this box; on NeoForge 1.21.1 the renderer answers instead. */
    public AABB getRenderBoundingBox() {
        return renderBox();
    }

    // ------------------------------------------------------------------------------------------------------------------
    // The window
    // ------------------------------------------------------------------------------------------------------------------

    /** Indices of the numbers the window gets. Big numbers go in two cells: the client gets 16 bits a cell. */
    public static final int D_COLUMNS = 0, D_FORMED = 1, D_STAGE = 2, D_CYCLE = 3, D_PROGRESS = 4, D_WORK = 5, D_ENERGY = 6,
            D_PROBLEM = 8, D_MADE = 9, D_FEED = 11, D_ENRICHED = 13, D_DEPLETED = 14, D_FE = 15, D_WORKING = 16, D_TAILS = 17, D_COUNT = 18;

    @Override
    public ContainerData windowNumbers() {
        return data;
    }

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case D_COLUMNS -> scan.columns();
                case D_FORMED -> scan.formed() ? 1 : 0;
                case D_STAGE -> stage.ordinal();
                case D_CYCLE -> cycle == null ? 0 : cycle.index() + 1;
                case D_PROGRESS -> progress;
                case D_WORK -> cycle == null ? 0 : cycle.work();
                case D_ENERGY -> energy & 0xFFFF;
                case D_ENERGY + 1 -> energy >>> 16;
                case D_PROBLEM -> problem.ordinal();
                case D_MADE -> made & 0xFFFF;
                case D_MADE + 1 -> made >>> 16;
                case D_FEED -> feedIn & 0xFFFF;
                case D_FEED + 1 -> feedIn >>> 16;
                case D_ENRICHED -> Math.min(enrichedOut, Short.MAX_VALUE);
                case D_DEPLETED -> Math.min(depletedOut, Short.MAX_VALUE);
                case D_FE -> working ? Cascade.energyPerTick(scan.columns()) : 0;
                case D_WORKING -> working ? 1 : 0;
                case D_TAILS -> Math.min(tailsIn, Short.MAX_VALUE);
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
        return new CascadeMenu(id, inv, data, ContainerLevelAccess.create(level, worldPosition));
    }

    // ------------------------------------------------------------------------------------------------------------------
    // Saving, in both spellings
    // ------------------------------------------------------------------------------------------------------------------

    private void write(CompoundTag tag) {
        tag.putInt("Stage", stage.ordinal());
        tag.putInt("Cycle", cycle == null ? -1 : cycle.index());   // the runs on yellowcake keep the index of their stage
        tag.putInt("Progress", progress);
        tag.putInt("Energy", energy);
        tag.putInt("Made", made);
    }

    private void read(CompoundTag tag) {
        stage = Stage.byIndex(tag.getInt("Stage"));
        int c = tag.contains("Cycle") ? tag.getInt("Cycle") : -1;
        cycle = c < 0 ? null : Run.byIndex(c);
        progress = tag.getInt("Progress");
        energy = Math.min(Cascade.BUFFER, tag.getInt("Energy"));
        made = tag.getInt("Made");
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
