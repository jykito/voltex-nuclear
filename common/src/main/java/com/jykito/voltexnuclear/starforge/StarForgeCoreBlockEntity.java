package com.jykito.voltexnuclear.starforge;

import com.jykito.industrialcore.block.custom.MultiblockPortBlock;
import com.jykito.industrialcore.block.entity.HeatPortBlockEntity;
import com.jykito.industrialcore.block.entity.ItemFluidPortBlockEntity;
import com.jykito.voltexnuclear.VoltexNuclear;
import com.jykito.voltexnuclear.content.NuclearBlockEntities;
import com.jykito.voltexnuclear.content.NuclearBlocks;
import com.jykito.voltexnuclear.content.NuclearItems;
import com.jykito.voltexnuclear.core.starforge.StarForge;
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
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The star forge's core (DESIGN steps 12, 21 and 28).
 * <p>
 * The structure, as the torus's: it looks at its blocks only when asked (step 16, no timers): when it is placed, when a
 * block of the forge is placed near it, broken or changed next to it, when it is clicked and once on its first tick after
 * loading. Formed, it turns its casing into frame blocks that know their place and hides the Voltex ports in its stands
 * (the renderer draws the stand and the port's square there); it gives them back when it comes apart.
 * <p>
 * The work: its heat is the hottest of the Voltex heat ports in its stands (the hottest, not their sum: a single port
 * right against the torus or the reactor is what makes the star heat; heat pipes hold no more than 4 000). Once a second the
 * input ports top up the slots with Voltex kronium alloy, palladium and helium cells; whenever the forge is at the star heat
 * and nothing melts, a melt takes its inputs and starts ({@link StarForge}); a melt that loses its heat waits. The star
 * alloy and the empty cell go out through the output ports.
 * <p>
 * Saving has a different method on each version, see {@link ReactorCoreBlockEntity}: the class declares both spellings.
 */
public class StarForgeCoreBlockEntity extends BlockEntity implements MenuProvider, WindowNumbers {

    /** Slots: the kronium alloy, the palladium and the helium cells in, the star alloy and the empty cells out. */
    public static final int IN_KRONIUM = 0, IN_PALLADIUM = 1, IN_HELIUM = 2, OUT_ALLOY = 3, OUT_CELL = 4, SLOTS = 5;
    /** Ticks between two top-ups from the input ports. */
    private static final int SERVICE_EVERY = 20;

    /** What the window says the forge is doing. */
    public enum Status { MELTING, COLD, NO_INPUT, OUTPUT_FULL }

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private final Slots slots = new Slots();
    private final StarForge.Melt melt = new StarForge.Melt();
    private int made;

    // runtime, not saved: found again after loading
    private boolean checkPending = true;
    private final Set<BlockPos> claimed = new HashSet<>();
    private List<BlockPos> ports = List.of();
    private int service;
    /** This tick, for the window. */
    private int heat, heatPorts;
    private Status status = Status.COLD;

    public StarForgeCoreBlockEntity(BlockPos pos, BlockState state) {
        super(NuclearBlockEntities.STAR_FORGE_CORE.get(), pos, state);
    }

    // ------------------------------------------------------------------------------------------------------------------
    // The work
    // ------------------------------------------------------------------------------------------------------------------

    void serverTick() {
        if (checkPending) check();
        if (level != null && HiddenPorts.gone(level, worldPosition, ports)) check();
        if (level == null || !formed()) return;
        List<ItemFluidPortBlockEntity> inputs = new ArrayList<>(), outputs = new ArrayList<>();
        heat = heatPorts = 0;
        for (BlockPos p : ports) {
            BlockEntity e = level.getBlockEntity(p);
            if (e instanceof ItemFluidPortBlockEntity port) {
                port.linkCore(worldPosition);   // the port is named after this machine in the AE2 terminal
                (port.isOutput() ? outputs : inputs).add(port);
            } else if (e instanceof HeatPortBlockEntity h) {
                heat = Math.max(heat, h.getHeat());
                heatPorts++;
            }
        }

        if (++service >= SERVICE_EVERY) {
            service = 0;
            topUp(inputs, IN_KRONIUM, kronium());
            topUp(inputs, IN_PALLADIUM, NuclearItems.PALLADIUM.get());
            topUp(inputs, IN_HELIUM, NuclearItems.HELIUM_CELL.get());
        }
        for (int slot : new int[]{OUT_ALLOY, OUT_CELL})
            if (!items.get(slot).isEmpty() && !outputs.isEmpty()) items.set(slot, VoltexPorts.put(outputs, items.get(slot)));

        boolean hot = heat >= StarForge.STAR_HEAT;
        if (!melt.running()) {
            if (!hasInputs()) status = Status.NO_INPUT;
            else if (!room(OUT_ALLOY) || !room(OUT_CELL)) status = Status.OUTPUT_FULL;
            else if (!hot) status = Status.COLD;
            else {
                items.get(IN_KRONIUM).shrink(StarForge.KRONIUM);
                items.get(IN_PALLADIUM).shrink(StarForge.PALLADIUM);
                items.get(IN_HELIUM).shrink(StarForge.HELIUM);
                melt.start();
            }
        }
        if (melt.running()) {
            status = hot ? Status.MELTING : Status.COLD;
            if (melt.tick(heat)) {
                add(OUT_ALLOY, NuclearItems.STAR_ALLOY.get());
                add(OUT_CELL, NuclearItems.EMPTY_CELL.get());
                made++;
            }
        }
        setLit(melt.running() && hot);
        setChanged();
    }

    private static Item kronium() {
        return VoltexNuclear.voltexItem("kronium_alloy");
    }

    private boolean hasInputs() {
        return items.get(IN_KRONIUM).getCount() >= StarForge.KRONIUM && items.get(IN_PALLADIUM).getCount() >= StarForge.PALLADIUM
                && items.get(IN_HELIUM).getCount() >= StarForge.HELIUM;
    }

    /** The input ports top up a slot with its item, to a full stack. */
    private void topUp(List<ItemFluidPortBlockEntity> inputs, int slot, Item item) {
        if (inputs.isEmpty()) return;
        ItemStack s = items.get(slot);
        int n = Math.min(item.getDefaultInstance().getMaxStackSize() - s.getCount(), VoltexPorts.count(inputs, item));
        if (n <= 0) return;
        VoltexPorts.take(inputs, item, n);
        if (s.isEmpty()) items.set(slot, new ItemStack(item, n));
        else s.grow(n);
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

    /** The face lights and the star burns while the forge melts; the state changes only when the light does (a remesh). */
    private void setLit(boolean lit) {
        BlockState s = getBlockState();
        if (s.getValue(StarForgeCoreBlock.LIT) != lit) level.setBlock(worldPosition, s.setValue(StarForgeCoreBlock.LIT, lit), Block.UPDATE_CLIENTS);
    }

    /** Everything the core holds goes into the world: it is being broken. */
    void dropAll() {
        if (level == null || level.isClientSide) return;
        Containers.dropContents(level, worldPosition, items);
        for (int i = 0; i < SLOTS; i++) items.set(i, ItemStack.EMPTY);
    }

    // ------------------------------------------------------------------------------------------------------------------
    // The structure
    // ------------------------------------------------------------------------------------------------------------------

    /** Look at the blocks on the next tick. */
    public void requestCheck() {
        checkPending = true;
    }

    public boolean formed() {
        return getBlockState().getValue(StarForgeCoreBlock.FORMED);
    }

    /** The Voltex ports in the stands of the formed forge. */
    public List<BlockPos> ports() {
        return ports;
    }

    private Direction facing() {
        return getBlockState().getValue(StarForgeCoreBlock.FACING);
    }

    void check() {
        checkPending = false;
        if (level == null || level.isClientSide) return;
        StarForgeStructure.Scan scan = StarForgeStructure.scan(level, worldPosition, facing());
        if (!scan.complete()) {
            if (formed() || !claimed.isEmpty()) dismantle();
            return;
        }
        ports = scan.ports();
        claimed.clear();
        for (StarForgeStructure.Place p : StarForgeStructure.PLACES) {
            if (p == StarForgeStructure.CORE) continue;
            BlockPos at = StarForgeStructure.pos(worldPosition, facing(), p);
            claimed.add(at);
            claim(at, p);
        }
        setCoreState(getBlockState().setValue(StarForgeCoreBlock.FORMED, true));
    }

    private void claim(BlockPos at, StarForgeStructure.Place p) {
        BlockState s = level.getBlockState(at), want = s;
        if (StarForgeStructure.isCasing(s))
            want = NuclearBlocks.STAR_FORGE_FRAME.get().defaultBlockState().setValue(StarForgeFrameBlock.PART, p.index()).setValue(StarForgeFrameBlock.FACING, facing());
        else if (s.getBlock() instanceof MultiblockPortBlock)
            want = s.setValue(MultiblockPortBlock.CONCEALED, true);
        if (want != s) level.setBlock(at, want, Block.UPDATE_ALL);
    }

    private void release(BlockPos at) {
        if (!level.isLoaded(at)) return;
        BlockState s = level.getBlockState(at), want = s;
        if (s.is(NuclearBlocks.STAR_FORGE_FRAME.get())) want = NuclearBlocks.NUCLEAR_CASING.get().defaultBlockState();
        else if (s.getBlock() instanceof MultiblockPortBlock && s.getValue(MultiblockPortBlock.CONCEALED)) want = s.setValue(MultiblockPortBlock.CONCEALED, false);
        if (want != s) level.setBlock(at, want, Block.UPDATE_ALL);
    }

    private void setCoreState(BlockState want) {
        if (want != getBlockState()) level.setBlock(worldPosition, want, Block.UPDATE_CLIENTS);
    }

    /**
     * The forge comes apart: every block it claimed and every frame of its places (claims are not saved) shows again as it
     * is. A frame of this forge is recognised by its place and facing, so a neighbouring forge is left alone. A melt under
     * way waits for the forge to stand again.
     */
    void dismantle() {
        if (level == null || level.isClientSide) return;
        Set<BlockPos> all = new HashSet<>(claimed);
        for (StarForgeStructure.Place p : StarForgeStructure.PLACES)
            if (p != StarForgeStructure.CORE) all.add(StarForgeStructure.pos(worldPosition, facing(), p));
        claimed.clear();
        ports = List.of();
        for (BlockPos p : all) {
            BlockState s = level.isLoaded(p) ? level.getBlockState(p) : null;
            if (s != null && s.is(NuclearBlocks.STAR_FORGE_FRAME.get()) && !ownPlace(p, s)) continue;
            release(p);
        }
        // the world, not the cached state: while the core is being broken the cache still holds it and would put it back
        if (level.getBlockState(worldPosition).is(NuclearBlocks.STAR_FORGE_CORE.get()))
            setCoreState(getBlockState().setValue(StarForgeCoreBlock.FORMED, false).setValue(StarForgeCoreBlock.LIT, false));
    }

    /** Whether a frame stands in a place of this very forge, by its place and facing. */
    private boolean ownPlace(BlockPos p, BlockState s) {
        StarForgeStructure.Place place = StarForgeStructure.PLACES.get(s.getValue(StarForgeFrameBlock.PART));
        return StarForgeStructure.coreOf(p, s.getValue(StarForgeFrameBlock.FACING), place).equals(worldPosition);
    }

    // ------------------------------------------------------------------------------------------------------------------
    // The slots, as the window sees them; pipes and hoppers reach the forge only through its ports
    // ------------------------------------------------------------------------------------------------------------------

    /** The window's container: the three inputs and the two outputs. */
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
            StarForgeCoreBlockEntity.this.setChanged();
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

    /** What a slot takes from the player: its own input in each input slot, nothing in the outputs. */
    public static boolean accepts(int slot, ItemStack stack) {
        return switch (slot) {
            case IN_KRONIUM -> stack.is(kronium());
            case IN_PALLADIUM -> stack.is(NuclearItems.PALLADIUM.get());
            case IN_HELIUM -> stack.is(NuclearItems.HELIUM_CELL.get());
            default -> false;
        };
    }

    // ------------------------------------------------------------------------------------------------------------------
    // The window
    // ------------------------------------------------------------------------------------------------------------------

    /** Indices of the numbers the window gets; the client gets 16 bits a cell, so the heat and the count go in two. */
    public static final int D_STATUS = 0, D_HEAT = 1, D_DONE = 3, D_MADE = 4, D_HEAT_PORTS = 6, D_COUNT = 7;

    @Override
    public ContainerData windowNumbers() {
        return data;
    }

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case D_STATUS -> status.ordinal();
                case D_HEAT -> heat & 0xFFFF;
                case D_HEAT + 1 -> heat >>> 16;
                case D_DONE -> (int) Math.round(melt.done() * 1000);
                case D_MADE -> made & 0xFFFF;
                case D_MADE + 1 -> made >>> 16;
                case D_HEAT_PORTS -> heatPorts;
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
        return new StarForgeMenu(id, inv, slots, data, ContainerLevelAccess.create(level, worldPosition));
    }

    /** What the renderer may draw: the whole forge, the crown and its star standing over the top block. */
    public AABB renderBox() {
        Direction f = facing(), r = f.getCounterClockWise();
        BlockPos a = worldPosition.relative(r, -2).relative(f, -2 - StarForgeStructure.CORE.z()).below(StarForgeStructure.CORE.y());
        BlockPos b = worldPosition.relative(r, 2).relative(f, 2 - StarForgeStructure.CORE.z()).above(StarForgeStructure.LAYERS + 1 - StarForgeStructure.CORE.y());
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
        tag.putBoolean("Melting", melt.running());
        tag.putDouble("Done", melt.done());
        tag.putInt("Made", made);
    }

    private void read(CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        for (int i = 0; i < SLOTS; i++) items.set(i, ItemStack.EMPTY);
        VoltexNuclear.platform().loadItems(tag, items, registries);
        melt.load(tag.getBoolean("Melting"), tag.getDouble("Done"));
        made = Math.max(0, tag.getInt("Made"));
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
