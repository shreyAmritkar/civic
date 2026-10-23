package com.civictrack.entity;

/** PENDING: waiting for the dispatcher. DISPATCHED: a JobRunr job exists. SENT: email delivered. DEAD: gave up. */
public enum OutboxStatus {
    PENDING, DISPATCHED, SENT, DEAD
}