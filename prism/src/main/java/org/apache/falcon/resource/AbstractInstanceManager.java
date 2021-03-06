/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.falcon.resource;

import org.apache.commons.lang.StringUtils;
import org.apache.falcon.*;
import org.apache.falcon.entity.EntityUtil;
import org.apache.falcon.entity.parser.ValidationException;
import org.apache.falcon.entity.v0.Entity;
import org.apache.falcon.entity.v0.EntityType;
import org.apache.falcon.entity.v0.Frequency;
import org.apache.falcon.entity.v0.SchemaHelper;
import org.apache.falcon.logging.LogProvider;
import org.apache.falcon.resource.InstancesResult.Instance;
import org.apache.falcon.workflow.engine.AbstractWorkflowEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;

/**
 * A base class for managing Entity's Instance operations.
 */
public abstract class AbstractInstanceManager extends AbstractEntityManager {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractInstanceManager.class);

    private static final long MINUTE_IN_MILLIS = 60000L;
    private static final long HOUR_IN_MILLIS = 3600000L;
    protected static final long DAY_IN_MILLIS = 86400000L;
    private static final long MONTH_IN_MILLIS = 2592000000L;

    protected void checkType(String type) {
        if (StringUtils.isEmpty(type)) {
            throw FalconWebException.newInstanceException("entity type is empty",
                    Response.Status.BAD_REQUEST);
        } else {
            EntityType entityType = EntityType.valueOf(type.toUpperCase());
            if (entityType == EntityType.CLUSTER) {
                throw FalconWebException.newInstanceException(
                        "Instance management functions don't apply to Cluster entities",
                        Response.Status.BAD_REQUEST);
            }
        }
    }

    protected List<LifeCycle> checkAndUpdateLifeCycle(List<LifeCycle> lifeCycleValues,
                                                      String type) throws FalconException {
        EntityType entityType = EntityType.valueOf(type.toUpperCase().trim());
        if (lifeCycleValues == null || lifeCycleValues.isEmpty()) {
            List<LifeCycle> lifeCycles = new ArrayList<LifeCycle>();
            if (entityType == EntityType.PROCESS) {
                lifeCycles.add(LifeCycle.valueOf(LifeCycle.EXECUTION.name()));
            } else if (entityType == EntityType.FEED) {
                lifeCycles.add(LifeCycle.valueOf(LifeCycle.REPLICATION.name()));
            }
            return lifeCycles;
        }
        for (LifeCycle lifeCycle : lifeCycleValues) {
            if (entityType != lifeCycle.getTag().getType()) {
                throw new FalconException("Incorrect lifecycle: " + lifeCycle + "for given type: " + type);
            }
        }
        return lifeCycleValues;
    }

    //SUSPEND CHECKSTYLE CHECK ParameterNumberCheck
    public InstancesResult getRunningInstances(String type, String entity,
                                               String colo, List<LifeCycle> lifeCycles, String filterBy,
                                               String orderBy, String sortOrder, Integer offset, Integer numResults) {
        checkColo(colo);
        checkType(type);
        try {
            lifeCycles = checkAndUpdateLifeCycle(lifeCycles, type);
            validateNotEmpty("entityName", entity);
            AbstractWorkflowEngine wfEngine = getWorkflowEngine();
            Entity entityObject = EntityUtil.getEntity(type, entity);
            return getInstanceResultSubset(wfEngine.getRunningInstances(entityObject, lifeCycles),
                    filterBy, orderBy, sortOrder, offset, numResults);
        } catch (Throwable e) {
            LOG.error("Failed to get running instances", e);
            throw FalconWebException.newInstanceException(e, Response.Status.BAD_REQUEST);
        }
    }

    //SUSPEND CHECKSTYLE CHECK ParameterNumberCheck
    public InstancesResult getInstances(String type, String entity, String startStr, String endStr,
                                        String colo, List<LifeCycle> lifeCycles,
                                        String filterBy, String orderBy, String sortOrder,
                                        Integer offset, Integer numResults) {
        return getStatus(type, entity, startStr, endStr, colo, lifeCycles,
                filterBy, orderBy, sortOrder, offset, numResults);
    }

    public InstancesResult getStatus(String type, String entity, String startStr, String endStr,
                                     String colo, List<LifeCycle> lifeCycles,
                                     String filterBy, String orderBy, String sortOrder,
                                     Integer offset, Integer numResults) {
        checkColo(colo);
        checkType(type);
        try {
            lifeCycles = checkAndUpdateLifeCycle(lifeCycles, type);
            validateParams(type, entity);
            Entity entityObject = EntityUtil.getEntity(type, entity);
            Pair<Date, Date> startAndEndDate = getStartAndEndDate(entityObject, startStr, endStr);

            // LifeCycle lifeCycleObject = EntityUtil.getLifeCycle(lifeCycle);
            AbstractWorkflowEngine wfEngine = getWorkflowEngine();
            return getInstanceResultSubset(wfEngine.getStatus(entityObject,
                            startAndEndDate.first, startAndEndDate.second, lifeCycles),
                    filterBy, orderBy, sortOrder, offset, numResults);
        } catch (Throwable e) {
            LOG.error("Failed to get instances status", e);
            throw FalconWebException
                    .newInstanceException(e, Response.Status.BAD_REQUEST);
        }
    }

    public InstancesSummaryResult getSummary(String type, String entity, String startStr, String endStr,
                                             String colo, List<LifeCycle> lifeCycles) {
        checkColo(colo);
        checkType(type);
        try {
            lifeCycles = checkAndUpdateLifeCycle(lifeCycles, type);
            validateParams(type, entity);
            Entity entityObject = EntityUtil.getEntity(type, entity);
            Pair<Date, Date> startAndEndDate = getStartAndEndDate(entityObject, startStr, endStr);

            AbstractWorkflowEngine wfEngine = getWorkflowEngine();
            return wfEngine.getSummary(entityObject, startAndEndDate.first, startAndEndDate.second, lifeCycles);
        } catch (Throwable e) {
            LOG.error("Failed to get instances status", e);
            throw FalconWebException.newInstanceSummaryException(e, Response.Status.BAD_REQUEST);
        }
    }

    public InstancesResult getLogs(String type, String entity, String startStr, String endStr,
                                   String colo, String runId, List<LifeCycle> lifeCycles,
                                   String filterBy, String orderBy, String sortOrder,
                                   Integer offset, Integer numResults) {
        try {
            lifeCycles = checkAndUpdateLifeCycle(lifeCycles, type);
            // getStatus does all validations and filters clusters
            InstancesResult result = getStatus(type, entity, startStr, endStr,
                    colo, lifeCycles, filterBy, orderBy, sortOrder, offset, numResults);
            LogProvider logProvider = new LogProvider();
            Entity entityObject = EntityUtil.getEntity(type, entity);
            for (Instance instance : result.getInstances()) {
                logProvider.populateLogUrls(entityObject, instance, runId);
            }
            return result;
        } catch (Exception e) {
            LOG.error("Failed to get logs for instances", e);
            throw FalconWebException.newInstanceException(e,
                    Response.Status.BAD_REQUEST);
        }
    }

    private InstancesResult getInstanceResultSubset(InstancesResult resultSet, String filterBy,
                                                    String orderBy, String sortOrder,
                                                    Integer offset, Integer numResults) {

        ArrayList<Instance> instanceSet = new ArrayList<Instance>();
        if (resultSet.getInstances() == null) {
            // return the empty resultSet
            resultSet.setInstances(new Instance[0]);
            return resultSet;
        }

        // Filter instances
        instanceSet = filteredInstanceSet(resultSet, instanceSet, getFilterByFieldsValues(filterBy));

        int pageCount = super.getRequiredNumberOfResults(instanceSet.size(), offset, numResults);
        if (pageCount == 0) {
            // return empty result set
            return new InstancesResult(resultSet.getMessage(), new Instance[0]);
        }
        // Sort the ArrayList using orderBy
        instanceSet = sortInstances(instanceSet, orderBy, sortOrder);
        return new InstancesResult(resultSet.getMessage(),
                instanceSet.subList(offset, (offset+pageCount)).toArray(new Instance[pageCount]));
    }

    private ArrayList<Instance> filteredInstanceSet(InstancesResult resultSet, ArrayList<Instance> instanceSet,
                                                  HashMap<String, String> filterByFieldsValues) {

        for (Instance instance : resultSet.getInstances()) {
            boolean addInstance = true;
            // If filterBy is empty, return all instances. Else return instances with matching filter.
            if (filterByFieldsValues.size() > 0) {
                String filterValue;
                for (Map.Entry<String, String> pair : filterByFieldsValues.entrySet()) {
                    filterValue = pair.getValue();
                    if (filterValue.equals("")) {
                        continue;
                    }
                    try {
                        switch (InstancesResult.InstanceFilterFields.valueOf(pair.getKey().toUpperCase())) {
                        case STATUS:
                            String status = "";
                            if (instance.getStatus() != null) {
                                status = instance.getStatus().toString();
                            }
                            if (!status.equalsIgnoreCase(filterValue)) {
                                addInstance = false;
                            }
                            break;
                        case CLUSTER:
                            if (!instance.getCluster().equalsIgnoreCase(filterValue)) {
                                addInstance = false;
                            }
                            break;
                        case SOURCECLUSTER:
                            if (!instance.getSourceCluster().equalsIgnoreCase(filterValue)) {
                                addInstance = false;
                            }
                            break;
                        case STARTEDAFTER:
                            if (instance.getStartTime().before(EntityUtil.parseDateUTC(filterValue))) {
                                addInstance = false;
                            }
                            break;
                        default:
                            break;
                        }
                    } catch (Exception e) {
                        LOG.error("Invalid entry for filterBy field", e);
                        throw FalconWebException.newInstanceException(e, Response.Status.BAD_REQUEST);
                    }
                    if (!addInstance) {
                        break;
                    }
                }
            }
            if (addInstance) {
                instanceSet.add(instance);
            }
        }
        return instanceSet;
    }

    private ArrayList<Instance> sortInstances(ArrayList<Instance> instanceSet,
                                              String orderBy, String sortOrder) {
        final String order = getValidSortOrder(sortOrder, orderBy);
        if (orderBy.equals("status")) {
            Collections.sort(instanceSet, new Comparator<Instance>() {
                @Override
                public int compare(Instance i1, Instance i2) {
                    if (i1.getStatus() == null) {
                        i1.status = InstancesResult.WorkflowStatus.ERROR;
                    }
                    if (i2.getStatus() == null) {
                        i2.status = InstancesResult.WorkflowStatus.ERROR;
                    }
                    return (order.equalsIgnoreCase("asc")) ? i1.getStatus().name().compareTo(i2.getStatus().name())
                            : i2.getStatus().name().compareTo(i1.getStatus().name());
                }
            });
        } else if (orderBy.equals("cluster")) {
            Collections.sort(instanceSet, new Comparator<Instance>() {
                @Override
                public int compare(Instance i1, Instance i2) {
                    return (order.equalsIgnoreCase("asc")) ? i1.getCluster().compareTo(i2.getCluster())
                            : i2.getCluster().compareTo(i1.getCluster());
                }
            });
        } else if (orderBy.equals("startTime")){
            Collections.sort(instanceSet, new Comparator<Instance>() {
                @Override
                public int compare(Instance i1, Instance i2) {
                    Date start1 = (i1.getStartTime() == null) ? new Date(0) : i1.getStartTime();
                    Date start2 = (i2.getStartTime() == null) ? new Date(0) : i2.getStartTime();
                    return (order.equalsIgnoreCase("asc")) ? start1.compareTo(start2)
                            : start2.compareTo(start1);
                }
            });
        } else if (orderBy.equals("endTime")) {
            Collections.sort(instanceSet, new Comparator<Instance>() {
                @Override
                public int compare(Instance i1, Instance i2) {
                    Date end1 = (i1.getEndTime() == null) ? new Date(0) : i1.getEndTime();
                    Date end2 = (i2.getEndTime() == null) ? new Date(0) : i2.getEndTime();
                    return (order.equalsIgnoreCase("asc")) ? end1.compareTo(end2)
                            : end2.compareTo(end1);
                }
            });
        }//Default : no sort

        return instanceSet;
    }

    //RESUME CHECKSTYLE CHECK ParameterNumberCheck

    public InstancesResult getInstanceParams(String type,
                                          String entity, String startTime,
                                          String colo, List<LifeCycle> lifeCycles) {
        checkColo(colo);
        checkType(type);
        try {
            lifeCycles = checkAndUpdateLifeCycle(lifeCycles, type);
            if (lifeCycles.size() != 1) {
                throw new FalconException("For displaying wf-params there can't be more than one lifecycle "
                        + lifeCycles);
            }
            validateParams(type, entity);
            Entity entityObject = EntityUtil.getEntity(type, entity);
            Pair<Date, Date> startAndEndDate = getStartAndEndDate(entityObject, startTime, null);

            AbstractWorkflowEngine wfEngine = getWorkflowEngine();
            return wfEngine.getInstanceParams(entityObject, startAndEndDate.first, startAndEndDate.second, lifeCycles);
        } catch (Throwable e) {
            LOG.error("Failed to display params of an instance", e);
            throw FalconWebException.newInstanceException(e, Response.Status.BAD_REQUEST);
        }
    }

    public InstancesResult killInstance(HttpServletRequest request,
                                        String type, String entity, String startStr,
                                        String endStr, String colo,
                                        List<LifeCycle> lifeCycles) {
        checkColo(colo);
        checkType(type);
        try {
            lifeCycles = checkAndUpdateLifeCycle(lifeCycles, type);
            audit(request, entity, type, "INSTANCE_KILL");
            validateParams(type, entity);
            Entity entityObject = EntityUtil.getEntity(type, entity);
            Pair<Date, Date> startAndEndDate = getStartAndEndDate(entityObject, startStr, endStr);

            Properties props = getProperties(request);
            AbstractWorkflowEngine wfEngine = getWorkflowEngine();
            return wfEngine.killInstances(entityObject,
                    startAndEndDate.first, startAndEndDate.second, props, lifeCycles);
        } catch (Throwable e) {
            LOG.error("Failed to kill instances", e);
            throw FalconWebException.newInstanceException(e, Response.Status.BAD_REQUEST);
        }
    }

    public InstancesResult suspendInstance(HttpServletRequest request,
                                           String type, String entity, String startStr,
                                           String endStr, String colo,
                                           List<LifeCycle> lifeCycles) {
        checkColo(colo);
        checkType(type);
        try {
            lifeCycles = checkAndUpdateLifeCycle(lifeCycles, type);
            audit(request, entity, type, "INSTANCE_SUSPEND");
            validateParams(type, entity);
            Entity entityObject = EntityUtil.getEntity(type, entity);
            Pair<Date, Date> startAndEndDate = getStartAndEndDate(entityObject, startStr, endStr);

            Properties props = getProperties(request);
            AbstractWorkflowEngine wfEngine = getWorkflowEngine();
            return wfEngine.suspendInstances(entityObject,
                    startAndEndDate.first, startAndEndDate.second, props, lifeCycles);
        } catch (Throwable e) {
            LOG.error("Failed to suspend instances", e);
            throw FalconWebException.newInstanceException(e, Response.Status.BAD_REQUEST);
        }
    }

    public InstancesResult resumeInstance(HttpServletRequest request,
                                          String type, String entity, String startStr,
                                          String endStr, String colo,
                                          List<LifeCycle> lifeCycles) {

        checkColo(colo);
        checkType(type);
        try {
            lifeCycles = checkAndUpdateLifeCycle(lifeCycles, type);
            audit(request, entity, type, "INSTANCE_RESUME");
            validateParams(type, entity);
            Entity entityObject = EntityUtil.getEntity(type, entity);
            Pair<Date, Date> startAndEndDate = getStartAndEndDate(entityObject, startStr, endStr);

            Properties props = getProperties(request);
            AbstractWorkflowEngine wfEngine = getWorkflowEngine();
            return wfEngine.resumeInstances(entityObject,
                    startAndEndDate.first, startAndEndDate.second, props, lifeCycles);
        } catch (Throwable e) {
            LOG.error("Failed to resume instances", e);
            throw FalconWebException.newInstanceException(e, Response.Status.BAD_REQUEST);
        }
    }

    public InstancesResult reRunInstance(String type, String entity, String startStr,
                                         String endStr, HttpServletRequest request,
                                         String colo, List<LifeCycle> lifeCycles) {

        checkColo(colo);
        checkType(type);
        try {
            lifeCycles = checkAndUpdateLifeCycle(lifeCycles, type);
            audit(request, entity, type, "INSTANCE_RERUN");
            validateParams(type, entity);
            Entity entityObject = EntityUtil.getEntity(type, entity);
            Pair<Date, Date> startAndEndDate = getStartAndEndDate(entityObject, startStr, endStr);

            Properties props = getProperties(request);
            AbstractWorkflowEngine wfEngine = getWorkflowEngine();
            return wfEngine.reRunInstances(entityObject,
                    startAndEndDate.first, startAndEndDate.second, props, lifeCycles);
        } catch (Exception e) {
            LOG.error("Failed to rerun instances", e);
            throw FalconWebException.newInstanceException(e, Response.Status.BAD_REQUEST);
        }
    }

    private Properties getProperties(HttpServletRequest request) throws IOException {
        Properties props = new Properties();
        ServletInputStream xmlStream = request == null ? null : request.getInputStream();
        if (xmlStream != null) {
            if (xmlStream.markSupported()) {
                xmlStream.mark(XML_DEBUG_LEN); // mark up to debug len
            }
            props.load(xmlStream);
        }
        return props;
    }

    private Pair<Date, Date> getStartAndEndDate(Entity entityObject, String startStr, String endStr)
        throws FalconException {
        Pair<Date, Date> clusterStartEndDates = EntityUtil.getEntityStartEndDates(entityObject);
        Frequency frequency = EntityUtil.getFrequency(entityObject);
        Date endDate = getEndDate(endStr, clusterStartEndDates.second);
        Date startDate = getStartDate(startStr, endDate, clusterStartEndDates.first, frequency);

        if (startDate.after(endDate)) {
            throw new FalconException("Specified End date " + SchemaHelper.getDateFormat().format(endDate)
                    + " is before the entity was scheduled " + SchemaHelper.getDateFormat().format(startDate));
        }
        return new Pair<Date, Date>(startDate, endDate);
    }

    private Date getEndDate(String endStr, Date clusterEndDate) throws FalconException {
        Date endDate = StringUtils.isEmpty(endStr) ? new Date() : EntityUtil.parseDateUTC(endStr);
        if (endDate.after(clusterEndDate)) {
            endDate = clusterEndDate;
        }
        return endDate;
    }

    private Date getStartDate(String startStr, Date end,
                              Date clusterStartDate, Frequency frequency) throws FalconException {
        Date start;
        final int dateMultiplier = 10;
        if (StringUtils.isEmpty(startStr)) {
            // set startDate to endDate - 10 times frequency
            long startMillis = end.getTime();

            switch (frequency.getTimeUnit().getCalendarUnit()){
            case Calendar.MINUTE :
                startMillis -= frequency.getFrequencyAsInt() * MINUTE_IN_MILLIS * dateMultiplier;
                break;

            case Calendar.HOUR :
                startMillis -= frequency.getFrequencyAsInt() * HOUR_IN_MILLIS * dateMultiplier;
                break;

            case Calendar.DATE :
                startMillis -= frequency.getFrequencyAsInt() * DAY_IN_MILLIS * dateMultiplier;
                break;

            case Calendar.MONTH :
                startMillis -= frequency.getFrequencyAsInt() * MONTH_IN_MILLIS * dateMultiplier;
                break;

            default:
                break;
            }

            start = new Date(startMillis);
        } else {
            start = EntityUtil.parseDateUTC(startStr);
        }

        if (start.before(clusterStartDate)) {
            start = clusterStartDate;
        }

        return start;
    }

    private void validateParams(String type, String entity) throws FalconException {
        validateNotEmpty("entityType", type);
        validateNotEmpty("entityName", entity);
    }

    private void validateNotEmpty(String field, String param) throws ValidationException {
        if (StringUtils.isEmpty(param)) {
            throw new ValidationException("Parameter " + field + " is empty");
        }
    }
}
