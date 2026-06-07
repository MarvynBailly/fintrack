package com.marvyn.fintrack.data

/**
 * Fixed set of spending categories plus a keyword-based auto-categorizer.
 * Income always maps to [INCOME]; everything else falls back to [OTHER].
 */
object Categories {
    const val INCOME = "Income"
    const val OTHER = "Other"

    /** Expense categories shown in pickers, budgets and charts. */
    val EXPENSE = listOf(
        "Groceries", "Dining", "Shopping", "Transport",
        "Bills", "Entertainment", "Health", OTHER
    )

    /** All categories (income first). */
    val ALL = listOf(INCOME) + EXPENSE

    private val rules: List<Pair<String, List<String>>> = listOf(
        "Groceries" to listOf(
            "grocery", "supermarket", "whole foods", "trader joe", "safeway",
            "kroger", "aldi", "costco", "walmart", "publix"
        ),
        "Dining" to listOf(
            "restaurant", "cafe", "coffee", "starbucks", "mcdonald", "chipotle",
            "doordash", "uber eats", "grubhub", "pizza", "diner", "bar &", "tavern"
        ),
        "Transport" to listOf(
            "uber", "lyft", "shell", "chevron", "gas station", "exxon", "bp ",
            "transit", "parking", "metro", "fuel", "76 "
        ),
        "Bills" to listOf(
            "electric", "water", "utility", "at&t", "verizon", "comcast", "xfinity",
            "internet", "insurance", "rent", "mortgage", "phone", "t-mobile"
        ),
        "Entertainment" to listOf(
            "netflix", "spotify", "hulu", "disney", "cinema", "movie", "steam",
            "playstation", "xbox", "hbo", "youtube premium"
        ),
        "Shopping" to listOf(
            "amazon", "target", "best buy", "ebay", "nike", "apple store",
            "etsy", "ikea", "home depot", "macy"
        ),
        "Health" to listOf(
            "pharmacy", "cvs", "walgreens", "doctor", "clinic", "gym",
            "fitness", "dental", "hospital", "rite aid"
        )
    )

    fun categorize(text: String, isIncome: Boolean): String {
        if (isIncome) return INCOME
        val lower = text.lowercase()
        for ((category, keywords) in rules) {
            if (keywords.any { lower.contains(it) }) return category
        }
        return OTHER
    }
}
