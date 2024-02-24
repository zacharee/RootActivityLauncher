package tk.zwander.rootactivitylauncher.util

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.pm.ComponentInfo
import android.content.pm.PackageInfo
import android.content.pm.PermissionInfo
import android.content.pm.ServiceInfo
import android.content.pm.Signature
import android.os.Build
import android.util.PrintWriterPrinter
import android.util.Printer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.PrintWriter
import java.io.StringWriter

fun processServiceInfo(info: ServiceInfo): List<CharSequence> {
    val sWriter = StringWriter()
    val pWriter = PrintWriter(sWriter)
    val printer = PrintWriterPrinter(pWriter)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        try {
            printer.println("type=${ServiceInfo.foregroundServiceTypeToLabel(info.foregroundServiceType)}")
        } catch (_: Exception) {}
    }

    info.dump(printer, "")

    sWriter.close()
    pWriter.close()

    return sWriter.toString().lines()
}

fun processActivityInfo(info: ActivityInfo): List<CharSequence> {
    val sWriter = StringWriter()
    val pWriter = PrintWriter(sWriter)
    val printer = PrintWriterPrinter(pWriter)

    if (Build.VERSION.SDK_INT < 31) {
        info.dump(printer, "")
    } else {
        if (info.permission != null) {
            printer.println("permission=${info.permission}")
        }

        printer.println(
            "taskAffinity=${info.taskAffinity} " +
                    "targetActivity=${info.targetActivity} " +
                    "persistableMode=${info.persistableModeToString()}"
        )

        if (info.launchMode != 0 || info.flags != 0 || info.privateFlags != 0 || info.theme != 0) {
            printer.println(
                "launchMode=${info.launchMode} " +
                        "flags=0x${info.flags.hexString} " +
                        "privateFlags=0x${info.privateFlags.hexString} " +
                        "theme=0x${info.theme.hexString}"
            )
        }

        if (info.screenOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED || info.configChanges != 0 || info.softInputMode != 0) {
            printer.println(
                "screenOrientation=${info.screenOrientation} " +
                        "configChanges=0x${info.configChanges.hexString} " +
                        "softInputMode=0x${info.softInputMode.hexString}"
            )
        }

        if (info.uiOptions != 0) {
            printer.println("uiOptions=0x${info.uiOptions.hexString}")
        }

        printer.println("lockTaskLaunchMode=${ActivityInfo.lockTaskLaunchModeToString(info.lockTaskLaunchMode)}")

        if (info.windowLayout != null) {
            printer.println("windowLayout=${info.windowLayout.width}|${info.windowLayout.widthFraction}, ${info.windowLayout.height}|${info.windowLayout.heightFraction}, ${info.windowLayout.gravity}")
        }

        printer.println("resizeMode=${ActivityInfo.resizeModeToString(info.resizeMode)}")

        if (info.requestedVrComponent != null) {
            printer.println("requestedVrComponent=${info.requestedVrComponent}")
        }

        if (info.rMaxAspectRatio != 0f) {
            printer.println("maxAspectRatio=${info.rMaxAspectRatio}")
        }

        if (info.manifestMinAspectRatio != 0f) {
            printer.println("manifestMinAspectRatio=${info.manifestMinAspectRatioCompat}")
        }

        if (info.supportsSizeChanges) {
            printer.println("supportsSizeChanges=true")
        }

        ComponentInfo::class.java.getDeclaredMethod(
            "dumpBack",
            Printer::class.java,
            String::class.java
        )
            .apply { isAccessible = true }
            .invoke(info, printer, "")
    }

    sWriter.close()
    pWriter.close()

    return sWriter.toString().lines()
}

@SuppressLint("PrivateApi", "DiscouragedPrivateApi")
fun processPackageInfo(info: PackageInfo): List<CharSequence> {
    val sWriter = StringWriter()
    val pWriter = PrintWriter(sWriter)
    val printer = PrintWriterPrinter(pWriter)

    info.packageName?.let {
        printer.println("packageName=$it")
    }

    info.splitNames?.let {
        printer.println("splitNames=${it.contentToString()}")
    }

    @Suppress("DEPRECATION")
    info.versionCode.let {
        printer.println("versionCode=$it")
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        info.longVersionCode.let {
            printer.println("longVersionCode=$it")
        }
    }

    info.versionName?.let {
        printer.println("versionName=$it")
    }

    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
        info.baseRevisionCode.let {
            printer.println("baseRevisionCode=$it")
        }

        info.splitRevisionCodes?.let {
            printer.println("splitRevisionCodes=${it.contentToString()}")
        }
    }

    info.sharedUserId?.let {
        printer.println("sharedUserId=$it")
    }

    info.sharedUserLabel.let {
        if (it != 0) {
            printer.println("sharedUserLabel=0x${it.hexString}")
        }
    }

    info.firstInstallTime.let {
        printer.println("firstInstallTime=$it")
    }

    info.lastUpdateTime.let {
        printer.println("lastUpdateTime=$it")
    }

    info.gids?.let {
        printer.println("gids=${it.contentToString()}")
    }

    info.instrumentation?.let {
        printer.println("instrumentation:")
        it.forEachIndexed { _, item ->
            printer.println("  instrumentation_info:")
            printer.println("    targetPackage=${item.targetPackage}")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                printer.println("    targetProcesses=${item.targetProcesses}")
            }
            printer.println("    sourceDir=${item.sourceDir}")
            printer.println("    publicSourceDir=${item.publicSourceDir}")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                item.splitNames?.let { names ->
                    printer.println("    splitNames=${names.contentToString()}")
                }
            }
            item.splitSourceDirs?.let { dirs ->
                printer.println("    splitSourceDirs=${dirs.contentToString()}")
            }
            item.splitPublicSourceDirs?.let { dirs ->
                printer.println("    splitPublicSourceDirs=${dirs.contentToString()}")
            }
            item.splitDependencies?.let { deps ->
                printer.println("    splitDependencies=${deps.map { dep -> dep.contentToString() }}")
            }
            item.dataDir?.let { dir ->
                printer.println("    dataDir=$dir")
            }
            item.deviceProtectedDataDir?.let { dir ->
                printer.println("    deviceProtectedDataDir=$dir")
            }
            item.credentialProtectedDataDir?.let { dir ->
                printer.println("    credentialProtectedDataDir=$dir")
            }
            item.primaryCpuAbi?.let { abi ->
                printer.println("    primaryCpuAbi=$abi")
            }
            item.secondaryCpuAbi?.let { abi ->
                printer.println("    secondaryCpuAbi=$abi")
            }
            item.nativeLibraryDir?.let { dir ->
                printer.println("    nativeLibraryDir=$dir")
            }
            item.secondaryNativeLibraryDir?.let { dir ->
                printer.println("    secondaryNativeLibraryDir=$dir")
            }
            item.handleProfiling.let { prof ->
                printer.println("    handleProfiling=$prof")
            }
            item.functionalTest.let { test ->
                printer.println("    functionalTest=$test")
            }
        }
    }

    info.permissions?.let { perms ->
        printer.println("permissions:")
        perms.forEach { perm ->
            printer.println("  permission:")
            perm.name?.let { name ->
                printer.println("    name=$name")
            }
            perm.packageName?.let { name ->
                printer.println("    packageName=$name")
            }
            perm.labelRes.let { res ->
                if (res != 0) {
                    printer.println("    labelRes=0x${res.hexString}")
                }
            }
            perm.nonLocalizedLabel?.let { label ->
                printer.println("    nonLocalizedLabel=$label")
            }
            perm.icon.let { icon ->
                if (icon != 0) {
                    printer.println("    icon=0x${icon.hexString}")
                }
            }
            perm.banner.let { banner ->
                if (banner != 0) {
                    printer.println("    banner=${banner.hexString}")
                }
            }
            @Suppress("DEPRECATION")
            perm.protectionLevel.let { level ->
                printer.println("    protectionLevel=${PermissionInfo.protectionToString(level)}")
            }
            perm.flags.let { flags ->
                printer.println("    flags=$flags")
            }
            perm.group?.let { group ->
                printer.println("    group=$group")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                perm.backgroundPermission?.let { p ->
                    printer.println("    backgroundPermission=$p")
                }
            }
            perm.descriptionRes.let { res ->
                printer.println("    descriptionRes=0x${res.hexString}")
            }
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                perm.requestRes.let { res ->
                    printer.println("    requestRes=0x${res.hexString}")
                }
            }
            perm.nonLocalizedDescription?.let { desc ->
                printer.println("    nonLocalizedDescription=$desc")
            }
        }
    }

    info.requestedPermissions?.let {
        printer.println("requestedPermissions:")
        it.forEach { p ->
            printer.println("  $p")
        }
    }

    info.requestedPermissionsFlags?.let {
        printer.println("requestedPermissionsFlags=${it.contentToString()}")
    }

    fun printSignature(prefix: String, signature: Signature) {
        printer.println("${prefix}signature:")

        signature.flags.let { flags ->
            printer.println("$prefix  flags=$flags")
        }
        signature.toByteArray()?.let { s ->
            printer.println("$prefix  mSignature=${s.contentToString()}")
        }
        signature.toCharsString()?.let { s ->
            printer.println("$prefix  mStringRef=$s")
        }
        signature.publicKey?.let { key ->
            printer.println("$prefix  publicKey:")
            printer.println("$prefix    algorithm=${key.algorithm}")
            printer.println("$prefix    format=${key.format}")
            printer.println("$prefix    encoded=${key.encoded.contentToString()}")
        }
        printer.println("$prefix  chainSignatures:")
        signature.chainSignatures.forEach { cs ->
            printSignature("$prefix  ", cs)
        }
    }

    @Suppress("DEPRECATION")
    info.signatures?.let { sigs ->
        printer.println("signatures:")
        sigs.forEach { sig ->
            printSignature("  ", sig)
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        info.signingInfo?.let { si ->
            printer.println("signingInfo:")
            printer.println("  hasMultipleSigners=${si.hasMultipleSigners()}")
            printer.println("  hasPastSigningCertificates=${si.hasPastSigningCertificates()}")
            printer.println("  signingCertificateHistory:")
            si.signingCertificateHistory?.let { hi ->
                hi.forEach { h ->
                    printSignature("    ", h)
                }
            }
            printer.println("  apkContentsSigners:")
            si.apkContentsSigners?.let { hi ->
                hi.forEach { h ->
                    printSignature("    ", h)
                }
            }
        }
    }

    info.configPreferences?.let {
        printer.println("configPreferences=${it.contentToString()}")
    }

    info.reqFeatures?.let {
        printer.println("reqFeatures:")
        it.forEach { f ->
            printer.println("  $f")
        }
    }

    info.featureGroups?.let {
        printer.println("featureGroups:")
        it.forEach { g ->
            printer.println("  group-features:")
            g.features.forEach { f ->
                printer.println("    $f")
            }
        }
    }

    info.installLocation.let {
        printer.println("installLocation=$it")
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        info.isStub.let {
            printer.println("isStub=$it")
        }
    }

    info.coreApp.let {
        printer.println("coreApp=$it")
    }

    info.requiredForAllUsers.let {
        printer.println("requiredForAllApps=$it")
    }

    info.restrictedAccountType?.let {
        printer.println("restrictedAccountType=$it")
    }

    info.requiredAccountType?.let {
        printer.println("requiredAccountType=$it")
    }

    if (info.overlayTarget != null) {
        info.overlayTarget?.let {
            printer.println("overlayTarget=$it")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            info.targetOverlayableName?.let {
                printer.println("targetOverlayableName=$it")
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.overlayCategory?.let {
                printer.println("overlayCategory=$it")
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            info.overlayPriority.let {
                printer.println("overlayPriority=$it")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // mOverlayIsStatic is package-protected on Android 10.
                PackageInfo::class.java
                    .getDeclaredField("mOverlayIsStatic")
                    .apply { isAccessible = true }
                    .get(info).let {
                        printer.println("mOverlayIsStatic=$it")
                    }
            } else {
                try {
                    PackageInfo::class.java
                        .getDeclaredField("isStaticOverlay")
                        .get(info).let {
                            printer.println("isStaticOverlay=$it")
                        }
                } catch (ignored: NoSuchFieldException) {
                }
            }
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        info.compileSdkVersion.let {
            printer.println("compileSdkVersion=$it")
        }

        info.compileSdkVersionCodename?.let {
            printer.println("compileSdkVersionCodename=$it")
        }
    }

    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
        info.isApex.let {
            printer.println("isApex=$it")
        }
    }

    info.applicationInfo.let {
        printer.println("applicationInfo:")
        it.dump(printer, "  ")
    }

    sWriter.close()
    pWriter.close()

    return sWriter.toString().lines()
}

fun formatDump(dump: List<CharSequence>, firstPass: Boolean = true): List<AnnotatedString> {
    val ret = ArrayList<AnnotatedString>()

    dump.forEach { line ->
        val actualLine = if (firstPass) {
            val regex = Regex("(?<=[^:])$")
            line.replace(regex, "\n")
        } else line

        val built = buildAnnotatedString {
            append(actualLine)

            Regex("[a-zA-Z\\d]+(=|: |:$)")
                .findAll(actualLine)
                .forEach { result ->
                    addStyle(
                        style = SpanStyle(
                            fontWeight = FontWeight.Bold
                        ),
                        start = result.range.first,
                        end = result.range.last
                    )
                }
        }

        ret.add(built)
    }

    return ret
}

private val applyMutex = Mutex()

suspend fun applyQuery(
    highlightColor: Color,
    dump: List<CharSequence>,
    query: CharSequence,
    firstPass: Boolean,
): List<AnnotatedString> {
    return applyMutex.withLock {
        val formatted = formatDump(dump.map { it.toString() }, firstPass)

        val lines = if (query.isNotBlank()) {
            formatted.map { line ->
                buildAnnotatedString {
                    append(line)

                    val regex = Regex(Regex.escape(query.toString()), RegexOption.IGNORE_CASE)
                    regex.findAll(line)
                        .forEach { result ->
                            addStyle(
                                style = SpanStyle(
                                    color = highlightColor
                                ),
                                start = result.range.first,
                                end = result.range.last + 1
                            )
                        }
                }
            }
        } else {
            formatted
        }

        lines
    }
}
