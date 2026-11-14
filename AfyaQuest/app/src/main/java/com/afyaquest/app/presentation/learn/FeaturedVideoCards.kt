package com.afyaquest.app.presentation.learn

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PregnantWoman
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afyaquest.app.R
import com.afyaquest.app.domain.model.VideoModule
import com.afyaquest.app.presentation.components.DoneBadge
import com.afyaquest.app.ui.theme.AfyaSuccess

/*
 * Shared building blocks for the featured videos and focus topics. They are used by the Learn hub
 * (carousel + topic cards), the topic screen and the Home hub (compact featured card), so the
 * spotlighted content looks the same wherever it appears.
 */

/**
 * Title-card still bundled for a video, or null when the video has none (the cards then fall back
 * to a brand-coloured placeholder). Only the featured videos carry one, to keep the APK small.
 */
@DrawableRes
fun videoThumbnailRes(videoId: String): Int? = when (videoId) {
    "mod1-female-reproductive" -> R.drawable.learn_thumb_female_reproductive
    "mod4-antenatal-care" -> R.drawable.learn_thumb_antenatal_care
    "mod2-malnutrition" -> R.drawable.learn_thumb_malnutrition
    else -> null
}

/** Icon that stands for a focus topic on its card and on the topic screen. */
fun topicIcon(topic: LearnTopic): ImageVector = when (topic) {
    LearnTopic.MATERNAL_HEALTH -> Icons.Filled.PregnantWoman
    LearnTopic.NUTRITION -> Icons.Filled.Restaurant
}

/**
 * 16:9 still of a video with a play button in its bottom-start corner, where it does not cover
 * the title that most of the stills carry. Purely decorative: the card names the video.
 */
@Composable
fun VideoThumbnail(
    video: VideoModule,
    modifier: Modifier = Modifier,
    playButtonSize: Dp = 48.dp,
    playButtonInset: Dp = 10.dp
) {
    val thumbnailRes = videoThumbnailRes(video.id)
    Box(modifier = modifier) {
        if (thumbnailRes != null) {
            Image(
                painter = painterResource(thumbnailRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    )
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(playButtonInset)
                .size(playButtonSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.92f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(playButtonSize * 0.6f)
            )
        }
    }
}

/** Small pill naming the topic a featured video belongs to ("Maternal Health"). */
@Composable
private fun TopicPill(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shadowElevation = 1.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Large featured-video card for the Learn hub carousel: still with play button, the topic it
 * belongs to, title, description and the module it comes from. Text heights are fixed so every
 * card in the row is the same height.
 */
@Composable
fun FeaturedVideoCard(
    video: VideoModule,
    topicTitle: String?,
    moduleLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            ) {
                VideoThumbnail(video = video, modifier = Modifier.fillMaxSize())
                if (topicTitle != null) {
                    TopicPill(
                        text = topicTitle,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                    )
                }
                if (video.watched) {
                    DoneBadge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp),
                        size = 26.dp,
                        contentDescription = stringResource(R.string.learn_watched)
                    )
                }
            }
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = video.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = moduleLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * One-row featured-video card for the Home hub's Learning Center. [eyebrow] reads
 * "Featured · Maternal Health".
 */
@Composable
fun FeaturedVideoCompactCard(
    video: VideoModule,
    eyebrow: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VideoThumbnail(
                video = video,
                modifier = Modifier
                    .width(116.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(10.dp)),
                playButtonSize = 26.dp,
                playButtonInset = 5.dp
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = eyebrow,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = video.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = video.description,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * A focus topic as one row: icon, title, description and how much of it (videos + lessons) is
 * done. Mirrors the module cards of the Videos tab, with an icon where modules show a number.
 */
@Composable
fun TopicCard(
    topic: LearnTopic,
    videosWatched: Int,
    videosTotal: Int,
    lessonsCompleted: Int,
    lessonsTotal: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val total = videosTotal + lessonsTotal
    val done = (videosWatched + lessonsCompleted).coerceAtMost(total)
    val isComplete = total > 0 && done >= total
    val progress = if (total > 0) done.toFloat() / total else 0f

    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = topicIcon(topic),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(topic.titleRes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (isComplete) {
                        DoneBadge()
                    }
                }
                Text(
                    text = stringResource(topic.descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (isComplete) AfyaSuccess else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = topicCountsText(videosWatched, videosTotal, lessonsCompleted, lessonsTotal),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** "2/7 videos • 0/1 lessons" (the lessons half is dropped for a topic without lessons). */
@Composable
private fun topicCountsText(
    videosWatched: Int,
    videosTotal: Int,
    lessonsCompleted: Int,
    lessonsTotal: Int
): String {
    val videos = stringResource(R.string.learn_videos_count_format, videosWatched, videosTotal)
    if (lessonsTotal <= 0) return videos
    return videos + " • " + stringResource(R.string.learn_lessons_count_format, lessonsCompleted, lessonsTotal)
}
