package de.fatox.meta.injection

class Key<T> private constructor(val type: Class<T>, var qualifier: Class<out Annotation>?, var name: String?) {
    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val key = o as Key<*>
        if (type != key.type) return false
        return if (if (qualifier != null) qualifier != key.qualifier else key.qualifier != null) false else !if (name != null) name != key.name else key.name != null
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + if (qualifier != null) qualifier.hashCode() else 0
        result = 31 * result + if (name != null) name.hashCode() else 0
        return result
    }

    override fun toString(): String {
        val suffix = if (name != null) "@\"$name\"" else if (qualifier != null) "@" + qualifier!!.simpleName else ""
        return type.name + suffix
    }

    companion object {
        /**
         * @return Key for a given type
         */
        fun <T> of(type: Class<T>): Key<T> {
            return Key(type, null, null)
        }

        /**
         * @return Key for a given type and qualifier annotation type
         */
        fun <T> of(type: Class<T>, qualifier: Class<out Annotation>?): Key<T> {
            return Key(type, qualifier, null)
        }

        /**
         * @return Key for a given type and name (@Named value)
         */
        fun <T> of(type: Class<T>, name: String?): Key<T> {
            return Key(type, Named::class.java, name)
        }

        fun <T> of(type: Class<T>, qualifier: Annotation?): Key<T> {
            return if (qualifier == null) {
                of(type)
            } else {
                if (qualifier.annotationType() == Named::class.java) of(
                    type,
                    (qualifier as Named).value()
                ) else of(type, qualifier.annotationType())
            }
        }
    }
}