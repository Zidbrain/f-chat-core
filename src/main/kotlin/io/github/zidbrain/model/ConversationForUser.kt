package io.github.zidbrain.model

data class ConversationForUser(
    val id: String,
    val symmetricKey: String,
    val participants: List<User>
)
