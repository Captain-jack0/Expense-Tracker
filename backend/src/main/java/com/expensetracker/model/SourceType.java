package com.expensetracker.model;

/**
 * How a transaction was created:
 * <ul>
 *   <li>MANUAL — entered directly by the user</li>
 *   <li>RECURRING — generated from a {@code RecurringRule} (Epic 4)</li>
 *   <li>INSTALLMENT — generated from an {@code InstallmentPlan} (Epic 5)</li>
 * </ul>
 */
public enum SourceType {
    MANUAL,
    RECURRING,
    INSTALLMENT
}
