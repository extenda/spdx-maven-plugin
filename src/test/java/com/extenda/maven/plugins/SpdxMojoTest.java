package com.extenda.maven.plugins;

import org.junit.Test;

/**
 * Unit test for simple App.
 */

public class SpdxMojoTest
{

    /**
     * Rigourous Test :-)
     */
    @Test
    public void testSpdxMojo()
    {
        SpdxMojo mojo = new SpdxMojo();
        System.out.println(mojo.generateUTCTime());
    }
}
