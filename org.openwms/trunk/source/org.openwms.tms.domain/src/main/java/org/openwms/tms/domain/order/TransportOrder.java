/*
 * openwms.org, the Open Warehouse Management System.
 *
 * This file is part of openwms.org.
 *
 * openwms.org is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * openwms.org is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software. If not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.openwms.tms.domain.order;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

import org.openwms.common.domain.DomainObject;
import org.openwms.common.domain.Location;
import org.openwms.common.domain.LocationGroup;
import org.openwms.common.domain.TransportUnit;
import org.openwms.common.domain.values.Problem;
import org.openwms.common.exception.InsufficientValueException;
import org.openwms.tms.domain.values.PriorityLevel;
import org.openwms.tms.domain.values.TransportOrderState;

/**
 * A TransportOrder.
 * <p>
 * Is used to move {@link TransportUnit}s from an actual {@link Location} to a
 * target {@link Location}.
 * </p>
 * 
 * @author <a href="mailto:openwms@googlemail.com">Heiko Scherrer</a>
 * @version $Revision$
 * @since 0.1
 * @see org.openwms.common.domain.TransportUnit
 * @see org.openwms.common.domain.Location
 */
@Entity
@Table(name = "TMS_TRANSPORT_ORDER")
@NamedQueries( {
        @NamedQuery(name = TransportOrder.NQ_FIND_ALL, query = "select to from TransportOrder to"),
        @NamedQuery(name = TransportOrder.NQ_FIND_BY_TU, query = "select to from TransportOrder to where to.transportUnit = :transportUnit"),
        @NamedQuery(name = TransportOrder.NQ_FIND_FOR_TU_IN_STATE, query = "select to from TransportOrder to where to.transportUnit = :transportUnit and to.state in (:states)"),
        @NamedQuery(name = TransportOrder.NQ_FIND_ORDERS_TO_START, query = "select to from TransportOrder to where to.transportUnit = :transportUnit and to.state in (INITIALIZED, INTERRUPTED) order by to.priority DESC, to.creationDate") })
public class TransportOrder implements DomainObject, Serializable {

    /**
     * The serialVersionUID
     */
    private static final long serialVersionUID = 4586898047981474230L;

    /**
     * Query to find all {@link TransportOrder}s.
     */
    public static final String NQ_FIND_ALL = "TransportOrder.findAll";

    /**
     * Query to find all {@link TransportOrder}s for a given
     * {@link TransportUnit}.
     * <li> Query parameter name transportUnit : The {@link TransportUnit} to
     * search for. </li>
     */
    public static final String NQ_FIND_BY_TU = "TransportOrder.findByTransportUnit";

    /**
     * Query to find all {@link TransportOrder}s for a given
     * {@link TransportUnit} in certain states.
     * <li>Query parameter name <strong>transportUnit</strong> : The
     * {@link TransportUnit} to search for.</li>
     * <li>Query parameter name <strong>states</strong> : A list of
     * {@link TransportOrderState}s.</li>
     */
    public static final String NQ_FIND_FOR_TU_IN_STATE = "TransportOrder.findActiveToForTu";

    /**
     * Query to find all {@link TransportOrder}s for a given
     * {@link TransportUnit} that can be started. Ready transports are in state
     * {@link TransportOrderState#INITIALIZED} or
     * {@link TransportOrderState#INTERRUPTED}. The list of possible transports
     * is sorted by priority and creationDate.
     * <li>Query parameter name transportUnit : The {@link TransportUnit} to
     * search for.</li>
     */
    public static final String NQ_FIND_ORDERS_TO_START = "TransportOrder.findOrdersToStartForTu";

    /**
     * Unique technical key.
     */
    @Id
    @Column(name = "ID")
    @GeneratedValue
    private Long id;

    /**
     * The {@link TransportUnit} to be moved by this {@link TransportOrder}.
     */
    @ManyToOne
    @JoinColumn(name = "TRANSPORT_UNIT", nullable = false)
    private TransportUnit transportUnit;

    /**
     * Date when the {@link TransportOrder} was updated the last time.
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "DATE_UPDATED")
    private Date dateUpdated = new Date();

    /**
     * A priority level of the {@link TransportOrder}. The lower the value the
     * lower the priority.<br>
     * The priority level affects the execution of the {@link TransportOrder}.
     * An order with high priority will be processed faster than these with
     * lower priority.
     */
    @Column(name = "PRIORITY")
    @Enumerated
    private PriorityLevel priority = PriorityLevel.NORMAL;

    /**
     * Date when the {@link TransportOrder} was started.
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "START_DATE")
    private Date startDate;

    /**
     * Last problem on this {@link TransportOrder}.
     */
    @Column(name = "PROBLEM")
    private Problem problem;

    /**
     * Date when the {@link TransportOrder} was created.
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CREATION_DATE")
    private Date creationDate = new Date();

    /**
     * Date when the {@link TransportOrder} ended.
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "END_DATE")
    private Date endDate;

    /**
     * State of this {@link TransportOrder}.
     */
    @Column(name = "STATE")
    @Enumerated(EnumType.STRING)
    private TransportOrderState state = TransportOrderState.CREATED;

    /**
     * The source {@link Location} of the {@link TransportOrder}.<br>
     * This property is set before the {@link TransportOrder} is started.
     */
    @ManyToOne
    @JoinColumn(name = "SOURCE_LOCATION")
    private Location sourceLocation;

    /**
     * The target {@link Location} of the {@link TransportOrder}.<br>
     * This property is set before the {@link TransportOrder} is started.
     */
    @ManyToOne
    @JoinColumn(name = "TARGET_LOCATION")
    private Location targetLocation;

    /**
     * A {@link LocationGroup} can also be set as target. At least one target
     * must be set when the {@link TransportOrder} is being started.
     */
    @ManyToOne
    @JoinColumn(name = "TARGET_LOCATION_GROUP")
    private LocationGroup targetLocationGroup;

    /**
     * Version field.
     */
    @Version
    @Column(name = "C_VERSION")
    private long version;

    /* ------------------- lifecycle callback methods ---------- */

    @PreUpdate
    protected void postUpdate() {
        this.dateUpdated = new Date();
    }

    /* ----------------------------- methods ------------------- */
    /**
     * Create a new {@link TransportOrder}.
     */
    public TransportOrder() {
        this.creationDate = new Date();
        this.state = TransportOrderState.CREATED;
    }

    /**
     * Returns the unique technical key.
     * 
     * @return The unique technical key
     */
    public Long getId() {
        return this.id;
    }

    /**
     * Checks if the instance is transient.
     * 
     * @return <code>true</code>: Entity is not present on the persistent
     *         storage.<br>
     *         <code>false</code> : Entity already exists on the persistent
     *         storage
     */
    public boolean isNew() {
        return (this.id == null);
    }

    /**
     * Get the priority level of this {@link TransportOrder}.
     * 
     * @return The priority
     */
    public PriorityLevel getPriority() {
        return this.priority;
    }

    /**
     * Set the priority level of this {@link TransportOrder}.
     * 
     * @param priority
     *            The priority to set
     */
    public void setPriority(PriorityLevel priority) {
        this.priority = priority;
    }

    /**
     * Returns the date when the {@link TransportOrder} was started.
     * 
     * @return The date when started
     */
    public Date getStartDate() {
        return this.startDate;
    }

    /**
     * Get the {@link TransportUnit} assigned to this {@link TransportOrder}.
     * 
     * @return The assigned transportUnit
     */
    public TransportUnit getTransportUnit() {
        return this.transportUnit;
    }

    /**
     * Assign a {@link TransportUnit} to this {@link TransportOrder}.
     * 
     * @param transportUnit
     *            The transportUnit to be assigned
     */
    public void setTransportUnit(TransportUnit transportUnit) {
        this.transportUnit = transportUnit;
    }

    /**
     * Returns the date when this {@link TransportOrder} was created.
     * 
     * @return The creation date
     */
    public Date getCreationDate() {
        return this.creationDate;
    }

    /**
     * Returns the state of this {@link TransportOrder}.
     * 
     * @return The state of the order
     */
    public TransportOrderState getState() {
        return this.state;
    }

    private void validateInitializationCondition() {
        if (transportUnit == null || (targetLocation == null && targetLocationGroup == null)) {
            throw new InsufficientValueException("Not all properties are set to switch transportOrder in next state");
        }
    }

    private void validateStateChange(TransportOrderState newState) {
        if (newState == null) {
            throw new IllegalStateException("transportState cannot be null");
        }
        if (getState().compareTo(newState) > 0) {
            // Don't allow to turn back the state!
            throw new IllegalStateException("Turning back state of transportOrder not allowed");
        }
        if (getState() == TransportOrderState.CREATED) {
            if (newState != TransportOrderState.INITIALIZED && newState != TransportOrderState.CANCELED) {
                // Don't allow to except the initialization
                throw new IllegalStateException("TransportOrder must be initialized after creation");
            }
            validateInitializationCondition();
        }
    }

    /**
     * Set the state of this {@link TransportOrder}.
     * 
     * @param newState
     *            The new state to set
     * @throws IllegalStateException
     *             in case
     *             <li>the newState is <code>null</code> or</li>
     *             <li>the newState is less than the old state or</li>
     *             <li>the TransportOrder is in state CREATED and shall be
     *             manually turned into something else then INITIALIZED or
     *             CANCELED</li>
     * @throws InsufficientValueException
     *             in case the TransportOrder is CREATED and shall be turned
     *             into INITIALIZED but is incomplete.
     */
    public void setState(TransportOrderState newState) {
        validateStateChange(newState);
        if (newState == TransportOrderState.STARTED) {
            startDate = new Date();
        }
        if (newState == TransportOrderState.FINISHED) {
            endDate = new Date();
        }
        state = newState;
    }

    /**
     * Get the target {@link Location} of this {@link TransportOrder}.
     * 
     * @return The targetLocation if any, otherwise <code>null</code>
     */
    public Location getTargetLocation() {
        return this.targetLocation;
    }

    /**
     * Set the target {@link Location} of this {@link TransportOrder}.
     * 
     * @param targetLocation
     *            The location to move on
     */
    public void setTargetLocation(Location targetLocation) {
        this.targetLocation = targetLocation;
    }

    /**
     * Get the date when the {@link TransportOrder} was changed last time.
     * 
     * @return The date of the last update
     */
    public Date getDateUpdated() {
        return dateUpdated;
    }

    /**
     * Get the targetLocationGroup.
     * 
     * @return The targetLocationGroup if any, otherwise <code>null</code>
     */
    public LocationGroup getTargetLocationGroup() {
        return targetLocationGroup;
    }

    /**
     * Set the targetLocationGroup.
     * 
     * @param targetLocationGroup
     *            The targetLocationGroup to set.
     */
    public void setTargetLocationGroup(LocationGroup targetLocationGroup) {
        this.targetLocationGroup = targetLocationGroup;
    }

    /**
     * Get the last {@link Problem}.
     * 
     * @return The last problem.
     */
    public Problem getProblem() {
        return problem;
    }

    /**
     * Set the last {@link Problem}.
     * 
     * @param problem
     *            The {@link Problem} to set.
     */
    public void setProblem(Problem problem) {
        this.problem = problem;
    }

    /**
     * Get the endDate.
     * 
     * @return The date the order ended
     */
    public Date getEndDate() {
        return endDate;
    }

    /**
     * Get the sourceLocation.
     * 
     * @return The sourceLocation
     */
    public Location getSourceLocation() {
        return sourceLocation;
    }

    /**
     * Set the sourceLocation.
     * 
     * @param sourceLocation
     *            The sourceLocation to set
     */
    public void setSourceLocation(Location sourceLocation) {
        this.sourceLocation = sourceLocation;
    }

    /**
     * JPA optimistic locking.
     * 
     * @return The version field
     */
    public long getVersion() {
        return this.version;
    }
}
