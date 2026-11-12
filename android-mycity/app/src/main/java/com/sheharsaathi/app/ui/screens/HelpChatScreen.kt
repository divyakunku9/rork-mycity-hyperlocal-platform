package com.sheharsaathi.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.sheharsaathi.app.data.CityViewModel
import com.sheharsaathi.app.data.Faq
import com.sheharsaathi.app.data.HelpBot
import com.sheharsaathi.app.data.HelpMessage
import com.sheharsaathi.app.data.HelpReply
import com.sheharsaathi.app.data.HelpTopic
import com.sheharsaathi.app.ui.theme.CityColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Help & FAQs: a chat with "Saathi" that answers questions about the app. Answers come from
 * the FAQ list on the phone (see HelpFaq.kt), so it works offline and nothing typed is sent anywhere.
 */
@Composable
fun HelpChatScreen(vm: CityViewModel, onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val messages = vm.helpChat
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var draft by rememberSaveable { mutableStateOf("") }
    var typing by remember { mutableStateOf(false) }
    val firstName = vm.profile.name.trim().substringBefore(' ')

    fun post(reply: HelpReply) {
        messages.add(HelpMessage(messages.size, false, reply.text, reply.faq?.link, reply.suggestions, reply.showTopics))
    }

    fun respond(userText: String, reply: () -> HelpReply) {
        if (typing) return
        messages.add(HelpMessage(messages.size, true, userText))
        typing = true
        scope.launch {
            delay(450)  // a short pause so the answer doesn't appear to jump in
            post(reply())
            typing = false
        }
    }

    fun send() {
        val question = draft.trim()
        if (question.isEmpty() || typing) return
        draft = ""
        respond(question) { HelpBot.reply(question) }
    }

    LaunchedEffect(Unit) {
        when {
            messages.isEmpty() -> post(HelpBot.welcome(firstName))
            // The screen was left while an answer was on its way: answer it now.
            messages.last().fromUser -> post(HelpBot.reply(messages.last().text))
        }
    }
    LaunchedEffect(messages.size, typing) {
        val lastIndex = messages.size - 1 + if (typing) 1 else 0
        if (lastIndex >= 0) listState.animateScrollToItem(lastIndex)
    }

    Column(
        Modifier.fillMaxSize().background(CityColors.Canvas).statusBarsPadding().imePadding().navigationBarsPadding(),
    ) {
        // Header
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            SaathiAvatar(40)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Help & FAQs", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                Text("Ask ${HelpBot.NAME} about the app • works offline", style = MaterialTheme.typography.bodySmall, color = CityColors.Muted, maxLines = 1)
            }
            if (messages.size > 1) {
                IconButton(onClick = { if (!typing) { messages.clear(); post(HelpBot.welcome(firstName)) } }) {
                    Icon(Icons.Filled.Refresh, "Start over", tint = CityColors.Muted)
                }
            }
        }

        // Conversation
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(messages, key = { it.id }) { m ->
                if (m.fromUser) UserBubble(m.text)
                else BotMessage(
                    message = m,
                    newest = m.id == messages.lastOrNull()?.id,
                    onAsk = { faq -> respond(faq.question) { HelpBot.answerFor(faq) } },
                    onTopic = { topic -> respond(topic.label) { HelpBot.topic(topic) } },
                    onNavigate = onNavigate,
                )
            }
            if (typing) item(key = "typing") {
                Row(verticalAlignment = Alignment.Bottom) {
                    SaathiAvatar(28)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${HelpBot.NAME} is typing…", style = MaterialTheme.typography.bodyMedium, color = CityColors.Muted,
                        modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(CityColors.Surface).padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }
        }

        // Question box
        Row(
            Modifier.fillMaxWidth().background(CityColors.Surface).padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it.take(200) },
                placeholder = { Text("Type your question…", color = CityColors.Muted) },
                maxLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send() }),
                shape = RoundedCornerShape(22.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = CityColors.SurfaceHigh, focusedContainerColor = CityColors.SurfaceHigh,
                    unfocusedBorderColor = Color.Transparent, focusedBorderColor = CityColors.Indigo, cursorColor = CityColors.Saffron,
                ),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(6.dp))
            val canSend = draft.isNotBlank() && !typing
            IconButton(
                onClick = { send() }, enabled = canSend,
                modifier = Modifier.size(48.dp).clip(CircleShape).background(if (canSend) CityColors.Saffron else CityColors.SurfaceHigh),
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, "Send question", tint = if (canSend) Color.White else CityColors.Muted)
            }
        }
    }
}

@Composable
private fun SaathiAvatar(size: Int) {
    Box(Modifier.size(size.dp).clip(CircleShape).background(CityColors.SaffronSoft), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.SupportAgent, null, tint = CityColors.SaffronText, modifier = Modifier.size((size * 0.6f).dp))
    }
}

@Composable
private fun UserBubble(text: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Text(
            text, style = MaterialTheme.typography.bodyLarge, color = Color.White,
            modifier = Modifier.widthIn(max = 300.dp)
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp))
                .background(CityColors.Indigo).padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun BotMessage(
    message: HelpMessage,
    newest: Boolean,
    onAsk: (Faq) -> Unit,
    onTopic: (HelpTopic) -> Unit,
    onNavigate: (String) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        SaathiAvatar(28)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                message.text, style = MaterialTheme.typography.bodyLarge, color = CityColors.Ink,
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                    .background(CityColors.Surface).padding(horizontal = 14.dp, vertical = 10.dp)
                    // Screen readers announce the newest answer when it arrives.
                    .then(if (newest) Modifier.semantics { liveRegion = LiveRegionMode.Polite } else Modifier),
            )

            message.link?.let { link ->
                Button(
                    onClick = { onNavigate(link.route) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CityColors.Saffron, contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Text(link.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, modifier = Modifier.size(18.dp))
                }
            }

            if (message.suggestions.isNotEmpty()) {
                val label = when {
                    message.showTopics -> "Popular questions"
                    message.link != null || message.text.length > 120 -> "Related questions"
                    else -> null  // the message itself already says to pick one
                }
                if (label != null) {
                    Text(label, style = MaterialTheme.typography.labelMedium, color = CityColors.Muted, modifier = Modifier.padding(top = 2.dp, start = 2.dp))
                }
                message.suggestions.forEach { faq -> QuestionChip(faq.question) { onAsk(faq) } }
            }

            if (message.showTopics) {
                Text("Browse by topic", style = MaterialTheme.typography.labelMedium, color = CityColors.Muted, modifier = Modifier.padding(top = 2.dp, start = 2.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(HelpTopic.values().toList(), key = { it.name }) { topic ->
                        Text(
                            topic.label, style = MaterialTheme.typography.labelLarge, color = CityColors.IndigoDeep,
                            modifier = Modifier.clip(CircleShape).background(CityColors.IndigoSoft)
                                .clickable(onClickLabel = "Show questions about ${topic.label}") { onTopic(topic) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionChip(question: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .border(1.dp, CityColors.Outline, RoundedCornerShape(14.dp))
            .clickable(onClickLabel = "Ask this question", onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(question, style = MaterialTheme.typography.bodyMedium, color = CityColors.IndigoDeep, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = CityColors.Muted, modifier = Modifier.size(18.dp))
    }
}
