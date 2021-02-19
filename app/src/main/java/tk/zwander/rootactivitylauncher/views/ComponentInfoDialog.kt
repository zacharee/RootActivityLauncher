package tk.zwander.rootactivitylauncher.views

import android.content.Context
import android.content.pm.*
import android.os.Build
import android.text.Html
import android.text.Spanned
import android.util.PrintWriterPrinter
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textview.MaterialTextView
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.util.map
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.math.sign

class ComponentInfoDialog(context: Context, info: Any) : MaterialAlertDialogBuilder(context) {
    init {
        setTitle(R.string.component_info)
        setPositiveButton(android.R.string.ok, null)

        val message = when (info) {
            is ActivityInfo -> processActivityInfo(info)
            is ServiceInfo -> processServiceInfo(info)
            is PackageInfo -> processPackageInfo(info)
            else -> null
        }

        setMessage(message)
    }

    override fun create(): AlertDialog {
        return super.create().apply {
            setOnShowListener {
                window?.findViewById<MaterialTextView>(android.R.id.message)?.apply {
                    setTextIsSelectable(true)
                }
            }
        }
    }

    fun processServiceInfo(info: ServiceInfo): Spanned {
        val sWriter = StringWriter()
        val pWriter = PrintWriter(sWriter)
        val printer = PrintWriterPrinter(pWriter)

        info.dump(printer, "")

        val string = formatDump(sWriter.toString())

        sWriter.close()
        pWriter.close()

        return Html.fromHtml(
            string
        )
    }

    fun processActivityInfo(info: ActivityInfo): Spanned {
        val sWriter = StringWriter()
        val pWriter = PrintWriter(sWriter)
        val printer = PrintWriterPrinter(pWriter)

        info.dump(printer, "")

        val string = formatDump(sWriter.toString())

        sWriter.close()
        pWriter.close()

        return Html.fromHtml(
            string
        )
    }

    fun processPackageInfo(info: PackageInfo): Spanned {
        val sWriter = StringWriter()
        val pWriter = PrintWriter(sWriter)
        val printer = PrintWriterPrinter(pWriter)

        info.packageName?.let {
            printer.println("packageName=$it")
        }

        info.splitNames?.let {
            printer.println("splitNames=${it.contentToString()}")
        }

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
            printer.println("sharedUserLabel=$it")
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
            it.forEachIndexed { index, item ->
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
                perm.protectionLevel.let { level ->
                    printer.println("    protectionLevel=$level")
                }
                perm.flags.let { flags ->
                    printer.println("    flags=$flags")
                }
                perm.group?.let { group ->
                    printer.println("    group=$group")
                }
                perm.backgroundPermission?.let { p ->
                    printer.println("    backgroundPermission=$p")
                }
                perm.descriptionRes.let { res ->
                    printer.println("    descriptionRes=$res")
                }
                perm.requestRes.let { res ->
                    printer.println("    requestRes=$res")
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

        info.isStub.let {
            printer.println("isStub=$it")
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

        if (info.isOverlayPackage) {
            info.overlayTarget?.let {
                printer.println("overlayTarget=$it")
            }

            info.targetOverlayableName?.let {
                printer.println("targetOverlayableName=$it")
            }

            info.overlayCategory?.let {
                printer.println("overlayCategory=$it")
            }

            info.overlayPriority.let {
                printer.println("overlayPriority=$it")
            }

            info.mOverlayIsStatic.let {
                printer.println("mOverlayIsStatic=$it")
            }
        }

        info.compileSdkVersion.let {
            printer.println("compileSdkVersion=$it")
        }

        info.compileSdkVersionCodename?.let {
            printer.println("compileSdkVersionCodename=$it")
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            info.isApex.let {
                printer.println("isApex=$it")
            }
        }

        val string = formatDump(sWriter.toString())

        sWriter.close()
        pWriter.close()

        return Html.fromHtml(
                string
        )
    }

    private fun formatDump(dump: String): String {
        val string = StringBuilder()
        dump.lines().apply {
            forEachIndexed { index, it ->
                val startsWithTwoSpaces = it.startsWith("  ")
                val formatted = it
                    .replace("  ", "&nbsp;&nbsp;")
                    .replace(", ", ",&nbsp;")
                    .replace(Regex(" "), "<br />${if (startsWithTwoSpaces) "&nbsp;&nbsp;" else ""}<b>")
                    .replace(Regex("(\r\n|\n)"), "<br />")
                    .replace(Regex("^&nbsp;&nbsp;"), "&nbsp;&nbsp;<b>")
                    .replaceFirst(Regex("^(?!&nbsp;&nbsp;)"), "<b>")
                    .replace("=", "</b>=")
                    .replace(Regex(":$"), "</b>:")

                when {
                    index == lastIndex -> string.append(formatted)
                    formatted.indexOf(":") == formatted.lastIndex -> string.append("$formatted<br />")
                    else -> string.appendHtmlLn(formatted)
                }
            }
        }
        return string.toString()
    }

    private fun StringBuilder.appendHtmlLn(line: Any?): StringBuilder {
        return append("$line<br><br>")
    }
}