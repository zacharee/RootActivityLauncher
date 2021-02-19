package tk.zwander.rootactivitylauncher.data.component

enum class ComponentType {
    ACTIVITY,
    SERVICE,
    RECEIVER;

    fun serialize(): String = name

    companion object {
        fun deserialize(name: String?) = if (name != null) valueOf(name) else null
    }
}