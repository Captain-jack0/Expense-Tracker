import React, { useState, useEffect } from 'react';
import { View, Text, ScrollView, TouchableOpacity, StyleSheet, SafeAreaView, Modal } from 'react-native';
import ExpenseCard from './components/ExpenseCard';
import ExpenseForm from './components/ExpenseForm';
import { expenseApi } from './services/api';

const App = () => {
  const [expenses, setExpenses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingExpense, setEditingExpense] = useState(null);
  const [totalExpenses, setTotalExpenses] = useState(0);

  useEffect(() => {
    fetchExpenses();
  }, []);

  useEffect(() => {
    const total = expenses.reduce((sum, exp) => sum + parseFloat(exp.amount || 0), 0);
    setTotalExpenses(total);
  }, [expenses]);

  const fetchExpenses = async () => {
    try {
      setLoading(true);
      const data = await expenseApi.getAllExpenses();
      setExpenses(data);
    } catch (error) {
      console.error('Error fetching expenses:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleAddExpense = async (expenseData) => {
    try {
      await expenseApi.createExpense(expenseData);
      fetchExpenses();
      setShowForm(false);
    } catch (error) {
      console.error('Error adding expense:', error);
    }
  };

  const handleUpdateExpense = async (expenseData) => {
    try {
      await expenseApi.updateExpense(editingExpense.id, expenseData);
      fetchExpenses();
      setShowForm(false);
      setEditingExpense(null);
    } catch (error) {
      console.error('Error updating expense:', error);
    }
  };

  const handleDeleteExpense = async (id) => {
    try {
      await expenseApi.deleteExpense(id);
      fetchExpenses();
    } catch (error) {
      console.error('Error deleting expense:', error);
    }
  };

  const handleEdit = (expense) => {
    setEditingExpense(expense);
    setShowForm(true);
  };

  const handleFormSubmit = (data) => {
    if (editingExpense) {
      handleUpdateExpense(data);
    } else {
      handleAddExpense(data);
    }
  };

  const handleCancel = () => {
    setShowForm(false);
    setEditingExpense(null);
  };

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(amount);
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.headerTitle}>Expense Tracker</Text>
        <View style={styles.totalContainer}>
          <Text style={styles.totalLabel}>Total Expenses</Text>
          <Text style={styles.totalAmount}>{formatCurrency(totalExpenses)}</Text>
        </View>
      </View>

      <TouchableOpacity style={styles.addButton} onPress={() => setShowForm(true)}>
        <Text style={styles.addButtonText}>+ Add Expense</Text>
      </TouchableOpacity>

      {loading ? (
        <View style={styles.loadingContainer}>
          <Text style={styles.loadingText}>Loading...</Text>
        </View>
      ) : expenses.length === 0 ? (
        <View style={styles.emptyContainer}>
          <Text style={styles.emptyText}>No expenses yet</Text>
          <Text style={styles.emptySubtext}>Tap the button above to add your first expense</Text>
        </View>
      ) : (
        <ScrollView style={styles.expenseList}>
          {expenses.map((expense) => (
            <ExpenseCard
              key={expense.id}
              expense={expense}
              onEdit={handleEdit}
              onDelete={handleDeleteExpense}
            />
          ))}
        </ScrollView>
      )}

      <Modal
        visible={showForm}
        animationType="slide"
        transparent={true}
        onRequestClose={handleCancel}
      >
        <View style={styles.modalOverlay}>
          <ExpenseForm
            expense={editingExpense}
            onSubmit={handleFormSubmit}
            onCancel={handleCancel}
          />
        </View>
      </Modal>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f6fa',
  },
  header: {
    backgroundColor: '#3498db',
    padding: 20,
    paddingTop: 40,
  },
  headerTitle: {
    fontSize: 28,
    fontWeight: '700',
    color: '#fff',
    marginBottom: 16,
  },
  totalContainer: {
    backgroundColor: 'rgba(255,255,255,0.2)',
    padding: 16,
    borderRadius: 12,
  },
  totalLabel: {
    fontSize: 14,
    color: 'rgba(255,255,255,0.8)',
  },
  totalAmount: {
    fontSize: 32,
    fontWeight: '700',
    color: '#fff',
  },
  addButton: {
    backgroundColor: '#27ae60',
    margin: 16,
    padding: 16,
    borderRadius: 12,
    alignItems: 'center',
  },
  addButtonText: {
    color: '#fff',
    fontSize: 18,
    fontWeight: '600',
  },
  expenseList: {
    flex: 1,
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  loadingText: {
    fontSize: 18,
    color: '#666',
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  emptyText: {
    fontSize: 20,
    fontWeight: '600',
    color: '#333',
    marginBottom: 8,
  },
  emptySubtext: {
    fontSize: 16,
    color: '#666',
    textAlign: 'center',
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.5)',
    justifyContent: 'center',
  },
});

export default App;
