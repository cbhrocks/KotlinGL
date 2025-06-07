package org.kotlingl.utils

import java.nio.file.Path


fun Path.toUnixString(): String {
    return "/" + this.joinToString { "/" }
}