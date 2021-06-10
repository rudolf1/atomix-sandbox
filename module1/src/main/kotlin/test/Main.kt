package test

import io.atomix.cluster.Node
import io.atomix.cluster.discovery.BootstrapDiscoveryProvider
import io.atomix.core.Atomix
import io.atomix.protocols.raft.partition.RaftPartitionGroup
import io.atomix.storage.StorageLevel
import io.atomix.utils.net.Address
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import java.io.File

const val host = "127.0.0.1"
val instances = mapOf(
    "member0" to Address.from(host, 8080),
    "member1" to Address.from(host, 8081),
    "member2" to Address.from(host, 8082),
//    "member3" to Address.from(host, 8083),
//    "member4" to Address.from(host, 8084),
)

val instanceNumber: Int = System.getProperty("instance")!!.toInt()
val instance: String = "member$instanceNumber"

val instanceDataFolder = File(".", ".data/$instance")
var atomix: Atomix = Atomix.builder()
    .withMemberId(instance)
    .withAddress(instances[instance]!!)
    .withMembershipProvider(
        BootstrapDiscoveryProvider.builder()
            .withNodes(
                instances.map {
                    Node.builder()
                        .withId(it.key)
                        .withAddress(it.value)
                        .build()
                }
            )
            .build()
    )
    .withManagementGroup(
        RaftPartitionGroup.builder("system")
            .withNumPartitions(1)
            .withStorageLevel(StorageLevel.MAPPED)
            .withDataDirectory(File(instanceDataFolder, "system"))
            .withMembers(instances.keys)
            .build()
    )
    .withPartitionGroups(
        RaftPartitionGroup.builder("raft")
            .withStorageLevel(StorageLevel.MAPPED)
            .withDataDirectory(File(instanceDataFolder, "raft"))
            .withPartitionSize(1)
            .withNumPartitions(1)
            .withMembers(instances.keys)
            .build()
    )
    .build()

fun main(): Unit = runBlocking {
//    println("Cleanup data")
//    instanceDataFolder.listFiles().orEmpty().forEach { it.deleteRecursively() }
    println("Starting...")
    atomix.start().await()
    println("Cluster joined")
    atomix.membershipService
        .addListener {
            atomix
                .membershipService
                .members.forEach {
                    println("M: ${it.id()}: A:${it.isActive} R:${it.isReachable}")
                }
        }
    leaderWorker()
//    pessimisticIntCounter()
}

