package com.jykito.voltexnuclear.reactor;

import com.jykito.industrialcore.block.custom.MultiblockPortBlock;
import com.jykito.industrialcore.block.entity.HeatPortBlockEntity;
import com.jykito.industrialcore.advancement.VoltexEventTrigger;
import com.jykito.industrialcore.block.entity.ItemFluidPortBlockEntity;
import com.jykito.voltexnuclear.VoltexNuclear;
import com.jykito.industrialcore.nanite.NaniteGenome;
import com.jykito.voltexnuclear.content.NuclearBlockEntities;
import com.jykito.voltexnuclear.content.NuclearBlocks;
import com.jykito.voltexnuclear.content.NuclearItems;
import com.jykito.voltexnuclear.content.NuclearNanites;
import com.jykito.voltexnuclear.core.reactor.Cooling;
import com.jykito.voltexnuclear.core.reactor.Fission;
import com.jykito.voltexnuclear.core.reactor.FluxMap;
import com.jykito.voltexnuclear.core.reactor.Part;
import com.jykito.voltexnuclear.core.reactor.Reactor;
import com.jykito.voltexnuclear.core.reactor.Watcher;
import com.jykito.voltexnuclear.core.reactor.Zone;
import com.jykito.voltexnuclear.machine.EnergyOutputPortBlockEntity;
import com.jykito.voltexnuclear.machine.EnergySource;
import com.jykito.voltexnuclear.machine.HiddenPorts;
import com.jykito.voltexnuclear.machine.VoltexPorts;
import com.jykito.voltexnuclear.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The fission reactor's core (DESIGN steps 16, 17 and 26).
 * <p>
 * The structure: it looks at its blocks only when asked (step 16, no timers): when it is placed, when a block of the
 * reactor is placed near it, broken or changed next to it, when it is clicked and once on its first tick after loading,
 * which also covers tools that change blocks past the game's events. Many asks in one tick make one look on the next.
 * Formed, it turns its casing into frame blocks that know their place, hides the Voltex ports in the plinth and tells
 * the chambers they are on; it gives them all back when it comes apart.
 * <p>
 * The work (step 17): the 49 cells of the zone are slots of one part each; a cell is open while the chambers allow it
 * (a part in a cell a chamber took away drops out). The {@link Reactor} of the core module runs the zone; a rod carries
 * its wear in its item, written back into the stack once a second and whenever it leaves the cell. A burnt rod leaves
 * its ghost in the cell: the cell remembers what stood there, and once a second the input ports put the same item into a
 * cell that is empty and has a ghost. Taken out by hand, a part takes its ghost with it. The spent rod goes into a queue
 * that is kept, then into the first output slot, then into the output ports; while the queue is not empty the reactor
 * is held (no power, no wear) so nothing is lost. A channel blank stands in its cell itself and carries its irradiation in
 * its item; once irradiated it becomes its product (a lithium blank a tritium cell), which goes the same way through a
 * queue of its own into the third output slot, and its ghost asks the input ports for a new blank. The water cells drink water and the cryo cells Voltex coolant from the
 * input ports' tanks (a pack's cells their own fluid); the cryo cells give the same amount of hot coolant to the output ports and work only while it fits.
 * <p>
 * The Watcher (step 10): the reactor gives off a core sample for every {@link Fission#SAMPLE_WORK} of power summed over
 * ticks, into the second output slot and on into the output ports; the Voltex evolution chamber breeds the Watcher
 * nanites on them. A Watcher strain in its slot caps the knob before the hull SCRAMs ({@link Watcher}) and adds to the
 * output by its knowledge.
 * <p>
 * The output: the energy goes into a buffer of {@link #BUFFER}; what does not fit is lost and the fuel burns on (no
 * autoregulator in 1.0, step 17). The knob "share into heat" sends up to that share as heat into the heat pipes next to
 * the Voltex heat ports, at the rate of the Voltex thermogenerator (step 23); what the pipes do not take stays energy.
 * Every tick the core pushes the buffer into whatever takes energy next to its energy output connectors, and cables that
 * draw take it through them ({@link EnergySource}).
 * <p>
 * Saving has a different method on each version: {@code saveAdditional(tag)} and {@code load(tag)} on Forge 1.20.1,
 * {@code saveAdditional(tag, registries)} and {@code loadAdditional(tag, registries)} on NeoForge 1.21.1. The class
 * declares both; each version overrides its own.
 */
public class ReactorCoreBlockEntity extends BlockEntity implements MenuProvider, EnergySource {

    /** Slots: the cells of the zone row by row, then the outputs (spent rods, core samples, channel products), then the Watcher's. */
    public static final int CELLS = Zone.SIZE * Zone.SIZE, OUT_SPENT = CELLS, OUT_SAMPLE = CELLS + 1, OUT_PRODUCT = CELLS + 2, WATCHER = CELLS + 3,
            SLOTS = CELLS + 4;
    /** The energy buffer, FE (step 17). */
    public static final int BUFFER = 10_000_000;
    /** FE a unit of heat stands for, the rate of the Voltex thermogenerator (step 23). */
    public static final int FE_PER_HEAT = 70;
    /** Ticks between the input ports' refills and the wear written into the rods (step 17: a rod a second). */
    private static final int SERVICE_EVERY = 20;
    /** The most hot coolant the output tanks are asked about at once, mB. */
    private static final int HOT_ROOM_ASK = 1_000_000;

    /** What the window shows as the reactor's state. */
    public enum Status { NOT_FORMED, STOPPED, RUNNING, SCRAM, HELD }

    /** The Voltex advancement events of the add-on's branch: the reactor running with power, the hull's own SCRAM. */
    private static final String EVENT_RUNNING = "voltex_nuclear:reactor_running", EVENT_SCRAM = "voltex_nuclear:reactor_scram";

    // the window's buttons (clickMenuButton ids, all under 128: 1.20.1 sends a button as one byte): run or stop, SCRAM or
    // reset, the knobs by 10, the knobs set to 5 * (id - base); a JEI scheme picks a cell, then the part for it (ReactorMenu)
    public static final int B_RUN = 0, B_SCRAM = 1, B_POWER_DOWN = 2, B_POWER_UP = 3, B_SPLIT_DOWN = 4, B_SPLIT_UP = 5,
            B_POWER_SET = 10, B_SPLIT_SET = 40, KNOB_STEPS = 20, B_SCHEME_CELL = 61, B_SCHEME_PART = B_SCHEME_CELL + CELLS;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private final Item[] ghosts = new Item[CELLS];
    private final List<Item> queue = new ArrayList<>();
    /** Channel products waiting for room in their output slot; the reactor is held while any wait, as for the spent rods. */
    private final List<Item> productQueue = new ArrayList<>();
    private final Reactor reactor = new Reactor(new Zone());
    private final Cells cells = new Cells();
    private double energy;
    /** The knobs in percent: power and the share of the output into heat. */
    private int knob = 100, split;
    /** The Watcher nanites in their slot (step 10): the cap they put on the knob, their slips. */
    private final Watcher watcher = new Watcher();
    /** Work towards the next core sample: the power summed over ticks. */
    private double sampleWork;

    // runtime, not saved: the first tick after loading looks again
    private boolean checkPending = true;
    /** The reactor ran with power on the last tick (for the advancement). */
    private boolean wasOn;
    private final Set<BlockPos> claimed = new HashSet<>();
    private List<BlockPos> ports = List.of();
    private int service;
    /** Each fluid drained from the ports but not used yet, mB by its id (the cells use fractions of a mB a tick). */
    private final Map<String, Double> credit = new HashMap<>();
    // what the last tick did, for the window
    private Status status = Status.NOT_FORMED;
    // what the renderer draws (step 16): the part of each cell as its place in ReactorParts.items plus one, the chambers,
    // the power in thousandths and SCRAM; sent to the client only when it changes
    private final int[] liveZone = new int[CELLS];
    private int liveChambers, livePower, liveStatus, liveFe, liveHull;
    private boolean liveScram;

    // the client's copy of it, kept by the update packets: the zone and its flux, the power, SCRAM and when it came
    @Nullable public Zone clientZone;
    public final Part[] clientParts = new Part[CELLS];
    @Nullable public FluxMap clientFlux;
    public double clientPower;
    public boolean clientScram;
    /** For Jade: the state, FE a tick into the buffer, the hull in percent. */
    public Status clientStatus = Status.NOT_FORMED;
    public int clientFe, clientHull;
    public long clientScramAt = Long.MIN_VALUE / 2;
    private double lastPower, lastFe, lastWater, lastCoolant, lastZoneHeat, lastRemoved, lastToHull;
    /** Why cells lacked their fluid on the last tick, {@link #SHORT_WATER} and the others; for the window. */
    private int shortBits;
    private int lastHeat, lastLost, given;
    /** What the Watcher did on the last tick, for the window: none in the slot, watching, or easing the power off. */
    private WatcherState watcherState = WatcherState.NONE;

    public enum WatcherState { NONE, WATCHING, EASED }

    public ReactorCoreBlockEntity(BlockPos pos, BlockState state) {
        super(NuclearBlockEntities.REACTOR_CORE.get(), pos, state);
    }

    // ------------------------------------------------------------------------------------------------------------------
    // The work
    // ------------------------------------------------------------------------------------------------------------------

    void serverTick() {
        if (checkPending) check();
        if (level != null && HiddenPorts.gone(level, worldPosition, ports)) check();
        if (level == null || !formed()) {
            status = Status.NOT_FORMED;
            return;
        }
        Platform platform = VoltexNuclear.platform();
        List<ItemFluidPortBlockEntity> inputs = new ArrayList<>(), outputs = new ArrayList<>();
        List<EnergyOutputPortBlockEntity> outlets = new ArrayList<>();
        List<HeatPortBlockEntity> heat = new ArrayList<>();
        for (BlockPos p : ports) {
            BlockEntity e = level.getBlockEntity(p);
            if (e instanceof ItemFluidPortBlockEntity port) {
                port.linkCore(worldPosition);   // the port is named after this machine in the AE2 terminal
                (port.isOutput() ? outputs : inputs).add(port);
            } else if (e instanceof EnergyOutputPortBlockEntity out) {
                out.link(worldPosition);
                outlets.add(out);
            } else if (e instanceof HeatPortBlockEntity h) {
                heat.add(h);
            }
        }

        if (++service >= SERVICE_EVERY) {
            service = 0;
            refill(inputs);
            writeWear();
        }
        moveOut(outputs);
        moveSamples(outputs);
        reactor.hold(!queue.isEmpty() || !productQueue.isEmpty());

        // the Watcher caps the knob from what it reads of the hull; without one the knob is the player's alone
        NaniteGenome strain = NuclearNanites.watcher(items.get(WATCHER));
        double cap = 1;
        if (strain != null) cap = watcher.tick(NuclearNanites.knowledge(strain), strain.stability(), reactor.hull() / Fission.HULL_CAPACITY, level.random::nextDouble);
        else watcher.reset();
        reactor.setKnob(Math.min(knob / 100.0, cap));

        // the cells may use what the input tanks hold; a cell that gives its fluid back (the cryo cell's coolant, hot) only
        // as much as the output tanks take back
        Map<String, Double> supply = new HashMap<>();
        Map<String, Fluid> back = new HashMap<>();
        shortBits = 0;
        for (Cooling k : reactor.drinking()) {
            Fluid in = fluid(k.fluid()), out = k.output() == null ? null : fluid(k.output());
            if (in == null) continue;
            int have = VoltexPorts.fluid(inputs, in);
            boolean water = Cooling.WATER.equals(k.fluid()), coolant = Cooling.COOLANT.equals(k.fluid());
            if (have <= 0) shortBits |= water ? SHORT_WATER : coolant ? SHORT_COOLANT : SHORT_PACK_FLUID;
            if (out != null) {
                int room = 0;
                for (ItemFluidPortBlockEntity port : outputs) room += platform.fillPort(port, out, HOT_ROOM_ASK, true);
                if (room <= 0 && have > 0) shortBits |= coolant ? SHORT_HOT_ROOM : SHORT_PACK_ROOM;
                have = Math.min(have, room);
                back.putIfAbsent(k.fluid(), out);
            }
            supply.merge(k.fluid(), credit.getOrDefault(k.fluid(), 0.0) + have, Math::min);   // two cells on one fluid: the stricter
        }
        Reactor.Tick t = reactor.tick(supply);
        for (String id : supply.keySet())
            credit.put(id, VoltexPorts.draw(inputs, fluid(id), t.used(id), credit.getOrDefault(id, 0.0), back.get(id), outputs));

        for (Reactor.Burnt b : t.burntOut()) {
            items.set(b.cell().row() * Zone.SIZE + b.cell().col(), ItemStack.EMPTY);   // the ghost stays
            queue.add(ReactorParts.spent(b.part()).getItem());
        }
        for (Reactor.Cell c : t.products()) {   // an irradiated blank has become its product; the ghost asks for a new blank
            int cell = c.row() * Zone.SIZE + c.col();
            ItemStack product = ReactorParts.product(items.get(cell).getItem());
            items.set(cell, ItemStack.EMPTY);
            if (!product.isEmpty()) productQueue.add(product.getItem());
        }
        if (!t.burntOut().isEmpty() || !t.products().isEmpty()) moveOut(outputs);

        // a core sample for every SAMPLE_WORK of power summed over ticks; with the output slot full the work waits for room
        sampleWork = Math.min(Fission.SAMPLE_WORK, sampleWork + t.power());
        if (sampleWork >= Fission.SAMPLE_WORK && addSample()) sampleWork -= Fission.SAMPLE_WORK;

        // the output, with what a knowing Watcher adds: up to the share into heat, what the pipes take; the rest into the buffer
        double produced = t.energy() * (strain != null ? Watcher.bonus(NuclearNanites.knowledge(strain)) : 1);
        int heatGiven = 0;
        if (split > 0 && produced > 0 && !heat.isEmpty()) {
            int want = (int) Math.min(Integer.MAX_VALUE, produced * split / 100 / FE_PER_HEAT);
            for (HeatPortBlockEntity h : heat) heatGiven += toNeighbours(h.getBlockPos(), want - heatGiven, true);
        }
        double fe = Math.max(0, produced - (double) heatGiven * FE_PER_HEAT);
        energy += fe;
        double lost = Math.max(0, energy - BUFFER);
        energy -= lost;
        given = 0;
        for (EnergyOutputPortBlockEntity out : outlets) given += toNeighbours(out.getBlockPos(), (int) Math.min(energy, Integer.MAX_VALUE), false);

        lastPower = t.power();
        lastFe = fe;
        lastHeat = heatGiven;
        lastLost = (int) Math.min(Integer.MAX_VALUE, lost);
        lastWater = t.waterMb();
        lastCoolant = t.coolantMb();
        lastZoneHeat = t.heat();
        lastRemoved = t.removed();
        lastToHull = t.toHull();
        Status was = status;
        status = reactor.scrammed() ? Status.SCRAM : !reactor.running() ? Status.STOPPED : reactor.held() ? Status.HELD : Status.RUNNING;
        // the players nearby earn the branch's advancements; a SCRAM by the button leaves the hull below full
        boolean on = status == Status.RUNNING && t.power() > 0;
        if (on && !wasOn) VoltexEventTrigger.award(level, worldPosition, 16, EVENT_RUNNING);
        wasOn = on;
        if (status == Status.SCRAM && was != Status.SCRAM && reactor.hull() >= Fission.HULL_CAPACITY)
            VoltexEventTrigger.award(level, worldPosition, 16, EVENT_SCRAM);
        // eased: the reactor runs below what the knob and the control rods would give it
        double wanted = Math.max(knob / 100.0, reactor.flux().minPower());
        watcherState = strain == null ? WatcherState.NONE : status == Status.RUNNING && t.power() < wanted - 1e-6 ? WatcherState.EASED : WatcherState.WATCHING;
        setLit(t.power() > 0);
        syncLive();
        setChanged();
    }

    /** The face lights while the reactor runs; the state changes only when the light does (a remesh of the chunk). */
    private void setLit(boolean lit) {
        BlockState s = getBlockState();
        if (s.getValue(ReactorCoreBlock.LIT) != lit) level.setBlock(worldPosition, s.setValue(ReactorCoreBlock.LIT, lit), Block.UPDATE_CLIENTS);
    }

    /** Tells the client what the renderer draws, when it changed since it was told last. */
    private void syncLive() {
        boolean changed = false;
        for (int i = 0; i < CELLS; i++) {
            ItemStack s = items.get(i);
            int k = s.isEmpty() ? 0 : ReactorParts.index(s.getItem()) + 1;
            if (liveZone[i] != k) {
                liveZone[i] = k;
                changed = true;
            }
        }
        int chambers = chamberBits(), power = (int) Math.round(reactor.power() * 1000);
        int fe = (int) Math.round(lastFe), hull = (int) Math.round(reactor.hull() / Fission.HULL_CAPACITY * 100);
        if (chambers != liveChambers || power != livePower || reactor.scrammed() != liveScram || status.ordinal() != liveStatus
                || fe != liveFe || hull != liveHull) {
            liveChambers = chambers;
            livePower = power;
            liveScram = reactor.scrammed();
            liveStatus = status.ordinal();
            liveFe = fe;
            liveHull = hull;
            changed = true;
        }
        if (changed) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    private CompoundTag liveTag() {
        CompoundTag live = new CompoundTag();
        live.putIntArray("Zone", liveZone);
        live.putInt("Chambers", liveChambers);
        live.putInt("Power", livePower);
        live.putBoolean("Scram", liveScram);
        live.putInt("Status", liveStatus);
        live.putInt("Fe", liveFe);
        live.putInt("Hull", liveHull);
        CompoundTag tag = new CompoundTag();
        tag.put("Live", live);
        return tag;
    }

    /** The client: the zone, its flux and the power as the server last told them. */
    private void readLive(CompoundTag live) {
        int[] parts = live.getIntArray("Zone");
        int bits = live.getInt("Chambers");
        Zone zone = new Zone();
        for (Zone.Side s : Zone.Side.values()) zone.setChamber(s, (bits & 1 << s.ordinal()) != 0);
        List<Item> known = ReactorParts.items();
        for (int i = 0; i < CELLS; i++) {
            int k = i < parts.length ? parts[i] : 0;
            Part p = k > 0 && k <= known.size() ? ReactorParts.of(new ItemStack(known.get(k - 1))) : null;
            clientParts[i] = p != null && zone.open(row(i), col(i)) ? p : null;
            if (clientParts[i] != null) zone.set(row(i), col(i), clientParts[i]);
        }
        clientZone = zone;
        clientFlux = FluxMap.of(zone);
        boolean scram = live.getBoolean("Scram");
        if (scram && !clientScram && level != null) clientScramAt = level.getGameTime();
        clientScram = scram;
        clientPower = live.getInt("Power") / 1000.0;
        int s = live.getInt("Status");
        clientStatus = s >= 0 && s < Status.values().length ? Status.values()[s] : Status.NOT_FORMED;
        clientFe = live.getInt("Fe");
        clientHull = live.getInt("Hull");
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

    /**
     * Gives energy (or heat) to the blocks round a port that are not the reactor's own; returns what they took. Energy
     * leaves the buffer as it goes.
     */
    private int toNeighbours(BlockPos port, int amount, boolean asHeat) {
        int took = VoltexPorts.toNeighbours(level, port, amount, asHeat, n -> n.equals(worldPosition) || claimed.contains(n));
        if (!asHeat) energy -= took;
        return took;
    }


    /** The input ports put the remembered part into each empty open cell with a ghost, as it lies there (with its wear). */
    /** The fluid of an id, or null when there is none (a pack's cell names a fluid of its own). */
    private static Fluid fluid(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        Fluid f = rl == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.get(rl);
        return f == Fluids.EMPTY ? null : f;
    }

    private void refill(List<ItemFluidPortBlockEntity> inputs) {
        if (inputs.isEmpty()) return;
        for (int i = 0; i < CELLS; i++) {
            if (ghosts[i] == null || !items.get(i).isEmpty() || !open(i)) continue;
            ItemStack got = VoltexPorts.takeOne(inputs, ghosts[i]);
            if (got.isEmpty()) continue;
            items.set(i, got);
            placePart(i, got);
        }
    }

    /** The spent rods and the channel products: each output slot into the output ports, its queue into the slot. */
    private void moveOut(List<ItemFluidPortBlockEntity> outputs) {
        moveOut(OUT_SPENT, queue, outputs);
        moveOut(OUT_PRODUCT, productQueue, outputs);
    }

    private void moveOut(int slotIndex, List<Item> pending, List<ItemFluidPortBlockEntity> outputs) {
        ItemStack out = items.get(slotIndex);
        if (!out.isEmpty() && !outputs.isEmpty()) items.set(slotIndex, VoltexPorts.put(outputs, out));
        while (!pending.isEmpty()) {
            Item next = pending.get(0);
            ItemStack slot = items.get(slotIndex);
            if (slot.isEmpty()) items.set(slotIndex, new ItemStack(next));
            else if (slot.is(next) && slot.getCount() < slot.getMaxStackSize()) slot.grow(1);
            else break;
            pending.remove(0);
        }
    }

    /** A new core sample into its output slot; false when the slot has no room. */
    private boolean addSample() {
        ItemStack slot = items.get(OUT_SAMPLE);
        if (slot.isEmpty()) items.set(OUT_SAMPLE, new ItemStack(NuclearItems.CORE_SAMPLE.get()));
        else if (slot.is(NuclearItems.CORE_SAMPLE.get()) && slot.getCount() < slot.getMaxStackSize()) slot.grow(1);
        else return false;
        return true;
    }

    /** The core samples: the output slot into the output ports, as the spent rods. */
    private void moveSamples(List<ItemFluidPortBlockEntity> outputs) {
        ItemStack out = items.get(OUT_SAMPLE);
        if (!out.isEmpty() && !outputs.isEmpty()) items.set(OUT_SAMPLE, VoltexPorts.put(outputs, out));
    }

    // ------------------------------------------------------------------------------------------------------------------
    // The zone
    // ------------------------------------------------------------------------------------------------------------------

    private static int row(int cell) {
        return cell / Zone.SIZE;
    }

    private static int col(int cell) {
        return cell % Zone.SIZE;
    }

    private boolean open(int cell) {
        return reactor.zone().open(row(cell), col(cell));
    }

    /** Puts the part of an item into its cell, with the wear the item carries; an item that is no part leaves it empty. */
    private void placePart(int cell, ItemStack stack) {
        Part part = ReactorParts.of(stack);
        if (part == null || !open(cell)) reactor.place(row(cell), col(cell), null);
        else reactor.place(row(cell), col(cell), part, ReactorParts.wear(stack));
    }

    /** Writes a rod's wear, or a channel blank's irradiation, from the reactor into its stack. */
    private void writeWear(int cell) {
        ItemStack s = items.get(cell);
        Part part = ReactorParts.of(s);
        if (part == null || !part.isFuel() && part.kind() != Part.Kind.CHANNEL) return;
        double w = part.isFuel() ? reactor.wear(row(cell), col(cell)) : reactor.channelWork(row(cell), col(cell));
        if (ReactorParts.wear(s) != w) ReactorParts.setWear(s, w);
    }

    private void writeWear() {
        for (int i = 0; i < CELLS; i++) writeWear(i);
    }

    /** The chambers changed: the cells they took away drop what stood in them, and forget it. */
    private void setChambers(Set<Zone.Side> now) {
        if (now.equals(reactor.zone().chambers())) return;
        writeWear();
        for (Zone.Side side : Zone.Side.values()) reactor.zone().setChamber(side, now.contains(side));
        for (int i = 0; i < CELLS; i++) {
            if (open(i)) continue;
            ghosts[i] = null;
            ItemStack s = items.get(i);
            if (s.isEmpty()) continue;
            items.set(i, ItemStack.EMPTY);
            reactor.place(row(i), col(i), null);
            BlockPos front = worldPosition.relative(facing());
            Containers.dropItemStack(level, front.getX() + 0.5, front.getY() + 0.5, front.getZ() + 0.5, s);
        }
        reactor.changed();
        setChanged();
    }

    /** The zone and the ghosts as they are, for the window. */
    public Reactor reactor() {
        return reactor;
    }

    /** Everything the core holds goes into the world: it is being broken. */
    void dropAll() {
        if (level == null || level.isClientSide) return;
        writeWear();
        Containers.dropContents(level, worldPosition, items);
        for (List<Item> pending : List.of(queue, productQueue)) {
            for (Item i : pending) Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, new ItemStack(i));
            pending.clear();
        }
        for (int i = 0; i < SLOTS; i++) items.set(i, ItemStack.EMPTY);
    }

    // ------------------------------------------------------------------------------------------------------------------
    // The window's controls
    // ------------------------------------------------------------------------------------------------------------------

    /** A button of the window (ReactorMenu.clickMenuButton). */
    public void press(int id) {
        if (id == B_RUN) {
            if (reactor.running()) reactor.stop();
            else if (!reactor.scrammed()) reactor.start();
        } else if (id == B_SCRAM) {
            if (reactor.scrammed()) reactor.reset();
            else reactor.scram();
        } else if (id == B_POWER_DOWN || id == B_POWER_UP) {
            knob = clampKnob(knob + (id == B_POWER_UP ? 10 : -10));
        } else if (id == B_SPLIT_DOWN || id == B_SPLIT_UP) {
            split = clampKnob(split + (id == B_SPLIT_UP ? 10 : -10));
        } else if (id >= B_POWER_SET && id <= B_POWER_SET + KNOB_STEPS) {
            knob = 5 * (id - B_POWER_SET);
        } else if (id >= B_SPLIT_SET && id <= B_SPLIT_SET + KNOB_STEPS) {
            split = 5 * (id - B_SPLIT_SET);
        } else {
            return;
        }
        reactor.setKnob(knob / 100.0);
        setChanged();
    }

    private static int clampKnob(int v) {
        return Math.max(0, Math.min(100, v));
    }

    /**
     * A cell of a scheme laid in from JEI (DESIGN step 24): the cell remembers the part as its ghost, and an empty open
     * cell takes one from the player's inventory as it lies there (a worn rod with its wear). A cell holding another part
     * keeps it; the window marks it.
     */
    public void schemeCell(int cell, Item item, Player player) {
        if (cell < 0 || cell >= CELLS || !open(cell) || ReactorParts.of(new ItemStack(item)) == null) return;
        ghosts[cell] = item;
        if (items.get(cell).isEmpty()) {
            Inventory inv = player.getInventory();
            for (int i = 0; i < inv.items.size(); i++) {
                ItemStack s = inv.items.get(i);
                if (!s.is(item)) continue;
                ItemStack one = s.split(1);
                items.set(cell, one);
                placePart(cell, one);
                inv.setChanged();
                break;
            }
        }
        setChanged();
    }

    // ------------------------------------------------------------------------------------------------------------------
    // The energy for the output connectors
    // ------------------------------------------------------------------------------------------------------------------

    @Override
    public int giveEnergy(int max, boolean simulate) {
        if (!formed()) return 0;
        int g = (int) Math.min(max, energy);
        if (!simulate && g > 0) {
            energy -= g;
            given += g;
            setChanged();
        }
        return g;
    }

    @Override
    public int energyStored() {
        return (int) energy;
    }

    @Override
    public int energyCapacity() {
        return BUFFER;
    }

    // ------------------------------------------------------------------------------------------------------------------
    // The structure
    // ------------------------------------------------------------------------------------------------------------------

    /** Look at the blocks on the next tick. */
    public void requestCheck() {
        checkPending = true;
    }

    public boolean formed() {
        return getBlockState().getValue(ReactorCoreBlock.FORMED);
    }

    /** The Voltex ports in the plinth of the formed reactor. */
    public List<BlockPos> ports() {
        return ports;
    }

    /** The sides of the active zone the chambers open. */
    public Set<Zone.Side> chambers() {
        return reactor.zone().chambers();
    }

    private Direction facing() {
        return getBlockState().getValue(ReactorCoreBlock.FACING);
    }

    void check() {
        checkPending = false;
        if (level == null || level.isClientSide) return;
        ReactorStructure.Scan scan = ReactorStructure.scan(level, worldPosition, facing());
        if (!scan.complete()) {
            if (formed() || !claimed.isEmpty()) dismantle();
            return;
        }
        ports = scan.ports();
        claimed.clear();
        for (ReactorStructure.Place p : ReactorStructure.PLACES) {
            if (p == ReactorStructure.CORE) continue;
            BlockPos at = ReactorStructure.pos(worldPosition, facing(), p);
            claimed.add(at);
            claim(at, p);
        }
        setChambers(scan.chambers().isEmpty() ? EnumSet.noneOf(Zone.Side.class) : EnumSet.copyOf(scan.chambers()));
        setCoreState(getBlockState().setValue(ReactorCoreBlock.FORMED, true));
    }

    private void claim(BlockPos at, ReactorStructure.Place p) {
        BlockState s = level.getBlockState(at), want = s;
        if (ReactorStructure.isCasing(s))
            want = NuclearBlocks.REACTOR_FRAME.get().defaultBlockState().setValue(ReactorFrameBlock.PART, p.index()).setValue(ReactorFrameBlock.FACING, facing());
        else if (s.getBlock() instanceof ExpansionChamberBlock)
            want = s.setValue(ExpansionChamberBlock.FORMED, true).setValue(ExpansionChamberBlock.FACING, ReactorStructure.outward(facing(), p));
        else if (s.getBlock() instanceof MultiblockPortBlock)
            want = s.setValue(MultiblockPortBlock.CONCEALED, true);
        if (want != s) level.setBlock(at, want, Block.UPDATE_ALL);
    }

    private void release(BlockPos at) {
        if (!level.isLoaded(at)) return;
        BlockState s = level.getBlockState(at), want = s;
        if (s.is(NuclearBlocks.REACTOR_FRAME.get())) want = NuclearBlocks.NUCLEAR_CASING.get().defaultBlockState();
        else if (s.getBlock() instanceof ExpansionChamberBlock) want = s.setValue(ExpansionChamberBlock.FORMED, false);
        else if (s.getBlock() instanceof MultiblockPortBlock) want = s.setValue(MultiblockPortBlock.CONCEALED, false);
        if (want != s) level.setBlock(at, want, Block.UPDATE_ALL);
        if (level.getBlockEntity(at) instanceof EnergyOutputPortBlockEntity out) out.link(null);
    }

    private void setCoreState(BlockState want) {
        if (want != getBlockState()) level.setBlock(worldPosition, want, Block.UPDATE_CLIENTS);
    }

    /**
     * The reactor comes apart: every block it claimed, and every frame of its places (claims are not saved), shows again
     * as it is. A frame of this reactor is recognised by its place and facing, so a neighbouring reactor is left alone.
     * The zone keeps its parts and its chambers: a reactor put together again goes on where it stood.
     */
    void dismantle() {
        if (level == null || level.isClientSide) return;
        Set<BlockPos> all = new HashSet<>(claimed);
        for (ReactorStructure.Place p : ReactorStructure.PLACES) if (p != ReactorStructure.CORE) all.add(ReactorStructure.pos(worldPosition, facing(), p));
        claimed.clear();
        ports = List.of();
        for (BlockPos p : all) {
            BlockState s = level.isLoaded(p) ? level.getBlockState(p) : null;
            if (s != null && s.is(NuclearBlocks.REACTOR_FRAME.get()) && !ownFrame(p, s)) continue;
            release(p);
        }
        // the world, not the cached state: while the core is being broken the cache still holds it and would put it back
        if (level.getBlockState(worldPosition).is(NuclearBlocks.REACTOR_CORE.get()))
            setCoreState(getBlockState().setValue(ReactorCoreBlock.FORMED, false).setValue(ReactorCoreBlock.LIT, false));
    }

    /** What the renderer may draw: the whole reactor, the lid standing into the block over the top layer. */
    public AABB renderBox() {
        Direction f = facing();
        return new AABB(ReactorStructure.at(worldPosition, f, -2, 0, -2)).minmax(new AABB(ReactorStructure.at(worldPosition, f, 2, ReactorStructure.LAYERS, 2)));
    }

    /** Forge 1.20.1 culls block entity renderers by this box; on NeoForge 1.21.1 the renderer answers instead. */
    public AABB getRenderBoundingBox() {
        return renderBox();
    }

    private boolean ownFrame(BlockPos p, BlockState frame) {
        return ReactorStructure.coreOf(p, frame.getValue(ReactorFrameBlock.FACING), ReactorStructure.PLACES.get(frame.getValue(ReactorFrameBlock.PART))).equals(worldPosition);
    }

    // ------------------------------------------------------------------------------------------------------------------
    // The slots, as the window sees them; pipes and hoppers reach the reactor only through its ports
    // ------------------------------------------------------------------------------------------------------------------

    /**
     * The window's container: the cells and the outputs. A part put in by hand sets the cell's ghost; taken out, it clears
     * the ghost if the ghost names it.
     */
    public final class Cells implements Container {

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

        /** The rod's wear goes into its stack before the stack leaves (a quick move copies it). */
        public void syncWear(int slot) {
            if (slot < CELLS) writeWear(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            syncWear(slot);
            ItemStack taken = items.get(slot).isEmpty() || amount <= 0 ? ItemStack.EMPTY : items.get(slot).split(amount);
            if (taken.isEmpty()) return taken;
            if (slot < CELLS && items.get(slot).isEmpty()) {
                forget(slot, taken.getItem());
                reactor.place(row(slot), col(slot), null);
            }
            setChanged();
            return taken;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return removeItem(slot, items.get(slot).getCount());
        }

        /** By hand: the old stack (going to the cursor or a hotbar slot as it is) gets its wear first. */
        @Override
        public void setItem(int slot, ItemStack stack) {
            syncWear(slot);
            // what stood in the cell: a shift-click has already shrunk the stack itself to nothing
            Part was = slot < CELLS ? reactor.zone().get(row(slot), col(slot)) : null;
            items.set(slot, stack);
            if (slot < CELLS) {
                if (ReactorParts.of(stack) != null) ghosts[slot] = stack.getItem();
                else if (was != null) forget(slot, ReactorParts.item(was));
                placePart(slot, stack);
            }
            setChanged();
        }

        /** A part left its cell by hand: the ghost goes with it, unless it names another part (one a scheme laid over). */
        private void forget(int slot, Item part) {
            if (ghosts[slot] == part) ghosts[slot] = null;
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            if (slot == WATCHER) return NuclearNanites.watcher(stack) != null;
            return slot < CELLS && open(slot) && ReactorParts.of(stack) != null;
        }

        @Override
        public void setChanged() {
            ReactorCoreBlockEntity.this.setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return Container.stillValidBlockEntity(ReactorCoreBlockEntity.this, player);
        }

        @Override
        public void clearContent() {
            for (int i = 0; i < SLOTS; i++) setItem(i, ItemStack.EMPTY);
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    // The window
    // ------------------------------------------------------------------------------------------------------------------

    /**
     * Indices of the numbers the window gets; the client gets 16 bits a cell, so big numbers go in two. Power and hull in
     * thousandths, water and coolant in hundredths of a mB a tick, the chambers as bits (L, R, T, B); the heat the zone
     * gives, the heat its outlets take and the heat that goes into the hull a tick (D_HEAT is the heat sent into the pipes);
     * a ghost as its place in {@link ReactorParts#items} plus one, a load as the heat an outlet was given on the last tick,
     * a took as what it took of it; D_SHORT the bits of {@link #SHORT_WATER} and the others: why cells lacked their fluid.
     */
    public static final int D_STATUS = 0, D_KNOB = 1, D_SPLIT = 2, D_POWER = 3, D_HULL = 4, D_CHAMBERS = 5, D_FE = 6, D_HEAT = 8,
            D_ENERGY = 10, D_GIVEN = 12, D_LOST = 14, D_WATER = 16, D_COOLANT = 17, D_ZONE_HEAT = 18, D_REMOVED = 19, D_TO_HULL = 20,
            D_GHOST = 21, D_LOAD = D_GHOST + CELLS, D_WATCHER = D_LOAD + CELLS, D_TOOK = D_WATCHER + 1, D_SHORT = D_TOOK + CELLS,
            D_COUNT = D_SHORT + 1;

    /** Why cells lacked their fluid on the last tick: none in the input ports, or (for the hot coolant) no room in the output ports. */
    public static final int SHORT_WATER = 1, SHORT_COOLANT = 2, SHORT_HOT_ROOM = 4, SHORT_PACK_FLUID = 8, SHORT_PACK_ROOM = 16;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            if (i >= D_GHOST && i < D_LOAD) return ReactorParts.index(ghosts[i - D_GHOST]) + 1;
            if (i >= D_LOAD && i < D_WATCHER) return (int) Math.min(Short.MAX_VALUE, Math.round(reactor.load(row(i - D_LOAD), col(i - D_LOAD))));
            if (i >= D_TOOK && i < D_SHORT) return (int) Math.min(Short.MAX_VALUE, Math.round(reactor.took(row(i - D_TOOK), col(i - D_TOOK))));
            return switch (i) {
                case D_SHORT -> shortBits;
                case D_WATCHER -> watcherState.ordinal();
                case D_STATUS -> status.ordinal();
                case D_KNOB -> knob;
                case D_SPLIT -> split;
                case D_POWER -> (int) Math.round(lastPower * 1000);
                case D_HULL -> (int) Math.round(reactor.hull() / Fission.HULL_CAPACITY * 1000);
                case D_CHAMBERS -> chamberBits();
                case D_FE -> (int) lastFe & 0xFFFF;
                case D_FE + 1 -> (int) lastFe >>> 16;
                case D_HEAT -> lastHeat & 0xFFFF;
                case D_HEAT + 1 -> lastHeat >>> 16;
                case D_ENERGY -> (int) energy & 0xFFFF;
                case D_ENERGY + 1 -> (int) energy >>> 16;
                case D_GIVEN -> given & 0xFFFF;
                case D_GIVEN + 1 -> given >>> 16;
                case D_LOST -> lastLost & 0xFFFF;
                case D_LOST + 1 -> lastLost >>> 16;
                case D_WATER -> (int) Math.min(Short.MAX_VALUE, Math.round(lastWater * 100));
                case D_COOLANT -> (int) Math.min(Short.MAX_VALUE, Math.round(lastCoolant * 100));
                case D_ZONE_HEAT -> (int) Math.min(Short.MAX_VALUE, Math.round(lastZoneHeat));
                case D_REMOVED -> (int) Math.min(Short.MAX_VALUE, Math.round(lastRemoved));
                case D_TO_HULL -> (int) Math.min(Short.MAX_VALUE, Math.round(lastToHull));
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

    private int chamberBits() {
        int bits = 0;
        for (Zone.Side s : reactor.zone().chambers()) bits |= 1 << s.ordinal();
        return bits;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ReactorMenu(id, inv, cells, data, ContainerLevelAccess.create(level, worldPosition));
    }

    // ------------------------------------------------------------------------------------------------------------------
    // Saving, in both spellings
    // ------------------------------------------------------------------------------------------------------------------

    private void write(CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        writeWear();
        VoltexNuclear.platform().saveItems(tag, items, registries);
        ListTag g = new ListTag();
        for (Item i : ghosts) g.add(StringTag.valueOf(i == null ? "" : BuiltInRegistries.ITEM.getKey(i).toString()));
        tag.put("Ghosts", g);
        ListTag q = new ListTag();
        for (Item i : queue) q.add(StringTag.valueOf(BuiltInRegistries.ITEM.getKey(i).toString()));
        tag.put("Queue", q);
        ListTag pq = new ListTag();
        for (Item i : productQueue) pq.add(StringTag.valueOf(BuiltInRegistries.ITEM.getKey(i).toString()));
        tag.put("ProductQueue", pq);
        tag.putDouble("Energy", energy);
        tag.putInt("Knob", knob);
        tag.putInt("Split", split);
        tag.putDouble("Hull", reactor.hull());
        tag.putBoolean("Running", reactor.running());
        tag.putBoolean("Scram", reactor.scrammed());
        tag.putByte("Chambers", (byte) chamberBits());
        tag.putDouble("SampleWork", sampleWork);
        tag.putDouble("WatcherCap", watcher.cap());
        tag.putInt("WatcherClock", watcher.clock());
        tag.putString("WatcherSlip", watcher.slip().name());
        tag.putInt("WatcherSlipLeft", watcher.slipLeft());
    }

    private void read(CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        if (tag.contains("Live")) {   // an update packet on the client
            readLive(tag.getCompound("Live"));
            return;
        }
        for (int i = 0; i < SLOTS; i++) items.set(i, ItemStack.EMPTY);
        VoltexNuclear.platform().loadItems(tag, items, registries);
        int bits = tag.getByte("Chambers");
        for (Zone.Side s : Zone.Side.values()) reactor.zone().setChamber(s, (bits & 1 << s.ordinal()) != 0);
        for (int i = 0; i < CELLS; i++) placePart(i, items.get(i));
        ListTag g = tag.getList("Ghosts", Tag.TAG_STRING);
        for (int i = 0; i < CELLS; i++) ghosts[i] = i < g.size() ? item(g.getString(i)) : null;
        readQueue(tag.getList("Queue", Tag.TAG_STRING), queue);
        readQueue(tag.getList("ProductQueue", Tag.TAG_STRING), productQueue);
        energy = Math.max(0, Math.min(BUFFER, tag.getDouble("Energy")));
        knob = tag.contains("Knob") ? clampKnob(tag.getInt("Knob")) : 100;
        split = clampKnob(tag.getInt("Split"));
        reactor.setKnob(knob / 100.0);
        reactor.restore(tag.getDouble("Hull"), tag.getBoolean("Running"), tag.getBoolean("Scram"));
        sampleWork = Math.max(0, Math.min(Fission.SAMPLE_WORK, tag.getDouble("SampleWork")));
        Watcher.Slip slip = Watcher.Slip.NONE;
        for (Watcher.Slip s : Watcher.Slip.values()) if (s.name().equals(tag.getString("WatcherSlip"))) slip = s;
        watcher.restore(tag.contains("WatcherCap") ? tag.getDouble("WatcherCap") : 1, tag.getInt("WatcherClock"), slip, tag.getInt("WatcherSlipLeft"));
    }

    private static void readQueue(ListTag list, List<Item> into) {
        into.clear();
        for (int i = 0; i < list.size(); i++) {
            Item it = item(list.getString(i));
            if (it != null) into.add(it);
        }
    }

    @Nullable
    private static Item item(String id) {
        ResourceLocation rl = id.isEmpty() ? null : ResourceLocation.tryParse(id);
        Item i = rl == null ? Items.AIR : BuiltInRegistries.ITEM.get(rl);
        return i == Items.AIR ? null : i;
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
