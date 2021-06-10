package test

import io.atomix.protocols.raft.MultiRaftProtocol
import io.atomix.protocols.raft.session.CommunicationStrategy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import java.time.Duration

suspend fun CoroutineScope.leaderWorker() {
    var lastReportedTs = System.currentTimeMillis()
    var lastRepCounter = 0L
    while (isActive) {
        try {
//            println("Locking")
            val lock = atomix.lockBuilder("leader1")
//                .withProtocol(DistributedLogProtocol.instance())
                .withProtocol(
                    MultiRaftProtocol.builder()
                        .withCommunicationStrategy(CommunicationStrategy.LEADER)
                        .build()
                )
                .build()
//            val lock = atomix.atomicLockBuilder("leader2").build()
            val counter = atomix.atomicCounterBuilder("leader1cnt").build()
            while (isActive) {
                try {

                    val success = lock.tryLock(Duration.ofSeconds(10))
                    if (!success) {
                        println("Lock failed")
                        lock.unlock()
                    } else {
                        val oldVal = counter.get()
                        if (!counter.compareAndSet(oldVal, oldVal + 1)) {
                            println("!!LOCK BROKEN!!!")
                        }
                        if (System.currentTimeMillis() - lastReportedTs > 10000) {
                            lastReportedTs = System.currentTimeMillis()
                            println("TP: ${(oldVal - lastRepCounter + 0.0) / 10}")
                            lastRepCounter = oldVal
                        }
                    }
                } finally {
                    lock.unlock()
                }
            }
        } catch (e: Exception) {
            println("Acquire error $e")
        }
    }
}