package com.voya.ui.screens

import android.media.MediaPlayer
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voya.ui.theme.VoyaColors
import com.example.voya.R
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

private data class IntroPage(
    val label: String,
    val heading: String,
    val paragraphs: List<String>
)

private val introPages = listOf(
    IntroPage(
        label = "THE WORLD",
        heading = "A world built on control",
        paragraphs = listOf(
            "In the near future, security becomes a pretext " +
                    "for exerting ever greater influence over people's lives. " +
                    "Technology promises protection, but demands control in return.",
            "At the centre is Alex Reyner, whose work was once meant " +
                    "to offer hope. When it becomes an instrument of surveillance, " +
                    "he is forced to confront his own creation."
        )
    ),
    IntroPage(
        label = "MAYA'S STORY",
        heading = "Everything changes",
        paragraphs = listOf(
            "One morning, everything changes. Alex is accused of murder, " +
                    "and his disappearance turns his family into the center " +
                    "of a nationwide manhunt.",
            "His nineteen-year-old daughter, Maya, wakes to alarming news " +
                    "and a desperate call from her father. Before he can explain " +
                    "what really happened, armed officers surround their home.",
            "As the police storm the house, a mysterious connection " +
                    "changes everything. Suddenly, the line between Maya's " +
                    "thoughts and someone else's control begins to blur.",
            "Now, with Special Agent Mercer demanding answers, " +
                    "every word could determine what happens next."
        )
    )
)

@Composable
fun StoryIntroScreen(
    onStartDialogue: () -> Unit,
    onBack: () -> Unit = {}
) {
    var pageIndex by rememberSaveable {
        mutableIntStateOf(0)
    }

    val page = introPages[pageIndex]
    val scrollState = key(pageIndex) { rememberScrollState() }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val audioResId = if (pageIndex == 0) R.raw.intro1 else R.raw.intro2
    var playbackPosition by rememberSaveable(pageIndex) { mutableIntStateOf(0) }
    var playbackFinished by rememberSaveable(pageIndex) { mutableStateOf(false) }
    var playbackFailed by remember(pageIndex) { mutableStateOf(false) }
    var retryCount by remember(pageIndex) { mutableIntStateOf(0) }

    BackHandler {
        if (pageIndex > 0) pageIndex-- else onBack()
    }

    DisposableEffect(context, audioResId, lifecycleOwner, retryCount) {
        playbackFailed = false
        val player = try { MediaPlayer.create(context, audioResId) } catch (_: Exception) { null }
        if (player == null) playbackFailed = true

        fun pause() {
            if (!playbackFailed && !playbackFinished && player != null) {
                if (player.isPlaying) player.pause()
                playbackPosition = player.currentPosition
            }
        }

        fun play() {
            if (!playbackFailed && !playbackFinished) player?.start()
        }

        player?.setOnCompletionListener {
            playbackFinished = true
            playbackPosition = 0
        }
        player?.setOnErrorListener { _, _, _ ->
            playbackFailed = true
            true
        }
        if (player != null && playbackPosition > 0) player.seekTo(playbackPosition)
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> play()
                Lifecycle.Event.ON_STOP -> pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) play()

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            pause()
            player?.release()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        VoyaColors.MidnightBlue,
                        VoyaColors.Background,
                        VoyaColors.Background
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "VOYA",
                color = VoyaColors.Gold,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp
            )

            TextButton(onClick = onStartDialogue) {
                Text(
                    text = "Skip »",
                    color = VoyaColors.TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(introPages.size) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .background(
                            if (index <= pageIndex)
                                VoyaColors.Gold
                            else
                                VoyaColors.MidnightBlue,
                            RoundedCornerShape(50)
                        )
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .drawWithContent {
                    drawContent()
                    val overflow = scrollState.maxValue
                    if (overflow > 0 && overflow != Int.MAX_VALUE && size.height > 0f) {
                        val inset = 8.dp.toPx()
                        val trackHeight = (size.height - inset * 2).coerceAtLeast(1f)
                        val width = 4.dp.toPx()
                        val thumbHeight = (trackHeight * size.height / (size.height + overflow))
                            .coerceIn(36.dp.toPx().coerceAtMost(trackHeight), trackHeight)
                        val thumbTop = inset + (trackHeight - thumbHeight) *
                            scrollState.value.toFloat() / overflow
                        val radius = CornerRadius(width / 2, width / 2)
                        drawRoundRect(VoyaColors.TextSecondary.copy(alpha = 0.18f),
                            topLeft = Offset(size.width - width, inset),
                            size = Size(width, trackHeight), cornerRadius = radius)
                        drawRoundRect(VoyaColors.Gold,
                            topLeft = Offset(size.width - width, thumbTop),
                            size = Size(width, thumbHeight), cornerRadius = radius)
                    }
                }
                .verticalScroll(scrollState)
                .padding(end = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = page.label,
                color = VoyaColors.Gold,
                fontSize = 11.sp,
                letterSpacing = 3.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = page.heading,
                color = VoyaColors.TextPrimary,
                fontSize = 27.sp,
                lineHeight = 33.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            page.paragraphs.forEach { paragraph ->
                Text(
                    text = paragraph,
                    color = VoyaColors.TextSecondary,
                    fontSize = 16.sp,
                    lineHeight = 25.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (playbackFailed) {
            Text("Could not play the narration. You can retry or continue reading.",
                color = VoyaColors.TextSecondary, textAlign = TextAlign.Center)
            TextButton(onClick = {
                playbackPosition = 0
                playbackFinished = false
                retryCount++
            }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Retry narration")
            }
        }

        Button(
            onClick = {
                if (pageIndex < introPages.lastIndex) {
                    pageIndex++
                } else {
                    onStartDialogue()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = VoyaColors.Gold,
                contentColor = VoyaColors.Background
            )
        ) {
            Text(
                text = if (pageIndex == 0)
                    "Continue"
                else
                    "Start Dialogue",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (pageIndex > 0) {
            TextButton(
                onClick = { pageIndex-- },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "Previous",
                    color = VoyaColors.TextSecondary
                )
            }
        }
    }
}
