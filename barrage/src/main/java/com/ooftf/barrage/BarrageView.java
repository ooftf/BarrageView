package com.ooftf.barrage;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;


/**
 * @author ooftf
 * @date 2018/8/8
 * @desc
 **/
public class BarrageView extends RelativeLayout {
    /**
     * 最小移动速度
     */
    public static final float SPEED_MIN = 1f;
    /**
     * 最大可正偏移量
     */
    public static final float SPEED_OFFSET = 1f;

    public BarrageView(Context context) {
        super(context);
        init();
    }

    public BarrageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BarrageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public BarrageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    /**
     * 定时器
     */
    Flowable<Long> interval;

    /**
     * 初始化
     */
    void init() {

        interval = Flowable.interval(100, 1000 / 100, TimeUnit.MILLISECONDS).observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * 移动弹幕
     */
    private void move() {
        for (LineItem es : moving) {
            Iterator<Item> iterator = es.items.iterator();
            while (iterator.hasNext()) {
                Item next = iterator.next();
                next.rectF.offset(-es.speed, 0);
                if (next.rectF.right < 0) {
                    viewCreator.destroyView(this,next.view);
                    iterator.remove();
                }
            }
        }
        requestLayout();
    }

    List<LineItem> moving = new ArrayList<>();
    ViewOperator viewCreator;
    List<Item> waiting = new ArrayList<>();

    public void addItem(Object object) {
        if (viewCreator == null) {
            return;
        }
        Item item = createItem(object);
        addView(item.view, new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        waiting.add(item);
    }

    public void setViewCreator(ViewOperator viewCreator) {
        this.viewCreator = viewCreator;
    }

    /**
     * 创建itemView
     *
     * @param object
     * @return
     */
    public View createView(Object object) {
        return viewCreator.createView(this, object);
    }


    /**
     * 创建item
     *
     * @param object
     * @return
     */
    private Item createItem(Object object) {
        Item item = new Item();
        item.view = createView(object);
        return item;
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        initMoving();
        /**
         * layout
         */
        for (LineItem lineItem : moving) {
            for (Item e : lineItem.items) {
                e.view.layout((int) e.rectF.left, (int) e.rectF.top, (int) e.rectF.right, (int) e.rectF.bottom);
            }

        }
    }
    /**
     * 初始化moving
     */
    private void initMoving() {
        if (moving.size() == 0 && waiting.size() > 0) {
            //计算有多少行
            int lines = getHeight() / waiting.get(0).view.getMeasuredHeight();
            for (int i = 0; i < lines; i++) {
                LineItem lineItem = new LineItem();
                lineItem.speed = getRandomSpeed();
                moving.add(lineItem);
            }
        }
    }
    /**
     * 将等待区的item转移到moving区
     */
    private void waitingToMoving() {
        if(moving.size() == 0){//如果没有初始化不执行这个操作
            return;
        }
        Iterator<Item> iterator = waiting.iterator();
        while (iterator.hasNext()) {
            Item next = iterator.next();

            // 计算应该加入第几行
            int line = calculateLine();
            iterator.remove();
            LineItem lineItem = moving.get(line);
            /**
             * 计算left位置
             */
            float left;
            if (lineItem.items.size() == 0) {
                left = getWidth();
            } else {
                float lineRight = lineItem.items.get(lineItem.items.size() - 1).rectF.right;
                left = Math.max(getWidth(), lineRight);
            }
            next.rectF = new RectF(left, line * next.view.getMeasuredHeight(), left + next.view.getMeasuredWidth(), (line + 1) * next.view.getMeasuredHeight());
            moving.get(line).items.add(next);
        }
    }

    /**
     * 生成随机数列
     *
     * @return
     */
    private int getRandomSpeed() {
        return Math.round(SPEED_MIN + random.nextFloat() * SPEED_OFFSET);
    }

    private Random random = new Random();

    /**
     * 计算下个item应该放在第几行
     *
     * @return
     */
    private int calculateLine() {
        int result = 0;
        float minRight = Float.MAX_VALUE;
        //因为不希望每次都从第一行开始检查，导致弹幕都是优先加入第一行所以引入随机数机制,除以2是希望优先上半边屏幕
        int randomInt = random.nextInt(moving.size() / 2);
        for (int i = 0; i < moving.size(); i++) {
            int realLine = (randomInt + i) % moving.size();
            LineItem lineItem = moving.get(realLine);
            if (lineItem.items.size() > 0) {
                /**
                 * 如果这一行不为空
                 */
                float right = lineItem.items.get(lineItem.items.size() - 1).rectF.right;
                /**
                 * 如果这行弹幕已有半个屏幕是空的，则选择这一行
                 */
                if (right < getWidth() / 2) {
                    return realLine;
                }
                if (right < minRight) {
                    result = realLine;
                    minRight = right;
                }
            } else {
                /**
                 * 如果检测到这一行为空则指定为这一行
                 */
                return realLine;
            }
        }
        return result;
    }

    Disposable disposable;

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = interval.subscribe(new Consumer<Long>() {
            @Override
            public void accept(Long aLong) throws Exception {
                move();
                waitingToMoving();
            }
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (disposable != null) {
            disposable.dispose();
        }
    }

    /**
     * 单个弹幕对象
     */
    static class Item {
        RectF rectF;
        View view;
    }

    /**
     * 一行弹幕对象
     */
    static class LineItem {
        ArrayList<Item> items = new ArrayList<>();
        int speed;
    }

    public interface ViewOperator {
        /**
         * 创建itemView
         *
         * @param parent
         * @param object
         * @return
         */
        View createView(BarrageView parent, Object object);

        void destroyView(BarrageView parent, View view);
    }
}
