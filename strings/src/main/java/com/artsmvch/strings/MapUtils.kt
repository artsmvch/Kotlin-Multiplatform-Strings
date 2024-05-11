package com.artsmvch.strings

internal fun <K, V> Map<K, V>.intersect(other: Map<K, V>): Map<K, V> {
    val intersection = HashMap<K, V>()
    forEach { (key, value) ->
        if (other.contains(key)) {
            intersection[key] = value
        }
    }
    return intersection
}

internal fun <K, V> Map<K, V>.outersect(other: Map<K, V>): Map<K, V> {
    val outersection = HashMap<K, V>()
    forEach { (key, value) ->
        if (!other.contains(key)) {
            outersection[key] = value
        }
    }
    other.forEach { (key, value) ->
        if (!contains(key)) {
            outersection[key] = value
        }
    }
    return outersection
}