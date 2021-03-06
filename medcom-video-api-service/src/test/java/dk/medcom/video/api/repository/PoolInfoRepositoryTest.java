package dk.medcom.video.api.repository;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.junit.Assert;
import org.junit.Test;

import dk.medcom.video.api.entity.PoolInfoEntity;

public class PoolInfoRepositoryTest extends RepositoryTest {

	@Resource
    private PoolInfoRepository subject;

	@Test
	public void testSchedulingInfo() {
		// Given
		final String POOL_TEST_ORG = "pool-test-org";
		final String POOL_TEST_ORG2 = "pool-test-org2";
		
		// When
		List<PoolInfoEntity> poolInfos = subject.getPoolInfos();
		
		// Then
		Assert.assertNotNull(poolInfos);
		Assert.assertEquals(2, poolInfos.size());
		
		Map<String, PoolInfoEntity> resultMap = new HashMap<String, PoolInfoEntity>();
		for (int i = 0; i < poolInfos.size(); i++) {
			resultMap.put(poolInfos.get(i).getOrganisationCode(), poolInfos.get(i));
		}
		
		Assert.assertTrue(resultMap.containsKey(POOL_TEST_ORG));
		Assert.assertTrue(resultMap.containsKey(POOL_TEST_ORG2));
		
		Assert.assertEquals(10, resultMap.get(POOL_TEST_ORG).getWantedPoolSize());
		Assert.assertEquals(30, resultMap.get(POOL_TEST_ORG2).getWantedPoolSize());
		
		Assert.assertEquals(1, resultMap.get(POOL_TEST_ORG).getAvailablePoolSize());
		Assert.assertEquals(0, resultMap.get(POOL_TEST_ORG2).getAvailablePoolSize());

	}
}