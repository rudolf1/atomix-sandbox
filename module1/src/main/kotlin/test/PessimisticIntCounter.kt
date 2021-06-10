package test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

fun CoroutineScope.pessimisticIntCounter() {
    val queue = atomix.atomicValueBuilder<Pair<String, Long>>("int")
        .build()
    queue.addStateChangeListener {
        println("State change $it")
    }
    var lastReported = System.currentTimeMillis()
    var throughput = 0
    queue.addListener {
        if (System.currentTimeMillis() - lastReported > 1000) {
            lastReported = System.currentTimeMillis()
            println("Actual value ${it.newValue()}")
            println("Actual TP: $throughput")
            throughput = 0
        }
        throughput++
    }
    launch {
        while (isActive) {
            try {
                val oldValue = queue.get()
                val newValue = instance to ((oldValue?.second ?: 0L) + instanceNumber + 1)

                try {
                    queue.compareAndSet(oldValue, newValue)
//                        println("Pessimistic lock failed $newValue")
                } catch (e: Exception) {
                    println("Failed to change $newValue")
                }
            } catch (e: Exception) {
                println("Failed to get data $e")
                delay(1000)
            }
//            delay(Random.nextLong(10, 30))
        }
    }
}