package com.github.milanmaji.triangleseekbar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sqrt

class TriangleSeekbar : View, View.OnTouchListener {
    enum class Position {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER
    }

    interface ProgressListener {
        fun onProgressChange(progress: Float)
    }

    private var mProgressListener: ProgressListener? = null


    private var mHeight = 0
    private var mWidth = 0

    private var mLoadedHeight = 0

    private var mLoadedWidth = 0

    private var mStairBarLineWidth = 0f

    private var mProgressX = 0f
    private var mProgressY = 0f

    private var mProgressPosition = Position.CENTER
    private val mTextPaint = Paint()
    private val mSeekbarPaint = Paint()
    private val mSeekbarLoadingPaint = Paint()

    private val mSeekbarPath = Path()
    private val mSeekbarLoadingPath = Path()

    private var mTextColor = 0
    var seekbarColor: Int = 0
        private set
    var stairSpaceColor: Int = 0
        private set
    private var mSeekbarLoadingColor = 0

    private var mIsProgressVisible = false
    private var mIsStaircaseStyle = false
    private var isLayoutComplete = false

    private var mFontName: String? = null

    private var mTextSize = 96f

    var progressvalue: Float = 0.0f
        private set
    var minValue: Float = 0.0f
        set(value) {
            field = value
            invalidate()
        }
    var maxValue: Float = 0.0f
        set(value) {
            field = value
            invalidate()
        }


    constructor(context: Context?) : super(context) {
        setOnTouchListener(this)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        setOnTouchListener(this)

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.TriangleSeekbar)

        seekbarColor =
            typedArray.getColor(
                R.styleable.TriangleSeekbar_seekbarColor,
                ContextCompat.getColor(context, R.color.seekbarPrimary)
            )
        mSeekbarLoadingColor =
            typedArray.getColor(
                R.styleable.TriangleSeekbar_seekbarLoadingColor,
                ContextCompat.getColor(context, R.color.seekbarPrimaryDark)
            )
        stairSpaceColor =
            typedArray.getColor(
                R.styleable.TriangleSeekbar_stairSpaceColor,
                ContextCompat.getColor(context, R.color.transparent)
            )

        mTextColor =
            typedArray.getColor(
                R.styleable.TriangleSeekbar_textColor,
                Color.BLACK
            )


        mIsProgressVisible = typedArray.getBoolean(R.styleable.TriangleSeekbar_showProgress, false)
        mIsStaircaseStyle =
            typedArray.getBoolean(R.styleable.TriangleSeekbar_staircaseStyle, false)

        mProgressPosition =
            Position.entries[typedArray.getInt(R.styleable.TriangleSeekbar_progressTextPosition, 4)]
        mTextSize = typedArray.getDimension(R.styleable.TriangleSeekbar_textFontSize, 96f)
        mFontName = typedArray.getString(R.styleable.TriangleSeekbar_textFontName)

        progressvalue = typedArray.getFloat(R.styleable.TriangleSeekbar_progress, 0f)
        minValue = typedArray.getFloat(R.styleable.TriangleSeekbar_minValue, 0f)
        maxValue = typedArray.getFloat(R.styleable.TriangleSeekbar_maxValue, 100f)
        mStairBarLineWidth = typedArray.getFloat(R.styleable.TriangleSeekbar_stairBarLineWidth, 6f)

        mSeekbarPaint.color = seekbarColor
        mSeekbarLoadingPaint.color = mSeekbarLoadingColor
        mTextPaint.color = mTextColor

        mTextPaint.textSize = mTextSize
        if (mFontName != null) {
            try {
                mTextPaint.setTypeface(
                    Typeface.createFromAsset(
                        context.assets,
                        "/fonts/\$mFontName"
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        typedArray.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        mHeight = MeasureSpec.getSize(heightMeasureSpec)
        mWidth = MeasureSpec.getSize(widthMeasureSpec)
    }


    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        isLayoutComplete = true
        mSeekbarPath.moveTo(mWidth.toFloat(), 0f)
        mSeekbarPath.lineTo(mWidth.toFloat(), mHeight.toFloat())
        mSeekbarPath.lineTo(0f, mHeight.toFloat())

        if (progressvalue > 0) {
            setProgress(progressvalue)
        }
    }


    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_MOVE, MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP ->  {
                var x = event.x
                if (x < 0) {
                    x = 0f
                } else if (x > mWidth) {
                    x = mWidth.toFloat()
                }

                buildLoadingTriangle(x,mProgressListener)
            }
        }
        invalidate()
        return true
    }

    @Synchronized
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if(mIsStaircaseStyle)
            drawBackgroundVerticalLines(canvas)
        else{
            canvas.drawPath(mSeekbarPath, mSeekbarPaint)
            canvas.drawPath(mSeekbarLoadingPath, mSeekbarLoadingPaint)
        }
        if (mIsProgressVisible) {
            val mPercentage = (progressvalue - minValue) / (maxValue - minValue)
            canvas.drawText(
                Math.round(mPercentage * 100f).toString() + " % ",
                mProgressX,
                mProgressY,
                mTextPaint
            )
        }
    }

    private fun drawBackgroundVerticalLines(canvas: Canvas) {
        val viewWidth = mWidth.toFloat() // Width of the view
        val totalLines = (viewWidth / mStairBarLineWidth).toInt() // Total number of lines

        val lineSpacing = viewWidth / totalLines // Spacing between lines
        val linePaint = Paint()

        var currentX = viewWidth // Start from the right side of the view
        var isBackgroundColor = false // Alternating flag for line color
        while (currentX >= 0) { // Draw lines from right to left
            linePaint.color = if (isBackgroundColor) {
                stairSpaceColor
            } else if (currentX <= mLoadedWidth) {
                mSeekbarLoadingPaint.color
            } else {
                mSeekbarPaint.color
            }

            // Calculate the height of the line based on its position
            val bottomRightY = mHeight.toFloat()
            val topRightY = mHeight - (currentX * (mHeight / viewWidth))
            val topLeftY = mHeight - ((currentX-mStairBarLineWidth) * (mHeight / viewWidth))
            val topLeftX = currentX-mStairBarLineWidth
            val path = Path().apply {
                moveTo(currentX, bottomRightY) // Start at the bottom-right corner
                lineTo(topLeftX, bottomRightY) // Line to the bottom-left corner
                lineTo(topLeftX, topLeftY) // Line to the top-left corner
                lineTo(currentX, topRightY) // Line to the bottom-right corner
                close() // Close the path
            }

            canvas.drawPath(path,linePaint)

            currentX -= lineSpacing // Move left by the calculated spacing value
            isBackgroundColor = !isBackgroundColor // Toggle color
        }
    }


    private fun buildLoadingTriangle(motionX: Float, listener: ProgressListener?) {
        if(!isLayoutComplete) return
        mSeekbarLoadingPath.reset()
        var hypotenuse = sqrt((mHeight * mHeight + mWidth * mWidth).toDouble())
        val sinA = mHeight / hypotenuse
        val cosA = sqrt((1 - (sinA * sinA)))
        hypotenuse = motionX / cosA
        mLoadedHeight = Math.round(hypotenuse * sinA).toInt()
        mLoadedWidth = Math.round(motionX)
        mSeekbarLoadingPath.moveTo(0f, mHeight.toFloat())

        mSeekbarLoadingPath.lineTo(mLoadedWidth.toFloat(), mHeight.toFloat())
        mSeekbarLoadingPath.lineTo(mLoadedWidth.toFloat(), ((mHeight - mLoadedHeight).toFloat()))

        progressvalue = calculateProgressValue()
        listener?.onProgressChange((progressvalue))

        setProgressPosition(mProgressPosition)
    }


    private fun setProgressPosition(position: Position) {
        val bounds = Rect()

        val text = "" + Math.round(progressvalue) + " % "
        mTextPaint.getTextBounds(text, 0, text.length, bounds)

        when (position) {
            Position.TOP_LEFT -> {
                mProgressX = bounds.height() * 0.25f
                mProgressY = bounds.height() + bounds.height() * 0.25f
            }

            Position.TOP_RIGHT -> {
                mProgressX = mWidth - (bounds.width() + bounds.height() * 0.25f)
                mProgressY = bounds.height() + bounds.height() * 0.25f
            }

            Position.BOTTOM_LEFT -> {
                mProgressX = bounds.height() / 2f
                mProgressY = mHeight - bounds.height() * 0.25f
            }

            Position.BOTTOM_RIGHT -> {
                mProgressX = mWidth - (bounds.width() + bounds.height() * 0.25f)
                mProgressY = mHeight - bounds.height() * 0.25f
            }

            Position.CENTER -> {
                mProgressX = mWidth / 2f
                mProgressY = mHeight / 1.25f
            }
        }
        invalidate()
    }

    /*//old calculation (az.rasul.triangleseekbar.TriangleSeekbar)

    private fun calculatePercentage(): Float {
        val loadedArea = (mLoadedHeight * mLoadedWidth).toDouble()
        val fullArea = (mHeight * mWidth)
        var bd: BigDecimal = BigDecimal((loadedArea / fullArea))
        bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP)
        return bd.floatValue()
    }


    fun setProgress(progress: Float) {
        if (progress >= 0.0 && progress <= 1.0) {
            val newWidth = mWidth * sqrt(progress.toDouble())
            buildLoadingTriangle(ceil(newWidth).toFloat())
            invalidate()
        } else {
            throw java.lang.IllegalArgumentException("Progress must be between 0.0 and 1.0")
        }
    }*/

    private fun calculateProgressValue(): Float {
        val totalLines = (( mWidth.toFloat() / mStairBarLineWidth)/2).plus(1).toInt() // Total number of lines
        val loadedLines = ((mLoadedWidth.toFloat() / mStairBarLineWidth)/2).plus(1).toInt() // Total number of loaded lines

        // Calculate the raw percentage based on the area ratio
        val rawPercentage = loadedLines.toDouble() / totalLines
        val adjustedPercentage = if (mWidth == mLoadedWidth) {
            1.0
        }else if(mLoadedWidth<=0) {
            0.0
        }else {
            rawPercentage.coerceAtMost(0.99)
        }
        // Scale the raw percentage to the specified range
        val scaledPercentage = minValue + (adjustedPercentage * (maxValue - minValue))
        val flooredPercentage = floor(scaledPercentage)

        return flooredPercentage.toInt().toFloat()
    }

    fun setProgress(progress: Float) {
        if (progress in minValue..maxValue) {
            progressvalue = progress
            val normalizedProgress = (progress - minValue) / (maxValue - minValue)

            // Calculate the total number of vertical lines
            val totalLines = ((mWidth.toFloat() / mStairBarLineWidth) / 2).plus(1).toInt()

            // Calculate how many lines should be loaded based on the normalized progress
            val loadedLines = (totalLines * normalizedProgress).toInt()

            // Calculate the new width based on the number of loaded lines
            val newWidth = loadedLines * mStairBarLineWidth * 2
            buildLoadingTriangle(ceil(newWidth),null) // progress listener hit on touch event
            invalidate()
        }
        else {
            throw IllegalArgumentException("Value must be between $minValue and $maxValue")
        }
    }

    var textColor: Int
        get() = mTextColor
        set(color) {
            this.mTextColor = color
            mTextPaint.color = mTextColor
            invalidate()
        }

    fun setSeekBarColor(color: Int) {
        this.seekbarColor = color
        mSeekbarPaint.color = seekbarColor
        invalidate()
    }


    var seekbarLoadingColor: Int
        get() = mSeekbarLoadingColor
        set(color) {
            this.mSeekbarLoadingColor = color
            mSeekbarLoadingPaint.color = mSeekbarLoadingColor
            invalidate()
        }

    var isProgressVisible: Boolean
        get() = mIsProgressVisible
        set(mIsProgressVisible) {
            this.mIsProgressVisible = mIsProgressVisible
            invalidate()
        }


    var textSize: Float
        get() = mTextSize
        set(mTextSize) {
            this.mTextSize = mTextSize
            invalidate()
        }

    fun setProgressListener(mProgressListener: ProgressListener?) {
        this.mProgressListener = mProgressListener
    }

    var fontName: String?
        get() = mFontName
        set(mFontName) {
            this.mFontName = mFontName
            try {
                mTextPaint.setTypeface(
                    Typeface.createFromAsset(
                        context.assets,
                        "/fonts/$mFontName"
                    )
                )
            } catch (e: Exception) {
                throw IllegalArgumentException("Please check that you correctly set the font")
            }
            invalidate()
        }

}