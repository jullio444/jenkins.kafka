package com.citi.gcg.eventhub.midas;


import org.junit.Assert;
import org.junit.Test;


public class ApplicationSpringTest {

	@Test
	public void applicationContextTest() {
		Application .main(new String[] {});
		Assert.assertTrue(Boolean.TRUE);
	}
}


