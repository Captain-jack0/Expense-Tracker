const API_BASE_URL = 'http://localhost:8080/api';

export const expenseApi = {
  getAllExpenses: async () => {
    const response = await fetch(`${API_BASE_URL}/expenses`);
    if (!response.ok) throw new Error('Failed to fetch expenses');
    return response.json();
  },

  getExpenseById: async (id) => {
    const response = await fetch(`${API_BASE_URL}/expenses/${id}`);
    if (!response.ok) throw new Error('Failed to fetch expense');
    return response.json();
  },

  createExpense: async (expense) => {
    const response = await fetch(`${API_BASE_URL}/expenses`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(expense),
    });
    if (!response.ok) throw new Error('Failed to create expense');
    return response.json();
  },

  updateExpense: async (id, expense) => {
    const response = await fetch(`${API_BASE_URL}/expenses/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(expense),
    });
    if (!response.ok) throw new Error('Failed to update expense');
    return response.json();
  },

  deleteExpense: async (id) => {
    const response = await fetch(`${API_BASE_URL}/expenses/${id}`, {
      method: 'DELETE',
    });
    if (!response.ok) throw new Error('Failed to delete expense');
  },

  getExpensesByCategory: async (category) => {
    const response = await fetch(`${API_BASE_URL}/expenses/category/${category}`);
    if (!response.ok) throw new Error('Failed to fetch expenses by category');
    return response.json();
  },

  getCategories: async () => {
    const response = await fetch(`${API_BASE_URL}/expenses/categories`);
    if (!response.ok) throw new Error('Failed to fetch categories');
    return response.json();
  },
};
