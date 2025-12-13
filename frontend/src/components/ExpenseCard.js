import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';

const ExpenseCard = ({ expense, onEdit, onDelete }) => {
  const formatDate = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleDateString();
  };

  const formatAmount = (amount) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(amount);
  };

  return (
    <View style={styles.card}>
      <View style={styles.header}>
        <Text style={styles.title}>{expense.title}</Text>
        <Text style={styles.amount}>{formatAmount(expense.amount)}</Text>
      </View>
      <View style={styles.details}>
        <Text style={styles.category}>{expense.category}</Text>
        <Text style={styles.date}>{formatDate(expense.date)}</Text>
      </View>
      {expense.description && (
        <Text style={styles.description}>{expense.description}</Text>
      )}
      <View style={styles.actions}>
        <TouchableOpacity style={styles.editButton} onPress={() => onEdit(expense)}>
          <Text style={styles.buttonText}>Edit</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.deleteButton} onPress={() => onDelete(expense.id)}>
          <Text style={styles.buttonText}>Delete</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  card: {
    backgroundColor: '#ffffff',
    borderRadius: 12,
    padding: 16,
    marginVertical: 8,
    marginHorizontal: 16,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  title: {
    fontSize: 18,
    fontWeight: '600',
    color: '#333',
    flex: 1,
  },
  amount: {
    fontSize: 18,
    fontWeight: '700',
    color: '#e74c3c',
  },
  details: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 8,
  },
  category: {
    fontSize: 14,
    color: '#3498db',
    backgroundColor: '#ebf5fb',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 4,
  },
  date: {
    fontSize: 14,
    color: '#7f8c8d',
  },
  description: {
    fontSize: 14,
    color: '#666',
    marginBottom: 12,
  },
  actions: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    gap: 8,
  },
  editButton: {
    backgroundColor: '#3498db',
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 6,
  },
  deleteButton: {
    backgroundColor: '#e74c3c',
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 6,
  },
  buttonText: {
    color: '#fff',
    fontWeight: '600',
  },
});

export default ExpenseCard;
