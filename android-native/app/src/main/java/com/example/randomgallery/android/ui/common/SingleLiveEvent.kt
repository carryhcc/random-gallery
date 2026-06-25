package com.example.randomgallery.android.ui.common

import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 只投递一次的事件型 LiveData。
 *
 * 普通 LiveData 在配置变化或重新订阅（如从返回栈回到页面）时会把最后一个值再次投递给新的 observer，
 * 用来承载“导航 / 弹 Snackbar / 删除后返回”等一次性事件时会被重复触发。
 * SingleLiveEvent 保证一个值只会被消费一次。
 */
class SingleLiveEvent<T> : MutableLiveData<T>() {

    private val pending = AtomicBoolean(false)

    @MainThread
    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        super.observe(owner) { value ->
            if (pending.compareAndSet(true, false)) {
                observer.onChanged(value)
            }
        }
    }

    @MainThread
    override fun setValue(value: T?) {
        pending.set(true)
        super.setValue(value)
    }

    @MainThread
    fun call(value: T) {
        this.value = value
    }
}
