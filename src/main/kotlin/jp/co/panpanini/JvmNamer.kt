package jp.co.panpanini

import pbandk.gen.Namer

class JvmNamer : Namer {
    private val disallowedTypeNames = mutableSetOf(
            "Boolean", "Companion", "Double", "Float", "Int", "List", "Long", "Map", "String"
    )
    private val disallowedFieldNames = mutableSetOf(
            "emptyList", "pbandk", "plus", "protoMarshal", "protoSize", "protoUnmarshal", "unknownFields"
    )
    private val kotlinKeywords = mutableSetOf(
            "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in",
            "interface", "is", "null", "object", "package", "return", "super", "this", "throw",
            "true", "try", "typealias", "val", "var", "when", "while"
    )

    fun addDisallowedTypeName(type: String) = disallowedTypeNames.add(type)

    fun addDisallowedFieldName(name: String) = disallowedFieldNames.add(name)


    protected fun underscoreToCamelCase(str: String): String {
        var ret = str
        var lastIndex = -1
        while (true) {
            lastIndex = ret.indexOf('_', lastIndex+1).also { if (it == -1) return ret }
            ret = ret.substring(0, lastIndex) + ret.substring(lastIndex + 1).capitalize()
        }
    }

    override fun newTypeName(preferred: String, nameSet: MutableSet<String>): String {
        var name = underscoreToCamelCase(preferred).capitalize()
        while (nameSet.contains(name) || disallowedTypeNames.contains(name)) name += '_'
        return name.also { nameSet += name }
    }

    override fun newFieldName(preferred: String, nameSet: MutableSet<String>): String {
        var name = underscoreToCamelCase(preferred).decapitalize()
        while (nameSet.contains(name) || disallowedFieldNames.contains(name)) name += '_'
        if (kotlinKeywords.contains(name)) name = "`$name`"
        return name.also { nameSet += name }
    }

    override fun newEnumValueName(preferred: String, nameSet: MutableSet<String>): String {
        var name = preferred.toUpperCase()
        while (nameSet.contains(name)) name += '_'
        return name.also { nameSet += name }
    }

}