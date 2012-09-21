package org.granite.test.externalizers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.granite.config.GraniteConfig;
import org.granite.config.flex.ServicesConfig;
import org.granite.context.GraniteContext;
import org.granite.context.SimpleGraniteContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public abstract class AbstractJPAExternalizerTest {

	protected GraniteConfig graniteConfig;
	protected ServicesConfig servicesConfig;
	protected EntityManagerFactory entityManagerFactory;

	@Before
	public void before() throws Exception {
		Properties props = new Properties();
		props.put("javax.persistence.jdbc.driver", "org.h2.Driver");
		props.put("javax.persistence.jdbc.url", "jdbc:h2:mem:testdb");
		props.put("javax.persistence.jdbc.user", "sa");
		props.put("javax.persistence.jdbc.password", "");
		String provider = setProperties(props);
		entityManagerFactory = Persistence.createEntityManagerFactory(provider + "-pu", props);
		
		InputStream is = getClass().getClassLoader().getResourceAsStream("WEB-INF/granite/granite-config-" + provider + ".xml");
		graniteConfig = new GraniteConfig(null, is, null, null);
		servicesConfig = new ServicesConfig(null, null, false);
	}
	
	protected abstract String setProperties(Properties props);
	
	
	
	@Test
	public void testSerializationLazy() throws Exception {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		EntityTransaction et = entityManager.getTransaction();
		et.begin();
		
		Entity5 e5 = new Entity5();
		e5.setName("Test");
		e5.setEntities(new HashSet<Entity6>());
		Entity6 e6 = new Entity6();
		e6.setName("Test");
		e5.getEntities().add(e6);
		e6.setEntity5(e5);
		entityManager.persist(e5);
		
		entityManager.flush();
		et.commit();
		entityManager.close();
		
		entityManager = entityManagerFactory.createEntityManager();		
		et = entityManager.getTransaction();
		et.begin();
		
		e5 = entityManager.find(Entity5.class, e5.getId());
		
		entityManager.flush();
		et.commit();
		entityManager.close();
		
		GraniteContext gc = SimpleGraniteContext.createThreadInstance(graniteConfig, servicesConfig, new HashMap<String, Object>());
		ByteArrayOutputStream baos = new ByteArrayOutputStream(20000);
		ObjectOutput out = gc.getGraniteConfig().newAMF3Serializer(baos);
		out.writeObject(e5);		
		GraniteContext.release();
		
		InputStream is = new ByteArrayInputStream(baos.toByteArray());
		gc = SimpleGraniteContext.createThreadInstance(graniteConfig, servicesConfig, new HashMap<String, Object>());
		ObjectInput in = gc.getGraniteConfig().newAMF3Deserializer(is);
		Object obj = in.readObject();
		GraniteContext.release();
		
		Assert.assertTrue("Entity5", obj instanceof Entity5);
	}
	
	@Test
	public void testSerializationLazyEmbeddedGDS838() throws Exception {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		EntityTransaction et = entityManager.getTransaction();
		et.begin();
		
		Entity2 e2 = new Entity2();
		e2.setName("Test");
		Entity3 e3 = new Entity3();
		e3.setEname("Tutu");
		e3.setEntities(new HashSet<Entity4>());
		e2.setEntity(e3);
		Entity4 e4 = new Entity4();
		e4.setName("Test");
		e3.getEntities().add(e4);
		e4.setEntity2(e2);
		entityManager.persist(e2);
		
		entityManager.flush();
		et.commit();
		entityManager.close();
		
		entityManager = entityManagerFactory.createEntityManager();		
		et = entityManager.getTransaction();
		et.begin();
		
		e2 = entityManager.find(Entity2.class, e2.getId());
		
		entityManager.flush();
		et.commit();
		entityManager.close();
		
		GraniteContext gc = SimpleGraniteContext.createThreadInstance(graniteConfig, servicesConfig, new HashMap<String, Object>());
		ByteArrayOutputStream baos = new ByteArrayOutputStream(20000);
		ObjectOutput out = gc.getGraniteConfig().newAMF3Serializer(baos);
		out.writeObject(e2);		
		GraniteContext.release();
		
		InputStream is = new ByteArrayInputStream(baos.toByteArray());
		gc = SimpleGraniteContext.createThreadInstance(graniteConfig, servicesConfig, new HashMap<String, Object>());
		ObjectInput in = gc.getGraniteConfig().newAMF3Deserializer(is);
		Object obj = in.readObject();
		GraniteContext.release();
		
		Assert.assertTrue("Entity2", obj instanceof Entity2);
		if (gc.getGraniteConfig().getClassGetter().getClass().getName().indexOf("hibernate") >= 0)
		    Assert.assertFalse("Entity 2 entities not loaded", gc.getGraniteConfig().getClassGetter().isInitialized(((Entity2)obj).getEntity(), "entities", ((Entity2)obj).getEntity().getEntities()));
	}
	
	@Test
	public void testSerializationEmbeddedDoubleAssociation() throws Exception {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		EntityTransaction et = entityManager.getTransaction();
		et.begin();
		
		EntityA a = new EntityA();
		a.setUid(1L);
		a.setEntities(new HashSet<EntityB>());
		EmbeddedEntity e = new EmbeddedEntity();
		e.setEntities(new HashSet<EntityB>());
		a.setEmbedded(e);
		EntityB b = new EntityB();
		b.setEntity(a);
		b.setName("Bla");
		b.setUid(2L);
		a.getEntities().add(b);
		e.getEntities().add(b);
		entityManager.persist(a);
		
		entityManager.flush();
		et.commit();
		entityManager.close();
		
		entityManager = entityManagerFactory.createEntityManager();		
		et = entityManager.getTransaction();
		et.begin();
		
		a = entityManager.find(EntityA.class, a.getId());
		
		entityManager.flush();
		et.commit();
		entityManager.close();
		
		GraniteContext gc = SimpleGraniteContext.createThreadInstance(graniteConfig, servicesConfig, new HashMap<String, Object>());
		ByteArrayOutputStream baos = new ByteArrayOutputStream(20000);
		ObjectOutput out = gc.getGraniteConfig().newAMF3Serializer(baos);
		out.writeObject(a);		
		GraniteContext.release();
		
		InputStream is = new ByteArrayInputStream(baos.toByteArray());
		gc = SimpleGraniteContext.createThreadInstance(graniteConfig, servicesConfig, new HashMap<String, Object>());
		ObjectInput in = gc.getGraniteConfig().newAMF3Deserializer(is);
		Object obj = in.readObject();
		GraniteContext.release();
		
		Assert.assertTrue("EntityA", obj instanceof EntityA);
	}
    
    @Test
    public void testSerializationUnmanagedEntity() throws Exception {
        Entity6 e6 = new Entity6();
        e6.setName("Test");
        
        GraniteContext gc = SimpleGraniteContext.createThreadInstance(graniteConfig, servicesConfig, new HashMap<String, Object>());
        ByteArrayOutputStream baos = new ByteArrayOutputStream(20000);
        ObjectOutput out = gc.getGraniteConfig().newAMF3Serializer(baos);
        out.writeObject(e6);        
        GraniteContext.release();
        
        InputStream is = new ByteArrayInputStream(baos.toByteArray());
        gc = SimpleGraniteContext.createThreadInstance(graniteConfig, servicesConfig, new HashMap<String, Object>());
        ObjectInput in = gc.getGraniteConfig().newAMF3Deserializer(is);
        Object obj = in.readObject();
        GraniteContext.release();
        
        Assert.assertTrue("Entity6", obj instanceof Entity6);
    }
    
    @Test
    public void testSerializationProxy() throws Exception {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction et = entityManager.getTransaction();
        et.begin();
        
        Entity8 e8 = new Entity8();
        e8.setName("Test");
        Entity7 e7 = new Entity7();
        e7.setName("Test");
        e7.setEntity8(e8);
        entityManager.persist(e7);
        
        entityManager.flush();
        et.commit();
        entityManager.close();
        
        entityManager = entityManagerFactory.createEntityManager();     
        et = entityManager.getTransaction();
        et.begin();
        
        e7 = entityManager.find(Entity7.class, e7.getId());
        
        entityManager.flush();
        et.commit();
        entityManager.close();
        
        GraniteContext gc = SimpleGraniteContext.createThreadInstance(graniteConfig, servicesConfig, new HashMap<String, Object>());
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream(20000);
        ObjectOutput out = gc.getGraniteConfig().newAMF3Serializer(baos);
        
        out.writeObject(e7);        
        GraniteContext.release();
        
        InputStream is = new ByteArrayInputStream(baos.toByteArray());
        gc = SimpleGraniteContext.createThreadInstance(graniteConfig, servicesConfig, new HashMap<String, Object>());
        ObjectInput in = gc.getGraniteConfig().newAMF3Deserializer(is);
        Object obj = in.readObject();
        GraniteContext.release();
        
        Assert.assertTrue("Entity7", obj instanceof Entity7);
        Assert.assertFalse("Entity8 not loaded", gc.getGraniteConfig().getClassGetter().isInitialized(obj, "entity8", ((Entity7)obj).getEntity8()));
    }
    
    @Test
    public void testSerializationProxy2() throws Exception {
        GraniteContext gc = SimpleGraniteContext.createThreadInstance(graniteConfig, servicesConfig, new HashMap<String, Object>());

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction et = entityManager.getTransaction();
        et.begin();
        
        Entity8 e8 = new Entity8();
        e8.setName("Test");
        Entity7 e7 = new Entity7();
        e7.setName("Test");
        e7.setEntity8(e8);
        entityManager.persist(e7);
        
        entityManager.flush();
        et.commit();
        entityManager.close();
        
        entityManager = entityManagerFactory.createEntityManager();     
        et = entityManager.getTransaction();
        et.begin();
        
        e7 = entityManager.find(Entity7.class, e7.getId());
        
        gc.getGraniteConfig().getClassGetter().initialize(e7, "entity8", e7.getEntity8());
        
        entityManager.flush();
        et.commit();
        entityManager.close();
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream(20000);
        ObjectOutput out = gc.getGraniteConfig().newAMF3Serializer(baos);
        out.writeObject(e7);        
        GraniteContext.release();
        
        InputStream is = new ByteArrayInputStream(baos.toByteArray());
        gc = SimpleGraniteContext.createThreadInstance(graniteConfig, servicesConfig, new HashMap<String, Object>());
        ObjectInput in = gc.getGraniteConfig().newAMF3Deserializer(is);
        Object obj = in.readObject();
        GraniteContext.release();
        
        Assert.assertTrue("Entity7", obj instanceof Entity7);
        Assert.assertTrue("Entity8 loaded", gc.getGraniteConfig().getClassGetter().isInitialized(obj, "entity8", ((Entity7)obj).getEntity8()));
    }
}
