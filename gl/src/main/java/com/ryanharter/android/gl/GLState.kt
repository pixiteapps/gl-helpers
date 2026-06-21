package com.ryanharter.android.gl

import android.opengl.GLES20
import android.opengl.GLES20.*
import android.opengl.GLES30.glBindVertexArray

private data class GLBugs(
  // Some drivers require the GL_TEXTURE_EXTERNAL_OES target to be bound when
  // the texture image changes, even if it's already bound to that texture
  val externalTextureNeedsRebind: Boolean
) {
  constructor(renderer: String) : this(
    externalTextureNeedsRebind = renderer.contains("Mali-T")
  )
}

object GLState {

  var logger: Logger = Logger.VoidLogger()

  // TODO choose the best renderer based on env
  private val renderer = GLES2Renderer()

  private var glVersion = GLVersion.GL_UNKNOWN
  private var glExtensions = ""
  private var maxTextureSize = -1

  private var _bugs: GLBugs? = null
  private val bugs: GLBugs
    get() {
      if (_bugs == null) {
        // Only set the internal property if we get a valid renderer (sometimes this returns null)
        val renderer = glGetString(GL_RENDERER) ?: return GLBugs(true)
        _bugs = GLBugs(renderer)
      }
      return _bugs!!
    }

  enum class GLVersion {
    GLES_20, GLES_30, GL_UNKNOWN
  }

  fun getGlVersion(): GLVersion {
    if (glVersion == GLVersion.GL_UNKNOWN) {
      val version = GLES20.glGetString(GL_VERSION)
      return if (version != null && version.startsWith("OpenGL ES 2.")) {
        GLVersion.GLES_20
      } else if (version != null && version.startsWith("OpenGL ES 3.")) {
        GLVersion.GLES_30
      } else {
        GLVersion.GL_UNKNOWN
      }
    }
    return glVersion
  }

  fun hasExtension(name: String): Boolean {
    if (glExtensions.isEmpty()) {
      glExtensions = glGetString(GL_EXTENSIONS)
    }
    return glExtensions.contains(name)
  }

  fun getMaxTextureSize(): Int {
    if (maxTextureSize < 0) {
      val tempInt = IntArray(16)
      glGetIntegerv(GL_MAX_TEXTURE_SIZE, tempInt, 0)
      maxTextureSize = tempInt[0]
    }
    return maxTextureSize
  }

  fun getViewport(viewport: IntArray) {
    glGetIntegerv(GL_VIEWPORT, viewport, 0)
  }

  fun getViewport(): IntArray {
    val viewport = IntArray(4)
      glGetIntegerv(GL_VIEWPORT, viewport, 0)
    return viewport
  }

  fun setViewport(x: Int, y: Int, w: Int, h: Int) {
    glViewport(x, y, w, h)
  }

  fun reset() {
    logger.log("Resetting state.")
    glVersion = GLVersion.GL_UNKNOWN
    _bugs = null
    maxTextureSize = -1
  }

  /**
   * Renders the current GL state to the active framebuffer.
   */
  fun render() {
    renderer.render()
  }

  fun useProgram(program: Int) {
    glUseProgram(program)
  }

  fun setTextureUnit(textureUnit: Int) {
    glActiveTexture(GL_TEXTURE0 + textureUnit)
  }

  fun bindTexture(unit: Int, target: Int, texture: Int) {
    setTextureUnit(unit)
    glBindTexture(target, texture)
  }

  fun bindFramebuffer(framebuffer: Int) {
    glBindFramebuffer(GL_FRAMEBUFFER, framebuffer)
  }

  fun setBlend(blend: Boolean, translucent: Boolean) {
    if (blend) {
      glEnable(GL_BLEND)
      if (translucent) {
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA)
      } else {
        glBlendFunc(GL_ONE, GL_ONE)
      }
    } else {
      glDisable(GL_BLEND)
    }
  }

  fun setAttributeEnabled(index: Int, enabled: Boolean) {
    if (enabled) {
      glEnableVertexAttribArray(index)
    } else {
      glDisableVertexAttribArray(index)
    }
  }

  fun bindArrayBuffer(buffer: Int): Boolean {
    glBindBuffer(GL_ARRAY_BUFFER, buffer)
    return true
  }

  fun bindElementArrayBuffer(buffer: Int): Boolean {
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, buffer)
    return true
  }

  fun bindVertexArray(array: Int): Boolean {
    glBindVertexArray(array)
    return true
  }
}
