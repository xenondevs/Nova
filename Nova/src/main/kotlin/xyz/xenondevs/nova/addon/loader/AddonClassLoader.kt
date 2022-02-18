package xyz.xenondevs.nova.addon.loader

import java.io.File
import java.net.URLClassLoader

class AddonClassLoader(val file: File, parent: ClassLoader) : URLClassLoader(arrayOf(file.toURI().toURL()), parent)