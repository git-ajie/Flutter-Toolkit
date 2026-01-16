package cn.neday.excavator.action

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.project.Project
import java.awt.Component
import java.awt.event.MouseEvent
import javax.swing.Icon

private const val PLUGIN_ACTION_ID_PREFIX = "cn.neday.excavator.action."
private const val SHOW_ALL_ACTION_ID = "cn.neday.excavator.action.ShowAllActionsAction"

private data class ActionInfo(
    val id: String,
    val text: String,
    val description: String,
    val icon: Icon?
) {
    val displayText: String
        get() = text.ifBlank { id }
}

class ShowAllActionsAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: run {
            notify(null, "未找到当前Project，无法显示Action列表。", NotificationType.ERROR)
            return
        }
        val items = collectItems()
        if (items.isEmpty()) {
            notify(project, "未找到本插件已注册的Action。", NotificationType.INFORMATION)
            return
        }
        val popup = JBPopupFactory.getInstance().createListPopup(ActionInfoPopupStep(items))
        showPopupAnchored(event, popup)
    }

    private fun showPopupAnchored(event: AnActionEvent, popup: com.intellij.openapi.ui.popup.JBPopup) {
        val anchorComponent: Component? =
            (event.inputEvent as? MouseEvent)?.component
                ?: event.getData(PlatformDataKeys.CONTEXT_COMPONENT)

        if (anchorComponent != null) {
            popup.showUnderneathOf(anchorComponent)
        } else {
            popup.showInBestPositionFor(event.dataContext)
        }
    }

    private fun notify(project: Project?, message: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Flutter ToolKit Notification")
            .createNotification(message, type)
            .notify(project)
    }

    private fun collectItems(): List<ActionInfo> {
        val actionManager = ActionManager.getInstance()
        val ids = actionManager.getActionIds(PLUGIN_ACTION_ID_PREFIX)
        return ids.mapNotNull { id ->
            if (id == SHOW_ALL_ACTION_ID) return@mapNotNull null
            val action = actionManager.getAction(id) ?: return@mapNotNull null
            action.toInfo(id)
        }.sortedWith(compareBy<ActionInfo> { it.displayText.lowercase() }.thenBy { it.id })
    }

    private fun AnAction.toInfo(id: String): ActionInfo {
        val p = templatePresentation
        return ActionInfo(
            id = id,
            text = p.text.orEmpty(),
            description = p.description.orEmpty(),
            icon = p.icon
        )
    }
}

private class ActionInfoPopupStep(
    items: List<ActionInfo>
) : BaseListPopupStep<ActionInfo>(null, items) {

    override fun getTextFor(value: ActionInfo): String = value.displayText

    override fun getIconFor(value: ActionInfo): Icon? = value.icon

    override fun isSpeedSearchEnabled(): Boolean = true

    override fun onChosen(selectedValue: ActionInfo?, finalChoice: Boolean): PopupStep<*>? {
        return FINAL_CHOICE
    }
}
