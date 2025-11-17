package org.nguh.nguhcraft.server

import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.LeavesBlock
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.tags.BlockTags
import net.minecraft.tags.ItemTags
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.level.ServerLevel
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import java.util.*


/**
 * This class handles chopping down an entire tree if a user breaks
 * part of it with an axe.
 */
class TreeToChop private constructor(private val Owner: ServerPlayer, private val WoodTypes: Array<Block>) {
    interface Accessor {
        fun `Nguhcraft$StartChoppingTree`(Tree: TreeToChop?)
        fun `Nguhcraft$GetTrees`(): MutableList<TreeToChop>
    }

    private val TreeBlocks: ArrayList<BlockPos> = ArrayList()
    private var Index: Int = 0
    private val Axe: ItemStack = Owner.mainHandItem

    private fun Add(pos: BlockPos) {
        TreeBlocks.add(pos)
    }

    private fun Block(): BlockPos {
        return TreeBlocks[Index]
    }

    private fun Chop(SW: ServerLevel) {
        if (Done()) return

        // Chop an entire level if this tree is multiple blocks wide.
        val Y = Block().y
        do {
            val B = Next()
            val State = SW.getBlockState(B)

            // Guard against race condition: the block at this position may have
            // been changed to something else after chopping started.
            if (!WoodTypes.contains(State.block)) continue

            // Drop the stacks manually because we need to pass in the axe for
            // enchantments like smelting to work properly.
            SW.destroyBlock(B, false, Owner)
            Block.dropResources(State, SW, B, null, Owner, Axe)
        } while (!Done() && Block().y == Y)
    }

    private fun Done(): Boolean {
        return Index >= TreeBlocks.size
    }

    private fun Finish(SL: ServerLevel) {
        TreeBlocks.sortWith(Comparator.comparingInt { obj: BlockPos -> obj.y })
        (SL as Accessor).`Nguhcraft$StartChoppingTree`(this)
    }

    private fun Next(): BlockPos {
        return TreeBlocks[Index++]
    }

    private fun Owner(): ServerPlayer {
        return Owner
    }

    companion object {
        private val LOG_TO_LEAVES: Map<Block, Array<Block>> = mapOf(
            Blocks.ACACIA_LOG to arrayOf(Blocks.ACACIA_LEAVES),
            Blocks.BIRCH_LOG to arrayOf(Blocks.BIRCH_LEAVES),
            Blocks.CHERRY_LOG to arrayOf(Blocks.CHERRY_LEAVES),
            Blocks.DARK_OAK_LOG to arrayOf(Blocks.DARK_OAK_LEAVES),
            Blocks.JUNGLE_LOG to arrayOf(Blocks.JUNGLE_LEAVES),
            Blocks.MANGROVE_LOG to arrayOf(Blocks.MANGROVE_LEAVES),
            Blocks.OAK_LOG to arrayOf(Blocks.OAK_LEAVES, Blocks.AZALEA_LEAVES, Blocks.FLOWERING_AZALEA_LEAVES),
            Blocks.SPRUCE_LOG to arrayOf(Blocks.SPRUCE_LEAVES),
            Blocks.PALE_OAK_LOG to arrayOf(Blocks.PALE_OAK_LEAVES)
        )

        private val WOOD_TYPES: Map<Block, Array<Block>> = mapOf(
            Blocks.ACACIA_LOG to arrayOf(Blocks.ACACIA_LOG),
            Blocks.BIRCH_LOG to arrayOf(Blocks.BIRCH_LOG),
            Blocks.CHERRY_LOG to arrayOf(Blocks.CHERRY_LOG),
            Blocks.DARK_OAK_LOG to arrayOf(Blocks.DARK_OAK_LOG),
            Blocks.JUNGLE_LOG to arrayOf(Blocks.JUNGLE_LOG),
            Blocks.MANGROVE_LOG to arrayOf(Blocks.MANGROVE_LOG, Blocks.MANGROVE_ROOTS),
            Blocks.OAK_LOG to arrayOf(Blocks.OAK_LOG),
            Blocks.SPRUCE_LOG to arrayOf(Blocks.SPRUCE_LOG),
            Blocks.PALE_OAK_LOG to arrayOf(Blocks.PALE_OAK_LOG)
        )

        private const val LOG_MARKED_FOR_SCAN = false
        private const val LOG_PROCESSED = true

        // Make sure we don't start creating new trees while we're chopping
        // them down; that shouldn't be possible, but we set the player as the
        // entity causing the chopping, so who knows what could happen.
        private var Chopping = false

        /** A block was just broken. Check if we need to chop down a tree. */
        @JvmStatic
        fun ActOnBlockDestroyed(
            Level: Level,
            Block: BlockPos,
            State: BlockState,
            Player: Player
        ) {
            if (
                Player is ServerPlayer &&
                !Chopping &&
                Level is ServerLevel &&
                (
                    State.`is`(BlockTags.OVERWORLD_NATURAL_LOGS) ||
                    State.`is`(Blocks.MANGROVE_ROOTS)
                ) &&
                Player.mainHandItem.`is`(ItemTags.AXES)
            ) ChopDownTree(Player, Level, Block, State)
        }

        @JvmStatic
        fun Tick(SL: ServerLevel) {
            val Trees = (SL as Accessor).`Nguhcraft$GetTrees`()
            Chopping = true
            Trees.forEach { it.Chop(SL) }
            Trees.removeIf { it.Done() }
            Chopping = false
        }

        private fun ChopDownTree(
            SP: ServerPlayer,
            SL: ServerLevel,
            Start: BlockPos,
            Wood: BlockState
        ) {
            // Flood fill to find the entire tree.
            val Q = ArrayDeque<BlockPos>()
            val Visited = HashMap<BlockPos, Boolean?>()
            val WoodTypes: Array<Block> = GetWoodTypes(Wood.block)
            val Tree = TreeToChop(SP, WoodTypes)
            val Leaves: Array<Block> = Objects.requireNonNull<Array<Block>>(LOG_TO_LEAVES[WoodTypes[0]])
            val ChopDownwards = Leaves[0] === Blocks.MANGROVE_LEAVES
            var SeenNonPersistentLeaves = false

            // For this, look at surrounding blocks and search for blocks that
            // have the same block type as the starting block, as well as for
            // leaves of the same wood type; if we can find any leaves that are
            // non-persistent, chop down all logs above the starting position.
            Q.add(Start.immutable())
            while (!Q.isEmpty()) {
                val Block = Q.remove()

                // Mark the log as fully processed; do not process the same log twice.
                if (LOG_PROCESSED == Visited.put(Block, LOG_PROCESSED)) continue

                // If this is the corresponding leaves block, and the leaves are
                // persistent, then a player has placed them here; do not chop
                // anything down here.
                //
                // If the leaves are non-persistent, we can chop down the tree; do
                // not look at any surrounding blocks.
                val St = SL.getBlockState(Block)
                val B = St.block
                if (Leaves.contains(B)) {
                    if (St.getValue(LeavesBlock.PERSISTENT)) return
                    SeenNonPersistentLeaves = true
                } else if (WoodTypes.contains(B)) {
                    Tree.Add(Block)

                    // Some trees are really dumb and generate in non-obvious ways, the worst
                    // offender arguably being acacia trees; for those, we also need to check
                    // the blocks that are a layer above the current block.
                    //
                    // Even otherwise fairly normal trees (like oaks) generate in weird ways
                    // in jungles, so just visit everything as part of the flood fill every
                    // time.
                    val Up = Block.above()
                    VisitBlock(Q, Visited, Up)
                    VisitBlock(Q, Visited, Block.north())
                    VisitBlock(Q, Visited, Block.east())
                    VisitBlock(Q, Visited, Block.south())
                    VisitBlock(Q, Visited, Block.west())
                    VisitBlock(Q, Visited, Block.west().north())
                    VisitBlock(Q, Visited, Block.west().south())
                    VisitBlock(Q, Visited, Block.east().north())
                    VisitBlock(Q, Visited, Block.east().south())
                    VisitBlock(Q, Visited, Up.north())
                    VisitBlock(Q, Visited, Up.east())
                    VisitBlock(Q, Visited, Up.south())
                    VisitBlock(Q, Visited, Up.west())
                    VisitBlock(Q, Visited, Up.west().north())
                    VisitBlock(Q, Visited, Up.west().south())
                    VisitBlock(Q, Visited, Up.east().north())
                    VisitBlock(Q, Visited, Up.east().south())

                    // Do chop downwards if these are Mangrove roots because it looks stupid
                    // if only the roots are left (re ‘that is not how trees work’: they’re
                    // roots; they’d probably fall over or something).
                    if (ChopDownwards && B == Blocks.MANGROVE_ROOTS) VisitBlock(Q, Visited, Block.below())
                }
            }

            // We’re done with the flood fill; if we have seen only non-persistent
            // leaves, we can mark the tree for chopping down.
            if (SeenNonPersistentLeaves) Tree.Finish(SL)
        }

        // Thank you, Minecraft, for adding Mangrove Roots.
        //
        // Returns an array of all block types that are considered ‘logs’ for the
        // purpose of chopping down this tree; the ‘primary log’, which can be used
        // to find the corresponding leaves block, is always the first element.
        private fun GetWoodTypes(Wood: Block): Array<Block> {
            var W = Wood
            if (W === Blocks.MANGROVE_ROOTS) W = Blocks.MANGROVE_LOG
            return Objects.requireNonNull<Array<Block>>(WOOD_TYPES[W])
        }

        // Used by ChopDownTree().
        private fun VisitBlock(
            Q: ArrayDeque<BlockPos>,
            Visited: HashMap<BlockPos, Boolean?>,
            Block: BlockPos
        ) {
            if (Visited.putIfAbsent(Block, LOG_MARKED_FOR_SCAN) == null) Q.add(Block)
        }
    }
}