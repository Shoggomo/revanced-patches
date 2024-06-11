package app.revanced.patches.music.layout.compactheader

import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.AccessFlags
import app.revanced.patcher.fingerprint.methodFingerprint

internal val constructCategoryBarFingerprint = methodFingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR)
    returns("V")
    parameters("Landroid/content/Context;", "L", "L", "L")
    opcodes(
        Opcode.IPUT_OBJECT,
        Opcode.CONST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IPUT_OBJECT,
        Opcode.CONST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.NEW_INSTANCE,
        Opcode.INVOKE_DIRECT,
    )
}