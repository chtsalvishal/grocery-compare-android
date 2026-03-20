package com.example.grocerycompare.util

object Categorizer {
    private val categoryKeywords = mapOf(
        "Dairy" to listOf("Milk", "Cheese", "Yogurt", "Butter", "Cream", "Egg"),
        "Produce" to listOf("Apple", "Banana", "Tomato", "Onion", "Potato", "Carrot", "Lettuce", "Fruit", "Veg"),
        "Pantry" to listOf("Pasta", "Rice", "Flour", "Sugar", "Oil", "Sauce", "Cereal", "Canned"),
        "Meat" to listOf("Chicken", "Beef", "Pork", "Lamb", "Mince", "Steak", "Sausage", "Bacon"),
        "Bakery" to listOf("Bread", "Roll", "Cake", "Cookie", "Pastry", "Donut"),
        "Frozen" to listOf("Ice Cream", "Frozen", "Pizza", "Peas"),
        "Household" to listOf("Cleaning", "Detergent", "Soap", "Paper", "Tissue", "Bin"),
        "Personal Care" to listOf("Shampoo", "Toothpaste", "Deodorant", "Body Wash")
    )

    fun categorize(name: String): String {
        val lowerName = name.lowercase()
        for ((category, keywords) in categoryKeywords) {
            if (keywords.any { lowerName.contains(it.lowercase()) }) {
                return category
            }
        }
        return "General"
    }
}
