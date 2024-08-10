package xyz.xenondevs.nova.patch.impl.worldgen

import net.minecraft.core.BlockPos
import net.minecraft.server.level.WorldGenRegion
import net.minecraft.util.RandomSource
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.WorldGenLevel
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.levelgen.feature.OreFeature
import net.minecraft.world.level.levelgen.feature.ReplaceBlockFeature
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration
import net.minecraft.world.level.levelgen.structure.templatesystem.PosRuleTest
import net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorRule
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleProcessor
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest
import org.objectweb.asm.Opcodes.INVOKESTATIC
import org.objectweb.asm.Opcodes.INVOKEVIRTUAL
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.MethodInsnNode
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.calls
import xyz.xenondevs.bytebase.util.next
import xyz.xenondevs.bytebase.util.previousLabel
import xyz.xenondevs.bytebase.util.replaceFirst
import xyz.xenondevs.commons.collections.findNthOfType
import xyz.xenondevs.nova.patch.MultiTransformer
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.BLOCK_GETTER_GET_BLOCK_STATE_METHOD
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.FEATURE_PLACE_CONTEXT_RANDOM_METHOD
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.ORE_FEATURE_CAN_PLACE_ORE_METHOD
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.ORE_FEATURE_DO_PLACE_METHOD
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.PROCESSOR_RULE_INPUT_PREDICATE_FIELD
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.PROCESSOR_RULE_LOC_PREDICATE_FIELD
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.PROCESSOR_RULE_POS_PREDICATE_FIELD
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.PROCESSOR_RULE_TEST_METHOD
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.REPLACE_BLOCK_PLACE_METHOD
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.RULE_PROCESSOR_PROCESS_BLOCK_METHOD
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.RULE_TEST_TEST_METHOD
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.TARGET_BLOCK_STATE_TARGET_FIELD
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import xyz.xenondevs.nova.world.generation.ExperimentalWorldGen
import xyz.xenondevs.nova.world.generation.ruletest.NovaRuleTest

/**
 * This patch allows [NovaRuleTest]s to be used in [OreFeature]s, [ReplaceBlockFeature]s and structure [ProcessorRule]s
 */
@OptIn(ExperimentalWorldGen::class)
internal object NovaRuleTestPatch : MultiTransformer(setOf(OreFeature::class, ReplaceBlockFeature::class, RuleProcessor::class), computeFrames = true) {
    
    override fun transform() {
        transformOreFeature()
        transformReplaceBlockFeature()
        transformProcessorRule()
    }
    
    private fun transformOreFeature() {
        val placeMethod = VirtualClassPath[ORE_FEATURE_DO_PLACE_METHOD]
        placeMethod.localVariables?.clear()
        val canPlaceCall = placeMethod.instructions.find { it.opcode == INVOKESTATIC && (it as MethodInsnNode).calls(ORE_FEATURE_CAN_PLACE_ORE_METHOD) } as MethodInsnNode
        val cantPlaceLabel = (canPlaceCall.next as JumpInsnNode).label
        val prevLabel = canPlaceCall.previousLabel()
        placeMethod.instructions.insertBefore(prevLabel, buildInsnList {
            addLabel()
            aLoad(56)
            aLoad(2)
            aLoad(23)
            aLoad(1)
            aLoad(58)
            invokeStatic(ReflectionUtils.getMethodByName(NovaRuleTestPatch::class, false, "checkOreNovaRuleTest"))
            ifeq(cantPlaceLabel)
        })
        val canPlaceMethod = VirtualClassPath[ORE_FEATURE_CAN_PLACE_ORE_METHOD]
        val novaRuleLabel = canPlaceMethod.instructions.findNthOfType<LabelNode>(2)
        canPlaceMethod.instructions.insert(buildInsnList {
            addLabel()
            aLoad(4)
            getField(TARGET_BLOCK_STATE_TARGET_FIELD)
            instanceOf(NovaRuleTest::class)
            ifne(novaRuleLabel)
        })
    }
    
    private fun transformReplaceBlockFeature() {
        val placeMethod = VirtualClassPath[REPLACE_BLOCK_PLACE_METHOD]
        placeMethod.localVariables?.clear()
        val testCall = placeMethod.instructions.find { it.opcode == INVOKEVIRTUAL && (it as MethodInsnNode).calls(RULE_TEST_TEST_METHOD) }!!
        val testLabel = testCall.previousLabel()
        val falseLabel = (testCall.next as JumpInsnNode).label
        val trueLabel = testCall.next(2) as LabelNode
        
        placeMethod.instructions.insertBefore(testLabel, buildInsnList {
            addLabel()
            aLoad(6)
            getField(TARGET_BLOCK_STATE_TARGET_FIELD)
            instanceOf(NovaRuleTest::class)
            ifeq(testLabel)
            
            addLabel()
            aLoad(2)
            aLoad(3)
            invokeInterface(BLOCK_GETTER_GET_BLOCK_STATE_METHOD, isInterface = true)
            aLoad(1)
            invokeVirtual(FEATURE_PLACE_CONTEXT_RANDOM_METHOD)
            aLoad(3)
            aLoad(2)
            aLoad(6)
            invokeStatic(ReflectionUtils.getMethodByName(NovaRuleTestPatch::class, false, "checkOreNovaRuleTest"))
            ifne(trueLabel)
            
            addLabel()
            goto(falseLabel)
        })
    }
    
    private fun transformProcessorRule() {
        // Replace ProcessorRule#test with static call to NovaRuleTestPatch#testProcessorRule
        // https://i.imgur.com/k824LTh.png -> https://i.imgur.com/4UIDGNP.png
        VirtualClassPath[RULE_PROCESSOR_PROCESS_BLOCK_METHOD].replaceFirst(
            0,
            0,
            buildInsnList {
                aLoad(1)
                invokeStatic(ReflectionUtils.getMethodByName(NovaRuleTestPatch::class, false, "testProcessorRule"))
            }
        ) { it.opcode == INVOKEVIRTUAL && (it as MethodInsnNode).calls(PROCESSOR_RULE_TEST_METHOD) }
    }
    
    @JvmStatic
    fun checkOreNovaRuleTest(
        state: BlockState,
        random: RandomSource,
        pos: BlockPos,
        levelReader: WorldGenLevel,
        targetState: OreConfiguration.TargetBlockState
    ): Boolean {
        val ruleTest = targetState.target
        if (ruleTest !is NovaRuleTest) return true
        val level = (levelReader as? WorldGenRegion)?.level ?: return false
        
        return ruleTest.test(level, pos, state, random)
    }
    
    @JvmStatic
    fun testProcessorRule(
        rule: ProcessorRule,
        inputState: BlockState,
        locState: BlockState,
        pos1: BlockPos,
        pos2: BlockPos,
        pos3: BlockPos,
        random: RandomSource,
        levelReader: LevelReader
    ): Boolean {
        val inputPredicate = PROCESSOR_RULE_INPUT_PREDICATE_FIELD[rule] as RuleTest
        val locPredicate = PROCESSOR_RULE_LOC_PREDICATE_FIELD[rule] as RuleTest
        val posPredicate = PROCESSOR_RULE_POS_PREDICATE_FIELD[rule] as PosRuleTest
        val level = (levelReader as? WorldGenRegion)?.level
        
        require(inputPredicate !is NovaRuleTest) { "Input predicate of ProcessorRule must not be a NovaRuleTest." } // TODO
        if (!inputPredicate.test(inputState, random)) return false
        
        if (locPredicate is NovaRuleTest) {
            if (level != null && !locPredicate.test(level, pos2, locState, random)) return false
        } else if (!locPredicate.test(locState, random)) return false
        
        return posPredicate.test(pos1, pos2, pos3, random)
    }
    
}