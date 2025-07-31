package org.kotlingl.devtools

import imgui.ImGui.*
import imgui.ImGuiIO
import imgui.ImVec2
import imgui.flag.ImGuiCond
import imgui.flag.ImGuiSelectableFlags
import imgui.flag.ImGuiTableColumnFlags
import imgui.flag.ImGuiTableFlags
import imgui.flag.ImGuiTableRowFlags
import imgui.flag.ImGuiTreeNodeFlags
import imgui.flag.ImGuiWindowFlags
import imgui.type.ImString
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolute
import kotlin.io.path.extension
import kotlin.io.path.fileSize
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension


data class FileNode(
    val path: Path,
    val index: Int,
    val children: List<FileNode> = mutableListOf(),
)

class FileBrowser(
    val defaultPath: Path
): Window(ImVec2(640f, 320f)) {
    val tableFlags = ImGuiTableFlags.BordersV or
            ImGuiTableFlags.BordersOuterH or
            ImGuiTableFlags.Resizable or
            ImGuiTableFlags.RowBg or
            ImGuiTableFlags.NoBordersInBody or
            ImGuiTableFlags.ScrollY

    var selection = mutableSetOf<Path>()
    var fileNodes = mutableListOf<FileNode>()
    val pathInputValue = ImString(defaultPath.toString())
    val selectedPath = ImString("")
    var nodeCount = 0
    lateinit var rootNode: FileNode
    lateinit var glob: String
    lateinit var title: String
    lateinit var currentPath: Path
    lateinit var onSubmit: (it: Path) -> Unit
    //var onCancel: (() -> Unit)? = null

    fun open(
        title: String,
        glob: String="*",
        path: Path=defaultPath,
        onSubmit: (it: Path) -> Unit
    ) {
        this.title=title
        currentPath=path
        this.glob=glob
        this.onSubmit=onSubmit
        selectedPath.clear()
        pathInputValue.set(path.absolute().toString(), true)
        fileNodes.clear()
        nodeCount = 0
        rootNode = loadContent()
        super.open()
    }

    fun loadContent(path: Path = currentPath): FileNode {
        val fileNode = FileNode(
            path,
            nodeCount++,
            path.let { childPath ->
                if (childPath.isDirectory()) {
                    val files = childPath.listDirectoryEntries(glob).filter{!it.isDirectory()}.map {
                        loadContent(it)
                    }
                    return@let childPath.listDirectoryEntries().filter{it.isDirectory()}.map {
                        loadContent(it)
                    } + files
                }
                mutableListOf()
            }
        )
        fileNodes.add(fileNode)
        return fileNode
    }

    fun upLevel() {
        currentPath = currentPath.parent
        rootNode = loadContent(currentPath)
        pathInputValue.set(currentPath)
    }

    fun displayFile(fileNode: FileNode = rootNode) {
        tableNextRow()
        tableNextColumn()

        if (fileNode.path.isDirectory()) {
            val open = treeNodeEx(fileNode.path.name)
            tableNextColumn()
            textDisabled("--")
            tableNextColumn()
            textDisabled("--")
            if (open) {
                fileNode.children.forEach {
                    displayFile(it)
                }
                treePop()
            }
        } else {
            val selected = selection.contains(fileNode.path)
            var flags = ImGuiTreeNodeFlags.Leaf or
                    ImGuiTreeNodeFlags.NoTreePushOnOpen or
                    ImGuiTreeNodeFlags.Bullet or
                    ImGuiTreeNodeFlags.SpanFullWidth

            if (selected) {
                flags = flags or ImGuiTreeNodeFlags.Selected
            }

            treeNodeEx(
                fileNode.path.name,
                        flags
            )
            if (isItemClicked()) {
                if (selection.contains(fileNode.path)) {
                    selection.remove(fileNode.path)
                } else {
                    val io = getIO()
                    if (io.keyCtrl)
                        selection.add(fileNode.path)
                    else  {
                        selection.clear()
                        selection.add(fileNode.path)
                    }
                    selectedPath.set(fileNode.path.toString(), true)
                }
            }
            tableNextColumn()
            text("${fileNode.path.fileSize()}")
            tableNextColumn()
            textUnformatted(fileNode.path.extension)
        }
    }

    fun displayFiles() {
        val size = ImVec2(
            getContentRegionAvailX(),
            getContentRegionAvailY() - getFrameHeightWithSpacing(),
        )

        if (beginTable("files", 3, tableFlags, size)) {
            tableSetupColumn("Name", ImGuiTableColumnFlags.NoHide)
            tableSetupColumn("Size", ImGuiTableColumnFlags.WidthFixed, TEXT_WIDTH*12)
            tableSetupColumn("Type", ImGuiTableColumnFlags.WidthFixed, TEXT_WIDTH*18)

            tableHeadersRow()
            displayFile()
            endTable()
        }
    }

    override fun update() {
        if (!open.get())
            return

        Utils.centerNextWindow(windowSize)
        if (begin(title, this.open)) {
            inputText("###current path", pathInputValue)
            sameLine()
            if (button("up")) { upLevel() }

            displayFiles()

            inputText("###selected path", selectedPath)
            sameLine()
            if (button("cancel")) {
                //onCancel?.invoke()
                close()
            }
            sameLine()
            if (button("open")) {
                onSubmit(Paths.get(selectedPath.get()))
                close()
            }

            end()
        }
    }
}