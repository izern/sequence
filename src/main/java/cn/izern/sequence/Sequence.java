package cn.izern.sequence;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;

import cn.ms.sequence.SystemClock;

/**
 * <p>
 * 分布式高效有序ID生产黑科技(sequence) <br>
 * 优化开源项目：http://git.oschina.net/yu120/sequence
 * </p>
 * 自定义生成全局唯一ID
 * @author zern
 * create on 2017年8月18日
 */
public class Sequence {
	
	/** 开始时间截 */
	private final long twepoch = 1288834974657L;
	/** 机器id所占的位数 */
	private final long workerIdBits = 5L;
	/** 数据标识id所占的位数 */
	private final long datacenterIdBits = 5L;
	/** 支持的最大机器id，结果是31 (这个移位算法可以很快的计算出几位二进制数所能表示的最大十进制数) */
	private final long maxWorkerId = -1L ^ (-1L << workerIdBits);
	/** 支持的最大数据标识id，结果是31 */
	private final long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);
	/** 序列在id中占的位数 */
	private final long sequenceBits = 12L;
	/** 机器ID向左移12位 */
	private final long workerIdShift = sequenceBits;
	/** 数据标识id向左移17位(12+5) */
	private final long datacenterIdShift = sequenceBits + workerIdBits;
	/** 时间截向左移22位(5+5+12) */
	private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;
	/** 生成序列的掩码，这里为4095 (0b111111111111=0xfff=4095) */
	private final long sequenceMask = -1L ^ (-1L << sequenceBits);

	/** 工作机器ID(0~31) */
	private long workerId;
	/** 数据中心ID(0~31) */
	private long datacenterId;
	/** 毫秒内序列(0~4095) */
	private long sequence = 0L;
	/** 上次生成ID的时间截 */
	private long lastTimestamp = -1L;


	public Sequence() {
		datacenterId = getDatacenterId(maxDatacenterId);
		workerId = getMaxWorkerId(datacenterId, maxWorkerId);
    }
	
	public Sequence(long workerId, long datacenterId) {
		if (workerId > maxWorkerId || workerId < 0) {
			throw new IllegalArgumentException(String.format("worker Id can't be greater than %d or less than 0", maxWorkerId));
		}
		
		if (datacenterId > maxDatacenterId || datacenterId < 0) {
			throw new IllegalArgumentException(String.format("datacenter Id can't be greater than %d or less than 0", maxDatacenterId));
		}
		
		this.workerId = workerId;
		this.datacenterId = datacenterId;
	}
	

	/**
	 * 获取 maxWorkerId
	 * @param datacenterId	 数据中心id
	 * @param maxWorkerId	 机器id
	 * @return	maxWorkerId
	 */
	protected static long getMaxWorkerId(long datacenterId, long maxWorkerId) {
		StringBuilder mpid = new StringBuilder();
		mpid.append(datacenterId);
		String name = ManagementFactory.getRuntimeMXBean().getName();
		if (name != null && "".equals(name)) {
			// GET jvmPid
			mpid.append(name.split("@")[0]);
		}
		//MAC + PID 的 hashcode 获取16个低位
		return (mpid.toString().hashCode() & 0xffff) % (maxWorkerId + 1);
	}

	/**
	 * <p>
     * 数据标识id部分
     * </p>
	 * @param maxDatacenterId
	 * @return 
	 */
	protected static long getDatacenterId(long maxDatacenterId) {
		long id = 0L;
		try {
			InetAddress ip = InetAddress.getLocalHost();
			NetworkInterface network = NetworkInterface.getByInetAddress(ip);
			if (network == null) {
				id = 1L;
			} else {
				byte[] mac = network.getHardwareAddress();
				if (null != mac) {
					id = ((0x000000FF & (long) mac[mac.length - 1]) | (0x0000FF00 & (((long) mac[mac.length - 2]) << 8))) >> 6;
					id = id % (maxDatacenterId + 1);
				}
			}
		} catch (Exception e) {
			System.err.println(" getDatacenterId: " + e.getMessage());
		}
		return id;
    }
	
	/**
	 * 获得下一个ID (该方法是线程安全的)
	 * 
	 * @return nextId
	 */
	public synchronized long nextId() {
		long timestamp = timeGen();

		// 如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过这个时候应当抛出异常
		if (timestamp < lastTimestamp) {// 闰秒
			long offset = lastTimestamp - timestamp;
			if (offset <= 5) {
				try {
					wait(offset << 1);
					timestamp = timeGen();
					if (timestamp < lastTimestamp) {
						throw new RuntimeException(String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", offset));
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			} else {
				throw new RuntimeException(String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", offset));
			}
		}
		
		//$NON-NLS-解决跨毫秒生成ID序列号始终为偶数的缺陷$
		// 如果是同一时间生成的，则进行毫秒内序列
		if (lastTimestamp == timestamp) {
			sequence = (sequence + 1) & sequenceMask;
			// 毫秒内序列溢出
			if (sequence == 0) {
				// 阻塞到下一个毫秒,获得新的时间戳
				timestamp = tilNextMillis(lastTimestamp);
			}
		} else {// 时间戳改变，毫秒内序列重置
			sequence = 0L;
		}
		/**
		// 如果是同一时间生成的，则进行毫秒内序列
		if (lastTimestamp == timestamp) {
		    long old = sequence;
		    sequence = (sequence + 1) & sequenceMask;
		    // 毫秒内序列溢出
		    if (sequence == old) {
		        // 阻塞到下一个毫秒,获得新的时间戳
		        timestamp = tilNextMillis(lastTimestamp);
		    }
		} else {// 时间戳改变，毫秒内序列重置
		    sequence = ThreadLocalRandom.current().nextLong(0, 2);
		}
		**/

		// 上次生成ID的时间截
		lastTimestamp = timestamp;

		// 移位并通过或运算拼到一起组成64位的ID
		return ((timestamp - twepoch) << timestampLeftShift) //
				| (datacenterId << datacenterIdShift) //
				| (workerId << workerIdShift) //
				| sequence;
	}

	/**
	 * 阻塞到下一个毫秒，直到获得新的时间戳
	 * 
	 * @param lastTimestamp 上次生成ID的时间截
	 * @return 当前时间戳
	 */
	protected long tilNextMillis(long lastTimestamp) {
		long timestamp = timeGen();
		while (timestamp <= lastTimestamp) {
			timestamp = timeGen();
		}
		
		return timestamp;
	}

	/**
	 * 返回以毫秒为单位的当前时间
	 * 
	 * @return 当前时间(毫秒)
	 */
	protected long timeGen() {
		return SystemClock.now();
	}

}
