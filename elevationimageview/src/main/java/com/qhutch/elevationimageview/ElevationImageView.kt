package com.qhutch.elevationimageview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.appcompat.widget.AppCompatImageView
import android.renderscript.*


/**
 * Created by quentin on 15/03/2018.
 */
open class ElevationImageView : AppCompatImageView {

    constructor(context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, @AttrRes defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.ElevationImageView)

        val elevation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            customElevation.toInt()
        } else {
            0
        }

        customElevation = a.getDimensionPixelSize(R.styleable.ElevationImageView_compatEvelation, elevation).toFloat()

        clipShadow = a.getBoolean(R.styleable.ElevationImageView_clipShadow, false)

        isTranslucent = a.getBoolean(R.styleable.ElevationImageView_isTranslucent, false)

        forceClip = a.getBoolean(R.styleable.ElevationImageView_forceClip, false)

        a.recycle()
    }

    private var clipShadow = false

    private var shadowBitmap: Bitmap? = null

    private var customElevation = 0f

    private var rect = Rect()

    private var forceClip = false
        set(value) {
            field = value
            invalidate()
        }

    var isTranslucent = false
        set(value) {
            field = value
            invalidate()
        }

    private lateinit var rs: RenderScript
    private lateinit var blurScript: ScriptIntrinsicBlur
    private lateinit var colorMatrixScript: ScriptIntrinsicColorMatrix

    override fun setElevation(elevation: Float) {
        customElevation = elevation
        invalidate()
    }

    fun setElevationDp(elevation: Float) {
        customElevation = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, elevation, resources.displayMetrics)
        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        if (!isInEditMode && canvas != null) {
            if (shadowBitmap == null && customElevation > 0) {
                generateShadow()
            }
            drawable?.let { drawable ->
                val bounds = drawable.copyBounds()
                shadowBitmap?.let {
                    canvas.save()

                    if (!clipShadow) {
                        canvas.getClipBounds(rect)
                        rect.inset(-2 * getBlurRadius().toInt(), -2 * getBlurRadius().toInt())
                        if (forceClip) {
                            canvas.clipRect(rect)
                        } else {
                            canvas.save()
                            canvas.clipRect(rect)
                        }
                        canvas.drawBitmap(it, bounds.left.toFloat() - getBlurRadius(), bounds.top - getBlurRadius() / 2f, null)
                    }

                    canvas.restore()
                }
            }
        }
        super.onDraw(canvas)
    }

    override fun invalidate() {
        shadowBitmap = null
        super.invalidate()
    }

    override fun onDetachedFromWindow() {
        if (!isInEditMode) {
            blurScript.destroy()
            colorMatrixScript.destroy()
            rs.destroy()
        }
        super.onDetachedFromWindow()
    }

    override fun onAttachedToWindow() {
        if (!isInEditMode) {

            if (forceClip) {
                (parent as ViewGroup).clipChildren = false
            }
            rs = RenderScript.create(context)
            val element = Element.U8_4(rs)
            blurScript = ScriptIntrinsicBlur.create(rs, element)
            colorMatrixScript = ScriptIntrinsicColorMatrix.create(rs, element)
        }
        super.onAttachedToWindow()
    }

    private fun getBlurRadius(): Float {
        val maxElevation = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24f, resources.displayMetrics)
        return Math.min(25f * (customElevation / maxElevation), 25f)
    }

    private fun getShadowBitmap(bitmap: Bitmap): Bitmap {
        val allocationIn = Allocation.createFromBitmap(rs, bitmap)
        val allocationOut = Allocation.createTyped(rs, allocationIn.type)

        val matrix = if (isTranslucent) {
            Matrix4f(floatArrayOf(
                    0.4f, 0f, 0f, 0f,
                    0f, 0.4f, 0f, 0f,
                    0f, 0f, 0.4f, 0f,
                    0f, 0f, 0f, 0.6f))
        } else {
            Matrix4f(floatArrayOf(
                    0f, 0f, 0f, 0f,
                    0f, 0f, 0f, 0f,
                    0f, 0f, 0f, 0f,
                    0f, 0f, 0f, 0.4f))
        }

        colorMatrixScript.setColorMatrix(matrix)
        colorMatrixScript.forEach(allocationIn, allocationOut)

        blurScript.setRadius(getBlurRadius())

        blurScript.setInput(allocationOut)
        blurScript.forEach(allocationIn)

        allocationIn.copyTo(bitmap)

        allocationIn.destroy()
        allocationOut.destroy()

        return bitmap
    }

    private fun generateShadow() {
        drawable?.let {
            shadowBitmap = getShadowBitmap(getBitmapFromDrawable())
        }
    }

    private fun getBitmapFromDrawable(): Bitmap {
        val drawable = drawable

        val blurRadius = getBlurRadius()

        val width = width + 2 * blurRadius.toInt()
        val height = height + 2 * blurRadius.toInt()

        val bitmap = if (width <= 0 || height <= 0) {
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        } else {
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }

        val canvas = Canvas(bitmap)

        val imageMatrix = imageMatrix
        canvas.translate(paddingLeft + blurRadius, paddingTop + blurRadius)
        if (imageMatrix != null) {
            canvas.concat(imageMatrix)
        }
        drawable.draw(canvas)

        return bitmap
    }
}
