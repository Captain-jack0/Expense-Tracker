import React, { useState, useEffect } from 'react';
import { View, Text, TextInput, TouchableOpacity, StyleSheet } from 'react-native';

const CATEGORIES = ['Food', 'Transport', 'Entertainment', 'Shopping', 'Bills', 'Health', 'Education', 'Other'];

const ExpenseForm = ({ expense, onSubmit, onCancel }) => {
  const [formData, setFormData] = useState({
    title: '',
    amount: '',
    category: 'Other',
    description: '',
    date: new Date().toISOString().split('T')[0],
  });

  useEffect(() => {
    if (expense) {
      setFormData({
        title: expense.title || '',
        amount: expense.amount?.toString() || '',
        category: expense.category || 'Other',
        description: expense.description || '',
        date: expense.date || new Date().toISOString().split('T')[0],
      });
    }
  }, [expense]);

  const handleSubmit = () => {
    if (!formData.title || !formData.amount || !formData.category) {
      alert('Please fill in all required fields');
      return;
    }

    onSubmit({
      ...formData,
      amount: parseFloat(formData.amount),
    });
  };

  return (
    <View style={styles.container}>
      <Text style={styles.heading}>{expense ? 'Edit Expense' : 'Add Expense'}</Text>

      <Text style={styles.label}>Title *</Text>
      <TextInput
        style={styles.input}
        value={formData.title}
        onChangeText={(text) => setFormData({ ...formData, title: text })}
        placeholder="Enter expense title"
      />

      <Text style={styles.label}>Amount *</Text>
      <TextInput
        style={styles.input}
        value={formData.amount}
        onChangeText={(text) => setFormData({ ...formData, amount: text })}
        placeholder="0.00"
        keyboardType="numeric"
      />

      <Text style={styles.label}>Category *</Text>
      <View style={styles.categoryContainer}>
        {CATEGORIES.map((cat) => (
          <TouchableOpacity
            key={cat}
            style={[
              styles.categoryButton,
              formData.category === cat && styles.categoryButtonActive,
            ]}
            onPress={() => setFormData({ ...formData, category: cat })}
          >
            <Text
              style={[
                styles.categoryText,
                formData.category === cat && styles.categoryTextActive,
              ]}
            >
              {cat}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      <Text style={styles.label}>Date *</Text>
      <TextInput
        style={styles.input}
        value={formData.date}
        onChangeText={(text) => setFormData({ ...formData, date: text })}
        placeholder="YYYY-MM-DD"
      />

      <Text style={styles.label}>Description</Text>
      <TextInput
        style={[styles.input, styles.textArea]}
        value={formData.description}
        onChangeText={(text) => setFormData({ ...formData, description: text })}
        placeholder="Optional description"
        multiline
        numberOfLines={3}
      />

      <View style={styles.buttonContainer}>
        <TouchableOpacity style={styles.cancelButton} onPress={onCancel}>
          <Text style={styles.cancelButtonText}>Cancel</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.submitButton} onPress={handleSubmit}>
          <Text style={styles.submitButtonText}>{expense ? 'Update' : 'Add'}</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    backgroundColor: '#fff',
    padding: 20,
    borderRadius: 12,
    margin: 16,
  },
  heading: {
    fontSize: 24,
    fontWeight: '700',
    marginBottom: 20,
    color: '#333',
  },
  label: {
    fontSize: 14,
    fontWeight: '600',
    color: '#555',
    marginBottom: 6,
  },
  input: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 12,
    fontSize: 16,
    marginBottom: 16,
  },
  textArea: {
    height: 80,
    textAlignVertical: 'top',
  },
  categoryContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
    marginBottom: 16,
  },
  categoryButton: {
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: '#ddd',
  },
  categoryButtonActive: {
    backgroundColor: '#3498db',
    borderColor: '#3498db',
  },
  categoryText: {
    color: '#666',
  },
  categoryTextActive: {
    color: '#fff',
  },
  buttonContainer: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    gap: 12,
    marginTop: 10,
  },
  cancelButton: {
    paddingHorizontal: 24,
    paddingVertical: 12,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#ddd',
  },
  cancelButtonText: {
    color: '#666',
    fontWeight: '600',
  },
  submitButton: {
    backgroundColor: '#27ae60',
    paddingHorizontal: 24,
    paddingVertical: 12,
    borderRadius: 8,
  },
  submitButtonText: {
    color: '#fff',
    fontWeight: '600',
  },
});

export default ExpenseForm;
