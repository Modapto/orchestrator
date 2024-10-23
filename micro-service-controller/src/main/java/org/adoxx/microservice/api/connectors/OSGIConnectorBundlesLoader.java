package org.adoxx.microservice.api.connectors;
/*
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
*/
public class OSGIConnectorBundlesLoader {
    /**
     * TODO
     * This Class is intended to be used in order to load connectors from OSGI bundles.
     * This will give the possibility to:
     * - Install and remove a connector at runtime
     * - Bundle the connector in a JAR with all its required dependencies avoiding conflicts
     * 
     * Notes:
     * - OSGI bundle start and stop will not be the same of the connector start and stop
     * - The Activator class in the OSGI bundle must register the exported Connector classes implementing the Sync or Async interfaces
     * - This class must instantiate Equinox or Felix, automatically check the jars in a specific folder every X seconds and add or remove the bundles relative to that jars
     * 
     * 
     * https://stackoverflow.com/questions/41721988/how-to-programmatically-start-osgi
     * https://stackoverflow.com/questions/4673406/programmatically-start-osgi-equinox
     * https://stackoverflow.com/questions/16933929/what-is-the-intended-use-case-for-bundle-classpath-in-osgi-bundles
     * https://codeaffectionado.wordpress.com/2014/09/12/felix_first_bundle_the_programmatic_way/
     * https://www.eclipsezone.com/eclipse/forums/t90544.html
     * https://www.javaworld.com/article/2077837/java-se-hello-osgi-part-1-bundles-for-beginners.html?page=3
     * https://developer.jboss.org/thread/203138?_sscc=t
     * http://felix.apache.org/documentation/subprojects/apache-felix-maven-bundle-plugin-bnd.html
     * 
     * http://blog.vogella.com/2017/02/13/control-osgi-ds-component-instances/
     * https://mohanadarshan.wordpress.com/2013/06/23/osgi-white-board-pattern-with-a-sample/
     * http://www.osgi.org/wp-content/uploads/whiteboard1.pdf
     * https://cqdump.wordpress.com/2014/08/05/managing-multiple-instances-of-services-osgi-service-factories/
     * 
     * GOOD EXAMPLE:
     * http://joemat.blogspot.com/2015/06/the-whiteboard-pattern-in-java-8-world.html
     * https://github.com/joemat/whiteboardpattern     * 
     * http://www.lucamasini.net/Home/osgi-with-felix/creating-osgi-bundles-of-your-maven-dependencies
     * 
     * */
    
    public void init() throws Exception {
        /*
        FrameworkFactory frameworkFactory = ServiceLoader.load(FrameworkFactory.class).iterator().next();
        Map<String, String> config = new HashMap<String, String>();
        //config.put("osgi.console", "");
        //config.put("osgi.clean", "true");
        //config.put("osgi.noShutdown", "true");
        //config.put("eclipse.ignoreApp", "true");
        //config.put("osgi.bundles.defaultStartLevel", "4");
        //config.put("osgi.configuration.area", "./configuration");
        // automated bundles deployment
        //config.put("felix.fileinstall.dir", "./dropins");
        //config.put("felix.fileinstall.noInitialDelay", "true");
        //config.put("felix.fileinstall.start.level", "4");
        
        Framework framework = frameworkFactory.newFramework(config);
        framework.start();
        try {
            BundleContext bundleContext = framework.getBundleContext();
            //Bundle myBoundle = bundleContext.installBundle("file:/path/to/bundle.jar");
            //myBoundle.start();
            //bundleContext.registerService(MyService.class.getName(), new MyServiceImpl(), null);
        } finally {
            framework.stop();
            framework.waitForStop(0);
        }
        */
    }
    
    public static void main(String[] argv) {
        try {
            OSGIConnectorBundlesLoader loader = new OSGIConnectorBundlesLoader();
            loader.init();
        }catch(Exception ex) {
            ex.printStackTrace();
        }
    }
}
