/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.moez.QKSMS.common.widget

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.viewpager.widget.ViewPager
import com.moez.QKSMS.R
import com.moez.QKSMS.common.util.Colors
import com.moez.QKSMS.common.util.extensions.forEach
import com.moez.QKSMS.common.util.extensions.resolveThemeColor
import com.moez.QKSMS.injection.appComponent
import com.uber.autodispose.android.ViewScopeProvider
import com.uber.autodispose.kotlin.autoDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import kotlinx.android.synthetic.main.tab_view.view.*
import javax.inject.Inject

class PagerTitleView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {

    @Inject lateinit var colors: Colors

    private val threadId: Subject<Long> = BehaviorSubject.create()

    var pager: ViewPager? = null
        set(value) {
            if (field !== value) {
                field = value
                recreate()
            }
        }

    init {
        if (!isInEditMode) appComponent.inject(this)
    }

    fun setThreadId(id: Long) {
        threadId.onNext(id)
    }

    private fun recreate() {
        removeAllViews()


        //Tomas Venegas: Iterators are a bad performance practice as is declaring variables in loops
        val view: View = LayoutInflater.from(context).inflate(R.layout.tab_view, this, false)
        for(i in 0 until pager?.adapter?.count!!){
            view.label.text = pager?.adapter?.getPageTitle(i)
            view.setOnClickListener { pager?.currentItem = i }
            addView(view)
        }

        for( i in 0 until childCount){
            getChildAt(i).isActivated = i == pager?.currentItem
        }

        pager?.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                for( i in 0 until childCount){
                    getChildAt(i).isActivated = i == position
                }
            }
        })
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        val states = arrayOf(
                intArrayOf(android.R.attr.state_activated),
                intArrayOf(-android.R.attr.state_activated))

        threadId
                .distinctUntilChanged()
                .switchMap { threadId -> colors.themeObservable(threadId) }
                .map { theme ->
                    val textSecondary = context.resolveThemeColor(android.R.attr.textColorSecondary)
                    ColorStateList(states, intArrayOf(theme.theme, textSecondary))
                }
                .autoDisposable(ViewScopeProvider.from(this))
                .subscribe { colorStateList ->
                    for( i in 0 until childCount){
                        (getChildAt(i) as? TextView)?.setTextColor(colorStateList)
                    }
                }
    }

}
