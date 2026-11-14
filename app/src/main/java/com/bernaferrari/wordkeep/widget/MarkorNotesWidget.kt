package com.bernaferrari.wordkeep.widget

import android.content.Context
import android.content.Intent
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import com.bernaferrari.wordkeep.activity.MainActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.bernaferrari.wordkeep.R

/**
 * A beautiful, modern notes widget for Markor.
 * Features:
 * - Gradient background matching Material 3 dynamic colors
 * - Quick note creation FAB
 * - Recent notes list with previews
 * - Smooth animations and interactions
 */
class MarkorNotesWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val recent = widgetRecentNotes(context).take(3)
        val openNotebook = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val quickNote = Intent(context, MainActivity::class.java)
            .setAction(MainActivity.ACTION_OPEN_WIDGET)
            .putExtra(MainActivity.EXTRA_WIDGET_NOTE, "quick_note")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .background(ColorProvider(Color(0xFF764BA2)))
                        .padding(16.dp)
                        .fillMaxSize()
                ) {
                    // Header
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_launcher_foreground),
                            contentDescription = context.getString(R.string.app_name),
                            modifier = GlanceModifier
                                .size(32.dp)
                                .cornerRadius(8.dp)
                        )
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        Text(
                            text = context.getString(R.string.app_name),
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = ColorProvider(Color.White)
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(16.dp))

                    // Quick Note FAB
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .clickable(actionStartActivity(quickNote))
                            .background(ColorProvider(Color.White.copy(alpha = 0.2f)))
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = GlanceModifier.fillMaxWidth()
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.ic_add),
                                contentDescription = context.getString(R.string.create_new_document),
                                modifier = GlanceModifier.size(24.dp)
                            )
                            Spacer(modifier = GlanceModifier.width(12.dp))
                            Text(
                                text = context.getString(R.string.widget_preview_tap_to_write),
                                style = TextStyle(
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 14.sp,
                                    color = ColorProvider(Color.White.copy(alpha = 0.8f))
                                )
                            )
                        }
                    }

                    Spacer(modifier = GlanceModifier.height(16.dp))

                    // Recent notes section
                    Text(
                        text = context.getString(R.string.widget_preview_recent_notes),
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ColorProvider(Color.White.copy(alpha = 0.9f))
                        )
                    )

                    Spacer(modifier = GlanceModifier.height(8.dp))

                    if (recent.isEmpty()) {
                        Text(context.getString(R.string.widget_no_recent_notes), style = TextStyle(color = ColorProvider(Color.White)))
                    }
                    recent.forEach { path ->
                        RecentNoteItem(path.substringAfterLast('/').substringBeforeLast('.'), widgetRecentNoteIntent(context, path))
                        Spacer(modifier = GlanceModifier.height(4.dp))
                    }

                    Spacer(modifier = GlanceModifier.defaultWeight())

                    // Open app button
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .clickable(actionStartActivity(openNotebook))
                            .background(ColorProvider(Color.White.copy(alpha = 0.25f)))
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = context.getString(R.string.open_with) + " " + context.getString(R.string.app_name),
                            style = TextStyle(
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = ColorProvider(Color.White)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentNoteItem(
    title: String,
    intent: Intent
) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .clickable(actionStartActivity(intent))
            .background(ColorProvider(Color.White.copy(alpha = 0.15f)))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Note icon
        Box(
            modifier = GlanceModifier
                .size(36.dp)
                .background(ColorProvider(Color.White.copy(alpha = 0.3f))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title.take(1).uppercase(),
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = ColorProvider(Color.White)
                )
            )
        }

        Spacer(modifier = GlanceModifier.width(12.dp))

        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = title,
                style = TextStyle(
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = ColorProvider(Color.White)
                ),
                maxLines = 1
            )
        }

    }
}

/**
 * Widget receiver for Markor Notes Widget
 */
class MarkorNotesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MarkorNotesWidget()
}
