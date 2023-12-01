/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
module org.jboss.logmanager.log4j {
    requires org.apache.logging.log4j;
    requires org.jboss.logmanager;

    exports org.jboss.logmanager.log4j;

    provides org.apache.logging.log4j.spi.Provider with
            org.jboss.logmanager.log4j.JBossProvider;
}