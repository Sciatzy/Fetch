package com.fetch.auth.production.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.fetch.auth.production.model.TaskStatus;

import org.junit.Test;

public class RiderTaskFlowValidatorTest {

    @Test
    public void riderCanProgressAcceptedBookingToCompletedState() {
        String riderId = "rider-123";

        RiderTaskFlowValidator.validateArrivedAtPickup(
                TaskStatus.ACCEPTED,
                riderId,
                riderId,
                RiderTaskFlowValidator.GEOFENCE_ENTERED_PICKUP
        );

        RiderTaskFlowValidator.validateStartTrip(
                TaskStatus.ARRIVED_PICKUP,
                riderId,
                riderId
        );

        RiderTaskFlowValidator.validateArrivedAtDropoff(
                TaskStatus.IN_PROGRESS,
                riderId,
                riderId
        );

        RiderTaskFlowValidator.validateCompletion(
                TaskStatus.ARRIVED_DROPOFF,
                riderId,
                riderId
        );
    }

    @Test
    public void completionFailsWhenTaskIsOnlyAccepted() {
        try {
            RiderTaskFlowValidator.validateCompletion(
                    TaskStatus.ACCEPTED,
                    "rider-123",
                    "rider-123"
            );
            fail("Expected IllegalStateException");
        } catch (IllegalStateException exception) {
            assertEquals("Only active tasks can be completed.", exception.getMessage());
        }
    }

    @Test
    public void completionFailsWhenRiderIsNotAssigned() {
        try {
            RiderTaskFlowValidator.validateCompletion(
                    TaskStatus.ARRIVED_DROPOFF,
                    "rider-123",
                    "rider-999"
            );
            fail("Expected IllegalStateException");
        } catch (IllegalStateException exception) {
            assertEquals("Only the assigned rider can complete this task.", exception.getMessage());
        }
    }

    @Test
    public void arrivedAtPickupFailsWhenOutsidePickupZone() {
        try {
            RiderTaskFlowValidator.validateArrivedAtPickup(
                    TaskStatus.ACCEPTED,
                    "rider-123",
                    "rider-123",
                    "exited_pickup"
            );
            fail("Expected IllegalStateException");
        } catch (IllegalStateException exception) {
            assertEquals("You must be in the pickup zone to continue.", exception.getMessage());
        }
    }

    @Test
    public void completionEarningsUseEstimatedFeeThenBudgetThenZero() {
        assertEquals(240.5, RiderTaskFlowValidator.resolveCompletionEarnings(240.5, 180.0), 0.0001);
        assertEquals(180.0, RiderTaskFlowValidator.resolveCompletionEarnings(null, 180.0), 0.0001);
        assertEquals(0.0, RiderTaskFlowValidator.resolveCompletionEarnings(null, null), 0.0001);
    }
}