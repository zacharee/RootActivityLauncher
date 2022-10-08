package tk.zwander.rootactivitylauncher.data

import android.content.ComponentName
import android.content.Intent
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import tk.zwander.rootactivitylauncher.R

@Parcelize
data class ExtraInfo(
    var key: String,
    var value: String,
    var type: ExtraType = ExtraType.STRING
) : Parcelable

enum class ExtraType(val value: String, val nameRes: Int) {
    INTEGER("integer", R.string.integer) {
        override val shellArgName: String
            get() = "ei"

        override fun putExtra(intent: Intent, key: String, value: String) {
            intent.putExtra(key, value.toIntOrNull())
        }
    },
    BOOLEAN("boolean", R.string.bool) {
        override val shellArgName: String
            get() = "ez"

        override fun putExtra(intent: Intent, key: String, value: String) {
            intent.putExtra(key, value.toBooleanStrictOrNull())
        }
    },
    STRING("string", R.string.str) {
        override val shellArgName: String
            get() = "es"

        override fun putExtra(intent: Intent, key: String, value: String) {
            intent.putExtra(key, value)
        }
    },
    BYTE("byte", R.string.byte_name) {
        override val shellArgName: String
            get() = "ei"

        override fun putExtra(intent: Intent, key: String, value: String) {
            intent.putExtra(key, value.toByteOrNull())
        }
    },
    CHAR("char", R.string.char_name) {
        override val shellArgName: String
            get() = "ei"

        override fun putExtra(intent: Intent, key: String, value: String) {
            intent.putExtra(key, value.toIntOrNull())
        }
    },
    LONG("long", R.string.long_name) {
        override val shellArgName: String
            get() = "el"

        override fun putExtra(intent: Intent, key: String, value: String) {
            intent.putExtra(key, value.toLongOrNull())
        }
    },
    FLOAT("float", R.string.float_name) {
        override val shellArgName: String
            get() = "ef"

        override fun putExtra(intent: Intent, key: String, value: String) {
            intent.putExtra(key, value.toFloatOrNull())
        }
    },
    DOUBLE("double", R.string.double_name) {
        override val shellArgName: String
            get() = "ed"

        override fun putExtra(intent: Intent, key: String, value: String) {
            intent.putExtra(key, value.toDoubleOrNull())
        }
    },
    SHORT("short", R.string.short_name) {
        override val shellArgName: String
            get() = "ei"

        override fun putExtra(intent: Intent, key: String, value: String) {
            intent.putExtra(key, value.toShortOrNull())
        }
    },
    INT_LIST("int_list", R.string.int_al) {
        override val shellArgName: String
            get() = "eial"

        override fun putExtra(intent: Intent, key: String, value: String) {
            intent.putExtra(key, ArrayList(value.split(",").mapNotNull { it.toIntOrNull() }))
        }
    },
    STRING_LIST("string_list", R.string.string_al) {
        override val shellArgName: String
            get() = "esal"

        override fun putExtra(intent: Intent, key: String, value: String) {
            intent.putExtra(
                key,
                ArrayList(value.split(Regex("(?<!\\\\\\\\),")))
            )
        }
    },
    FLOAT_LIST("float_list", R.string.float_al) {
        override val shellArgName: String
            get() = "efal"

        override fun putExtra(intent: Intent, key: String, value: String) {
            intent.putExtra(key, ArrayList(value.split(",").mapNotNull { it.toFloatOrNull() }))
        }
    },
    DOUBLE_LIST("double_list", R.string.double_al) {
        override val shellArgName: String
            get() = "edal"

        override fun putExtra(intent: Intent, key: String, value: String) {
            intent.putExtra(key, ArrayList(value.split(",").mapNotNull { it.toDoubleOrNull() }))
        }
    },
    BYTE_ARRAY("byte_array", R.string.byte_a) {
        override val shellArgName: String
            get() = "eia"

        override fun putExtra(intent: Intent, key: String, value: String) {
            intent.putExtra(key, value.split(",").mapNotNull { it.toByteOrNull() }.toTypedArray())
        }
    },
    SHORT_ARRAY("short_array", R.string.short_a) {
        override val shellArgName: String
            get() = "eia"

        override fun putExtra(intent: Intent, key: String, value: String) {
            intent.putExtra(key, value.split(",").mapNotNull { it.toShortOrNull() }.toTypedArray())
        }
    },
    CHAR_ARRAY("char_array", R.string.char_a) {
        override val shellArgName: String
            get() = "eia"

        override fun putExtra(intent: Intent, key: String, value: String) {
            intent.putExtra(key, value.split(",").mapNotNull { it.firstOrNull() }.toTypedArray())
        }
    },
    INT_ARRAY("int_array", R.string.int_a) {
        override val shellArgName: String
            get() = "eia"

        override fun putExtra(intent: Intent, key: String, value: String) {
            intent.putExtra(key, value.split(",").mapNotNull { it.toIntOrNull() }.toTypedArray())
        }
    },
    LONG_ARRAY("long_array", R.string.long_a) {
        override val shellArgName: String
            get() = "ela"

        override fun putExtra(intent: Intent, key: String, value: String) {
            intent.putExtra(key, value.split(",").mapNotNull { it.toLongOrNull() }.toTypedArray())
        }
    },
    FLOAT_ARRAY("float_array", R.string.float_a) {
        override val shellArgName: String
            get() = "efa"

        override fun putExtra(intent: Intent, key: String, value: String) {
            intent.putExtra(key, value.split(",").mapNotNull { it.toFloatOrNull() }.toTypedArray())
        }
    },
    DOUBLE_ARRAY("double_array", R.string.double_a) {
        override val shellArgName: String
            get() = "eda"

        override fun putExtra(intent: Intent, key: String, value: String) {
            intent.putExtra(key, value.split(",").mapNotNull { it.toDoubleOrNull() }.toTypedArray())
        }
    },
    STRING_ARRAY("string_array", R.string.string_a) {
        override val shellArgName: String
            get() = "esa"

        override fun putExtra(intent: Intent, key: String, value: String) {
            intent.putExtra(key, value.split(",").toTypedArray())
        }
    },
    COMPONENT("component", R.string.component) {
        override val shellArgName: String
            get() = "ecn"

        override fun putExtra(intent: Intent, key: String, value: String) {
            intent.putExtra(key, ComponentName.unflattenFromString(value))
        }
    };

    abstract val shellArgName: String
    abstract fun putExtra(intent: Intent, key: String, value: String)
}
