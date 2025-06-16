package model

import org.kotlingl.entity.Material
import org.kotlingl.model.Mesh

fun createEmptyMesh(): Mesh {
    return Mesh(listOf(), listOf(), Material())
}
