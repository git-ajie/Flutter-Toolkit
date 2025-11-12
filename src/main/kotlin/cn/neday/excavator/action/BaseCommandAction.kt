package cn.neday.excavator.action

import cn.neday.excavator.checker.ProjectChecker
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import org.jetbrains.plugins.terminal.ShellTerminalWidget
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory
import org.jetbrains.plugins.terminal.TerminalToolWindowManager
import java.io.IOException

abstract class BaseGenerationAnAction : BaseAnAction() {
    abstract val command: String
    abstract val title: String
    abstract val successMessage: String
    abstract val errorMessage: String

    override fun update(event: AnActionEvent) {
        val project = event.getData(PlatformDataKeys.PROJECT)
        val projectPath = project?.basePath
        event.presentation.isEnabledAndVisible = ProjectChecker().check(projectPath).isOk
    }

    override fun actionPerformed(event: AnActionEvent) {
        event.project?.let { project ->
            project.basePath?.let { projectPath ->
                // Use default command
                execCommand(project, projectPath, command)
            } ?: showErrorMessage("Current directory does not seem to be a project directory.")
        } ?: showErrorMessage("Current directory does not seem to be a project directory.")
    }

    fun execCommand(project: Project, projectPath: String, command: String) {
        // Check terminal exists
        val window = ToolWindowManager.getInstance(project).getToolWindow(TerminalToolWindowFactory.TOOL_WINDOW_ID)
        if (window == null) {
            showErrorMessage("Please check that the following plugins are installed: Terminal")
            return
        }

        // Close existing flutter toolkit
        val terminalName = "Flutter-Toolkit"
        val content = window.contentManager.findContent(terminalName)
        if (content != null) {
            // Close existing tab with same name using ContentManager API
            window.contentManager.removeContent(content, true)
        }

        // Start new terminal using TerminalToolWindowManager (preferred API)
        // Ensure tool window is activated so the component is showing for the reworked engine
        window.activate({
            try {
                val terminalWidget = TerminalToolWindowManager.getInstance(project)
                    .createShellWidget(projectPath, terminalName, false, false)
                terminalWidget.sendCommandToExecute(command)
            } catch (exception: IOException) {
                showErrorMessage("Cannot run command:" + command + "  " + exception.message)
            } catch (exception: NullPointerException) {
                // Avoid hard crash on non-JediTerm engines
                showErrorMessage("Terminal execution failed. Please disable 'New Terminal' in IDE settings or update the IDE.")
            } catch (exception: IllegalStateException) {
                // Handle the reworked terminal engine component visibility timing
                showErrorMessage("Terminal UI not ready. Please retry or switch to Classic engine.")
            }
        }, true)
    }
}
