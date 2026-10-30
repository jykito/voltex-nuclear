package com.jykito.voltexnuclear.fusion;

import com.jykito.industrialcore.advancement.VoltexEventTrigger;
import com.jykito.industrialcore.block.custom.MultiblockPortBlock;
import com.jykito.industrialcore.block.entity.EnergyPortBlockEntity;
import com.jykito.industrialcore.block.entity.HeatPortBlockEntity;
import com.jykito.industrialcore.block.entity.ItemFluidPortBlockEntity;
import com.jykito.industrialcore.nanite.NaniteGenome;
import com.jykito.voltexnuclear.VoltexNuclear;
import com.jykito.voltexnuclear.content.NuclearBlockEntities;
import com.jykito.voltexnuclear.content.NuclearBlocks;
import com.jykito.voltexnuclear.content.NuclearItems;
import com.jykito.voltexnuclear.content.NuclearNanites;
import com.jykito.voltexnuclear.core.fusion.Fusion;
import com.jykito.voltexnuclear.core.fusion.Torus;
import com.jykito.voltexnuclear.machine.EnergyOutputPortBlockEntity;
import com.jykito.voltexnuclear.machine.EnergySource;
import com.jykito.voltexnuclear.machine.HiddenPorts;
import com.jykito.voltexnuclear.machine.VoltexPorts;
import com.jykito.voltexnuclear.machine.WindowNumbers;
import com.jykito.voltexnuclear.reactor.ReactorCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The fusion torus's core (DESIGN steps 11, 18 and 19).
 * <p>
 * The structure, as the fission reactor's: it looks at its blocks only when asked (step 16, no timers): when it is placed,
 * when a block of the torus is placed near it, broken or changed next to it, when it is clicked and once on its first
 * tick after loading. Formed, it turns its casing into frame blocks that know their place, tells its coils which of the
 * eight they are and hides the Voltex ports in the stands; it gives them all back when it comes apart, and the plasma goes
 * out.
 * <p>
 * The work: the {@link Torus} of the core module burns. Once a second the input ports top up the deuterium and tritium
 * slots and a slot empties a cell into its tank when the cell fits there; the empty cell goes to its output slot. Helium
 * fills one of those empty cells whenever the tank holds a cell's worth; the helium cells and the empty cells left go out
 * through the output ports. Every tick the coils drink the Voltex superfluid condensate from the input ports' tanks and the
 * Voltex energy ports charge the ignition reserve. The output, with what a knowing Watcher adds, goes up to the knob's
 * share as heat into the pipes next to the heat ports at the rate of the Voltex thermogenerator, the rest into the
 * buffer; the energy output connectors give out what is above the reserve.
 * <p>
 * Saving has a different method on each version, see {@link ReactorCoreBlockEntity}: the class declares both spellings.
 */
public class TorusCoreBlockEntity extends BlockEntity implements MenuProvider, EnergySource, WindowNumbers {

    /** Slots: the deuterium and tritium cells in, the helium cells and the empty cells out, the Watcher's. */
    public static final int IN_DEUTERIUM = 0, IN_TRITIUM = 1, OUT_HELIUM = 2, OUT_EMPTY = 3, WATCHER = 4, SLOTS = 5;
    /** The most condensate one tick takes from the input ports, mB. */
    private static final int CONDENSATE_INTAKE = 1_000;
    /** The Voltex advancement event of the add-on's branch: the torus lit. */
    private static final String EVENT_LIT = "voltex_nuclear:torus_lit";

    /** Buttons of the window (TorusMenu.clickMenuButton): run, the knobs a step down and up, and set from 0 to 100 in steps of 5. */
    public static final int B_RUN = 0, B_MIX_DOWN = 1, B_MIX_UP = 2, B_SPLIT_DOWN = 3, B_SPLIT_UP = 4, KNOB_STEPS = 20,
            B_MIX_SET = 10, B_SPLIT_SET = B_MIX_SET + KNOB_STEPS + 1;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private final Slots slots = new Slots();
    private final Torus torus = new Torus();
    /** The knobs, in percent: the tritium share and the share into heat. */
    private int mix = 50, split;

    // runtime, not saved: found again after loading
    private boolean checkPending = true;
    private final Set<BlockPos> claimed = new HashSet<>();
    private List<BlockPos> ports = List.of();
    private int service;
    /** This tick, for the window. */
    private Torus.Tick last;
    private double lastFe, lastHeat, lastLost, given;
    private double knowledge;
    /** What the renderer was last told: the phase, and the ticks of the ignition then. */
    private int livePhase = -1;

    // the client: the phase and the game time the ignition started at, as the server last told them (DESIGN step 18: the
    // state is synced when it changes, the running clot and the blinking come from the game's time)
    public Torus.Phase clientPhase = Torus.Phase.OFF;
    public long clientHeatStart;

    public TorusCoreBlockEntity(BlockPos pos, BlockState state) {
        super(NuclearBlockEntities.TORUS_CORE.get(), pos, state);
        torus.setMix(mix / 100.0);
    }

    // ------------------------------------------------------------------------------------------------------------------
    // The work
    // ------------------------------------------------------------------------------------------------------------------

    void serverTick() {
        if (checkPending) check();
        if (level != null && HiddenPorts.gone(level, worldPosition, ports)) check();
        if (level == null || !formed()) return;
        List<ItemFluidPortBlockEntity> inputs = new ArrayList<>(), outputs = new ArrayList<>();
        List<EnergyPortBlockEntity> power = new ArrayList<>();
        List<EnergyOutputPortBlockEntity> outlets = new ArrayList<>();
        List<HeatPortBlockEntity> heat = new ArrayList<>();
        for (BlockPos p : ports) {
            BlockEntity e = level.getBlockEntity(p);
            if (e instanceof ItemFluidPortBlockEntity port) {
                port.linkCore(worldPosition);   // the port is named after this machine in the AE2 terminal
                (port.isOutput() ? outputs : inputs).add(port);
            } else if (e instanceof EnergyPortBlockEntity port) {
                power.add(port);
            } else if (e instanceof EnergyOutputPortBlockEntity out) {
                out.link(worldPosition);
                outlets.add(out);
            } else if (e instanceof HeatPortBlockEntity h) {
                heat.add(h);
            }
        }

        if (++service >= Fusion.CELL_EVERY) {
            service = 0;
            topUp(inputs, IN_DEUTERIUM, NuclearItems.DEUTERIUM_CELL.get());
            topUp(inputs, IN_TRITIUM, NuclearItems.TRITIUM_CELL.get());
            if (!items.get(IN_DEUTERIUM).isEmpty() && room(OUT_EMPTY) && torus.fillDeuterium()) emptied(IN_DEUTERIUM);
            if (!items.get(IN_TRITIUM).isEmpty() && room(OUT_EMPTY) && torus.fillTritium()) emptied(IN_TRITIUM);
            if (items.get(OUT_EMPTY).getCount() > 0 && room(OUT_HELIUM) && torus.takeHelium()) {
                items.get(OUT_EMPTY).shrink(1);
                add(OUT_HELIUM, NuclearItems.HELIUM_CELL.get());
            }
        }
        for (int slot : new int[]{OUT_HELIUM, OUT_EMPTY})
            if (!items.get(slot).isEmpty() && !outputs.isEmpty()) items.set(slot, VoltexPorts.put(outputs, items.get(slot)));

        // the condensate for the coils and the reserve for the next ignition
        Fluid condensate = VoltexNuclear.voltexFluid("superfluid_condensate");
        int want = (int) Math.min(CONDENSATE_INTAKE, Fusion.CONDENSATE_TANK - torus.condensate());
        for (ItemFluidPortBlockEntity port : inputs) {
            if (want <= 0) break;
            int got = VoltexNuclear.platform().drainPort(port, condensate, want);
            torus.addCondensate(got);
            want -= got;
        }
        for (EnergyPortBlockEntity port : power) {
            int took = (int) torus.charge(port.getEnergy().getEnergyStored());
            if (took <= 0) continue;
            port.getEnergy().consumeEnergy(took);
            port.setChanged();
        }

        NaniteGenome strain = NuclearNanites.watcher(items.get(WATCHER));
        knowledge = strain != null ? NuclearNanites.knowledge(strain) : 0;
        last = torus.tick(knowledge, strain != null ? strain.stability() : 1, level.random::nextDouble);

        // the output: up to the share into heat, what the pipes take; the rest into the buffer, which the connectors empty
        double produced = last.energy();
        int heatGiven = 0;
        if (split > 0 && produced > 0 && !heat.isEmpty()) {
            int wantHeat = (int) Math.min(Integer.MAX_VALUE, produced * split / 100 / ReactorCoreBlockEntity.FE_PER_HEAT);
            for (HeatPortBlockEntity h : heat) heatGiven += VoltexPorts.toNeighbours(level, h.getBlockPos(), wantHeat - heatGiven, true, this::own);
        }
        lastFe = Math.max(0, produced - (double) heatGiven * ReactorCoreBlockEntity.FE_PER_HEAT);
        lastHeat = heatGiven;
        lastLost = torus.store(lastFe);
        given = 0;
        for (EnergyOutputPortBlockEntity out : outlets) {
            int took = VoltexPorts.toNeighbours(level, out.getBlockPos(), above(), false, this::own);
            given += torus.drain(took);
        }
        // lit just now (the last phase told to the client was another): the players nearby earn the branch's advancement
        if (torus.phase() == Torus.Phase.LIT && livePhase != Torus.Phase.LIT.ordinal()) VoltexEventTrigger.award(level, worldPosition, 32, EVENT_LIT);
        setLit(torus.phase() == Torus.Phase.HEAT || torus.phase() == Torus.Phase.LIT || torus.phase() == Torus.Phase.COOL);
        syncLive();
        setChanged();
    }

    /** Tells the client what the renderer draws, when the phase changed since it was told last. */
    private void syncLive() {
        if (torus.phase().ordinal() == livePhase) return;
        livePhase = torus.phase().ordinal();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    private CompoundTag liveTag() {
        CompoundTag live = new CompoundTag();
        live.putInt("Phase", torus.phase().ordinal());
        live.putInt("Heating", torus.heating());
        CompoundTag tag = new CompoundTag();
        tag.put("Live", live);
        return tag;
    }

    /** The client: the phase, and when the ignition under way started by the client's own clock. */
    private void readLive(CompoundTag live) {
        int p = live.getInt("Phase");
        clientPhase = p >= 0 && p < Torus.Phase.values().length ? Torus.Phase.values()[p] : Torus.Phase.OFF;
        clientHeatStart = (level != null ? level.getGameTime() : 0) - live.getInt("Heating");
    }

    /** The chunk and the update packets carry only what the renderer draws; both spellings, each version takes its own. */
    public CompoundTag getUpdateTag() {
        return liveTag();
    }

    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return liveTag();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** The energy above the reserve, what the connectors may give out, as a whole number of FE. */
    private int above() {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, torus.buffer() - Fusion.RESERVE));
    }

    /** Whether a block is this torus's own: its core or a block it claimed. */
    private boolean own(BlockPos p) {
        return p.equals(worldPosition) || claimed.contains(p);
    }

    /** The input ports top up a slot with its cells, to a full stack. */
    private void topUp(List<ItemFluidPortBlockEntity> inputs, int slot, Item cell) {
        if (inputs.isEmpty()) return;
        ItemStack s = items.get(slot);
        int n = Math.min(cell.getDefaultInstance().getMaxStackSize() - s.getCount(), VoltexPorts.count(inputs, cell));
        if (n <= 0) return;
        VoltexPorts.take(inputs, cell, n);
        if (s.isEmpty()) items.set(slot, new ItemStack(cell, n));
        else s.grow(n);
    }

    /** A cell from this slot went into its tank: the empty cell to its output slot. */
    private void emptied(int slot) {
        items.get(slot).shrink(1);
        add(OUT_EMPTY, NuclearItems.EMPTY_CELL.get());
    }

    private boolean room(int slot) {
        ItemStack s = items.get(slot);
        return s.isEmpty() || s.getCount() < s.getMaxStackSize();
    }

    private void add(int slot, Item item) {
        ItemStack s = items.get(slot);
        if (s.isEmpty()) items.set(slot, new ItemStack(item));
        else s.grow(1);
    }

    /** The face lights while the torus heats, burns or cools; the state changes only when the light does (a remesh). */
    private void setLit(boolean lit) {
        BlockState s = getBlockState();
        if (s.getValue(TorusCoreBlock.LIT) != lit) level.setBlock(worldPosition, s.setValue(TorusCoreBlock.LIT, lit), Block.UPDATE_CLIENTS);
    }

    public Torus torus() {
        return torus;
    }

    /** Everything the core holds goes into the world: it is being broken. */
    void dropAll() {
        if (level == null || level.isClientSide) return;
        Containers.dropContents(level, worldPosition, items);
        for (int i = 0; i < SLOTS; i++) items.set(i, ItemStack.EMPTY);
    }

    // ------------------------------------------------------------------------------------------------------------------
    // The window's controls
    // ------------------------------------------------------------------------------------------------------------------

    /** A button of the window (TorusMenu.clickMenuButton). */
    public void press(int id) {
        if (id == B_RUN) {
            if (torus.running()) torus.stop();
            else torus.start();
        } else if (id == B_MIX_DOWN || id == B_MIX_UP) {
            mix = clampKnob(mix + (id == B_MIX_UP ? 5 : -5));
        } else if (id == B_SPLIT_DOWN || id == B_SPLIT_UP) {
            split = clampKnob(split + (id == B_SPLIT_UP ? 10 : -10));
        } else if (id >= B_MIX_SET && id <= B_MIX_SET + KNOB_STEPS) {
            mix = 5 * (id - B_MIX_SET);
        } else if (id >= B_SPLIT_SET && id <= B_SPLIT_SET + KNOB_STEPS) {
            split = 5 * (id - B_SPLIT_SET);
        } else {
            return;
        }
        torus.setMix(mix / 100.0);
        setChanged();
    }

    private static int clampKnob(int v) {
        return Math.max(0, Math.min(100, v));
    }

    // ------------------------------------------------------------------------------------------------------------------
    // The energy for the output connectors: only what is above the reserve
    // ------------------------------------------------------------------------------------------------------------------

    @Override
    public int giveEnergy(int max, boolean simulate) {
        if (!formed()) return 0;
        int g = Math.min(max, above());
        if (!simulate && g > 0) {
            torus.drain(g);
            given += g;
            setChanged();
        }
        return g;
    }

    @Override
    public int energyStored() {
        return above();
    }

    @Override
    public int energyCapacity() {
        return (int) (Fusion.BUFFER - Fusion.RESERVE);
    }

    // ------------------------------------------------------------------------------------------------------------------
    // The structure
    // ------------------------------------------------------------------------------------------------------------------

    /** Look at the blocks on the next tick. */
    public void requestCheck() {
        checkPending = true;
    }

    public boolean formed() {
        return getBlockState().getValue(TorusCoreBlock.FORMED);
    }

    /** The Voltex ports in the stands of the formed torus. */
    public List<BlockPos> ports() {
        return ports;
    }

    private Direction facing() {
        return getBlockState().getValue(TorusCoreBlock.FACING);
    }

    void check() {
        checkPending = false;
        if (level == null || level.isClientSide) return;
        TorusStructure.Scan scan = TorusStructure.scan(level, worldPosition, facing());
        if (!scan.complete()) {
            if (formed() || !claimed.isEmpty()) dismantle();
            return;
        }
        ports = scan.ports();
        claimed.clear();
        for (TorusStructure.Place p : TorusStructure.PLACES) {
            if (p == TorusStructure.CORE) continue;
            BlockPos at = TorusStructure.pos(worldPosition, facing(), p);
            claimed.add(at);
            claim(at, p);
        }
        setCoreState(getBlockState().setValue(TorusCoreBlock.FORMED, true));
    }

    private void claim(BlockPos at, TorusStructure.Place p) {
        BlockState s = level.getBlockState(at), want = s;
        if (TorusStructure.isCasing(s))
            want = NuclearBlocks.TORUS_FRAME.get().defaultBlockState().setValue(TorusFrameBlock.PART, p.index()).setValue(TorusFrameBlock.FACING, facing());
        else if (s.getBlock() instanceof TorusCoilBlock)
            want = s.setValue(TorusCoilBlock.FORMED, true).setValue(TorusCoilBlock.COIL, p.coil()).setValue(TorusCoilBlock.FACING, facing());
        else if (s.getBlock() instanceof MultiblockPortBlock)
            want = s.setValue(MultiblockPortBlock.CONCEALED, true);
        if (want != s) level.setBlock(at, want, Block.UPDATE_ALL);
    }

    private void release(BlockPos at) {
        if (!level.isLoaded(at)) return;
        BlockState s = level.getBlockState(at), want = s;
        if (s.is(NuclearBlocks.TORUS_FRAME.get())) want = NuclearBlocks.NUCLEAR_CASING.get().defaultBlockState();
        else if (s.getBlock() instanceof TorusCoilBlock) want = s.setValue(TorusCoilBlock.FORMED, false);
        else if (s.getBlock() instanceof MultiblockPortBlock) want = s.setValue(MultiblockPortBlock.CONCEALED, false);
        if (want != s) level.setBlock(at, want, Block.UPDATE_ALL);
        if (level.getBlockEntity(at) instanceof EnergyOutputPortBlockEntity out) out.link(null);
    }

    private void setCoreState(BlockState want) {
        if (want != getBlockState()) level.setBlock(worldPosition, want, Block.UPDATE_CLIENTS);
    }

    /**
     * The torus comes apart: the plasma goes out, every block it claimed and every frame and coil of its places (claims
     * are not saved) shows again as it is. A frame or a coil of this torus is recognised by its place and facing, so a
     * neighbouring torus is left alone. The tanks keep what they hold.
     */
    void dismantle() {
        if (level == null || level.isClientSide) return;
        torus.quench();
        Set<BlockPos> all = new HashSet<>(claimed);
        for (TorusStructure.Place p : TorusStructure.PLACES) if (p != TorusStructure.CORE) all.add(TorusStructure.pos(worldPosition, facing(), p));
        claimed.clear();
        ports = List.of();
        for (BlockPos p : all) {
            BlockState s = level.isLoaded(p) ? level.getBlockState(p) : null;
            if (s != null && (s.is(NuclearBlocks.TORUS_FRAME.get()) || s.is(NuclearBlocks.TORUS_COIL.get()) && s.getValue(TorusCoilBlock.FORMED))
                    && !ownPlace(p, s)) continue;
            release(p);
        }
        // the world, not the cached state: while the core is being broken the cache still holds it and would put it back
        if (level.getBlockState(worldPosition).is(NuclearBlocks.TORUS_CORE.get()))
            setCoreState(getBlockState().setValue(TorusCoreBlock.FORMED, false).setValue(TorusCoreBlock.LIT, false));
    }

    /** Whether a frame or a formed coil stands in a place of this very torus, by its place and facing. */
    private boolean ownPlace(BlockPos p, BlockState s) {
        TorusStructure.Place place = s.is(NuclearBlocks.TORUS_FRAME.get()) ? TorusStructure.PLACES.get(s.getValue(TorusFrameBlock.PART))
                : TorusStructure.COILS.get(s.getValue(TorusCoilBlock.COIL));
        Direction f = s.is(NuclearBlocks.TORUS_FRAME.get()) ? s.getValue(TorusFrameBlock.FACING) : s.getValue(TorusCoilBlock.FACING);
        return TorusStructure.coreOf(p, f, place).equals(worldPosition);
    }

    // ------------------------------------------------------------------------------------------------------------------
    // The slots, as the window sees them; pipes and hoppers reach the torus only through its ports
    // ------------------------------------------------------------------------------------------------------------------

    /** The window's container: the cells in and out and the Watcher's slot. */
    public final class Slots implements Container {

        @Override
        public int getContainerSize() {
            return SLOTS;
        }

        @Override
        public boolean isEmpty() {
            return items.stream().allMatch(ItemStack::isEmpty);
        }

        @Override
        public ItemStack getItem(int slot) {
            return items.get(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack s = items.get(slot);
            if (s.isEmpty() || amount <= 0) return ItemStack.EMPTY;
            ItemStack out = s.split(amount);
            setChanged();
            return out;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack s = items.get(slot);
            items.set(slot, ItemStack.EMPTY);
            return s;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            items.set(slot, stack);
            setChanged();
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return accepts(slot, stack);
        }

        @Override
        public void setChanged() {
            TorusCoreBlockEntity.this.setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            for (int i = 0; i < SLOTS; i++) items.set(i, ItemStack.EMPTY);
        }
    }

    /** What a slot takes from the player: its own cells in the inputs, a Watcher strain in its slot, nothing in the outputs. */
    public static boolean accepts(int slot, ItemStack stack) {
        return switch (slot) {
            case IN_DEUTERIUM -> stack.is(NuclearItems.DEUTERIUM_CELL.get());
            case IN_TRITIUM -> stack.is(NuclearItems.TRITIUM_CELL.get());
            case WATCHER -> NuclearNanites.watcher(stack) != null;
            default -> false;
        };
    }

    // ------------------------------------------------------------------------------------------------------------------
    // The window
    // ------------------------------------------------------------------------------------------------------------------

    /**
     * Indices of the numbers the window gets; the client gets 16 bits a cell, so the big ones go in two. The phase and why
     * as their ordinals; the mix in the plasma and its yield in thousandths (the mix 0xFFFF while it does not burn); the
     * Watcher's knowledge in thousandths; the tanks in whole units and mB.
     */
    public static final int D_PHASE = 0, D_WHY = 1, D_MIX = 2, D_SPLIT = 3, D_FED = 4, D_PLASMA = 5, D_YIELD = 6, D_HEATING = 7,
            D_COOLING = 8, D_FE = 9, D_HEAT = 11, D_BUFFER = 13, D_GIVEN = 15, D_LOST = 17, D_DEUTERIUM = 19, D_TRITIUM = 20,
            D_CONDENSATE = 21, D_HELIUM = 22, D_KNOWLEDGE = 23, D_JITTER = 24, D_RUNNING = 25, D_COUNT = 26;

    @Override
    public ContainerData windowNumbers() {
        return data;
    }

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            int fe = wide(lastFe), heat = wide(lastHeat), buffer = wide(torus.buffer()), out = wide(given), lost = wide(lastLost);
            return switch (i) {
                case D_PHASE -> torus.phase().ordinal();
                case D_WHY -> torus.why().ordinal();
                case D_MIX -> mix;
                case D_SPLIT -> split;
                case D_FED -> (int) Math.round(torus.fed() * 1000);
                case D_PLASMA -> last == null || Double.isNaN(last.mix()) ? 0xFFFF : (int) Math.round(last.mix() * 1000);
                case D_YIELD -> last == null ? 0 : (int) Math.round(last.yield() * 1000);
                case D_HEATING -> torus.heating();
                case D_COOLING -> torus.cooling();
                case D_FE -> fe & 0xFFFF;
                case D_FE + 1 -> fe >>> 16;
                case D_HEAT -> heat & 0xFFFF;
                case D_HEAT + 1 -> heat >>> 16;
                case D_BUFFER -> buffer & 0xFFFF;
                case D_BUFFER + 1 -> buffer >>> 16;
                case D_GIVEN -> out & 0xFFFF;
                case D_GIVEN + 1 -> out >>> 16;
                case D_LOST -> lost & 0xFFFF;
                case D_LOST + 1 -> lost >>> 16;
                case D_DEUTERIUM -> (int) Math.round(torus.deuterium());
                case D_TRITIUM -> (int) Math.round(torus.tritium());
                case D_CONDENSATE -> (int) Math.round(torus.condensate());
                case D_HELIUM -> (int) Math.round(torus.helium());
                case D_KNOWLEDGE -> (int) Math.round(knowledge * 1000);
                case D_JITTER -> torus.jitterLeft() > 0 ? (torus.jitter() > 0 ? 1 : 2) : 0;
                case D_RUNNING -> torus.running() ? 1 : 0;
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

    private static int wide(double v) {
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, Math.round(v)));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new TorusMenu(id, inv, slots, data, ContainerLevelAccess.create(level, worldPosition));
    }

    /** What the renderer may draw: the whole torus, the solenoid's cap standing into the layer over the top one. */
    public AABB renderBox() {
        Direction f = facing(), r = f.getCounterClockWise();
        BlockPos a = worldPosition.relative(r, -3).relative(f, -3 - TorusStructure.CORE.z()).below(TorusStructure.CORE.y());
        BlockPos b = worldPosition.relative(r, 3).relative(f, 3 - TorusStructure.CORE.z()).above(TorusStructure.LAYERS - TorusStructure.CORE.y());
        return new AABB(a).minmax(new AABB(b));
    }

    /** Forge 1.20.1 culls block entity renderers by this box; on NeoForge 1.21.1 the renderer answers instead. */
    public AABB getRenderBoundingBox() {
        return renderBox();
    }

    // ------------------------------------------------------------------------------------------------------------------
    // Saving, in both spellings
    // ------------------------------------------------------------------------------------------------------------------

    private void write(CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        VoltexNuclear.platform().saveItems(tag, items, registries);
        tag.putInt("Mix", mix);
        tag.putInt("Split", split);
        tag.putDouble("Deuterium", torus.deuterium());
        tag.putDouble("Tritium", torus.tritium());
        tag.putDouble("Condensate", torus.condensate());
        tag.putDouble("Helium", torus.helium());
        tag.putDouble("Buffer", torus.buffer());
        tag.putBoolean("Running", torus.running());
        tag.putString("Phase", torus.phase().name());
        tag.putString("Why", torus.why().name());
        tag.putInt("Heating", torus.heating());
        tag.putInt("Cooling", torus.cooling());
        tag.putDouble("Jitter", torus.jitter());
        tag.putInt("JitterLeft", torus.jitterLeft());
    }

    private void read(CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        if (tag.contains("Live")) {   // an update packet on the client
            readLive(tag.getCompound("Live"));
            return;
        }
        for (int i = 0; i < SLOTS; i++) items.set(i, ItemStack.EMPTY);
        VoltexNuclear.platform().loadItems(tag, items, registries);
        mix = tag.contains("Mix") ? clampKnob(tag.getInt("Mix")) : 50;
        split = clampKnob(tag.getInt("Split"));
        torus.setMix(mix / 100.0);
        torus.restoreTanks(tag.getDouble("Deuterium"), tag.getDouble("Tritium"), tag.getDouble("Condensate"), tag.getDouble("Helium"), tag.getDouble("Buffer"));
        torus.restoreRun(tag.getBoolean("Running"), named(Torus.Phase.values(), tag.getString("Phase")), named(Torus.Why.values(), tag.getString("Why")),
                tag.getInt("Heating"), tag.getInt("Cooling"), tag.getDouble("Jitter"), tag.getInt("JitterLeft"));
    }

    @Nullable
    private static <E extends Enum<E>> E named(E[] values, String name) {
        for (E e : values) if (e.name().equals(name)) return e;
        return null;
    }

    protected void saveAdditional(CompoundTag tag) {
        write(tag, null);
    }

    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        write(tag, registries);
    }

    public void load(CompoundTag tag) {
        read(tag, null);
    }

    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        read(tag, registries);
    }
}
