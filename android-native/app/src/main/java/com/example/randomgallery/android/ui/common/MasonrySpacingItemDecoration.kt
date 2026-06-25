package com.example.randomgallery.android.ui.common

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

class MasonrySpacingItemDecoration(
    private val spanCount: Int,
    private val spacingPx: Int
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION) return

        val layoutParams = view.layoutParams as? StaggeredGridLayoutManager.LayoutParams
        if (layoutParams == null) {
            outRect.top = if (position == 0) 0 else spacingPx
            return
        }

        val spanIndex = layoutParams.spanIndex
        val halfSpacing = spacingPx / 2

        outRect.left = if (spanIndex == 0) 0 else halfSpacing
        outRect.right = if (spanIndex == spanCount - 1) 0 else halfSpacing
        outRect.top = if (position < spanCount) 0 else spacingPx
    }
}
