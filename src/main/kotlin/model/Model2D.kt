package org.kotlingl.model

import org.kotlingl.shapes.Updatable

class Model2D(val name: String, model: Model? = null): Updatable {
    val model: Model

    init {
        this.model = model ?: PrimitiveFactory.createQuad(name)
    }

    override fun update(timeDelta: Float) {
    }
}