package org.kociumba.kutils.client.utils

/**
 * An extremely unnecessary data class turning version strings like 0.0.5-p1 into comparable types,
 * could have just reported a new version when a difference was detected, but I had to make it better xd
 */
data class KutilsVersion(
    var Major: Int,
    var Minor: Int,
    var Revision: Int,
    var Patch: Int?
) : Comparable<KutilsVersion> {
    override fun toString(): String {
        return if (Patch != null) {
            "%d.%d.%d-p%d".format(
                this.Major,
                this.Minor,
                this.Revision,
                this.Patch
            )
        } else {
            "%d.%d.%d".format(
                this.Major,
                this.Minor,
                this.Revision
            )
        }
    }

    override fun compareTo(other: KutilsVersion): Int {
        if (Major != other.Major) return Major.compareTo(other.Major)
        if (Minor != other.Minor) return Minor.compareTo(other.Minor)
        if (Revision != other.Revision) return Revision.compareTo(other.Revision)

        return when {
            Patch == null && other.Patch == null -> 0 // Both no patch, considered equal for patch part
            Patch == null && other.Patch != null -> -1 // This version has no patch, other has, so this is older
            Patch != null && other.Patch == null -> 1  // This version has patch, other has none, so this is newer
            else -> Patch!!.compareTo(other.Patch!!) // Both have patches, compare them numerically
        }
    }

    companion object {
        /**
         * Parses a version from a string like 0.0.5-p1, way too much error handling
         */
        fun fromString(v: String): KutilsVersion {
            val parts = v.split(".")
            if (parts.size != 3) {
                throw IllegalArgumentException("Invalid KutilsVersion string format: '$v'. Expected format 'Major.Minor.Revision' or 'Major.Minor.Revision-pPatch'.")
            }

            val major = parts[0].toIntOrNull()
                ?: throw IllegalArgumentException("Invalid Major version: '${parts[0]}'. Must be an integer.")
            val minor = parts[1].toIntOrNull()
                ?: throw IllegalArgumentException("Invalid Minor version: '${parts[1]}'. Must be an integer.")

            var revisionPart = parts[2]
            var patch: Int? = null

            if (revisionPart.contains("-p")) {
                val revisionSplit = revisionPart.split("-p")
                if (revisionSplit.size != 2) {
                    throw IllegalArgumentException("Invalid Revision-Patch format in: '$revisionPart'. Expected format 'Revision-pPatch'.")
                }
                val revision = revisionSplit[0].toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid Revision version: '${revisionSplit[0]}'. Must be an integer.")
                patch = revisionSplit[1].toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid Patch version: '${revisionSplit[1]}'. Must be an integer.")
                revisionPart = revisionSplit[0]
            }

            val revision = revisionPart.toIntOrNull()
                ?: throw IllegalArgumentException("Invalid Revision version: '$revisionPart'. Must be an integer.")

            return KutilsVersion(major, minor, revision, patch)
        }
    }
}
