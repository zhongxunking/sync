# sync
1. 简介

> sync提供分布式场景下基于redis的一些同步机制，目前已提供：分布式可重入互斥锁、分布式可重入读写锁、分布式信号量。提供相应注解，使用简单，可与spring-boot无缝集成。

> 本框架已经上传到[maven中央库](https://search.maven.org/#search%7Cga%7C1%7Corg.antframework.sync)

2. 环境要求

> * jdk1.8

3. 技术支持

> 欢迎加我微信（zhong_xun_）入群交流。<br/>
<img src="https://note.youdao.com/yws/api/personal/file/WEBbca9e0a9a6e1ea2d9ab9def1cc90f839?method=download&shareKey=00e90849ae0d3b5cb8ed7dd12bc6842e" width=150 />


## 1. 将sync引入进你的系统
引入sync很简单，按照以下操作即可。

### 1.1 引入依赖
```xml
<dependency>
    <groupId>org.antframework.sync</groupId>
    <artifactId>sync</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 1.2 配置
sync提供对spring-boot和非spring-boot项目两种配置方式。

### 1.2.1 spring-boot项目
如果你的应用是spring-boot项目，则按照以下进行配置。

- 引入spring-boot-starter-data-redis
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

- 配置redis和sync
```properties
# 必填：配置redis地址
spring.redis.host=127.0.0.1
spring.redis.port=6379

# 选填：服务端类型（默认为redis）（sync还提供local模式，可以不依赖redis，这种模式只对单个应用实例起效果，无法用在分布式场景）
ant.sync.server-type=redis
# 选填：发生异常时redis中数据的存活时长（毫秒，默认为10分钟）
ant.sync.redis.live-time=600000
# 选填：@Lock、@ReadLock、@WriteLock、@Semaphore切面执行的优先级（默认比@Transactional先执行）
ant.sync.aop-order=2147483637
# 选填：@Semaphore注解使用的许可总数，通过ant.sync.semaphore.key-total-permits.前缀可分别对每个信号量进行配置
ant.sync.semaphore.key-total-permits.trade-abc=100
ant.sync.semaphore.key-total-permits.trade-edf=200
```

### 1.2.2 非spring-boot项目
如果你的应用是非是spring-boot项目，则按照以下进行配置。
```java
// 初始化SyncContext（redisConnectionFactory是org.springframework.data.redis.connection.RedisConnectionFactory的一个是实例）
SyncContext syncContext = new SyncContext(new RedisServer(new SpringDataRedisExecutor(redisConnectionFactory), 10 * 60 * 1000), 10 * 1000);
```
如果非spring-boot项目想使用注解@Lock、@ReadLock、@WriteLock、@Semaphore，则还需要配置org.antframework.sync.lock.annotation.support.LockAop和org.antframework.sync.semaphore.annotation.support.SemaphoreAop，具体配置方式请参考org.antframework.sync.boot.SyncAutoConfiguration。

## 2. 使用sync
提供两种使用sync方式：
- 通过SyncContext使用
- 通过注解使用

### 2.1 通过SyncContext使用
使用sync前需先获取SyncContext。
- spring-boot项目获取SyncContext：
```java
@Autowired
private SyncContext syncContext;
```
- 非spring-boot项目直接使用自己初始化的SyncContext

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
Semaphore semaphore=syncContext.getSemaphoreContext().getSemaphore("trade-abc", 100);
semaphore.acquire(5);       // 获取5个许可
try {
    // TODO 具体业务逻辑
}finally {
    semaphore.release(5);   // 释放5个许可
}
```

### 2.2 通过注解使用
- 如果是spring-boot项目，则可以直接使用注解，无需额外配置
- 如果是非spring-boot项目，则需要配置org.antframework.sync.lock.annotation.support.LockAop和org.antframework.sync.semaphore.annotation.support.SemaphoreAop，具体配置方式请参考org.antframework.sync.boot.SyncAutoConfiguration

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
sync本身提供基于redis和local两种实现，同时具备灵活的扩展性。如果你想基于zookeeper来实现，则可以实现org.antframework.sync.extension.Server接口；如果你想基于redis实现，但又不想使用spring-data-redis提供的org.springframework.data.redis.connection.RedisConnectionFactory，则可以实现org.antframework.sync.extension.redis.extension.RedisExecutor接口。

扩展时可以参考：org.antframework.sync.extension.redis.RedisServer、org.antframework.sync.extension.local.LocalServer、org.antframework.sync.extension.redis.extension.springdataredis.SpringDataRedisExecutor
