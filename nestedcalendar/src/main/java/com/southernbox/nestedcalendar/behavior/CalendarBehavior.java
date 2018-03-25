package com.southernbox.nestedcalendar.behavior;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.math.MathUtils;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Scroller;

import com.prolificinteractive.materialcalendarview.CalendarMode;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.southernbox.nestedcalendar.helper.ViewOffsetBehavior;

import java.util.Calendar;

import static android.support.v4.view.ViewCompat.TYPE_TOUCH;

/**
 * 列表 Behavior
 * Created by SouthernBox on 2018/1/19.
 */

public class CalendarBehavior extends ViewOffsetBehavior<MaterialCalendarView> {

    private CalendarMode calendarMode = CalendarMode.MONTHS;
    private int weekOfMonth = Calendar.getInstance().get(Calendar.WEEK_OF_MONTH);
    private int calendarLineHeight;
    private int velocityY;
    private boolean canAutoScroll = true;

    public CalendarBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onStartNestedScroll(@NonNull CoordinatorLayout coordinatorLayout,
                                       @NonNull MaterialCalendarView child,
                                       @NonNull View directTargetChild,
                                       @NonNull View target,
                                       int axes, int type) {
        return (axes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedPreScroll(@NonNull CoordinatorLayout coordinatorLayout,
                                  @NonNull final MaterialCalendarView child,
                                  @NonNull View target,
                                  int dx, int dy,
                                  @NonNull int[] consumed,
                                  int type) {
        if (target.canScrollVertically(-1)) {
            return;
        }
        setMonthMode(child);
        if (calendarMode == CalendarMode.MONTHS) {
            // 移动日历
            if (calendarLineHeight == 0) {
                calendarLineHeight = child.getMeasuredHeight() / 7;
            }
            int headerMinOffset = -calendarLineHeight * (weekOfMonth - 1);
            int headerOffset = MathUtils.clamp(
                    getTopAndBottomOffset() - dy, headerMinOffset, 0);
            setTopAndBottomOffset(headerOffset);

            // 移动列表
            final CoordinatorLayout.Behavior behavior =
                    ((CoordinatorLayout.LayoutParams) target.getLayoutParams()).getBehavior();
            if (behavior instanceof CalendarScrollBehavior) {
                final CalendarScrollBehavior listBehavior = (CalendarScrollBehavior) behavior;
                int listMinOffset = -calendarLineHeight * 5;
                int listOffset = MathUtils.clamp(
                        listBehavior.getTopAndBottomOffset() - dy, listMinOffset, 0);
                listBehavior.setTopAndBottomOffset(listOffset);
                if (listOffset > listMinOffset && listOffset < 0) {
                    consumed[1] = dy;
                }
            }
        }
    }

    @Override
    public void onStopNestedScroll(@NonNull final CoordinatorLayout coordinatorLayout,
                                   @NonNull final MaterialCalendarView child,
                                   @NonNull final View target,
                                   int type) {
        if (calendarLineHeight == 0) {
            return;
        }
        if (target.getTop() == calendarLineHeight * 2) {
            setWeekMode(child);
            return;
        } else if (target.getTop() == calendarLineHeight * 7) {
            setMonthMode(child);
            return;
        }
        if (!canAutoScroll) {
            return;
        }
        if (calendarMode == CalendarMode.MONTHS) {
            final Scroller scroller = new Scroller(coordinatorLayout.getContext());
            int scaleY;
            int duration = 800;
            if (Math.abs(velocityY) < 1000) {
                if (target.getTop() > calendarLineHeight * 4) {
                    scaleY = calendarLineHeight * 7 - target.getTop();
                } else {
                    scaleY = calendarLineHeight * 2 - target.getTop();
                }
            } else {
                if (velocityY > 0) {
                    // 滚动到周模式
                    scaleY = calendarLineHeight * 2 - target.getTop();
                } else {
                    // 滚动到月模式
                    scaleY = calendarLineHeight * 7 - target.getTop();
                }
            }
            duration = (duration * Math.abs(scaleY)) / (calendarLineHeight * 5);
            scroller.startScroll(
                    0, target.getTop(),
                    0, scaleY,
                    duration);
            ViewCompat.postOnAnimation(child, new Runnable() {
                @Override
                public void run() {
                    if (scroller.computeScrollOffset() &&
                            target instanceof RecyclerView) {
                        canAutoScroll = false;
                        RecyclerView recyclerView = (RecyclerView) target;
                        int delta = target.getTop() - scroller.getCurrY();
                        recyclerView.startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, TYPE_TOUCH);
                        recyclerView.dispatchNestedPreScroll(
                                0, delta, new int[2], new int[2], TYPE_TOUCH);
                        ViewCompat.postOnAnimation(child, this);
                    } else {
                        canAutoScroll = true;
                        // 滚动完成
                        if (target.getTop() == calendarLineHeight * 2) {
                            setWeekMode(child);
                        } else if (target.getTop() == calendarLineHeight * 7) {
                            setMonthMode(child);
                        }
                    }
                }
            });
        }
    }

    @Override
    public boolean onNestedPreFling(@NonNull CoordinatorLayout coordinatorLayout,
                                    @NonNull MaterialCalendarView child,
                                    @NonNull View target,
                                    float velocityX, float velocityY) {
        this.velocityY = (int) velocityY;
        return !(target.getTop() == calendarLineHeight * 2 ||
                target.getTop() == calendarLineHeight * 7);
    }

    private void setMonthMode(MaterialCalendarView calendarView) {
        if (calendarMode != CalendarMode.WEEKS) {
            return;
        }
        calendarMode = null;
        calendarView.state().edit()
                .setCalendarDisplayMode(CalendarMode.MONTHS)
                .commit();
        setTopAndBottomOffset(-calendarLineHeight * (weekOfMonth - 1));
        calendarMode = CalendarMode.MONTHS;
    }

    private void setWeekMode(MaterialCalendarView calendarView) {
        if (calendarMode != CalendarMode.MONTHS) {
            return;
        }
        calendarMode = null;
        calendarView.state().edit()
                .setCalendarDisplayMode(CalendarMode.WEEKS)
                .commit();
        setTopAndBottomOffset(0);
        calendarMode = CalendarMode.WEEKS;
    }

    public void setWeekOfMonth(int weekOfMonth) {
        this.weekOfMonth = weekOfMonth;
    }

    public CalendarMode getCalendarMode() {
        return calendarMode;
    }
}