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
    enum class BarStyle{
        FILL, STAIR
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
    private val mStairSpacePaint = Paint()

    private val mSeekbarPath = Path()
    private val mSeekbarLoadingPath = Path()

    private var mTextColor = 0
    private var mSeekbarColor: Int = 0
    private var mStairSpaceColor: Int = 0
    private var mSeekbarLoadingColor = 0

    private var mIsProgressVisible = false
    private var mBarStyle = BarStyle.FILL

    private var mFontName: String? = null

    private var mTextSize = 96f


    constructor(context: Context?) : super(context) {
        setOnTouchListener(this)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        setOnTouchListener(this)

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.TriangleSeekbar)

        mSeekbarColor =
            typedArray.getColor(
                R.styleable.TriangleSeekbar_seekbarColor,
                ContextCompat.getColor(context, R.color.seekbarPrimary)
            )
        mSeekbarLoadingColor =
            typedArray.getColor(
                R.styleable.TriangleSeekbar_seekbarLoadingColor,
                ContextCompat.getColor(context, R.color.seekbarPrimaryDark)
            )
        mStairSpaceColor =
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
        mBarStyle =
            BarStyle.entries[typedArray.getInt(R.styleable.TriangleSeekbar_barStyle, 0)]

        mProgressPosition =
            Position.entries[typedArray.getInt(R.styleable.TriangleSeekbar_progressTextPosition, 4)]
        mTextSize = typedArray.getDimension(R.styleable.TriangleSeekbar_textFontSize, 96f)
        mFontName = typedArray.getString(R.styleable.TriangleSeekbar_textFontName)

        progressvalue = typedArray.getFloat(R.styleable.TriangleSeekbar_progress, 0f)
        minValue = typedArray.getFloat(R.styleable.TriangleSeekbar_minValue, 0f)
        maxValue = typedArray.getFloat(R.styleable.TriangleSeekbar_maxValue, 100f)
        mStairBarLineWidth = typedArray.getFloat(R.styleable.TriangleSeekbar_stairBarLineWidth, 6f)

        mSeekbarPaint.color = mSeekbarColor
        mSeekbarLoadingPaint.color = mSeekbarLoadingColor
        mTextPaint.color = mTextColor
        mStairSpacePaint.color = mStairSpaceColor

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

                buildLoadingTriangle(x)
            }
        }
        invalidate()
        return true
    }

    @Synchronized
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        when(mBarStyle){
            BarStyle.FILL -> {
                canvas.drawPath(mSeekbarPath, mSeekbarPaint)
                canvas.drawPath(mSeekbarLoadingPath, mSeekbarLoadingPaint)
            }
            BarStyle.STAIR -> {
                drawBackgroundVerticalLines(canvas)
            }
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
                mStairSpacePaint.color
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


    private fun buildLoadingTriangle(motionX: Float) {
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

        progressvalue = calculatePercentage()
        mProgressListener?.onProgressChange((progressvalue))

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

    private fun calculatePercentage(): Float {
        val totalLines = (( mWidth.toFloat() / mStairBarLineWidth)/2).plus(1).toInt() // Total number of lines
        val loadedLines = ((mLoadedWidth.toFloat() / mStairBarLineWidth)/2).plus(1).toInt() // Total number of loaded lines

        // Calculate the raw percentage based on the area ratio
        val rawPercentage = loadedLines.toDouble() / totalLines
        val adjustedPercentage = if (mWidth == mLoadedWidth) {
            1.0
        }else if(mLoadedWidth<=0) {
            0.0
        }else {
            rawPercentage.coerceAtMost(1.0)
        }
        // Scale the raw percentage to the specified range
        val scaledPercentage = minValue + (adjustedPercentage * (maxValue - minValue))
        val flooredPercentage = floor(scaledPercentage)

        return flooredPercentage.toInt().toFloat()
    }

    /**
     * Sets the current progress value for the Triangle SeekBar and updates its visual representation.
     *
     * @param progress A Float value representing the current progress.
     *                 This value must be within the range of [minValue, maxValue].
     *
     * @throws IllegalArgumentException If the progress value is outside the range of minValue and maxValue.
     */
    fun setProgress(progress: Float) {

        require(progress in minValue..maxValue) { "Value must be between $minValue and $maxValue" }

        progressvalue = progress
        val normalizedProgress = (progress - minValue) / (maxValue - minValue)

        // Calculate the total number of vertical lines
        val totalLines = ((mWidth.toFloat() / mStairBarLineWidth) / 2).plus(1).toInt()

        // Calculate how many lines should be loaded based on the normalized progress
        val loadedLines = (totalLines * normalizedProgress).toInt()

        // Calculate the new width based on the number of loaded lines
        val newWidth = loadedLines * mStairBarLineWidth * 2
        buildLoadingTriangle(ceil(newWidth)) // progress listener hit on touch event
        invalidate()
    }

    /**
     * Holds the current progress value of the Triangle SeekBar.
     *
     * - The value is a `Float` representing the progress, ranging between `minValue` and `maxValue`.
     * - This value is updated when `setProgress()` is called.
     * - The value is **read-only** from outside the class but can be accessed via a getter.
     */
    var progressvalue: Float = 0.0f
        private set

    /**
     * Defines the minimum value for the Triangle SeekBar's progress range.
     *
     * - The value is a `Float` representing the lower bound of the progress range.
     * - Must always be less than `maxValue`. An exception is thrown if this condition is violated.
     * - Default value is `100.0f`.
     *
     * @throws IllegalArgumentException If the new `minValue` is not less than `maxValue`.
     */
    var minValue: Float = 0.0f
        set(value) {
            require(value < maxValue) { "minValue must be less than maxValue" }
            field = value
            invalidate()
        }

    /**
     * Defines the maximum value for the Triangle SeekBar's progress range.
     *
     * - The value is a `Float` representing the upper bound of the progress range.
     * - Must always be greater than `minValue`. An exception is thrown if this condition is violated.
     * - Default value is `100.0f`.
     *
     * @throws IllegalArgumentException If the new `maxValue` is not greater than `minValue`.
     */
    var maxValue: Float = 100.0f
        set(value) {
            require(minValue < value) { "maxValue must be greater than minValue" }
            field = value
            invalidate()
        }

    /**
     * Defines the color of the progress text displayed on the Triangle SeekBar.
     *
     * - The value is an `Int` representing a color.
     * - Default value is `Color.BLACK`.
     */
    var textColor: Int
        get() = mTextColor
        set(color) {
            this.mTextColor = color
            mTextPaint.color = mTextColor
            invalidate()
        }

    /**
     * Defines the color of the space between the stair bars in the Stair Style SeekBar.
     *
     * - The value is an `Int` representing a color.
     * - Default value is `Color.TRANSPARENT`.
     */
    var stairSpaceColor:Int
        get() = mStairSpaceColor
        set(color) {
            this.mStairSpaceColor = color
            mStairSpacePaint.color = mStairSpaceColor
            invalidate()
        }

    /**
     * Defines the color of the progress (loading) part of the SeekBar.
     *
     * - The value is an `Int` representing a color.
     * - Default value is `#E64A19`.
     */
    var seekbarLoadingColor: Int
        get() = mSeekbarLoadingColor
        set(color) {
            this.mSeekbarLoadingColor = color
            mSeekbarLoadingPaint.color = mSeekbarLoadingColor
            invalidate()
        }

    /**
     * Defines the color of the SeekBar's background or the unfilled part of the progress bar.
     *
     * - The value is an `Int` representing a color.
     * - Default value is `#FFA000`.
     */
    var seekbarColor:Int
        get() = mSeekbarColor
        set(color) {
            this.mSeekbarColor = color
            mSeekbarPaint.color = mSeekbarColor
            invalidate()
        }

    /**
     * Controls the visibility of the progress text on the Triangle SeekBar.
     *
     * - The value is a `Boolean`, where `true` makes the text visible, and `false` hides it.
     * - Default value is `false`, meaning the progress is hides by default.
     */
    var isProgressVisible: Boolean
        get() = mIsProgressVisible
        set(mIsProgressVisible) {
            this.mIsProgressVisible = mIsProgressVisible
            invalidate()
        }

    /**
     * Defines the size of the text displayed on the Triangle SeekBar.
     *
     * - The value is a `Float` representing the text size in pixels.
     * - Default value is `96f`.
     */
    var textSize: Float
        get() = mTextSize
        set(mTextSize) {
            this.mTextSize = mTextSize
            invalidate()
        }

    /**
     * Defines the style of the SeekBar (either 'normal' or 'stair' style).
     *
     * - The value is a `BarStyle` enum, which can either be `BarStyle.NORMAL` or `BarStyle.STAIR`.
     * - Default value is `BarStyle.NORMAL`.
     */
    var barStyle: BarStyle
        get() = mBarStyle
        set(mBarStyle) {
            this.mBarStyle = mBarStyle
            invalidate()
        }


    /**
     * Sets a listener to receive updates when the progress value changes.
     *
     * - The parameter `mProgressListener` is a `ProgressListener` that will be notified when the progress changes.
     * - If the listener is not null, the `onProgressChange` method will be called immediately with the current progress value.
     * - This listener allows you to track progress updates in real-time.
     */
    fun setProgressListener(mProgressListener: ProgressListener?) {
        this.mProgressListener = mProgressListener
        this.mProgressListener?.onProgressChange(progressvalue)
    }

    /**
     * Sets the custom font for the text displayed on the SeekBar.
     *
     * - The value is a `String?`, representing the font file name (e.g., "custom_font.ttf") located in the assets/fonts folder.
     * - When this property is set, it attempts to load the font from the assets folder and apply it to the text paint.
     * - If the font file is not found or the name is incorrect, an `IllegalArgumentException` is thrown with an appropriate message.
     * - This allows you to customize the font of the text displayed on the SeekBar.
     */
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

    /**
     * Defines the position of the progress text displayed on the SeekBar.
     *
     * - The value is a `Position` enum, which defines where the progress text should be positioned relative to the SeekBar.
     * - Valid values for `Position` include:
     *   - `Position.TOP_LEFT`: Places the text at the top-left corner of the SeekBar.
     *   - `Position.TOP_RIGHT`: Places the text at the top-right corner of the SeekBar.
     *   - `Position.BOTTOM_LEFT`: Places the text at the bottom-left corner of the SeekBar.
     *   - `Position.BOTTOM_RIGHT`: Places the text at the bottom-right corner of the SeekBar.
     *   - `Position.CENTER`: Places the text at the center of the SeekBar.
     * - Default value is `Position.CENTER`.
     */
    var progressTextPosition: Position
        get() = mProgressPosition
        set(mProgressPosition) {
            this.mProgressPosition = mProgressPosition
            invalidate()
        }

}