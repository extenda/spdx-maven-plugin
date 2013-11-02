package com.extenda.maven.plugins;

import java.util.Collections;
import java.util.List;

import org.apache.maven.settings.Settings;

public class SettingsStub extends Settings
{
	/** {@inheritDoc} */
	public List getProxies()
	{
		return Collections.EMPTY_LIST;
	}
}

