package tk.zwander.rootactivitylauncher.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.*
import android.graphics.Typeface
import android.os.Build
import android.text.*
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.PrintWriterPrinter
import android.util.Printer
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.databinding.ComponentInfoDialogBinding
import tk.zwander.rootactivitylauncher.util.*
import java.io.PrintWriter
import java.io.StringWriter

class ComponentInfoDialog(context: Context, private val info: Any) : MaterialAlertDialogBuilder(context), TextWatcher, CoroutineScope by MainScope() {
    private val view = ComponentInfoDialogBinding.inflate(LayoutInflater.from(context))
    private val highlightColor = ContextCompat.getColor(context, R.color.colorPrimaryDark)

    private var queryJob: Job? = null

    private val dump: SpannableStringBuilder = SpannableStringBuilder()
        get() {
            if (field.isBlank()) {
                field.append(
                    when (info) {
                        is ActivityInfo -> processActivityInfo(info)
                        is ServiceInfo -> processServiceInfo(info)
                        is PackageInfo -> processPackageInfo(info)
                        else -> ""
                    }
                )
                formatDump(field)
            }

            return field
        }

    init {
        setTitle(R.string.component_info)
        setPositiveButton(android.R.string.ok, null)

        setView(view.root)

        (context as LifecycleOwner).lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                cancel()
            }
        })

        launch(Dispatchers.IO) {
            val dump = this@ComponentInfoDialog.dump

            withContext(Dispatchers.Main) {
                view.loader.isVisible = false
                view.bodyWrapper.isVisible = true
                view.message.setText(dump, TextView.BufferType.SPANNABLE)
            }
        }
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        handleQuery(s)
    }

    override fun afterTextChanged(s: Editable?) {}
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    private fun handleQuery(query: CharSequence) {
        queryJob?.cancel()
        queryJob = launch(Dispatchers.IO) {
            val applied = applyQuery(dump, query)

            withContext(Dispatchers.Main) {
                view.message.text = SpannableStringBuilder(applied)
            }
        }
    }

    override fun create(): AlertDialog {
        return super.create().apply {
            setOnShowListener {
                view.infoSearchInput.addTextChangedListener(this@ComponentInfoDialog)
            }

            setOnDismissListener {
                view.infoSearchInput.removeTextChangedListener(this@ComponentInfoDialog)
            }
        }
    }

    private fun processServiceInfo(info: ServiceInfo): CharSequence {
        val sWriter = StringWriter()
        val pWriter = PrintWriter(sWriter)
        val printer = PrintWriterPrinter(pWriter)

        info.dump(printer, "")

        sWriter.close()
        pWriter.close()

        return sWriter.toString()
    }

    private fun processActivityInfo(info: ActivityInfo): CharSequence {
        val sWriter = StringWriter()
        val pWriter = PrintWriter(sWriter)
        val printer = PrintWriterPrinter(pWriter)

        if (Build.VERSION.SDK_INT < 31) {
            info.dump(printer, "")
        } else {
            if (info.permission != null) {
                printer.println("permission=${info.permission}")
            }

            printer.println("taskAffinity=${info.taskAffinity} " +
                    "targetActivity=${info.targetActivity} " +
                    "persistableMode=${info.persistableModeToString()}")

            if (info.launchMode != 0 || info.flags != 0 || info.privateFlags != 0 || info.theme != 0) {
                printer.println("launchMode=${info.launchMode} " +
                        "flags=0x${info.flags.hexString} " +
                        "privateFlags=0x${info.privateFlags.hexString} " +
                        "theme=0x${info.theme.hexString}")
            }

            if (info.screenOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED || info.configChanges != 0 || info.softInputMode != 0) {
                printer.println("screenOrientation=${info.screenOrientation} " +
                        "configChanges=0x${info.configChanges.hexString} " +
                        "softInputMode=0x${info.softInputMode.hexString}")
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

            ComponentInfo::class.java.getDeclaredMethod("dumpBack", Printer::class.java, String::class.java)
                .apply { isAccessible = true }
                .invoke(info, printer, "")
        }

        sWriter.close()
        pWriter.close()

        return sWriter.toString()
    }

    @SuppressLint("PrivateApi")
    private fun processPackageInfo(info: PackageInfo): CharSequence {
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
                    PackageInfo::class.java
                        .getDeclaredField("isStaticOverlay")
                        .get(info).let {
                            printer.println("isStaticOverlay=$it")
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

        return sWriter.toString()
    }

    private fun formatDump(dump: SpannableStringBuilder, firstPass: Boolean = true) {
        Regex("[a-zA-Z\\d]+(=|: |:\n)")
            .findAll(dump)
            .forEach { result ->
                dump.setSpan(StyleSpan(Typeface.BOLD),
                    result.range.first, result.range.last,
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
            }

        if (firstPass) {
            val regex = Regex("[^:]\n")
            var lastIndex = 0

            while (lastIndex < dump.length) {
                val result = regex.find(dump, lastIndex) ?: break
                val range = result.range

                dump.replace(range.first + 1, range.last + 1, "\n\n")

                lastIndex = range.last + 2
            }
        }
    }

    private val applyMutex = Mutex()

    private suspend fun applyQuery(dump: SpannableStringBuilder, query: CharSequence): CharSequence {
        return applyMutex.withLock {
            dump.clearSpans()
            formatDump(dump, false)

            if (query.isNotBlank()) {
//                val indices = arrayListOf<Int>()
//
//                while (indices.isEmpty() || indices.last() < dump.length) {
//                    val index = dump.indexOf(query.toString(), indices.lastOrNull()?.plus(query.length) ?: 0)
//
//                    if (index == -1) {
//                        break
//                    }
//
//                    dump.setSpan(
//                        ForegroundColorSpan(highlightColor),
//                        index, index + query.length,
//                        Spannable.SPAN_INCLUSIVE_INCLUSIVE
//                    )
//
//                    indices.add(index)
//                }

                Regex(Regex.escape(query.toString()), RegexOption.IGNORE_CASE)
                    .findAll(dump)
                    .forEach { result ->
                        dump.setSpan(ForegroundColorSpan(highlightColor),
                            result.range.first, result.range.last + 1,
                            Spannable.SPAN_INCLUSIVE_INCLUSIVE)
                    }
            }

            dump
        }
    }
}