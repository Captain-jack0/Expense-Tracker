package com.expensetracker.model;

/**
 * Budgeting bucket for the "rules for finance" (50/30/20) breakdown.
 * INCOME is the money-in bucket; NEEDS/WANTS/SAVINGS are the three
 * expense buckets compared against the user's target percentages;
 * OTHER is an uncategorised expense fallback.
 */
public enum Bucket {
    INCOME,
    NEEDS,
    WANTS,
    SAVINGS,
    OTHER
}
