package bitcask;

import java.util.ArrayList;
import java.util.List;

import org.cyclopsgroup.jcli.annotation.Argument;
import org.cyclopsgroup.jcli.annotation.Cli;
import org.cyclopsgroup.jcli.annotation.MultiValue;
import org.cyclopsgroup.jcli.annotation.Option;

@Cli( name = "riak-test", description = "Tesk Riak from cmdline", note = "" )
public class MainOptions
{
	
	private List<String> arguments = new ArrayList<String>();

    public static final String STDERR = "System.err";

    public static final String STDOUT = "System.out";
    
    private boolean help = false;
    
	/**
     * @return {@link #setHelp(boolean)}
     */
    public final boolean isHelp() {
        return help ;
    }

    /**
     * @param help True to turn on <code>help</code> flag
     */
    @Option( name = "h", longName = "help", description = "Show usage of this command line" )
    public final void setHelp( boolean help ) {
        this.help = help;
    }
    
    public List<String> getArguments() {
    	return arguments;
    }

    @MultiValue( listType = ArrayList.class )
    @Argument ( description = "List of hostnames")
    public void setArguments(List<String> arg) {
    	arguments = arg;
    }

}
