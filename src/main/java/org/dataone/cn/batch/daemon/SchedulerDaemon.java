/**
 * This work was created by participants in the DataONE project, and is
 * jointly copyrighted by participating institutions in DataONE. For 
 * more information on DataONE, see our web site at http://dataone.org.
 *
 *   Copyright ${year}
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 * 
 * $Id: ServiceDaemon.java 897078 2010-01-08 01:52:47Z sebb $
 */

package org.dataone.cn.batch.daemon;

import java.util.Collection;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.dataone.cn.ldap.NodeAccess;
import org.dataone.cn.ldap.ProcessingState;
import org.dataone.configuration.Settings;
import org.dataone.service.cn.replication.v1.audit.ScheduledReplicationAuditController;
import org.dataone.service.types.v1.NodeReference;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import com.hazelcast.core.Hazelcast;

/**
 * Implements Apache Commons Daemon interface. The Class will start up a Spring
 * Application context configured in a set location The Class assumes that it
 * will be running a Quartz scheduler that will be shutdown when the process
 * ends.
 * 
 * @author rwaltz
 */
public class SchedulerDaemon implements Daemon {

    private String appContextLocation = "file:/etc/dataone/process/applicationContext.xml";
    private ApplicationContext context;
    static final String localCnIdentifier = Settings.getConfiguration().getString("cn.nodeId");

    private static final String SCHEDULED_REPLICA_AUDIT_NAME = "scheduledReplicationAuditController";
    private ScheduledReplicationAuditController replicationAuditScheduler;

    public SchedulerDaemon() {
        super();
        System.out.println("ServiceDaemon: instance " + this.hashCode() + " created");
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        System.out.println("ServiceDaemon: instance " + this.hashCode() + " garbage collected");
    }

    /**
     * init and destroy were added in jakarta-tomcat-daemon.
     */
    @Override
    public void init(DaemonContext context) throws Exception {
        /* Set the err */
        System.out.println("ServiceDaemon: instance " + this.hashCode() + " init");

        System.out.println("ServiceDaemon: init done ");

    }

    @Override
    public void start() {
        /* Dump a message */
        System.out.println("ServiceDaemon: starting");
        context = new FileSystemXmlApplicationContext(appContextLocation);
        // context = new ClassPathXmlApplicationContext(new
        // String[]{"/org/dataone/configuration/applicationContext.xml"});

        startScheduledReplicationAuditing();

        System.out.println("ServiceDaemon: started");
    }

    @Override
    public void stop() {
        /* Dump a message */
        System.out.println("ServiceDaemon: stopping");
        try {

            SchedulerFactory schedulerFactory = new StdSchedulerFactory();
            Collection<Scheduler> quartzSchedulers = schedulerFactory.getAllSchedulers();
            int numOfSchedulers = quartzSchedulers.size();
            System.out.println("Found " + numOfSchedulers + " Quartz schedulers");
            Scheduler[] schedulers = new Scheduler[numOfSchedulers];
            schedulers = quartzSchedulers.toArray(schedulers);

            for (int i = 0; i < numOfSchedulers; i++) {
                if (!schedulers[i].isShutdown()) {
                    System.out.println("Stopping Quartz scheduler "
                            + schedulers[i].getSchedulerName());
                    schedulers[i].shutdown(true);
                }
            }

            /*
             * the code below blows up with
             * java.util.ConcurrentModificationException at
             * java.util.HashMap$HashIterator.nextEntry(HashMap.java:793) at
             * java.util.HashMap$ValueIterator.next(HashMap.java:822) at
             * java.util
             * .Collections$UnmodifiableCollection$1.next(Collections.java:1010)
             * 
             * for (Scheduler scheduler : quartzSchedulers) {
             * System.out.println("Stopping Quartz scheduler " +
             * scheduler.getSchedulerName()); if (!scheduler.isShutdown()) {
             * scheduler.shutdown(true); }
             * System.out.println("Quartz scheduler " +
             * scheduler.getSchedulerName() + " stopped"); }
             */

        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println("Shutting down Quartz scheduler failed: ");
        }
        Hazelcast.shutdownAll();
        NodeAccess nodeAccess = new NodeAccess();
        NodeReference localNodeReference = new NodeReference();
        localNodeReference.setValue(localCnIdentifier);
        try {
            nodeAccess.setProcessingState(localNodeReference, ProcessingState.Offline);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println("Updating Process State failed");
        }

        stopScheduledReplicationAuditing();

        System.out.println("ServiceDaemon: stopped");
    }

    private void stopScheduledReplicationAuditing() {
        if (replicationAuditScheduler != null) {
            System.out.println("Stopping replication audit scheduler....");
            replicationAuditScheduler.shutdown();
        }
    }

    private void startScheduledReplicationAuditing() {
        try {
            replicationAuditScheduler = (ScheduledReplicationAuditController) context
                    .getBean(SCHEDULED_REPLICA_AUDIT_NAME);
            replicationAuditScheduler.startup();
        } catch (Exception e) {
            System.out.println("ServiceDaemon: unable to start Replication Audit Scheduler.");
            e.printStackTrace();
        }
    }

    @Override
    public void destroy() {
        System.out.println("ServiceDaemon: instance " + this.hashCode() + " destroy");
    }
}
