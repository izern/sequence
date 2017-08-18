package cn.izern.sequence;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import cn.izern.sequence.Sequence;

public class RepeatedTest {

	/**
	 * 重复性测试
	 */
	@Test
	public void testRepeated() {
		Set<Long> set = new HashSet<Long>();
		int maxTimes = 1000000 * 10;
		Sequence sequence = new Sequence(0, 0);
		for (int i = 0; i < maxTimes; i++) {
			set.add(sequence.nextId());
		}
		System.out.println(maxTimes == set.size());
//		Assert.assertEquals(maxTimes, set.size());
	}

}
