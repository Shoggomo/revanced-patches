package app.revanced.patches.layout

import app.revanced.patcher.PatcherData
import app.revanced.patcher.extensions.AccessFlagExtensions.Companion.or
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.*
import app.revanced.patcher.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patcher.signature.MethodMetadata
import app.revanced.patcher.signature.MethodSignature
import app.revanced.patcher.signature.MethodSignatureMetadata
import app.revanced.patcher.signature.PatternScanMethod
import app.revanced.patcher.smali.asInstructions
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.builder.instruction.BuilderInstruction22c
import org.jf.dexlib2.builder.instruction.BuilderInstruction35c
import org.jf.dexlib2.iface.instruction.formats.Instruction22c
import org.jf.dexlib2.iface.instruction.formats.Instruction35c
import org.jf.dexlib2.immutable.ImmutableMethod
import org.jf.dexlib2.immutable.ImmutableMethodImplementation

private val compatiblePackages = arrayOf("com.google.android.youtube")

class HideSuggestionsPatch : Patch(
    metadata = PatchMetadata(
        "hide-suggestions",
        "Hide suggestions patch",
        "Hide suggested videos.",
        compatiblePackages,
        "1.0.0"
    ),
    signatures = listOf(
        MethodSignature(
            methodSignatureMetadata = MethodSignatureMetadata(
                name = "hide-suggestions-parent-method",
                methodMetadata = MethodMetadata(null, null), // unknown
                patternScanMethod = PatternScanMethod.Fuzzy(2), // FIXME: Test this threshold and find the best value.
                compatiblePackages = compatiblePackages,
                description = "Signature for a parent method, which is needed to find the actual method required to be patched.",
                version = "0.0.1"
            ),
            returnType = "V",
            accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
            methodParameters = listOf("L", "Z"),
            opcodes = listOf(
                Opcode.CONST_4,
                Opcode.IF_EQZ,
                Opcode.IGET,
                Opcode.AND_INT_LIT16,
                Opcode.IF_EQZ,
                Opcode.IGET_OBJECT,
                Opcode.IF_NEZ,
                Opcode.SGET_OBJECT,
                Opcode.IGET,
                Opcode.CONST,
                Opcode.IF_NE,
                Opcode.IGET_OBJECT,
                Opcode.IF_NEZ,
                Opcode.SGET_OBJECT,
                Opcode.IGET,
                Opcode.IF_NE,
                Opcode.IGET_OBJECT,
                Opcode.CHECK_CAST,
                Opcode.GOTO,
                Opcode.SGET_OBJECT,
                Opcode.GOTO,
                Opcode.CONST_4,
                Opcode.IF_EQZ,
                Opcode.IGET_BOOLEAN,
                Opcode.IF_EQ
            )
        )
    )
) {
    override fun execute(patcherData: PatcherData): PatchResult {
        val result = signatures.first().result!!.findParentMethod(
            MethodSignature(
                methodSignatureMetadata = MethodSignatureMetadata(
                    name = "hide-suggestions-method",
                    methodMetadata = MethodMetadata(null, null), // unknown
                    patternScanMethod = PatternScanMethod.Fuzzy(2), // FIXME: Test this threshold and find the best value.
                    compatiblePackages = compatiblePackages,
                    description = "Signature for the method, which is required to be patched.",
                    version = "0.0.1"
                ),
                returnType = "V",
                accessFlags = AccessFlags.FINAL or AccessFlags.PUBLIC,
                listOf("Z"),
                listOf(
                    Opcode.IPUT_BOOLEAN,
                    Opcode.IGET_OBJECT,
                    Opcode.IGET_BOOLEAN,
                    Opcode.INVOKE_VIRTUAL,
                    Opcode.RETURN_VOID
                )
            )
        ) ?: return PatchResultError("Method old-quality-patch-method has not been found")

        // deep clone the method in order to add a new register
        // TODO: replace by a mutable method implementation with settable register count when available
        val originalMethod = result.immutableMethod
        val originalImplementation = originalMethod.implementation!!
        val clonedMethod = ImmutableMethod(
            originalMethod.returnType,
            originalMethod.name,
            originalMethod.parameters,
            originalMethod.returnType,
            originalMethod.accessFlags,
            originalMethod.annotations,
            originalMethod.hiddenApiRestrictions,
            ImmutableMethodImplementation(
                originalImplementation.registerCount + 1, // additional register for the boolean
                originalImplementation.instructions,
                originalImplementation.tryBlocks,
                originalImplementation.debugItems,
            )
        ).toMutable() // create mutable clone out of the immutable method clone

        val clonedImplementation = clonedMethod.implementation!!

        return PatchResultSuccess() // TODO: fix below

        // fix the instructions registers
        clonedImplementation.instructions.forEachIndexed { index, it ->
            val opcode = it.opcode
            // increment all registers (instance register and object register) by 1
            // due to adding a new virtual register for the boolean value
            clonedImplementation.replaceInstruction(
                index,
                when (it) {
                    is Instruction22c -> BuilderInstruction22c(
                        opcode,
                        it.registerA + 1, // increment register
                        it.registerB + 1, // increment register
                        it.reference
                    )
                    is Instruction35c -> BuilderInstruction35c(
                        opcode,
                        1,
                        it.registerC + 1, // increment register
                        0,
                        0,
                        0,
                        0,
                        it.reference
                    )
                    else -> return@forEachIndexed
                }
            )
        }

        // resolve the class proxy
        val clazz = result.definingClassProxy.resolve()

        // remove the old method & add the clone with our additional register
        clazz.methods.remove(originalMethod)
        clazz.methods.add(clonedMethod)

        // Proxy the first parameter of our clone by passing it to the RemoveSuggestions method
        // TODO: this crashes, find out why
        clonedImplementation.addInstructions(
            0,
            """
                invoke-static/range { v2 .. v2 }, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;
                move-result-object v0
                invoke-static { v0 }, Lfi/razerman/youtube/XAdRemover;->RemoveSuggestions(Ljava/lang/Boolean;)Ljava/lang/Boolean;
                move-result-object v0
                invoke-virtual/range { v0 .. v0 }, Ljava/lang/Boolean;->booleanValue()Z
                move-result v2
            """.trimIndent().asInstructions()
        )
        return PatchResultSuccess()
    }
}