package org.kotlingl.devtools

import imgui.ImGui
import imgui.ImVec2
import imgui.flag.ImGuiTreeNodeFlags
import imgui.type.ImBoolean
import org.joml.Quaternionf
import org.joml.Vector3f
import org.kotlingl.math.toFloatArray
import org.kotlingl.model.Model
import org.kotlingl.model.SkeletonNode


class ModelWindow(val model: Model): Window() {
    val posArray = model.position.toFloatArray()
    val scaleArray = model.scale.toFloatArray()
    val rotationArray = model.rotation.getEulerAnglesXYZ(Vector3f())
        .mul(Math.toDegrees(1.0).toFloat())
        .toFloatArray()

    override fun update() {
        if (!this.open.get())
            return

        if (ImGui.beginPopupContextItem()) {
            if (ImGui.menuItem("Close")) {
                this.close()
            }
        }

        // ImGui.setNextWindowSize()

        if (ImGui.begin(model.name, this.open)) {
            val animationSpeedArray = floatArrayOf(model.skeletonAnimator.animationSpeed)

            ImGui.separatorText("Transforms")
            ImGui.dragFloat3("Position", posArray)
            ImGui.dragFloat3("Rotation", rotationArray)
            ImGui.dragFloat3("Scale", scaleArray)

            if (ImGui.collapsingHeader("Animations")) {
                if (ImGui.beginTabBar("AnimationTabs")) {
                    if (ImGui.beginTabItem("Model")){
                        ImGui.dragFloat("AnimationSpeed", animationSpeedArray, 0.1f, -5.0f, 5.0f)
                        ImGui.beginChild("Animations", ImVec2(ImGui.getContentRegionAvailX(), 260f))
                        model.skeleton.animations.forEach {
                            ImGui.text(it.value.name)
                            val isPlaying = model.skeletonAnimator.currentAnimation == it.value
                            val playSize = ImGui.calcTextSizeX("Play") + ImGui.getFrameHeight()
                            val loopSize = ImGui.calcTextSizeX("Loop") + ImGui.getFrameHeight()
                            ImGui.sameLine(ImGui.getContentRegionMaxX() - playSize * 2)
                            if (ImGui.button(if (isPlaying) "Stop" else "Play" + "###play_${it.key}")) {
                                model.skeletonAnimator.currentAnimation = if (isPlaying) null else it.value
                            }
                            ImGui.sameLine(ImGui.getContentRegionMaxX() - loopSize)
                            if (ImGui.checkbox("Loop###loop_${it.key}", it.value.isLoop)) {
                                it.value.isLoop = !it.value.isLoop
                            }
                        }
                        ImGui.endChild()
                        ImGui.endTabItem()
                    }
                    if (ImGui.beginTabItem("Texture")) {
                        ImGui.dragFloat("AnimationSpeed", animationSpeedArray, 0.1f, -5.0f, 5.0f)
                        ImGui.beginChild("Animations", ImVec2(ImGui.getContentRegionAvailX(), 260f))
                        model.textureAnimator.animations.forEach {
                            ImGui.text(it.value.name)
                            val isPlaying = model.textureAnimator.activeAnimations.containsKey(it.key)
                            val playSize = ImGui.calcTextSizeX("Play") + ImGui.getFrameHeight()
                            val loopSize = ImGui.calcTextSizeX("Loop") + ImGui.getFrameHeight()
                            ImGui.sameLine(ImGui.getContentRegionMaxX() - playSize * 2)
                            if (ImGui.button(if (isPlaying) "Stop" else "Play" + "###play_${it.key}")) {
                                model.textureAnimator.playAnimation(it.key)
                            }
                            // ImGui.sameLine(ImGui.getContentRegionMaxX() - loopSize)
                            // if (ImGui.checkbox("Loop###loop_${it.key}", it.value.isLoop)) {
                            //     it.value.isLoop = !it.value.isLoop
                            // }
                        }
                        ImGui.endChild()
                        ImGui.endTabItem()
                    }
                    ImGui.endTabBar()
                }
            }

            if (ImGui.collapsingHeader("Skeleton")) {
                ImGui.beginChild("Nodes", ImVec2(ImGui.getContentRegionAvailX(), 260f))
                createSkeletonNode(model, model.skeleton.nodeMap.getValue(model.skeleton.rootId))
                ImGui.endChild()
            }


            // sync values
            model.transform(
                Vector3f(posArray),
                Quaternionf().identity().rotateXYZ(
                    Math.toRadians(rotationArray[0].toDouble()).toFloat(),
                    Math.toRadians(rotationArray[1].toDouble()).toFloat(),
                    Math.toRadians(rotationArray[2].toDouble()).toFloat()
                ),
                Vector3f(scaleArray)
            )
            model.skeletonAnimator.animationSpeed = animationSpeedArray[0]
        }
        ImGui.end()
    }

    fun createSkeletonNode(model: Model, currentNode: SkeletonNode) {
        if (ImGui.treeNodeEx(currentNode.name, ImGuiTreeNodeFlags.DefaultOpen)) {
            val meshIndices = model.nodeIdToMeshIndices[currentNode.id]
            meshIndices?.forEach {
                val mesh = model.meshes[it]
                val material = model.getMeshMaterial(it)
                if (ImGui.treeNodeEx("Mesh: ${mesh.name}")) {
                    ImGui.text("Material: ${material.name}")
                    ImGui.treePop()
                }
            }
            currentNode.childIds.forEach {
                createSkeletonNode(model, model.skeleton.nodeMap.getValue(it))
            }
            ImGui.treePop()
        }
    }
}