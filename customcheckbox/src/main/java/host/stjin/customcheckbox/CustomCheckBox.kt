package host.stjin.customcheckbox

import android.widget.Checkable
import kotlin.jvm.JvmOverloads
import android.annotation.TargetApi
import android.os.Build
import android.os.Parcelable
import android.os.Bundle
import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import net.stjin.customcheckbox.R
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Forked from https://github.com/andyxialm/SmoothCheckBox/blob/master/library/src/main/java/cn/refactor/library/SmoothCheckBox.java
 *
 * @author andyxialm
 */
class CustomCheckBox : View, Checkable {
    private var mPaint: Paint? = null
    private var mTickPaint: Paint? = null
    private var mFloorPaint: Paint? = null
    private var mTickPoints: Array<Point?> = arrayOfNulls(3)
    private var mCenterPoint: Point? = null
    private var mTickPath: Path? = null
    private var mLeftLineDistance = 0f
    private var mRightLineDistance = 0f
    private var mDrewDistance = 0f
    private var mScaleVal = 1.0f
    private var mFloorScale = 1.0f
    private var mWidth = 0
    private var mAnimDuration = 0
    private var mStrokeWidth = 0
    private var mTickColor = Color.WHITE
    var checkedColor = 0
    var unCheckedColor = 0
    var floorColor = 0
    var floorUnCheckedColor = 0
    private var mChecked = false
    private var mTickDrawing = false
    private var mListener: OnCheckedChangeListener? = null
    var isSmallTick = false

    @JvmOverloads
    constructor(context: Context?, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(attrs)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.CustomCheckBox)
        mTickColor = ta.getColor(R.styleable.CustomCheckBox_color_tick, COLOR_TICK)
        mAnimDuration = ta.getInt(R.styleable.CustomCheckBox_duration, DEF_ANIM_DURATION)
        floorColor =
            ta.getColor(R.styleable.CustomCheckBox_color_unchecked_stroke, COLOR_FLOOR_UNCHECKED)
        checkedColor = ta.getColor(R.styleable.CustomCheckBox_color_checked, COLOR_CHECKED)
        unCheckedColor = ta.getColor(R.styleable.CustomCheckBox_color_unchecked, COLOR_UNCHECKED)
        mStrokeWidth = ta.getDimensionPixelSize(
            R.styleable.CustomCheckBox_stroke_width, dp2px(
                context, 0f
            )
        )
        isSmallTick = ta.getBoolean(R.styleable.CustomCheckBox_small_tick, false)
        ta.recycle()
        floorUnCheckedColor = floorColor
        mTickPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mTickPaint!!.style = Paint.Style.STROKE
        mTickPaint!!.strokeCap = Paint.Cap.SQUARE
        mTickPaint!!.color = mTickColor
        mFloorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mFloorPaint!!.style = Paint.Style.FILL
        mFloorPaint!!.color = floorColor
        mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mPaint!!.style = Paint.Style.FILL
        mPaint!!.color = checkedColor
        mTickPath = Path()
        mCenterPoint = Point()
        mTickPoints = arrayOfNulls(3)
        mTickPoints[0] = Point()
        mTickPoints[1] = Point()
        mTickPoints[2] = Point()
        setOnClickListener {
            toggle()
            mTickDrawing = false
            mDrewDistance = 0f
            if (isChecked) {
                startCheckedAnimation()
            } else {
                startUnCheckedAnimation()
            }
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        val bundle = Bundle()
        bundle.putParcelable(KEY_INSTANCE_STATE, super.onSaveInstanceState())
        bundle.putBoolean(KEY_INSTANCE_STATE, isChecked)
        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state is Bundle) {
            val isChecked = state.getBoolean(KEY_INSTANCE_STATE)
            setChecked(isChecked)
            super.onRestoreInstanceState(state.getParcelable(KEY_INSTANCE_STATE))
            return
        }
        super.onRestoreInstanceState(state)
    }

    override fun isChecked(): Boolean {
        return mChecked
    }

    override fun toggle() {
        this.isChecked = !isChecked
    }

    override fun setChecked(checked: Boolean) {
        mChecked = checked
        reset()
        invalidate()
        if (mListener != null) {
            mListener!!.onCheckedChanged(this@CustomCheckBox, mChecked)
        }
    }

    /**
     * checked with animation
     *
     * @param checked checked
     * @param animate change with animation
     */
    fun setChecked(checked: Boolean, animate: Boolean) {
        if (animate) {
            mTickDrawing = false
            mChecked = checked
            mDrewDistance = 0f
            if (checked) {
                startCheckedAnimation()
            } else {
                startUnCheckedAnimation()
            }
            if (mListener != null) {
                mListener!!.onCheckedChanged(this@CustomCheckBox, mChecked)
            }
        } else {
            this.isChecked = checked
        }
    }

    private fun reset() {
        mTickDrawing = true
        mFloorScale = 1.0f
        mScaleVal = if (isChecked) 0f else 1.0f
        floorColor = if (isChecked) checkedColor else floorUnCheckedColor
        mDrewDistance = if (isChecked) (mLeftLineDistance + mRightLineDistance) else 0f
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        //Measure Width
        val width: Int = when (widthMode) {
            MeasureSpec.EXACTLY -> {
                //Must be this size
                widthSize
            }
            MeasureSpec.AT_MOST -> {
                //Can't be bigger than...
                DEF_DRAW_SIZE.coerceAtMost(widthSize)
            }
            else -> {
                //Be whatever you want
                DEF_DRAW_SIZE
            }
        }

        //Measure Height
        val height: Int = when (heightMode) {
            MeasureSpec.EXACTLY -> {
                //Must be this size
                heightSize
            }
            MeasureSpec.AT_MOST -> {
                //Can't be bigger than...
                DEF_DRAW_SIZE.coerceAtMost(heightSize)
            }
            else -> {
                //Be whatever you want
                DEF_DRAW_SIZE
            }
        }

        //MUST CALL THIS
        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        mWidth = measuredWidth
        val totalPoints: Int = if (isSmallTick) {
            40
        } else {
            20
        }
        mStrokeWidth = measuredWidth / totalPoints
        mCenterPoint!!.x = mWidth / 2
        mCenterPoint!!.y = measuredHeight / 2
        val widthUnity = measuredWidth.toFloat() / totalPoints
        val heightUnity = measuredHeight.toFloat() / totalPoints
        if (isSmallTick) {
            mTickPoints[0]!!.x = (widthUnity * 15).roundToInt()
            mTickPoints[0]!!.y = (heightUnity * 20).roundToInt()
            mTickPoints[1]!!.x = (widthUnity * 19).roundToInt()
            mTickPoints[1]!!.y = (heightUnity * 23).roundToInt()
            mTickPoints[2]!!.x = (widthUnity * 25).roundToInt()
            mTickPoints[2]!!.y = (heightUnity * 18).roundToInt()
        } else {
            mTickPoints[0]!!.x = (widthUnity * 6).roundToInt()
            mTickPoints[0]!!.y = (heightUnity * 10).roundToInt()
            mTickPoints[1]!!.x = (widthUnity * 8).roundToInt()
            mTickPoints[1]!!.y = (heightUnity * 13).roundToInt()
            mTickPoints[2]!!.x = (widthUnity * 15).roundToInt()
            mTickPoints[2]!!.y = (heightUnity * 8).roundToInt()
        }
        mLeftLineDistance = sqrt(
            (mTickPoints[1]!!.x - mTickPoints[0]!!.x).toDouble().pow(2.0) +
                    (mTickPoints[1]!!.y - mTickPoints[0]!!.y).toDouble().pow(2.0)
        ).toFloat()
        mRightLineDistance = sqrt(
            (mTickPoints[2]!!.x - mTickPoints[1]!!.x).toDouble().pow(2.0) +
                    (mTickPoints[2]!!.y - mTickPoints[1]!!.y).toDouble().pow(2.0)
        ).toFloat()
        mTickPaint!!.strokeWidth = mStrokeWidth.toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        drawBorder(canvas)
        drawCenter(canvas)
        drawTick(canvas)
    }

    private fun drawCenter(canvas: Canvas) {
        mPaint!!.color = unCheckedColor
        val radius = (mCenterPoint!!.x - mStrokeWidth) * mScaleVal
        canvas.drawCircle(mCenterPoint!!.x.toFloat(), mCenterPoint!!.y.toFloat(), radius, mPaint!!)
    }

    private fun drawBorder(canvas: Canvas) {
        mFloorPaint!!.color = floorColor
        val radius = mCenterPoint!!.x
        canvas.drawCircle(
            mCenterPoint!!.x.toFloat(),
            mCenterPoint!!.y.toFloat(),
            radius * mFloorScale,
            mFloorPaint!!
        )
    }

    private fun drawTick(canvas: Canvas) {
        if (mTickDrawing && isChecked) {
            drawTickPath(canvas)
        }
    }

    private fun drawTickPath(canvas: Canvas) {
        mTickPath!!.reset()
        // draw left of the tick
        if (mDrewDistance < mLeftLineDistance) {
            val step: Float = if ((mWidth / 20.0f) < 3) 3f else (mWidth / 20.0f)
            mDrewDistance += step
            val stopX =
                mTickPoints[0]!!.x + (mTickPoints[1]!!.x - mTickPoints[0]!!.x) * mDrewDistance / mLeftLineDistance
            val stopY =
                mTickPoints[0]!!.y + (mTickPoints[1]!!.y - mTickPoints[0]!!.y) * mDrewDistance / mLeftLineDistance
            mTickPath!!.moveTo(mTickPoints[0]!!.x.toFloat(), mTickPoints[0]!!.y.toFloat())
            mTickPath!!.lineTo(stopX, stopY)
            mTickPaint?.let { canvas.drawPath(mTickPath!!, it) }
            if (mDrewDistance > mLeftLineDistance) {
                mDrewDistance = mLeftLineDistance
            }
        } else {
            mTickPath!!.moveTo(mTickPoints[0]!!.x.toFloat(), mTickPoints[0]!!.y.toFloat())
            mTickPath!!.lineTo(mTickPoints[1]!!.x.toFloat(), mTickPoints[1]!!.y.toFloat())
            mTickPaint?.let { canvas.drawPath(mTickPath!!, it) }

            // draw right of the tick
            if (mDrewDistance < mLeftLineDistance + mRightLineDistance) {
                val stopX =
                    mTickPoints[1]!!.x + (mTickPoints[2]!!.x - mTickPoints[1]!!.x) * (mDrewDistance - mLeftLineDistance) / mRightLineDistance
                val stopY =
                    mTickPoints[1]!!.y - (mTickPoints[1]!!.y - mTickPoints[2]!!.y) * (mDrewDistance - mLeftLineDistance) / mRightLineDistance
                mTickPath!!.reset()
                mTickPath!!.moveTo(mTickPoints[1]!!.x.toFloat(), mTickPoints[1]!!.y.toFloat())
                mTickPath!!.lineTo(stopX, stopY)
                mTickPaint?.let { canvas.drawPath(mTickPath!!, it) }
                val step: Float = if ((mWidth / 20) < 3) 3f else (mWidth / 20).toFloat()
                mDrewDistance += step
            } else {
                mTickPath!!.reset()
                mTickPath!!.moveTo(mTickPoints[1]!!.x.toFloat(), mTickPoints[1]!!.y.toFloat())
                mTickPath!!.lineTo(mTickPoints[2]!!.x.toFloat(), mTickPoints[2]!!.y.toFloat())
                mTickPaint?.let { canvas.drawPath(mTickPath!!, it) }
            }
        }

        // invalidate
        if (mDrewDistance < mLeftLineDistance + mRightLineDistance) {
            postDelayed({ postInvalidate() }, 10)
        }
    }

    private fun startCheckedAnimation() {
        val animator = ValueAnimator.ofFloat(1.0f, 0f)
        animator.duration = (mAnimDuration / 3 * 2).toLong()
        animator.interpolator = LinearInterpolator()
        animator.addUpdateListener { animation ->
            mScaleVal = animation.animatedValue as Float
            floorColor = getGradientColor(unCheckedColor, checkedColor, 1 - mScaleVal)
            postInvalidate()
        }
        animator.start()
        val floorAnimator = ValueAnimator.ofFloat(1.0f, 0.8f, 1.0f)
        floorAnimator.duration = mAnimDuration.toLong()
        floorAnimator.interpolator = LinearInterpolator()
        floorAnimator.addUpdateListener { animation ->
            mFloorScale = animation.animatedValue as Float
            postInvalidate()
        }
        floorAnimator.start()
        drawTickDelayed()
    }

    private fun startUnCheckedAnimation() {
        val animator = ValueAnimator.ofFloat(0f, 1.0f)
        animator.duration = mAnimDuration.toLong()
        animator.interpolator = LinearInterpolator()
        animator.addUpdateListener { animation ->
            mScaleVal = animation.animatedValue as Float
            floorColor = getGradientColor(checkedColor, floorUnCheckedColor, mScaleVal)
            postInvalidate()
        }
        animator.start()
        val floorAnimator = ValueAnimator.ofFloat(1.0f, 0.8f, 1.0f)
        floorAnimator.duration = mAnimDuration.toLong()
        floorAnimator.interpolator = LinearInterpolator()
        floorAnimator.addUpdateListener { animation ->
            mFloorScale = animation.animatedValue as Float
            postInvalidate()
        }
        floorAnimator.start()
    }

    private fun drawTickDelayed() {
        postDelayed({
            mTickDrawing = true
            postInvalidate()
        }, mAnimDuration.toLong())
    }

    var tickColor: Int
        get() = mTickColor
        set(color) {
            mTickColor = color
            mTickPaint!!.color = mTickColor
        }

    fun setOnCheckedChangeListener(l: OnCheckedChangeListener?) {
        mListener = l
    }

    interface OnCheckedChangeListener {
        fun onCheckedChanged(checkBox: CustomCheckBox?, isChecked: Boolean)
    }

    companion object {
        private val KEY_INSTANCE_STATE = "InstanceState"
        private val COLOR_TICK = Color.WHITE
        private val COLOR_UNCHECKED = Color.WHITE
        private val COLOR_CHECKED = Color.parseColor("#FB4846")
        private val COLOR_FLOOR_UNCHECKED = Color.parseColor("#DFDFDF")
        private val DEF_DRAW_SIZE = 100
        private val DEF_ANIM_DURATION = 300
        private fun getGradientColor(startColor: Int, endColor: Int, percent: Float): Int {
            val startA = Color.alpha(startColor)
            val startR = Color.red(startColor)
            val startG = Color.green(startColor)
            val startB = Color.blue(startColor)
            val endA = Color.alpha(endColor)
            val endR = Color.red(endColor)
            val endG = Color.green(endColor)
            val endB = Color.blue(endColor)
            val currentA = (startA * (1 - percent) + endA * percent).toInt()
            val currentR = (startR * (1 - percent) + endR * percent).toInt()
            val currentG = (startG * (1 - percent) + endG * percent).toInt()
            val currentB = (startB * (1 - percent) + endB * percent).toInt()
            return Color.argb(currentA, currentR, currentG, currentB)
        }

        private fun dp2px(context: Context, dipValue: Float): Int {
            val scale = context.resources.displayMetrics.density
            return (dipValue * scale + 0.5f).toInt()
        }
    }
}