package org.kotlingl

import org.kotlingl.shapes.Circle
import org.kotlingl.shapes.Vector3

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main() {
//    val name = "Kotlin"
//    //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
//    // to see how IntelliJ IDEA suggests fixing it.
//    println("Hello, " + name + "!")
//
//    for (i in 1..5) {
//        //TIP Press <shortcut actionId="Debug"/> to start debugging your code. We have set one <icon src="AllIcons.Debugger.Db_set_breakpoint"/> breakpoint
//        // for you, but you can always add more by pressing <shortcut actionId="ToggleLineBreakpoint"/>.
//        println("i = $i")
//    }

    val scene = Scene(
        shapes = mutableListOf(
            Circle(
                Vector3(0f, 0f, 10f),
                1f,
                Vector3(0f, 255f, 0f)
            )
        ),
        cameras = mutableListOf(
            Camera()
        )
    )

    Renderer(scene).run()
}