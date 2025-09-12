package ca.kebs.courrier.helpers

fun splitFrom(from: String): Pair<String, String> {
    return from.split("<")[0] to from.split("<")[1].removeSuffix(">")
}