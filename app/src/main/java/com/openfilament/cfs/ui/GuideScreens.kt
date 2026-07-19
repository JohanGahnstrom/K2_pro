package com.openfilament.cfs.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class GuideTopic(val title: String, val subtitle: String, val icon: ImageVector) {
    SCAN_WRITE("Scan & write a tag", "Step by step through the Tag screen", Icons.Default.Nfc),
    APPLY_TAG("Apply a tag to a spool", "Which physical tags to buy, and where to stick them", Icons.Default.Inventory2),
    REUSE_TRANSFER("Reuse or move a tag", "Rewriting in place vs. peeling one off", Icons.Default.Autorenew),
}

data class GuideStep(val title: String, val body: String)

/**
 * Content sourced from real community repos/forums researched for this
 * project (see docs/SOURCE_REGISTER.md "Physical tag application and
 * reuse"), not invented — including one finding that changes the naive
 * assumption: peeling a stuck-down tag and reapplying it elsewhere is
 * NOT confirmed to work by any source checked, while rewriting a tag in
 * place on the same spool IS community-confirmed to work repeatedly.
 */
private val scanWriteSteps = listOf(
    GuideStep(
        "Check the compatibility banner first",
        "Open Home. If it says your phone can't do MIFARE Classic 1K, stop here — writing won't work regardless of the steps below, and retrying won't change that (see the banner's explanation)."
    ),
    GuideStep(
        "Pick filament, colour and spool size",
        "On the Tag screen, choose the product, its colour, and the spool weight. In Simple Mode, anything marked Experimental or Unsupported is blocked from writing on purpose."
    ),
    GuideStep(
        "Tap \"Tap a tag to write\"",
        "The app now waits for a tag — nothing happens until you actually present one."
    ),
    GuideStep(
        "Hold the tag flat against the back of your phone",
        "Most phones' NFC antenna sits behind the camera bump, roughly the top third of the back panel — but this varies by model. If nothing happens after a couple of seconds, slowly slide the tag around that area rather than pressing harder in one spot. Remove a thick case if you're not getting a read."
    ),
    GuideStep(
        "Keep it still until you see a result",
        "The app authenticates the tag, writes three data blocks, then immediately reads them back to confirm the write took — moving the tag mid-write is the most common cause of a failed write, not a defective tag."
    ),
    GuideStep(
        "Check the outcome banner",
        "A green \"Write verified\" message means the read-back matched exactly what you asked for. Anything else names the specific problem — device unsupported, or a write/verify failure — rather than a generic error."
    ),
)

private val applyTagSteps = listOf(
    GuideStep(
        "Buy the right physical tag",
        "You need genuine MIFARE Classic 1K adhesive stickers — commonly sold as 25 mm round NFC labels. NTAG213/215/216 and MIFARE Ultralight stickers are NOT compatible with the CFS or this app, even though they look identical and are far more common in generic \"NFC sticker\" listings — confirm the listing explicitly says \"MIFARE Classic 1K\" before buying."
    ),
    GuideStep(
        "Plan for two tags per spool",
        "Community reports describe the CFS reading noticeably more reliably with one tag on each face of the spool, since each lane has its own reader position. Write the spool once with this app, then physically stick a tag on each side — the data on both is identical, only the physical copy differs."
    ),
    GuideStep(
        "Align it the way the CFS expects",
        "Community-documented placement: centre the tag's semi-circular cutout (if it has one) on the spool's centre, positioned between the spool's two large structural holes. Third-party printable tag holders mirror this alignment, and mention orienting the tag's thin/wide tab differently depending on which side of the CFS lane it faces, since the two lanes are mirror images of each other."
    ),
    GuideStep(
        "Consider a tag holder for spools you'll discard",
        "For cardboard or third-party spools you don't plan to keep, a free 3D-printed tag holder (community designs exist for Sunlu, Extrudr, and generic cardboard spools) lets you slot a tag in rather than gluing it to something you'll throw away — see \"Reuse or move a tag\" for why that matters."
    ),
)

private val reuseSteps = listOf(
    GuideStep(
        "Keeping the same spool? Don't peel anything.",
        "If you're refilling or reusing the same physical spool, rewrite the existing tag in place with this app instead of touching the sticker at all. This is community-confirmed to work: forum testing shows the same physical tag can have its material, colour and weight rewritten repeatedly with no degradation."
    ),
    GuideStep(
        "Retiring a spool? Peeling is a real risk, not a safe workaround.",
        "No source checked for this app — not the community repos, not the forums, not general RFID-industry documentation — confirms that peeling a stuck-down MIFARE sticker off one spool and re-adhering it to another reliably preserves the tag. Adhesive RFID/NFC stickers commonly use a fine, flexible antenna coil that can fracture when peeled; general RFID sources describe exactly this failure mode. Treat a peeled tag as likely dead until proven otherwise, not as a safe default move."
    ),
    GuideStep(
        "Prefer a fresh tag or a holder instead",
        "MIFARE Classic 1K stickers are inexpensive and sold in packs — buying a new one for the new spool avoids the peeling risk entirely. For spools you cycle often, a 3D-printed tag holder (see \"Apply a tag to a spool\") means the tag was never stuck to the disposable part in the first place, so there's nothing to peel when you retire that spool."
    ),
    GuideStep(
        "If you do peel one anyway",
        "Work slowly and pull the backing away from a flat corner rather than lifting the tag by its face, then scan it with this app's Tag screen immediately after moving it. A tag that still authenticates and reads back correctly is fine to keep using; one that returns a read/authentication error has likely had its antenna damaged and should be replaced, not retried repeatedly."
    ),
)

@Composable
fun GuideFlow(onExit: () -> Unit, startAt: GuideTopic? = null) {
    var selected by rememberSaveable { mutableStateOf(startAt) }
    val topic = selected
    if (topic == null) {
        GuideHubScreen(onBack = onExit, onSelect = { selected = it })
    } else {
        GuideDetailScreen(topic, onBack = { selected = null })
    }
}

@Composable
private fun GuideHubScreen(onBack: () -> Unit, onSelect: (GuideTopic) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { GuideBackRow("Guides", onBack) }
        item {
            Text(
                "Practical, hands-on steps for scanning, writing, and physically applying or reusing CFS RFID tags — separate from the protocol detail in Expert Mode.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(GuideTopic.entries) { t -> GuideTopicCard(t) { onSelect(t) } }
    }
}

@Composable
private fun GuideTopicCard(topic: GuideTopic, click: () -> Unit) = Card(onClick = click, shape = RoundedCornerShape(24.dp)) {
    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(48.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(topic.icon, null) }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(topic.title, fontWeight = FontWeight.Black)
            Text(topic.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, null)
    }
}

@Composable
private fun GuideDetailScreen(topic: GuideTopic, onBack: () -> Unit) {
    val steps = when (topic) {
        GuideTopic.SCAN_WRITE -> scanWriteSteps
        GuideTopic.APPLY_TAG -> applyTagSteps
        GuideTopic.REUSE_TRANSFER -> reuseSteps
    }
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { GuideBackRow(topic.title, onBack) }
        itemsIndexed(steps) { index, step -> GuideStepCard(index + 1, step) }
        item {
            Text(
                "Sources: community forum reports and third-party tag-holder listings, not Creality's own documentation — treated as community-verified, not manufacturer-confirmed. See docs/SOURCE_REGISTER.md \"Physical tag application and reuse.\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GuideStepCard(number: Int, step: GuideStep) = Card(shape = RoundedCornerShape(22.dp)) {
    Row(Modifier.padding(18.dp)) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp)) {
            Box(contentAlignment = Alignment.Center) { Text("$number", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Black, fontSize = 13.sp) }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(step.title, fontWeight = FontWeight.Bold)
            Text(step.body, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun GuideBackRow(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
        Spacer(Modifier.width(4.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
    }
}
