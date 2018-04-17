package com.qhutch.elevationimageview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Region
import android.os.Build
import android.support.annotation.AttrRes
import android.support.v7.widget.AppCompatImageView
import android.support.v8.renderscript.Allocation
import android.support.v8.renderscript.Element
import android.support.v8.renderscript.RenderScript
import android.support.v8.renderscript.ScriptIntrinsicBlur
import android.util.AttributeSet
import android.util.TypedValue


/**
 * Created by quentin on 15/03/2018.
 */
class ElevationImageView : AppCompatImageView {

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

        a.recycle()
    }

    private var clipShadow = false

    private var shadowBitmap: Bitmap? = null

    private var customElevation = 0f

    private lateinit var rs: RenderScript
    private lateinit var blurScript: ScriptIntrinsicBlur
    private lateinit var convertToShadowAlphaScript: ScriptC_convertToShadowAlpha

    override fun setElevation(elevation: Float) {
        customElevation = elevation
        invalidate()
    }

    fun setElevationDp(elevation: Float) {
        customElevation = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, elevation, resources.displayMetrics)
        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        if (shadowBitmap == null && canvas != null && customElevation > 0) {
            generateShadow()
        }

        if (shadowBitmap != null) {

            if (!clipShadow) {
                canvas?.let {
                    val newRect = it.clipBounds
                    newRect.inset(-2 * getBlurRadius().toInt(), -2 * getBlurRadius().toInt())
                    it.clipRect(newRect, Region.Op.REPLACE)
                }
            }

            val bounds = drawable.copyBounds()
            canvas?.drawBitmap(shadowBitmap, bounds.left.toFloat() - getBlurRadius(), bounds.top - getBlurRadius() / 2f, null)

        }

        super.onDraw(canvas)
    }

    override fun invalidate() {
        shadowBitmap = null
        super.invalidate()
    }

    override fun onDetachedFromWindow() {
        blurScript.destroy()
        convertToShadowAlphaScript.destroy()
        rs.destroy()
        super.onDetachedFromWindow()
    }

    override fun onAttachedToWindow() {
        rs = RenderScript.create(context)
        blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
        convertToShadowAlphaScript = ScriptC_convertToShadowAlpha(rs)
        super.onAttachedToWindow()
    }

    private fun getBlurRadius(): Float {
        val maxElevation = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24f, resources.displayMetrics)
        return Math.min(25f * (customElevation / maxElevation), 25f)
    }

    private fun getShadowBitmap(bitmap: Bitmap): Bitmap {
        val allocationIn = Allocation.createFromBitmap(rs, bitmap)
        val allocationOut = Allocation.createTyped(rs, allocationIn.type)

        convertToShadowAlphaScript.forEach_root(allocationIn, allocationOut)

        blurScript.setRadius(getBlurRadius())

        blurScript.setInput(allocationOut)
        blurScript.forEach(allocationIn)

        allocationIn.copyTo(bitmap)

        allocationIn.destroy()
        allocationOut.destroy()

        return bitmap
    }

    private fun generateShadow() {
        shadowBitmap = getShadowBitmap(getBitmapFromDrawable())
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