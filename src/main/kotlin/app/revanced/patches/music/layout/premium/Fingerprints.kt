package app.revanced.patches.music.layout.premium

import app.revanced.patcher.fingerprint.methodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val hideGetPremiumFingerprint = methodFingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("V")
    parameters()
    opcodes(
        Opcode.IF_NEZ,
        Opcode.CONST_16,
        Opcode.INVOKE_VIRTUAL,
    )
    strings("FEmusic_history", "FEmusic_offline")
}

internal val membershipSettingsFingerprint = methodFingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("Ljava/lang/CharSequence;")
    opcodes(
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IF_EQZ,
        Opcode.IGET_OBJECT,
    )
}