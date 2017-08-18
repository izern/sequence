package cn.izern.hibernate.id;

import java.io.Serializable;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.Configurable;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

import cn.izern.sequence.Sequence;

/**
 * ID生成器,分布式Long型唯一ID,大小序列
 * @author zern
 * create on 2017年8月18日
 */
public class IDSequenceGenerator implements Configurable, IdentifierGenerator{

	private Sequence sequence = new Sequence();
	
	@Override
	public Serializable generate(SessionImplementor arg0, Object arg1) throws HibernateException {
		return sequence.nextId();
	}

	@Override
	public void configure(Type arg0, Properties arg1, ServiceRegistry arg2) throws MappingException {
		// TODO Auto-generated method stub
		
	}

}
