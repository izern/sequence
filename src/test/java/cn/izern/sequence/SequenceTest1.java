package cn.izern.sequence;

import org.junit.Test;

import cn.izern.sequence.Sequence;

public class SequenceTest1 {

	@Test
	public void name() {
		try {
			int times = 0, maxTimes = 1000;
			Sequence sequence = new Sequence(0, 0);
			for (int i = 0; i < maxTimes; i++) {
				long id = sequence.nextId();
				if(id%2==0){
					times++;
				}
				Thread.sleep(5);
			}
			System.out.println("偶数:" + times + ",奇数:" + (maxTimes - times) + "!");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
