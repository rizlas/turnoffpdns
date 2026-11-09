package it.rizlas.turnoffpdns

import android.net.InetAddresses
import java.net.InetAddress

/** An IPv4/IPv6 subnet ("192.168.1.0/24") or a single IP ("192.168.1.1"). */
class Cidr private constructor(private val network: ByteArray, private val prefix: Int) {

    fun contains(addr: InetAddress): Boolean {
        val bytes = addr.address
        if (bytes.size != network.size) return false
        var bits = prefix
        for (i in bytes.indices) {
            if (bits <= 0) return true
            val mask = if (bits >= 8) 0xFF else (0xFF shl (8 - bits)) and 0xFF
            if ((bytes[i].toInt() and mask) != (network[i].toInt() and mask)) return false
            bits -= 8
        }
        return true
    }

    companion object {
        fun parse(text: String): Cidr? {
            val parts = text.trim().split("/")
            if (parts.size > 2 || parts[0].isEmpty()) return null
            if (!InetAddresses.isNumericAddress(parts[0])) return null
            val bytes = InetAddresses.parseNumericAddress(parts[0]).address
            val max = bytes.size * 8
            val prefix = if (parts.size == 2) parts[1].toIntOrNull() ?: return null else max
            if (prefix !in 0..max) return null
            return Cidr(bytes, prefix)
        }

        /** One entry per line (commas also accepted). Returns valid and invalid entries. */
        fun parseList(text: String): Pair<List<Cidr>, List<String>> {
            val entries = text.split('\n', ',').map { it.trim() }.filter { it.isNotEmpty() }
            val valid = mutableListOf<Cidr>()
            val invalid = mutableListOf<String>()
            for (e in entries) parse(e)?.let { valid += it } ?: run { invalid += e }
            return valid to invalid
        }
    }
}
