# Sync

1. 简介
> Sync是一款分布式场景下基于Redis的安全高效的线程同步组件，提供分布式可重入互斥锁、分布式可重入读写锁、分布式信号量。提供相应注解，使用简单，可与spring-boot无缝集成。

> 本组件已经上传到[maven中央库](https://search.maven.org/search?q=org.antframework.sync)

2. 环境要求
> * JDK1.8及以上

3. 技术支持

> 欢迎加我微信（zhong_xun_）入群交流。<br/>
<img src="https://note.youdao.com/yws/api/personal/file/WEB6b849e698db2a635b43eba5bc949ce1c?method=download&shareKey=27623320b5ca82cbf768b61130c81de0" width=150 />

## 1. 将Sync引入进你的系统
引入Sync很简单，按照以下操作即可。

### 1.1 引入Maven依赖
Sync支持SpringBoot v2.x，也支持SpringBoot v1.x
```xml
<dependency>
    <groupId>org.antframework.sync</groupId>
    <artifactId>sync</artifactId>
    <version>1.1.0</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
    <version>${spring-boot版本}</version>
</dependency>
```

### 1.2 配置
在application.properties或application.yaml中配置Redis和Sync
```properties
# 必填：配置Redis地址
spring.redis.host=127.0.0.1
spring.redis.port=6379

# 必填：命名空间（也可以通过ant.sync.namespace配置）
spring.application.name=customer    #这里使用customer（会员系统）作为举例

# 以下配置均是选填配置，使用方一般使用默认配置即可，无需自定义配置
# 选填：是否启用Sync（true为启用，false为不启用；默认启用）
ant.sync.enable=true
# 选填：等待同步消息的最长时间（毫秒，默认为10秒）
ant.sync.max-wait-time=10000
# 选填：服务端类型（默认为redis）（sync还提供local模式，可以不依赖Redis，这种模式只对单个应用实例起效果，无法用在分布式场景）
ant.sync.server-type=redis
# 选填：发生异常时Redis中数据的存活时长（毫秒，默认为10分钟）
ant.sync.redis.live-time=600000
# 选填：@Semaphore注解使用的许可总数，通过ant.sync.semaphore.key-total-permits.${key}=${许可总数}的形式对key的许可总数进行配置
ant.sync.semaphore.key-total-permits.trade-123=100
ant.sync.semaphore.key-total-permits.trade-456=200
# 选填：@Lock、@ReadLock、@WriteLock、@Semaphore的AOP执行的优先级（默认为Ordered.LOWEST_PRECEDENCE - 10，默认比@Transactional先执行）
ant.sync.aop-order=2147483637
```

## 2. 使用sync
提供两种使用sync方式：
- 通过SyncContext使用
- 通过注解使用

### 2.1 通过SyncContext使用
使用sync前需先获取SyncContext：
```java
@Autowired
private SyncContext syncContext;
```

#### 2.1.1 分布式可重入互斥锁
```java
// 传入锁的标识（比如：trade-123）就可获取对应的锁
Lock lock= syncContext.getLockContext().getLock("trade-123");
lock.lock();        // 加锁
try{
    // TODO 具体业务逻辑
}finally {
    lock.unlock();  // 解锁
}
```

#### 2.1.2 分布式可重入读写锁
- 读锁
```java
// 传入锁的标识（比如：trade-123）就可获取对应的锁
ReadWriteLock rwLock=syncContext.getLockContext().getRWLock("trade-123");
rwLock.readLock().lock();       // 加读锁
try {
    // TODO 具体业务逻辑
}finally {
    rwLock.readLock().unlock(); // 解读锁
}
```
- 写锁
```java
// 传入锁的标识（比如：trade-123）就可获取对应的锁
ReadWriteLock rwLock=syncContext.getLockContext().getRWLock("trade-123");
rwLock.writeLock().lock();       // 加写锁
try {
    // TODO 具体业务逻辑
}finally {
    rwLock.writeLock().unlock(); // 解写锁
}
```

#### 2.1.3 分布式信号量
```java
// 传入信号量的标识（比如：trade-abc）就可获取对应的信号量，同时需指定分布式环境下总的可用许可数（比如：100）
Semaphore semaphore=syncContext.getSemaphoreContext().getSemaphore("trade-123", 100);
semaphore.acquire(5);       // 获取5个许可
try {
    // TODO 具体业务逻辑
}finally {
    semaphore.release(5);   // 释放5个许可
}
```

### 2.2 通过注解使用

#### 2.2.1 分布式可重入互斥锁
```java
@org.springframework.stereotype.Service
public class TradeService {
    @Lock(key = "#tradeId")     // key是锁的标识。进入方法前加锁，退出方法后解锁
    public Trade createTrade(String tradeId){
        // TODO 具体业务逻辑
    }
}
```

#### 2.2.2 分布式可重入读写锁
- 读锁
```java
@org.springframework.stereotype.Service
public class TradeService {
    @ReadLock(key = "#tradeId")     // key是锁的标识。进入方法前加锁，退出方法后解锁
    public Trade findTrade(String tradeId){
        // TODO 具体业务逻辑
    }
}
```
- 写锁
```java
@org.springframework.stereotype.Service
public class TradeService {
    @WriteLock(key = "#trade.tradeId")  // key是锁的标识。进入方法前加锁，退出方法后解锁
    public void updateTrade(Trade trade){
        // TODO 具体业务逻辑
    }
}
```

#### 2.2.3 分布式信号量
```java
@org.springframework.stereotype.Service
public class TradeService {
    // 总许可数通过ant.sync.semaphore.key-total-permits.trade-abc=100配置（这里指定分布式环境下trade-abc的总许可数为100）
    @Semaphore(key = "#name", permits = 5)  //  key是信号量的标识。进入方法前获取5个许可，退出方法后释放5个许可
    public void doBiz(String name){
        // TODO 具体业务逻辑
    }
}
```

## 3. 扩展性
Sync本身提供基于Redis和Local两种实现，同时具备灵活的扩展性。如果你想基于Zookeeper来实现，则可以实现org.antframework.sync.extension.Server接口；如果你想基于Redis实现，但又不想使用spring-data-redis提供的org.springframework.data.redis.connection.RedisConnectionFactory，则可以实现org.antframework.sync.extension.redis.extension.RedisExecutor接口。

扩展时可以参考：org.antframework.sync.extension.redis.RedisServer、org.antframework.sync.extension.local.LocalServer、org.antframework.sync.extension.redis.extension.springdataredis.SpringDataRedisExecutor
