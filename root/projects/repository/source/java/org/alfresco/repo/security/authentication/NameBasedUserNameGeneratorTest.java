package org.alfresco.repo.security.authentication;

import junit.framework.TestCase;

public class NameBasedUserNameGeneratorTest extends TestCase
{
	public void testGenerate()
	{
		NameBasedUserNameGenerator generator = new NameBasedUserNameGenerator();
		generator.setUserNameLength(10);
		generator.setNamePattern("%firstName%_%lastName%");
		
		String firstName = "Buffy";
		String lastName = "Summers";
		String emailAddress = "buffy@sunnydale.com";
		
		// should generate buffy_summers
		String userName = generator.generateUserName(firstName, lastName, emailAddress, 0);
		assertEquals("", (firstName + "_" + lastName).toLowerCase(), userName);
		
		// should generate something different from above since seed > 0
		userName = generator.generateUserName(firstName, lastName, emailAddress, 1);
		assertEquals("", (firstName + "_" + lastName).toLowerCase().substring(0,7), userName.substring(0,7));
		assertTrue("", !(firstName + "_" + lastName).toLowerCase().equals(userName));
		
		// should generate buffy_summers@sunnydale.com
		generator.setNamePattern("%emailAddress%");
		userName = generator.generateUserName(firstName, lastName, emailAddress, 0);
		assertEquals("", (emailAddress).toLowerCase(), userName);
		
		// should generate  buffy_s123
		userName = generator.generateUserName(firstName, lastName, emailAddress, 1);
		assertTrue("", !(emailAddress).toLowerCase().equals(userName));
		
		// should generate summers.buffy
		generator.setNamePattern("%lastName%.%firstName%");
		userName = generator.generateUserName(firstName, lastName, emailAddress, 0);
		assertEquals("", (lastName + "." + firstName).toLowerCase(), userName);
		
		// should generate bsummers
		generator.setNamePattern("%i%%lastName%");
		userName = generator.generateUserName(firstName, lastName, emailAddress, 0);
		assertEquals("", ("bsummers").toLowerCase(), userName);
		
	}

}
