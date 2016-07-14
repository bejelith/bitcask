package com.workday.bitcask;

import java.util.ArrayList;
import java.util.List;

import org.cyclopsgroup.jcli.annotation.Argument;
import org.cyclopsgroup.jcli.annotation.Cli;
import org.cyclopsgroup.jcli.annotation.MultiValue;
import org.cyclopsgroup.jcli.annotation.Option;

@Cli(name = "bitcask", description = "Tesk Riak from cmdline", note = "")
public class MainOptions {

	private List<String> arguments = new ArrayList<String>();

	public static final String STDERR = "System.err";

	public static final String STDOUT = "System.out";

	private boolean help = false;

	private String logLevel = "DEBUG";

	private String file = null;

	@Option(name = "l", longName = "level", description = "String level")
	public final void setLogLevel(String level) {
		this.logLevel = level;
	}

	public final String getLogLevel() {
		return this.logLevel;
	}
	
	@Option(name = "k", longName = "keys", description = "List of keys")
	public final void setKeyFile(String file) {
		this.file = file;
	}

	public final String getKeyFile() {
		return this.file ;
	}

	/**
	 * @param help
	 *            True to turn on <code>help</code> flag
	 */
	@Option(name = "h", longName = "help", description = "Show usage of this command line")
	public final boolean isHelp() {
		return help;
	}

	public final void setHelp(boolean help) {
		this.help = help;
	}

	public List<String> getArguments() {
		return arguments;
	}

	@MultiValue(listType = ArrayList.class)
	@Argument(description = "List of bucket/key")
	public void setArguments(List<String> arg) {
		arguments = arg;
	}
}
