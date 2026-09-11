package com.memforce.ui.common;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;
import com.memforce.R;

/**
 * Swipe gestures on a list row: left for the destructive action, right for adding to the lobby.
 *
 * <p>The directions are the absolute ones rather than start and end, so the gesture a user learns
 * on one screen is the same gesture on every other, whichever way the layout runs. Neither action
 * removes the row by itself: the handler decides, and the row is put back so that a cancelled
 * confirmation leaves the list as it was.
 */
public final class SwipeActions {

    public interface Action {
        void invoke(int position);
    }

    private SwipeActions() {
    }

    /**
     * @param onSwipeLeft  invoked for a swipe to the left, or null to forbid that direction
     * @param onSwipeRight invoked for a swipe to the right, or null to forbid that direction
     */
    public static void attach(@NonNull RecyclerView list,
                              @Nullable Action onSwipeLeft,
                              @Nullable Action onSwipeRight) {
        int directions = (onSwipeLeft == null ? 0 : ItemTouchHelper.LEFT)
                | (onSwipeRight == null ? 0 : ItemTouchHelper.RIGHT);
        if (directions == 0) {
            return;
        }
        new ItemTouchHelper(new Callback(list, directions, onSwipeLeft, onSwipeRight)).attachToRecyclerView(list);
    }

    private static final class Callback extends ItemTouchHelper.SimpleCallback {

        private final Action onSwipeLeft;
        private final Action onSwipeRight;
        private final Paint paint = new Paint();
        private final Drawable deleteIcon;
        private final Drawable lobbyIcon;
        @ColorInt
        private final int deleteColor;
        @ColorInt
        private final int lobbyColor;

        Callback(@NonNull RecyclerView list,
                 int directions,
                 @Nullable Action onSwipeLeft,
                 @Nullable Action onSwipeRight) {
            super(0, directions);
            this.onSwipeLeft = onSwipeLeft;
            this.onSwipeRight = onSwipeRight;
            this.deleteColor = MaterialColors.getColor(
                    list, com.google.android.material.R.attr.colorErrorContainer);
            this.lobbyColor = MaterialColors.getColor(
                    list, com.google.android.material.R.attr.colorPrimaryContainer);
            this.deleteIcon = tinted(list, R.drawable.ic_delete, MaterialColors.getColor(
                    list, com.google.android.material.R.attr.colorOnErrorContainer));
            this.lobbyIcon = tinted(list, R.drawable.ic_add_to_lobby, MaterialColors.getColor(
                    list, com.google.android.material.R.attr.colorOnPrimaryContainer));
        }

        @Nullable
        private static Drawable tinted(@NonNull View view, @DrawableRes int id, @ColorInt int color) {
            Drawable icon = ContextCompat.getDrawable(view.getContext(), id);
            if (icon == null) {
                return null;
            }
            Drawable wrapped = DrawableCompat.wrap(icon.mutate());
            DrawableCompat.setTint(wrapped, color);
            return wrapped;
        }

        @Override
        public boolean onMove(@NonNull RecyclerView list,
                              @NonNull RecyclerView.ViewHolder holder,
                              @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder holder, int direction) {
            int position = holder.getBindingAdapterPosition();
            if (position == RecyclerView.NO_POSITION) {
                return;
            }
            if (direction == ItemTouchHelper.LEFT && onSwipeLeft != null) {
                onSwipeLeft.invoke(position);
            } else if (direction == ItemTouchHelper.RIGHT && onSwipeRight != null) {
                onSwipeRight.invoke(position);
            }
        }

        @Override
        public void onChildDraw(@NonNull Canvas canvas,
                                @NonNull RecyclerView list,
                                @NonNull RecyclerView.ViewHolder holder,
                                float dX,
                                float dY,
                                int actionState,
                                boolean isCurrentlyActive) {
            View row = holder.itemView;
            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX != 0f) {
                boolean toTheRight = dX > 0;
                paint.setColor(toTheRight ? lobbyColor : deleteColor);
                if (toTheRight) {
                    canvas.drawRect(row.getLeft(), row.getTop(),
                            row.getLeft() + dX, row.getBottom(), paint);
                } else {
                    canvas.drawRect(row.getRight() + dX, row.getTop(),
                            row.getRight(), row.getBottom(), paint);
                }
                draw(canvas, row, toTheRight ? lobbyIcon : deleteIcon, toTheRight, Math.abs(dX));
            }
            super.onChildDraw(canvas, list, holder, dX, dY, actionState, isCurrentlyActive);
        }

        private void draw(@NonNull Canvas canvas,
                          @NonNull View row,
                          @Nullable Drawable icon,
                          boolean toTheRight,
                          float swiped) {
            if (icon == null) {
                return;
            }
            int margin = (row.getHeight() - icon.getIntrinsicHeight()) / 2;
            if (margin < 0 || swiped < icon.getIntrinsicWidth() + margin) {
                return;
            }
            int top = row.getTop() + margin;
            int left = toTheRight
                    ? row.getLeft() + margin
                    : row.getRight() - margin - icon.getIntrinsicWidth();
            icon.setBounds(left, top,
                    left + icon.getIntrinsicWidth(), top + icon.getIntrinsicHeight());
            icon.draw(canvas);
        }
    }
}
